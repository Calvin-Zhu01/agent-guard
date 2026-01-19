/**
 * 统计分析 API 请求封装
 *
 * @author zhuhx
 */
import request from '@/utils/request'
import type {
  StatsOverview,
  CostTrend,
  AgentCostRank,
  StatsQueryParams,
  TopAgentsParams,
  BudgetWithUsage
} from '@/types/stats'

/**
 * 获取成本概览
 */
export function getStatsOverview(params?: StatsQueryParams): Promise<StatsOverview> {
  return request.get('/stats/overview', { params })
}

/**
 * 获取成本趋势
 */
export function getCostTrends(params?: StatsQueryParams): Promise<CostTrend[]> {
  return request.get('/stats/trends', { params })
}

/**
 * 获取 TOP Agent 成本排行
 */
export function getTopAgents(params?: TopAgentsParams): Promise<AgentCostRank[]> {
  return request.get('/stats/top-agents', { params })
}

/**
 * 获取当月预算及使用情况
 */
export function getCurrentBudget(): Promise<BudgetWithUsage> {
  return request.get('/budgets/current')
}
