package com.agentguard.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批操作 DTO（批准/拒绝）
 *
 * @author zhuhx
 */
@Data
@Schema(description = "审批操作请求")
public class ApprovalActionDTO {

    @Schema(description = "审批人ID")
    private String approverId;

    @Schema(description = "审批备注（拒绝时可填写拒绝原因）")
    private String remark;
}
