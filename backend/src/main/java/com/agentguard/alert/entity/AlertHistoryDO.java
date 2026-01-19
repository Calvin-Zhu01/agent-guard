package com.agentguard.alert.entity;

import com.agentguard.alert.enums.AlertStatus;
import com.agentguard.alert.enums.AlertType;
import com.agentguard.alert.enums.NotificationChannelType;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警历史数据对象
 *
 * @author zhuhx
 */
@Data
@TableName("alert_history")
public class AlertHistoryDO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 告警规则ID */
    private String ruleId;

    /** 告警类型：COST/ERROR_RATE/APPROVAL/SYSTEM */
    private AlertType type;

    /** 告警标题 */
    private String title;

    /** 告警内容 */
    private String content;

    /** 接收人 */
    private String recipient;

    /** 通知渠道：EMAIL/WEBHOOK */
    private NotificationChannelType channelType;

    /** 发送状态：SUCCESS/FAILED */
    private AlertStatus status;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
