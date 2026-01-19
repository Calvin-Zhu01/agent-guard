package com.agentguard.budget.controller;

import com.agentguard.budget.dto.BudgetCreateDTO;
import com.agentguard.budget.dto.BudgetDTO;
import com.agentguard.budget.dto.BudgetUpdateDTO;
import com.agentguard.budget.dto.BudgetWithUsageDTO;
import com.agentguard.budget.service.BudgetService;
import com.agentguard.common.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 预算管理控制器
 *
 * @author zhuhx
 */
@Tag(name = "预算管理", description = "预算管理相关接口")
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "创建预算")
    @PostMapping
    public Result<BudgetDTO> create(@Valid @RequestBody BudgetCreateDTO dto) {
        return Result.success(budgetService.create(dto));
    }

    @Operation(summary = "获取预算详情")
    @GetMapping("/{id}")
    public Result<BudgetDTO> getById(@Parameter(description = "预算ID") @PathVariable String id) {
        return Result.success(budgetService.getById(id));
    }

    @Operation(summary = "分页查询预算列表")
    @GetMapping
    public Result<IPage<BudgetDTO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<BudgetDTO> page = new Page<>(pageNum, pageSize);
        return Result.success(budgetService.page(page));
    }

    @Operation(summary = "更新预算")
    @PutMapping("/{id}")
    public Result<BudgetDTO> update(
            @Parameter(description = "预算ID") @PathVariable String id,
            @Valid @RequestBody BudgetUpdateDTO dto) {
        return Result.success(budgetService.update(id, dto));
    }

    @Operation(summary = "获取当月预算及使用情况")
    @GetMapping("/current")
    public Result<BudgetWithUsageDTO> getCurrentBudget() {
        return Result.success(budgetService.getCurrentBudget());
    }

    @Operation(summary = "检查并触发预算告警")
    @PostMapping("/check-alert")
    public Result<Void> checkAndAlert() {
        budgetService.checkAndAlert();
        return Result.success();
    }
}
