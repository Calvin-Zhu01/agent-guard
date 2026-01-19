package com.agentguard.policy.engine;

import com.agentguard.policy.dto.PolicyResult;

import java.util.Map;

/**
 * 策略引擎接口
 * 
 * 用于评估请求是否应该被拦截，支持配置化策略加载
 *
 * @author zhuhx
 */
public interface PolicyEngine {

    /**
     * 评估请求是否应该被拦截
     *
     * @param targetUrl 目标URL
     * @param method HTTP方法
     * @param headers 请求头
     * @param body 请求体
     * @param agentId Agent ID（可选，用于Agent级别策略）
     * @return 策略评估结果
     */
    PolicyResult evaluate(String targetUrl, String method, Map<String, String> headers, Map<String, Object> body, String agentId);

    /**
     * 评估请求是否应该被拦截（兼容旧接口）
     *
     * @param targetUrl 目标URL
     * @param method HTTP方法
     * @param headers 请求头
     * @param body 请求体
     * @return 策略评估结果
     */
    default PolicyResult evaluate(String targetUrl, String method, Map<String, String> headers, Map<String, Object> body) {
        return evaluate(targetUrl, method, headers, body, null);
    }

    /**
     * 刷新策略配置
     * 
     * 从数据源重新加载策略规则，用于策略变更后的热更新
     */
    void refreshPolicies();
}
