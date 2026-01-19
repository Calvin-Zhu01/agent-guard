package com.agentguard.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 限流结果数据模型
 *
 * @author zhuhx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "限流结果")
public class RateLimitResult {

    /** 是否允许通过 */
    @Schema(description = "是否允许通过", example = "true")
    private boolean allowed;

    /** 当前请求计数 */
    @Schema(description = "当前请求计数", example = "5")
    private long currentCount;

    /** 剩余可用次数 */
    @Schema(description = "剩余可用次数", example = "95")
    private long remaining;

    /** 重置时间（Unix 时间戳，毫秒） */
    @Schema(description = "重置时间（Unix 时间戳，毫秒）", example = "1704067200000")
    private long resetTime;

    /** 限流原因（如果被限流） */
    @Schema(description = "限流原因（如果被限流）", example = "请求频率超过限制")
    private String reason;

    /**
     * 创建允许通过的结果
     *
     * @param currentCount 当前计数
     * @param remaining    剩余次数
     * @param resetTime    重置时间
     * @return 限流结果
     */
    public static RateLimitResult allow(long currentCount, long remaining, long resetTime) {
        return RateLimitResult.builder()
                .allowed(true)
                .currentCount(currentCount)
                .remaining(remaining)
                .resetTime(resetTime)
                .build();
    }

    /**
     * 创建拒绝的结果
     *
     * @param currentCount 当前计数
     * @param resetTime    重置时间
     * @param reason       拒绝原因
     * @return 限流结果
     */
    public static RateLimitResult deny(long currentCount, long resetTime, String reason) {
        return RateLimitResult.builder()
                .allowed(false)
                .currentCount(currentCount)
                .remaining(0)
                .resetTime(resetTime)
                .reason(reason)
                .build();
    }
}
