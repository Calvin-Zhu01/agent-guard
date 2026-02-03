package com.agentguard.alert.channel;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import com.agentguard.alert.enums.NotificationChannelType;
import com.agentguard.settings.dto.WebhookSettingsDTO;
import com.agentguard.settings.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 企业微信机器人通知渠道实现
 * 继承 AbstractNotificationChannel 以获得重试机制
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComNotificationChannel extends AbstractNotificationChannel {

    private final SystemSettingsService systemSettingsService;

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.WECOM;
    }

    @Override
    protected boolean doSend(String recipient, String subject, String content) throws Exception {
        // 从系统设置中获取企业微信配置
        WebhookSettingsDTO webhookSettings = systemSettingsService.getWebhookSettings();

        if (!Boolean.TRUE.equals(webhookSettings.getWeComEnabled())) {
            log.info("企业微信通知未启用，跳过发送");
            return false;
        }

        String webhook = webhookSettings.getWeComWebhook();

        if (StrUtil.isBlank(webhook)) {
            log.warn("企业微信Webhook地址为空，无法发送通知");
            return false;
        }

        // 构建企业微信消息体（Markdown格式）
        JSONObject message = new JSONObject();
        message.set("msgtype", "markdown");

        JSONObject markdown = new JSONObject();
        markdown.set("content", "### " + subject + "\n\n" + content);
        message.set("markdown", markdown);

        // 发送请求
        HttpResponse response = HttpRequest.post(webhook)
                .header("Content-Type", "application/json")
                .body(message.toString())
                .timeout(10000)
                .execute();

        int statusCode = response.getStatus();
        if (statusCode >= 200 && statusCode < 300) {
            log.info("企业微信通知发送成功: subject={}", subject);
            return true;
        } else {
            log.warn("企业微信通知发送失败: subject={}, status={}, body={}",
                    subject, statusCode, response.body());
            return false;
        }
    }
}
