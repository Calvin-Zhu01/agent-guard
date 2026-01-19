package com.agentguard.common.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Redis 健康指示器
 * 
 * 检查 Redis 连接状态，包括连接可用性和基本信息
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            try {
                // 执行 PING 命令检查连接
                String pong = connection.ping();
                if (!"PONG".equals(pong)) {
                    return Health.down()
                            .withDetail("error", "Redis PING 响应异常: " + pong)
                            .build();
                }

                // 获取 Redis 服务器信息
                Properties info = connection.serverCommands().info("server");
                
                Health.Builder builder = Health.up()
                        .withDetail("ping", "PONG");

                if (info != null) {
                    // 添加 Redis 版本信息
                    String redisVersion = info.getProperty("redis_version");
                    if (redisVersion != null) {
                        builder.withDetail("version", redisVersion);
                    }
                    
                    // 添加运行模式
                    String redisMode = info.getProperty("redis_mode");
                    if (redisMode != null) {
                        builder.withDetail("mode", redisMode);
                    }
                }

                return builder.build();
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            log.error("Redis 健康检查失败: {}", e.getMessage(), e);
            return Health.down()
                    .withDetail("error", "Redis 连接失败: " + e.getMessage())
                    .build();
        }
    }
}
