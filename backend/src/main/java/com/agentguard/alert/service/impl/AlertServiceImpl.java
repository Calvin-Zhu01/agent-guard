package com.agentguard.alert.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agentguard.alert.channel.NotificationChannel;
import com.agentguard.alert.channel.NotificationChannelFactory;
import com.agentguard.alert.dto.AlertDTO;
import com.agentguard.alert.entity.AlertHistoryDO;
import com.agentguard.alert.entity.AlertRuleDO;
import com.agentguard.alert.enums.AlertStatus;
import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.mapper.AlertHistoryMapper;
import com.agentguard.alert.mapper.AlertRuleMapper;
import com.agentguard.alert.service.AlertService;
import com.agentguard.approval.entity.ApprovalRequestDO;
import com.agentguard.approval.enums.ApprovalStatus;
import com.agentguard.approval.mapper.ApprovalMapper;
import com.agentguard.budget.dto.BudgetWithUsageDTO;
import com.agentguard.budget.service.BudgetService;
import com.agentguard.log.entity.AgentLogDO;
import com.agentguard.log.enums.ResponseStatus;
import com.agentguard.log.mapper.AgentLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 告警服务实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertHistoryMapper alertHistoryMapper;
    private final NotificationChannelFactory channelFactory;
    private final BudgetService budgetService;
    private final AgentLogMapper agentLogMapper;
    private final ApprovalMapper approvalMapper;
    private final List<HealthIndicator> healthIndicators;

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

        // 获取当前预算使用情况
        BudgetWithUsageDTO currentBudget = budgetService.getCurrentBudget();

        if (ObjectUtil.isNull(currentBudget.getId())) {
            log.debug("未设置预算，跳过成本告警检查");
            return;
        }

        // 获取启用的成本告警规则
        List<AlertRuleDO> costRules = getEnabledRulesByType(AlertType.COST);

        if (costRules.isEmpty()) {
            log.debug("没有启用的成本告警规则");
            return;
        }

        BigDecimal usagePercentage = currentBudget.getUsagePercentage();

        for (AlertRuleDO rule : costRules) {
            BigDecimal threshold = rule.getThreshold();

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
                        threshold.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP),
                        currentBudget.getRemainingAmount());

                sendAlertByRule(rule, title, content);
            }
        }

        // 检查是否超预算
        if (currentBudget.getOverBudget()) {
            for (AlertRuleDO rule : costRules) {
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

                sendAlertByRule(rule, title, content);
            }
        }

        log.debug("成本告警检查完成");
    }


    @Override
    public void checkErrorRateAlerts(int windowMinutes, double errorRateThreshold) {
        log.debug("开始检查错误率告警: windowMinutes={}, threshold={}", windowMinutes, errorRateThreshold);

        // 获取启用的错误率告警规则
        List<AlertRuleDO> errorRateRules = getEnabledRulesByType(AlertType.ERROR_RATE);

        if (errorRateRules.isEmpty()) {
            log.debug("没有启用的错误率告警规则");
            return;
        }

        // 计算时间窗口
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(windowMinutes);

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

        for (AlertRuleDO rule : errorRateRules) {
            double threshold = rule.getThreshold().doubleValue();

            // 检查是否超过阈值
            if (errorRate >= threshold) {
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
                        windowMinutes,
                        totalRequests,
                        failedRequests,
                        String.format("%.2f", errorRate * 100),
                        String.format("%.0f", threshold * 100));

                sendAlertByRule(rule, title, content);
            }
        }

        log.debug("错误率告警检查完成");
    }


    @Override
    public void sendApprovalReminders(int reminderMinutes) {
        log.debug("开始发送审批提醒: reminderMinutes={}", reminderMinutes);

        // 获取启用的审批提醒规则
        List<AlertRuleDO> approvalRules = getEnabledRulesByType(AlertType.APPROVAL);

        if (approvalRules.isEmpty()) {
            log.debug("没有启用的审批提醒规则");
            return;
        }

        // 计算即将过期的时间范围
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(reminderMinutes);

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

        for (ApprovalRequestDO approval : pendingApprovals) {
            for (AlertRuleDO rule : approvalRules) {
                String title = StrUtil.format("【审批提醒】审批请求即将过期");

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

                sendAlertByRule(rule, title, content);
            }
        }

        // 同时发送新的待审批请求提醒
        sendNewApprovalReminders(approvalRules);

        log.debug("审批提醒发送完成");
    }

    /**
     * 发送新的待审批请求提醒
     *
     * @param approvalRules 审批提醒规则列表
     */
    private void sendNewApprovalReminders(List<AlertRuleDO> approvalRules) {
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
            for (AlertRuleDO rule : approvalRules) {
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

                sendAlertByRule(rule, title, content);
            }
        }
    }


    @Override
    public void checkSystemHealth() {
        log.debug("开始检查系统健康状态...");

        // 获取启用的系统告警规则
        List<AlertRuleDO> systemRules = getEnabledRulesByType(AlertType.SYSTEM);

        if (systemRules.isEmpty()) {
            log.debug("没有启用的系统告警规则");
            return;
        }

        // 检查所有健康指示器
        StringBuilder unhealthyComponents = new StringBuilder();
        int unhealthyCount = 0;

        for (HealthIndicator indicator : healthIndicators) {
            try {
                Health health = indicator.health();
                if (!org.springframework.boot.actuate.health.Status.UP.equals(health.getStatus())) {
                    unhealthyCount++;
                    String componentName = indicator.getClass().getSimpleName().replace("HealthIndicator", "");
                    unhealthyComponents.append(StrUtil.format("- {}: {} ({})\n",
                            componentName,
                            health.getStatus(),
                            health.getDetails()));
                }
            } catch (Exception e) {
                unhealthyCount++;
                String componentName = indicator.getClass().getSimpleName().replace("HealthIndicator", "");
                unhealthyComponents.append(StrUtil.format("- {}: 检查异常 ({})\n",
                        componentName,
                        e.getMessage()));
                log.error("健康检查异常: component={}, error={}", componentName, e.getMessage(), e);
            }
        }

        if (unhealthyCount > 0) {
            for (AlertRuleDO rule : systemRules) {
                String title = StrUtil.format("【系统告警】发现{}个组件异常", unhealthyCount);

                String content = StrUtil.format(
                        "系统健康告警\n\n" +
                        "检查时间：{}\n" +
                        "异常组件数：{}\n\n" +
                        "异常详情：\n{}\n" +
                        "请及时排查系统问题！",
                        LocalDateTime.now(),
                        unhealthyCount,
                        unhealthyComponents.toString());

                sendAlertByRule(rule, title, content);
            }
        }

        log.debug("系统健康检查完成: unhealthyCount={}", unhealthyCount);
    }

    /**
     * 获取指定类型的启用告警规则
     *
     * @param type 告警类型
     * @return 告警规则列表
     */
    private List<AlertRuleDO> getEnabledRulesByType(AlertType type) {
        LambdaQueryWrapper<AlertRuleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRuleDO::getType, type)
               .eq(AlertRuleDO::getEnabled, true);
        return alertRuleMapper.selectList(wrapper);
    }

    /**
     * 根据告警规则发送告警
     *
     * @param rule    告警规则
     * @param title   告警标题
     * @param content 告警内容
     */
    private void sendAlertByRule(AlertRuleDO rule, String title, String content) {
        // 解析渠道配置获取接收人
        String recipient = parseRecipient(rule.getChannelConfig());

        if (StrUtil.isBlank(recipient)) {
            recipient = defaultRecipient;
        }

        AlertDTO alert = new AlertDTO();
        alert.setRuleId(rule.getId());
        alert.setType(rule.getType());
        alert.setTitle(title);
        alert.setContent(content);
        alert.setRecipient(recipient);
        alert.setChannelType(rule.getChannelType());

        sendAlert(alert);
    }

    /**
     * 解析渠道配置获取接收人
     *
     * @param channelConfig 渠道配置（JSON格式）
     * @return 接收人
     */
    private String parseRecipient(String channelConfig) {
        if (StrUtil.isBlank(channelConfig)) {
            return null;
        }

        try {
            // 尝试解析 JSON 格式
            if (channelConfig.startsWith("{")) {
                Map<String, Object> config = JSONUtil.toBean(channelConfig, Map.class);
                // 支持 email、url、recipient 等字段
                if (config.containsKey("email")) {
                    return String.valueOf(config.get("email"));
                }
                if (config.containsKey("url")) {
                    return String.valueOf(config.get("url"));
                }
                if (config.containsKey("recipient")) {
                    return String.valueOf(config.get("recipient"));
                }
            }
            // 如果不是 JSON，直接返回原值（可能是邮箱地址或 URL）
            return channelConfig;
        } catch (Exception e) {
            log.warn("解析渠道配置失败: config={}, error={}", channelConfig, e.getMessage());
            return channelConfig;
        }
    }
}
