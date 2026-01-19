"""
异常检测服务

基于 Isolation Forest 算法检测 Agent 行为异常

@author zhuhx
"""

from typing import List, Dict, Any
from datetime import datetime
import numpy as np
from sklearn.ensemble import IsolationForest


class AnomalyDetector:
    """基于 Isolation Forest 的异常检测器"""

    def __init__(self, contamination: float = 0.1):
        self.contamination = contamination
        self.model = IsolationForest(contamination=contamination, random_state=42)

    def detect(self, tenant_id: str, logs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        检测日志中的异常

        Args:
            tenant_id: 租户ID
            logs: 日志列表

        Returns:
            异常结果列表
        """
        if not logs:
            return []

        # 提取特征
        features = self._extract_features(logs)
        if len(features) < 10:
            # 数据量太少，使用简单规则检测
            return self._rule_based_detection(logs)

        # 使用 Isolation Forest 检测
        predictions = self.model.fit_predict(features)
        anomalies = []

        for i, (log, pred) in enumerate(zip(logs, predictions)):
            if pred == -1:  # 异常
                anomaly = self._create_anomaly_result(log, features[i])
                anomalies.append(anomaly)

        return anomalies

    def _extract_features(self, logs: List[Dict[str, Any]]) -> np.ndarray:
        """提取特征向量"""
        features = []
        for log in logs:
            feature = [
                log.get("response_time_ms", 0),
                log.get("token_count", 0) or 0,
                log.get("cost", 0) or 0,
            ]
            features.append(feature)
        return np.array(features)

    def _rule_based_detection(self, logs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """基于规则的简单异常检测"""
        anomalies = []
        for log in logs:
            # 响应时间异常
            if log.get("response_time_ms", 0) > 5000:
                anomalies.append(
                    {
                        "agent_id": log["agent_id"],
                        "anomaly_type": "response_time",
                        "severity": "MEDIUM",
                        "description": f"响应时间过长: {log['response_time_ms']}ms",
                        "timestamp": log["timestamp"],
                    }
                )
            # 成本异常
            if log.get("cost", 0) and log["cost"] > 1.0:
                anomalies.append(
                    {
                        "agent_id": log["agent_id"],
                        "anomaly_type": "cost_spike",
                        "severity": "HIGH",
                        "description": f"单次调用成本过高: ${log['cost']:.2f}",
                        "timestamp": log["timestamp"],
                    }
                )
        return anomalies

    def _create_anomaly_result(
        self, log: Dict[str, Any], features: np.ndarray
    ) -> Dict[str, Any]:
        """创建异常结果"""
        # 判断异常类型
        anomaly_type = "unknown"
        severity = "MEDIUM"
        description = "检测到异常行为"

        if features[0] > 3000:  # 响应时间
            anomaly_type = "response_time"
            description = f"响应时间异常: {features[0]:.0f}ms"
        elif features[2] > 0.5:  # 成本
            anomaly_type = "cost_spike"
            severity = "HIGH"
            description = f"成本异常: ${features[2]:.2f}"

        return {
            "agent_id": log["agent_id"],
            "anomaly_type": anomaly_type,
            "severity": severity,
            "description": description,
            "timestamp": log["timestamp"],
        }
