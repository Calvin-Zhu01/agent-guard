<script setup lang="ts">
/**
 * Agent 调用日志列表页面
 *
 * @author zhuhx
 */
import { ref, onMounted, computed, watch } from 'vue'
import { Refresh, View } from '@element-plus/icons-vue'
import * as logApi from '@/api/log'
import * as agentApi from '@/api/agent'
import type { AgentLog, AgentLogListParams, ResponseStatus, PolicyAction, PolicyType, RequestType } from '@/types/log'
import type { Agent } from '@/types/agent'

const loading = ref(false)
const logs = ref<AgentLog[]>([])
const total = ref(0)
const agents = ref<Agent[]>([])
const activeTab = ref<RequestType>('LLM_CALL') // 默认显示LLM调用

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
  { label: '已拦截', value: 'BLOCKED' },
  { label: '待审批', value: 'PENDING_APPROVAL' }
]

const policyActionLabels: Record<PolicyAction, string> = {
  ALLOW: '允许',
  DENY: '拒绝',
  APPROVAL: '审批',
  RATE_LIMIT: '限流'
}

const policyTypeLabels: Record<PolicyType, string> = {
  ACCESS_CONTROL: '访问控制',
  RATE_LIMIT: '频率限制',
  APPROVAL: '人工审批'
}

// 详情对话框
const detailVisible = ref(false)
const currentLog = ref<AgentLog | null>(null)

const formattedRequestHeaders = computed(() => {
  if (!currentLog.value?.requestHeaders) return ''
  try {
    return JSON.stringify(JSON.parse(currentLog.value.requestHeaders), null, 2)
  } catch {
    return currentLog.value.requestHeaders
  }
})

const formattedRequestBody = computed(() => {
  if (!currentLog.value?.requestBody) return ''
  try {
    return JSON.stringify(JSON.parse(currentLog.value.requestBody), null, 2)
  } catch {
    return currentLog.value.requestBody
  }
})

const formattedResponseBody = computed(() => {
  if (!currentLog.value?.responseBody) return ''
  try {
    return JSON.stringify(JSON.parse(currentLog.value.responseBody), null, 2)
  } catch {
    return currentLog.value.responseBody
  }
})

/**
 * 格式化策略条件为易读字符串
 */
function formatPolicyConditions(conditionsStr: string | undefined): string {
  if (!conditionsStr) return '-'
  try {
    const conditions = JSON.parse(conditionsStr)
    const parts: string[] = []

    // HTTP 方法
    if (conditions.method) {
      parts.push(conditions.method)
    }

    // URL 模式
    if (conditions.urlPattern) {
      parts.push(conditions.urlPattern)
    }

    // 请求体条件
    if (conditions.bodyConditions?.length) {
      const bodyParts = conditions.bodyConditions.map((c: { field: string; operator: string; value: unknown }) => {
        const opSymbol: Record<string, string> = {
          eq: '=', ne: '!=', gt: '>', gte: '>=', lt: '<', lte: '<=',
          contains: '包含', startsWith: '开头', endsWith: '结尾'
        }
        return `${c.field} ${opSymbol[c.operator] || c.operator} ${c.value}`
      })
      parts.push(`[${bodyParts.join(', ')}]`)
    }

    // 频率限制配置
    if (conditions.maxRequests && conditions.windowSeconds) {
      parts.push(`${conditions.maxRequests}次/${conditions.windowSeconds}秒`)
    }

    return parts.length > 0 ? parts.join(' | ') : JSON.stringify(conditions)
  } catch {
    return conditionsStr
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params: AgentLogListParams = {
      current: queryParams.value.current,
      size: queryParams.value.size,
      requestType: activeTab.value
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

function handleViewDetail(row: AgentLog) {
  currentLog.value = row
  detailVisible.value = true
}

function getStatusType(status: ResponseStatus): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'warning'
    case 'BLOCKED':
      return 'danger'
    case 'PENDING_APPROVAL':
      return 'info'
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
    case 'PENDING_APPROVAL':
      return '待审批'
    default:
      return status
  }
}

function getPolicyActionType(action: PolicyAction): 'success' | 'warning' | 'danger' | 'info' {
  switch (action) {
    case 'ALLOW':
      return 'success'
    case 'DENY':
      return 'danger'
    case 'APPROVAL':
      return 'warning'
    case 'RATE_LIMIT':
      return 'info'
    default:
      return 'info'
  }
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}

/**
 * 获取完成原因的显示文本
 */
function getFinishReasonLabel(finishReason: string | null | undefined): string {
  if (!finishReason) return '-'

  const labels: Record<string, string> = {
    'stop': '正常完成',
    'length': '达到长度限制',
    'tool_calls': '调用工具',
    'function_call': '调用函数',
    'content_filter': '内容过滤'
  }

  return labels[finishReason] || finishReason
}

/**
 * 获取完成原因的标签类型
 */
function getFinishReasonType(finishReason: string | null | undefined): 'success' | 'warning' | 'danger' | 'info' {
  if (!finishReason) return 'info'

  switch (finishReason) {
    case 'stop':
      return 'success'  // 正常完成 - 绿色
    case 'length':
      return 'warning'  // 达到长度限制 - 黄色
    case 'tool_calls':
    case 'function_call':
      return 'info'     // 调用工具/函数 - 蓝色
    case 'content_filter':
      return 'danger'   // 内容过滤 - 红色
    default:
      return 'info'
  }
}

/**
 * 获取完成原因的详细说明（用于tooltip）
 */
function getFinishReasonTooltip(finishReason: string | null | undefined): string {
  if (!finishReason) return ''

  const tooltips: Record<string, string> = {
    'stop': '正常完成响应',
    'length': '响应达到了 max_tokens 参数设置的限制而被截断',
    'tool_calls': '模型决定需要调用外部工具（function calling）来完成任务',
    'function_call': '模型决定需要调用外部工具（function calling）来完成任务',
    'content_filter': '响应因违反内容政策而被安全过滤器拦截'
  }

  return tooltips[finishReason] || `未知的完成原因: ${finishReason}`
}

// 根据响应时间获取颜色类型（总用时）
function getResponseTimeType(timeMs: number): 'success' | 'danger' {
  if (timeMs < 60000) return 'success'  // < 60s 绿色
  return 'danger'  // >= 60s 红色
}

// 根据首token时间获取颜色类型
function getFirstTokenTimeType(timeMs: number): 'success' | 'warning' | 'danger' {
  if (timeMs < 3000) return 'success'  // < 3s 绿色
  if (timeMs < 10000) return 'warning'  // 3-10s 黄色
  return 'danger'  // > 10s 红色
}

// 格式化成本显示
function formatCost(cost: number): string {
  if (!cost) return '-'
  return `¥${cost.toFixed(6)}`
}

onMounted(() => {
  fetchData()
  fetchAgents()
})

// 监听Tab切换，重新获取数据
watch(activeTab, () => {
  queryParams.value.current = 1 // 切换Tab时重置到第一页
  fetchData()
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

      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" class="log-tabs">
        <el-tab-pane label="LLM调用" name="LLM_CALL">
          <el-table :data="logs" v-loading="loading" stripe>
            <el-table-column prop="createdAt" label="时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="agentName" label="Agent" width="150" />
            <el-table-column prop="model" label="模型" min-width="200" show-overflow-tooltip />
            <el-table-column label="用时/首字" width="140">
              <template #default="{ row }">
                <div class="time-cell">
                  <el-tag :type="getResponseTimeType(row.responseTimeMs)" effect="light" size="small">
                    {{ row.responseTimeMs ? `${(row.responseTimeMs / 1000).toFixed(1)}s` : '-' }}
                  </el-tag>
                  <el-tag v-if="row.firstTokenTimeMs" :type="getFirstTokenTimeType(row.firstTokenTimeMs)" effect="light" size="small">
                    {{ (row.firstTokenTimeMs / 1000).toFixed(1) }}s
                  </el-tag>
                  <el-tag v-if="!row.firstTokenTimeMs" type="info" effect="light" size="small">
                    非流
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="tokenInput" label="输入" width="80" align="right" />
            <el-table-column prop="tokenOutput" label="输出" width="80" align="right" />
            <el-table-column label="花费" width="110" align="right">
              <template #default="{ row }">
                {{ formatCost(row.cost) }}
              </template>
            </el-table-column>
            <el-table-column prop="responseStatus" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.responseStatus)" effect="dark" size="small">
                  {{ getStatusLabel(row.responseStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="完成原因" width="120">
              <template #default="{ row }">
                <el-tooltip v-if="row.finishReason" :content="getFinishReasonTooltip(row.finishReason)" placement="top">
                  <el-tag :type="getFinishReasonType(row.finishReason)" effect="plain" size="small">
                    {{ getFinishReasonLabel(row.finishReason) }}
                  </el-tag>
                </el-tooltip>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="primary" link :icon="View" @click="handleViewDetail(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="API调用" name="API_CALL">
          <el-table :data="logs" v-loading="loading" stripe>
            <el-table-column prop="createdAt" label="时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="agentName" label="Agent" width="150" />
            <el-table-column prop="endpoint" label="端点" min-width="200" show-overflow-tooltip />
            <el-table-column prop="method" label="方法" width="80" />
            <el-table-column prop="responseStatus" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.responseStatus)" effect="dark">
                  {{ getStatusLabel(row.responseStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="触发策略" width="150">
              <template #default="{ row }">
                <template v-if="row.policySnapshot">
                  <el-tooltip :content="row.policySnapshot.reason" placement="top" :disabled="!row.policySnapshot.reason">
                    <el-tag :type="getPolicyActionType(row.policySnapshot.action)" effect="plain" size="small">
                      {{ row.policySnapshot.name || row.policySnapshot.id }}
                    </el-tag>
                  </el-tooltip>
                </template>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="responseTimeMs" label="响应时间" width="100">
              <template #default="{ row }">
                {{ row.responseTimeMs ? `${row.responseTimeMs}ms` : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link :icon="View" @click="handleViewDetail(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <el-pagination
        v-model:current-page="queryParams.current"
        :page-size="queryParams.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="请求详情" width="80%" top="5vh">
      <div class="detail-container" v-if="currentLog">
        <div class="detail-header">
          <el-descriptions :column="4" border size="small">
            <el-descriptions-item label="时间">{{ formatDateTime(currentLog.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="Agent">{{ currentLog.agentName }}</el-descriptions-item>
            <el-descriptions-item label="方法">{{ currentLog.method }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(currentLog.responseStatus)" effect="dark" size="small">
                {{ getStatusLabel(currentLog.responseStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="端点" :span="3">{{ currentLog.endpoint }}</el-descriptions-item>
            <el-descriptions-item label="响应时间">{{ currentLog.responseTimeMs ? `${currentLog.responseTimeMs}ms` : '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="currentLog.requestType === 'LLM_CALL'" label="模型" :span="2">{{ currentLog.model || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="currentLog.requestType === 'LLM_CALL'" label="首token时间">
              {{ currentLog.firstTokenTimeMs ? `${currentLog.firstTokenTimeMs}ms` : '-' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="currentLog.requestType === 'LLM_CALL'" label="输入Token">{{ currentLog.tokenInput || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="currentLog.requestType === 'LLM_CALL'" label="输出Token">{{ currentLog.tokenOutput || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="currentLog.requestType === 'LLM_CALL'" label="成本">{{ formatCost(currentLog.cost) }}</el-descriptions-item>
            <el-descriptions-item v-if="currentLog.requestType === 'LLM_CALL'" label="完成原因">
              <el-tooltip v-if="currentLog.finishReason" :content="getFinishReasonTooltip(currentLog.finishReason)" placement="top">
                <el-tag :type="getFinishReasonType(currentLog.finishReason)" effect="plain" size="small">
                  {{ getFinishReasonLabel(currentLog.finishReason) }}
                </el-tag>
              </el-tooltip>
              <span v-else class="text-muted">-</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 策略信息面板 -->
        <div class="policy-info" v-if="currentLog.policySnapshot">
          <el-alert type="warning" :closable="false" show-icon>
            <template #title>
              <span class="policy-title">
                触发策略：{{ currentLog.policySnapshot.name || currentLog.policySnapshot.id }}
                <el-tag
                  :type="getPolicyActionType(currentLog.policySnapshot.action)"
                  effect="dark"
                  size="small"
                  style="margin-left: 8px"
                >
                  {{ policyActionLabels[currentLog.policySnapshot.action] || currentLog.policySnapshot.action }}
                </el-tag>
                <el-tag
                  v-if="currentLog.policySnapshot.type"
                  type="info"
                  effect="plain"
                  size="small"
                  style="margin-left: 8px"
                >
                  {{ policyTypeLabels[currentLog.policySnapshot.type] || currentLog.policySnapshot.type }}
                </el-tag>
              </span>
            </template>
            <template #default>
              <div class="policy-conditions" v-if="currentLog.policySnapshot.conditions">
                <span class="conditions-label">匹配条件：</span>
                <code class="conditions-value">{{ formatPolicyConditions(currentLog.policySnapshot.conditions) }}</code>
              </div>
              <div class="policy-reason" v-if="currentLog.policySnapshot.reason">
                {{ currentLog.policySnapshot.reason }}
              </div>
            </template>
          </el-alert>
        </div>

        <div class="detail-body">
          <!-- 左侧：请求信息 -->
          <div class="detail-column">
            <div class="detail-panel request-headers">
              <div class="panel-title">请求头 (Request Headers)</div>
              <div class="panel-content">
                <pre v-if="formattedRequestHeaders">{{ formattedRequestHeaders }}</pre>
                <el-empty v-else description="无请求头数据" :image-size="60" />
              </div>
            </div>
            <div class="detail-panel request-body">
              <div class="panel-title">请求体 (Request Body)</div>
              <div class="panel-content">
                <pre v-if="formattedRequestBody">{{ formattedRequestBody }}</pre>
                <el-empty v-else description="无请求体数据" :image-size="60" />
              </div>
            </div>
          </div>
          <!-- 右侧：响应信息 -->
          <div class="detail-column">
            <div class="detail-panel response-body">
              <div class="panel-title">响应体 (Response Body)</div>
              <div class="panel-content">
                <pre v-if="formattedResponseBody">{{ formattedResponseBody }}</pre>
                <el-empty v-else description="无响应体数据" :image-size="60" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.text-muted {
  color: var(--el-text-color-placeholder);
}

.log-tabs {
  margin-top: 16px;
}

.time-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header {
  margin-bottom: 8px;
}

.policy-info {
  margin-bottom: 8px;
}

.policy-title {
  display: flex;
  align-items: center;
  font-weight: 600;
}

.policy-conditions {
  margin-top: 4px;
  font-size: 13px;
}

.conditions-label {
  color: var(--el-text-color-secondary);
}

.conditions-value {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  background-color: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--el-color-primary);
}

.policy-reason {
  margin-top: 4px;
  color: var(--el-text-color-regular);
}

.detail-body {
  display: flex;
  gap: 16px;
  height: 55vh;
}

.detail-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  overflow: hidden;
}

.detail-panel.request-headers {
  flex: 0 0 auto;
  max-height: 30%;
}

.detail-panel.request-body {
  flex: 1;
  min-height: 0;
}

.detail-panel.response-body {
  flex: 1;
}

.panel-title {
  padding: 12px 16px;
  font-weight: 600;
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-light);
}

.panel-content {
  flex: 1;
  padding: 16px;
  overflow: auto;
  background-color: var(--el-fill-color-blank);
}

.panel-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
}
</style>
