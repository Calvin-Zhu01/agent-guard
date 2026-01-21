package com.agentguard.proxy.controller;

import com.agentguard.common.response.Result;
import com.agentguard.proxy.dto.ProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyResponseDTO;
import com.agentguard.proxy.service.ProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent代理控制器
 * 
 * 接收Agent的HTTP请求，进行策略评估和日志记录
 *
 * @author zhuhx
 */
@Tag(name = "Agent代理", description = "Agent请求代理接口")
@RestController
@RequestMapping("/proxy/v1")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;

    @Operation(summary = "代理请求", description = "接收Agent请求，进行策略评估后转发或拦截。支持可选的metadata字段用于业务语义标注")
    @PostMapping("/request")
    public Result<ProxyResponseDTO> handleRequest(@Valid @RequestBody ProxyRequestDTO request) {
        return Result.success(proxyService.handleRequest(request));
    }
}
