package com.agentguard.alert.dto;

import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.enums.NotificationChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 告警信息 DTO（用于发送告警）
 *
 * @author zhuhx
 */
@Data
@Schema(description = "告警信息")
public class AlertDTO {

    @Schema(description = "告警规则ID")
    private String ruleId;

    @Schema(description = "告警类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private AlertType type;

    @Schema(description = "告警标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "告警内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "接收人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String recipient;

    @Schema(description = "通知渠道", requiredMode = Schema.RequiredMode.REQUIRED)
    private NotificationChannelType channelType;
}
