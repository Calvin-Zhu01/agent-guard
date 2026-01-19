package com.agentguard.policy.service;

import com.agentguard.policy.dto.RateLimitResult;

import java.util.Map;

/**
 * 频率限制服务接口
 * 
 * 基于 Redis 实现滑动窗口限流
 *
 * @author zhuhx
 */
public interface RateLimiterService {

    /**
     * 检查是否允许请求通过
     *
     * @param key           限流键（由 keyExtractor 生成）
     * @param windowSeconds 时间窗口（秒）
     * @param maxRequests   最大请求数
     * @return 限流结果
     */
    RateLimitResult checkLimit(String key, int windowSeconds, int maxRequests);

    /**
     * 从请求中提取限流键
     *
     * @param keyExtractor 键提取器配置（如 "header:X-Agent-Id"、"body:userId"、"ip"）
     * @param headers      请求头
     * @param body         请求体
     * @param clientIp     客户端IP
     * @return 限流键
     */
    String extractKey(String keyExtractor, Map<String, String> headers,
                      Map<String, Object> body, String clientIp);

    /**
     * 检查 URL 是否匹配限流模式
     *
     * @param url        请求 URL
     * @param urlPattern URL 匹配模式（支持通配符 *）
     * @return 是否匹配
     */
    boolean matchUrl(String url, String urlPattern);
}
