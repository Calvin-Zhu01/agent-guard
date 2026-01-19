package com.agentguard.alert.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警发送状态枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum AlertStatus {

    /** 发送成功 */
    SUCCESS("SUCCESS", "发送成功"),

    /** 发送失败 */
    FAILED("FAILED", "发送失败");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
