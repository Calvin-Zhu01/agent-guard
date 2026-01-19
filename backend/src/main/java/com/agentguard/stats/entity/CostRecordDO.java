package com.agentguard.stats.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成本记录数据对象
 *
 * @author zhuhx
 */
@Data
@TableName("cost_record")
public class CostRecordDO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** AgentID */
    private String agentId;

    /** 日期 */
    private LocalDate date;

    /** LLM模型 */
    private String model;

    /** 输入token总数 */
    private Long tokenInput;

    /** 输出token总数 */
    private Long tokenOutput;

    /** API调用次数 */
    private Integer apiCalls;

    /** LLM成本 */
    private BigDecimal llmCost;

    /** API成本 */
    private BigDecimal apiCost;

    /** 总成本 */
    private BigDecimal totalCost;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
