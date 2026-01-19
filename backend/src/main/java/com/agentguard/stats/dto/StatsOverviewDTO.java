package com.agentguard.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成本概览数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "成本概览")
public class StatsOverviewDTO {

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "LLM成本")
    private BigDecimal llmCost;

    @Schema(description = "API成本")
    private BigDecimal apiCost;

    @Schema(description = "总Token数")
    private Long totalTokens;

    @Schema(description = "输入Token数")
    private Long tokenInput;

    @Schema(description = "输出Token数")
    private Long tokenOutput;

    @Schema(description = "总调用次数")
    private Integer totalCalls;

    @Schema(description = "Agent数量")
    private Integer agentCount;
}
