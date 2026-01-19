package com.agentguard.policy.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.agentguard.policy.dto.PolicyResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 硬编码策略引擎实现
 * 
 * MVP阶段使用硬编码规则：拦截 /transfer 路径且金额大于 1000 的请求
 * 此实现已被 ConfigurablePolicyEngine 替代，保留用于向后兼容
 *
 * @author zhuhx
 */
@Component("hardcodedPolicyEngine")
public class HardcodedPolicyEngine implements PolicyEngine {

    /** 转账路径关键字 */
    private static final String TRANSFER_PATH = "/transfer";

    /** 大额转账阈值 */
    private static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("1000");

    /** 拦截消息 */
    private static final String BLOCK_MESSAGE = "等待审批：大额转账操作需要人工审批";

    @Override
    public PolicyResult evaluate(String targetUrl, String method, Map<String, String> headers, Map<String, Object> body, String agentId) {
        // 检查是否为转账请求且金额超过阈值
        if (isTransferRequest(targetUrl) && isLargeAmount(body)) {
            return PolicyResult.block(BLOCK_MESSAGE);
        }
        return PolicyResult.allow();
    }

    @Override
    public void refreshPolicies() {
        // 硬编码引擎无需刷新策略
    }

    /**
     * 检查是否为转账请求
     *
     * @param targetUrl 目标URL
     * @return 是否包含 /transfer 路径
     */
    private boolean isTransferRequest(String targetUrl) {
        return StrUtil.contains(targetUrl, TRANSFER_PATH);
    }

    /**
     * 检查是否为大额转账
     *
     * @param body 请求体
     * @return 金额是否大于阈值
     */
    private boolean isLargeAmount(Map<String, Object> body) {
        if (CollUtil.isEmpty(body) || !body.containsKey("amount")) {
            return false;
        }

        BigDecimal amount = parseAmount(body.get("amount"));
        return amount != null && amount.compareTo(AMOUNT_THRESHOLD) > 0;
    }

    /**
     * 解析金额值
     *
     * @param amountObj 金额对象
     * @return 解析后的BigDecimal，解析失败返回null
     */
    private BigDecimal parseAmount(Object amountObj) {
        if (amountObj == null) {
            return null;
        }

        try {
            if (amountObj instanceof Number) {
                return NumberUtil.toBigDecimal((Number) amountObj);
            } else if (amountObj instanceof String && NumberUtil.isNumber((String) amountObj)) {
                return NumberUtil.toBigDecimal((String) amountObj);
            }
        } catch (Exception e) {
            // 解析失败，返回null
            return null;
        }
        return null;
    }
}
