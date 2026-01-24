package com.agentguard.stats.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 模型价格配置实体
 *
 * @author zhuhx
 */
@Data
@TableName("model_price")
public class ModelPriceDO {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** LLM提供商 */
    private String provider;

    /** 模型名称 */
    private String modelName;

    /** 输入价格（美元/百万Token） */
    private BigDecimal inputPrice;

    /** 输出价格（美元/百万Token） */
    private BigDecimal outputPrice;

    /** 货币单位 */
    private String currency;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 是否启用 */
    private Boolean enabled;

    /** 价格说明 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
