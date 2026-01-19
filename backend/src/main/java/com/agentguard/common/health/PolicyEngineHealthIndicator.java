package com.agentguard.common.health;

import com.agentguard.policy.dto.PolicyDTO;
import com.agentguard.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 策略引擎健康指示器
 * 
 * 检查策略引擎状态，包括策略加载情况和引擎可用性
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyEngineHealthIndicator implements HealthIndicator {

    private final PolicyService policyService;

    @Override
    public Health health() {
        try {
            // 尝试获取启用的策略列表，验证策略引擎是否正常工作
            List<PolicyDTO> enabledPolicies = policyService.getEnabledPolicies();
            
            int enabledCount = enabledPolicies.size();
            
            // 统计各类型策略数量
            long accessControlCount = enabledPolicies.stream()
                    .filter(p -> p.getType() != null && "ACCESS_CONTROL".equals(p.getType().name()))
                    .count();
            long contentProtectionCount = enabledPolicies.stream()
                    .filter(p -> p.getType() != null && "CONTENT_PROTECTION".equals(p.getType().name()))
                    .count();
            long rateLimitCount = enabledPolicies.stream()
                    .filter(p -> p.getType() != null && "RATE_LIMIT".equals(p.getType().name()))
                    .count();
            long approvalCount = enabledPolicies.stream()
                    .filter(p -> p.getType() != null && "APPROVAL".equals(p.getType().name()))
                    .count();

            return Health.up()
                    .withDetail("status", "策略引擎运行正常")
                    .withDetail("enabledPolicies", enabledCount)
                    .withDetail("accessControlPolicies", accessControlCount)
                    .withDetail("contentProtectionPolicies", contentProtectionCount)
                    .withDetail("rateLimitPolicies", rateLimitCount)
                    .withDetail("approvalPolicies", approvalCount)
                    .build();
        } catch (Exception e) {
            log.error("策略引擎健康检查失败: {}", e.getMessage(), e);
            return Health.down()
                    .withDetail("error", "策略引擎异常: " + e.getMessage())
                    .build();
        }
    }
}
