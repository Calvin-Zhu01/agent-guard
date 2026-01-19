package com.agentguard.budget.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算及使用情况数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "预算及使用情况")
public class BudgetWithUsageDTO {

    @Schema(description = "预算ID")
    private String id;

    @Schema(description = "预算月份（格式：YYYY-MM）", example = "2026-01")
    private String month;

    @Schema(description = "预算上限金额", example = "10000.00")
    private BigDecimal limitAmount;

    @Schema(description = "告警阈值（0-1之间）", example = "0.8")
    private BigDecimal alertThreshold;

    @Schema(description = "当前已使用金额", example = "5000.00")
    private BigDecimal usedAmount;

    @Schema(description = "使用百分比（0-1之间）", example = "0.5")
    private BigDecimal usagePercentage;

    @Schema(description = "剩余金额", example = "5000.00")
    private BigDecimal remainingAmount;

    @Schema(description = "是否已触发告警")
    private Boolean alertTriggered;

    @Schema(description = "是否已超预算")
    private Boolean overBudget;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
