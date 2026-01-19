package com.agentguard.alert.scheduler;

import com.agentguard.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 告警定时任务调度器
 * 
 * 负责定时执行各类告警检查任务：
 * - 成本告警检查
 * - 错误率告警检查
 * - 审批过期提醒
 * - 系统健康检查
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "alert.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class AlertScheduler {

    private final AlertService alertService;

    /** 错误率检查时间窗口（分钟） */
    @Value("${alert.scheduler.error-rate.window-minutes:30}")
    private int errorRateWindowMinutes;

    /** 错误率告警阈值（0-1） */
    @Value("${alert.scheduler.error-rate.threshold:0.1}")
    private double errorRateThreshold;

    /** 审批过期提醒时间（分钟） */
    @Value("${alert.scheduler.approval.reminder-minutes:30}")
    private int approvalReminderMinutes;

    /**
     * 成本告警检查定时任务
     * 
     * 默认每小时执行一次，检查预算使用情况
     * 可通过 alert.scheduler.cost.cron 配置调整执行频率
     */
    @Scheduled(cron = "${alert.scheduler.cost.cron:0 0 * * * ?}")
    public void checkCostAlerts() {
        log.info("开始执行成本告警检查定时任务...");
        try {
            alertService.checkCostAlerts();
            log.info("成本告警检查定时任务执行完成");
        } catch (Exception e) {
            log.error("成本告警检查定时任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 错误率告警检查定时任务
     * 
     * 默认每5分钟执行一次，检查系统错误率
     * 可通过 alert.scheduler.error-rate.cron 配置调整执行频率
     */
    @Scheduled(cron = "${alert.scheduler.error-rate.cron:0 */5 * * * ?}")
    public void checkErrorRateAlerts() {
        log.info("开始执行错误率告警检查定时任务...");
        try {
            alertService.checkErrorRateAlerts(errorRateWindowMinutes, errorRateThreshold);
            log.info("错误率告警检查定时任务执行完成");
        } catch (Exception e) {
            log.error("错误率告警检查定时任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 审批过期提醒定时任务
     * 
     * 默认每10分钟执行一次，检查即将过期的审批请求
     * 可通过 alert.scheduler.approval.cron 配置调整执行频率
     */
    @Scheduled(cron = "${alert.scheduler.approval.cron:0 */10 * * * ?}")
    public void sendApprovalReminders() {
        log.info("开始执行审批过期提醒定时任务...");
        try {
            alertService.sendApprovalReminders(approvalReminderMinutes);
            log.info("审批过期提醒定时任务执行完成");
        } catch (Exception e) {
            log.error("审批过期提醒定时任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 系统健康检查定时任务
     * 
     * 默认每分钟执行一次，检查系统组件健康状态
     * 可通过 alert.scheduler.health.cron 配置调整执行频率
     */
    @Scheduled(cron = "${alert.scheduler.health.cron:0 * * * * ?}")
    public void checkSystemHealth() {
        log.info("开始执行系统健康检查定时任务...");
        try {
            alertService.checkSystemHealth();
            log.info("系统健康检查定时任务执行完成");
        } catch (Exception e) {
            log.error("系统健康检查定时任务执行失败: {}", e.getMessage(), e);
        }
    }
}
