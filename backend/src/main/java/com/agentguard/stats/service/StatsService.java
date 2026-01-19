package com.agentguard.stats.service;

import com.agentguard.stats.dto.AgentCostRankDTO;
import com.agentguard.stats.dto.CostTrendDTO;
import com.agentguard.stats.dto.StatsOverviewDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计服务接口
 *
 * @author zhuhx
 */
public interface StatsService {

    /**
     * 获取成本概览
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 成本概览
     */
    StatsOverviewDTO getOverview(LocalDate startDate, LocalDate endDate);

    /**
     * 获取成本趋势
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 成本趋势列表
     */
    List<CostTrendDTO> getTrends(LocalDate startDate, LocalDate endDate);

    /**
     * 获取Agent成本排行
     *
     * @param limit     限制数量
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return Agent成本排行列表
     */
    List<AgentCostRankDTO> getTopAgents(int limit, LocalDate startDate, LocalDate endDate);

    /**
     * 更新成本记录（根据日志数据）
     *
     * @param agentId     AgentID
     * @param model       模型
     * @param tokenInput  输入token数
     * @param tokenOutput 输出token数
     * @param llmCost     LLM成本
     * @param apiCost     API成本
     * @param isApiCall   是否为API调用
     */
    void updateCostRecord(String agentId, String model, Integer tokenInput, 
                          Integer tokenOutput, java.math.BigDecimal llmCost, 
                          java.math.BigDecimal apiCost, boolean isApiCall);
}
