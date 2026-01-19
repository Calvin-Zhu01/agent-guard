package com.agentguard.stats.controller;

import com.agentguard.common.response.Result;
import com.agentguard.stats.dto.AgentCostRankDTO;
import com.agentguard.stats.dto.CostTrendDTO;
import com.agentguard.stats.dto.StatsOverviewDTO;
import com.agentguard.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计控制器
 *
 * @author zhuhx
 */
@Tag(name = "成本统计", description = "成本统计相关接口")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "获取成本概览")
    @GetMapping("/overview")
    public Result<StatsOverviewDTO> getOverview(
            @Parameter(description = "开始日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        // 默认查询当月数据
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        return Result.success(statsService.getOverview(startDate, endDate));
    }

    @Operation(summary = "获取成本趋势")
    @GetMapping("/trends")
    public Result<List<CostTrendDTO>> getTrends(
            @Parameter(description = "开始日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        // 默认查询最近30天数据
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        return Result.success(statsService.getTrends(startDate, endDate));
    }

    @Operation(summary = "获取Agent成本排行")
    @GetMapping("/top-agents")
    public Result<List<AgentCostRankDTO>> getTopAgents(
            @Parameter(description = "排行数量") @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "开始日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") 
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        // 默认查询当月数据
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        return Result.success(statsService.getTopAgents(limit, startDate, endDate));
    }
}
