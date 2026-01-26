package com.agentguard.policy.dto;

import com.agentguard.log.enums.RequestType;
import com.agentguard.policy.enums.PolicyAction;
import com.agentguard.policy.enums.PolicyScope;
import com.agentguard.policy.enums.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 策略更新请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "更新策略请求")
public class PolicyUpdateDTO {

    @Schema(description = "策略名称", example = "禁止访问支付接口V2")
    @Size(max = 100, message = "策略名称不能超过100个字符")
    private String name;

    @Schema(description = "策略描述", example = "升级版支付接口访问控制策略")
    private String description;

    @Schema(description = "策略类型", example = "ACCESS_CONTROL")
    private PolicyType type;

    @Schema(description = "策略条件（JSON格式）", example = "{\"urlPattern\": \"/api/payment.*\", \"method\": \"POST\"}")
    private String conditions;

    @Schema(description = "策略动作", example = "DENY")
    private PolicyAction action;

    @Schema(description = "优先级（数值越大优先级越高）", example = "200")
    @Min(value = 0, message = "优先级必须是非负整数")
    private Integer priority;

    @Schema(description = "作用域：GLOBAL-全局，AGENT-Agent级别")
    private PolicyScope scope;

    @Schema(description = "关联的Agent ID（仅当scope为AGENT时有效）")
    private String agentId;

    @Schema(description = "请求类型：LLM_CALL-LLM调用，API_CALL-API调用，ALL-全部")
    private RequestType requestType;

    @Schema(description = "策略标签（JSON数组）", example = "[\"财务\", \"支付\", \"高风险\"]")
    private String tags;
}
