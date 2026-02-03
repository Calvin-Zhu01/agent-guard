package com.agentguard.settings.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统设置实体
 *
 * @author zhuhx
 */
@Data
@TableName("system_settings")
public class SystemSettingsDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 设置键
     */
    @TableField("setting_key")
    private String settingKey;

    /**
     * 设置值
     */
    @TableField("setting_value")
    private String settingValue;

    /**
     * 设置分类（alert_email, alert_dingtalk, alert_wecom等）
     */
    @TableField("category")
    private String category;

    /**
     * 设置描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否加密存储
     */
    @TableField("encrypted")
    private Boolean encrypted;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 是否删除
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}
