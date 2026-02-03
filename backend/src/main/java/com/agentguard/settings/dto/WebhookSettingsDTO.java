package com.agentguard.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Webhook通知配置 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "Webhook通知配置")
public class WebhookSettingsDTO {

    @Schema(description = "钉钉机器人是否启用")
    private Boolean dingTalkEnabled;

    @Schema(description = "钉钉机器人Webhook地址")
    private String dingTalkWebhook;

    @Schema(description = "钉钉机器人签名密钥")
    private String dingTalkSecret;

    @Schema(description = "企业微信机器人是否启用")
    private Boolean weComEnabled;

    @Schema(description = "企业微信机器人Webhook地址")
    private String weComWebhook;

    @Schema(description = "自定义Webhook是否启用")
    private Boolean customWebhookEnabled;

    @Schema(description = "自定义Webhook地址")
    private String customWebhookUrl;
}
