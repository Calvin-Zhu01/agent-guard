package com.agentguard.budget.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "预算信息")
public class BudgetDTO {

    @Schema(description = "预算ID")
    private String id;

    @Schema(description = "预算月份（格式：YYYY-MM）", example = "2026-01")
    private String month;

    @Schema(description = "预算上限金额", example = "10000.00")
    private BigDecimal limitAmount;

    @Schema(description = "告警阈值（0-1之间）", example = "0.8")
    private BigDecimal alertThreshold;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
