package com.agentguard.agent.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent运行环境枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum AgentEnvironment {

    /** 测试环境 */
    TEST("TEST", "测试"),

    /** 预发布环境 */
    STAGING("STAGING", "预发布"),

    /** 生产环境 */
    PRODUCTION("PRODUCTION", "生产");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
