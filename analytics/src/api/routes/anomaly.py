"""
异常检测 API 路由

@author zhuhx
"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

from src.services.anomaly_detection import AnomalyDetector

router = APIRouter()
detector = AnomalyDetector()


class LogEntry(BaseModel):
    agent_id: str
    timestamp: datetime
    request_type: str
    response_time_ms: int
    token_count: Optional[int] = None
    cost: Optional[float] = None


class AnomalyRequest(BaseModel):
    tenant_id: str
    logs: List[LogEntry]


class AnomalyResult(BaseModel):
    agent_id: str
    anomaly_type: str
    severity: str  # LOW, MEDIUM, HIGH
    description: str
    timestamp: datetime


@router.post("/detect", response_model=List[AnomalyResult])
async def detect_anomalies(request: AnomalyRequest):
    """检测日志中的异常行为"""
    results = detector.detect(request.tenant_id, [log.model_dump() for log in request.logs])
    return results


@router.get("/summary/{tenant_id}")
async def get_anomaly_summary(tenant_id: str, days: int = 7):
    """获取异常统计摘要"""
    return {
        "tenant_id": tenant_id,
        "period_days": days,
        "total_anomalies": 5,
        "by_severity": {"HIGH": 1, "MEDIUM": 2, "LOW": 2},
        "by_type": {"response_time": 2, "cost_spike": 2, "error_rate": 1},
    }
