package com.agentguard.log.dto;

import com.agentguard.log.enums.RequestType;
import com.agentguard.log.enums.ResponseStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agent日志数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "Agent日志信息")
public class AgentLogDTO {

    @Schema(description = "日志ID")
    private String id;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "AgentID")
    private String agentId;

    @Schema(description = "Agent名称（关联查询）")
    private String agentName;

    @Schema(description = "请求类型：API_CALL/LLM_CALL")
    private RequestType requestType;

    @Schema(description = "请求地址")
    private String endpoint;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "请求摘要（JSON格式）")
    private String requestSummary;

    @Schema(description = "响应状态：SUCCESS/FAILED/BLOCKED")
    private ResponseStatus responseStatus;

    @Schema(description = "响应时间（毫秒）")
    private Integer responseTimeMs;

    @Schema(description = "输入token数")
    private Integer tokenInput;

    @Schema(description = "输出token数")
    private Integer tokenOutput;

    @Schema(description = "LLM模型")
    private String model;

    @Schema(description = "成本")
    private BigDecimal cost;

    @Schema(description = "触发的策略ID")
    private String policyId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
