package com.agentguard.proxy.service;

import com.agentguard.proxy.dto.ProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyResponseDTO;

/**
 * 代理服务接口
 *
 * @author zhuhx
 */
public interface ProxyService {

    /**
     * 处理代理请求
     *
     * @param request 代理请求
     * @return 代理响应
     */
    ProxyResponseDTO handleRequest(ProxyRequestDTO request);
}
