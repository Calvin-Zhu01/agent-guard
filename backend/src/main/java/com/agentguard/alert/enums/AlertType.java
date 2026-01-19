package com.agentguard.alert.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警类型枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum AlertType {

    /** 成本告警 */
    COST("COST", "成本告警"),

    /** 异常告警 */
    ERROR_RATE("ERROR_RATE", "异常告警"),

    /** 审批提醒 */
    APPROVAL("APPROVAL", "审批提醒"),

    /** 系统告警 */
    SYSTEM("SYSTEM", "系统告警");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
