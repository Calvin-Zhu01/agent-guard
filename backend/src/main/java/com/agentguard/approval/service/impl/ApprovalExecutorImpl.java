package com.agentguard.approval.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agentguard.agent.entity.AgentDO;
import com.agentguard.agent.mapper.AgentMapper;
import com.agentguard.alert.dto.AlertDTO;
import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.enums.NotificationChannelType;
import com.agentguard.alert.service.AlertService;
import com.agentguard.approval.dto.ApprovalExecutionResultDTO;
import com.agentguard.approval.entity.ApprovalRequestDO;
import com.agentguard.approval.enums.ApprovalStatus;
import com.agentguard.approval.enums.ExecutionStatus;
import com.agentguard.approval.mapper.ApprovalMapper;
import com.agentguard.approval.service.ApprovalExecutor;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.common.util.EncryptionUtil;
import com.agentguard.proxy.dto.ProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyResponseDTO;
import com.agentguard.proxy.service.ProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审批执行器实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalExecutorImpl implements ApprovalExecutor {

    private final ApprovalMapper approvalMapper;
    private final AgentMapper agentMapper;
    private final ProxyService proxyService;
    private final AlertService alertService;
    private final RestTemplate restTemplate;
    private final EncryptionUtil encryptionUtil;
    private final com.agentguard.log.service.AgentLogService agentLogService;

    @Value("${approval.auto-execute:true}")
    private boolean autoExecuteEnabled;

    @Value("${alert.default-recipient:admin@agentguard.com}")
    private String defaultRecipient;

    @Override
    @Transactional
    public ApprovalExecutionResultDTO execute(String approvalId) {
        log.info("开始执行审批请求: approvalId={}", approvalId);

        // 1. 获取审批请求
        ApprovalRequestDO approval = approvalMapper.selectById(approvalId);
        if (ObjectUtil.isNull(approval)) {
            throw new BusinessException(ErrorCode.APPROVAL_NOT_FOUND);
        }

        // 2. 检查审批状态
        if (approval.getStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.APPROVAL_NOT_APPROVED);
        }

        // 3. 检查是否已执行
        if (approval.getExecutionStatus() == ExecutionStatus.SUCCESS) {
            log.warn("审批请求已执行成功，跳过重复执行: approvalId={}", approvalId);
            return ApprovalExecutionResultDTO.success(approvalId, approval.getExecutionResult());
        }

        // 4. 更新执行状态为执行中
        approval.setExecutionStatus(ExecutionStatus.EXECUTING);
        approvalMapper.updateById(approval);

        try {
            // 5. 获取Agent信息
            AgentDO agent = agentMapper.selectById(approval.getAgentId());
            if (ObjectUtil.isNull(agent)) {
                throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
            }

            // 6. 解析请求数据并执行
            Map<String, Object> result = executeApprovalRequest(agent, approval);

            // 7. 更新执行结果
            String executionResultJson = JSONUtil.toJsonStr(result);
            approval.setExecutionStatus(ExecutionStatus.SUCCESS);
            approval.setExecutionResult(executionResultJson);
            approval.setExecutedAt(LocalDateTime.now());
            approvalMapper.updateById(approval);

            // 8. 更新关联的日志响应体（只保存 body 部分）
            try {
                Object bodyContent = result.get("body");
                String responseBodyJson = bodyContent != null ? JSONUtil.toJsonStr(bodyContent) : null;
                agentLogService.updateResponseBodyByApprovalRequestId(approvalId, responseBodyJson);
            } catch (Exception e) {
                log.error("更新日志响应体失败: approvalId={}, error={}", approvalId, e.getMessage(), e);
            }

            log.info("审批请求执行成功: approvalId={}", approvalId);
            return ApprovalExecutionResultDTO.success(approvalId, result);

        } catch (Exception e) {
            log.error("审批请求执行失败: approvalId={}, error={}", approvalId, e.getMessage(), e);

            // 更新执行状态为失败
            approval.setExecutionStatus(ExecutionStatus.FAILED);
            approval.setExecutionResult(JSONUtil.toJsonStr(Map.of("error", e.getMessage())));
            approval.setExecutedAt(LocalDateTime.now());
            approvalMapper.updateById(approval);

            // 发送执行失败通知
            sendExecutionFailureNotification(approvalId, e.getMessage());

            return ApprovalExecutionResultDTO.failed(approvalId, e.getMessage());
        }
    }

    @Override
    @Async
    public void executeAsync(String approvalId) {
        log.info("异步执行审批请求: approvalId={}", approvalId);
        try {
            execute(approvalId);
        } catch (Exception e) {
            log.error("异步执行审批请求失败: approvalId={}, error={}", approvalId, e.getMessage(), e);
            // 异常已在 execute 方法中处理，这里只记录日志
        }
    }

    @Override
    public boolean isAutoExecuteEnabled() {
        return autoExecuteEnabled;
    }

    @Override
    public void sendExecutionFailureNotification(String approvalId, String errorMessage) {
        log.info("发送执行失败通知: approvalId={}", approvalId);

        ApprovalRequestDO approval = approvalMapper.selectById(approvalId);
        if (ObjectUtil.isNull(approval)) {
            log.warn("审批请求不存在，无法发送通知: approvalId={}", approvalId);
            return;
        }

        String agentName = getAgentName(approval.getAgentId());

        String title = "【执行失败】审批请求执行失败";
        String content = StrUtil.format(
                "审批执行失败通知\n\n" +
                "审批ID：{}\n" +
                "Agent：{}\n" +
                "策略ID：{}\n" +
                "审批时间：{}\n" +
                "执行时间：{}\n" +
                "失败原因：{}\n\n" +
                "请检查请求配置或手动处理。",
                approvalId,
                agentName,
                approval.getPolicyId(),
                approval.getApprovedAt(),
                LocalDateTime.now(),
                errorMessage);

        sendAlert(title, content);
    }

    @Override
    public void sendRejectionNotification(String approvalId, String reason) {
        log.info("发送审批拒绝通知: approvalId={}", approvalId);

        ApprovalRequestDO approval = approvalMapper.selectById(approvalId);
        if (ObjectUtil.isNull(approval)) {
            log.warn("审批请求不存在，无法发送通知: approvalId={}", approvalId);
            return;
        }

        String agentName = getAgentName(approval.getAgentId());

        String title = "【审批拒绝】审批请求已被拒绝";
        String content = StrUtil.format(
                "审批拒绝通知\n\n" +
                "审批ID：{}\n" +
                "Agent：{}\n" +
                "策略ID：{}\n" +
                "创建时间：{}\n" +
                "拒绝时间：{}\n" +
                "拒绝原因：{}\n\n" +
                "如有疑问，请联系管理员。",
                approvalId,
                agentName,
                approval.getPolicyId(),
                approval.getCreatedAt(),
                approval.getApprovedAt(),
                StrUtil.isBlank(reason) ? "无" : reason);

        sendAlert(title, content);
    }

    /**
     * 执行审批请求（根据类型分发）
     *
     * @param agent Agent信息
     * @param approval 审批请求
     * @return 执行结果
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> executeApprovalRequest(AgentDO agent, ApprovalRequestDO approval) {
        String requestDataJson = approval.getRequestData();
        if (StrUtil.isBlank(requestDataJson)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求数据为空");
        }

        Map<String, Object> requestData = JSONUtil.toBean(requestDataJson, Map.class);

        // 检查请求类型
        String type = (String) requestData.get("type");
        if (StrUtil.isBlank(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求类型为空");
        }

        return switch (type) {
            // TODO: LLM 审批功能暂时注释，后期再详细设计
            // case "llm_call" -> executeLlmApprovalRequest(agent, requestData);
            case "llm_call" -> throw new BusinessException(ErrorCode.BAD_REQUEST, "LLM 审批功能暂未开放");
            case "api_call" -> executeApiApprovalRequest(agent, requestData);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的请求类型: " + type);
        };
    }

    /**
     * 执行 LLM 审批请求
     *
     * @param agent Agent信息
     * @param requestData 请求数据
     * @return LLM响应
     */
    @SuppressWarnings("unchecked")
    // TODO: LLM 审批功能暂时注释，后期再详细设计
    // /**
    //  * 执行 LLM 审批请求
    //  *
    //  * @param agent Agent信息
    //  * @param requestData 请求数据
    //  * @return LLM响应
    //  */
    // @SuppressWarnings("unchecked")
    private Map<String, Object> executeLlmApprovalRequest(AgentDO agent, Map<String, Object> requestData) {
        // 提取LLM请求体
        Object bodyObj = requestData.get("body");
        if (bodyObj == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "LLM请求体为空");
        }

        Map<String, Object> llmRequestBody = (Map<String, Object>) bodyObj;
        return executeLlmRequest(agent, llmRequestBody);
    }

    /**
     * 执行 API 审批请求
     *
     * @param agent Agent信息
     * @param requestData 请求数据
     * @return API响应
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> executeApiApprovalRequest(AgentDO agent, Map<String, Object> requestData) {
        String targetUrl = (String) requestData.get("targetUrl");
        String method = (String) requestData.get("method");
        Map<String, String> headers = (Map<String, String>) requestData.get("headers");
        Object body = requestData.get("body");

        if (StrUtil.isBlank(targetUrl)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目标URL为空");
        }

        log.info("执行审批后的API请求: url={}, method={}", targetUrl, method);

        // 构建请求头
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }

        // 构建请求体
        String requestBodyJson = body != null ? JSONUtil.toJsonStr(body) : null;
        HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, httpHeaders);

        // 发起请求
        HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
        ResponseEntity<String> response = restTemplate.exchange(
                targetUrl,
                httpMethod,
                entity,
                String.class
        );

        // 构建响应
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("statusCode", response.getStatusCode().value());
        result.put("headers", response.getHeaders().toSingleValueMap());

        // 解析响应体为对象，避免二次序列化导致的转义问题
        String responseBody = response.getBody();
        if (StrUtil.isNotBlank(responseBody)) {
            try {
                // 尝试解析为 JSON 对象
                Object bodyObj = JSONUtil.parse(responseBody);
                result.put("body", bodyObj);
            } catch (Exception e) {
                // 如果解析失败，说明不是 JSON 格式，直接存储字符串
                log.warn("响应体不是有效的 JSON 格式，直接存储为字符串: {}", e.getMessage());
                result.put("body", responseBody);
            }
        } else {
            result.put("body", null);
        }

        return result;
    }

    /**
     * 解析请求数据（已废弃，使用 executeApprovalRequest 代替）
     *
     * @param approval 审批请求
     * @return LLM请求体
     * @deprecated 使用 {@link #executeApprovalRequest(AgentDO, ApprovalRequestDO)} 代替
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseLlmRequestBody(ApprovalRequestDO approval) {
        String requestDataJson = approval.getRequestData();
        if (StrUtil.isBlank(requestDataJson)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求数据为空");
        }

        Map<String, Object> requestData = JSONUtil.toBean(requestDataJson, Map.class);

        // 检查请求类型
        String type = (String) requestData.get("type");
        if (!"llm_call".equals(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的请求类型: " + type);
        }

        // 提取LLM请求体
        Object bodyObj = requestData.get("body");
        if (bodyObj == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "LLM请求体为空");
        }

        return (Map<String, Object>) bodyObj;
    }

    /**
     * 执行LLM请求（绕过策略检查，直接调用LLM API）
     *
     * @param agent Agent信息
     * @param requestBody LLM请求体
     * @return LLM响应
     */
    // TODO: LLM 审批功能暂时注释，后期再详细设计
    // /**
    //  * 执行LLM请求（绕过策略检查，直接调用LLM API）
    //  *
    //  * @param agent Agent信息
    //  * @param requestBody LLM请求体
    //  * @return LLM响应
    //  */
    private Map<String, Object> executeLlmRequest(AgentDO agent, Map<String, Object> requestBody) {
        // 检查LLM配置
        if (StrUtil.isBlank(agent.getLlmApiKey()) || StrUtil.isBlank(agent.getLlmBaseUrl())) {
            throw new BusinessException(ErrorCode.AGENT_LLM_CONFIG_INCOMPLETE);
        }

        // 检查是否为流式请求
        Boolean stream = (Boolean) requestBody.get("stream");
        boolean isStreamRequest = stream != null && stream;

        if (isStreamRequest) {
            // 流式请求：需要收集完整响应
            return executeLlmStreamRequest(agent, requestBody);
        } else {
            // 非流式请求：直接返回
            return executeLlmNonStreamRequest(agent, requestBody);
        }
    }

    // /**
    //  * 执行非流式LLM请求
    //  *
    //  * @param agent Agent信息
    //  * @param requestBody LLM请求体
    //  * @return LLM响应
    //  */
    // @SuppressWarnings("unchecked")
    private Map<String, Object> executeLlmNonStreamRequest(AgentDO agent, Map<String, Object> requestBody) {
        // 确保stream=false
        requestBody.put("stream", false);

        // 解密 LLM API Key
        String decryptedApiKey = encryptionUtil.decrypt(agent.getLlmApiKey());

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + decryptedApiKey);
        headers.set("Content-Type", "application/json");

        // 构建请求体
        String requestBodyJson = JSONUtil.toJsonStr(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);

        // 发起请求
        String llmUrl = agent.getLlmBaseUrl() + "/chat/completions";
        log.info("执行审批后的LLM请求: url={}, model={}", llmUrl, requestBody.get("model"));

        ResponseEntity<String> response = restTemplate.exchange(
                llmUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        // 解析响应
        return JSONUtil.toBean(response.getBody(), Map.class);
    }

    // /**
    //  * 执行流式LLM请求并收集完整响应
    //  *
    //  * @param agent Agent信息
    //  * @param requestBody LLM请求体
    //  * @return 收集后的完整响应（OpenAI标准格式）
    //  */
    // @SuppressWarnings("unchecked")
    private Map<String, Object> executeLlmStreamRequest(AgentDO agent, Map<String, Object> requestBody) {
        log.info("执行流式LLM请求并收集完整响应");

        // 将stream设置为false，直接获取完整响应
        // 这样可以避免处理SSE流的复杂性
        requestBody.put("stream", false);

        return executeLlmNonStreamRequest(agent, requestBody);
    }

    /**
     * 获取 Agent 名称
     *
     * @param agentId Agent ID
     * @return Agent 名称
     */
    private String getAgentName(String agentId) {
        if (StrUtil.isBlank(agentId)) {
            return "未知";
        }
        AgentDO agent = agentMapper.selectById(agentId);
        return ObjectUtil.isNotNull(agent) ? agent.getName() : agentId;
    }

    /**
     * 发送告警
     *
     * @param title 告警标题
     * @param content 告警内容
     */
    private void sendAlert(String title, String content) {
        try {
            AlertDTO alert = new AlertDTO();
            alert.setType(AlertType.APPROVAL);
            alert.setTitle(title);
            alert.setContent(content);
            alert.setRecipient(defaultRecipient);
            alert.setChannelType(NotificationChannelType.EMAIL);

            alertService.sendAlert(alert);
        } catch (Exception e) {
            log.error("发送告警失败: title={}, error={}", title, e.getMessage(), e);
        }
    }
}
