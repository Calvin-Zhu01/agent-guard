package com.agentguard.agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.agentguard.agent.dto.AgentCreateDTO;
import com.agentguard.agent.dto.AgentDTO;
import com.agentguard.agent.dto.AgentUpdateDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        // 加密 LLM API Key（如果提供了新的密钥）
        if (StrUtil.isNotBlank(dto.getLlmApiKey())) {
            dto.setLlmApiKey(encryptionUtil.encrypt(dto.getLlmApiKey()));
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
}
