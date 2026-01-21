# AgentGuard Backend

AI Agent 治理与监控平台后端服务

## 技术栈

- Java 21
- Spring Boot 3.2+
- Spring Security 6.2+
- MyBatis-Plus 3.5+
- MySQL 8.0+
- Redis 7.0+

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 8.0+
- Redis 7.0+

### 本地开发

1. 创建数据库

```sql
CREATE DATABASE agent_guard DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改配置

编辑 `src/main/resources/application-dev.yml`，配置数据库和Redis连接信息。

3. 启动服务

```bash
mvn spring-boot:run
```

4. 访问API文档

打开浏览器访问 http://localhost:8080/swagger-ui.html

## 项目结构

```
src/main/java/com/agentguard/
├── AgentGuardApplication.java    # 启动类
├── common/                       # 通用模块
│   ├── config/                   # 配置类
│   ├── exception/                # 异常处理
│   ├── response/                 # 统一响应
│   └── util/                     # 工具类
├── security/                     # 安全模块
├── agent/                        # Agent管理模块
├── policy/                       # 策略管理模块
├── approval/                     # 审批模块
├── log/                          # 日志模块
├── stats/                        # 统计模块
├── proxy/                        # Agent代理模块
├── alert/                        # 告警模块
└── tenant/                       # 多租户模块
```

## API 接口

| 模块 | 路径 | 说明 |
|-----|------|------|
| Agent | /api/v1/agents | Agent管理 |
| Policy | /api/v1/policies | 策略管理 |
| Approval | /api/v1/approvals | 审批管理 |
| Stats | /api/v1/stats | 统计分析 |
| Proxy | /proxy/v1 | Agent代理 |
