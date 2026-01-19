/**
 * 告警管理 API 请求封装
 *
 * @author zhuhx
 */
import request from '@/utils/request'
import type {
  AlertRule,
  AlertRuleCreateDTO,
  AlertRuleUpdateDTO,
  AlertRuleListParams,
  AlertHistory,
  AlertHistoryListParams,
  AlertType,
  AlertStatus
} from '@/types/alert'
import type { PageResult } from '@/types/api'

// ==================== 告警规则管理 ====================

/**
 * 获取告警规则列表
 */
export function getAlertRuleList(params: AlertRuleListParams): Promise<PageResult<AlertRule>> {
  return request.get('/alerts/rules', { params })
}

/**
 * 获取告警规则详情
 */
export function getAlertRuleById(id: string): Promise<AlertRule> {
  return request.get(`/alerts/rules/${id}`)
}

/**
 * 创建告警规则
 */
export function createAlertRule(data: AlertRuleCreateDTO): Promise<AlertRule> {
  return request.post('/alerts/rules', data)
}

/**
 * 更新告警规则
 */
export function updateAlertRule(id: string, data: AlertRuleUpdateDTO): Promise<AlertRule> {
  return request.put(`/alerts/rules/${id}`, data)
}

/**
 * 删除告警规则
 */
export function deleteAlertRule(id: string): Promise<void> {
  return request.delete(`/alerts/rules/${id}`)
}

/**
 * 启用告警规则
 */
export function enableAlertRule(id: string): Promise<void> {
  return request.post(`/alerts/rules/${id}/enable`)
}

/**
 * 停用告警规则
 */
export function disableAlertRule(id: string): Promise<void> {
  return request.post(`/alerts/rules/${id}/disable`)
}

/**
 * 获取所有已启用的告警规则
 */
export function getEnabledAlertRules(): Promise<AlertRule[]> {
  return request.get('/alerts/rules/enabled')
}

// ==================== 告警历史查询 ====================

/**
 * 获取告警历史列表
 */
export function getAlertHistoryList(params: AlertHistoryListParams): Promise<PageResult<AlertHistory>> {
  return request.get('/alerts/history', { params })
}

/**
 * 获取告警历史详情
 */
export function getAlertHistoryById(id: string): Promise<AlertHistory> {
  return request.get(`/alerts/history/${id}`)
}

/**
 * 统计告警数量
 */
export function countAlertHistory(params?: {
  type?: AlertType
  status?: AlertStatus
  startTime?: string
  endTime?: string
}): Promise<number> {
  return request.get('/alerts/history/count', { params })
}

/**
 * 导出告警历史数据
 */
export function exportAlertHistory(params?: {
  type?: AlertType
  status?: AlertStatus
  startTime?: string
  endTime?: string
}): Promise<AlertHistory[]> {
  return request.get('/alerts/history/export', { params })
}

/**
 * 获取最近的告警历史
 */
export function getRecentAlertHistory(limit: number = 10): Promise<AlertHistory[]> {
  return request.get('/alerts/history/recent', { params: { limit } })
}

/**
 * 根据规则ID查询告警历史
 */
export function getAlertHistoryByRuleId(
  ruleId: string,
  params: { current: number; size: number }
): Promise<PageResult<AlertHistory>> {
  return request.get(`/alerts/history/rule/${ruleId}`, { params })
}
