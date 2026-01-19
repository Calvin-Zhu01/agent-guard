/**
 * 统计相关类型定义
 *
 * @author zhuhx
 */

/** 成本概览 */
export interface StatsOverview {
  totalCost: number
  llmCost: number
  apiCost: number
  totalTokens: number
  tokenInput: number
  tokenOutput: number
  totalCalls: number
  agentCount: number
}

/** 成本趋势 */
export interface CostTrend {
  date: string
  totalCost: number
  llmCost: number
  apiCost: number
  apiCalls: number
  totalTokens: number
}

/** Agent成本排行 */
export interface AgentCostRank {
  agentId: string
  agentName: string
  totalCost: number
  llmCost: number
  apiCost: number
  totalTokens: number
  apiCalls: number
  rank: number
}

/** 预算信息 */
export interface Budget {
  id: string
  month: string
  limitAmount: number
  alertThreshold: number
  createdAt: string
  updatedAt: string
}

/** 预算及使用情况 */
export interface BudgetWithUsage {
  id: string
  month: string
  limitAmount: number
  alertThreshold: number
  usedAmount: number
  usagePercentage: number
  remainingAmount: number
  alertTriggered: boolean
  overBudget: boolean
  createdAt: string
  updatedAt: string
}

/** 创建预算请求 */
export interface BudgetCreateDTO {
  month: string
  limitAmount: number
  alertThreshold?: number
}

/** 更新预算请求 */
export interface BudgetUpdateDTO {
  limitAmount?: number
  alertThreshold?: number
}

/** 统计查询参数 */
export interface StatsQueryParams {
  startDate?: string
  endDate?: string
}

/** TOP Agent 查询参数 */
export interface TopAgentsParams {
  limit?: number
  startDate?: string
  endDate?: string
}
