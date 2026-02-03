package com.agentguard.alert.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.agentguard.alert.channel.NotificationChannel;
import com.agentguard.alert.channel.NotificationChannelFactory;
import com.agentguard.alert.dto.AlertDTO;
import com.agentguard.alert.entity.AlertHistoryDO;
import com.agentguard.alert.enums.AlertStatus;
import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.mapper.AlertHistoryMapper;
import com.agentguard.alert.service.AlertService;
import com.agentguard.approval.entity.ApprovalRequestDO;
import com.agentguard.approval.enums.ApprovalStatus;
import com.agentguard.approval.mapper.ApprovalMapper;
import com.agentguard.budget.dto.BudgetWithUsageDTO;
import com.agentguard.budget.service.BudgetService;
import com.agentguard.log.entity.AgentLogDO;
import com.agentguard.log.enums.ResponseStatus;
import com.agentguard.log.mapper.AgentLogMapper;
import com.agentguard.settings.service.SystemSettingsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.agentguard.alert.enums.NotificationChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警服务实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertHistoryMapper alertHistoryMapper;
    private final NotificationChannelFactory channelFactory;
    private final BudgetService budgetService;
    private final AgentLogMapper agentLogMapper;
    private final ApprovalMapper approvalMapper;
    private final SystemSettingsService systemSettingsService;

    @Value("${alert.default-recipient:admin@agentguard.com}")
    private String defaultRecipient;

    @Override
    public String sendAlert(AlertDTO alert) {
        log.info("发送告警: type={}, title={}", alert.getType(), alert.getTitle());

        // 获取通知渠道
        NotificationChannel channel = channelFactory.getChannel(alert.getChannelType());

        // 发送通知
        boolean success = channel.send(alert.getRecipient(), alert.getTitle(), alert.getContent());

        // 记录告警历史
        AlertHistoryDO history = new AlertHistoryDO();
        history.setRuleId(alert.getRuleId());
        history.setType(alert.getType());
        history.setTitle(alert.getTitle());
        history.setContent(alert.getContent());
        history.setRecipient(alert.getRecipient());
        history.setChannelType(alert.getChannelType());
        history.setStatus(success ? AlertStatus.SUCCESS : AlertStatus.FAILED);
        if (!success) {
            history.setErrorMessage("通知发送失败");
        }

        alertHistoryMapper.insert(history);

        log.info("告警记录已保存: id={}, status={}", history.getId(), history.getStatus());
        return history.getId();
    }


    @Override
    public void checkCostAlerts() {
        log.debug("开始检查成本告警...");

        // 从系统设置获取告警配置
        var alertSettings = systemSettingsService.getAlertSettings();

        // 检查成本告警是否启用
        if (!Boolean.TRUE.equals(alertSettings.getCostAlertEnabled())) {
            log.debug("成本告警未启用");
            return;
        }

        // 获取当前预算使用情况
        BudgetWithUsageDTO currentBudget = budgetService.getCurrentBudget();

        if (ObjectUtil.isNull(currentBudget.getId())) {
            log.debug("未设置预算，跳过成本告警检查");
            return;
        }

        // 获取邮件配置中的默认收件人
        var emailSettings = systemSettingsService.getEmailSettings();
        String recipient = emailSettings.getDefaultRecipients();
        if (StrUtil.isBlank(recipient)) {
            recipient = defaultRecipient;
        }

        BigDecimal usagePercentage = currentBudget.getUsagePercentage();

        // 使用系统设置中的阈值（转换为小数，如85% -> 0.85）
        BigDecimal threshold = new BigDecimal(alertSettings.getCostThreshold()).divide(new BigDecimal("100"));

        // 检查是否超过阈值
        if (usagePercentage.compareTo(threshold) >= 0) {
            String title = StrUtil.format("【成本告警】{}月预算使用已达{}%",
                    currentBudget.getMonth(),
                    usagePercentage.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));

            String content = StrUtil.format(
                    "预算告警通知\n\n" +
                    "月份：{}\n" +
                    "预算上限：{}\n" +
                    "已使用金额：{}\n" +
                    "使用百分比：{}%\n" +
                    "告警阈值：{}%\n" +
                    "剩余金额：{}\n\n" +
                    "请及时关注成本使用情况。",
                    currentBudget.getMonth(),
                    currentBudget.getLimitAmount(),
                    currentBudget.getUsedAmount(),
                    usagePercentage.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                    alertSettings.getCostThreshold(),
                    currentBudget.getRemainingAmount());

            // 发送告警
            AlertDTO alert = new AlertDTO();
            alert.setType(AlertType.COST);
            alert.setTitle(title);
            alert.setContent(content);
            alert.setRecipient(recipient);
            alert.setChannelType(NotificationChannelType.EMAIL);

            sendAlert(alert);
        }

        // 检查是否超预算
        if (currentBudget.getOverBudget()) {
            String title = StrUtil.format("【预算超支】{}月已超出预算！", currentBudget.getMonth());

            String content = StrUtil.format(
                    "预算超支告警\n\n" +
                    "月份：{}\n" +
                    "预算上限：{}\n" +
                    "已使用金额：{}\n" +
                    "超支金额：{}\n\n" +
                    "请立即采取措施控制成本！",
                    currentBudget.getMonth(),
                    currentBudget.getLimitAmount(),
                    currentBudget.getUsedAmount(),
                    currentBudget.getUsedAmount().subtract(currentBudget.getLimitAmount()));

            // 发送告警
            AlertDTO alert = new AlertDTO();
            alert.setType(AlertType.COST);
            alert.setTitle(title);
            alert.setContent(content);
            alert.setRecipient(recipient);
            alert.setChannelType(NotificationChannelType.EMAIL);

            sendAlert(alert);
        }

        log.debug("成本告警检查完成");
    }


    @Override
    public void checkErrorRateAlerts(int windowMinutes, double errorRateThreshold) {
        log.debug("开始检查错误率告警: windowMinutes={}, threshold={}", windowMinutes, errorRateThreshold);

        // 从系统设置获取告警配置
        var alertSettings = systemSettingsService.getAlertSettings();

        // 检查错误率告警是否启用
        if (!Boolean.TRUE.equals(alertSettings.getErrorRateAlertEnabled())) {
            log.debug("错误率告警未启用");
            return;
        }

        // 使用系统设置中的时间窗口和阈值（如果配置了的话）
        int effectiveWindowMinutes = alertSettings.getErrorRateWindow() != null
            ? alertSettings.getErrorRateWindow()
            : windowMinutes;

        double effectiveThreshold = alertSettings.getErrorRateThreshold() != null
            ? alertSettings.getErrorRateThreshold() / 100.0  // 转换为小数，如10% -> 0.1
            : errorRateThreshold;

        // 计算时间窗口
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(effectiveWindowMinutes);

        // 查询时间窗口内的所有日志
        LambdaQueryWrapper<AgentLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(AgentLogDO::getCreatedAt, startTime)
               .le(AgentLogDO::getCreatedAt, endTime);

        List<AgentLogDO> logs = agentLogMapper.selectList(wrapper);

        if (logs.isEmpty()) {
            log.debug("时间窗口内没有日志记录");
            return;
        }

        // 计算总请求数和失败请求数
        long totalRequests = logs.size();
        long failedRequests = logs.stream()
                .filter(log -> ResponseStatus.FAILED.equals(log.getResponseStatus()))
                .count();

        // 计算错误率
        double errorRate = (double) failedRequests / totalRequests;

        log.debug("错误率统计: total={}, failed={}, errorRate={}", totalRequests, failedRequests, errorRate);

        // 检查是否超过阈值
        if (errorRate >= effectiveThreshold) {
            // 获取邮件配置中的默认收件人
            var emailSettings = systemSettingsService.getEmailSettings();
            String recipient = emailSettings.getDefaultRecipients();
            if (StrUtil.isBlank(recipient)) {
                recipient = defaultRecipient;
            }

            String title = StrUtil.format("【异常告警】系统错误率已达{}%",
                    String.format("%.2f", errorRate * 100));

            String content = StrUtil.format(
                    "系统异常告警\n\n" +
                    "时间窗口：最近{}分钟\n" +
                    "总请求数：{}\n" +
                    "失败请求数：{}\n" +
                    "当前错误率：{}%\n" +
                    "告警阈值：{}%\n\n" +
                    "请及时排查系统异常！",
                    effectiveWindowMinutes,
                    totalRequests,
                    failedRequests,
                    String.format("%.2f", errorRate * 100),
                    String.format("%.0f", effectiveThreshold * 100));

            // 发送告警
            AlertDTO alert = new AlertDTO();
            alert.setType(AlertType.ERROR_RATE);
            alert.setTitle(title);
            alert.setContent(content);
            alert.setRecipient(recipient);
            alert.setChannelType(NotificationChannelType.EMAIL);

            sendAlert(alert);
        }

        log.debug("错误率告警检查完成");
    }


    @Override
    public void sendApprovalReminders(int reminderMinutes) {
        log.debug("开始发送审批提醒: reminderMinutes={}", reminderMinutes);

        // 从系统设置获取告警配置
        var alertSettings = systemSettingsService.getAlertSettings();

        // 检查审批提醒是否启用
        if (!Boolean.TRUE.equals(alertSettings.getApprovalReminderEnabled())) {
            log.debug("审批提醒未启用");
            return;
        }

        // 使用系统设置中的提醒时间（如果配置了的话）
        int effectiveReminderMinutes = alertSettings.getApprovalReminderMinutes() != null
            ? alertSettings.getApprovalReminderMinutes()
            : reminderMinutes;

        // 计算即将过期的时间范围
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(effectiveReminderMinutes);

        // 查询即将过期的待审批请求
        LambdaQueryWrapper<ApprovalRequestDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRequestDO::getStatus, ApprovalStatus.PENDING)
               .le(ApprovalRequestDO::getExpiresAt, reminderTime)
               .gt(ApprovalRequestDO::getExpiresAt, now);

        List<ApprovalRequestDO> pendingApprovals = approvalMapper.selectList(wrapper);

        if (pendingApprovals.isEmpty()) {
            log.debug("没有即将过期的审批请求");
            return;
        }

        log.info("发现{}个即将过期的审批请求", pendingApprovals.size());

        // 获取邮件配置中的默认收件人
        var emailSettings = systemSettingsService.getEmailSettings();
        String recipient = emailSettings.getDefaultRecipients();
        if (StrUtil.isBlank(recipient)) {
            recipient = defaultRecipient;
        }

        for (ApprovalRequestDO approval : pendingApprovals) {
            String title = "【审批提醒】审批请求即将过期";

            // 计算剩余时间
            long remainingMinutes = java.time.Duration.between(now, approval.getExpiresAt()).toMinutes();

            String content = StrUtil.format(
                    "审批过期提醒\n\n" +
                    "审批ID：{}\n" +
                    "Agent ID：{}\n" +
                    "策略ID：{}\n" +
                    "创建时间：{}\n" +
                    "过期时间：{}\n" +
                    "剩余时间：{}分钟\n\n" +
                    "请尽快处理该审批请求！",
                    approval.getId(),
                    approval.getAgentId(),
                    approval.getPolicyId(),
                    approval.getCreatedAt(),
                    approval.getExpiresAt(),
                    remainingMinutes);

            // 发送告警
            AlertDTO alert = new AlertDTO();
            alert.setType(AlertType.APPROVAL);
            alert.setTitle(title);
            alert.setContent(content);
            alert.setRecipient(recipient);
            alert.setChannelType(NotificationChannelType.EMAIL);

            sendAlert(alert);
        }

        // 同时发送新的待审批请求提醒
        sendNewApprovalReminders(recipient);

        log.debug("审批提醒发送完成");
    }

    /**
     * 发送新的待审批请求提醒
     *
     * @param recipient 接收人邮箱
     */
    private void sendNewApprovalReminders(String recipient) {
        // 查询最近创建的待审批请求（最近5分钟内创建的）
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        LambdaQueryWrapper<ApprovalRequestDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRequestDO::getStatus, ApprovalStatus.PENDING)
               .ge(ApprovalRequestDO::getCreatedAt, fiveMinutesAgo);

        List<ApprovalRequestDO> newApprovals = approvalMapper.selectList(wrapper);

        if (newApprovals.isEmpty()) {
            return;
        }

        log.info("发现{}个新的待审批请求", newApprovals.size());

        for (ApprovalRequestDO approval : newApprovals) {
            String title = "【审批提醒】有新的审批请求待处理";

            String content = StrUtil.format(
                    "新审批请求通知\n\n" +
                    "审批ID：{}\n" +
                    "Agent ID：{}\n" +
                    "策略ID：{}\n" +
                    "创建时间：{}\n" +
                    "过期时间：{}\n\n" +
                    "请及时处理该审批请求。",
                    approval.getId(),
                    approval.getAgentId(),
                    approval.getPolicyId(),
                    approval.getCreatedAt(),
                    approval.getExpiresAt());

            // 发送告警
            AlertDTO alert = new AlertDTO();
            alert.setType(AlertType.APPROVAL);
            alert.setTitle(title);
            alert.setContent(content);
            alert.setRecipient(recipient);
            alert.setChannelType(NotificationChannelType.EMAIL);

            sendAlert(alert);
        }
    }
}
