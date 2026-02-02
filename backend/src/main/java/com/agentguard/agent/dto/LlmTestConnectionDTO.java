package com.agentguard.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * LLM 连接测试请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "LLM连接测试请求")
public class LlmTestConnectionDTO {

    @Schema(description = "Agent ID（可选，如果提供则使用该Agent的配置）", example = "agent-123")
    private String agentId;

    @Schema(description = "LLM提供商：openai/anthropic/azure", example = "openai")
    private String llmProvider;

    @Schema(description = "LLM API密钥", example = "sk-xxx")
    private String llmApiKey;

    @Schema(description = "LLM API地址", example = "https://api.openai.com/v1")
    private String llmBaseUrl;

    @Schema(description = "模型名称", example = "gpt-3.5-turbo")
    private String llmModel;
}
