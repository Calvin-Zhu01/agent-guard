package com.agentguard.alert.controller;

import com.agentguard.alert.dto.AlertHistoryDTO;
import com.agentguard.alert.dto.AlertRuleCreateDTO;
import com.agentguard.alert.dto.AlertRuleDTO;
import com.agentguard.alert.dto.AlertRuleUpdateDTO;
import com.agentguard.alert.enums.AlertStatus;
import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.service.AlertHistoryService;
import com.agentguard.alert.service.AlertRuleService;
import com.agentguard.common.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警管理控制器
 *
 * @author zhuhx
 */
@Tag(name = "告警管理", description = "告警规则和告警历史管理接口")
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRuleService alertRuleService;
    private final AlertHistoryService alertHistoryService;

    // ==================== 告警规则管理 ====================

    @Operation(summary = "创建告警规则")
    @PostMapping("/rules")
    public Result<AlertRuleDTO> createRule(@Valid @RequestBody AlertRuleCreateDTO dto) {
        return Result.success(alertRuleService.create(dto));
    }

    @Operation(summary = "获取告警规则详情")
    @GetMapping("/rules/{id}")
    public Result<AlertRuleDTO> getRuleById(@PathVariable String id) {
        return Result.success(alertRuleService.getById(id));
    }


    @Operation(summary = "分页查询告警规则列表")
    @GetMapping("/rules")
    public Result<IPage<AlertRuleDTO>> pageRules(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词（名称模糊搜索）") @RequestParam(required = false) String keyword,
            @Parameter(description = "告警类型") @RequestParam(required = false) AlertType type,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        Page<AlertRuleDTO> page = new Page<>(current, size);
        return Result.success(alertRuleService.page(page, keyword, type, enabled));
    }

    @Operation(summary = "更新告警规则")
    @PutMapping("/rules/{id}")
    public Result<AlertRuleDTO> updateRule(@PathVariable String id, @Valid @RequestBody AlertRuleUpdateDTO dto) {
        return Result.success(alertRuleService.update(id, dto));
    }

    @Operation(summary = "删除告警规则")
    @DeleteMapping("/rules/{id}")
    public Result<Void> deleteRule(@PathVariable String id) {
        alertRuleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "启用告警规则")
    @PostMapping("/rules/{id}/enable")
    public Result<Void> enableRule(@PathVariable String id) {
        alertRuleService.enable(id);
        return Result.success();
    }

    @Operation(summary = "停用告警规则")
    @PostMapping("/rules/{id}/disable")
    public Result<Void> disableRule(@PathVariable String id) {
        alertRuleService.disable(id);
        return Result.success();
    }

    @Operation(summary = "获取所有已启用的告警规则")
    @GetMapping("/rules/enabled")
    public Result<List<AlertRuleDTO>> getEnabledRules() {
        return Result.success(alertRuleService.getEnabledRules());
    }

    @Operation(summary = "根据类型获取已启用的告警规则")
    @GetMapping("/rules/enabled/{type}")
    public Result<List<AlertRuleDTO>> getEnabledRulesByType(@PathVariable AlertType type) {
        return Result.success(alertRuleService.getEnabledRulesByType(type));
    }

    // ==================== 告警历史查询 ====================

    @Operation(summary = "获取告警历史详情")
    @GetMapping("/history/{id}")
    public Result<AlertHistoryDTO> getHistoryById(@PathVariable String id) {
        return Result.success(alertHistoryService.getById(id));
    }

    @Operation(summary = "分页查询告警历史")
    @GetMapping("/history")
    public Result<IPage<AlertHistoryDTO>> pageHistory(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "告警类型") @RequestParam(required = false) AlertType type,
            @Parameter(description = "发送状态") @RequestParam(required = false) AlertStatus status,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<AlertHistoryDTO> page = new Page<>(current, size);
        return Result.success(alertHistoryService.page(page, type, status, startTime, endTime));
    }

    @Operation(summary = "统计告警数量")
    @GetMapping("/history/count")
    public Result<Long> countHistory(
            @Parameter(description = "告警类型") @RequestParam(required = false) AlertType type,
            @Parameter(description = "发送状态") @RequestParam(required = false) AlertStatus status,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.success(alertHistoryService.count(type, status, startTime, endTime));
    }

    @Operation(summary = "导出告警历史数据")
    @GetMapping("/history/export")
    public Result<List<AlertHistoryDTO>> exportHistory(
            @Parameter(description = "告警类型") @RequestParam(required = false) AlertType type,
            @Parameter(description = "发送状态") @RequestParam(required = false) AlertStatus status,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.success(alertHistoryService.export(type, status, startTime, endTime));
    }

    @Operation(summary = "根据规则ID查询告警历史")
    @GetMapping("/history/rule/{ruleId}")
    public Result<IPage<AlertHistoryDTO>> pageHistoryByRuleId(
            @PathVariable String ruleId,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        Page<AlertHistoryDTO> page = new Page<>(current, size);
        return Result.success(alertHistoryService.pageByRuleId(ruleId, page));
    }

    @Operation(summary = "获取最近的告警历史")
    @GetMapping("/history/recent")
    public Result<List<AlertHistoryDTO>> getRecentHistory(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(alertHistoryService.getRecent(limit));
    }
}
