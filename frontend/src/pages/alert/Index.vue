<script setup lang="ts">
/**
 * 告警管理页面
 *
 * @author zhuhx
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as alertApi from '@/api/alert'
import type {
  AlertRule,
  AlertRuleCreateDTO,
  AlertRuleUpdateDTO,
  AlertType,
  NotificationChannelType,
  AlertHistory,
  AlertStatus
} from '@/types/alert'

// Tab 状态
const activeTab = ref('rules')

// ==================== 告警规则列表 ====================
const rulesLoading = ref(false)
const rules = ref<AlertRule[]>([])
const rulesTotal = ref(0)
const rulesQueryParams = ref({
  current: 1,
  size: 10,
  keyword: '',
  type: undefined as AlertType | undefined,
  enabled: undefined as boolean | undefined
})

// 对话框状态
const dialogVisible = ref(false)
const isEditMode = ref(false)
const editingRuleId = ref<string | null>(null)
const formLoading = ref(false)

// 表单数据
const formData = ref({
  name: '',
  type: 'COST' as AlertType,
  threshold: 0.8,
  channelType: 'EMAIL' as NotificationChannelType,
  channelConfig: ''
})

// Tag 颜色类型
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

// 告警类型映射
const alertTypeMap: Record<AlertType, { label: string; color: TagType; desc: string }> = {
  COST: { label: '成本告警', color: 'warning', desc: '当成本达到预算阈值时触发' },
  ERROR_RATE: { label: '异常告警', color: 'danger', desc: '当错误率超过阈值时触发' },
  APPROVAL: { label: '审批提醒', color: 'primary', desc: '审批请求创建或即将过期时触发' },
  SYSTEM: { label: '系统告警', color: 'info', desc: '系统组件异常时触发' }
}

// 通知渠道映射
const channelTypeMap: Record<NotificationChannelType, { label: string; color: TagType }> = {
  EMAIL: { label: '邮件', color: 'primary' },
  WEBHOOK: { label: 'Webhook', color: 'success' }
}

// 告警类型选项
const alertTypeOptions: { label: string; value: AlertType; desc: string }[] = [
  { label: '成本告警', value: 'COST', desc: '当成本达到预算阈值时触发' },
  { label: '异常告警', value: 'ERROR_RATE', desc: '当错误率超过阈值时触发' },
  { label: '审批提醒', value: 'APPROVAL', desc: '审批请求创建或即将过期时触发' },
  { label: '系统告警', value: 'SYSTEM', desc: '系统组件异常时触发' }
]

// 通知渠道选项
const channelTypeOptions: { label: string; value: NotificationChannelType }[] = [
  { label: '邮件', value: 'EMAIL' },
  { label: 'Webhook', value: 'WEBHOOK' }
]

// 阈值提示
const thresholdHint = computed(() => {
  switch (formData.value.type) {
    case 'COST':
      return '成本占预算的百分比（0-1），例如 0.8 表示 80%'
    case 'ERROR_RATE':
      return '错误率阈值（0-1），例如 0.1 表示 10%'
    case 'APPROVAL':
      return '提前提醒时间（分钟），例如 30 表示提前 30 分钟提醒'
    case 'SYSTEM':
      return '系统告警无需配置阈值'
    default:
      return ''
  }
})

// 渠道配置提示
const channelConfigHint = computed(() => {
  if (formData.value.channelType === 'EMAIL') {
    return '请输入邮箱地址，例如：admin@example.com'
  } else {
    return '请输入 Webhook URL，例如：https://hooks.example.com/alert'
  }
})

/**
 * 获取告警类型显示信息
 */
function getAlertTypeInfo(type: AlertType) {
  return alertTypeMap[type] || { label: type, color: 'default', desc: '' }
}

/**
 * 获取通知渠道显示信息
 */
function getChannelTypeInfo(type: NotificationChannelType) {
  return channelTypeMap[type] || { label: type, color: 'default' }
}

/**
 * 格式化渠道配置显示
 */
function formatChannelConfig(config: string): string {
  if (!config) return '-'
  try {
    const parsed = JSON.parse(config)
    if (parsed.email) return parsed.email
    if (parsed.url) return parsed.url
    return config
  } catch {
    return config
  }
}

/**
 * 格式化阈值显示
 */
function formatThreshold(type: AlertType, threshold: number): string {
  if (threshold === null || threshold === undefined) return '-'
  switch (type) {
    case 'COST':
    case 'ERROR_RATE':
      return `${(threshold * 100).toFixed(0)}%`
    case 'APPROVAL':
      return `${threshold} 分钟`
    default:
      return String(threshold)
  }
}

/**
 * 获取告警规则列表
 */
async function fetchRules() {
  rulesLoading.value = true
  try {
    const res = await alertApi.getAlertRuleList(rulesQueryParams.value)
    rules.value = res.records
    rulesTotal.value = res.total
  } finally {
    rulesLoading.value = false
  }
}

/**
 * 重置表单
 */
function resetForm() {
  formData.value = {
    name: '',
    type: 'COST',
    threshold: 0.8,
    channelType: 'EMAIL',
    channelConfig: ''
  }
  isEditMode.value = false
  editingRuleId.value = null
}

/**
 * 打开新建对话框
 */
function handleOpenCreate() {
  resetForm()
  dialogVisible.value = true
}

/**
 * 打开编辑对话框
 */
function handleOpenEdit(rule: AlertRule) {
  isEditMode.value = true
  editingRuleId.value = rule.id
  formData.value.name = rule.name
  formData.value.type = rule.type
  formData.value.threshold = rule.threshold
  formData.value.channelType = rule.channelType
  
  // 解析渠道配置
  try {
    const config = JSON.parse(rule.channelConfig)
    formData.value.channelConfig = config.email || config.url || ''
  } catch {
    formData.value.channelConfig = rule.channelConfig
  }
  
  dialogVisible.value = true
}

/**
 * 构建渠道配置 JSON
 */
function buildChannelConfig(): string {
  if (formData.value.channelType === 'EMAIL') {
    return JSON.stringify({ email: formData.value.channelConfig })
  } else {
    return JSON.stringify({ url: formData.value.channelConfig })
  }
}

/**
 * 提交表单
 */
async function handleSubmit() {
  if (!formData.value.name?.trim()) {
    ElMessage.warning('请输入规则名称')
    return
  }
  if (!formData.value.channelConfig?.trim()) {
    ElMessage.warning('请输入通知渠道配置')
    return
  }
  
  // 验证邮箱格式
  if (formData.value.channelType === 'EMAIL') {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(formData.value.channelConfig)) {
      ElMessage.warning('请输入有效的邮箱地址')
      return
    }
  }
  
  // 验证 Webhook URL 格式
  if (formData.value.channelType === 'WEBHOOK') {
    try {
      new URL(formData.value.channelConfig)
    } catch {
      ElMessage.warning('请输入有效的 Webhook URL')
      return
    }
  }
  
  formLoading.value = true
  try {
    const submitData: AlertRuleCreateDTO = {
      name: formData.value.name,
      type: formData.value.type,
      threshold: formData.value.threshold,
      channelType: formData.value.channelType,
      channelConfig: buildChannelConfig()
    }
    
    if (isEditMode.value && editingRuleId.value) {
      await alertApi.updateAlertRule(editingRuleId.value, submitData as AlertRuleUpdateDTO)
      ElMessage.success('更新成功')
    } else {
      await alertApi.createAlertRule(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    resetForm()
    fetchRules()
  } finally {
    formLoading.value = false
  }
}

/**
 * 删除告警规则
 */
async function handleDelete(rule: AlertRule) {
  try {
    await ElMessageBox.confirm(
      `确定要删除告警规则「${rule.name}」吗？删除后无法恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
    )
    await alertApi.deleteAlertRule(rule.id)
    ElMessage.success('删除成功')
    fetchRules()
  } catch {
    // user cancelled
  }
}

/**
 * 切换告警规则启用状态
 */
async function handleToggleEnabled(rule: AlertRule) {
  const action = rule.enabled ? '停用' : '启用'
  try {
    if (rule.enabled) {
      await alertApi.disableAlertRule(rule.id)
    } else {
      await alertApi.enableAlertRule(rule.id)
    }
    ElMessage.success(`${action}成功`)
    fetchRules()
  } catch {
    rule.enabled = !rule.enabled
  }
}

/**
 * 搜索告警规则
 */
function handleRulesSearch() {
  rulesQueryParams.value.current = 1
  fetchRules()
}

/**
 * 重置搜索
 */
function handleRulesReset() {
  rulesQueryParams.value.keyword = ''
  rulesQueryParams.value.type = undefined
  rulesQueryParams.value.enabled = undefined
  rulesQueryParams.value.current = 1
  fetchRules()
}

/**
 * 分页变化
 */
function handleRulesPageChange(page: number) {
  rulesQueryParams.value.current = page
  fetchRules()
}

/**
 * 对话框关闭
 */
function handleDialogClose() {
  resetForm()
}

// ==================== 告警历史列表 ====================
const historyLoading = ref(false)
const historyList = ref<AlertHistory[]>([])
const historyTotal = ref(0)
const historyQueryParams = ref({
  current: 1,
  size: 10,
  type: undefined as AlertType | undefined,
  status: undefined as AlertStatus | undefined,
  startTime: '',
  endTime: ''
})
const dateRange = ref<[Date, Date] | null>(null)

// 告警状态映射
const alertStatusMap: Record<AlertStatus, { label: string; color: TagType }> = {
  SUCCESS: { label: '发送成功', color: 'success' },
  FAILED: { label: '发送失败', color: 'danger' }
}

// 告警状态选项
const alertStatusOptions: { label: string; value: AlertStatus }[] = [
  { label: '发送成功', value: 'SUCCESS' },
  { label: '发送失败', value: 'FAILED' }
]

/**
 * 获取告警状态显示信息
 */
function getAlertStatusInfo(status: AlertStatus) {
  return alertStatusMap[status] || { label: status, color: 'default' }
}

/**
 * 获取告警历史列表
 */
async function fetchHistory() {
  historyLoading.value = true
  try {
    // 处理日期范围
    if (dateRange.value && dateRange.value.length === 2) {
      historyQueryParams.value.startTime = formatDateTime(dateRange.value[0])
      historyQueryParams.value.endTime = formatDateTime(dateRange.value[1])
    } else {
      historyQueryParams.value.startTime = ''
      historyQueryParams.value.endTime = ''
    }
    
    const res = await alertApi.getAlertHistoryList(historyQueryParams.value)
    historyList.value = res.records
    historyTotal.value = res.total
  } finally {
    historyLoading.value = false
  }
}

/**
 * 格式化日期时间
 */
function formatDateTime(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 搜索告警历史
 */
function handleHistorySearch() {
  historyQueryParams.value.current = 1
  fetchHistory()
}

/**
 * 重置搜索
 */
function handleHistoryReset() {
  historyQueryParams.value.type = undefined
  historyQueryParams.value.status = undefined
  dateRange.value = null
  historyQueryParams.value.current = 1
  fetchHistory()
}

/**
 * 分页变化
 */
function handleHistoryPageChange(page: number) {
  historyQueryParams.value.current = page
  fetchHistory()
}

/**
 * 导出告警历史
 */
async function handleExport() {
  try {
    const params: { type?: AlertType; status?: AlertStatus; startTime?: string; endTime?: string } = {}
    if (historyQueryParams.value.type) params.type = historyQueryParams.value.type
    if (historyQueryParams.value.status) params.status = historyQueryParams.value.status
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = formatDateTime(dateRange.value[0])
      params.endTime = formatDateTime(dateRange.value[1])
    }
    
    const data = await alertApi.exportAlertHistory(params)
    
    // 转换为 CSV
    const headers = ['ID', '告警类型', '标题', '内容', '接收人', '通知渠道', '状态', '错误信息', '创建时间']
    const rows = data.map(item => [
      item.id,
      getAlertTypeInfo(item.type).label,
      item.title,
      item.content,
      item.recipient,
      getChannelTypeInfo(item.channelType).label,
      getAlertStatusInfo(item.status).label,
      item.errorMessage || '',
      item.createdAt
    ])
    
    const csvContent = [headers, ...rows].map(row => row.map(cell => `"${cell}"`).join(',')).join('\n')
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `告警历史_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

/**
 * Tab 切换
 */
function handleTabChange(tab: string) {
  if (tab === 'history' && historyList.value.length === 0) {
    fetchHistory()
  }
}

onMounted(() => {
  fetchRules()
})
</script>

<template>
  <div class="alert-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>告警管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 告警规则 Tab -->
        <el-tab-pane label="告警规则" name="rules">
          <!-- 搜索栏 -->
          <el-form :inline="true" @submit.prevent="handleRulesSearch" class="search-form">
            <el-form-item>
              <el-input
                v-model="rulesQueryParams.keyword"
                placeholder="搜索规则名称"
                clearable
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item>
              <el-select
                v-model="rulesQueryParams.type"
                placeholder="告警类型"
                clearable
                style="width: 150px"
              >
                <el-option
                  v-for="item in alertTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select
                v-model="rulesQueryParams.enabled"
                placeholder="状态"
                clearable
                style="width: 120px"
              >
                <el-option label="已启用" :value="true" />
                <el-option label="已停用" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleRulesSearch">搜索</el-button>
              <el-button @click="handleRulesReset">重置</el-button>
              <el-button type="primary" @click="handleOpenCreate">新建规则</el-button>
            </el-form-item>
          </el-form>

          <!-- 规则表格 -->
          <el-table :data="rules" v-loading="rulesLoading" stripe>
            <el-table-column prop="name" label="规则名称" min-width="150" />
            <el-table-column prop="type" label="告警类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getAlertTypeInfo(row.type).color" size="small">
                  {{ getAlertTypeInfo(row.type).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="阈值" width="100">
              <template #default="{ row }">
                {{ formatThreshold(row.type, row.threshold) }}
              </template>
            </el-table-column>
            <el-table-column prop="channelType" label="通知渠道" width="100">
              <template #default="{ row }">
                <el-tag :type="getChannelTypeInfo(row.channelType).color" size="small">
                  {{ getChannelTypeInfo(row.channelType).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="渠道配置" min-width="180">
              <template #default="{ row }">
                <span class="channel-config">{{ formatChannelConfig(row.channelConfig) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-switch
                  v-model="row.enabled"
                  @change="handleToggleEnabled(row)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="170" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleOpenEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="rulesQueryParams.current"
            :page-size="rulesQueryParams.size"
            :total="rulesTotal"
            layout="total, prev, pager, next"
            @current-change="handleRulesPageChange"
            style="margin-top: 20px; justify-content: flex-end"
          />
        </el-tab-pane>

        <!-- 告警历史 Tab -->
        <el-tab-pane label="告警历史" name="history">
          <!-- 搜索栏 -->
          <el-form :inline="true" @submit.prevent="handleHistorySearch" class="search-form">
            <el-form-item>
              <el-select
                v-model="historyQueryParams.type"
                placeholder="告警类型"
                clearable
                style="width: 150px"
              >
                <el-option
                  v-for="item in alertTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select
                v-model="historyQueryParams.status"
                placeholder="发送状态"
                clearable
                style="width: 120px"
              >
                <el-option
                  v-for="item in alertStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-date-picker
                v-model="dateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                style="width: 360px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleHistorySearch">搜索</el-button>
              <el-button @click="handleHistoryReset">重置</el-button>
              <el-button @click="handleExport">导出</el-button>
            </el-form-item>
          </el-form>

          <!-- 历史表格 -->
          <el-table :data="historyList" v-loading="historyLoading" stripe>
            <el-table-column prop="title" label="告警标题" min-width="200" />
            <el-table-column prop="type" label="告警类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getAlertTypeInfo(row.type).color" size="small">
                  {{ getAlertTypeInfo(row.type).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="recipient" label="接收人" min-width="180" />
            <el-table-column prop="channelType" label="通知渠道" width="100">
              <template #default="{ row }">
                <el-tag :type="getChannelTypeInfo(row.channelType).color" size="small">
                  {{ getChannelTypeInfo(row.channelType).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="发送状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getAlertStatusInfo(row.status).color" size="small">
                  {{ getAlertStatusInfo(row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="errorMessage" label="错误信息" min-width="150">
              <template #default="{ row }">
                <span v-if="row.errorMessage" class="error-message">{{ row.errorMessage }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="发送时间" width="170" />
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="historyQueryParams.current"
            :page-size="historyQueryParams.size"
            :total="historyTotal"
            layout="total, prev, pager, next"
            @current-change="handleHistoryPageChange"
            style="margin-top: 20px; justify-content: flex-end"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditMode ? '编辑告警规则' : '新建告警规则'"
      width="550px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item label="规则名称" required>
          <el-input v-model="formData.name" placeholder="请输入规则名称" maxlength="100" />
        </el-form-item>
        
        <el-form-item label="告警类型" required>
          <el-select v-model="formData.type" style="width: 100%" :disabled="isEditMode">
            <el-option
              v-for="item in alertTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            >
              <div class="type-option">
                <span>{{ item.label }}</span>
                <span class="type-desc">{{ item.desc }}</span>
              </div>
            </el-option>
          </el-select>
          <div class="field-hint">{{ getAlertTypeInfo(formData.type).desc }}</div>
        </el-form-item>

        <el-form-item v-if="formData.type !== 'SYSTEM'" label="阈值">
          <el-input-number
            v-model="formData.threshold"
            :min="0"
            :max="formData.type === 'APPROVAL' ? 1440 : 1"
            :step="formData.type === 'APPROVAL' ? 1 : 0.05"
            :precision="formData.type === 'APPROVAL' ? 0 : 2"
            style="width: 200px"
          />
          <div class="field-hint">{{ thresholdHint }}</div>
        </el-form-item>

        <el-form-item label="通知渠道" required>
          <el-radio-group v-model="formData.channelType">
            <el-radio value="EMAIL">邮件</el-radio>
            <el-radio value="WEBHOOK">Webhook</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="渠道配置" required>
          <el-input
            v-model="formData.channelConfig"
            :placeholder="formData.channelType === 'EMAIL' ? 'admin@example.com' : 'https://hooks.example.com/alert'"
          />
          <div class="field-hint">{{ channelConfigHint }}</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">
          {{ isEditMode ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.alert-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 16px;
}

.channel-config {
  color: #606266;
  font-size: 13px;
  word-break: break-all;
}

.error-message {
  color: #f56c6c;
  font-size: 13px;
}

.type-option {
  display: flex;
  flex-direction: column;
}

.type-desc {
  font-size: 12px;
  color: #909399;
}

.field-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
