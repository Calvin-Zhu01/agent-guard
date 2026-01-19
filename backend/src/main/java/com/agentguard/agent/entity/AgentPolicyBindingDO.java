package com.agentguard.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent策略绑定数据对象
 *
 * @author zhuhx
 */
@Data
@TableName("agent_policy_binding")
public class AgentPolicyBindingDO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** Agent ID */
    private String agentId;

    /** 策略 ID */
    private String policyId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
