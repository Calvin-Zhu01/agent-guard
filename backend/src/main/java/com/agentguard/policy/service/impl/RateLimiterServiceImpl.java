package com.agentguard.policy.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.agentguard.policy.dto.RateLimitResult;
import com.agentguard.policy.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 频率限制服务实现
 * 
 * 使用 Redis 滑动窗口算法实现限流
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterServiceImpl implements RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    /** 限流键前缀 */
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    /** 默认限流键 */
    private static final String DEFAULT_KEY = "default";

    /**
     * 滑动窗口限流 Lua 脚本
     * 
     * 使用 Redis 的 ZSET 实现滑动窗口：
     * 1. 移除窗口外的旧记录
     * 2. 统计当前窗口内的请求数
     * 3. 如果未超限，添加新记录
     * 4. 返回当前计数
     */
    private static final String SLIDING_WINDOW_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local windowStart = now - window * 1000
            
            -- 移除窗口外的旧记录
            redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
            
            -- 统计当前窗口内的请求数
            local count = redis.call('ZCARD', key)
            
            if count < limit then
                -- 未超限，添加新记录（使用当前时间戳作为 score 和 member）
                redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))
                -- 设置过期时间为窗口大小
                redis.call('EXPIRE', key, window)
                return {1, count + 1}
            else
                -- 超限，返回当前计数
                return {0, count}
            end
            """;

    private final DefaultRedisScript<java.util.List> slidingWindowScript = new DefaultRedisScript<>(
            SLIDING_WINDOW_SCRIPT, java.util.List.class);

    @Override
    public RateLimitResult checkLimit(String key, int windowSeconds, int maxRequests) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + key;
        long now = System.currentTimeMillis();
        long resetTime = now + windowSeconds * 1000L;

        try {
            @SuppressWarnings("unchecked")
            java.util.List<Long> result = redisTemplate.execute(
                    slidingWindowScript,
                    Collections.singletonList(redisKey),
                    String.valueOf(now),
                    String.valueOf(windowSeconds),
                    String.valueOf(maxRequests)
            );

            if (ObjectUtil.isNull(result) || result.size() < 2) {
                // Redis 执行失败，降级为允许通过
                log.warn("Redis 限流脚本执行失败，降级为允许通过，key: {}", key);
                return RateLimitResult.allow(0, maxRequests, resetTime);
            }

            boolean allowed = result.get(0) == 1;
            long currentCount = result.get(1);
            long remaining = Math.max(0, maxRequests - currentCount);

            if (allowed) {
                return RateLimitResult.allow(currentCount, remaining, resetTime);
            } else {
                return RateLimitResult.deny(currentCount, resetTime,
                        String.format("请求频率超过限制，当前窗口(%d秒)内已达到最大请求数(%d)", 
                                windowSeconds, maxRequests));
            }
        } catch (Exception e) {
            // Redis 连接失败，降级为允许通过
            log.error("Redis 限流检查失败，降级为允许通过，key: {}, error: {}", key, e.getMessage());
            return RateLimitResult.allow(0, maxRequests, resetTime);
        }
    }

    @Override
    public String extractKey(String keyExtractor, Map<String, String> headers,
                             Map<String, Object> body, String clientIp) {
        if (StrUtil.isBlank(keyExtractor)) {
            return DEFAULT_KEY;
        }

        String extractor = keyExtractor.trim().toLowerCase();

        // 支持 ip 提取客户端 IP
        if ("ip".equals(extractor)) {
            return StrUtil.isNotBlank(clientIp) ? clientIp : DEFAULT_KEY;
        }

        // 支持 header:X-Agent-Id 格式提取请求头
        if (extractor.startsWith("header:")) {
            String headerName = keyExtractor.substring(7).trim();
            if (ObjectUtil.isNotNull(headers)) {
                // 请求头名称不区分大小写
                String value = findHeaderValue(headers, headerName);
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            }
            log.debug("未找到请求头 {}，使用默认键", headerName);
            return DEFAULT_KEY;
        }

        // 支持 body:fieldName 格式提取请求体字段
        if (extractor.startsWith("body:")) {
            String fieldName = keyExtractor.substring(5).trim();
            if (ObjectUtil.isNotNull(body)) {
                Object value = getNestedValue(body, fieldName);
                if (ObjectUtil.isNotNull(value)) {
                    return String.valueOf(value);
                }
            }
            log.debug("未找到请求体字段 {}，使用默认键", fieldName);
            return DEFAULT_KEY;
        }

        log.warn("不支持的 keyExtractor 格式: {}，使用默认键", keyExtractor);
        return DEFAULT_KEY;
    }

    @Override
    public boolean matchUrl(String url, String urlPattern) {
        if (StrUtil.isBlank(urlPattern)) {
            // 没有配置 URL 模式，匹配所有 URL
            return true;
        }

        if (StrUtil.isBlank(url)) {
            return false;
        }

        // 将通配符模式转换为正则表达式
        String regex = urlPattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*");

        try {
            return Pattern.matches(regex, url);
        } catch (Exception e) {
            log.warn("URL 模式匹配失败，pattern: {}, url: {}, error: {}", 
                    urlPattern, url, e.getMessage());
            return false;
        }
    }

    /**
     * 查找请求头值（不区分大小写）
     */
    private String findHeaderValue(Map<String, String> headers, String headerName) {
        // 先尝试精确匹配
        String value = headers.get(headerName);
        if (StrUtil.isNotBlank(value)) {
            return value;
        }

        // 不区分大小写匹配
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * 获取嵌套字段值（支持点号分隔的路径，如 "user.id"）
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String fieldPath) {
        if (ObjectUtil.isNull(map) || StrUtil.isBlank(fieldPath)) {
            return null;
        }

        String[] parts = fieldPath.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }

            if (ObjectUtil.isNull(current)) {
                return null;
            }
        }

        return current;
    }
}
