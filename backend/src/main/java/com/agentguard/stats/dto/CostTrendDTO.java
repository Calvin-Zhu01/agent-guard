package com.agentguard.stats.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 成本趋势数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "成本趋势")
public class CostTrendDTO {

    @Schema(description = "日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "LLM成本")
    private BigDecimal llmCost;

    @Schema(description = "API成本")
    private BigDecimal apiCost;

    @Schema(description = "调用次数")
    private Integer apiCalls;

    @Schema(description = "Token总数")
    private Long totalTokens;
}
