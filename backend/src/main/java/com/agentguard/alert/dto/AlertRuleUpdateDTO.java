package com.agentguard.alert.dto;

import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.enums.NotificationChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 告警规则更新请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "更新告警规则请求")
public class AlertRuleUpdateDTO {

    @Schema(description = "规则名称", example = "成本超限告警")
    @Size(max = 100, message = "规则名称不能超过100个字符")
    private String name;

    @Schema(description = "告警类型", example = "COST")
    private AlertType type;

    @Schema(description = "阈值", example = "0.8")
    private BigDecimal threshold;

    @Schema(description = "通知渠道", example = "EMAIL")
    private NotificationChannelType channelType;

    @Schema(description = "渠道配置（JSON格式）", example = "{\"email\": \"admin@example.com\"}")
    private String channelConfig;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}
