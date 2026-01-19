package com.agentguard.alert.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知渠道类型枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum NotificationChannelType {

    /** 邮件 */
    EMAIL("EMAIL", "邮件"),

    /** Webhook */
    WEBHOOK("WEBHOOK", "Webhook");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
