package com.agentguard.policy.entity;

import com.agentguard.log.enums.RequestType;
import com.agentguard.policy.enums.PolicyAction;
import com.agentguard.policy.enums.PolicyScope;
import com.agentguard.policy.enums.PolicyType;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 策略数据对象
 *
 * @author zhuhx
 */
@Data
@TableName("policy")
public class PolicyDO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 策略名称 */
    private String name;

    /** 策略描述 */
    private String description;

    /** 策略类型 */
    private PolicyType type;

    /** 策略条件（JSON格式） */
    private String conditions;

    /** 策略动作 */
    private PolicyAction action;

    /** 优先级（数值越大优先级越高） */
    private Integer priority;

    /** 作用域：GLOBAL-全局，AGENT-Agent级别 */
    private PolicyScope scope;

    /** 关联的Agent ID（仅当scope为AGENT时有效） */
    private String agentId;

    /** 请求类型：LLM_CALL-LLM调用，API_CALL-API调用，ALL-全部（默认ALL） */
    private RequestType requestType;

    /** 是否启用 */
    private Boolean enabled;

    /** 策略标签（JSON数组，如 ["财务", "支付", "高风险"]） */
    private String tags;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
