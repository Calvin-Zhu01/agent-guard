package com.agentguard.approval.service;

import com.agentguard.approval.dto.ApprovalCreateDTO;
import com.agentguard.approval.dto.ApprovalDTO;
import com.agentguard.approval.dto.ApprovalStatusDTO;
import com.agentguard.approval.enums.ApprovalStatus;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 审批服务接口
 *
 * @author zhuhx
 */
public interface ApprovalService {

    /**
     * 创建审批请求
     *
     * @param dto 创建请求
     * @return 审批请求信息
     */
    ApprovalDTO create(ApprovalCreateDTO dto);

    /**
     * 根据ID获取审批请求详情
     *
     * @param id 审批请求ID
     * @return 审批请求信息
     */
    ApprovalDTO getById(String id);

    /**
     * 分页查询审批请求列表
     *
     * @param page 分页参数
     * @param status 审批状态（可选）
     * @param agentId Agent ID（可选）
     * @param approvalId 审批ID（可选，支持模糊匹配）
     * @return 分页结果
     */
    IPage<ApprovalDTO> page(Page<ApprovalDTO> page, ApprovalStatus status, String agentId, String approvalId);

    /**
     * 批准审批请求
     *
     * @param id 审批请求ID
     * @param approverId 审批人ID
     * @param remark 审批备注
     * @return 审批请求信息
     */
    ApprovalDTO approve(String id, String approverId, String remark);

    /**
     * 拒绝审批请求
     *
     * @param id 审批请求ID
     * @param approverId 审批人ID
     * @param remark 审批备注
     * @return 审批请求信息
     */
    ApprovalDTO reject(String id, String approverId, String remark);

    /**
     * 处理过期的审批请求
     * 将超过过期时间的待审批请求状态更新为已过期
     */
    void expireOverdue();

    /**
     * 获取待审批数量
     *
     * @return 待审批数量
     */
    long getPendingCount();

    /**
     * 获取审批状态（用于客户端SDK轮询）
     * 当审批通过时，返回执行结果
     * 当审批拒绝时，返回拒绝原因
     *
     * @param id 审批请求ID
     * @return 审批状态信息
     */
    ApprovalStatusDTO getStatus(String id);

    /**
     * 提交审批申请理由
     *
     * @param id 审批请求ID
     * @param reason 申请理由
     * @return 审批请求信息
     */
    ApprovalDTO submitReason(String id, String reason);
}
