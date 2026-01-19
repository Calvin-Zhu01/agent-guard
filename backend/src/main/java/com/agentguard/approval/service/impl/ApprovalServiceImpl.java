package com.agentguard.approval.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.agentguard.agent.entity.AgentDO;
import com.agentguard.agent.mapper.AgentMapper;
import com.agentguard.approval.dto.ApprovalCreateDTO;
import com.agentguard.approval.dto.ApprovalDTO;
import com.agentguard.approval.dto.ApprovalExecutionResultDTO;
import com.agentguard.approval.entity.ApprovalRequestDO;
import com.agentguard.approval.enums.ApprovalStatus;
import com.agentguard.approval.enums.ExecutionStatus;
import com.agentguard.approval.mapper.ApprovalMapper;
import com.agentguard.approval.service.ApprovalExecutor;
import com.agentguard.approval.service.ApprovalService;
import com.agentguard.common.exception.BusinessException;
import com.agentguard.common.exception.ErrorCode;
import com.agentguard.policy.entity.PolicyDO;
import com.agentguard.policy.mapper.PolicyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    @Transactional
    public ApprovalDTO create(ApprovalCreateDTO dto) {
        ApprovalRequestDO approvalDO = new ApprovalRequestDO();
        approvalDO.setPolicyId(dto.getPolicyId());
        approvalDO.setAgentId(dto.getAgentId());
        approvalDO.setRequestData(dto.getRequestData());
        approvalDO.setStatus(ApprovalStatus.PENDING);
        approvalDO.setExecutionStatus(ExecutionStatus.NOT_EXECUTED);
        approvalDO.setExpiresAt(LocalDateTime.now().plusMinutes(dto.getExpireMinutes()));

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
    public IPage<ApprovalDTO> page(Page<ApprovalDTO> page, ApprovalStatus status, String agentId) {
        LambdaQueryWrapper<ApprovalRequestDO> wrapper = new LambdaQueryWrapper<>();

        if (ObjectUtil.isNotNull(status)) {
            wrapper.eq(ApprovalRequestDO::getStatus, status);
        }

        if (StrUtil.isNotBlank(agentId)) {
            wrapper.eq(ApprovalRequestDO::getAgentId, agentId);
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

        // 如果配置为自动执行，则执行原始请求
        if (approvalExecutor.isAutoExecuteEnabled()) {
            log.info("自动执行已批准的请求: approvalId={}", id);
            try {
                ApprovalExecutionResultDTO result = approvalExecutor.execute(id);
                // 重新获取更新后的审批记录
                approvalDO = approvalMapper.selectById(id);
                log.info("自动执行完成: approvalId={}, status={}", id, result.getStatus());
            } catch (Exception e) {
                log.error("自动执行失败: approvalId={}, error={}", id, e.getMessage(), e);
                // 执行失败不影响审批结果，通知已在执行器中发送
            }
        }

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
        LambdaUpdateWrapper<ApprovalRequestDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApprovalRequestDO::getStatus, ApprovalStatus.PENDING)
               .lt(ApprovalRequestDO::getExpiresAt, LocalDateTime.now())
               .set(ApprovalRequestDO::getStatus, ApprovalStatus.EXPIRED);

        approvalMapper.update(null, wrapper);
    }

    @Override
    public long getPendingCount() {
        LambdaQueryWrapper<ApprovalRequestDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRequestDO::getStatus, ApprovalStatus.PENDING);
        return approvalMapper.selectCount(wrapper);
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
