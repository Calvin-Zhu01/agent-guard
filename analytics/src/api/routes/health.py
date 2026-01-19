"""
健康检查 API 路由

@author zhuhx
"""

from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
async def health_check():
    """健康检查接口"""
    return {"status": "healthy", "service": "analytics"}
