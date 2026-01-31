package com.agentguard.approval.dto;

import com.agentguard.approval.enums.ApprovalStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批状态查询响应DTO
 * 用于客户端SDK轮询审批状态
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "审批状态查询响应")
public class ApprovalStatusDTO {

    @Schema(description = "审批状态：PENDING/APPROVED/REJECTED/EXPIRED")
    private ApprovalStatus status;

    @Schema(description = "执行结果（仅当status=APPROVED且已执行时返回）")
    private Object executionResult;

    @Schema(description = "审批备注（仅当status=REJECTED时返回拒绝原因）")
    private String remark;
}
