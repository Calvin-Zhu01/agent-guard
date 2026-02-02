/**
 * Agent API 请求封装
 *
 * @author zhuhx
 */
import request from '@/utils/request'
import type { Agent, AgentCreateDTO, AgentUpdateDTO, AgentListParams, AgentPolicyBinding } from '@/types/agent'
import type { Policy } from '@/types/policy'
import type { PageResult } from '@/types/api'

export function getAgentList(params: AgentListParams): Promise<PageResult<Agent>> {
  return request.get('/agents', { params })
}

export function getAgentById(id: string): Promise<Agent> {
  return request.get(`/agents/${id}`)
}

export function createAgent(data: AgentCreateDTO): Promise<Agent> {
  return request.post('/agents', data)
}

export function updateAgent(id: string, data: AgentUpdateDTO): Promise<Agent> {
  return request.put(`/agents/${id}`, data)
}

export function deleteAgent(id: string): Promise<void> {
  return request.delete(`/agents/${id}`)
}

// ==================== 策略绑定管理 ====================

/**
 * 绑定策略到Agent
 */
export function bindPolicy(agentId: string, policyId: string): Promise<AgentPolicyBinding> {
  return request.post(`/agents/${agentId}/policies/${policyId}`)
}

/**
 * 解绑Agent的策略
 */
export function unbindPolicy(agentId: string, policyId: string): Promise<void> {
  return request.delete(`/agents/${agentId}/policies/${policyId}`)
}

/**
 * 获取Agent绑定的策略列表
 */
export function getAgentPolicies(agentId: string): Promise<Policy[]> {
  return request.get(`/agents/${agentId}/policies`)
}

/**
 * 获取Agent的策略绑定记录
 */
export function getAgentPolicyBindings(agentId: string): Promise<AgentPolicyBinding[]> {
  return request.get(`/agents/${agentId}/policy-bindings`)
}

/**
 * 测试LLM连接配置
 */
export function testLlmConnection(data: {
  agentId?: string
  llmProvider?: string
  llmApiKey?: string
  llmBaseUrl?: string
  llmModel?: string
}): Promise<{ success: boolean; message: string; actualModel?: string }> {
  return request.post('/agents/test-connection', data)
}
