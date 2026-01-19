package com.agentguard.proxy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Tool Schema 请求 DTO
 * 
 * Agent 调用 AgentGuard 的声明式请求规范
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tool Schema请求")
public class ToolSchemaRequestDTO {

    /** Agent API 密钥 */
    @Schema(description = "Agent API密钥", requiredMode = Schema.RequiredMode.REQUIRED, example = "ag_xxx")
    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    /** 操作类型 */
    @Schema(description = "操作类型：transfer_funds/send_email/read_document/write_database/call_api/invoke_llm", 
            requiredMode = Schema.RequiredMode.REQUIRED, example = "transfer_funds")
    @NotBlank(message = "操作类型不能为空")
    private String action;

    /** 目标资源 */
    @Schema(description = "目标资源")
    private ResourceDTO resource;

    /** 操作参数 */
    @Schema(description = "操作参数")
    private Map<String, Object> params;

    /** 业务原因 */
    @Schema(description = "业务原因", example = "用户请求转账")
    private String reason;

    /** 风险等级提示 */
    @Schema(description = "风险等级提示：low/medium/high", example = "medium")
    private String riskHint;
}
