package com.agentguard.alert.scheduler;

import com.agentguard.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 告警定时任务调度器
 *
 * 采用"高频检测 + 通知冷却"的设计模式：
 * - 定时任务固定为高频率（每1分钟），确保及时发现问题
 * - 通知频率由系统设置中的冷却时间控制，避免重复通知
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alert.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class AlertScheduler {

    private final AlertService alertService;

    /**
     * 统一告警检查定时任务
     *
     * 固定每1分钟执行一次，检查所有类型的告警
     * 通知频率由系统设置中的冷却时间控制
     */
    @Scheduled(fixedRate = 60000)
    public void checkAllAlerts() {
        log.debug("开始执行告警检查定时任务...");

        try {
            // 检查成本告警
            alertService.checkCostAlerts();

            // 检查错误率告警-默认参数
            alertService.checkErrorRateAlerts(30, 0.1);

            // 发送审批提醒-默认参数
            alertService.sendApprovalReminders(30);

            log.debug("告警检查定时任务执行完成");
        } catch (Exception e) {
            log.error("告警检查定时任务执行失败: {}", e.getMessage(), e);
        }
    }
}
