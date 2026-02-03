package com.agentguard.alert.controller;

import com.agentguard.alert.dto.AlertHistoryDTO;
import com.agentguard.alert.enums.AlertStatus;
import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.service.AlertHistoryService;
import com.agentguard.common.response.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警管理控制器
 *
 * 简化版：只保留告警历史查询功能
 * 告警规则配置已移至系统设置模块
 *
 * @author zhuhx
 */
@Tag(name = "告警管理", description = "告警历史查询接口")
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertHistoryService alertHistoryService;

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

    @Operation(summary = "获取最近的告警历史")
    @GetMapping("/history/recent")
    public Result<List<AlertHistoryDTO>> getRecentHistory(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(alertHistoryService.getRecent(limit));
    }
}
