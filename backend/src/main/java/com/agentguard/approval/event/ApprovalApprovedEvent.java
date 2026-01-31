package com.agentguard.approval.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 审批通过事件
 * 当审批请求被批准时发布此事件
 *
 * @author zhuhx
 */
@Getter
public class ApprovalApprovedEvent extends ApplicationEvent {

    /**
     * 审批请求ID
     */
    private final String approvalId;

    public ApprovalApprovedEvent(Object source, String approvalId) {
        super(source);
        this.approvalId = approvalId;
    }
}
