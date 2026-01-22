package com.agentguard.log.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 策略快照DTO
 * 用于在日志中冗余存储触发策略时的策略信息
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "策略快照")
public class PolicySnapshotDTO {

    @Schema(description = "策略ID")
    private String id;

    @Schema(description = "策略名称")
    private String name;

    @Schema(description = "策略类型：ACCESS_CONTROL/RATE_LIMIT/APPROVAL")
    private String type;

    @Schema(description = "策略动作：ALLOW/DENY/APPROVAL/RATE_LIMIT")
    private String action;

    @Schema(description = "策略条件配置（JSON字符串）")
    private String conditions;

    @Schema(description = "策略原因/拦截说明")
    private String reason;
}
