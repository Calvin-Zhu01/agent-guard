package com.agentguard.alert.channel;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.agentguard.alert.enums.NotificationChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Webhook 通知渠道实现
 * 继承 AbstractNotificationChannel 以获得重试机制
 *
 * @author zhuhx
 */
@Slf4j
@Component
public class WebhookNotificationChannel extends AbstractNotificationChannel {

    @Value("${alert.webhook.timeout:10000}")
    private int timeout;

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.WEBHOOK;
    }

    @Override
    protected boolean doSend(String recipient, String subject, String content) throws Exception {
        if (StrUtil.isBlank(recipient)) {
            log.warn("Webhook URL 为空，跳过发送");
            return false;
        }

        // 构建 Webhook 请求体
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", subject);
        payload.put("content", content);
        payload.put("timestamp", System.currentTimeMillis());

        String jsonBody = JSONUtil.toJsonStr(payload);

        // 使用 Hutool HttpUtil 发送 POST 请求
        HttpResponse response = HttpRequest.post(recipient)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .timeout(timeout)
                .execute();

        int statusCode = response.getStatus();
        if (statusCode >= 200 && statusCode < 300) {
            log.info("Webhook 发送成功: url={}, subject={}, status={}", recipient, subject, statusCode);
            return true;
        } else {
            log.warn("Webhook 发送失败: url={}, subject={}, status={}, body={}",
                    recipient, subject, statusCode, response.body());
            return false;
        }
    }
}
