package com.agentguard.agent.dto;

import com.agentguard.agent.enums.AgentEnvironment;
import com.agentguard.agent.enums.AgentStatus;
import com.agentguard.agent.enums.AgentType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "Agent信息")
public class AgentDTO {

    @Schema(description = "Agent ID")
    private String id;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "Agent名称")
    private String name;

    @Schema(description = "Agent类型")
    private AgentType type;

    @Schema(description = "所属部门")
    private String department;

    @Schema(description = "运行环境")
    private AgentEnvironment environment;

    @Schema(description = "API密钥")
    private String apiKey;

    @Schema(description = "描述信息")
    private String description;

    @Schema(description = "状态")
    private AgentStatus status;

    @Schema(description = "最后活跃时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActiveAt;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
