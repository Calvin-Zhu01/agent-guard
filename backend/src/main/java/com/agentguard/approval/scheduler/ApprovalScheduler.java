package com.agentguard.approval.scheduler;

import com.agentguard.approval.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审批定时任务调度器
 *
 * @author zhuhx
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "approval.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ApprovalScheduler {

    private final ApprovalService approvalService;

    /**
     * 过期审批检查定时任务
     *
     * 每1分钟执行一次，批量更新过期的审批状态
     */
    @Scheduled(fixedRate = 60000)
    public void expireOverdueApprovals() {
        log.debug("开始执行过期审批检查定时任务...");

        try {
            approvalService.expireOverdue();
            log.debug("过期审批检查定时任务执行完成");
        } catch (Exception e) {
            log.error("过期审批检查定时任务执行失败: {}", e.getMessage(), e);
        }
    }
}
