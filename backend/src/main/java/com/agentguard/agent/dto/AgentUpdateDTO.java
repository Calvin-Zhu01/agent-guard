package com.agentguard.agent.dto;

import com.agentguard.agent.enums.AgentEnvironment;
import com.agentguard.agent.enums.AgentStatus;
import com.agentguard.agent.enums.AgentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Agent 更新请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "更新Agent请求")
public class AgentUpdateDTO {

    @Schema(description = "Agent名称", example = "客服助手V2")
    @Size(max = 100, message = "Agent名称不能超过100个字符")
    private String name;

    @Schema(description = "Agent类型", example = "CUSTOMER_SERVICE")
    private AgentType type;

    @Schema(description = "所属部门", example = "技术部")
    private String department;

    @Schema(description = "运行环境", example = "PRODUCTION")
    private AgentEnvironment environment;

    @Schema(description = "描述信息", example = "升级版客服助手")
    private String description;

    @Schema(description = "状态", example = "ENABLED")
    private AgentStatus status;
}
