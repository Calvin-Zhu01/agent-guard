package com.agentguard.proxy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * LLM 代理请求 DTO
 * 用于 /proxy/v1/chat/completions 端点
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "LLM代理请求")
public class LlmProxyRequestDTO {

    /** 请求体（直接透传给 LLM API） */
    @Schema(description = "请求体（包含model、messages等字段）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> body;
}
