package com.agentguard.log.entity;

import com.agentguard.log.enums.RequestType;
import com.agentguard.log.enums.ResponseStatus;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent日志数据对象
 *
 * @author zhuhx
 */
@Data
@TableName("agent_log")
public class AgentLogDO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** AgentID */
    private String agentId;

    /** 请求类型：API_CALL/LLM_CALL */
    private RequestType requestType;

    /** 请求地址 */
    private String endpoint;

    /** 请求方法 */
    private String method;

    /** 请求摘要（JSON格式） */
    private String requestSummary;

    /** 响应状态：SUCCESS/FAILED/BLOCKED */
    private ResponseStatus responseStatus;

    /** 响应时间（毫秒） */
    private Integer responseTimeMs;

    /** 输入token数 */
    private Integer tokenInput;

    /** 输出token数 */
    private Integer tokenOutput;

    /** LLM模型 */
    private String model;

    /** 成本 */
    private BigDecimal cost;

    /** 触发的策略ID */
    private String policyId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
