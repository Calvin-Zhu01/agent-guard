package com.agentguard.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算创建请求 DTO
 *
 * @author zhuhx
 */
@Data
@Schema(description = "创建预算请求")
public class BudgetCreateDTO {

    @Schema(description = "预算月份（格式：YYYY-MM）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01")
    @NotBlank(message = "预算月份不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "预算月份格式必须为YYYY-MM")
    private String month;

    @Schema(description = "预算上限金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    @NotNull(message = "预算上限金额不能为空")
    @DecimalMin(value = "0.01", message = "预算上限金额必须大于0")
    private BigDecimal limitAmount;

    @Schema(description = "告警阈值（0-1之间，如0.8表示80%）", example = "0.8")
    @DecimalMin(value = "0", message = "告警阈值必须在0-1之间")
    @DecimalMax(value = "1", message = "告警阈值必须在0-1之间")
    private BigDecimal alertThreshold = new BigDecimal("0.8");
}
