package com.agentguard.proxy.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
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
import com.agentguard.proxy.service.ProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 代理服务实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
public class ProxyServiceImpl implements ProxyService {

    private final AgentService agentService;
    private final AgentLogService agentLogService;
    private final PolicyEngine policyEngine;
    private final ContentMaskerService contentMaskerService;
    private final ApprovalService approvalService;

    public ProxyServiceImpl(
            AgentService agentService,
            AgentLogService agentLogService,
            PolicyEngine policyEngine,
            ContentMaskerService contentMaskerService,
            @Lazy ApprovalService approvalService) {
        this.agentService = agentService;
        this.agentLogService = agentLogService;
        this.policyEngine = policyEngine;
        this.contentMaskerService = contentMaskerService;
        this.approvalService = approvalService;
    }

    @Override
    public ProxyResponseDTO handleRequest(ProxyRequestDTO request) {
        long startTime = System.currentTimeMillis();

        // 1. 验证 API Key 并获取 Agent 信息
        AgentDTO agent = validateApiKey(request.getApiKey());

        // 2. 策略评估（传入 agentId 支持 Agent 级别策略）
        PolicyResult policyResult = policyEngine.evaluate(
                request.getTargetUrl(),
                request.getMethod(),
                request.getHeaders(),
                request.getBody(),
                agent.getId()
        );

        // 3. 根据策略结果处理请求
        ProxyResponseDTO response;
        ResponseStatus responseStatus;

        if (policyResult.isBlocked()) {
            if (policyResult.isRequireApproval()) {
                // 需要审批，创建审批请求
                String approvalRequestId = createApprovalRequest(agent.getId(), policyResult, request);
                response = ProxyResponseDTO.pendingApproval(policyResult.getReason(), approvalRequestId);
                responseStatus = ResponseStatus.PENDING_APPROVAL;
            } else {
                // 请求被拦截
                response = ProxyResponseDTO.blocked(policyResult.getReason());
                responseStatus = ResponseStatus.BLOCKED;
            }
        } else {
            // 请求允许通过，返回 mock 响应（MVP 阶段）
            response = createMockResponse(request);
            responseStatus = ResponseStatus.SUCCESS;
            
            // 检查是否需要脱敏处理
            if (policyResult.isRequireMask() && ObjectUtil.isNotNull(policyResult.getMaskConfig())) {
                response = applyMasking(response, policyResult);
            }
        }

        // 4. 记录日志
        long responseTimeMs = System.currentTimeMillis() - startTime;
        recordLog(agent.getId(), request, responseStatus, responseTimeMs, policyResult);

        return response;
    }

    /**
     * 验证 API Key 有效性
     *
     * @param apiKey API密钥
     * @return Agent信息
     * @throws BusinessException 如果API Key无效
     */
    private AgentDTO validateApiKey(String apiKey) {
        return Optional.ofNullable(agentService.getByApiKey(apiKey))
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_API_KEY_INVALID));
    }

    /**
     * 创建审批请求
     *
     * @param agentId Agent ID
     * @param policyResult 策略结果
     * @param request 代理请求
     * @return 审批请求ID
     */
    private String createApprovalRequest(String agentId, PolicyResult policyResult, ProxyRequestDTO request) {
        log.info("Creating approval request for agent {} with policy {}", agentId, policyResult.getPolicyId());
        
        // 构建请求数据 JSON
        Map<String, Object> requestData = MapUtil.builder(new LinkedHashMap<String, Object>())
                .put("targetUrl", request.getTargetUrl())
                .put("method", request.getMethod())
                .put("headers", request.getHeaders())
                .put("body", request.getBody())
                .put("reason", policyResult.getReason())
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
     * 创建 mock 响应（MVP 阶段不实际转发请求）
     *
     * @param request 代理请求
     * @return mock响应
     */
    private ProxyResponseDTO createMockResponse(ProxyRequestDTO request) {
        Map<String, Object> mockData = MapUtil.builder(new LinkedHashMap<String, Object>())
                .put("success", true)
                .put("message", "Mock response for " + request.getTargetUrl())
                .put("timestamp", System.currentTimeMillis())
                .build();
        return ProxyResponseDTO.success(mockData);
    }

    /**
     * 对响应进行脱敏处理
     *
     * @param response 原始响应
     * @param policyResult 策略结果（包含脱敏配置）
     * @return 脱敏后的响应
     */
    @SuppressWarnings("unchecked")
    private ProxyResponseDTO applyMasking(ProxyResponseDTO response, PolicyResult policyResult) {
        try {
            Object data = response.getResponse();
            if (ObjectUtil.isNull(data)) {
                return response;
            }

            // 调用 ContentMaskerService 进行脱敏
            Object maskedData = contentMaskerService.maskContent(data, policyResult.getMaskConfig());
            
            // 创建新的响应对象
            ProxyResponseDTO maskedResponse = new ProxyResponseDTO();
            maskedResponse.setStatus(response.getStatus());
            maskedResponse.setMessage(response.getMessage());
            maskedResponse.setApprovalRequestId(response.getApprovalRequestId());
            maskedResponse.setResponse(maskedData);

            log.debug("Applied masking to response with policy {}", policyResult.getPolicyId());
            return maskedResponse;
        } catch (Exception e) {
            log.error("Failed to apply masking to response: {}", e.getMessage());
            // 脱敏失败，返回原始响应
            return response;
        }
    }

    /**
     * 记录请求日志
     *
     * @param agentId Agent ID
     * @param request 代理请求
     * @param responseStatus 响应状态
     * @param responseTimeMs 响应时间（毫秒）
     * @param policyResult 策略评估结果
     */
    private void recordLog(String agentId, ProxyRequestDTO request,
                           ResponseStatus responseStatus, long responseTimeMs,
                           PolicyResult policyResult) {
        try {
            AgentLogCreateDTO logDto = new AgentLogCreateDTO();
            logDto.setAgentId(agentId);
            logDto.setRequestType(RequestType.API_CALL);
            logDto.setEndpoint(request.getTargetUrl());
            logDto.setMethod(request.getMethod());
            logDto.setRequestSummary(createRequestSummary(request, policyResult));
            logDto.setResponseStatus(responseStatus);
            logDto.setResponseTimeMs((int) responseTimeMs);

            agentLogService.create(logDto);
        } catch (Exception e) {
            log.error("Failed to record agent log", e);
        }
    }

    /**
     * 创建请求摘要
     *
     * @param request 代理请求
     * @param policyResult 策略评估结果
     * @return JSON格式的请求摘要
     */
    private String createRequestSummary(ProxyRequestDTO request, PolicyResult policyResult) {
        try {
            Map<String, Object> summary = MapUtil.builder(new LinkedHashMap<String, Object>())
                    .put("method", request.getMethod())
                    .put("url", request.getTargetUrl())
                    .build();
            if (CollUtil.isNotEmpty(request.getBody())) {
                summary.put("bodyKeys", request.getBody().keySet());
            }
            // 记录业务元数据
            if (CollUtil.isNotEmpty(request.getMetadata())) {
                summary.put("metadata", request.getMetadata());
            }
            // 添加策略信息
            if (policyResult != null && policyResult.getPolicyId() != null) {
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
