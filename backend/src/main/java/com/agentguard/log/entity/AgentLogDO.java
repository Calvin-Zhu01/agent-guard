package com.agentguard.log.entity;

import com.agentguard.log.dto.PolicySnapshotDTO;
import com.agentguard.log.enums.RequestType;
import com.agentguard.log.enums.ResponseStatus;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent日志数据对象
 *
 * @author zhuhx
 */
@Data
@TableName(value = "agent_log", autoResultMap = true)
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

    /** 请求头（JSON格式） */
    private String requestHeaders;

    /** 完整请求体（JSON格式） */
    private String requestBody;

    /** 完整响应体（JSON格式） */
    private String responseBody;

    /** 响应状态：SUCCESS/FAILED/BLOCKED/PENDING_APPROVAL */
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

    /** 策略快照（触发策略时的策略信息） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private PolicySnapshotDTO policySnapshot;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
