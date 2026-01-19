package com.agentguard.alert.dto;

import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.enums.NotificationChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 告警规则创建请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "创建告警规则请求")
public class AlertRuleCreateDTO {

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "成本超限告警")
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称不能超过100个字符")
    private String name;

    @Schema(description = "告警类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "COST")
    @NotNull(message = "告警类型不能为空")
    private AlertType type;

    @Schema(description = "阈值", example = "0.8")
    private BigDecimal threshold;

    @Schema(description = "通知渠道", requiredMode = Schema.RequiredMode.REQUIRED, example = "EMAIL")
    @NotNull(message = "通知渠道不能为空")
    private NotificationChannelType channelType;

    @Schema(description = "渠道配置（JSON格式）", requiredMode = Schema.RequiredMode.REQUIRED, example = "{\"email\": \"admin@example.com\"}")
    @NotBlank(message = "渠道配置不能为空")
    private String channelConfig;
}
