package com.agentguard.alert.service;

import com.agentguard.alert.dto.AlertDTO;

/**
 * 告警服务接口
 *
 * @author zhuhx
 */
public interface AlertService {

    /**
     * 发送告警
     *
     * @param alert 告警信息
     * @return 告警记录ID
     */
    String sendAlert(AlertDTO alert);

    /**
     * 检查成本告警
     * 检查所有预算配置，如果成本超过阈值则发送告警
     */
    void checkCostAlerts();

    /**
     * 检查异常告警
     * 检查 Agent 错误率，如果超过阈值则发送告警
     *
     * @param windowMinutes      时间窗口（分钟）
     * @param errorRateThreshold 错误率阈值（0-1）
     */
    void checkErrorRateAlerts(int windowMinutes, double errorRateThreshold);

    /**
     * 发送审批提醒
     * 检查即将过期的审批请求并发送提醒
     *
     * @param reminderMinutes 提前提醒时间（分钟）
     */
    void sendApprovalReminders(int reminderMinutes);
}
