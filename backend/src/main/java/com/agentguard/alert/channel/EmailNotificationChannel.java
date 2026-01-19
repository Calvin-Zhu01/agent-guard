package com.agentguard.alert.channel;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailUtil;
import com.agentguard.alert.enums.NotificationChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 邮件通知渠道实现
 * 继承 AbstractNotificationChannel 以获得重试机制
 *
 * @author zhuhx
 */
@Slf4j
@Component
public class EmailNotificationChannel extends AbstractNotificationChannel {

    @Value("${alert.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${alert.email.from:}")
    private String fromAddress;

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    protected boolean doSend(String recipient, String subject, String content) throws Exception {
        if (StrUtil.isBlank(recipient)) {
            log.warn("邮件接收人为空，跳过发送");
            return false;
        }

        if (!emailEnabled) {
            log.info("邮件通知未启用，模拟发送邮件: to={}, subject={}", recipient, subject);
            return true;
        }

        // 使用 Hutool MailUtil 发送邮件
        // 需要在 mail.setting 或 application.yml 中配置邮件服务器
        MailUtil.send(recipient, subject, content, false);
        log.info("邮件发送成功: to={}, subject={}", recipient, subject);
        return true;
    }
}
