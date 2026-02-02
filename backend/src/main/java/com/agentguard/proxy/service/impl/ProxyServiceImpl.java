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
import com.agentguard.log.dto.PolicySnapshotDTO;
import com.agentguard.log.enums.RequestType;
import com.agentguard.log.enums.ResponseStatus;
import com.agentguard.log.service.AgentLogService;
import com.agentguard.policy.dto.PolicyResult;
import com.agentguard.policy.engine.PolicyEngine;
import com.agentguard.proxy.config.ProxyProperties;
import com.agentguard.proxy.dto.LlmProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyResponseDTO;
import com.agentguard.proxy.service.ProxyService;
import com.agentguard.stats.cost.CostCalculator;
import com.agentguard.stats.dto.TokenUsageDTO;
import com.agentguard.stats.token.TokenParser;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
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
    private final WebClient webClient;
    private final AgentService agentService;
    private final AgentLogService agentLogService;
    private final PolicyEngine policyEngine;
    private final ApprovalService approvalService;
    private final ProxyProperties proxyProperties;
    private final TokenParser tokenParser;
    private final CostCalculator costCalculator;

    public ProxyServiceImpl(
            RestTemplate restTemplate,
            WebClient webClient,
            AgentService agentService,
            AgentLogService agentLogService,
            PolicyEngine policyEngine,
            @Lazy ApprovalService approvalService,
            ProxyProperties proxyProperties,
            TokenParser tokenParser,
            CostCalculator costCalculator) {
        this.restTemplate = restTemplate;
        this.webClient = webClient;
        this.agentService = agentService;
        this.agentLogService = agentLogService;
        this.policyEngine = policyEngine;
        this.approvalService = approvalService;
        this.proxyProperties = proxyProperties;
        this.tokenParser = tokenParser;
        this.costCalculator = costCalculator;
    }

    @Override
    public ProxyResponseDTO handleLlmRequest(String authorization, LlmProxyRequestDTO request) {
        long startTime = System.currentTimeMillis();

        // 1. 提取并验证 AG 密钥
        String agKey = extractBearerToken(authorization);
        AgentDTO agent = validateApiKey(agKey);

        // 2. 检查 LLM 配置是否完整
        if (StrUtil.isBlank(agent.getLlmApiKey()) || StrUtil.isBlank(agent.getLlmBaseUrl()) || StrUtil.isBlank(agent.getLlmModel())) {
            throw new BusinessException(ErrorCode.AGENT_LLM_CONFIG_INCOMPLETE);
        }

        // 3. 策略评估（针对 LLM 调用）
        String llmUrl = buildLlmUrl(agent.getLlmBaseUrl());
        PolicyResult policyResult = policyEngine.evaluate(
                llmUrl,
                "POST",
                null,
                request.getBody(),
                agent.getId(),
                null,
                RequestType.LLM_CALL
        );

        // 4. 根据策略结果处理请求
        ProxyResponseDTO response;
        ResponseStatus responseStatus;
        String approvalRequestId = null;

        if (policyResult.isBlocked()) {
            // TODO: LLM 审批功能暂时注释，后期再详细设计
            // if (policyResult.isRequireApproval()) {
            //     // 需要审批
            //     approvalRequestId = createLlmApprovalRequest(agent.getId(), policyResult, request);
            //     response = ProxyResponseDTO.pendingApproval(policyResult.getReason(), approvalRequestId);
            //     responseStatus = ResponseStatus.PENDING_APPROVAL;
            // } else {
            //     // 请求被拦截
            //     response = ProxyResponseDTO.blocked(policyResult.getReason());
            //     responseStatus = ResponseStatus.BLOCKED;
            // }

            // 暂时统一拦截，不创建审批请求
            response = ProxyResponseDTO.blocked(policyResult.getReason());
            responseStatus = ResponseStatus.BLOCKED;
        } else {
            // 请求允许通过，转发到 LLM API
            try {
                response = forwardLlmRequest(agent, request);
                responseStatus = ResponseStatus.SUCCESS;
            } catch (Exception e) {
                response = handleLlmForwardingError(e, llmUrl);
                responseStatus = ResponseStatus.FAILED;
            }
        }

        // 5. 记录日志（简化版，不记录完整对话内容）
        long responseTimeMs = System.currentTimeMillis() - startTime;
        boolean success = (responseStatus == ResponseStatus.SUCCESS);
        recordLlmLog(agent, request, response, responseStatus, responseTimeMs, policyResult, success, approvalRequestId);

        return response;
    }

    @Override
    public SseEmitter handleLlmStreamRequest(String authorization, LlmProxyRequestDTO request) {
        long startTime = System.currentTimeMillis();

        // 1. 提取并验证 AG 密钥
        String agKey = extractBearerToken(authorization);
        AgentDTO agent = validateApiKey(agKey);

        // 2. 检查 LLM 配置是否完整
        if (StrUtil.isBlank(agent.getLlmApiKey()) || StrUtil.isBlank(agent.getLlmBaseUrl()) || StrUtil.isBlank(agent.getLlmModel())) {
            throw new BusinessException(ErrorCode.AGENT_LLM_CONFIG_INCOMPLETE);
        }

        // 3. 策略评估（针对 LLM 调用）
        String llmUrl = buildLlmUrl(agent.getLlmBaseUrl());
        PolicyResult policyResult = policyEngine.evaluate(
                llmUrl,
                "POST",
                null,
                request.getBody(),
                agent.getId(),
                null,
                RequestType.LLM_CALL
        );

        // 4. 如果被策略拦截，返回标准的 OpenAI chunk 格式（作为正常回复）
        if (policyResult.isBlocked()) {
            SseEmitter emitter = new SseEmitter();
            String approvalRequestId = null; // 声明在外部作用域
            try {
                // 使用Agent配置的模型（而不是客户端传入的模型）
                String model = agent.getLlmModel();

                // TODO: LLM 审批功能暂时注释，后期再详细设计
                // if (policyResult.isRequireApproval()) {
                //     // 需要审批：创建审批请求并返回 tool_calls
                //     approvalRequestId = createLlmApprovalRequest(agent.getId(), policyResult, request);
                //     String errorMessage = "请求需要审批: " + policyResult.getReason();
                //
                //     // 第一个 chunk：返回消息内容
                //     Map<String, Object> contentDelta = MapUtil.builder(new LinkedHashMap<String, Object>())
                //             .put("role", "assistant")
                //             .put("content", String.format(
                //                     "[AgentGuard 审批] 您的请求需要人工审批。\n原因：%s\n审批ID：%s\n正在等待审批...",
                //                     policyResult.getReason(),
                //                     approvalRequestId))
                //             .build();
                //
                //     Map<String, Object> contentChoice = MapUtil.builder(new LinkedHashMap<String, Object>())
                //             .put("index", 0)
                //             .put("delta", contentDelta)
                //             .build();
                //
                //     Map<String, Object> contentChunk = MapUtil.builder(new LinkedHashMap<String, Object>())
                //             .put("id", "chatcmpl-ag-approval-" + java.util.UUID.randomUUID().toString())
                //             .put("object", "chat.completion.chunk")
                //             .put("created", System.currentTimeMillis() / 1000)
                //             .put("model", model)
                //             .put("choices", CollUtil.newArrayList(contentChoice))
                //             .build();
                //
                //     emitter.send(SseEmitter.event().data(JSONUtil.toJsonStr(contentChunk)));
                //
                //     // 第二个 chunk：返回 tool_calls（要求客户端调用审批状态查询）
                //     Map<String, Object> toolCallFunction = MapUtil.builder(new LinkedHashMap<String, Object>())
                //             .put("name", "check_agentguard_approval_status")
                //             .put("arguments", JSONUtil.toJsonStr(MapUtil.builder(new LinkedHashMap<String, Object>())
                //                     .put("approval_id", approvalRequestId)
                //                     .put("poll_interval_seconds", 10)
                //                     .put("max_wait_seconds", 300)
                //                     .build()))
                //             .build();
                //
                //     Map<String, Object> toolCall = MapUtil.builder(new LinkedHashMap<String, Object>())
                //             .put("id", "call_" + java.util.UUID.randomUUID().toString().replace("-", ""))
                //             .put("type", "function")
                //             .put("function", toolCallFunction)
                //             .build();
                //
                //     Map<String, Object> toolDelta = MapUtil.builder(new LinkedHashMap<String, Object>())
                //             .put("tool_calls", CollUtil.newArrayList(toolCall))
                //             .build();
                //
                //     Map<String, Object> toolChoice = MapUtil.builder(new LinkedHashMap<String, Object>())
                //             .put("index", 0)
                //             .put("delta", toolDelta)
                //             .put("finish_reason", "tool_calls")
                //             .build();
                //
                //     Map<String, Object> toolChunk = MapUtil.builder(new LinkedHashMap<String, Object>())
                //             .put("id", "chatcmpl-ag-approval-" + java.util.UUID.randomUUID().toString())
                //             .put("object", "chat.completion.chunk")
                //             .put("created", System.currentTimeMillis() / 1000)
                //             .put("model", model)
                //             .put("choices", CollUtil.newArrayList(toolChoice))
                //             .build();
                //
                //     emitter.send(SseEmitter.event().data(JSONUtil.toJsonStr(toolChunk)));
                //
                //     // 发送 [DONE]
                //     emitter.send(SseEmitter.event().data("[DONE]"));
                //
                //     // 记录日志
                //     long responseTimeMs = System.currentTimeMillis() - startTime;
                //     recordLlmLog(agent, request,
                //             ProxyResponseDTO.builder()
                //                     .status(ResponseStatus.PENDING_APPROVAL)
                //                     .message(errorMessage)
                //                     .build(),
                //             ResponseStatus.PENDING_APPROVAL,
                //             responseTimeMs,
                //             policyResult,
                //             false,
                //             approvalRequestId);
                //
                // } else {
                //     // 直接拦截（不需要审批）
                //     String errorMessage = "请求被拦截: " + policyResult.getReason();

                // 暂时统一拦截，不创建审批请求
                String errorMessage = "请求被拦截: " + policyResult.getReason();

                Map<String, Object> deltaMap = MapUtil.builder(new LinkedHashMap<String, Object>())
                        .put("role", "assistant")
                        .put("content", "[AgentGuard 拦截] " + errorMessage)
                        .build();

                Map<String, Object> choiceMap = MapUtil.builder(new LinkedHashMap<String, Object>())
                        .put("index", 0)
                        .put("delta", deltaMap)
                        .put("finish_reason", "content_filter")
                        .build();

                Map<String, Object> chunkResponse = MapUtil.builder(new LinkedHashMap<String, Object>())
                        .put("id", "chatcmpl-ag-error-" + java.util.UUID.randomUUID().toString())
                        .put("object", "chat.completion.chunk")
                        .put("created", System.currentTimeMillis() / 1000)
                        .put("model", model)
                        .put("choices", CollUtil.newArrayList(choiceMap))
                        .build();

                emitter.send(SseEmitter.event().data(JSONUtil.toJsonStr(chunkResponse)));
                emitter.send(SseEmitter.event().data("[DONE]"));

                // 记录日志
                long responseTimeMs = System.currentTimeMillis() - startTime;
                recordLlmLog(agent, request,
                        ProxyResponseDTO.builder()
                                .status(ResponseStatus.BLOCKED)
                                .message(errorMessage)
                                .build(),
                        ResponseStatus.BLOCKED,
                        responseTimeMs,
                        policyResult,
                        false,
                        null);
                // } // 注释掉的 else 结束

                emitter.complete();
            } catch (IOException e) {
                log.error("发送策略拦截消息失败", e);
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 5. 创建 SseEmitter 并转发流式请求到 LLM API
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 用于收集流式响应中的 token 统计信息
        final StringBuilder lastChunkBuilder = new StringBuilder();
        final StringBuilder finishReasonChunkBuilder = new StringBuilder(); // 用于保存包含finish_reason的chunk
        final String logId = java.util.UUID.randomUUID().toString();
        final long[] firstTokenTime = {0L}; // 记录首token时间
        final String[] finishReason = {null}; // 记录完成原因
        final List<Map<String, Object>> accumulatedToolCalls = new java.util.ArrayList<>(); // 累积的tool_calls

        // 修改请求体，添加 stream_options 以获取 token 使用统计
        Map<String, Object> modifiedBody = new LinkedHashMap<>(request.getBody());

        // 使用 Agent 配置的模型
        if (StrUtil.isNotBlank(agent.getLlmModel())) {
            modifiedBody.put("model", agent.getLlmModel());
            log.debug("使用Agent配置的模型: {}", agent.getLlmModel());
        }

        // 添加 stream_options
        if (!modifiedBody.containsKey("stream_options")) {
            Map<String, Object> streamOptions = MapUtil.builder(new LinkedHashMap<String, Object>())
                    .put("include_usage", true)
                    .build();
            modifiedBody.put("stream_options", streamOptions);
            log.debug("已添加stream_options到请求体以追踪Token使用量");
        }

        // 使用 WebClient 订阅流式响应
        webClient.post()
                .uri(llmUrl)
                .header("Authorization", "Bearer " + agent.getLlmApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(modifiedBody)
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                        chunk -> {
                            // 转发每个 SSE 数据块
                            try {
                                // 记录首token时间
                                if (firstTokenTime[0] == 0L) {
                                    firstTokenTime[0] = System.currentTimeMillis();
                                    log.debug("首个Token接收时间: {}ms", firstTokenTime[0] - startTime);
                                }

                                log.debug("接收到流式数据块: {}", chunk);
                                emitter.send(SseEmitter.event().data(chunk));

                                // 检查 chunk 是否包含 tool_calls（在 delta 中）
                                if (chunk.contains("\"tool_calls\"")) {
                                    try {
                                        // 去掉 "data: " 前缀（如果有）
                                        String jsonStr = chunk;
                                        if (jsonStr.startsWith("data: ")) {
                                            jsonStr = jsonStr.substring(6).trim();
                                        }

                                        // 确保是有效的 JSON
                                        if (jsonStr.startsWith("{")) {
                                            Map<String, Object> chunkMap = JSONUtil.toBean(jsonStr, Map.class);
                                            if (chunkMap.containsKey("choices")) {
                                                List<?> choices = (List<?>) chunkMap.get("choices");
                                                if (CollUtil.isNotEmpty(choices) && choices.get(0) instanceof Map) {
                                                    Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);

                                                    // 从 delta 中提取 tool_calls
                                                    if (firstChoice.containsKey("delta")) {
                                                        Map<String, Object> delta = (Map<String, Object>) firstChoice.get("delta");
                                                        if (delta.containsKey("tool_calls")) {
                                                            Object toolCallsObj = delta.get("tool_calls");
                                                            if (toolCallsObj instanceof List) {
                                                                List<?> toolCalls = (List<?>) toolCallsObj;
                                                                for (Object tc : toolCalls) {
                                                                    if (tc instanceof Map) {
                                                                        accumulatedToolCalls.add((Map<String, Object>) tc);
                                                                        log.info("累积tool_call: {}", JSONUtil.toJsonStr(tc));
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("从数据块解析tool_calls失败: {}", e.getMessage());
                                    }
                                }

                                // 检查 chunk 是否包含 finish_reason（在 choices 数组中）
                                if (chunk.contains("\"finish_reason\"")) {
                                    try {
                                        // 去掉 "data: " 前缀（如果有）
                                        String jsonStr = chunk;
                                        if (jsonStr.startsWith("data: ")) {
                                            jsonStr = jsonStr.substring(6).trim();
                                        }

                                        // 确保是有效的 JSON
                                        if (jsonStr.startsWith("{")) {
                                            Map<String, Object> chunkMap = JSONUtil.toBean(jsonStr, Map.class);
                                            if (chunkMap.containsKey("choices")) {
                                                List<?> choices = (List<?>) chunkMap.get("choices");
                                                if (CollUtil.isNotEmpty(choices) && choices.get(0) instanceof Map) {
                                                    Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
                                                    if (firstChoice.containsKey("finish_reason")) {
                                                        Object finishReasonObj = firstChoice.get("finish_reason");
                                                        // 只有当finish_reason不是null时才记录
                                                        if (finishReasonObj != null && !"null".equals(finishReasonObj.toString())) {
                                                            finishReason[0] = finishReasonObj.toString();
                                                            // 保存包含finish_reason的chunk，用于后续日志记录
                                                            finishReasonChunkBuilder.setLength(0);
                                                            finishReasonChunkBuilder.append(chunk);
                                                            log.info("从流式数据块中捕获finish_reason: {}", finishReason[0]);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("从数据块解析finish_reason失败: {}", e.getMessage());
                                    }
                                }

                                // 检查 chunk 是否包含 usage 信息（OpenAI 会在最后发送包含 usage 的 chunk）
                                // 只有包含 "usage" 字段的 chunk 才保存
                                if (chunk.contains("\"usage\"")) {
                                    lastChunkBuilder.setLength(0);
                                    lastChunkBuilder.append(chunk);
                                    log.debug("找到包含usage信息的数据块");
                                }
                            } catch (IOException e) {
                                log.error("发送SSE数据块失败", e);
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            // 流式响应错误 - 返回标准的 OpenAI chunk 格式（作为正常回复）
                            log.error("Agent {} 的流式请求失败: {}", agent.getId(), error.getMessage());
                            try {
                                // 构建错误消息
                                String errorMessage = "LLM API 请求失败: " + error.getMessage();

                                // 使用Agent配置的模型
                                String model = agent.getLlmModel();

                                // 构建符合 OpenAI chat.completion.chunk 格式的响应
                                Map<String, Object> deltaMap = MapUtil.builder(new LinkedHashMap<String, Object>())
                                        .put("role", "assistant")
                                        .put("content", "[AgentGuard 错误] " + errorMessage)
                                        .build();

                                Map<String, Object> choiceMap = MapUtil.builder(new LinkedHashMap<String, Object>())
                                        .put("index", 0)
                                        .put("delta", deltaMap)
                                        .put("finish_reason", "stop")
                                        .build();

                                Map<String, Object> chunkResponse = MapUtil.builder(new LinkedHashMap<String, Object>())
                                        .put("id", "chatcmpl-ag-error-" + java.util.UUID.randomUUID().toString())
                                        .put("object", "chat.completion.chunk")
                                        .put("created", System.currentTimeMillis() / 1000)
                                        .put("model", model)
                                        .put("choices", CollUtil.newArrayList(choiceMap))
                                        .build();

                                // 发送标准的 OpenAI chunk 格式
                                emitter.send(SseEmitter.event().data(JSONUtil.toJsonStr(chunkResponse)));

                                // 发送 [DONE] 标记结束
                                emitter.send(SseEmitter.event().data("[DONE]"));

                                // 记录失败日志
                                long responseTimeMs = System.currentTimeMillis() - startTime;
                                recordLlmLog(agent, request,
                                        ProxyResponseDTO.builder()
                                                .status(ResponseStatus.FAILED)
                                                .message(errorMessage)
                                                .build(),
                                        ResponseStatus.FAILED,
                                        responseTimeMs,
                                        policyResult,
                                        false,
                                        null);

                                // 正常完成（而不是 completeWithError）
                                emitter.complete();
                            } catch (IOException ioEx) {
                                log.error("发送错误消息失败", ioEx);
                                emitter.completeWithError(ioEx);
                            }
                        },
                        () -> {
                            // 流式响应完成
                            long responseTimeMs = System.currentTimeMillis() - startTime;
                            log.info("Agent {} 的流式请求完成，耗时 {}ms", agent.getId(), responseTimeMs);

                            // 记录日志（包含 token 统计）
                            try {
                                AgentLogCreateDTO logDto = new AgentLogCreateDTO();
                                logDto.setAgentId(agent.getId());
                                logDto.setRequestType(RequestType.LLM_CALL);
                                logDto.setEndpoint(llmUrl);
                                logDto.setMethod("POST");
                                logDto.setRequestSummary(createLlmRequestSummary(request));
                                logDto.setResponseStatus(ResponseStatus.SUCCESS);
                                logDto.setResponseTimeMs((int) responseTimeMs);

                                // 记录首token时间
                                if (firstTokenTime[0] > 0) {
                                    int firstTokenTimeMs = (int) (firstTokenTime[0] - startTime);
                                    logDto.setFirstTokenTimeMs(firstTokenTimeMs);
                                    log.debug("首Token时间: {}ms", firstTokenTimeMs);
                                }

                                // 使用Agent配置的模型
                                String model = null;
                                if (StrUtil.isNotBlank(agent.getLlmModel())) {
                                    model = agent.getLlmModel();
                                    logDto.setModel(model);
                                }

                                // 记录finish_reason（如果已经捕获到）
                                if (finishReason[0] != null) {
                                    logDto.setFinishReason(finishReason[0]);
                                    log.info("记录finish_reason到日志: {}", finishReason[0]);

                                    // 如果是 tool_calls，从累积的 tool_calls 中提取工具名称
                                    if ("tool_calls".equals(finishReason[0]) && CollUtil.isNotEmpty(accumulatedToolCalls)) {
                                        try {
                                            log.info("累积的tool_calls数量: {}", accumulatedToolCalls.size());
                                            String toolNames = parseToolCallsArray(accumulatedToolCalls);
                                            if (StrUtil.isNotBlank(toolNames)) {
                                                log.info("从累积的tool_calls提取的工具名称: {}", toolNames);
                                                logDto.setToolCalls(toolNames);
                                            } else {
                                                log.warn("从累积的tool_calls中未能提取到工具名称");
                                            }
                                        } catch (Exception e) {
                                            log.error("从累积的tool_calls提取工具名称失败: {}", e.getMessage(), e);
                                        }
                                    }
                                } else {
                                    log.warn("流式响应完成，但未捕获到finish_reason");
                                }

                                // 尝试从最后的 chunk 中解析 token 统计
                                String lastChunk = lastChunkBuilder.toString();
                                if (StrUtil.isNotBlank(lastChunk) && lastChunk.contains("\"usage\"")) {
                                    try {
                                        log.debug("尝试从包含usage字段的数据块解析Token使用量: {}", lastChunk);

                                        // 如果 chunk 以 "data: " 开头，去掉前缀
                                        String jsonStr = lastChunk;
                                        if (jsonStr.startsWith("data: ")) {
                                            jsonStr = jsonStr.substring(6).trim();
                                        }

                                        // 确保是有效的 JSON
                                        if (jsonStr.startsWith("{")) {
                                            String provider = StrUtil.isNotBlank(agent.getLlmProvider()) ? agent.getLlmProvider() : "openai";
                                            TokenUsageDTO tokenUsage = tokenParser.parseFromResponse(jsonStr, provider);

                                            if (tokenUsage != null) {
                                                log.info("成功从流式响应解析Token使用量: input={}, output={}, total={}",
                                                        tokenUsage.getInputTokens(), tokenUsage.getOutputTokens(), tokenUsage.getTotalTokens());

                                                logDto.setTokenInput(tokenUsage.getInputTokens());
                                                logDto.setTokenOutput(tokenUsage.getOutputTokens());

                                                // 计算成本
                                                if (model != null) {
                                                    BigDecimal cost = costCalculator.calculateCost(
                                                            provider,
                                                            model,
                                                            tokenUsage.getInputTokens(),
                                                            tokenUsage.getOutputTokens()
                                                    );
                                                    logDto.setCost(cost);
                                                    log.info("流式请求成本计算完成: {}", cost);
                                                }
                                            } else {
                                                log.warn("无法从流式响应解析Token使用量");
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("从流式响应解析Token使用量失败: {}", e.getMessage());
                                    }
                                } else {
                                    log.warn("流式响应中未找到包含usage信息的数据块。请确保启用了stream_options.include_usage");
                                }

                                agentLogService.create(logDto);
                            } catch (Exception e) {
                                log.error("记录流式日志失败", e);
                            }

                            emitter.complete();
                        }
                );

        return emitter;
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
                agent.getId(),
                null,
                RequestType.API_CALL
        );

        // 3. 根据策略结果处理请求
        ProxyResponseDTO response;
        ResponseStatus responseStatus;
        String approvalRequestId = null;

        if (policyResult.isBlocked()) {
            if (policyResult.isRequireApproval()) {
                // 需要审批，创建审批请求
                approvalRequestId = createApprovalRequest(agent.getId(), policyResult, request);
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
            } catch (Exception e) {
                response = handleForwardingError(e, request);
                responseStatus = ResponseStatus.FAILED;
            }
        }

        // 4. 记录日志
        long responseTimeMs = System.currentTimeMillis() - startTime;
        boolean success = (responseStatus == ResponseStatus.SUCCESS);
        recordLog(agent.getId(), request, response, responseStatus, responseTimeMs, policyResult, success, approvalRequestId);

        return response;
    }

    /**
     * 验证 API Key 有效性（用于代理服务，返回未脱敏的真实密钥）
     *
     * @param apiKey API密钥
     * @return Agent信息（包含真实的LLM API Key）
     * @throws BusinessException 如果API Key无效
     */
    private AgentDTO validateApiKey(String apiKey) {
        return Optional.ofNullable(agentService.getByApiKeyForProxy(apiKey))
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
            // 拒绝内网地址（除非配置允许）
            if (!proxyProperties.isAllowInternalAddress() && isInternalAddress(host)) {
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
            log.warn("解析主机失败: {}", host);
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
                .message("请求转发成功")
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
        log.info("为Agent {} 创建审批请求，策略: {}", agentId, policyResult.getPolicyId());

        // 构建请求数据 JSON（只保存原始请求信息，不包含 reason）
        Map<String, Object> requestData = MapUtil.builder(new LinkedHashMap<String, Object>())
                .put("type", "api_call")
                .put("targetUrl", request.getTargetUrl())
                .put("method", request.getMethod())
                .put("headers", request.getHeaders())
                .put("body", request.getBody())
                .build();

        ApprovalCreateDTO createDTO = new ApprovalCreateDTO();
        createDTO.setPolicyId(policyResult.getPolicyId());
        createDTO.setAgentId(agentId);
        createDTO.setRequestData(JSONUtil.toJsonStr(requestData));
        createDTO.setExpireMinutes(60);

        ApprovalDTO approval = approvalService.create(createDTO);
        log.info("审批请求创建成功，ID: {}", approval.getId());
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
     * 记录请求日志
     *
     * @param agentId Agent ID
     * @param request 代理请求
     * @param response 代理响应
     * @param responseStatus 响应状态
     * @param responseTimeMs 响应时间（毫秒）
     * @param policyResult 策略评估结果
     * @param success 是否成功
     * @param approvalRequestId 审批请求ID（可选）
     */
    private void recordLog(String agentId, ProxyRequestDTO request, ProxyResponseDTO response,
                           ResponseStatus responseStatus, long responseTimeMs,
                           PolicyResult policyResult, boolean success, String approvalRequestId) {
        try {
            AgentLogCreateDTO logDto = new AgentLogCreateDTO();
            logDto.setAgentId(agentId);
            logDto.setRequestType(RequestType.API_CALL);
            logDto.setEndpoint(request.getTargetUrl());
            logDto.setMethod(request.getMethod());
            logDto.setRequestSummary(createRequestSummary(request, policyResult));
            logDto.setResponseStatus(responseStatus);
            logDto.setResponseTimeMs((int) responseTimeMs);

            // 如果是待审批状态，保存审批请求ID
            log.info("记录API日志 - responseStatus: {}, approvalRequestId: {}", responseStatus, approvalRequestId);
            if (responseStatus == ResponseStatus.PENDING_APPROVAL && StrUtil.isNotBlank(approvalRequestId)) {
                logDto.setApprovalRequestId(approvalRequestId);
                log.info("已设置审批请求ID到API日志DTO: {}", approvalRequestId);
            }

            // 记录请求头
            if (CollUtil.isNotEmpty(request.getHeaders())) {
                logDto.setRequestHeaders(JSONUtil.toJsonStr(request.getHeaders()));
            }

            // 记录完整请求体
            if (CollUtil.isNotEmpty(request.getBody())) {
                logDto.setRequestBody(JSONUtil.toJsonStr(request.getBody()));
            }

            // 记录完整响应体
            if (response != null && response.getResponse() != null) {
                logDto.setResponseBody(JSONUtil.toJsonStr(response.getResponse()));
            }

            // 记录策略快照
            if (policyResult != null && policyResult.getPolicyId() != null) {
                PolicySnapshotDTO policySnapshot = PolicySnapshotDTO.builder()
                        .id(policyResult.getPolicyId())
                        .name(policyResult.getPolicyName())
                        .type(policyResult.getPolicyType() != null ? policyResult.getPolicyType().getCode() : null)
                        .action(policyResult.getAction() != null ? policyResult.getAction().getCode() : null)
                        .conditions(policyResult.getPolicyConditions())
                        .reason(policyResult.getReason())
                        .build();
                logDto.setPolicySnapshot(policySnapshot);
            }

            agentLogService.create(logDto);
        } catch (Exception e) {
            log.error("记录Agent日志失败", e);
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
            return JSONUtil.toJsonStr(summary);
        } catch (Exception e) {
            log.warn("创建请求摘要失败", e);
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
        log.error("URL {} 的请求转发失败: {}", request.getTargetUrl(), e.getMessage());

        ProxyResponseDTO.ProxyResponseDTOBuilder responseBuilder = ProxyResponseDTO.builder()
                .status(ResponseStatus.FAILED);

        if (e instanceof ResourceAccessException) {
            // 网络错误或超时
            responseBuilder
                    .statusCode(504)
                    .message("目标服务不可达或超时");
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
                    .message("目标服务返回客户端错误")
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
                    .message("目标服务返回服务器错误")
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
                    .message("内部代理错误");
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

    /**
     * 提取 Bearer Token
     *
     * @param authorization Authorization header
     * @return token
     */
    private String extractBearerToken(String authorization) {
        if (StrUtil.isBlank(authorization)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authorization.substring(7);
    }

    /**
     * 转发 LLM 请求
     *
     * @param agent Agent信息
     * @param request LLM请求
     * @return 代理响应
     */
    private ProxyResponseDTO forwardLlmRequest(AgentDTO agent, LlmProxyRequestDTO request) {
        // 构建请求头，替换为真实的 LLM API Key
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + agent.getLlmApiKey());
        headers.set("Content-Type", "application/json");

        // 使用 Agent 配置的模型覆盖请求体中的模型
        Map<String, Object> modifiedBody = new LinkedHashMap<>(request.getBody());
        if (StrUtil.isNotBlank(agent.getLlmModel())) {
            modifiedBody.put("model", agent.getLlmModel());
            log.debug("使用Agent配置的模型: {}", agent.getLlmModel());
        }

        // 构建请求体
        String requestBody = JSONUtil.toJsonStr(modifiedBody);
        log.debug("转发LLM请求体: {}", requestBody);
        log.debug("请求体长度: {}", requestBody != null ? requestBody.length() : 0);

        // 创建 HttpEntity
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 发起请求
        String llmUrl = buildLlmUrl(agent.getLlmBaseUrl());
        log.debug("转发到LLM URL: {}", llmUrl);
        ResponseEntity<String> response = restTemplate.exchange(
                llmUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        // ===== 临时添加：输出完整响应体用于调试 =====
        log.info("===== LLM API 响应体（用于调试）=====");
        log.info("{}", response.getBody());
        log.info("===== 响应体结束 =====");

        // 构建响应
        return buildSuccessResponse(response);
    }

    /**
     * 处理 LLM 转发错误
     *
     * @param e 异常对象
     * @param llmUrl LLM URL
     * @return 错误响应DTO
     */
    private ProxyResponseDTO handleLlmForwardingError(Exception e, String llmUrl) {
        log.error("URL {} 的LLM请求转发失败: {}", llmUrl, e.getMessage());
        return handleForwardingError(e, new ProxyRequestDTO());
    }

    /**
     * 创建 LLM 审批请求
     *
     * @param agentId Agent ID
     * @param policyResult 策略结果
     * @param request LLM请求
     * @return 审批请求ID
     */
    private String createLlmApprovalRequest(String agentId, PolicyResult policyResult, LlmProxyRequestDTO request) {
        log.info("为Agent {} 创建LLM审批请求，策略: {}", agentId, policyResult.getPolicyId());

        // 构建请求数据 JSON（只保存原始请求信息，不包含 reason）
        Map<String, Object> requestData = MapUtil.builder(new LinkedHashMap<String, Object>())
                .put("type", "llm_call")
                .put("body", request.getBody())
                .build();

        ApprovalCreateDTO createDTO = new ApprovalCreateDTO();
        createDTO.setPolicyId(policyResult.getPolicyId());
        createDTO.setAgentId(agentId);
        createDTO.setRequestData(JSONUtil.toJsonStr(requestData));
        createDTO.setExpireMinutes(60);

        ApprovalDTO approval = approvalService.create(createDTO);
        log.info("审批请求创建成功，ID: {}", approval.getId());
        return approval.getId();
    }

    /**
     * 记录 LLM 请求日志（简化版）
     *
     * @param agent Agent信息
     * @param request LLM请求
     * @param response 代理响应
     * @param responseStatus 响应状态
     * @param responseTimeMs 响应时间（毫秒）
     * @param policyResult 策略评估结果
     * @param success 是否成功
     * @param approvalRequestId 审批请求ID（可选）
     */
    private void recordLlmLog(AgentDTO agent, LlmProxyRequestDTO request, ProxyResponseDTO response,
                              ResponseStatus responseStatus, long responseTimeMs,
                              PolicyResult policyResult, boolean success, String approvalRequestId) {
        try {
            // 构建真实的 LLM URL
            String llmUrl = buildLlmUrl(agent.getLlmBaseUrl());

            AgentLogCreateDTO logDto = new AgentLogCreateDTO();
            logDto.setAgentId(agent.getId());
            logDto.setRequestType(RequestType.LLM_CALL);
            logDto.setEndpoint(llmUrl);
            logDto.setMethod("POST");
            logDto.setRequestSummary(createLlmRequestSummary(request));
            logDto.setResponseStatus(responseStatus);
            logDto.setResponseTimeMs((int) responseTimeMs);

            // 如果是待审批状态，保存审批请求ID
            log.info("记录日志 - responseStatus: {}, approvalRequestId: {}", responseStatus, approvalRequestId);
            if (responseStatus == ResponseStatus.PENDING_APPROVAL && StrUtil.isNotBlank(approvalRequestId)) {
                logDto.setApprovalRequestId(approvalRequestId);
                log.info("已设置审批请求ID到日志DTO: {}", approvalRequestId);
            }

            // 使用Agent配置的模型
            String model = null;
            if (StrUtil.isNotBlank(agent.getLlmModel())) {
                model = agent.getLlmModel();
                logDto.setModel(model);
            }

            // Token 解析和成本计算（仅在成功时）
            if (success && response != null && response.getResponse() != null) {
                try {
                    String responseBody = JSONUtil.toJsonStr(response.getResponse());
                    log.debug("从响应体解析Token使用量: {}", responseBody);

                    // 如果 llmProvider 为空，使用默认值 "openai"
                    String provider = StrUtil.isNotBlank(agent.getLlmProvider()) ? agent.getLlmProvider() : "openai";
                    log.debug("使用提供商 {} 进行Token解析", provider);

                    TokenUsageDTO tokenUsage = tokenParser.parseFromResponse(responseBody, provider);

                    if (tokenUsage != null) {
                        log.debug("Token使用量解析成功: input={}, output={}, total={}",
                                tokenUsage.getInputTokens(), tokenUsage.getOutputTokens(), tokenUsage.getTotalTokens());

                        logDto.setTokenInput(tokenUsage.getInputTokens());
                        logDto.setTokenOutput(tokenUsage.getOutputTokens());

                        // 计算成本
                        if (model != null) {
                            BigDecimal cost = costCalculator.calculateCost(
                                    provider,
                                    model,
                                    tokenUsage.getInputTokens(),
                                    tokenUsage.getOutputTokens()
                            );
                            log.debug("成本计算完成: {}", cost);
                            logDto.setCost(cost);
                        } else {
                            log.warn("模型为null，无法计算成本");
                        }
                    } else {
                        log.warn("从响应解析Token使用量失败");
                    }

                    // 解析 finishReason 和 tool_calls - 直接从 response.getResponse() 对象中提取
                    try {
                        Object responseObj = response.getResponse();
                        log.debug("响应对象类型: {}", responseObj.getClass().getName());

                        // 如果是 Map 类型，直接使用
                        if (responseObj instanceof Map) {
                            Map<String, Object> responseMap = (Map<String, Object>) responseObj;
                            if (responseMap.containsKey("choices")) {
                                List<?> choices = (List<?>) responseMap.get("choices");
                                if (CollUtil.isNotEmpty(choices) && choices.get(0) instanceof Map) {
                                    Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);

                                    // 提取 finish_reason
                                    if (firstChoice.containsKey("finish_reason")) {
                                        Object finishReasonObj = firstChoice.get("finish_reason");
                                        if (finishReasonObj != null) {
                                            String finishReason = finishReasonObj.toString();
                                            logDto.setFinishReason(finishReason);
                                            log.info("提取的finish_reason: {}", finishReason);

                                            // 如果是 tool_calls，提取工具名称
                                            if ("tool_calls".equals(finishReason)) {
                                                String toolNames = extractToolNames(firstChoice);
                                                if (StrUtil.isNotBlank(toolNames)) {
                                                    log.info("提取的工具名称: {}", toolNames);
                                                    // 直接设置到 toolCalls 字段
                                                    logDto.setToolCalls(toolNames);
                                                }
                                            }
                                        } else {
                                            log.warn("响应中finish_reason为null");
                                        }
                                    } else {
                                        log.warn("第一个choice中未找到finish_reason键。可用键: {}", firstChoice.keySet());
                                    }
                                } else {
                                    log.warn("choices数组为空或第一个choice不是Map");
                                }
                            } else {
                                log.warn("响应中未找到choices键。可用键: {}", responseMap.keySet());
                            }
                        } else {
                            // 如果不是 Map，尝试从 JSON 字符串解析
                            log.debug("响应不是Map，从JSON字符串解析");
                            Map<String, Object> responseMap = JSONUtil.toBean(responseBody, Map.class);
                            if (responseMap.containsKey("choices")) {
                                List<?> choices = (List<?>) responseMap.get("choices");
                                if (CollUtil.isNotEmpty(choices)) {
                                    Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
                                    if (firstChoice.containsKey("finish_reason")) {
                                        String finishReason = firstChoice.get("finish_reason").toString();
                                        logDto.setFinishReason(finishReason);
                                        log.info("从JSON提取的finish_reason: {}", finishReason);

                                        // 如果是 tool_calls，提取工具名称
                                        if ("tool_calls".equals(finishReason)) {
                                            String toolNames = extractToolNames(firstChoice);
                                            if (StrUtil.isNotBlank(toolNames)) {
                                                log.info("提取的工具名称: {}", toolNames);
                                                // 直接设置到 toolCalls 字段
                                                logDto.setToolCalls(toolNames);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("从响应解析finish_reason失败: {}", e.getMessage(), e);
                    }
                } catch (Exception e) {
                    log.error("解析Token使用量或计算成本时出错", e);
                }
            }

            // 记录策略快照
            if (policyResult != null && policyResult.getPolicyId() != null) {
                PolicySnapshotDTO policySnapshot = PolicySnapshotDTO.builder()
                        .id(policyResult.getPolicyId())
                        .name(policyResult.getPolicyName())
                        .type(policyResult.getPolicyType() != null ? policyResult.getPolicyType().getCode() : null)
                        .action(policyResult.getAction() != null ? policyResult.getAction().getCode() : null)
                        .conditions(policyResult.getPolicyConditions())
                        .reason(policyResult.getReason())
                        .build();
                logDto.setPolicySnapshot(policySnapshot);
            }

            // 注意：不记录完整的请求体和响应体（数据量太大）

            agentLogService.create(logDto);
        } catch (Exception e) {
            log.error("记录LLM日志失败", e);
        }
    }

    /**
     * 从 choice 中提取工具名称
     *
     * @param choice choice对象（Map格式）
     * @return 工具名称列表（逗号分隔），如果没有则返回null
     */
    private String extractToolNames(Map<String, Object> choice) {
        try {
            // 添加调试日志：打印 choice 的所有键
            log.info("===== 开始提取工具名称 =====");
            log.info("choice 对象的键: {}", choice.keySet());
            log.info("choice 对象内容: {}", JSONUtil.toJsonStr(choice));

            // 非流式响应：tool_calls 在 message 中
            if (choice.containsKey("message")) {
                log.info("找到 message 键");
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                log.info("message 对象的键: {}", message.keySet());
                log.info("message 对象内容: {}", JSONUtil.toJsonStr(message));

                if (message.containsKey("tool_calls")) {
                    log.info("找到 tool_calls 键");
                    Object toolCallsObj = message.get("tool_calls");
                    log.info("tool_calls 类型: {}", toolCallsObj.getClass().getName());
                    log.info("tool_calls 内容: {}", JSONUtil.toJsonStr(toolCallsObj));
                    String result = parseToolCallsArray(toolCallsObj);
                    log.info("解析结果: {}", result);
                    return result;
                } else {
                    log.warn("message 中没有 tool_calls 键");
                }
            } else {
                log.warn("choice 中没有 message 键");
            }

            // 流式响应：tool_calls 在 delta 中
            if (choice.containsKey("delta")) {
                log.info("找到 delta 键");
                Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                log.info("delta 对象的键: {}", delta.keySet());
                log.info("delta 对象内容: {}", JSONUtil.toJsonStr(delta));

                if (delta.containsKey("tool_calls")) {
                    log.info("找到 tool_calls 键");
                    Object toolCallsObj = delta.get("tool_calls");
                    log.info("tool_calls 类型: {}", toolCallsObj.getClass().getName());
                    log.info("tool_calls 内容: {}", JSONUtil.toJsonStr(toolCallsObj));
                    String result = parseToolCallsArray(toolCallsObj);
                    log.info("解析结果: {}", result);
                    return result;
                } else {
                    log.warn("delta 中没有 tool_calls 键");
                }
            } else {
                log.warn("choice 中没有 delta 键");
            }

            log.warn("未能从 choice 中提取到工具名称");
        } catch (Exception e) {
            log.error("提取工具名称失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 解析 tool_calls 数组，提取工具名称
     *
     * @param toolCallsObj tool_calls对象
     * @return 工具名称列表（逗号分隔）
     */
    private String parseToolCallsArray(Object toolCallsObj) {
        log.info("===== 开始解析 tool_calls 数组 =====");
        log.info("toolCallsObj 类型: {}", toolCallsObj != null ? toolCallsObj.getClass().getName() : "null");

        if (!(toolCallsObj instanceof List)) {
            log.warn("toolCallsObj 不是 List 类型，返回 null");
            return null;
        }

        List<?> toolCalls = (List<?>) toolCallsObj;
        log.info("tool_calls 数组大小: {}", toolCalls.size());

        if (CollUtil.isEmpty(toolCalls)) {
            log.warn("tool_calls 数组为空，返回 null");
            return null;
        }

        List<String> toolNames = new java.util.ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            Object toolCallObj = toolCalls.get(i);
            log.info("处理第 {} 个 tool_call，类型: {}", i, toolCallObj != null ? toolCallObj.getClass().getName() : "null");

            if (toolCallObj instanceof Map) {
                Map<String, Object> toolCall = (Map<String, Object>) toolCallObj;
                log.info("tool_call 的键: {}", toolCall.keySet());
                log.info("tool_call 内容: {}", JSONUtil.toJsonStr(toolCall));

                // 提取 function.name
                if (toolCall.containsKey("function")) {
                    log.info("找到 function 键");
                    Object functionObj = toolCall.get("function");
                    log.info("function 类型: {}", functionObj != null ? functionObj.getClass().getName() : "null");

                    if (functionObj instanceof Map) {
                        Map<String, Object> function = (Map<String, Object>) functionObj;
                        log.info("function 的键: {}", function.keySet());
                        log.info("function 内容: {}", JSONUtil.toJsonStr(function));

                        if (function.containsKey("name")) {
                            String toolName = function.get("name").toString();
                            log.info("提取到工具名称: {}", toolName);
                            toolNames.add(toolName);
                        } else {
                            log.warn("function 中没有 name 键");
                        }
                    } else {
                        log.warn("function 不是 Map 类型");
                    }
                } else {
                    log.warn("tool_call 中没有 function 键");
                }
            } else {
                log.warn("tool_call 不是 Map 类型");
            }
        }

        String result = CollUtil.isEmpty(toolNames) ? null : String.join(", ", toolNames);
        log.info("最终提取的工具名称: {}", result);
        return result;
    }

    /**
     * 创建 LLM 请求摘要
     * 记录请求参数的补充信息
     *
     * @param request LLM请求
     * @return JSON格式的请求摘要
     */
    private String createLlmRequestSummary(LlmProxyRequestDTO request) {
        try {
            // 打印完整的请求体内容用于调试
            if (request.getBody() != null) {
                // log.debug("LLM请求体内容: {}", JSONUtil.toJsonPrettyStr(request.getBody()));
            }

            Map<String, Object> summary = new LinkedHashMap<>();

            if (request.getBody() != null) {
                Map<String, Object> body = request.getBody();

                // 消息数量和系统提示词长度
                if (body.containsKey("messages")) {
                    Object messages = body.get("messages");
                    if (messages instanceof List) {
                        List<?> messageList = (List<?>) messages;
                        summary.put("messageCount", messageList.size());

                        // 计算系统提示词长度
                        int systemPromptLength = 0;
                        for (Object msg : messageList) {
                            if (msg instanceof Map) {
                                Map<?, ?> message = (Map<?, ?>) msg;
                                if ("system".equals(message.get("role"))) {
                                    Object content = message.get("content");
                                    if (content != null) {
                                        systemPromptLength += content.toString().length();
                                    }
                                }
                            }
                        }
                        if (systemPromptLength > 0) {
                            summary.put("systemPromptLength", systemPromptLength);
                        }
                    }
                }

                // 温度参数
                if (body.containsKey("temperature")) {
                    summary.put("temperature", body.get("temperature"));
                }

                // 最大 token 数
                if (body.containsKey("max_tokens")) {
                    summary.put("max_tokens", body.get("max_tokens"));
                }

                // top_p 参数
                if (body.containsKey("top_p")) {
                    summary.put("top_p", body.get("top_p"));
                }
            }

            return JSONUtil.toJsonStr(summary);
        } catch (Exception e) {
            log.warn("创建LLM请求摘要失败", e);
            return "{}";
        }
    }

    /**
     * 根据规则构建 LLM API URL
     * 规则：
     * 1. 如果以 # 结尾，强制使用输入的地址（去掉 #）
     * 2. 如果以 / 结尾，忽略 v1 版本，直接拼接 /chat/completions
     * 3. 否则，默认拼接 /v1/chat/completions
     *
     * @param baseUrl LLM Base URL
     * @return 完整的 LLM API URL
     */
    private String buildLlmUrl(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            throw new BusinessException(ErrorCode.AGENT_LLM_CONFIG_INCOMPLETE);
        }

        // 如果以 # 结尾，强制使用输入的地址（去掉 #）
        if (baseUrl.endsWith("#")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        // 如果以 / 结尾，忽略 v1 版本，直接拼接 /chat/completions
        if (baseUrl.endsWith("/")) {
            return baseUrl + "chat/completions";
        }

        // 否则，默认拼接 /v1/chat/completions
        return baseUrl + "/v1/chat/completions";
    }
}
