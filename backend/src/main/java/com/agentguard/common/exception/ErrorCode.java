package com.agentguard.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 *
 * @author zhuhx
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用错误
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // 用户模块 1xxx
    USER_NAME_DUPLICATE(1001, "用户名已存在"),
    USER_LOGIN_FAILED(1002, "用户名或密码错误"),
    USER_TOKEN_INVALID(1003, "Token无效或已过期"),

    // Agent 模块 2xxx
    AGENT_NOT_FOUND(2001, "Agent不存在"),
    AGENT_NAME_DUPLICATE(2002, "Agent名称已存在"),
    AGENT_API_KEY_INVALID(2003, "Agent API Key无效"),

    // 策略模块 3xxx
    POLICY_NOT_FOUND(3001, "策略不存在"),
    POLICY_NAME_DUPLICATE(3002, "策略名称已存在"),
    POLICY_CONDITION_INVALID(3003, "策略条件格式无效"),

    // 审批模块 4xxx
    APPROVAL_NOT_FOUND(4001, "审批请求不存在"),
    APPROVAL_ALREADY_PROCESSED(4002, "审批请求已处理"),
    APPROVAL_EXPIRED(4003, "审批请求已过期"),
    APPROVAL_NOT_APPROVED(4004, "审批请求未批准，无法执行"),

    // 预算模块 5xxx
    BUDGET_NOT_FOUND(5001, "预算不存在"),
    BUDGET_MONTH_DUPLICATE(5002, "预算月份已存在"),

    // 日志模块 6xxx
    LOG_NOT_FOUND(6001, "日志不存在"),

    // 告警模块 7xxx
    ALERT_RULE_NOT_FOUND(7001, "告警规则不存在"),
    ALERT_RULE_NAME_DUPLICATE(7002, "告警规则名称已存在"),
    ALERT_HISTORY_NOT_FOUND(7003, "告警历史不存在"),
    ALERT_SEND_FAILED(7004, "告警发送失败"),
    ALERT_CHANNEL_CONFIG_INVALID(7005, "告警渠道配置无效"),
    ALERT_NOTIFICATION_RETRY_EXHAUSTED(7006, "告警通知重试次数已用尽"),

    // Agent策略绑定模块 8xxx
    POLICY_BINDING_NOT_FOUND(8001, "策略绑定不存在"),
    POLICY_BINDING_ALREADY_EXISTS(8002, "策略绑定已存在"),
    POLICY_BINDING_AGENT_DISABLED(8003, "Agent已禁用，无法绑定策略"),
    POLICY_BINDING_POLICY_DISABLED(8004, "策略已禁用，无法绑定"),

    // Tool Schema 模块 9xxx
    TOOL_SCHEMA_PARSE_ERROR(9001, "Tool Schema 请求解析失败"),
    TOOL_SCHEMA_ACTION_INVALID(9002, "不支持的 Tool Schema 操作类型"),
    TOOL_SCHEMA_RESOURCE_INVALID(9003, "Tool Schema 资源格式无效"),
    TOOL_SCHEMA_PARAMS_INVALID(9004, "Tool Schema 参数格式无效"),
    TOOL_SCHEMA_CONVERSION_ERROR(9005, "Tool Schema 转换为代理请求失败");

    private final int code;
    private final String message;
}
