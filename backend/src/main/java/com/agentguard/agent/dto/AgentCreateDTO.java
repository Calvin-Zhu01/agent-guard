package com.agentguard.agent.dto;

import com.agentguard.agent.enums.AgentEnvironment;
import com.agentguard.agent.enums.AgentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Agent 创建请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "创建Agent请求")
public class AgentCreateDTO {

    @Schema(description = "Agent名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "客服助手")
    @NotBlank(message = "Agent名称不能为空")
    @Size(max = 100, message = "Agent名称不能超过100个字符")
    private String name;

    @Schema(description = "Agent类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "CUSTOMER_SERVICE")
    @NotNull(message = "Agent类型不能为空")
    private AgentType type;

    @Schema(description = "所属部门", example = "技术部")
    private String department;

    @Schema(description = "运行环境", defaultValue = "TEST")
    private AgentEnvironment environment = AgentEnvironment.TEST;

    @Schema(description = "描述信息", example = "用于处理客户咨询的智能助手")
    private String description;
}
