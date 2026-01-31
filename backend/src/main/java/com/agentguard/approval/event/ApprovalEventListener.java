package com.agentguard.approval.event;

import com.agentguard.approval.service.ApprovalExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 审批事件监听器
 * 监听审批相关事件并在事务提交后执行相应操作
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalEventListener {

    private final ApprovalExecutor approvalExecutor;

    /**
     * 监听审批通过事件
     * 在事务提交后异步执行审批请求
     *
     * @param event 审批通过事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleApprovalApproved(ApprovalApprovedEvent event) {
        String approvalId = event.getApprovalId();
        log.info("收到审批通过事件，准备异步执行: approvalId={}", approvalId);

        // 检查是否配置为自动执行
        if (approvalExecutor.isAutoExecuteEnabled()) {
            approvalExecutor.executeAsync(approvalId);
        } else {
            log.info("自动执行已禁用，跳过执行: approvalId={}", approvalId);
        }
    }
}
