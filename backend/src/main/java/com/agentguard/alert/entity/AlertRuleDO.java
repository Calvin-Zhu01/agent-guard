package com.agentguard.alert.entity;

import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.enums.NotificationChannelType;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则数据对象
 *
 * @author zhuhx
 */
@Data
@TableName("alert_rule")
public class AlertRuleDO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 规则名称 */
    private String name;

    /** 告警类型：COST/ERROR_RATE/APPROVAL/SYSTEM */
    private AlertType type;

    /** 阈值 */
    private BigDecimal threshold;

    /** 通知渠道：EMAIL/WEBHOOK */
    private NotificationChannelType channelType;

    /** 渠道配置（邮箱地址/Webhook URL） */
    private String channelConfig;

    /** 是否启用：0-停用，1-启用 */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
