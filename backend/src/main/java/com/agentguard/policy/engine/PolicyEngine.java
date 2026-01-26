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
     * @param clientIp 客户端IP（用于频率限制）
     * @param requestType 请求类型（LLM_CALL/API_CALL/ALL）
     * @return 策略评估结果
     */
    PolicyResult evaluate(String targetUrl, String method, Map<String, String> headers,
                          Map<String, Object> body, String agentId, String clientIp,
                          com.agentguard.log.enums.RequestType requestType);

    /**
     * 刷新策略配置
     *
     * 从数据源重新加载策略规则，用于策略变更后的热更新
     */
    void refreshPolicies();
}
