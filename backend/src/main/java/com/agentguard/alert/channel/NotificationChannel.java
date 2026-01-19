package com.agentguard.alert.channel;

import com.agentguard.alert.enums.NotificationChannelType;

/**
 * 通知渠道接口
 *
 * @author zhuhx
 */
public interface NotificationChannel {

    /**
     * 获取渠道类型
     *
     * @return 渠道类型（EMAIL/WEBHOOK）
     */
    NotificationChannelType getType();

    /**
     * 发送通知
     *
     * @param recipient 接收人（邮箱地址或 Webhook URL）
     * @param subject   主题
     * @param content   内容
     * @return 是否发送成功
     */
    boolean send(String recipient, String subject, String content);
}
