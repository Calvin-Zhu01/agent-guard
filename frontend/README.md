# AgentGuard Frontend

AI Agent 治理与监控平台前端

## 技术栈

- Vue 3.4+
- TypeScript 5.0+
- Vite 5.0+
- Element Plus 2.5+
- ECharts 5.5+
- Pinia
- Vue Router

## 快速开始

### 环境要求

- Node.js 18+
- npm 9+ 或 pnpm 8+

### 安装依赖

```bash
# 使用 npm
npm install

# 或使用 pnpm
pnpm install
```

### 启动开发服务器

```bash
# 使用 npm
npm run dev

# 或使用 pnpm
pnpm dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
pnpm build
```

### Windows PowerShell 执行策略问题

如果遇到 "禁止运行脚本" 错误，以管理员身份运行 PowerShell 执行：

```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

或者直接使用 cmd 运行 pnpm 命令。

## 项目结构

```
src/
├── api/            # API 请求封装
├── assets/         # 静态资源
├── components/     # 通用组件
├── composables/    # 组合式函数
├── layouts/        # 布局组件
├── pages/          # 页面组件
├── router/         # 路由配置
├── stores/         # Pinia 状态管理
├── types/          # TypeScript 类型定义
├── utils/          # 工具函数
├── App.vue
└── main.ts
```

## 页面说明

| 路径 | 页面 | 说明 |
|-----|------|------|
| /dashboard | 仪表盘 | 数据概览、调用趋势 |
| /agents | Agent管理 | Agent的增删改查 |
| /policies | 策略管理 | 安全策略配置 |
| /approvals | 审批中心 | 待审批任务处理 |
| /stats | 成本分析 | 成本统计与报表 |
| /settings | 系统设置 | 告警、用户等配置 |
