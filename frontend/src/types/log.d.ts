/**
 * Agent 日志类型定义
 *
 * @author zhuhx
 */

/** 请求类型 */
export type RequestType = 'API_CALL' | 'LLM_CALL'

/** 响应状态 */
export type ResponseStatus = 'SUCCESS' | 'FAILED' | 'BLOCKED'

/** Agent 日志 */
export interface AgentLog {
  id: string
  tenantId: string
  agentId: string
  agentName: string
  requestType: RequestType
  endpoint: string
  method: string
  requestSummary: string
  responseStatus: ResponseStatus
  responseTimeMs: number
  tokenInput: number
  tokenOutput: number
  model: string
  cost: number
  policyId: string
  createdAt: string
}

/** 日志列表查询参数 */
export interface AgentLogListParams {
  current: number
  size: number
  agentId?: string
  responseStatus?: ResponseStatus
}
