<p align="center">
  <img src="./frontend/public/logo-horizontal.svg" alt="AgentGuard" width="400" height="100" />
</p>

<p align="center">
  <strong>AI Agent 治理与监控平台</strong>
</p>

<p align="center">
  帮助团队「管住自己到处跑的 AI Agent」，让 Agent 可控、可审计、算得清成本
</p>

<p align="center">
  <a href="./README.md">English</a> | <a href="./README_CN.md">中文</a>
</p>

---

## ✨ 核心功能

- **📊 统一 Agent 视图** - 在一个平台注册和管理所有 AI Agent
- **📝 行为日志记录** - 记录每次 API 调用和 LLM 请求的完整上下文
- **🔒 策略引擎** - 基于 URL 模式定义访问控制、频率限制和审批流程
- **💰 成本分析** - 追踪所有 Agent 的 Token 使用量和成本
- **🚨 智能告警** - 异常行为或策略违规时及时通知
- **🔌 易于集成** - 透明 HTTP 代理模式，支持可选的元数据标注

## 🎯 解决的痛点

| 痛点 | 描述 |
|-----|------|
| 看不见 | 不知道有多少 Agent，在干什么，调了哪些接口 |
| 管不住 | 担心 Agent 乱发邮件、乱调支付接口、乱读隐私文档 |
| 算不清 | 不知道每个 Agent 的 LLM/接口成本，无法评估 ROI |

## 🚀 快速开始

### Docker Compose（推荐）

```bash
# 克隆项目
git clone https://github.com/your-org/agent-guard.git
cd agent-guard

# 启动所有服务
docker-compose up -d

# 访问控制台
open http://localhost:3000
```

### 手动部署

详见 [开发指南](./docs/development.md)。

## 🏗️ 系统架构

AgentGuard 采用**透明 HTTP 代理**模式，追求极致的简单性和灵活性：

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (Vue 3)                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  监控仪表盘  │  │  策略配置   │  │  成本分析           │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                   后端 (Spring Boot)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ HTTP 代理   │  │  策略引擎   │  │  审批服务            │  │
│  │ (URL 路由)  │  │ (JSON配置)  │  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      数据层                                  │
│  ┌─────────────┐  ┌─────────────┐                          │
│  │   MySQL     │  │    Redis    │                          │
│  └─────────────┘  └─────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

### 工作原理

1. **Agent 发送 HTTP 请求**到 AgentGuard 代理端点
2. **策略引擎评估**基于 URL 模式、方法、请求头
3. **触发审批流程**（如果是高风险操作）
4. **转发请求**到目标 API，同时记录完整日志
5. **返回响应**给 Agent，并追踪成本

### 集成示例

```bash
# 原本直接调用 API：
curl https://api.example.com/v1/users

# 通过 AgentGuard 代理：
curl https://agentguard.example.com/api/v1/proxy \
  -H "X-Agent-ID: agent-123" \
  -H "X-Target-URL: https://api.example.com/v1/users" \
  -H "Content-Type: application/json" \
  -d '{
    "method": "GET",
    "metadata": {
      "operation": "list_users",
      "reason": "每日同步"
    }
  }'
```

**优势：**
- ✅ 无需修改现有 Agent 代码
- ✅ 适用于任何 HTTP API
- ✅ 简单的 URL 模式策略规则
- ✅ 可选的业务元数据标注
- ✅ 完整的请求/响应日志

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|-----|------|------|
| 后端框架 | Spring Boot | 3.2+ |
| JDK | OpenJDK | 21 |
| ORM | MyBatis-Plus | 3.5+ |
| 前端框架 | Vue | 3.4+ |
| UI 组件库 | Element Plus | 2.5+ |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 7.0+ |

## 📖 文档

暂无文档，待完善。

## 🤝 参与贡献

我们欢迎各种形式的贡献！请查看 [贡献指南](./CONTRIBUTING.md)。

- 🐛 报告 Bug
- 💡 提出新功能建议
- 📖 改进文档
- 🔧 提交代码

## 📄 许可证

[MIT License](./LICENSE) - 可自由使用、修改和分发。

## 🌟 Star 历史

如果这个项目对你有帮助，请给个 Star！⭐

## 📬 联系我们

- GitHub Issues: [报告问题](https://github.com/Calvin-Zhu01/agent-guard/issues)
