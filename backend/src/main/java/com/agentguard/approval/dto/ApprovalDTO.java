package com.agentguard.approval.dto;

import com.agentguard.approval.enums.ApprovalStatus;
import com.agentguard.approval.enums.ExecutionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批请求数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "审批请求信息")
public class ApprovalDTO {

    @Schema(description = "审批请求ID")
    private String id;

    @Schema(description = "关联策略ID")
    private String policyId;

    @Schema(description = "关联策略名称")
    private String policyName;

    @Schema(description = "关联Agent ID")
    private String agentId;

    @Schema(description = "关联Agent名称")
    private String agentName;

    @Schema(description = "请求数据（JSON格式）")
    private String requestData;

    @Schema(description = "审批状态")
    private ApprovalStatus status;

    @Schema(description = "审批人ID")
    private String approverId;

    @Schema(description = "审批时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;

    @Schema(description = "审批备注")
    private String remark;

    @Schema(description = "执行状态：NOT_EXECUTED/EXECUTING/SUCCESS/FAILED")
    private ExecutionStatus executionStatus;

    @Schema(description = "执行结果（JSON格式）")
    private String executionResult;

    @Schema(description = "执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedAt;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
