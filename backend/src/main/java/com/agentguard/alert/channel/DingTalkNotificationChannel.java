package com.agentguard.alert.channel;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agentguard.alert.enums.NotificationChannelType;
import com.agentguard.settings.dto.WebhookSettingsDTO;
import com.agentguard.settings.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 钉钉机器人通知渠道实现
 * 继承 AbstractNotificationChannel 以获得重试机制
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkNotificationChannel extends AbstractNotificationChannel {

    private final SystemSettingsService systemSettingsService;

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.DINGTALK;
    }

    @Override
    protected boolean doSend(String recipient, String subject, String content) throws Exception {
        // 从系统设置中获取钉钉配置
        WebhookSettingsDTO webhookSettings = systemSettingsService.getWebhookSettings();

        if (!Boolean.TRUE.equals(webhookSettings.getDingTalkEnabled())) {
            log.info("钉钉通知未启用，跳过发送");
            return false;
        }

        String webhook = webhookSettings.getDingTalkWebhook();
        String secret = webhookSettings.getDingTalkSecret();

        if (StrUtil.isBlank(webhook)) {
            log.warn("钉钉Webhook地址为空，无法发送通知");
            return false;
        }

        // 如果配置了签名密钥，需要计算签名
        String finalWebhook = webhook;
        if (StrUtil.isNotBlank(secret)) {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            String sign = SecureUtil.hmacSha256(secret).digestBase64(stringToSign, StandardCharsets.UTF_8, true);
            finalWebhook = webhook + "&timestamp=" + timestamp + "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);
        }

        // 构建钉钉消息体（Markdown格式）
        JSONObject message = new JSONObject();
        message.set("msgtype", "markdown");

        JSONObject markdown = new JSONObject();
        markdown.set("title", subject);
        markdown.set("text", "### " + subject + "\n\n" + content);
        message.set("markdown", markdown);

        // 发送请求
        HttpResponse response = HttpRequest.post(finalWebhook)
                .header("Content-Type", "application/json")
                .body(message.toString())
                .timeout(10000)
                .execute();

        int statusCode = response.getStatus();
        if (statusCode >= 200 && statusCode < 300) {
            log.info("钉钉通知发送成功: subject={}", subject);
            return true;
        } else {
            log.warn("钉钉通知发送失败: subject={}, status={}, body={}",
                    subject, statusCode, response.body());
            return false;
        }
    }
}
