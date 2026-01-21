package com.agentguard.proxy.service.impl;

import cn.hutool.core.collection.CollUtil;
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
import com.agentguard.proxy.service.ProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
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

    private final RestTemplate restTemplate;
    private final AgentService agentService;
    private final AgentLogService agentLogService;
    private final PolicyEngine policyEngine;
    private final ContentMaskerService contentMaskerService;
    private final ApprovalService approvalService;

    public ProxyServiceImpl(
            RestTemplate restTemplate,
            AgentService agentService,
            AgentLogService agentLogService,
            PolicyEngine policyEngine,
            ContentMaskerService contentMaskerService,
            @Lazy ApprovalService approvalService) {
        this.restTemplate = restTemplate;
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
            // 请求允许通过，转发到目标服务
            try {
                response = forwardRequest(request);
                responseStatus = ResponseStatus.SUCCESS;
                
                // 检查是否需要脱敏处理
                if (policyResult.isRequireMask() && ObjectUtil.isNotNull(policyResult.getMaskConfig())) {
                    response = applyMasking(response, policyResult);
                }
            } catch (Exception e) {
                response = handleForwardingError(e, request);
                responseStatus = ResponseStatus.FAILED;
            }
        }

        // 4. 记录日志
        long responseTimeMs = System.currentTimeMillis() - startTime;
        boolean success = (responseStatus == ResponseStatus.SUCCESS);
        recordLog(agent.getId(), request, responseStatus, responseTimeMs, policyResult, success);

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
     * 验证目标 URL
     *
     * @param url 目标URL
     * @throws BusinessException 如果URL为空或格式错误
     */
    private void validateTargetUrl(String url) {
        if (StrUtil.isBlank(url)) {
            throw new BusinessException(ErrorCode.INVALID_TARGET_URL);
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            
            // 拒绝内网地址
            if (isInternalAddress(host)) {
                throw new BusinessException(ErrorCode.INTERNAL_ADDRESS_FORBIDDEN);
            }
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.INVALID_TARGET_URL);
        }
    }

    /**
     * 检查是否为内网地址
     *
     * @param host 主机名或IP地址
     * @return 如果是内网地址返回true，否则返回false
     */
    private boolean isInternalAddress(String host) {
        if (StrUtil.isBlank(host)) {
            return false;
        }

        // 检查 localhost
        if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
            return true;
        }

        // 检查私有 IP 段
        try {
            InetAddress addr = InetAddress.getByName(host);
            return addr.isSiteLocalAddress() || addr.isLoopbackAddress();
        } catch (Exception e) {
            log.warn("Failed to resolve host: {}", host);
            return false;
        }
    }

    /**
     * 转发请求到目标服务
     *
     * @param request 代理请求
     * @return 代理响应
     * @throws BusinessException 如果URL验证失败
     */
    private ProxyResponseDTO forwardRequest(ProxyRequestDTO request) {
        // URL 验证
        validateTargetUrl(request.getTargetUrl());
        
        // 构建请求头
        HttpHeaders headers = buildHeaders(request.getHeaders());
        
        // 构建请求体（将 Map<String, Object> 转换为 JSON 字符串）
        String requestBody = null;
        if (CollUtil.isNotEmpty(request.getBody())) {
            requestBody = JSONUtil.toJsonStr(request.getBody());
        }
        
        // 创建 HttpEntity
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        // 发起请求
        ResponseEntity<String> response = restTemplate.exchange(
            request.getTargetUrl(),
            HttpMethod.valueOf(request.getMethod().toUpperCase()),
            entity,
            String.class
        );
        
        // 构建响应
        return buildSuccessResponse(response);
    }

    /**
     * 构建请求头
     *
     * @param headerMap 请求头Map
     * @return HttpHeaders对象
     */
    private HttpHeaders buildHeaders(Map<String, String> headerMap) {
        HttpHeaders headers = new HttpHeaders();
        if (CollUtil.isNotEmpty(headerMap)) {
            headerMap.forEach(headers::add);
        }
        return headers;
    }

    /**
     * 构建成功响应
     *
     * @param response RestTemplate响应
     * @return 代理响应DTO
     */
    private ProxyResponseDTO buildSuccessResponse(ResponseEntity<String> response) {
        // 解析响应体为对象
        Object responseData = null;
        if (StrUtil.isNotBlank(response.getBody())) {
            try {
                // 尝试解析为 JSON 对象
                responseData = JSONUtil.parse(response.getBody());
            } catch (Exception e) {
                // 如果不是 JSON，直接使用字符串
                responseData = response.getBody();
            }
        }
        
        return ProxyResponseDTO.builder()
                .status(com.agentguard.log.enums.ResponseStatus.SUCCESS)
                .statusCode(response.getStatusCode().value())
                .message("Request forwarded successfully")
                .response(responseData)
                .build();
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
     * @param success 是否成功
     */
    private void recordLog(String agentId, ProxyRequestDTO request,
                           ResponseStatus responseStatus, long responseTimeMs,
                           PolicyResult policyResult, boolean success) {
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
            
            // 添加脱敏后的请求头
            if (CollUtil.isNotEmpty(request.getHeaders())) {
                String maskedHeaders = maskSensitiveHeaders(request.getHeaders());
                if (StrUtil.isNotBlank(maskedHeaders)) {
                    summary.put("headers", JSONUtil.parse(maskedHeaders));
                }
            }
            
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

    /**
     * 处理转发错误
     *
     * @param e 异常对象
     * @param request 代理请求
     * @return 错误响应DTO
     */
    private ProxyResponseDTO handleForwardingError(Exception e, ProxyRequestDTO request) {
        log.error("Request forwarding failed for URL {}: {}", request.getTargetUrl(), e.getMessage());

        ProxyResponseDTO.ProxyResponseDTOBuilder responseBuilder = ProxyResponseDTO.builder()
                .status(ResponseStatus.FAILED);

        if (e instanceof ResourceAccessException) {
            // 网络错误或超时
            responseBuilder
                    .statusCode(504)
                    .message("Target service unreachable or timeout");
        } else if (e instanceof HttpClientErrorException) {
            // 4xx 错误
            HttpClientErrorException clientError = (HttpClientErrorException) e;
            Object responseData = null;
            String responseBody = clientError.getResponseBodyAsString();
            
            // 尝试解析响应体为 JSON
            if (StrUtil.isNotBlank(responseBody)) {
                try {
                    responseData = JSONUtil.parse(responseBody);
                } catch (Exception parseEx) {
                    // 如果不是 JSON，直接使用字符串
                    responseData = responseBody;
                }
            }
            
            responseBuilder
                    .statusCode(clientError.getStatusCode().value())
                    .message("Target service returned client error")
                    .response(responseData);
        } else if (e instanceof HttpServerErrorException) {
            // 5xx 错误
            HttpServerErrorException serverError = (HttpServerErrorException) e;
            Object responseData = null;
            String responseBody = serverError.getResponseBodyAsString();
            
            // 尝试解析响应体为 JSON
            if (StrUtil.isNotBlank(responseBody)) {
                try {
                    responseData = JSONUtil.parse(responseBody);
                } catch (Exception parseEx) {
                    // 如果不是 JSON，直接使用字符串
                    responseData = responseBody;
                }
            }
            
            responseBuilder
                    .statusCode(serverError.getStatusCode().value())
                    .message("Target service returned server error")
                    .response(responseData);
        } else if (e instanceof BusinessException) {
            // 业务异常（如 URL 验证失败）
            BusinessException bizError = (BusinessException) e;
            responseBuilder
                    .statusCode(400)
                    .message(bizError.getMessage());
        } else {
            // 其他未知异常
            responseBuilder
                    .statusCode(500)
                    .message("Internal proxy error");
        }

        return responseBuilder.build();
    }

    /**
     * 脱敏敏感请求头
     *
     * @param headers 请求头Map
     * @return 脱敏后的JSON字符串
     */
    private String maskSensitiveHeaders(Map<String, String> headers) {
        if (CollUtil.isEmpty(headers)) {
            return null;
        }

        // 创建敏感 header 名称列表
        List<String> sensitiveKeys = CollUtil.newArrayList(
                "authorization", "api-key", "x-api-key", "token"
        );

        // 创建副本并脱敏
        Map<String, String> masked = MapUtil.newHashMap(headers.size());
        headers.forEach((key, value) -> {
            if (sensitiveKeys.contains(key.toLowerCase())) {
                masked.put(key, "***MASKED***");
            } else {
                masked.put(key, value);
            }
        });

        // 转换为 JSON 字符串
        return JSONUtil.toJsonStr(masked);
    }
}
