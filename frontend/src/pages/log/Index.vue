<script setup lang="ts">
/**
 * Agent 调用日志列表页面
 *
 * @author zhuhx
 */
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as logApi from '@/api/log'
import * as agentApi from '@/api/agent'
import type { AgentLog, AgentLogListParams, ResponseStatus } from '@/types/log'
import type { Agent } from '@/types/agent'

const loading = ref(false)
const logs = ref<AgentLog[]>([])
const total = ref(0)
const agents = ref<Agent[]>([])

const queryParams = ref<AgentLogListParams>({
  current: 1,
  size: 10,
  agentId: undefined,
  responseStatus: undefined
})

const responseStatusOptions = [
  { label: '全部', value: '' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已拦截', value: 'BLOCKED' }
]

const requestTypeLabels: Record<string, string> = {
  API_CALL: 'API调用',
  LLM_CALL: 'LLM调用'
}

async function fetchData() {
  loading.value = true
  try {
    const params: AgentLogListParams = {
      current: queryParams.value.current,
      size: queryParams.value.size
    }
    if (queryParams.value.agentId) {
      params.agentId = queryParams.value.agentId
    }
    if (queryParams.value.responseStatus) {
      params.responseStatus = queryParams.value.responseStatus
    }
    const res = await logApi.getLogList(params)
    logs.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function fetchAgents() {
  try {
    const res = await agentApi.getAgentList({ current: 1, size: 100 })
    agents.value = res.records
  } catch (e) {
    // ignore
  }
}

function handleSearch() {
  queryParams.value.current = 1
  fetchData()
}

function handleRefresh() {
  fetchData()
}

function handlePageChange(page: number) {
  queryParams.value.current = page
  fetchData()
}

function getStatusType(status: ResponseStatus): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'warning'
    case 'BLOCKED':
      return 'danger'
    default:
      return 'info'
  }
}

function getStatusLabel(status: ResponseStatus): string {
  switch (status) {
    case 'SUCCESS':
      return '成功'
    case 'FAILED':
      return '失败'
    case 'BLOCKED':
      return '已拦截'
    default:
      return status
  }
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  fetchData()
  fetchAgents()
})
</script>

<template>
  <div class="log-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>调用日志</span>
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="Agent">
          <el-select
            v-model="queryParams.agentId"
            placeholder="全部Agent"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="agent.name"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryParams.responseStatus"
            placeholder="全部状态"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="opt in responseStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="agentName" label="Agent" width="150" />
        <el-table-column prop="requestType" label="请求类型" width="100">
          <template #default="{ row }">
            {{ requestTypeLabels[row.requestType] || row.requestType }}
          </template>
        </el-table-column>
        <el-table-column prop="endpoint" label="端点" min-width="200" show-overflow-tooltip />
        <el-table-column prop="method" label="方法" width="80" />
        <el-table-column prop="responseStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.responseStatus)" effect="dark">
              {{ getStatusLabel(row.responseStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseTimeMs" label="响应时间" width="100">
          <template #default="{ row }">
            {{ row.responseTimeMs ? `${row.responseTimeMs}ms` : '-' }}
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.current"
        :page-size="queryParams.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
