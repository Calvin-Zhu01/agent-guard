package com.agentguard.approval.service;

import com.agentguard.approval.dto.ApprovalExecutionResultDTO;

/**
 * 审批执行器接口
 *
 * @author zhuhx
 */
public interface ApprovalExecutor {

    /**
     * 执行已批准的请求
     *
     * @param approvalId 审批请求ID
     * @return 执行结果
     */
    ApprovalExecutionResultDTO execute(String approvalId);

    /**
     * 异步执行已批准的请求
     *
     * @param approvalId 审批请求ID
     */
    void executeAsync(String approvalId);

    /**
     * 检查是否配置为自动执行
     *
     * @return 是否自动执行
     */
    boolean isAutoExecuteEnabled();

    /**
     * 发送执行失败通知
     *
     * @param approvalId 审批请求ID
     * @param errorMessage 错误消息
     */
    void sendExecutionFailureNotification(String approvalId, String errorMessage);

    /**
     * 发送审批拒绝通知
     *
     * @param approvalId 审批请求ID
     * @param reason 拒绝原因
     */
    void sendRejectionNotification(String approvalId, String reason);
}
