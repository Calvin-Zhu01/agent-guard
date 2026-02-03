package com.agentguard.agent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "Agent信息")
public class AgentDTO {

    @Schema(description = "Agent ID")
    private String id;

    @Schema(description = "Agent名称")
    private String name;

    @Schema(description = "API密钥")
    private String apiKey;

    @Schema(description = "描述信息")
    private String description;

    @Schema(description = "LLM提供商：openai/anthropic/azure")
    private String llmProvider;

    @Schema(description = "真实的LLM API密钥（脱敏显示）")
    private String llmApiKey;

    @Schema(description = "LLM API地址")
    private String llmBaseUrl;

    @Schema(description = "默认模型")
    private String llmModel;

    @Schema(description = "Agent状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "最后活跃时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActiveAt;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "绑定的策略列表")
    private List<PolicySummaryDTO> policies;

    /**
     * 策略摘要信息（用于 Agent 列表展示）
     */
    @Data
    @Schema(description = "策略摘要信息")
    public static class PolicySummaryDTO {
        @Schema(description = "策略ID")
        private String id;

        @Schema(description = "策略名称")
        private String name;

        @Schema(description = "是否启用")
        private Boolean enabled;
    }
}
