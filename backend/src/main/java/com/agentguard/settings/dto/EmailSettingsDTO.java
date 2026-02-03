package com.agentguard.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 邮件通知配置 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "邮件通知配置")
public class EmailSettingsDTO {

    @Schema(description = "是否启用邮件通知")
    private Boolean enabled;

    @Schema(description = "SMTP服务器地址")
    private String smtpHost;

    @Schema(description = "SMTP服务器端口")
    private Integer smtpPort;

    @Schema(description = "发件人邮箱")
    private String fromEmail;

    @Schema(description = "发件人名称")
    private String fromName;

    @Schema(description = "SMTP用户名")
    private String username;

    @Schema(description = "SMTP密码（加密存储）")
    private String password;

    @Schema(description = "是否启用SSL")
    private Boolean sslEnabled;

    @Schema(description = "默认收件人（多个用逗号分隔）")
    private String defaultRecipients;
}
