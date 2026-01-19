package com.agentguard.proxy.handler.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agentguard.agent.dto.AgentDTO;
import com.agentguard.agent.service.AgentService;
import com.agentguard.approval.dto.ApprovalCreateDTO;
import com.agentguard.approval.dto.ApprovalDTO;
import com.agentguard.approval.service.ApprovalService;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.log.dto.AgentLogCreateDTO;
import com.agentguard.log.enums.RequestType;
import com.agentguard.log.enums.ResponseStatus;
import com.agentguard.log.service.AgentLogService;
import com.agentguard.policy.dto.PolicyResult;
import com.agentguard.policy.engine.PolicyEngine;
import com.agentguard.policy.service.ContentMaskerService;
import com.agentguard.proxy.dto.ProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyResponseDTO;
import com.agentguard.proxy.dto.ResourceDTO;
import com.agentguard.proxy.dto.ToolSchemaRequestDTO;
import com.agentguard.proxy.enums.ToolAction;
import com.agentguard.proxy.handler.ToolSchemaHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tool Schema 请求处理器实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolSchemaHandlerImpl implements ToolSchemaHandler {

    private final AgentService agentService;
    private final AgentLogService agentLogService;
    private final PolicyEngine policyEngine;
    private final ContentMaskerService contentMaskerService;
    private final ApprovalService approvalService;

    @Override
    public ProxyResponseDTO handleToolRequest(ToolSchemaRequestDTO request) {
        long startTime = System.currentTimeMillis();

        // 1. 验证 API Key 并获取 Agent 信息
        AgentDTO agent = validateApiKey(request.getApiKey());

        // 2. 解析 action 类型
        ToolAction toolAction = parseToolAction(request.getAction());

        // 3. 转换为代理请求格式
        ProxyRequestDTO proxyRequest = convertToProxyRequest(request);

        // 4. 策略评估
        PolicyResult policyResult = policyEngine.evaluate(
                proxyRequest.getTargetUrl(),
                proxyRequest.getMethod(),
                proxyRequest.getHeaders(),
                proxyRequest.getBody(),
                agent.getId()
        );

        // 5. 根据策略结果处理请求
        ProxyResponseDTO response;
        ResponseStatus responseStatus;

        if (policyResult.isBlocked()) {
            if (policyResult.isRequireApproval()) {
                // 需要审批
                String approvalRequestId = createApprovalRequest(agent.getId(), policyResult, request);
                response = ProxyResponseDTO.pendingApproval(policyResult.getReason(), approvalRequestId);
                responseStatus = ResponseStatus.PENDING_APPROVAL;
            } else {
                // 请求被拦截
                response = ProxyResponseDTO.blocked(policyResult.getReason());
                responseStatus = ResponseStatus.BLOCKED;
            }
        } else {
            // 请求允许通过，返回 mock 响应
            response = createMockResponse(request, toolAction);
            responseStatus = ResponseStatus.SUCCESS;

            // 检查是否需要脱敏处理
            if (policyResult.isRequireMask() && ObjectUtil.isNotNull(policyResult.getMaskConfig())) {
                response = applyMasking(response, policyResult);
            }
        }

        // 6. 记录日志（特殊处理 invoke_llm）
        long responseTimeMs = System.currentTimeMillis() - startTime;
        recordLog(agent.getId(), request, toolAction, responseStatus, responseTimeMs, policyResult);

        return response;
    }

    @Override
    public ProxyRequestDTO convertToProxyRequest(ToolSchemaRequestDTO toolRequest) {
        ToolAction toolAction = parseToolAction(toolRequest.getAction());
        
        // 构建目标 URL
        String targetUrl = buildTargetUrl(toolAction, toolRequest.getResource());
        
        // 构建请求体
        Map<String, Object> body = buildRequestBody(toolRequest);

        return ProxyRequestDTO.builder()
                .apiKey(toolRequest.getApiKey())
                .targetUrl(targetUrl)
                .method(toolAction.getDefaultMethod())
                .body(body)
                .build();
    }

    /**
     * 验证 API Key 有效性
     */
    private AgentDTO validateApiKey(String apiKey) {
        return Optional.ofNullable(agentService.getByApiKey(apiKey))
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_API_KEY_INVALID));
    }

    /**
     * 解析 Tool Action
     */
    private ToolAction parseToolAction(String action) {
        ToolAction toolAction = ToolAction.fromCode(action);
        if (ObjectUtil.isNull(toolAction)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的操作类型: " + action);
        }
        return toolAction;
    }

    /**
     * 构建目标 URL
     */
    private String buildTargetUrl(ToolAction toolAction, ResourceDTO resource) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("tool://").append(toolAction.getCode());
        
        if (ObjectUtil.isNotNull(resource)) {
            if (StrUtil.isNotBlank(resource.getType())) {
                urlBuilder.append("/").append(resource.getType());
            }
            if (StrUtil.isNotBlank(resource.getId())) {
                urlBuilder.append("/").append(resource.getId());
            }
        }
        
        return urlBuilder.toString();
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(ToolSchemaRequestDTO request) {
        Map<String, Object> body = new LinkedHashMap<>();
        
        body.put("action", request.getAction());
        
        if (ObjectUtil.isNotNull(request.getResource())) {
            body.put("resource", request.getResource());
        }
        
        if (MapUtil.isNotEmpty(request.getParams())) {
            body.put("params", request.getParams());
        }
        
        if (StrUtil.isNotBlank(request.getReason())) {
            body.put("reason", request.getReason());
        }
        
        if (StrUtil.isNotBlank(request.getRiskHint())) {
            body.put("riskHint", request.getRiskHint());
        }
        
        return body;
    }

    /**
     * 创建审批请求
     */
    private String createApprovalRequest(String agentId, PolicyResult policyResult, ToolSchemaRequestDTO request) {
        log.info("Creating approval request for agent {} with policy {}", agentId, policyResult.getPolicyId());

        Map<String, Object> requestData = MapUtil.builder(new LinkedHashMap<String, Object>())
                .put("action", request.getAction())
                .put("resource", request.getResource())
                .put("params", request.getParams())
                .put("reason", request.getReason())
                .put("riskHint", request.getRiskHint())
                .put("policyReason", policyResult.getReason())
                .build();

        ApprovalCreateDTO createDTO = new ApprovalCreateDTO();
        createDTO.setPolicyId(policyResult.getPolicyId());
        createDTO.setAgentId(agentId);
        createDTO.setRequestData(JSONUtil.toJsonStr(requestData));
        createDTO.setExpireMinutes(60);

        ApprovalDTO approval = approvalService.create(createDTO);
        return approval.getId();
    }

    /**
     * 创建 mock 响应
     */
    private ProxyResponseDTO createMockResponse(ToolSchemaRequestDTO request, ToolAction toolAction) {
        Map<String, Object> mockData = new LinkedHashMap<>();
        mockData.put("success", true);
        mockData.put("action", toolAction.getCode());
        mockData.put("message", "Mock response for " + toolAction.getDesc());
        mockData.put("timestamp", System.currentTimeMillis());

        // 针对不同 action 类型返回不同的 mock 数据
        switch (toolAction) {
            case TRANSFER_FUNDS:
                mockData.put("transactionId", "txn_" + System.currentTimeMillis());
                mockData.put("status", "completed");
                break;
            case SEND_EMAIL:
                mockData.put("messageId", "msg_" + System.currentTimeMillis());
                mockData.put("status", "sent");
                break;
            case READ_DOCUMENT:
                mockData.put("content", "Mock document content");
                mockData.put("contentType", "text/plain");
                break;
            case WRITE_DATABASE:
                mockData.put("affectedRows", 1);
                mockData.put("status", "committed");
                break;
            case CALL_API:
                mockData.put("responseCode", 200);
                mockData.put("data", MapUtil.of("result", "success"));
                break;
            case INVOKE_LLM:
                mockData.put("model", getModelFromParams(request.getParams()));
                mockData.put("response", "This is a mock LLM response.");
                mockData.put("usage", MapUtil.builder(new LinkedHashMap<String, Object>())
                        .put("promptTokens", 100)
                        .put("completionTokens", 50)
                        .put("totalTokens", 150)
                        .build());
                break;
        }

        return ProxyResponseDTO.success(mockData);
    }

    /**
     * 从参数中获取模型名称
     */
    private String getModelFromParams(Map<String, Object> params) {
        if (MapUtil.isEmpty(params)) {
            return "gpt-4";
        }
        Object model = params.get("model");
        return ObjectUtil.isNotNull(model) ? model.toString() : "gpt-4";
    }

    /**
     * 对响应进行脱敏处理
     */
    private ProxyResponseDTO applyMasking(ProxyResponseDTO response, PolicyResult policyResult) {
        try {
            Object data = response.getResponse();
            if (ObjectUtil.isNull(data)) {
                return response;
            }

            Object maskedData = contentMaskerService.maskContent(data, policyResult.getMaskConfig());

            ProxyResponseDTO maskedResponse = new ProxyResponseDTO();
            maskedResponse.setStatus(response.getStatus());
            maskedResponse.setMessage(response.getMessage());
            maskedResponse.setApprovalRequestId(response.getApprovalRequestId());
            maskedResponse.setResponse(maskedData);

            log.debug("Applied masking to response with policy {}", policyResult.getPolicyId());
            return maskedResponse;
        } catch (Exception e) {
            log.error("Failed to apply masking to response: {}", e.getMessage());
            return response;
        }
    }

    /**
     * 记录请求日志
     */
    private void recordLog(String agentId, ToolSchemaRequestDTO request, ToolAction toolAction,
                           ResponseStatus responseStatus, long responseTimeMs, PolicyResult policyResult) {
        try {
            AgentLogCreateDTO logDto = new AgentLogCreateDTO();
            logDto.setAgentId(agentId);
            logDto.setEndpoint(buildTargetUrl(toolAction, request.getResource()));
            logDto.setMethod(toolAction.getDefaultMethod());
            logDto.setRequestSummary(createRequestSummary(request, policyResult));
            logDto.setResponseStatus(responseStatus);
            logDto.setResponseTimeMs((int) responseTimeMs);

            // 特殊处理 invoke_llm：记录 LLM 调用信息
            if (toolAction == ToolAction.INVOKE_LLM) {
                logDto.setRequestType(RequestType.LLM_CALL);
                populateLlmInfo(logDto, request.getParams());
            } else {
                logDto.setRequestType(RequestType.API_CALL);
            }

            if (ObjectUtil.isNotNull(policyResult) && StrUtil.isNotBlank(policyResult.getPolicyId())) {
                logDto.setPolicyId(policyResult.getPolicyId());
            }

            agentLogService.create(logDto);
        } catch (Exception e) {
            log.error("Failed to record agent log", e);
        }
    }

    /**
     * 填充 LLM 调用信息
     */
    private void populateLlmInfo(AgentLogCreateDTO logDto, Map<String, Object> params) {
        if (MapUtil.isEmpty(params)) {
            return;
        }

        // 获取模型名称
        Object model = params.get("model");
        if (ObjectUtil.isNotNull(model)) {
            logDto.setModel(model.toString());
        }

        // 获取 token 信息（如果有）
        Object tokenInput = params.get("tokenInput");
        if (ObjectUtil.isNotNull(tokenInput)) {
            logDto.setTokenInput(Integer.parseInt(tokenInput.toString()));
        }

        Object tokenOutput = params.get("tokenOutput");
        if (ObjectUtil.isNotNull(tokenOutput)) {
            logDto.setTokenOutput(Integer.parseInt(tokenOutput.toString()));
        }

        // 计算成本（简单估算）
        Object cost = params.get("cost");
        if (ObjectUtil.isNotNull(cost)) {
            logDto.setCost(new BigDecimal(cost.toString()));
        }
    }

    /**
     * 创建请求摘要
     */
    private String createRequestSummary(ToolSchemaRequestDTO request, PolicyResult policyResult) {
        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("action", request.getAction());
            
            if (ObjectUtil.isNotNull(request.getResource())) {
                summary.put("resourceType", request.getResource().getType());
                summary.put("resourceId", request.getResource().getId());
            }
            
            if (StrUtil.isNotBlank(request.getReason())) {
                summary.put("reason", request.getReason());
            }
            
            if (StrUtil.isNotBlank(request.getRiskHint())) {
                summary.put("riskHint", request.getRiskHint());
            }
            
            if (MapUtil.isNotEmpty(request.getParams())) {
                summary.put("paramKeys", request.getParams().keySet());
            }

            if (ObjectUtil.isNotNull(policyResult) && StrUtil.isNotBlank(policyResult.getPolicyId())) {
                summary.put("policyId", policyResult.getPolicyId());
                summary.put("policyAction", policyResult.getAction() != null ? policyResult.getAction().getCode() : null);
            }

            return JSONUtil.toJsonStr(summary);
        } catch (Exception e) {
            log.warn("Failed to create request summary", e);
            return "{}";
        }
    }
}
