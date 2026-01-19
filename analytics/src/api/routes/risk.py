"""
风险评分 API 路由

@author zhuhx
"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Optional

from src.services.risk_scoring import RiskScorer

router = APIRouter()
scorer = RiskScorer()


class AgentRiskRequest(BaseModel):
    agent_id: str
    tenant_id: str
    recent_calls: int
    error_rate: float
    avg_response_time: float
    sensitive_api_calls: int
    approval_rejections: int


class RiskScore(BaseModel):
    agent_id: str
    score: float  # 0-100
    level: str  # LOW, MEDIUM, HIGH, CRITICAL
    factors: List[str]
    recommendations: List[str]


@router.post("/score", response_model=RiskScore)
async def calculate_risk_score(request: AgentRiskRequest):
    """计算 Agent 风险评分"""
    result = scorer.calculate(request.model_dump())
    return result


@router.get("/agents/{tenant_id}")
async def get_agents_risk_ranking(tenant_id: str, limit: int = 10):
    """获取租户下 Agent 风险排名"""
    return {
        "tenant_id": tenant_id,
        "agents": [
            {"agent_id": "agent-1", "name": "财务Agent", "score": 75, "level": "HIGH"},
            {"agent_id": "agent-2", "name": "客服Agent", "score": 35, "level": "LOW"},
            {"agent_id": "agent-3", "name": "运营Agent", "score": 52, "level": "MEDIUM"},
        ],
    }
