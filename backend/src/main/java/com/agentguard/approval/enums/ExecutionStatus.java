package com.agentguard.approval.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批执行状态枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum ExecutionStatus {

    /** 未执行 */
    NOT_EXECUTED("NOT_EXECUTED", "未执行"),

    /** 执行中 */
    EXECUTING("EXECUTING", "执行中"),

    /** 执行成功 */
    SUCCESS("SUCCESS", "执行成功"),

    /** 执行失败 */
    FAILED("FAILED", "执行失败");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
