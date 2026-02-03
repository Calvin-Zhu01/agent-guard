package com.agentguard.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 告警配置 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "告警配置")
public class AlertSettingsDTO {

    @Schema(description = "成本告警是否启用")
    private Boolean costAlertEnabled;

    @Schema(description = "成本告警阈值（百分比，如85表示85%）")
    private Integer costThreshold;

    @Schema(description = "错误率告警是否启用")
    private Boolean errorRateAlertEnabled;

    @Schema(description = "错误率告警阈值（百分比，如10表示10%）")
    private Integer errorRateThreshold;

    @Schema(description = "错误率统计时间窗口（分钟）")
    private Integer errorRateWindow;

    @Schema(description = "审批提醒是否启用")
    private Boolean approvalReminderEnabled;

    @Schema(description = "审批提醒提前时间（分钟）")
    private Integer approvalReminderMinutes;
}
