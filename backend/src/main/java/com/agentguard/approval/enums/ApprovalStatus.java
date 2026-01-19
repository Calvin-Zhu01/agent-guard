package com.agentguard.approval.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批状态枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum ApprovalStatus {

    /** 待审批 */
    PENDING("PENDING", "待审批"),

    /** 已批准 */
    APPROVED("APPROVED", "已批准"),

    /** 已拒绝 */
    REJECTED("REJECTED", "已拒绝"),

    /** 已过期 */
    EXPIRED("EXPIRED", "已过期");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
