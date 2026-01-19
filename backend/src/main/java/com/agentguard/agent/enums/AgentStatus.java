package com.agentguard.agent.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent状态枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum AgentStatus {

    /** 禁用 */
    DISABLED(0, "禁用"),

    /** 启用 */
    ENABLED(1, "启用");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String desc;
}
