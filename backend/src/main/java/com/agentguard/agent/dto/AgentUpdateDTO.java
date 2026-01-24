package com.agentguard.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Agent 更新请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "更新Agent请求")
public class AgentUpdateDTO {

    @Schema(description = "Agent名称", example = "客服助手V2")
    @Size(max = 100, message = "Agent名称不能超过100个字符")
    private String name;

    @Schema(description = "描述信息", example = "升级版客服助手")
    private String description;

    @Schema(description = "LLM提供商：openai/anthropic/azure", example = "openai")
    private String llmProvider;

    @Schema(description = "真实的LLM API密钥", example = "sk-xxx")
    private String llmApiKey;

    @Schema(description = "LLM API地址", example = "https://api.openai.com/v1")
    private String llmBaseUrl;

    @Schema(description = "默认模型", example = "gpt-3.5-turbo")
    private String llmModel;
}
