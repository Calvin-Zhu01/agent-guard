package com.agentguard.stats.cost;

import java.math.BigDecimal;

/**
 * 成本计算器接口
 *
 * @author zhuhx
 */
public interface CostCalculator {

    /**
     * 计算 LLM 调用成本
     *
     * @param provider LLM提供商
     * @param model 模型名称
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     * @return 成本（美元）
     */
    BigDecimal calculateCost(String provider, String model,
                            Integer inputTokens, Integer outputTokens);
}
