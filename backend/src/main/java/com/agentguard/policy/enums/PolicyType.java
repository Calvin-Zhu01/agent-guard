package com.agentguard.policy.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 策略类型枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum PolicyType {

    /** 访问控制 */
    ACCESS_CONTROL("ACCESS_CONTROL", "访问控制"),

    /** 内容保护 */
    CONTENT_PROTECTION("CONTENT_PROTECTION", "内容保护"),

    /** 人工审批 */
    APPROVAL("APPROVAL", "人工审批"),

    /** 频率限制 */
    RATE_LIMIT("RATE_LIMIT", "频率限制");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
