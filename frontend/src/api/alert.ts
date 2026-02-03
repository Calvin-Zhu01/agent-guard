import request from '@/utils/request'
import type { AlertHistory, AlertHistoryQuery } from '@/types/alert'

/**
 * 告警历史 API（仅查询功能）
 * 告警规则配置已移至系统设置模块
 */

// ==================== 告警历史查询 ====================

/**
 * 分页查询告警历史
 */
export function getAlertHistoryList(params: AlertHistoryQuery) {
  return request.get<{
    records: AlertHistory[]
    total: number
    current: number
    size: number
  }>('/alerts/history', { params })
}

/**
 * 获取告警历史详情
 */
export function getAlertHistoryById(id: string) {
  return request.get<AlertHistory>(`/alerts/history/${id}`)
}

/**
 * 统计告警数量
 */
export function countAlertHistory(params: Omit<AlertHistoryQuery, 'current' | 'size'>) {
  return request.get<number>('/alerts/history/count', { params })
}

/**
 * 获取最近的告警历史
 */
export function getRecentAlertHistory(limit: number = 10) {
  return request.get<AlertHistory[]>('/alerts/history/recent', { params: { limit } })
}

/**
 * 导出告警历史数据
 */
export function exportAlertHistory(params: Omit<AlertHistoryQuery, 'current' | 'size'>) {
  return request.get<AlertHistory[]>('/alerts/history/export', { params })
}
