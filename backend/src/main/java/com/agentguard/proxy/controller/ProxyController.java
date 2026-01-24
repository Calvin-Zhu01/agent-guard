package com.agentguard.proxy.controller;

import com.agentguard.common.response.Result;
import com.agentguard.proxy.dto.LlmProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyResponseDTO;
import com.agentguard.proxy.service.ProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Agent代理控制器
 *
 * 接收Agent的HTTP请求，进行策略评估和日志记录
 *
 * @author zhuhx
 */
@Slf4j
@Tag(name = "Agent代理", description = "Agent请求代理接口")
@RestController
@RequestMapping("/proxy/v1")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;

    @Operation(summary = "LLM对话代理", description = "代理LLM对话请求，支持密钥替换、Token统计和成本计算，支持流式和非流式响应")
    @PostMapping(value = "/chat/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object chatCompletions(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> requestBody) {

        log.debug("接收到LLM请求体: {}", requestBody);

        // 将原始请求体包装到 DTO 中
        LlmProxyRequestDTO request = new LlmProxyRequestDTO();
        request.setBody(requestBody);

        // 检查是否为流式请求
        boolean isStreaming = requestBody != null &&
                             Boolean.TRUE.equals(requestBody.get("stream"));

        if (isStreaming) {
            // 流式响应 - 直接返回 SseEmitter（Spring MVC 会自动处理）
            return proxyService.handleLlmStreamRequest(authorization, request);
        } else {
            // 非流式响应（保持原有行为）
            return ResponseEntity.ok(Result.success(proxyService.handleLlmRequest(authorization, request)));
        }
    }

    @Operation(summary = "业务API代理", description = "代理业务API请求，进行策略评估后转发或拦截")
    @PostMapping("/api")
    public Result<ProxyResponseDTO> apiProxy(@Valid @RequestBody ProxyRequestDTO request) {
        return Result.success(proxyService.handleRequest(request));
    }

    @Deprecated
    @Operation(summary = "代理请求（已废弃）", description = "请使用 /api 端点代替")
    @PostMapping("/request")
    public Result<ProxyResponseDTO> handleRequest(@Valid @RequestBody ProxyRequestDTO request) {
        return Result.success(proxyService.handleRequest(request));
    }
}
