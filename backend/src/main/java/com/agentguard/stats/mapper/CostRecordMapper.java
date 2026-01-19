package com.agentguard.stats.mapper;

import com.agentguard.stats.dto.AgentCostRankDTO;
import com.agentguard.stats.dto.CostTrendDTO;
import com.agentguard.stats.dto.StatsOverviewDTO;
import com.agentguard.stats.entity.CostRecordDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 成本记录数据访问层
 *
 * @author zhuhx
 */
@Mapper
public interface CostRecordMapper extends BaseMapper<CostRecordDO> {

    /**
     * 查询成本概览
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 成本概览
     */
    @Select("""
            SELECT 
                COALESCE(SUM(total_cost), 0) as totalCost,
                COALESCE(SUM(llm_cost), 0) as llmCost,
                COALESCE(SUM(api_cost), 0) as apiCost,
                COALESCE(SUM(token_input + token_output), 0) as totalTokens,
                COALESCE(SUM(token_input), 0) as tokenInput,
                COALESCE(SUM(token_output), 0) as tokenOutput,
                COALESCE(SUM(api_calls), 0) as totalCalls,
                COUNT(DISTINCT agent_id) as agentCount
            FROM cost_record
            WHERE date >= #{startDate} AND date <= #{endDate}
            """)
    StatsOverviewDTO selectOverview(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /**
     * 查询成本趋势
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 成本趋势列表
     */
    @Select("""
            SELECT 
                date,
                COALESCE(SUM(total_cost), 0) as totalCost,
                COALESCE(SUM(llm_cost), 0) as llmCost,
                COALESCE(SUM(api_cost), 0) as apiCost,
                COALESCE(SUM(api_calls), 0) as apiCalls,
                COALESCE(SUM(token_input + token_output), 0) as totalTokens
            FROM cost_record
            WHERE date >= #{startDate} AND date <= #{endDate}
            GROUP BY date
            ORDER BY date ASC
            """)
    List<CostTrendDTO> selectTrends(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /**
     * 查询Agent成本排行
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param limit     限制数量
     * @return Agent成本排行列表
     */
    @Select("""
            SELECT 
                cr.agent_id as agentId,
                a.name as agentName,
                COALESCE(SUM(cr.total_cost), 0) as totalCost,
                COALESCE(SUM(cr.llm_cost), 0) as llmCost,
                COALESCE(SUM(cr.api_cost), 0) as apiCost,
                COALESCE(SUM(cr.token_input + cr.token_output), 0) as totalTokens,
                COALESCE(SUM(cr.api_calls), 0) as apiCalls
            FROM cost_record cr
            LEFT JOIN agent a ON cr.agent_id = a.id
            WHERE cr.date >= #{startDate} AND cr.date <= #{endDate}
            GROUP BY cr.agent_id, a.name
            ORDER BY totalCost DESC
            LIMIT #{limit}
            """)
    List<AgentCostRankDTO> selectTopAgents(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("limit") int limit);

    /**
     * 根据AgentID、日期、模型查询成本记录
     *
     * @param agentId AgentID
     * @param date    日期
     * @param model   模型
     * @return 成本记录
     */
    @Select("""
            SELECT * FROM cost_record
            WHERE agent_id = #{agentId} AND date = #{date} AND model = #{model}
            """)
    CostRecordDO selectByAgentDateModel(@Param("agentId") String agentId,
                                        @Param("date") LocalDate date,
                                        @Param("model") String model);
}
