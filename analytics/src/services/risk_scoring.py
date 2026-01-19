"""
风险评分服务

多维度评估 Agent 风险等级

@author zhuhx
"""

from typing import Dict, Any, List


class RiskScorer:
    """Agent 风险评分器"""

    # 风险因子权重
    WEIGHTS = {
        "error_rate": 25,
        "sensitive_api": 30,
        "approval_rejection": 20,
        "response_time": 15,
        "call_volume": 10,
    }

    # 风险等级阈值
    LEVELS = {
        "LOW": (0, 30),
        "MEDIUM": (30, 60),
        "HIGH": (60, 80),
        "CRITICAL": (80, 100),
    }

    def calculate(self, data: Dict[str, Any]) -> Dict[str, Any]:
        """
        计算 Agent 风险评分

        Args:
            data: Agent 数据，包含各项指标

        Returns:
            风险评分结果
        """
        scores = {}
        factors = []
        recommendations = []

        # 错误率评分 (0-25)
        error_rate = data.get("error_rate", 0)
        if error_rate > 0.1:
            scores["error_rate"] = min(error_rate * 100, 25)
            factors.append(f"错误率较高: {error_rate:.1%}")
            recommendations.append("检查 Agent 配置和目标 API 可用性")
        else:
            scores["error_rate"] = error_rate * 50

        # 敏感 API 调用评分 (0-30)
        sensitive_calls = data.get("sensitive_api_calls", 0)
        if sensitive_calls > 10:
            scores["sensitive_api"] = min(sensitive_calls * 2, 30)
            factors.append(f"敏感 API 调用频繁: {sensitive_calls} 次")
            recommendations.append("审查敏感 API 调用必要性，考虑添加审批策略")
        else:
            scores["sensitive_api"] = sensitive_calls * 1.5

        # 审批拒绝评分 (0-20)
        rejections = data.get("approval_rejections", 0)
        if rejections > 3:
            scores["approval_rejection"] = min(rejections * 4, 20)
            factors.append(f"审批拒绝次数较多: {rejections} 次")
            recommendations.append("检查 Agent 行为是否符合业务规范")
        else:
            scores["approval_rejection"] = rejections * 3

        # 响应时间评分 (0-15)
        avg_response = data.get("avg_response_time", 0)
        if avg_response > 2000:
            scores["response_time"] = min((avg_response - 1000) / 200, 15)
            factors.append(f"平均响应时间较长: {avg_response:.0f}ms")
            recommendations.append("优化 Agent 调用链路，考虑缓存策略")
        else:
            scores["response_time"] = max(0, (avg_response - 500) / 200)

        # 调用量评分 (0-10)
        recent_calls = data.get("recent_calls", 0)
        if recent_calls > 10000:
            scores["call_volume"] = min(recent_calls / 2000, 10)
            factors.append(f"调用量较大: {recent_calls} 次")
        else:
            scores["call_volume"] = recent_calls / 2000

        # 计算总分
        total_score = sum(scores.values())
        total_score = min(max(total_score, 0), 100)

        # 确定风险等级
        level = "LOW"
        for lvl, (low, high) in self.LEVELS.items():
            if low <= total_score < high:
                level = lvl
                break

        if not factors:
            factors.append("各项指标正常")
        if not recommendations:
            recommendations.append("继续保持当前配置")

        return {
            "agent_id": data["agent_id"],
            "score": round(total_score, 1),
            "level": level,
            "factors": factors,
            "recommendations": recommendations,
        }
