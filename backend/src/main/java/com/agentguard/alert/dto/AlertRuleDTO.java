package com.agentguard.alert.dto;

import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.enums.NotificationChannelType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "告警规则信息")
public class AlertRuleDTO {

    @Schema(description = "规则ID")
    private String id;

    @Schema(description = "规则名称")
    private String name;

    @Schema(description = "告警类型")
    private AlertType type;

    @Schema(description = "阈值")
    private BigDecimal threshold;

    @Schema(description = "通知渠道")
    private NotificationChannelType channelType;

    @Schema(description = "渠道配置（JSON格式）")
    private String channelConfig;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
