package com.agentguard.alert.dto;

import com.agentguard.alert.enums.AlertStatus;
import com.agentguard.alert.enums.AlertType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警历史数据传输对象
 *
 * @author zhuhx
 */
@Data
@Schema(description = "告警历史信息")
public class AlertHistoryDTO {

    @Schema(description = "记录ID")
    private String id;

    @Schema(description = "告警规则ID")
    private String ruleId;

    @Schema(description = "告警类型")
    private AlertType type;

    @Schema(description = "告警标题")
    private String title;

    @Schema(description = "告警内容")
    private String content;

    @Schema(description = "接收人")
    private String recipient;

    @Schema(description = "通知渠道，多个渠道用逗号分隔")
    private String channelType;

    @Schema(description = "发送状态")
    private AlertStatus status;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
