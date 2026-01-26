/**
 * 策略相关类型定义
 *
 * @author zhuhx
 */

/** 策略类型 */
export type PolicyType = 'ACCESS_CONTROL' | 'RATE_LIMIT' | 'APPROVAL'

/** 策略动作 */
export type PolicyAction = 'ALLOW' | 'DENY' | 'APPROVAL' | 'RATE_LIMIT'

/** 策略作用域 */
export type PolicyScope = 'GLOBAL' | 'AGENT'

/** 请求类型 */
export type RequestType = 'LLM_CALL' | 'API_CALL' | 'ALL'

/** 策略信息 */
export interface Policy {
  id: string
  name: string
  description: string
  type: PolicyType
  conditions: string
  action: PolicyAction
  priority: number
  scope: PolicyScope
  agentId?: string
  requestType?: RequestType
  enabled: boolean
  createdAt: string
  updatedAt: string
}

/** 创建策略请求 */
export interface PolicyCreateDTO {
  name: string
  description?: string
  type: PolicyType
  conditions?: string
  action: PolicyAction
  priority?: number
  scope?: PolicyScope
  agentId?: string
  requestType?: RequestType
}

/** 更新策略请求 */
export interface PolicyUpdateDTO {
  name?: string
  description?: string
  type?: PolicyType
  conditions?: string
  action?: PolicyAction
  priority?: number
  scope?: PolicyScope
  agentId?: string
  requestType?: RequestType
}

/** 排序方式 */
export type PolicySortBy = 'priority_desc' | 'priority_asc' | 'updated_desc' | 'updated_asc'

/** 策略列表查询参数 */
export interface PolicyListParams {
  current: number
  size: number
  keyword?: string
  type?: PolicyType
  scope?: PolicyScope
  sortBy?: PolicySortBy
}
