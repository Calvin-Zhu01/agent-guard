package com.agentguard.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算更新请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "更新预算请求")
public class BudgetUpdateDTO {

    @Schema(description = "预算上限金额", example = "15000.00")
    @DecimalMin(value = "0.01", message = "预算上限金额必须大于0")
    private BigDecimal limitAmount;

    @Schema(description = "告警阈值（0-1之间，如0.8表示80%）", example = "0.85")
    @DecimalMin(value = "0", message = "告警阈值必须在0-1之间")
    @DecimalMax(value = "1", message = "告警阈值必须在0-1之间")
    private BigDecimal alertThreshold;
}
