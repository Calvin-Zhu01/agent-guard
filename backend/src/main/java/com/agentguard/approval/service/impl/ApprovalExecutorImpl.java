package com.agentguard.approval.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agentguard.agent.entity.AgentDO;
import com.agentguard.agent.mapper.AgentMapper;
import com.agentguard.alert.dto.AlertDTO;
import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.enums.NotificationChannelType;
import com.agentguard.alert.service.AlertService;
import com.agentguard.approval.dto.ApprovalExecutionResultDTO;
import com.agentguard.approval.entity.ApprovalRequestDO;
import com.agentguard.approval.enums.ApprovalStatus;
import com.agentguard.approval.enums.ExecutionStatus;
import com.agentguard.approval.mapper.ApprovalMapper;
import com.agentguard.approval.service.ApprovalExecutor;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.proxy.dto.ProxyRequestDTO;
import com.agentguard.proxy.dto.ProxyResponseDTO;
import com.agentguard.proxy.service.ProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审批执行器实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalExecutorImpl implements ApprovalExecutor {

    private final ApprovalMapper approvalMapper;
    private final AgentMapper agentMapper;
    private final ProxyService proxyService;
    private final AlertService alertService;

    @Value("${approval.auto-execute:true}")
    private boolean autoExecuteEnabled;

    @Value("${alert.default-recipient:admin@agentguard.com}")
    private String defaultRecipient;

    @Override
    @Transactional
    public ApprovalExecutionResultDTO execute(String approvalId) {
        log.info("开始执行审批请求: approvalId={}", approvalId);

        // 1. 获取审批请求
        ApprovalRequestDO approval = approvalMapper.selectById(approvalId);
        if (ObjectUtil.isNull(approval)) {
            throw new BusinessException(ErrorCode.APPROVAL_NOT_FOUND);
        }

        // 2. 检查审批状态
        if (approval.getStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.APPROVAL_NOT_APPROVED);
        }

        // 3. 检查是否已执行
        if (approval.getExecutionStatus() == ExecutionStatus.SUCCESS) {
            log.warn("审批请求已执行成功，跳过重复执行: approvalId={}", approvalId);
            return ApprovalExecutionResultDTO.success(approvalId, approval.getExecutionResult());
        }

        // 4. 更新执行状态为执行中
        approval.setExecutionStatus(ExecutionStatus.EXECUTING);
        approvalMapper.updateById(approval);

        try {
            // 5. 解析原始请求数据
            ProxyRequestDTO proxyRequest = parseRequestData(approval);

            // 6. 执行原始请求（跳过策略检查，直接执行）
            ProxyResponseDTO response = executeRequest(proxyRequest);

            // 7. 更新执行结果
            approval.setExecutionStatus(ExecutionStatus.SUCCESS);
            approval.setExecutionResult(JSONUtil.toJsonStr(response));
            approval.setExecutedAt(LocalDateTime.now());
            approvalMapper.updateById(approval);

            log.info("审批请求执行成功: approvalId={}", approvalId);
            return ApprovalExecutionResultDTO.success(approvalId, response);

        } catch (Exception e) {
            log.error("审批请求执行失败: approvalId={}, error={}", approvalId, e.getMessage(), e);

            // 更新执行状态为失败
            approval.setExecutionStatus(ExecutionStatus.FAILED);
            approval.setExecutionResult(JSONUtil.toJsonStr(Map.of("error", e.getMessage())));
            approval.setExecutedAt(LocalDateTime.now());
            approvalMapper.updateById(approval);

            // 发送执行失败通知
            sendExecutionFailureNotification(approvalId, e.getMessage());

            return ApprovalExecutionResultDTO.failed(approvalId, e.getMessage());
        }
    }

    @Override
    public boolean isAutoExecuteEnabled() {
        return autoExecuteEnabled;
    }

    @Override
    public void sendExecutionFailureNotification(String approvalId, String errorMessage) {
        log.info("发送执行失败通知: approvalId={}", approvalId);

        ApprovalRequestDO approval = approvalMapper.selectById(approvalId);
        if (ObjectUtil.isNull(approval)) {
            log.warn("审批请求不存在，无法发送通知: approvalId={}", approvalId);
            return;
        }

        String agentName = getAgentName(approval.getAgentId());

        String title = "【执行失败】审批请求执行失败";
        String content = StrUtil.format(
                "审批执行失败通知\n\n" +
                "审批ID：{}\n" +
                "Agent：{}\n" +
                "策略ID：{}\n" +
                "审批时间：{}\n" +
                "执行时间：{}\n" +
                "失败原因：{}\n\n" +
                "请检查请求配置或手动处理。",
                approvalId,
                agentName,
                approval.getPolicyId(),
                approval.getApprovedAt(),
                LocalDateTime.now(),
                errorMessage);

        sendAlert(title, content);
    }

    @Override
    public void sendRejectionNotification(String approvalId, String reason) {
        log.info("发送审批拒绝通知: approvalId={}", approvalId);

        ApprovalRequestDO approval = approvalMapper.selectById(approvalId);
        if (ObjectUtil.isNull(approval)) {
            log.warn("审批请求不存在，无法发送通知: approvalId={}", approvalId);
            return;
        }

        String agentName = getAgentName(approval.getAgentId());

        String title = "【审批拒绝】审批请求已被拒绝";
        String content = StrUtil.format(
                "审批拒绝通知\n\n" +
                "审批ID：{}\n" +
                "Agent：{}\n" +
                "策略ID：{}\n" +
                "创建时间：{}\n" +
                "拒绝时间：{}\n" +
                "拒绝原因：{}\n\n" +
                "如有疑问，请联系管理员。",
                approvalId,
                agentName,
                approval.getPolicyId(),
                approval.getCreatedAt(),
                approval.getApprovedAt(),
                StrUtil.isBlank(reason) ? "无" : reason);

        sendAlert(title, content);
    }

    /**
     * 解析请求数据
     *
     * @param approval 审批请求
     * @return 代理请求DTO
     */
    @SuppressWarnings("unchecked")
    private ProxyRequestDTO parseRequestData(ApprovalRequestDO approval) {
        String requestDataJson = approval.getRequestData();
        if (StrUtil.isBlank(requestDataJson)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请求数据为空");
        }

        Map<String, Object> requestData = JSONUtil.toBean(requestDataJson, Map.class);

        // 获取 Agent 的 API Key
        AgentDO agent = agentMapper.selectById(approval.getAgentId());
        if (ObjectUtil.isNull(agent)) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }

        return ProxyRequestDTO.builder()
                .apiKey(agent.getApiKey())
                .targetUrl((String) requestData.get("targetUrl"))
                .method((String) requestData.getOrDefault("method", "POST"))
                .headers((Map<String, String>) requestData.get("headers"))
                .body((Map<String, Object>) requestData.get("body"))
                .build();
    }

    /**
     * 执行请求（MVP阶段返回模拟响应）
     *
     * @param request 代理请求
     * @return 代理响应
     */
    private ProxyResponseDTO executeRequest(ProxyRequestDTO request) {
        // MVP阶段：返回模拟响应
        // 实际生产环境中，这里应该调用实际的目标API
        log.info("执行已批准的请求: targetUrl={}, method={}", request.getTargetUrl(), request.getMethod());

        return ProxyResponseDTO.success(Map.of(
                "success", true,
                "message", "Approved request executed successfully",
                "targetUrl", request.getTargetUrl(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 获取 Agent 名称
     *
     * @param agentId Agent ID
     * @return Agent 名称
     */
    private String getAgentName(String agentId) {
        if (StrUtil.isBlank(agentId)) {
            return "未知";
        }
        AgentDO agent = agentMapper.selectById(agentId);
        return ObjectUtil.isNotNull(agent) ? agent.getName() : agentId;
    }

    /**
     * 发送告警
     *
     * @param title 告警标题
     * @param content 告警内容
     */
    private void sendAlert(String title, String content) {
        try {
            AlertDTO alert = new AlertDTO();
            alert.setType(AlertType.APPROVAL);
            alert.setTitle(title);
            alert.setContent(content);
            alert.setRecipient(defaultRecipient);
            alert.setChannelType(NotificationChannelType.EMAIL);

            alertService.sendAlert(alert);
        } catch (Exception e) {
            log.error("发送告警失败: title={}, error={}", title, e.getMessage(), e);
        }
    }
}
