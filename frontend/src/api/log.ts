/**
 * Agent 日志 API 请求封装
 *
 * @author zhuhx
 */
import request from '@/utils/request'
import type { AgentLog, AgentLogListParams } from '@/types/log'
import type { PageResult } from '@/types/api'

/**
 * 获取日志列表
 */
export function getLogList(params: AgentLogListParams): Promise<PageResult<AgentLog>> {
  return request.get('/logs', { params })
}

/**
 * 获取日志详情
 */
export function getLogById(id: string): Promise<AgentLog> {
  return request.get(`/logs/${id}`)
}
