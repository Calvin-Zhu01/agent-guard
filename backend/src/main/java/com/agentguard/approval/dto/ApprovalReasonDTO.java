package com.agentguard.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 审批申请理由数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "审批申请理由")
public class ApprovalReasonDTO {

    @Schema(description = "申请理由", required = true)
    @NotBlank(message = "申请理由不能为空")
    private String reason;
}
