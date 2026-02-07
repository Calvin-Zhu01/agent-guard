package com.agentguard.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审批请求创建 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "创建审批请求")
public class ApprovalCreateDTO {

    @Schema(description = "关联策略ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "策略ID不能为空")
    private String policyId;

    @Schema(description = "关联Agent ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Agent ID不能为空")
    private String agentId;

    @Schema(description = "请求数据（JSON格式）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请求数据不能为空")
    private String requestData;
}
