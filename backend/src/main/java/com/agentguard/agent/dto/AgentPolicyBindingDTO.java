package com.agentguard.agent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent策略绑定数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "Agent策略绑定信息")
public class AgentPolicyBindingDTO {

    @Schema(description = "绑定ID")
    private String id;

    @Schema(description = "Agent ID")
    private String agentId;

    @Schema(description = "策略ID")
    private String policyId;

    @Schema(description = "Agent名称")
    private String agentName;

    @Schema(description = "策略名称")
    private String policyName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
