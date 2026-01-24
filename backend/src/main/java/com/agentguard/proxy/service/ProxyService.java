package com.agentguard.proxy.service;

import com.agentguard.proxy.dto.LlmProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyResponseDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 代理服务接口
 *
 * @author zhuhx
 */
public interface ProxyService {

    /**
     * 处理LLM对话代理请求（非流式）
     * 支持密钥替换、Token统计和成本计算
     *
     * @param authorization Authorization header (Bearer ag-xxx)
     * @param request LLM代理请求
     * @return 代理响应
     */
    ProxyResponseDTO handleLlmRequest(String authorization, LlmProxyRequestDTO request);

    /**
     * 处理LLM对话代理请求（流式）
     * 支持密钥替换、实时流式响应
     *
     * @param authorization Authorization header (Bearer ag-xxx)
     * @param request LLM代理请求
     * @return SSE流式响应
     */
    SseEmitter handleLlmStreamRequest(String authorization, LlmProxyRequestDTO request);

    /**
     * 处理业务API代理请求
     *
     * @param request 代理请求
     * @return 代理响应
     */
    ProxyResponseDTO handleRequest(ProxyRequestDTO request);
}
