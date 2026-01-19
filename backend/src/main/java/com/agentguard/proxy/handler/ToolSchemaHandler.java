package com.agentguard.proxy.handler;

import com.agentguard.proxy.dto.ProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyResponseDTO;
import com.agentguard.proxy.dto.ToolSchemaRequestDTO;

/**
 * Tool Schema 请求处理器接口
 * 
 * 处理 Agent 发送的 Tool Schema 格式请求
 *
 * @author zhuhx
 */
public interface ToolSchemaHandler {

    /**
     * 处理 Tool Schema 请求
     *
     * @param request Tool Schema 格式的请求
     * @return 代理响应
     */
    ProxyResponseDTO handleToolRequest(ToolSchemaRequestDTO request);

    /**
     * 将 Tool Schema 请求转换为代理请求
     *
     * @param toolRequest Tool Schema 请求
     * @return 代理请求
     */
    ProxyRequestDTO convertToProxyRequest(ToolSchemaRequestDTO toolRequest);
}
