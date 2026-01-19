/**
 * Agent 状态管理
 *
 * @author zhuhx
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Agent } from '@/types/agent'
import * as agentApi from '@/api/agent'

export const useAgentStore = defineStore('agent', () => {
  const agents = ref<Agent[]>([])
  const currentAgent = ref<Agent | null>(null)
  const loading = ref(false)
  const total = ref(0)

  async function fetchAgents(params: { current: number; size: number; keyword?: string }) {
    loading.value = true
    try {
      const res = await agentApi.getAgentList(params)
      agents.value = res.records
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  async function fetchAgentById(id: string) {
    loading.value = true
    try {
      currentAgent.value = await agentApi.getAgentById(id)
    } finally {
      loading.value = false
    }
  }

  return {
    agents,
    currentAgent,
    loading,
    total,
    fetchAgents,
    fetchAgentById
  }
})
