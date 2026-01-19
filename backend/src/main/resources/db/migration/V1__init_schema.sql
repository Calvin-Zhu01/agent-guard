-- AgentGuard 初始化数据库脚本
-- 包含所有表结构和初始数据

-- =====================================================
-- 用户模块
-- =====================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `email` VARCHAR(100) COMMENT '邮箱',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_username` (`username`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =====================================================
-- Agent 模块
-- =====================================================

-- Agent表
CREATE TABLE IF NOT EXISTS `agent` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT 'Agent名称',
    `type` VARCHAR(20) NOT NULL COMMENT '类型: CUSTOMER_SERVICE/FINANCE/OPERATION/INTERNAL/OTHER',
    `department` VARCHAR(50) COMMENT '所属部门',
    `environment` VARCHAR(20) NOT NULL DEFAULT 'TEST' COMMENT '环境: TEST/PRODUCTION',
    `api_key` VARCHAR(64) NOT NULL COMMENT 'API密钥',
    `description` TEXT COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-停用 1-活跃',
    `last_active_at` DATETIME COMMENT '最后活跃时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent表';

-- Agent日志表
CREATE TABLE IF NOT EXISTS `agent_log` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `agent_id` VARCHAR(36) NOT NULL COMMENT 'AgentID',
    `request_type` VARCHAR(20) NOT NULL COMMENT '请求类型: API_CALL/LLM_CALL',
    `endpoint` VARCHAR(500) COMMENT '请求地址',
    `method` VARCHAR(10) COMMENT '请求方法',
    `request_summary` JSON COMMENT '请求摘要',
    `response_status` VARCHAR(20) NOT NULL COMMENT '响应状态: SUCCESS/FAILED/BLOCKED',
    `response_time_ms` INT COMMENT '响应时间(ms)',
    `token_input` INT COMMENT '输入token数',
    `token_output` INT COMMENT '输出token数',
    `model` VARCHAR(50) COMMENT 'LLM模型',
    `cost` DECIMAL(10,6) COMMENT '成本',
    `policy_id` VARCHAR(36) COMMENT '触发的策略ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent日志表';

-- =====================================================
-- 策略模块
-- =====================================================

-- 策略表
CREATE TABLE IF NOT EXISTS `policy` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '策略名称',
    `description` TEXT COMMENT '策略描述',
    `type` VARCHAR(30) NOT NULL COMMENT '类型: ACCESS_CONTROL/CONTENT_PROTECTION/APPROVAL/RATE_LIMIT',
    `conditions` JSON COMMENT '条件配置（JSON格式）',
    `action` VARCHAR(20) NOT NULL COMMENT '动作: ALLOW/DENY/APPROVAL/MASK/RATE_LIMIT',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级（数值越大优先级越高）',
    `scope` VARCHAR(20) NOT NULL DEFAULT 'GLOBAL' COMMENT '作用域: GLOBAL-全局, AGENT-Agent级别',
    `agent_id` VARCHAR(36) NULL COMMENT '关联的Agent ID（仅当scope为AGENT时有效）',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-停用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_name` (`name`, `deleted`),
    KEY `idx_scope` (`scope`),
    KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略表';

-- Agent策略绑定表
CREATE TABLE IF NOT EXISTS `agent_policy_binding` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `agent_id` VARCHAR(36) NOT NULL COMMENT 'Agent ID',
    `policy_id` VARCHAR(36) NOT NULL COMMENT '策略 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_agent_policy` (`agent_id`, `policy_id`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_policy_id` (`policy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent策略绑定表';

-- =====================================================
-- 审批模块
-- =====================================================

-- 审批请求表
CREATE TABLE IF NOT EXISTS `approval_request` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `policy_id` VARCHAR(36) NOT NULL COMMENT '策略ID',
    `agent_id` VARCHAR(36) NOT NULL COMMENT 'AgentID',
    `request_data` JSON NOT NULL COMMENT '请求数据',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/EXPIRED',
    `approver_id` VARCHAR(36) COMMENT '审批人ID',
    `approved_at` DATETIME COMMENT '审批时间',
    `remark` TEXT COMMENT '审批备注',
    `execution_status` VARCHAR(20) COMMENT '执行状态: NOT_EXECUTED/EXECUTING/SUCCESS/FAILED',
    `execution_result` TEXT COMMENT '执行结果（JSON格式）',
    `executed_at` DATETIME COMMENT '执行时间',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_status` (`status`),
    KEY `idx_agent_id` (`agent_id`),
    KEY `idx_execution_status` (`execution_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批请求表';

-- =====================================================
-- 成本模块
-- =====================================================

-- 成本记录表
CREATE TABLE IF NOT EXISTS `cost_record` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `agent_id` VARCHAR(36) NOT NULL COMMENT 'AgentID',
    `date` DATE NOT NULL COMMENT '日期',
    `model` VARCHAR(50) COMMENT 'LLM模型',
    `token_input` BIGINT NOT NULL DEFAULT 0 COMMENT '输入token总数',
    `token_output` BIGINT NOT NULL DEFAULT 0 COMMENT '输出token总数',
    `api_calls` INT NOT NULL DEFAULT 0 COMMENT 'API调用次数',
    `llm_cost` DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT 'LLM成本',
    `api_cost` DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT 'API成本',
    `total_cost` DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '总成本',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_agent_date_model` (`agent_id`, `date`, `model`),
    KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成本记录表';

-- 预算配置表
CREATE TABLE IF NOT EXISTS `budget` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `month` VARCHAR(7) NOT NULL COMMENT '月份 YYYY-MM',
    `limit_amount` DECIMAL(10,2) NOT NULL COMMENT '预算上限',
    `alert_threshold` DECIMAL(3,2) NOT NULL DEFAULT 0.80 COMMENT '告警阈值 0-1',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_month` (`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算配置表';

-- =====================================================
-- 告警模块
-- =====================================================

-- 告警规则表
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `type` VARCHAR(30) NOT NULL COMMENT '告警类型: COST/ERROR_RATE/APPROVAL/SYSTEM',
    `threshold` DECIMAL(10,4) COMMENT '阈值',
    `channel_type` VARCHAR(20) NOT NULL COMMENT '通知渠道: EMAIL/WEBHOOK',
    `channel_config` JSON NOT NULL COMMENT '渠道配置（邮箱地址/Webhook URL）',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-停用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_name` (`name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- 告警历史表
CREATE TABLE IF NOT EXISTS `alert_history` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `rule_id` VARCHAR(36) COMMENT '告警规则ID',
    `type` VARCHAR(30) NOT NULL COMMENT '告警类型: COST/ERROR_RATE/APPROVAL/SYSTEM',
    `title` VARCHAR(200) NOT NULL COMMENT '告警标题',
    `content` TEXT NOT NULL COMMENT '告警内容',
    `recipient` VARCHAR(500) NOT NULL COMMENT '接收人',
    `channel_type` VARCHAR(20) NOT NULL COMMENT '通知渠道: EMAIL/WEBHOOK',
    `status` VARCHAR(20) NOT NULL COMMENT '发送状态: SUCCESS/FAILED',
    `error_message` TEXT COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警历史表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 初始化管理员账号
-- 用户名: admin
-- 密码: admin123
INSERT INTO `user` (`id`, `username`, `password`, `email`, `role`, `status`, `created_at`, `updated_at`, `deleted`)
VALUES (
    '936e9abf07c8f7a8be417bb3d34e6829',
    'admin',
    '$2a$10$/RBL4dSQp1mlWi/g2NPnO.gmWOYMtapZA9RhLW6/5CsXuzjix9hg6',
    'admin@agentguard.com',
    'ADMIN',
    1,
    NOW(),
    NOW(),
    0
);
