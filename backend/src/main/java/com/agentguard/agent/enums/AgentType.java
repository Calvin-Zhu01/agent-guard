package com.agentguard.agent.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent类型枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum AgentType {

    /** 客服 */
    CUSTOMER_SERVICE("CUSTOMER_SERVICE", "客服"),

    /** 财务 */
    FINANCE("FINANCE", "财务"),

    /** 运营 */
    OPERATION("OPERATION", "运营"),

    /** 内部工具 */
    INTERNAL("INTERNAL", "内部工具"),

    /** 其他 */
    OTHER("OTHER", "其他");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
