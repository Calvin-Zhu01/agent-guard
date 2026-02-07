<script setup lang="ts">
/**
 * Agent 调用日志列表页面
 *
 * @author zhuhx
 */
import { ref, onMounted, computed, watch } from 'vue'
import { Refresh, View } from '@element-plus/icons-vue'
import CodeBlock from '@/components/CodeBlock.vue'
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
  size: 20,
  agentId: undefined,
  responseStatus: undefined
})

const responseStatusOptions = [
  { label: '全部', value: '' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已拦截', value: 'BLOCKED' },
  { label: '待审批', value: 'PENDING_APPROVAL' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已过期', value: 'EXPIRED' }
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

// 解析请求摘要
const parsedRequestSummary = computed(() => {
  if (!currentLog.value?.requestSummary) return null
  try {
    return JSON.parse(currentLog.value.requestSummary)
  } catch {
    return null
  }
})

// 格式化请求摘要为 JSON 字符串
const formattedRequestSummary = computed(() => {
  if (!parsedRequestSummary.value) return ''
  return JSON.stringify(parsedRequestSummary.value, null, 2)
})

// 计算 Token 总数
const totalTokens = computed(() => {
  if (!currentLog.value) return 0
  return (currentLog.value.tokenInput || 0) + (currentLog.value.tokenOutput || 0)
})

// 计算平均生成速度（tokens/秒）
const tokensPerSecond = computed(() => {
  if (!currentLog.value || !currentLog.value.tokenOutput || !currentLog.value.responseTimeMs) return 0
  return (currentLog.value.tokenOutput / (currentLog.value.responseTimeMs / 1000)).toFixed(1)
})

// API 调用的格式化方法（保留用于 API_CALL）
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

/**
 * 每页条数变化
 */
function handleSizeChange(size: number) {
  queryParams.value.size = size
  queryParams.value.current = 1
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
      return 'danger'
    case 'BLOCKED':
      return 'danger'
    case 'REJECTED':
      return 'warning'
    case 'PENDING_APPROVAL':
      return 'info'
    case 'EXPIRED':
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
    case 'REJECTED':
      return '已拒绝'
    case 'PENDING_APPROVAL':
      return '待审批'
    case 'EXPIRED':
      return '已过期'
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
            <el-table-column prop="model" label="模型" min-width="100" show-overflow-tooltip />
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
            <el-table-column label="完成原因" width="140">
              <template #default="{ row }">
                <!-- 当有工具调用时，显示工具名称的tooltip -->
                <el-tooltip
                  v-if="row.finishReason && (row.finishReason === 'tool_calls' || row.finishReason === 'function_call') && row.toolCalls"
                  :content="`调用工具: ${row.toolCalls}`"
                  placement="top"
                >
                  <el-tag :type="getFinishReasonType(row.finishReason)" effect="plain" size="small">
                    {{ getFinishReasonLabel(row.finishReason) }}
                    <span class="tool-count">({{ row.toolCalls.split(',').length }})</span>
                  </el-tag>
                </el-tooltip>
                <!-- 其他完成原因，显示原有的tooltip -->
                <el-tooltip v-else-if="row.finishReason" :content="getFinishReasonTooltip(row.finishReason)" placement="top">
                  <el-tag :type="getFinishReasonType(row.finishReason)" effect="plain" size="small">
                    {{ getFinishReasonLabel(row.finishReason) }}
                  </el-tag>
                </el-tooltip>
                <span v-else class="text-muted">-</span>
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
        v-model:page-size="queryParams.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="currentLog?.requestType === 'LLM_CALL' ? 'LLM 调用详情' : 'API 调用详情'"
      width="80%"
      top="5vh"
    >
      <div class="detail-container" v-if="currentLog">
        <!-- LLM 调用详情 -->
        <template v-if="currentLog.requestType === 'LLM_CALL'">
          <!-- 基本信息 -->
          <div class="detail-header">
            <el-descriptions :column="4" border size="small">
              <el-descriptions-item label="时间">{{ formatDateTime(currentLog.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="Agent">{{ currentLog.agentName }}</el-descriptions-item>
              <el-descriptions-item label="模型">{{ currentLog.model || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(currentLog.responseStatus)" effect="dark" size="small">
                  {{ getStatusLabel(currentLog.responseStatus) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="端点" :span="2">{{ currentLog.endpoint }}</el-descriptions-item>
              <el-descriptions-item label="传输方式">
                <el-tag :type="currentLog.firstTokenTimeMs ? 'success' : 'info'" effect="plain" size="small">
                  {{ currentLog.firstTokenTimeMs ? '流式' : '非流式' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="完成原因">
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

          <!-- 卡片式布局 -->
          <div class="llm-cards">
            <!-- Token 使用卡片 -->
            <el-card class="metric-card" shadow="hover">
              <template #header>
                <div class="card-header-title">Token 使用</div>
              </template>
              <div class="metric-content">
                <div class="metric-item">
                  <div class="metric-label">输入 Token</div>
                  <div class="metric-value primary">{{ currentLog.tokenInput || 0 }}</div>
                </div>
                <div class="metric-item">
                  <div class="metric-label">输出 Token</div>
                  <div class="metric-value success">{{ currentLog.tokenOutput || 0 }}</div>
                </div>
                <div class="metric-item">
                  <div class="metric-label">总计</div>
                  <div class="metric-value">{{ totalTokens }}</div>
                </div>
              </div>
            </el-card>

            <!-- 成本分析卡片 -->
            <el-card class="metric-card" shadow="hover">
              <template #header>
                <div class="card-header-title">成本分析</div>
              </template>
              <div class="metric-content">
                <div class="metric-item">
                  <div class="metric-label">总成本</div>
                  <div class="metric-value warning">{{ formatCost(currentLog.cost) }}</div>
                </div>
                <div class="metric-item">
                  <div class="metric-label">模型</div>
                  <div class="metric-value-text">{{ currentLog.model || '-' }}</div>
                </div>
              </div>
            </el-card>

            <!-- 性能指标卡片 -->
            <el-card class="metric-card" shadow="hover">
              <template #header>
                <div class="card-header-title">性能指标</div>
              </template>
              <div class="metric-content">
                <div class="metric-item">
                  <div class="metric-label">总响应时间</div>
                  <div class="metric-value">
                    {{ currentLog.responseTimeMs ? `${(currentLog.responseTimeMs / 1000).toFixed(2)}s` : '-' }}
                  </div>
                </div>
                <div class="metric-item">
                  <div class="metric-label">首 Token 时间</div>
                  <div class="metric-value" :class="currentLog.firstTokenTimeMs ? 'info' : ''">
                    {{ currentLog.firstTokenTimeMs ? `${(currentLog.firstTokenTimeMs / 1000).toFixed(2)}s` : '非流式' }}
                  </div>
                </div>
                <div class="metric-item" v-if="currentLog.firstTokenTimeMs">
                  <div class="metric-label">生成速度</div>
                  <div class="metric-value success">{{ tokensPerSecond }} tokens/s</div>
                </div>
              </div>
            </el-card>
          </div>

          <!-- 请求摘要 -->
          <div class="detail-panel request-summary" v-if="formattedRequestSummary">
            <div class="panel-title">请求摘要 (Request Summary)</div>
            <div class="panel-content">
              <CodeBlock :code="formattedRequestSummary" language="json" />
            </div>
          </div>
        </template>

        <!-- API 调用详情（保持原有布局） -->
        <template v-else>
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
                  <CodeBlock v-if="formattedRequestHeaders" :code="formattedRequestHeaders" language="json" />
                  <el-empty v-else description="无请求头数据" :image-size="60" />
                </div>
              </div>
              <div class="detail-panel request-body">
                <div class="panel-title">请求体 (Request Body)</div>
                <div class="panel-content">
                  <CodeBlock v-if="formattedRequestBody" :code="formattedRequestBody" language="json" />
                  <el-empty v-else description="无请求体数据" :image-size="60" />
                </div>
              </div>
            </div>
            <!-- 右侧：响应信息 -->
            <div class="detail-column">
              <div class="detail-panel response-body">
                <div class="panel-title">响应体 (Response Body)</div>
                <div class="panel-content">
                  <CodeBlock v-if="formattedResponseBody" :code="formattedResponseBody" language="json" />
                  <el-empty v-else description="无响应体数据" :image-size="60" />
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
:deep(.el-card) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-card__header) {
  border-radius: 12px 12px 0 0;
}

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

.tool-count {
  margin-left: 4px;
  font-size: 11px;
  opacity: 0.8;
  font-weight: 600;
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

/* LLM 调用卡片布局 */
.llm-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.metric-card {
  border-radius: 8px;
}

.card-header-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.metric-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.metric-value.primary {
  color: var(--el-color-primary);
}

.metric-value.success {
  color: var(--el-color-success);
}

.metric-value.warning {
  color: var(--el-color-warning);
}

.metric-value.info {
  color: var(--el-color-info);
}

.metric-value-text {
  font-size: 14px;
  color: var(--el-text-color-regular);
  word-break: break-all;
}

/* API 调用详情布局（原有样式） */
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
  border: 1px solid #dcdfe6;
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

.detail-panel.request-summary {
  max-height: 300px;
}

.panel-title {
  padding: 12px 16px;
  font-weight: 600;
  background-color: var(--el-fill-color-light);
}

.panel-content {
  flex: 1;
  overflow: auto;
  background-color: var(--el-fill-color-blank);
}

/* 代码块在panel中时移除边框，与标题融为一体 */
.panel-content :deep(.code-block) {
  border: none;
  border-radius: 0;
}

/* el-empty保持padding */
.panel-content :deep(.el-empty) {
  padding: 16px;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .llm-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .llm-cards {
    grid-template-columns: 1fr;
  }
}
</style>
