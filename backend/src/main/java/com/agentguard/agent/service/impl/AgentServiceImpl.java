package com.agentguard.agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agentguard.agent.dto.AgentCreateDTO;
import com.agentguard.agent.dto.AgentDTO;
import com.agentguard.agent.dto.AgentUpdateDTO;
import com.agentguard.agent.dto.LlmTestConnectionDTO;
import com.agentguard.agent.entity.AgentDO;
import com.agentguard.agent.mapper.AgentMapper;
import com.agentguard.agent.mapper.AgentPolicyBindingMapper;
import com.agentguard.agent.service.AgentService;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.common.util.EncryptionUtil;
import com.agentguard.policy.entity.PolicyDO;
import com.agentguard.policy.mapper.PolicyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 服务实现类
 *
 * @author zhuhx
 */
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final String API_KEY_PREFIX = "ag_";

    private final AgentMapper agentMapper;
    private final AgentPolicyBindingMapper bindingMapper;
    private final PolicyMapper policyMapper;
    private final EncryptionUtil encryptionUtil;
    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public AgentDTO create(AgentCreateDTO dto) {
        AgentDO agentDO = BeanUtil.copyProperties(dto, AgentDO.class);
        agentDO.setApiKey(generateApiKey());

        // 加密 LLM API Key
        if (StrUtil.isNotBlank(dto.getLlmApiKey())) {
            agentDO.setLlmApiKey(encryptionUtil.encrypt(dto.getLlmApiKey()));
        }

        agentMapper.insert(agentDO);
        return toDTO(agentDO);
    }

    @Override
    public AgentDTO getById(String id) {
        AgentDO agentDO = agentMapper.selectById(id);
        if (ObjectUtil.isNull(agentDO)) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }
        return toDTO(agentDO);
    }

    @Override
    public IPage<AgentDTO> page(Page<AgentDTO> page, String keyword) {
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(AgentDO::getName, keyword)
                    .or().like(AgentDO::getDescription, keyword));
        }
        wrapper.orderByDesc(AgentDO::getCreatedAt);

        Page<AgentDO> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<AgentDO> result = agentMapper.selectPage(entityPage, wrapper);

        return result.convert(this::toDTO);
    }

    @Override
    @Transactional
    public AgentDTO update(String id, AgentUpdateDTO dto) {
        AgentDO agentDO = agentMapper.selectById(id);
        if (ObjectUtil.isNull(agentDO)) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }

        // 处理 LLM API Key 更新
        // 如果提供了新的密钥，需要判断是否为脱敏后的密钥
        if (StrUtil.isNotBlank(dto.getLlmApiKey())) {
            // 检查是否为脱敏密钥（包含 "***"）
            if (dto.getLlmApiKey().contains("***")) {
                // 脱敏密钥，不更新（保持原有密钥）
                dto.setLlmApiKey(null);
            } else {
                // 真实密钥，加密后更新
                dto.setLlmApiKey(encryptionUtil.encrypt(dto.getLlmApiKey()));
            }
        }

        // 使用 Hutool 忽略空值拷贝
        BeanUtil.copyProperties(dto, agentDO, CopyOptions.create().ignoreNullValue());

        agentDO.setUpdatedAt(java.time.LocalDateTime.now());

        agentMapper.updateById(agentDO);
        return toDTO(agentDO);
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (agentMapper.deleteById(id) == 0) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }
    }

    @Override
    public AgentDTO getByApiKey(String apiKey) {
        AgentDO agentDO = agentMapper.selectOne(
                new LambdaQueryWrapper<AgentDO>().eq(AgentDO::getApiKey, apiKey)
        );
        if (ObjectUtil.isNull(agentDO)) {
            return null;
        }
        return toDTO(agentDO);
    }

    @Override
    public AgentDTO getByApiKeyForProxy(String apiKey) {
        AgentDO agentDO = agentMapper.selectOne(
                new LambdaQueryWrapper<AgentDO>().eq(AgentDO::getApiKey, apiKey)
        );
        if (ObjectUtil.isNull(agentDO)) {
            return null;
        }
        // 返回未脱敏的 DTO（用于代理服务内部使用）
        return toDTOWithoutMasking(agentDO);
    }

    /**
     * 转换为 DTO，包含策略信息
     */
    private AgentDTO toDTO(AgentDO agentDO) {
        AgentDTO dto = BeanUtil.copyProperties(agentDO, AgentDTO.class);

        // 解密并脱敏 LLM API Key（只显示前缀和后4位）
        if (StrUtil.isNotBlank(agentDO.getLlmApiKey())) {
            String decrypted = encryptionUtil.decrypt(agentDO.getLlmApiKey());
            dto.setLlmApiKey(maskApiKey(decrypted));
        }

        // 查询绑定的策略
        List<String> policyIds = bindingMapper.selectPolicyIdsByAgentId(agentDO.getId());
        if (!policyIds.isEmpty()) {
            List<PolicyDO> policies = policyMapper.selectBatchIds(policyIds);
            List<AgentDTO.PolicySummaryDTO> policySummaries = policies.stream()
                    .map(policy -> {
                        AgentDTO.PolicySummaryDTO summary = new AgentDTO.PolicySummaryDTO();
                        summary.setId(policy.getId());
                        summary.setName(policy.getName());
                        summary.setEnabled(policy.getEnabled());
                        return summary;
                    })
                    .collect(Collectors.toList());
            dto.setPolicies(policySummaries);
        }

        return dto;
    }

    /**
     * 转换为 DTO（不脱敏，用于代理服务内部使用）
     */
    private AgentDTO toDTOWithoutMasking(AgentDO agentDO) {
        AgentDTO dto = BeanUtil.copyProperties(agentDO, AgentDTO.class);

        // 解密 LLM API Key，保留真实密钥用于代理转发
        // 注意：此方法仅供内部代理服务使用，不应暴露给外部API
        if (StrUtil.isNotBlank(agentDO.getLlmApiKey())) {
            dto.setLlmApiKey(encryptionUtil.decrypt(agentDO.getLlmApiKey()));
        }

        // 查询绑定的策略
        List<String> policyIds = bindingMapper.selectPolicyIdsByAgentId(agentDO.getId());
        if (!policyIds.isEmpty()) {
            List<PolicyDO> policies = policyMapper.selectBatchIds(policyIds);
            List<AgentDTO.PolicySummaryDTO> policySummaries = policies.stream()
                    .map(policy -> {
                        AgentDTO.PolicySummaryDTO summary = new AgentDTO.PolicySummaryDTO();
                        summary.setId(policy.getId());
                        summary.setName(policy.getName());
                        summary.setEnabled(policy.getEnabled());
                        return summary;
                    })
                    .collect(Collectors.toList());
            dto.setPolicies(policySummaries);
        }

        return dto;
    }

    /**
     * 脱敏 API Key
     * 例如：sk-1234567890abcdef -> sk-***abcdef
     */
    private String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey) || apiKey.length() <= 10) {
            return "***";
        }
        String prefix = apiKey.substring(0, 3);
        String suffix = apiKey.substring(apiKey.length() - 6);
        return prefix + "***" + suffix;
    }

    private String generateApiKey() {
        return API_KEY_PREFIX + IdUtil.simpleUUID();
    }

    @Override
    public Map<String, Object> testLlmConnection(LlmTestConnectionDTO dto) {
        Map<String, Object> result = new HashMap<>();

        String llmProvider;
        String llmApiKey;
        String llmBaseUrl;
        String llmModel;

        // 如果提供了 agentId，从数据库加载配置
        if (StrUtil.isNotBlank(dto.getAgentId())) {
            AgentDO agentDO = agentMapper.selectById(dto.getAgentId());
            if (ObjectUtil.isNull(agentDO)) {
                result.put("success", false);
                result.put("message", "Agent 不存在");
                return result;
            }

            // 使用 Agent 的配置作为基础
            llmProvider = StrUtil.isNotBlank(dto.getLlmProvider()) ? dto.getLlmProvider() : agentDO.getLlmProvider();
            llmBaseUrl = StrUtil.isNotBlank(dto.getLlmBaseUrl()) ? dto.getLlmBaseUrl() : agentDO.getLlmBaseUrl();
            llmModel = StrUtil.isNotBlank(dto.getLlmModel()) ? dto.getLlmModel() : agentDO.getLlmModel();

            // API Key 处理：如果 DTO 中提供了新的密钥，使用新密钥；否则使用数据库中的密钥
            if (StrUtil.isNotBlank(dto.getLlmApiKey()) && !dto.getLlmApiKey().contains("***")) {
                llmApiKey = dto.getLlmApiKey();
            } else {
                // 解密数据库中的密钥
                llmApiKey = encryptionUtil.decrypt(agentDO.getLlmApiKey());
            }
        } else {
            // 没有提供 agentId，使用 DTO 中的配置
            llmProvider = dto.getLlmProvider();
            llmApiKey = dto.getLlmApiKey();
            llmBaseUrl = dto.getLlmBaseUrl();
            llmModel = dto.getLlmModel();

            // 验证必填字段
            if (StrUtil.isBlank(llmProvider) || StrUtil.isBlank(llmApiKey) ||
                StrUtil.isBlank(llmBaseUrl) || StrUtil.isBlank(llmModel)) {
                result.put("success", false);
                result.put("message", "请填写完整的 LLM 配置信息");
                return result;
            }
        }

        try {
            // 构建 LLM API URL
            String llmUrl = buildLlmUrl(llmBaseUrl);

            // 构建测试请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", llmModel);
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", "Hello")
            ));
            requestBody.put("max_tokens", 10);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + llmApiKey);

            HttpEntity<String> entity = new HttpEntity<>(JSONUtil.toJsonStr(requestBody), headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                llmUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            // 检查响应
            if (response.getStatusCode().is2xxSuccessful()) {
                result.put("success", true);
                result.put("message", "连接成功");

                // 尝试解析响应以获取更多信息
                try {
                    JSONObject responseJson = JSONUtil.parseObj(response.getBody());
                    if (responseJson.containsKey("model")) {
                        result.put("actualModel", responseJson.getStr("model"));
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            } else {
                result.put("success", false);
                result.put("message", "连接失败：HTTP " + response.getStatusCode());
            }

        } catch (Exception e) {
            result.put("success", false);
            String errorMessage = e.getMessage();

            // 提取更友好的错误信息
            if (errorMessage != null) {
                if (errorMessage.contains("401")) {
                    result.put("message", "API 密钥无效或已过期");
                } else if (errorMessage.contains("404")) {
                    result.put("message", "API 地址不正确或模型不存在");
                } else if (errorMessage.contains("timeout") || errorMessage.contains("timed out")) {
                    result.put("message", "连接超时，请检查网络或 API 地址");
                } else if (errorMessage.contains("Connection refused")) {
                    result.put("message", "无法连接到 API 服务器");
                } else {
                    result.put("message", "连接失败：" + errorMessage);
                }
            } else {
                result.put("message", "连接失败：未知错误");
            }
        }

        return result;
    }

    /**
     * 构建 LLM API URL
     * 规则：
     * - 以 # 结尾：强制使用输入的地址（去掉 #）
     * - 以 / 结尾：忽略 v1 版本，直接拼接 /chat/completions
     * - 其他：默认拼接 /v1/chat/completions
     */
    private String buildLlmUrl(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            throw new BusinessException(ErrorCode.AGENT_LLM_CONFIG_INCOMPLETE);
        }

        if (baseUrl.endsWith("#")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        if (baseUrl.endsWith("/")) {
            return baseUrl + "chat/completions";
        }

        return baseUrl + "/v1/chat/completions";
    }
}
