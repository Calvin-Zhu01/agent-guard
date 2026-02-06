package com.agentguard.alert.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.agentguard.agent.entity.AgentDO;
import com.agentguard.agent.mapper.AgentMapper;
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
import com.agentguard.policy.entity.PolicyDO;
import com.agentguard.policy.mapper.PolicyMapper;
import com.agentguard.settings.dto.AlertSettingsDTO;
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
import java.time.format.DateTimeFormatter;
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
    private final AgentMapper agentMapper;
    private final PolicyMapper policyMapper;
    private final SystemSettingsService systemSettingsService;

    @Value("${alert.default-recipient:admin@agentguard.com}")
    private String defaultRecipient;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /** 时间格式化器 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        history.setChannelType(alert.getChannelType().getCode());
        history.setStatus(success ? AlertStatus.SUCCESS : AlertStatus.FAILED);
        history.setSentAt(LocalDateTime.now());
        if (!success) {
            history.setErrorMessage("通知发送失败");
        }

        alertHistoryMapper.insert(history);

        log.info("告警记录已保存: id={}, status={}", history.getId(), history.getStatus());
        return history.getId();
    }

    /**
     * 发送告警到所有启用的通知渠道
     *
     * @param type 告警类型
     * @param title 告警标题
     * @param content 告警内容
     * @param recipient 收件人（用于邮件通知）
     */
    private void sendAlertToAllChannels(AlertType type, String title, String content, String recipient) {
        // 获取webhook配置
        var webhookSettings = systemSettingsService.getWebhookSettings();
        var emailSettings = systemSettingsService.getEmailSettings();

        // 收集所有启用的渠道和发送结果
        List<String> enabledChannels = new java.util.ArrayList<>();
        List<String> successChannels = new java.util.ArrayList<>();
        List<String> failedChannels = new java.util.ArrayList<>();
        StringBuilder errorMessages = new StringBuilder();

        // 发送邮件通知
        if (Boolean.TRUE.equals(emailSettings.getEnabled()) && StrUtil.isNotBlank(recipient)) {
            enabledChannels.add(NotificationChannelType.EMAIL.getCode());
            try {
                NotificationChannel channel = channelFactory.getChannel(NotificationChannelType.EMAIL);
                boolean success = channel.send(recipient, title, content);
                if (success) {
                    successChannels.add(NotificationChannelType.EMAIL.getCode());
                } else {
                    failedChannels.add(NotificationChannelType.EMAIL.getCode());
                    errorMessages.append("EMAIL发送失败; ");
                }
            } catch (Exception e) {
                failedChannels.add(NotificationChannelType.EMAIL.getCode());
                errorMessages.append("EMAIL发送异常: ").append(e.getMessage()).append("; ");
                log.error("发送邮件通知失败", e);
            }
        }

        // 发送企业微信通知
        if (Boolean.TRUE.equals(webhookSettings.getWeComEnabled()) && StrUtil.isNotBlank(webhookSettings.getWeComWebhook())) {
            enabledChannels.add(NotificationChannelType.WECOM.getCode());
            try {
                NotificationChannel channel = channelFactory.getChannel(NotificationChannelType.WECOM);
                boolean success = channel.send(webhookSettings.getWeComWebhook(), title, content);
                if (success) {
                    successChannels.add(NotificationChannelType.WECOM.getCode());
                } else {
                    failedChannels.add(NotificationChannelType.WECOM.getCode());
                    errorMessages.append("WECOM发送失败; ");
                }
            } catch (Exception e) {
                failedChannels.add(NotificationChannelType.WECOM.getCode());
                errorMessages.append("WECOM发送异常: ").append(e.getMessage()).append("; ");
                log.error("发送企业微信通知失败", e);
            }
        }

        // 发送钉钉通知
        if (Boolean.TRUE.equals(webhookSettings.getDingTalkEnabled()) && StrUtil.isNotBlank(webhookSettings.getDingTalkWebhook())) {
            enabledChannels.add(NotificationChannelType.DINGTALK.getCode());
            try {
                NotificationChannel channel = channelFactory.getChannel(NotificationChannelType.DINGTALK);
                boolean success = channel.send(webhookSettings.getDingTalkWebhook(), title, content);
                if (success) {
                    successChannels.add(NotificationChannelType.DINGTALK.getCode());
                } else {
                    failedChannels.add(NotificationChannelType.DINGTALK.getCode());
                    errorMessages.append("DINGTALK发送失败; ");
                }
            } catch (Exception e) {
                failedChannels.add(NotificationChannelType.DINGTALK.getCode());
                errorMessages.append("DINGTALK发送异常: ").append(e.getMessage()).append("; ");
                log.error("发送钉钉通知失败", e);
            }
        }

        // 发送自定义Webhook通知
        if (Boolean.TRUE.equals(webhookSettings.getCustomWebhookEnabled()) && StrUtil.isNotBlank(webhookSettings.getCustomWebhookUrl())) {
            enabledChannels.add(NotificationChannelType.WEBHOOK.getCode());
            try {
                NotificationChannel channel = channelFactory.getChannel(NotificationChannelType.WEBHOOK);
                boolean success = channel.send(webhookSettings.getCustomWebhookUrl(), title, content);
                if (success) {
                    successChannels.add(NotificationChannelType.WEBHOOK.getCode());
                } else {
                    failedChannels.add(NotificationChannelType.WEBHOOK.getCode());
                    errorMessages.append("WEBHOOK发送失败; ");
                }
            } catch (Exception e) {
                failedChannels.add(NotificationChannelType.WEBHOOK.getCode());
                errorMessages.append("WEBHOOK发送异常: ").append(e.getMessage()).append("; ");
                log.error("发送Webhook通知失败", e);
            }
        }

        // 如果有启用的渠道，创建一条告警历史记录
        if (!enabledChannels.isEmpty()) {
            AlertHistoryDO history = new AlertHistoryDO();
            history.setType(type);
            history.setTitle(title);
            history.setContent(content);
            history.setRecipient(recipient);
            history.setChannelType(String.join(",", enabledChannels));
            history.setStatus(failedChannels.isEmpty() ? AlertStatus.SUCCESS : AlertStatus.FAILED);
            history.setSentAt(LocalDateTime.now());

            if (!failedChannels.isEmpty()) {
                history.setErrorMessage(errorMessages.toString());
            }

            alertHistoryMapper.insert(history);
            log.info("告警记录已保存: id={}, channels={}, status={}",
                    history.getId(), history.getChannelType(), history.getStatus());
        }
    }


    @Override
    public void checkCostAlerts() {
        log.debug("开始检查成本告警...");

        // 从系统设置获取告警配置
        AlertSettingsDTO alertSettings = systemSettingsService.getAlertSettings();

        // 检查成本告警是否启用
        if (!Boolean.TRUE.equals(alertSettings.getCostAlertEnabled())) {
            log.debug("成本告警未启用");
            return;
        }

        // 检查是否在冷却期内
        if (isInCooldownPeriod(AlertType.COST, alertSettings.getCostAlertCooldownMinutes())) {
            log.debug("成本告警在冷却期内，跳过本次通知");
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
                    "#### 📊【AgentGuard】预算告警通知\n\n" +
                    "**月份：** {}\n\n" +
                    "**预算上限：** ¥{}\n\n" +
                    "**已使用金额：** ¥{}\n\n" +
                    "**使用百分比：** {}%\n\n" +
                    "**告警阈值：** {}%\n\n" +
                    "**剩余金额：** ¥{}\n\n" +
                    "请及时关注成本使用情况。",
                    currentBudget.getMonth(),
                    currentBudget.getLimitAmount(),
                    currentBudget.getUsedAmount(),
                    usagePercentage.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                    alertSettings.getCostThreshold(),
                    currentBudget.getRemainingAmount());

            // 发送告警到所有启用的通知渠道
            sendAlertToAllChannels(AlertType.COST, title, content, recipient);
        }

        // 检查是否超预算
        if (currentBudget.getOverBudget()) {
            String title = StrUtil.format("【预算超支】{}月已超出预算！", currentBudget.getMonth());

            String content = StrUtil.format(
                    "#### ⚠️【AgentGuard】预算超支告警\n\n" +
                    "**月份：** {}\n\n" +
                    "**预算上限：** ¥{}\n\n" +
                    "**已使用金额：** ¥{}\n\n" +
                    "**超支金额：** ¥{}\n\n" +
                    "请立即采取措施控制成本！",
                    currentBudget.getMonth(),
                    currentBudget.getLimitAmount(),
                    currentBudget.getUsedAmount(),
                    currentBudget.getUsedAmount().subtract(currentBudget.getLimitAmount()));

            // 发送告警到所有启用的通知渠道
            sendAlertToAllChannels(AlertType.COST, title, content, recipient);
        }

        log.debug("成本告警检查完成");
    }


    @Override
    public void checkErrorRateAlerts(int windowMinutes, double errorRateThreshold) {
        log.debug("开始检查错误率告警...");

        // 从系统设置获取告警配置
        AlertSettingsDTO alertSettings = systemSettingsService.getAlertSettings();

        // 检查错误率告警是否启用
        if (!Boolean.TRUE.equals(alertSettings.getErrorRateAlertEnabled())) {
            log.debug("错误率告警未启用");
            return;
        }

        // 检查是否在冷却期内
        if (isInCooldownPeriod(AlertType.ERROR_RATE, alertSettings.getErrorRateAlertCooldownMinutes())) {
            log.debug("错误率告警在冷却期内，跳过本次通知");
            return;
        }

        // 使用系统设置中的时间窗口和阈值
        int effectiveWindowMinutes = alertSettings.getErrorRateWindow() != null
            ? alertSettings.getErrorRateWindow()
            : windowMinutes;

        double effectiveThreshold = alertSettings.getErrorRateThreshold() != null
            ? alertSettings.getErrorRateThreshold() / 100.0  // 转换为小数，如10% -> 0.1
            : errorRateThreshold;
        log.debug("错误率告警： windowMinutes={}, threshold={}", effectiveWindowMinutes, effectiveThreshold);

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
                    "#### 🚨【AgentGuard】系统异常告警\n\n" +
                    "**时间窗口：** 最近 {} 分钟\n\n" +
                    "**总请求数：** {}\n\n" +
                    "**失败请求数：** {}\n\n" +
                    "**当前错误率：** {}%\n\n" +
                    "**告警阈值：** {}%\n\n" +
                    "请及时排查系统异常！",
                    effectiveWindowMinutes,
                    totalRequests,
                    failedRequests,
                    String.format("%.2f", errorRate * 100),
                    String.format("%.0f", effectiveThreshold * 100));

            // 发送告警到所有启用的通知渠道
            sendAlertToAllChannels(AlertType.ERROR_RATE, title, content, recipient);
        }

        log.debug("错误率告警检查完成");
    }


    @Override
    public void sendApprovalReminders(int reminderMinutes) {
        log.debug("开始发送审批提醒...");

        // 从系统设置获取告警配置
        var alertSettings = systemSettingsService.getAlertSettings();

        // 检查审批提醒是否启用
        if (!Boolean.TRUE.equals(alertSettings.getApprovalReminderEnabled())) {
            log.debug("审批提醒未启用");
            return;
        }

        // 检查是否在冷却期内
        if (isInCooldownPeriod(AlertType.APPROVAL, alertSettings.getApprovalReminderCooldownMinutes())) {
            log.debug("审批提醒在冷却期内，跳过本次通知");
            return;
        }

        // 使用系统设置中的提醒时间
        int effectiveReminderMinutes = alertSettings.getApprovalReminderMinutes() != null
            ? alertSettings.getApprovalReminderMinutes()
            : reminderMinutes;
        log.debug("审批提醒: reminderMinutes={}", effectiveReminderMinutes);

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

        log.info("发现{}个即将过期的审批请求，将发送汇总通知", pendingApprovals.size());

        // 获取邮件配置中的默认收件人
        var emailSettings = systemSettingsService.getEmailSettings();
        String recipient = emailSettings.getDefaultRecipients();
        if (StrUtil.isBlank(recipient)) {
            recipient = defaultRecipient;
        }

        // 按过期时间排序，取前3条
        List<ApprovalRequestDO> top3Approvals = pendingApprovals.stream()
                .sorted((a1, a2) -> a1.getExpiresAt().compareTo(a2.getExpiresAt()))
                .limit(3)
                .toList();

        String title = StrUtil.format("【审批提醒】有{}个审批请求即将过期", pendingApprovals.size());

        // 构建表格内容
        StringBuilder tableBuilder = new StringBuilder();
        tableBuilder.append("| 审批ID | 过期时间 | 剩余时间 |\n");
        tableBuilder.append("| :----- | :----: | -------: |\n");

        for (ApprovalRequestDO approval : top3Approvals) {
            long remainingMinutes = java.time.Duration.between(now, approval.getExpiresAt()).toMinutes();
            tableBuilder.append(StrUtil.format("| {} | {} | {} 分钟 |\n",
                    approval.getId(),
                    formatDateTime(approval.getExpiresAt()),
                    remainingMinutes));
        }

        String content = StrUtil.format(
                "#### ⏰【AgentGuard】审批过期提醒\n\n" +
                "**待审批总数：** {}\n\n" +
                "**即将过期的前3条审批请求：**\n\n" +
                "{}\n" +
                "请尽快处理这些审批请求！\n\n" +
                "👉 [前往审批中心处理]({})",
                pendingApprovals.size(),
                tableBuilder.toString(),
                frontendUrl + "/approvals");

        // 发送告警到所有启用的通知渠道
        sendAlertToAllChannels(AlertType.APPROVAL, title, content, recipient);


        // 同时发送新的待审批请求提醒
        // sendNewApprovalReminders(recipient);

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

            // 获取Agent和Policy名称
            String agentName = getAgentName(approval.getAgentId());
            String policyName = getPolicyName(approval.getPolicyId());

            String content = StrUtil.format(
                    "#### 📋 新审批请求通知\n\n" +
                    "**审批ID：** `{}`\n\n" +
                    "**Agent：** `{}` (ID: `{}`)\n\n" +
                    "**策略：** `{}` (ID: `{}`)\n\n" +
                    "**创建时间：** `{}`\n\n" +
                    "**过期时间：** `{}`\n\n" +
                    "> 💡 请及时处理该审批请求。",
                    approval.getId(),
                    agentName,
                    approval.getAgentId(),
                    policyName,
                    approval.getPolicyId(),
                    formatDateTime(approval.getCreatedAt()),
                    formatDateTime(approval.getExpiresAt()));

            // 发送告警到所有启用的通知渠道
            sendAlertToAllChannels(AlertType.APPROVAL, title, content, recipient);
        }
    }

    /**
     * 格式化时间
     *
     * @param dateTime 时间对象
     * @return 格式化后的时间字符串
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * 获取Agent名称
     *
     * @param agentId Agent ID
     * @return Agent名称，如果未找到则返回ID
     */
    private String getAgentName(String agentId) {
        if (StrUtil.isBlank(agentId)) {
            return "未知";
        }
        AgentDO agent = agentMapper.selectById(agentId);
        return agent != null && StrUtil.isNotBlank(agent.getName())
            ? agent.getName()
            : agentId;
    }

    /**
     * 获取策略名称
     *
     * @param policyId 策略ID
     * @return 策略名称，如果未找到则返回ID
     */
    private String getPolicyName(String policyId) {
        if (StrUtil.isBlank(policyId)) {
            return "未知";
        }
        PolicyDO policy = policyMapper.selectById(policyId);
        return policy != null && StrUtil.isNotBlank(policy.getName())
            ? policy.getName()
            : policyId;
    }

    /**
     * 检查指定类型的告警是否在冷却期内
     *
     * @param alertType 告警类型
     * @param cooldownMinutes 冷却时间（分钟）
     * @return true-在冷却期内，false-不在冷却期内
     */
    private boolean isInCooldownPeriod(AlertType alertType, Integer cooldownMinutes) {
        if (cooldownMinutes == null || cooldownMinutes <= 0) {
            return false;
        }

        // 查询最近一次成功发送的相同类型告警
        LambdaQueryWrapper<AlertHistoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertHistoryDO::getType, alertType)
               .eq(AlertHistoryDO::getStatus, AlertStatus.SUCCESS)
               .orderByDesc(AlertHistoryDO::getCreatedAt)
               .last("LIMIT 1");

        AlertHistoryDO lastAlert = alertHistoryMapper.selectOne(wrapper);

        if (lastAlert == null) {
            // 没有历史记录，不在冷却期
            return false;
        }

        // 计算距离上次发送的时间（分钟）
        LocalDateTime lastSentTime = lastAlert.getCreatedAt();
        LocalDateTime now = LocalDateTime.now();
        long minutesSinceLastSent = java.time.Duration.between(lastSentTime, now).toMinutes();

        boolean inCooldown = minutesSinceLastSent < cooldownMinutes;
        if (inCooldown) {
            log.debug("告警类型 {} 在冷却期内，上次发送时间: {}, 已过去 {} 分钟，冷却时间: {} 分钟",
                    alertType, lastSentTime, minutesSinceLastSent, cooldownMinutes);
        }

        return inCooldown;
    }
}
