<p align="center">
  <img src="./frontend/public/logo-horizontal.svg" alt="AgentGuard" width="400" height="100" />
</p>

<p align="center">
  <strong>AI Agent Governance & Monitoring Platform</strong>
</p>

<p align="center">
  Help teams manage their AI Agents - controllable, auditable, cost-trackable
</p>

<p align="center">
  <a href="./README.md">English</a> | <a href="./README_CN.md">中文</a>
</p>

---

## ✨ Features

- **📊 Unified Agent View** - Register and manage all AI Agents in one platform
- **📝 Behavior Logging** - Record complete context of every API call and LLM request
- **🔒 Policy Engine** - Define access control, rate limits, and approval workflows based on URL patterns
- **💰 Cost Analytics** - Track token usage and costs across all Agents
- **🚨 Smart Alerts** - Get notified on anomalies or policy violations
- **🔌 Easy Integration** - Transparent HTTP proxy mode with optional metadata support

## 🎯 Problems We Solve

| Problem | Description |
|---------|-------------|
| Can't See | Don't know how many Agents exist, what they're doing, which APIs they call |
| Can't Control | Worried about Agents sending emails, making payments, accessing sensitive data |
| Can't Calculate | Don't know the LLM/API costs per Agent, can't evaluate ROI |

## 🚀 Quick Start

### Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/your-org/agent-guard.git
cd agent-guard

# Start all services
docker-compose up -d

# Access the dashboard
open http://localhost:3000
```

### Manual Setup

See [Development Guide](./docs/development.md) for detailed instructions.

## 🏗️ Architecture

AgentGuard uses a **transparent HTTP proxy** approach for maximum simplicity and flexibility:

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Vue 3)                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Dashboard  │  │   Policy    │  │  Cost Analytics     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                  Backend (Spring Boot)                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ HTTP Proxy  │  │Policy Engine│  │  Approval Service   │  │
│  │  (URL-based)│  │(JSON Config)│  │                     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                              │
│  ┌─────────────┐  ┌─────────────┐                          │
│  │   MySQL     │  │    Redis    │                          │
│  └─────────────┘  └─────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

### How It Works

1. **Agent sends HTTP request** to AgentGuard proxy endpoint
2. **Policy engine evaluates** based on URL pattern, method, headers
3. **Approval workflow** triggers if needed (high-risk operations)
4. **Request forwarded** to target API with full logging
5. **Response returned** to Agent with cost tracking

### Integration Example

```bash
# Instead of calling API directly:
curl https://api.example.com/v1/users

# Route through AgentGuard:
curl https://agentguard.example.com/api/v1/proxy \
  -H "X-Agent-ID: agent-123" \
  -H "X-Target-URL: https://api.example.com/v1/users" \
  -H "Content-Type: application/json" \
  -d '{
    "method": "GET",
    "metadata": {
      "operation": "list_users",
      "reason": "Daily sync"
    }
  }'
```

**Benefits:**
- ✅ No code changes to existing Agents
- ✅ Works with any HTTP API
- ✅ Simple URL-based policy rules
- ✅ Optional metadata for business context
- ✅ Full request/response logging

## 🛠️ Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| Backend | Spring Boot | 3.2+ |
| JDK | OpenJDK | 21 |
| ORM | MyBatis-Plus | 3.5+ |
| Frontend | Vue | 3.4+ |
| UI | Element Plus | 2.5+ |
| Database | MySQL | 8.0+ |
| Cache | Redis | 7.0+ |

## 📖 Documentation

No documents available at present. Under construction.

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](./CONTRIBUTING.md).

- 🐛 Report bugs
- 💡 Suggest features
- 📖 Improve documentation
- 🔧 Submit pull requests

## 📄 License

[MIT License](./LICENSE) - Free to use, modify, and distribute.

## 🌟 Star History

If you find this project useful, please give it a star! ⭐

## 📬 Contact

- GitHub Issues: [Report a bug](https://github.com/Calvin-Zhu01/agent-guard/issues)