package com.agentguard.policy.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 策略动作枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum PolicyAction {

    /** 允许 */
    ALLOW("ALLOW", "允许"),

    /** 拒绝 */
    DENY("DENY", "拒绝"),

    /** 审批 */
    APPROVAL("APPROVAL", "审批"),

    /** 限流 */
    RATE_LIMIT("RATE_LIMIT", "限流");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
