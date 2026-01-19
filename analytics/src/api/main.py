"""
AgentGuard Analytics 应用入口

@author zhuhx
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from src.api.routes import anomaly, risk, health

app = FastAPI(
    title="AgentGuard Analytics",
    description="AI Agent 治理与监控平台 - 数据分析服务",
    version="0.1.0",
)

# CORS 配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(health.router, tags=["Health"])
app.include_router(anomaly.router, prefix="/api/v1/anomaly", tags=["Anomaly Detection"])
app.include_router(risk.router, prefix="/api/v1/risk", tags=["Risk Scoring"])


@app.get("/")
async def root():
    return {"message": "AgentGuard Analytics Service", "version": "0.1.0"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8081)
