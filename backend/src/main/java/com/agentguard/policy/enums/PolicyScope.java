package com.agentguard.policy.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 策略作用域枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum PolicyScope {

    /** 全局策略 */
    GLOBAL("GLOBAL", "全局"),

    /** Agent级别策略 */
    AGENT("AGENT", "Agent级别");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
