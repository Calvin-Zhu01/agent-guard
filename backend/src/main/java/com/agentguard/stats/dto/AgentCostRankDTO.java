package com.agentguard.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Agent成本排行数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "Agent成本排行")
public class AgentCostRankDTO {

    @Schema(description = "AgentID")
    private String agentId;

    @Schema(description = "Agent名称")
    private String agentName;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "LLM成本")
    private BigDecimal llmCost;

    @Schema(description = "API成本")
    private BigDecimal apiCost;

    @Schema(description = "总Token数")
    private Long totalTokens;

    @Schema(description = "调用次数")
    private Integer apiCalls;

    @Schema(description = "排名")
    private Integer rank;
}
