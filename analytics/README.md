# AgentGuard Analytics

AI Agent 治理与监控平台 - 数据分析服务

## 技术栈

- Python 3.11+
- FastAPI
- pandas / numpy / scikit-learn
- SQLAlchemy
- Redis

## 功能

- 异常检测：基于 Isolation Forest 算法检测 Agent 行为异常
- 风险评分：多维度评估 Agent 风险等级
- 模式分析：识别 Agent 调用模式和趋势

## 快速开始

### 环境要求

- Python 3.11+
- Poetry

### 安装依赖

```bash
poetry install
```

### 启动服务

```bash
poetry run uvicorn src.api.main:app --host 0.0.0.0 --port 8081 --reload
```

### 访问 API 文档

打开浏览器访问 http://localhost:8081/docs

## 项目结构

```
src/
├── api/                    # FastAPI 接口
│   ├── main.py            # 应用入口
│   └── routes/            # 路由模块
├── services/              # 业务服务
│   ├── anomaly_detection.py
│   ├── risk_scoring.py
│   └── pattern_analysis.py
├── models/                # 数据模型
│   └── schemas.py
└── utils/                 # 工具函数
    └── db.py
```

## API 接口

| 方法 | 路径 | 说明 |
|-----|------|------|
| GET | /health | 健康检查 |
| POST | /api/v1/anomaly/detect | 异常检测 |
| GET | /api/v1/anomaly/summary/{tenant_id} | 异常统计 |
| POST | /api/v1/risk/score | 风险评分 |
| GET | /api/v1/risk/agents/{tenant_id} | Agent风险排名 |
