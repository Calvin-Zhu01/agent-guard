package com.agentguard.log.dto;

import com.agentguard.log.enums.RequestType;
import com.agentguard.log.enums.ResponseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Agent日志创建请求DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "创建Agent日志请求")
public class AgentLogCreateDTO {

    @Schema(description = "AgentID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "AgentID不能为空")
    private String agentId;

    @Schema(description = "请求类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "API_CALL")
    @NotNull(message = "请求类型不能为空")
    private RequestType requestType;

    @Schema(description = "请求地址", example = "https://api.example.com/transfer")
    private String endpoint;

    @Schema(description = "请求方法", example = "POST")
    private String method;

    @Schema(description = "请求摘要（JSON格式）")
    private String requestSummary;

    @Schema(description = "请求头（JSON格式）")
    private String requestHeaders;

    @Schema(description = "完整请求体（JSON格式）")
    private String requestBody;

    @Schema(description = "完整响应体（JSON格式）")
    private String responseBody;

    @Schema(description = "响应状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUCCESS")
    @NotNull(message = "响应状态不能为空")
    private ResponseStatus responseStatus;

    @Schema(description = "响应时间（毫秒）", example = "150")
    private Integer responseTimeMs;

    @Schema(description = "输入token数")
    private Integer tokenInput;

    @Schema(description = "输出token数")
    private Integer tokenOutput;

    @Schema(description = "LLM模型", example = "gpt-4")
    private String model;

    @Schema(description = "成本")
    private BigDecimal cost;

    @Schema(description = "策略快照（触发策略时的策略信息）")
    private PolicySnapshotDTO policySnapshot;
}
