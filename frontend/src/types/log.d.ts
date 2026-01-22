/**
 * Agent 日志类型定义
 *
 * @author zhuhx
 */

/** 请求类型 */
export type RequestType = 'API_CALL' | 'LLM_CALL'

/** 响应状态 */
export type ResponseStatus = 'SUCCESS' | 'FAILED' | 'BLOCKED' | 'PENDING_APPROVAL'

/** 策略类型 */
export type PolicyType = 'ACCESS_CONTROL' | 'RATE_LIMIT' | 'APPROVAL'

/** 策略动作 */
export type PolicyAction = 'ALLOW' | 'DENY' | 'APPROVAL' | 'RATE_LIMIT'

/** 策略快照 */
export interface PolicySnapshot {
  id: string
  name: string
  type: PolicyType
  action: PolicyAction
  conditions: string
  reason: string
}

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
  requestHeaders: string
  requestBody: string
  responseBody: string
  responseStatus: ResponseStatus
  responseTimeMs: number
  tokenInput: number
  tokenOutput: number
  model: string
  cost: number
  policySnapshot: PolicySnapshot | null
  createdAt: string
}

/** 日志列表查询参数 */
export interface AgentLogListParams {
  current: number
  size: number
  agentId?: string
  responseStatus?: ResponseStatus
}
