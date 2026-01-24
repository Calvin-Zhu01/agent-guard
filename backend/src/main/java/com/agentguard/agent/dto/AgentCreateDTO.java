package com.agentguard.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Agent 创建请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "创建Agent请求")
public class AgentCreateDTO {

    @Schema(description = "Agent名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "客服助手")
    @NotBlank(message = "Agent名称不能为空")
    @Size(max = 100, message = "Agent名称不能超过100个字符")
    private String name;

    @Schema(description = "描述信息", example = "用于处理客户咨询的智能助手")
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
