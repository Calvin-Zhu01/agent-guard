package com.agentguard.approval.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.agentguard.agent.entity.AgentDO;
import com.agentguard.agent.mapper.AgentMapper;
import com.agentguard.approval.dto.ApprovalCreateDTO;
import com.agentguard.approval.dto.ApprovalDTO;
import com.agentguard.approval.dto.ApprovalExecutionResultDTO;
import com.agentguard.approval.dto.ApprovalStatusDTO;
import com.agentguard.approval.entity.ApprovalRequestDO;
import com.agentguard.approval.enums.ApprovalStatus;
import com.agentguard.approval.enums.ExecutionStatus;
import com.agentguard.approval.event.ApprovalApprovedEvent;
import com.agentguard.approval.mapper.ApprovalMapper;
import com.agentguard.approval.service.ApprovalExecutor;
import com.agentguard.approval.service.ApprovalService;
import com.agentguard.approval.util.ApprovalIdGenerator;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.log.enums.ResponseStatus;
import com.agentguard.log.service.AgentLogService;
import com.agentguard.policy.entity.PolicyDO;
import com.agentguard.policy.mapper.PolicyMapper;
import com.agentguard.settings.service.SystemSettingsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审批服务实现类
 *
 * @author zhuhx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalMapper approvalMapper;
    private final PolicyMapper policyMapper;
    private final AgentMapper agentMapper;
    private final ApprovalExecutor approvalExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final AgentLogService agentLogService;
    private final ApprovalIdGenerator approvalIdGenerator;
    private final SystemSettingsService systemSettingsService;

    @Override
    @Transactional
    public ApprovalDTO create(ApprovalCreateDTO dto) {
        ApprovalRequestDO approvalDO = new ApprovalRequestDO();
        // 生成自定义审批ID
        approvalDO.setId(approvalIdGenerator.nextUUID(approvalDO));
        approvalDO.setPolicyId(dto.getPolicyId());
        approvalDO.setAgentId(dto.getAgentId());
        approvalDO.setRequestData(dto.getRequestData());
        approvalDO.setStatus(ApprovalStatus.PENDING);
        approvalDO.setExecutionStatus(ExecutionStatus.NOT_EXECUTED);

        // 获取过期时间
        int expireMinutes = dto.getExpireMinutes();
        if (expireMinutes <= 0) {
            var alertSettings = systemSettingsService.getAlertSettings();
            expireMinutes = alertSettings.getApprovalExpirationMinutes();
        }
        approvalDO.setExpiresAt(LocalDateTime.now().plusMinutes(expireMinutes));

        approvalMapper.insert(approvalDO);
        return toDTO(approvalDO);
    }

    @Override
    public ApprovalDTO getById(String id) {
        ApprovalRequestDO approvalDO = approvalMapper.selectById(id);
        if (ObjectUtil.isNull(approvalDO)) {
            throw new BusinessException(ErrorCode.APPROVAL_NOT_FOUND);
        }
        return toDTO(approvalDO);
    }

    @Override
    public IPage<ApprovalDTO> page(Page<ApprovalDTO> page, ApprovalStatus status, String agentId, String approvalId) {
        LambdaQueryWrapper<ApprovalRequestDO> wrapper = new LambdaQueryWrapper<>();

        if (ObjectUtil.isNotNull(status)) {
            wrapper.eq(ApprovalRequestDO::getStatus, status);
        }

        if (StrUtil.isNotBlank(agentId)) {
            wrapper.eq(ApprovalRequestDO::getAgentId, agentId);
        }

        // 支持审批ID模糊查询
        if (StrUtil.isNotBlank(approvalId)) {
            wrapper.like(ApprovalRequestDO::getId, approvalId);
        }

        wrapper.orderByDesc(ApprovalRequestDO::getCreatedAt);

        Page<ApprovalRequestDO> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<ApprovalRequestDO> result = approvalMapper.selectPage(entityPage, wrapper);

        return result.convert(this::toDTO);
    }

    @Override
    @Transactional
    public ApprovalDTO approve(String id, String approverId, String remark) {
        ApprovalRequestDO approvalDO = getApprovalForProcess(id);

        approvalDO.setStatus(ApprovalStatus.APPROVED);
        approvalDO.setApproverId(approverId);
        approvalDO.setApprovedAt(LocalDateTime.now());
        approvalDO.setRemark(remark);

        approvalMapper.updateById(approvalDO);

        // 更新关联的日志状态为 APPROVED
        try {
            agentLogService.updateStatusByApprovalRequestId(id, ResponseStatus.APPROVED);
            log.info("已更新审批请求 {} 关联的日志状态为 APPROVED", id);
        } catch (Exception e) {
            log.error("更新日志状态失败: approvalId={}, error={}", id, e.getMessage(), e);
        }

        // 发布审批通过事件，事件监听器会在事务提交后异步执行原始请求
        log.info("发布审批通过事件: approvalId={}", id);
        eventPublisher.publishEvent(new ApprovalApprovedEvent(this, id));

        return toDTO(approvalDO);
    }

    @Override
    @Transactional
    public ApprovalDTO reject(String id, String approverId, String remark) {
        ApprovalRequestDO approvalDO = getApprovalForProcess(id);

        approvalDO.setStatus(ApprovalStatus.REJECTED);
        approvalDO.setApproverId(approverId);
        approvalDO.setApprovedAt(LocalDateTime.now());
        approvalDO.setRemark(remark);

        approvalMapper.updateById(approvalDO);

        // 更新关联的日志状态为 REJECTED
        try {
            agentLogService.updateStatusByApprovalRequestId(id, ResponseStatus.REJECTED);
            log.info("已更新审批请求 {} 关联的日志状态为 REJECTED", id);
        } catch (Exception e) {
            log.error("更新日志状态失败: approvalId={}, error={}", id, e.getMessage(), e);
        }

        // 发送拒绝通知
        try {
            approvalExecutor.sendRejectionNotification(id, remark);
        } catch (Exception e) {
            log.error("发送拒绝通知失败: approvalId={}, error={}", id, e.getMessage(), e);
        }

        return toDTO(approvalDO);
    }

    @Override
    @Transactional
    public void expireOverdue() {
        // 1. 查询所有过期的待审批记录
        LambdaQueryWrapper<ApprovalRequestDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApprovalRequestDO::getStatus, ApprovalStatus.PENDING)
                   .lt(ApprovalRequestDO::getExpiresAt, LocalDateTime.now());

        java.util.List<ApprovalRequestDO> expiredApprovals = approvalMapper.selectList(queryWrapper);

        if (expiredApprovals.isEmpty()) {
            log.debug("没有过期的审批记录");
            return;
        }

        log.info("发现 {} 条过期的审批记录，开始批量更新", expiredApprovals.size());

        // 2. 批量更新审批状态
        LambdaUpdateWrapper<ApprovalRequestDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ApprovalRequestDO::getStatus, ApprovalStatus.PENDING)
                   .lt(ApprovalRequestDO::getExpiresAt, LocalDateTime.now())
                   .set(ApprovalRequestDO::getStatus, ApprovalStatus.EXPIRED);

        int updatedCount = approvalMapper.update(null, updateWrapper);
        log.info("已更新 {} 条审批记录状态为 EXPIRED", updatedCount);

        // 3. 同步更新关联的日志状态
        for (ApprovalRequestDO approval : expiredApprovals) {
            try {
                agentLogService.updateStatusByApprovalRequestId(approval.getId(), ResponseStatus.EXPIRED);
            } catch (Exception e) {
                log.error("更新审批 {} 关联的日志状态失败: {}", approval.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public long getPendingCount() {
        LambdaQueryWrapper<ApprovalRequestDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRequestDO::getStatus, ApprovalStatus.PENDING);
        return approvalMapper.selectCount(wrapper);
    }

    @Override
    public ApprovalStatusDTO getStatus(String id) {
        ApprovalRequestDO approvalDO = approvalMapper.selectById(id);
        if (ObjectUtil.isNull(approvalDO)) {
            throw new BusinessException(ErrorCode.APPROVAL_NOT_FOUND);
        }

        // 检查是否已过期，这里只做展示层转换，不修改数据库，数据库的更新由定时任务负责
        ApprovalStatus displayStatus = approvalDO.getStatus();
        if (approvalDO.getStatus() == ApprovalStatus.PENDING
                && approvalDO.getExpiresAt() != null
                && LocalDateTime.now().isAfter(approvalDO.getExpiresAt())) {
            displayStatus = ApprovalStatus.EXPIRED;
        }

        ApprovalStatusDTO statusDTO = ApprovalStatusDTO.builder()
                .status(displayStatus)
                .build();

        // 如果审批通过且已执行成功，返回执行结果
        if (approvalDO.getStatus() == ApprovalStatus.APPROVED
                && approvalDO.getExecutionStatus() == ExecutionStatus.SUCCESS
                && StrUtil.isNotBlank(approvalDO.getExecutionResult())) {
            // 将JSON字符串解析为对象返回
            try {
                Object executionResult = JSONUtil.parse(approvalDO.getExecutionResult());
                statusDTO.setExecutionResult(executionResult);
            } catch (Exception e) {
                log.warn("解析执行结果失败: approvalId={}, error={}", id, e.getMessage());
                // 如果解析失败，返回原始字符串
                statusDTO.setExecutionResult(approvalDO.getExecutionResult());
            }
        }

        // 如果审批被拒绝，返回拒绝原因
        if (approvalDO.getStatus() == ApprovalStatus.REJECTED) {
            statusDTO.setRemark(approvalDO.getRemark());
        }

        return statusDTO;
    }

    @Override
    @Transactional
    public ApprovalDTO submitReason(String id, String reason) {
        ApprovalRequestDO approvalDO = approvalMapper.selectById(id);
        if (ObjectUtil.isNull(approvalDO)) {
            throw new BusinessException(ErrorCode.APPROVAL_NOT_FOUND);
        }

        // 只有待审批状态才能提交理由
        if (approvalDO.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_PROCESSED);
        }

        // 检查是否已过期
        if (LocalDateTime.now().isAfter(approvalDO.getExpiresAt())) {
            approvalDO.setStatus(ApprovalStatus.EXPIRED);
            approvalMapper.updateById(approvalDO);
            throw new BusinessException(ErrorCode.APPROVAL_EXPIRED);
        }

        // 更新申请理由
        approvalDO.setApplicationReason(reason);
        approvalMapper.updateById(approvalDO);

        log.info("审批请求 {} 已提交申请理由", id);
        return toDTO(approvalDO);
    }

    /**
     * 获取待处理的审批请求
     *
     * @param id 审批请求ID
     * @return 审批请求实体
     */
    private ApprovalRequestDO getApprovalForProcess(String id) {
        ApprovalRequestDO approvalDO = approvalMapper.selectById(id);
        if (ObjectUtil.isNull(approvalDO)) {
            throw new BusinessException(ErrorCode.APPROVAL_NOT_FOUND);
        }

        // 检查是否已处理
        if (approvalDO.getStatus() != ApprovalStatus.PENDING) {
            if (approvalDO.getStatus() == ApprovalStatus.EXPIRED) {
                throw new BusinessException(ErrorCode.APPROVAL_EXPIRED);
            }
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_PROCESSED);
        }

        // 检查是否已过期
        if (LocalDateTime.now().isAfter(approvalDO.getExpiresAt())) {
            approvalDO.setStatus(ApprovalStatus.EXPIRED);
            approvalMapper.updateById(approvalDO);
            throw new BusinessException(ErrorCode.APPROVAL_EXPIRED);
        }

        return approvalDO;
    }

    /**
     * 转换为DTO
     *
     * @param approvalDO 实体对象
     * @return DTO对象
     */
    private ApprovalDTO toDTO(ApprovalRequestDO approvalDO) {
        ApprovalDTO dto = BeanUtil.copyProperties(approvalDO, ApprovalDTO.class);

        // 检查是否已过期，这里只做展示层转换，不修改数据库，数据库的更新由定时任务负责
        if (approvalDO.getStatus() == ApprovalStatus.PENDING
                && approvalDO.getExpiresAt() != null
                && LocalDateTime.now().isAfter(approvalDO.getExpiresAt())) {
            dto.setStatus(ApprovalStatus.EXPIRED);
        }

        // 获取策略名称
        if (StrUtil.isNotBlank(approvalDO.getPolicyId())) {
            PolicyDO policyDO = policyMapper.selectById(approvalDO.getPolicyId());
            if (ObjectUtil.isNotNull(policyDO)) {
                dto.setPolicyName(policyDO.getName());
            }
        }

        // 获取Agent名称
        if (StrUtil.isNotBlank(approvalDO.getAgentId())) {
            AgentDO agentDO = agentMapper.selectById(approvalDO.getAgentId());
            if (ObjectUtil.isNotNull(agentDO)) {
                dto.setAgentName(agentDO.getName());
            }
        }

        return dto;
    }
}
