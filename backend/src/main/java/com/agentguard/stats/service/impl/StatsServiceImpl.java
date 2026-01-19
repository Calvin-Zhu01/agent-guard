package com.agentguard.stats.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.agentguard.stats.dto.AgentCostRankDTO;
import com.agentguard.stats.dto.CostTrendDTO;
import com.agentguard.stats.dto.StatsOverviewDTO;
import com.agentguard.stats.entity.CostRecordDO;
import com.agentguard.stats.mapper.CostRecordMapper;
import com.agentguard.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统计服务实现类
 *
 * @author zhuhx
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final CostRecordMapper costRecordMapper;

    @Override
    public StatsOverviewDTO getOverview(LocalDate startDate, LocalDate endDate) {
        StatsOverviewDTO overview = costRecordMapper.selectOverview(startDate, endDate);
        if (ObjectUtil.isNull(overview)) {
            overview = new StatsOverviewDTO();
            overview.setTotalCost(BigDecimal.ZERO);
            overview.setLlmCost(BigDecimal.ZERO);
            overview.setApiCost(BigDecimal.ZERO);
            overview.setTotalTokens(0L);
            overview.setTokenInput(0L);
            overview.setTokenOutput(0L);
            overview.setTotalCalls(0);
            overview.setAgentCount(0);
        }
        return overview;
    }

    @Override
    public List<CostTrendDTO> getTrends(LocalDate startDate, LocalDate endDate) {
        return costRecordMapper.selectTrends(startDate, endDate);
    }

    @Override
    public List<AgentCostRankDTO> getTopAgents(int limit, LocalDate startDate, LocalDate endDate) {
        List<AgentCostRankDTO> ranks = costRecordMapper.selectTopAgents(startDate, endDate, limit);
        
        // 设置排名
        AtomicInteger rankNum = new AtomicInteger(1);
        ranks.forEach(rank -> rank.setRank(rankNum.getAndIncrement()));
        
        return ranks;
    }

    @Override
    @Transactional
    public void updateCostRecord(String agentId, String model, Integer tokenInput,
                                 Integer tokenOutput, BigDecimal llmCost,
                                 BigDecimal apiCost, boolean isApiCall) {
        LocalDate today = LocalDate.now();
        String modelKey = ObjectUtil.isNull(model) ? "unknown" : model;

        // 查询当天该Agent该模型的成本记录
        CostRecordDO existingRecord = costRecordMapper.selectByAgentDateModel(agentId, today, modelKey);

        if (ObjectUtil.isNull(existingRecord)) {
            // 创建新记录
            CostRecordDO newRecord = new CostRecordDO();
            newRecord.setAgentId(agentId);
            newRecord.setDate(today);
            newRecord.setModel(modelKey);
            newRecord.setTokenInput(ObjectUtil.isNull(tokenInput) ? 0L : tokenInput.longValue());
            newRecord.setTokenOutput(ObjectUtil.isNull(tokenOutput) ? 0L : tokenOutput.longValue());
            newRecord.setApiCalls(isApiCall ? 1 : 0);
            newRecord.setLlmCost(ObjectUtil.isNull(llmCost) ? BigDecimal.ZERO : llmCost);
            newRecord.setApiCost(ObjectUtil.isNull(apiCost) ? BigDecimal.ZERO : apiCost);
            newRecord.setTotalCost(newRecord.getLlmCost().add(newRecord.getApiCost()));
            costRecordMapper.insert(newRecord);
        } else {
            // 更新现有记录
            existingRecord.setTokenInput(existingRecord.getTokenInput() + 
                    (ObjectUtil.isNull(tokenInput) ? 0L : tokenInput.longValue()));
            existingRecord.setTokenOutput(existingRecord.getTokenOutput() + 
                    (ObjectUtil.isNull(tokenOutput) ? 0L : tokenOutput.longValue()));
            existingRecord.setApiCalls(existingRecord.getApiCalls() + (isApiCall ? 1 : 0));
            existingRecord.setLlmCost(existingRecord.getLlmCost()
                    .add(ObjectUtil.isNull(llmCost) ? BigDecimal.ZERO : llmCost));
            existingRecord.setApiCost(existingRecord.getApiCost()
                    .add(ObjectUtil.isNull(apiCost) ? BigDecimal.ZERO : apiCost));
            existingRecord.setTotalCost(existingRecord.getLlmCost().add(existingRecord.getApiCost()));
            costRecordMapper.updateById(existingRecord);
        }
    }
}
