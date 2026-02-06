package com.agentguard.settings.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agentguard.common.util.EncryptionUtil;
import com.agentguard.settings.dto.AlertSettingsDTO;
import com.agentguard.settings.dto.EmailSettingsDTO;
import com.agentguard.settings.dto.WebhookSettingsDTO;
import com.agentguard.settings.entity.SystemSettingsDO;
import com.agentguard.settings.mapper.SystemSettingsMapper;
import com.agentguard.settings.service.SystemSettingsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 系统设置服务实现
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private final SystemSettingsMapper settingsMapper;
    private final EncryptionUtil encryptionUtil;

    private static final String CATEGORY_EMAIL = "alert_email";
    private static final String CATEGORY_WEBHOOK = "alert_webhook";
    private static final String CATEGORY_ALERT = "alert_config";

    @Override
    public EmailSettingsDTO getEmailSettings() {
        Map<String, String> settings = getSettingsByCategory(CATEGORY_EMAIL);
        EmailSettingsDTO dto = new EmailSettingsDTO();
        dto.setEnabled(Boolean.parseBoolean(settings.getOrDefault("enabled", "false")));
        dto.setSmtpHost(settings.get("smtp_host"));
        dto.setSmtpPort(parseInteger(settings.get("smtp_port"), 587));
        dto.setFromEmail(settings.get("from_email"));
        dto.setFromName(settings.get("from_name"));
        dto.setUsername(settings.get("username"));
        // 密码解密
        String encryptedPassword = settings.get("password");
        if (StrUtil.isNotBlank(encryptedPassword)) {
            try {
                dto.setPassword(encryptionUtil.decrypt(encryptedPassword));
            } catch (Exception e) {
                log.warn("解密邮件密码失败", e);
            }
        }
        dto.setSslEnabled(Boolean.parseBoolean(settings.getOrDefault("ssl_enabled", "true")));
        dto.setDefaultRecipients(settings.get("default_recipients"));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmailSettings(EmailSettingsDTO dto) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("enabled", dto.getEnabled());
        settings.put("smtp_host", dto.getSmtpHost());
        settings.put("smtp_port", dto.getSmtpPort());
        settings.put("from_email", dto.getFromEmail());
        settings.put("from_name", dto.getFromName());
        settings.put("username", dto.getUsername());
        // 密码加密
        if (StrUtil.isNotBlank(dto.getPassword())) {
            try {
                settings.put("password", encryptionUtil.encrypt(dto.getPassword()));
            } catch (Exception e) {
                log.error("加密邮件密码失败", e);
                throw new RuntimeException("加密邮件密码失败");
            }
        }
        settings.put("ssl_enabled", dto.getSslEnabled());
        settings.put("default_recipients", dto.getDefaultRecipients());

        saveSettings(CATEGORY_EMAIL, settings);
    }

    @Override
    public WebhookSettingsDTO getWebhookSettings() {
        Map<String, String> settings = getSettingsByCategory(CATEGORY_WEBHOOK);
        WebhookSettingsDTO dto = new WebhookSettingsDTO();
        dto.setDingTalkEnabled(Boolean.parseBoolean(settings.getOrDefault("dingtalk_enabled", "false")));
        dto.setDingTalkWebhook(settings.get("dingtalk_webhook"));
        dto.setDingTalkSecret(settings.get("dingtalk_secret"));
        dto.setWeComEnabled(Boolean.parseBoolean(settings.getOrDefault("wecom_enabled", "false")));
        dto.setWeComWebhook(settings.get("wecom_webhook"));
        dto.setCustomWebhookEnabled(Boolean.parseBoolean(settings.getOrDefault("custom_webhook_enabled", "false")));
        dto.setCustomWebhookUrl(settings.get("custom_webhook_url"));
        dto.setCustomWebhookSecret(settings.get("custom_webhook_secret"));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWebhookSettings(WebhookSettingsDTO dto) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("dingtalk_enabled", dto.getDingTalkEnabled());
        settings.put("dingtalk_webhook", dto.getDingTalkWebhook());
        settings.put("dingtalk_secret", dto.getDingTalkSecret());
        settings.put("wecom_enabled", dto.getWeComEnabled());
        settings.put("wecom_webhook", dto.getWeComWebhook());
        settings.put("custom_webhook_enabled", dto.getCustomWebhookEnabled());
        settings.put("custom_webhook_url", dto.getCustomWebhookUrl());
        settings.put("custom_webhook_secret", dto.getCustomWebhookSecret());

        saveSettings(CATEGORY_WEBHOOK, settings);
    }

    @Override
    public AlertSettingsDTO getAlertSettings() {
        Map<String, String> settings = getSettingsByCategory(CATEGORY_ALERT);
        AlertSettingsDTO dto = new AlertSettingsDTO();
        dto.setCostAlertEnabled(Boolean.parseBoolean(settings.getOrDefault("cost_alert_enabled", "true")));
        dto.setCostThreshold(parseInteger(settings.get("cost_threshold"), 85));
        dto.setCostAlertCooldownMinutes(parseInteger(settings.get("cost_alert_cooldown_minutes"), 60));
        dto.setErrorRateAlertEnabled(Boolean.parseBoolean(settings.getOrDefault("error_rate_alert_enabled", "true")));
        dto.setErrorRateThreshold(parseInteger(settings.get("error_rate_threshold"), 10));
        dto.setErrorRateWindow(parseInteger(settings.get("error_rate_window"), 60));
        dto.setErrorRateAlertCooldownMinutes(parseInteger(settings.get("error_rate_alert_cooldown_minutes"), 30));
        dto.setApprovalReminderEnabled(Boolean.parseBoolean(settings.getOrDefault("approval_reminder_enabled", "true")));
        dto.setApprovalReminderMinutes(parseInteger(settings.get("approval_reminder_minutes"), 30));
        dto.setApprovalReminderCooldownMinutes(parseInteger(settings.get("approval_reminder_cooldown_minutes"), 10));
        dto.setApprovalExpirationMinutes(parseInteger(settings.get("approval_expiration_minutes"), 60));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlertSettings(AlertSettingsDTO dto) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("cost_alert_enabled", dto.getCostAlertEnabled());
        settings.put("cost_threshold", dto.getCostThreshold());
        settings.put("cost_alert_cooldown_minutes", dto.getCostAlertCooldownMinutes());
        settings.put("error_rate_alert_enabled", dto.getErrorRateAlertEnabled());
        settings.put("error_rate_threshold", dto.getErrorRateThreshold());
        settings.put("error_rate_window", dto.getErrorRateWindow());
        settings.put("error_rate_alert_cooldown_minutes", dto.getErrorRateAlertCooldownMinutes());
        settings.put("approval_reminder_enabled", dto.getApprovalReminderEnabled());
        settings.put("approval_reminder_minutes", dto.getApprovalReminderMinutes());
        settings.put("approval_reminder_cooldown_minutes", dto.getApprovalReminderCooldownMinutes());
        settings.put("approval_expiration_minutes", dto.getApprovalExpirationMinutes());

        saveSettings(CATEGORY_ALERT, settings);
    }

    @Override
    public boolean testEmailSettings(EmailSettingsDTO dto) {
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(dto.getSmtpHost());
            mailSender.setPort(dto.getSmtpPort());
            mailSender.setUsername(dto.getUsername());
            mailSender.setPassword(dto.getPassword());

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", dto.getSslEnabled());
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.connectiontimeout", "5000");

            // 测试连接
            mailSender.testConnection();
            log.info("邮件配置测试成功");
            return true;
        } catch (Exception e) {
            log.error("邮件配置测试失败", e);
            return false;
        }
    }

    /**
     * 获取指定分类的所有设置
     */
    private Map<String, String> getSettingsByCategory(String category) {
        LambdaQueryWrapper<SystemSettingsDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemSettingsDO::getCategory, category);
        List<SystemSettingsDO> list = settingsMapper.selectList(wrapper);

        Map<String, String> result = new HashMap<>();
        for (SystemSettingsDO setting : list) {
            result.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return result;
    }

    /**
     * 保存设置
     */
    private void saveSettings(String category, Map<String, Object> settings) {
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            // 查找是否已存在
            LambdaQueryWrapper<SystemSettingsDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemSettingsDO::getCategory, category)
                   .eq(SystemSettingsDO::getSettingKey, key);
            SystemSettingsDO existing = settingsMapper.selectOne(wrapper);

            if (existing != null) {
                // 更新
                existing.setSettingValue(value.toString());
                settingsMapper.updateById(existing);
            } else {
                // 新增
                SystemSettingsDO newSetting = new SystemSettingsDO();
                newSetting.setCategory(category);
                newSetting.setSettingKey(key);
                newSetting.setSettingValue(value.toString());
                newSetting.setEncrypted(key.contains("password") || key.contains("secret"));
                settingsMapper.insert(newSetting);
            }
        }
    }

    /**
     * 解析整数，失败返回默认值
     */
    private Integer parseInteger(String value, Integer defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
