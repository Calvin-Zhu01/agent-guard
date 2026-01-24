export interface Agent {
  id: string
  name: string
  apiKey: string
  description?: string
  llmProvider?: string
  llmApiKey?: string
  llmBaseUrl?: string
  llmModel?: string
  lastActiveAt?: string
  createdAt: string
  updatedAt: string
  policies?: PolicySummary[]
}

export interface PolicySummary {
  id: string
  name: string
  enabled: boolean
}

export interface AgentCreateDTO {
  name: string
  description?: string
  llmProvider?: string
  llmApiKey?: string
  llmBaseUrl?: string
  llmModel?: string
}

export interface AgentUpdateDTO {
  name?: string
  description?: string
  llmProvider?: string
  llmApiKey?: string
  llmBaseUrl?: string
  llmModel?: string
}

export interface AgentListParams {
  current: number
  size: number
  keyword?: string
}

/** Agent策略绑定信息 */
export interface AgentPolicyBinding {
  id: string
  agentId: string
  policyId: string
  agentName: string
  policyName: string
  createdAt: string
}
