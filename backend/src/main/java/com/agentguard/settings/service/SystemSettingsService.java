package com.agentguard.settings.service;

import com.agentguard.settings.dto.AlertSettingsDTO;
import com.agentguard.settings.dto.EmailSettingsDTO;
import com.agentguard.settings.dto.WebhookSettingsDTO;

/**
 * 系统设置服务接口
 *
 * @author zhuhx
 */
public interface SystemSettingsService {

    /**
     * 获取邮件通知配置
     */
    EmailSettingsDTO getEmailSettings();

    /**
     * 更新邮件通知配置
     */
    void updateEmailSettings(EmailSettingsDTO dto);

    /**
     * 获取Webhook通知配置
     */
    WebhookSettingsDTO getWebhookSettings();

    /**
     * 更新Webhook通知配置
     */
    void updateWebhookSettings(WebhookSettingsDTO dto);

    /**
     * 获取告警配置
     */
    AlertSettingsDTO getAlertSettings();

    /**
     * 更新告警配置
     */
    void updateAlertSettings(AlertSettingsDTO dto);

    /**
     * 测试邮件配置
     */
    boolean testEmailSettings(EmailSettingsDTO dto);
}
