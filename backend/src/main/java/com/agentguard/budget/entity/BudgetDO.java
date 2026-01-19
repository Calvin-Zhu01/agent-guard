package com.agentguard.budget.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算数据对象
 *
 * @author zhuhx
 */
@Data
@TableName("budget")
public class BudgetDO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 预算月份（格式：YYYY-MM） */
    private String month;

    /** 预算上限金额 */
    private BigDecimal limitAmount;

    /** 告警阈值（0-1之间，如0.8表示80%） */
    private BigDecimal alertThreshold;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
