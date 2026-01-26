package com.agentguard.policy.dto;

import com.agentguard.log.enums.RequestType;
import com.agentguard.policy.enums.PolicyAction;
import com.agentguard.policy.enums.PolicyScope;
import com.agentguard.policy.enums.PolicyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 策略数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "策略信息")
public class PolicyDTO {

    @Schema(description = "策略ID")
    private String id;

    @Schema(description = "策略名称")
    private String name;

    @Schema(description = "策略描述")
    private String description;

    @Schema(description = "策略类型")
    private PolicyType type;

    @Schema(description = "策略条件（JSON格式）")
    private String conditions;

    @Schema(description = "策略动作")
    private PolicyAction action;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "作用域：GLOBAL-全局，AGENT-Agent级别")
    private PolicyScope scope;

    @Schema(description = "关联的Agent ID（仅当scope为AGENT时有效）")
    private String agentId;

    @Schema(description = "请求类型：LLM_CALL-LLM调用，API_CALL-API调用，ALL-全部")
    private RequestType requestType;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "策略标签（JSON数组）", example = "[\"财务\", \"支付\", \"高风险\"]")
    private String tags;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
