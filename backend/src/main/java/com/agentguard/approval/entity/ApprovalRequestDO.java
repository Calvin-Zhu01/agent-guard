package com.agentguard.approval.entity;

import com.agentguard.approval.enums.ApprovalStatus;
import com.agentguard.approval.enums.ExecutionStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批请求数据对象
 *
 * @author zhuhx
 */
@Data
@TableName("approval_request")
public class ApprovalRequestDO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 关联策略ID */
    private String policyId;

    /** 关联Agent ID */
    private String agentId;

    /** 请求数据（JSON格式） */
    private String requestData;

    /** 审批状态 */
    private ApprovalStatus status;

    /** 审批人ID */
    private String approverId;

    /** 审批时间 */
    private LocalDateTime approvedAt;

    /** 审批备注 */
    private String remark;

    /** 执行状态：NOT_EXECUTED/EXECUTING/SUCCESS/FAILED */
    private ExecutionStatus executionStatus;

    /** 执行结果（JSON格式） */
    private String executionResult;

    /** 执行时间 */
    private LocalDateTime executedAt;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
