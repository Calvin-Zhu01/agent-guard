package com.agentguard.alert.channel;

import cn.hutool.core.util.StrUtil;
import com.agentguard.alert.enums.NotificationChannelType;
import com.agentguard.settings.dto.EmailSettingsDTO;
import com.agentguard.settings.service.SystemSettingsService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * 邮件通知渠道实现
 * 使用Spring Boot JavaMail发送邮件
 * 继承 AbstractNotificationChannel 以获得重试机制
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationChannel extends AbstractNotificationChannel {

    private final SystemSettingsService systemSettingsService;

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

        // 从系统设置中获取邮件配置
        EmailSettingsDTO emailSettings = systemSettingsService.getEmailSettings();

        if (!Boolean.TRUE.equals(emailSettings.getEnabled())) {
            log.info("邮件通知未启用，模拟发送邮件: to={}, subject={}", recipient, subject);
            return true;
        }

        // 验证必要配置
        if (StrUtil.isBlank(emailSettings.getSmtpHost()) ||
            StrUtil.isBlank(emailSettings.getUsername()) ||
            StrUtil.isBlank(emailSettings.getPassword())) {
            log.warn("邮件配置不完整，无法发送邮件");
            return false;
        }

        // 创建JavaMailSender
        JavaMailSenderImpl mailSender = createMailSender(emailSettings);

        // 创建邮件消息
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(emailSettings.getFromEmail(), emailSettings.getFromName());
        helper.setTo(recipient.split(","));
        helper.setSubject(subject);
        helper.setText(content, false);

        // 发送邮件
        mailSender.send(message);
        log.info("邮件发送成功: to={}, subject={}", recipient, subject);
        return true;
    }

    /**
     * 根据配置创建JavaMailSender
     */
    private JavaMailSenderImpl createMailSender(EmailSettingsDTO settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(settings.getSmtpHost());
        mailSender.setPort(settings.getSmtpPort());
        mailSender.setUsername(settings.getUsername());
        mailSender.setPassword(settings.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", settings.getSslEnabled());
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
