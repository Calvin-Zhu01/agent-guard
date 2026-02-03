package com.agentguard.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 数据对象
 *
 * @author zhuhx
 */
@Data
@TableName("agent")
public class AgentDO {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** Agent名称 */
    private String name;

    /** API密钥 */
    private String apiKey;

    /** 描述信息 */
    private String description;

    /** LLM提供商：openai/anthropic/azure */
    private String llmProvider;

    /** 真实的LLM API密钥（加密存储） */
    private String llmApiKey;

    /** LLM API地址 */
    private String llmBaseUrl;

    /** 默认模型 */
    private String llmModel;

    /** Agent状态：0-禁用，1-启用 */
    private Integer status;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
