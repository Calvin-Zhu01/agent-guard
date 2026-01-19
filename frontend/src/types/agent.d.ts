export interface Agent {
  id: string
  tenantId: string
  name: string
  type: AgentType
  department: string
  environment: Environment
  apiKey: string
  description: string
  status: number
  lastActiveAt: string
  createdAt: string
  updatedAt: string
}

export type AgentType = 'CUSTOMER_SERVICE' | 'FINANCE' | 'OPERATION' | 'INTERNAL' | 'OTHER'

export type Environment = 'TEST' | 'PRODUCTION'

export interface AgentCreateDTO {
  name: string
  type: AgentType
  department?: string
  environment?: Environment
  description?: string
}

export interface AgentUpdateDTO {
  name?: string
  type?: AgentType
  department?: string
  environment?: Environment
  description?: string
  status?: number
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
