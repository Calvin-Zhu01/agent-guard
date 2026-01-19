package com.agentguard.policy.dto;

import com.agentguard.policy.enums.PolicyAction;
import com.agentguard.policy.enums.PolicyScope;
import com.agentguard.policy.enums.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 策略创建请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "创建策略请求")
public class PolicyCreateDTO {

    @Schema(description = "策略名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "禁止访问支付接口")
    @NotBlank(message = "策略名称不能为空")
    @Size(max = 100, message = "策略名称不能超过100个字符")
    private String name;

    @Schema(description = "策略描述", example = "禁止Agent访问支付相关的API接口")
    private String description;

    @Schema(description = "策略类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACCESS_CONTROL")
    @NotNull(message = "策略类型不能为空")
    private PolicyType type;

    @Schema(description = "策略条件（JSON格式）", example = "{\"urlPattern\": \"/api/payment.*\", \"method\": \"POST\"}")
    private String conditions;

    @Schema(description = "策略动作", requiredMode = Schema.RequiredMode.REQUIRED, example = "DENY")
    @NotNull(message = "策略动作不能为空")
    private PolicyAction action;

    @Schema(description = "优先级（数值越大优先级越高）", example = "100")
    @Min(value = 0, message = "优先级必须是非负整数")
    private Integer priority = 0;

    @Schema(description = "作用域：GLOBAL-全局，AGENT-Agent级别", defaultValue = "GLOBAL")
    private PolicyScope scope = PolicyScope.GLOBAL;

    @Schema(description = "关联的Agent ID（仅当scope为AGENT时有效）")
    private String agentId;
}
