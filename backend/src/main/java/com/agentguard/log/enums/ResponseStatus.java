package com.agentguard.log.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum ResponseStatus {

    /** 成功 */
    SUCCESS("SUCCESS", "成功"),

    /** 失败 */
    FAILED("FAILED", "失败"),

    /** 被拦截 */
    BLOCKED("BLOCKED", "被拦截"),

    /** 待审批 */
    PENDING_APPROVAL("PENDING_APPROVAL", "待审批"),

    /** 审批通过 */
    APPROVED("APPROVED", "审批通过"),

    /** 审批拒绝 */
    REJECTED("REJECTED", "审批拒绝"),

    /** 已过期 */
    EXPIRED("EXPIRED", "已过期");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
