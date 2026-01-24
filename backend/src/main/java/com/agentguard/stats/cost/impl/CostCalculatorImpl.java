package com.agentguard.stats.cost.impl;

import com.agentguard.stats.cost.CostCalculator;
import com.agentguard.stats.entity.ModelPriceDO;
import com.agentguard.stats.mapper.ModelPriceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 成本计算器实现类
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CostCalculatorImpl implements CostCalculator {

    private final ModelPriceMapper modelPriceMapper;

    /** 百万（用于价格计算） */
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    @Override
    public BigDecimal calculateCost(String provider, String model,
                                   Integer inputTokens, Integer outputTokens) {
        if (inputTokens == null || outputTokens == null) {
            return BigDecimal.ZERO;
        }

        // 查询模型价格
        ModelPriceDO price = getModelPrice(provider, model);
        if (price == null) {
            log.warn("未找到 {}/{} 的价格配置，成本将为0", provider, model);
            return BigDecimal.ZERO;
        }

        // 计算成本
        // 输入成本 = (输入Token数 / 1,000,000) × 输入单价
        BigDecimal inputCost = new BigDecimal(inputTokens)
                .divide(ONE_MILLION, 10, RoundingMode.HALF_UP)
                .multiply(price.getInputPrice());

        // 输出成本 = (输出Token数 / 1,000,000) × 输出单价
        BigDecimal outputCost = new BigDecimal(outputTokens)
                .divide(ONE_MILLION, 10, RoundingMode.HALF_UP)
                .multiply(price.getOutputPrice());

        // 总成本，保留6位小数
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * 获取模型价格配置
     */
    private ModelPriceDO getModelPrice(String provider, String model) {
        LambdaQueryWrapper<ModelPriceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelPriceDO::getProvider, provider)
               .eq(ModelPriceDO::getModelName, model)
               .eq(ModelPriceDO::getEnabled, true)
               .orderByDesc(ModelPriceDO::getEffectiveDate)
               .last("LIMIT 1");

        return modelPriceMapper.selectOne(wrapper);
    }
}
