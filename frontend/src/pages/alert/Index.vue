<script setup lang="ts">
/**
 * 告警记录页面
 *
 * @author zhuhx
 */
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getAlertHistoryList, exportAlertHistory } from '@/api/alert'
import type { AlertHistory, AlertHistoryQuery, AlertType, AlertStatus } from '@/types/alert'
import { marked } from 'marked'

/** 查询表单 */
const queryForm = ref<AlertHistoryQuery>({
  current: 1,
  size: 20,
  type: undefined,
  status: undefined,
  startTime: undefined,
  endTime: undefined
})

/** 告警历史列表 */
const alertList = ref<AlertHistory[]>([])
/** 总记录数 */
const total = ref(0)
/** 加载状态 */
const loading = ref(false)
/** 导出加载状态 */
const exporting = ref(false)
/** 详情对话框显示状态 */
const detailDialogVisible = ref(false)
/** 当前查看的告警详情 */
const currentAlert = ref<AlertHistory | null>(null)

// 告警类型选项
const alertTypeOptions = [
  { label: '成本告警', value: 'COST' },
  { label: '异常告警', value: 'ERROR_RATE' },
  { label: '审批提醒', value: 'APPROVAL' },
  { label: '系统告警', value: 'SYSTEM' }
]

// 告警状态选项
const alertStatusOptions = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' }
]

// 告警类型映射
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const alertTypeMap: Record<AlertType, { label: string; color: TagType }> = {
  COST: { label: '成本告警', color: 'warning' },
  ERROR_RATE: { label: '异常告警', color: 'danger' },
  APPROVAL: { label: '审批提醒', color: 'primary' },
  SYSTEM: { label: '系统告警', color: 'info' }
}

// 告警状态映射
const alertStatusMap: Record<AlertStatus, { label: string; color: TagType }> = {
  SUCCESS: { label: '成功', color: 'success' },
  FAILED: { label: '失败', color: 'danger' }
}

/**
 * 获取告警类型显示信息
 */
function getAlertTypeInfo(type: AlertType) {
  return alertTypeMap[type] || { label: type, color: 'info' }
}

/**
 * 获取告警状态显示信息
 */
function getAlertStatusInfo(status: AlertStatus) {
  return alertStatusMap[status] || { label: status, color: 'info' }
}

/**
 * 获取通知渠道显示标签
 */
function getChannelLabel(channel: string): string {
  const channelMap: Record<string, string> = {
    EMAIL: '邮件',
    WEBHOOK: 'Webhook',
    DINGTALK: '钉钉',
    WECOM: '企业微信'
  }
  return channelMap[channel] || channel
}

/**
 * 获取通知渠道数组
 */
function getChannelArray(channelType: string): string[] {
  if (!channelType) return []
  return channelType.split(',').filter(c => c.trim())
}

/**
 * 格式化时间显示
 */
function formatTime(dateStr: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

/**
 * 查看告警详情
 */
function handleViewDetail(alert: AlertHistory) {
  currentAlert.value = alert
  detailDialogVisible.value = true
}

/**
 * 渲染Markdown内容
 */
function renderMarkdown(content: string): string {
  if (!content) return ''
  try {
    return marked(content) as string
  } catch (error) {
    console.error('Markdown渲染失败:', error)
    return content
  }
}

/**
 * 查询告警历史
 */
async function fetchAlertHistory() {
  loading.value = true
  try {
    const res = await getAlertHistoryList(queryForm.value)
    alertList.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('查询告警历史失败:', error)
    ElMessage.error('查询告警历史失败')
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 */
function handleSearch() {
  queryForm.value.current = 1
  fetchAlertHistory()
}

/**
 * 重置查询条件
 */
function handleReset() {
  queryForm.value = {
    current: 1,
    size: 20,
    type: undefined,
    status: undefined,
    startTime: undefined,
    endTime: undefined
  }
  fetchAlertHistory()
}

/**
 * 分页变化
 */
function handlePageChange(page: number) {
  queryForm.value.current = page
  fetchAlertHistory()
}

/**
 * 每页条数变化
 */
function handleSizeChange(size: number) {
  queryForm.value.size = size
  queryForm.value.current = 1
  fetchAlertHistory()
}

/**
 * 导出告警历史
 */
async function handleExport() {
  exporting.value = true
  try {
    const { current, size, ...exportParams } = queryForm.value
    await exportAlertHistory(exportParams)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出告警历史失败:', error)
    ElMessage.error('导出告警历史失败')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  fetchAlertHistory()
})
</script>

<template>
  <div class="alert-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>告警记录</span>
        </div>
      </template>

      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="告警类型">
          <el-select
            v-model="queryForm.type"
            placeholder="全部类型"
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
        <el-form-item label="告警状态">
          <el-select
            v-model="queryForm.status"
            placeholder="全部状态"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in alertStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.startTime"
            type="datetime"
            placeholder="开始时间"
            style="width: 180px"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
          <span style="margin: 0 8px">-</span>
          <el-date-picker
            v-model="queryForm.endTime"
            type="datetime"
            placeholder="结束时间"
            style="width: 180px"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button :icon="Download" :loading="exporting" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="alertList"
        style="width: 100%"
        stripe
      >
        <el-table-column prop="type" label="告警类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getAlertTypeInfo(row.type).color" size="small">
              {{ getAlertTypeInfo(row.type).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="告警标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="channelType" label="通知渠道" width="200">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; flex-wrap: wrap; gap: 4px">
              <template v-if="getChannelArray(row.channelType).length <= 2">
                <el-tag
                  v-for="channel in getChannelArray(row.channelType)"
                  :key="channel"
                  size="small"
                >
                  {{ getChannelLabel(channel) }}
                </el-tag>
              </template>
              <template v-else>
                <el-tag
                  v-for="channel in getChannelArray(row.channelType).slice(0, 2)"
                  :key="channel"
                  size="small"
                >
                  {{ getChannelLabel(channel) }}
                </el-tag>
                <el-tooltip placement="top">
                  <template #content>
                    <div style="max-width: 300px">
                      <div v-for="channel in getChannelArray(row.channelType)" :key="channel" style="margin: 2px 0">
                        {{ getChannelLabel(channel) }}
                      </div>
                    </div>
                  </template>
                  <el-tag size="small" type="info">
                    +{{ getChannelArray(row.channelType).length - 2 }}
                  </el-tag>
                </el-tooltip>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="发送状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getAlertStatusInfo(row.status).color" size="small">
              {{ getAlertStatusInfo(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.errorMessage || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="sentAt" label="发送时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.sentAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.current"
        v-model:page-size="queryForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <!-- 告警详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="告警详情"
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-if="currentAlert" class="alert-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="告警类型">
            <el-tag :type="getAlertTypeInfo(currentAlert.type).color" size="small">
              {{ getAlertTypeInfo(currentAlert.type).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发送状态">
            <el-tag :type="getAlertStatusInfo(currentAlert.status).color" size="small">
              {{ getAlertStatusInfo(currentAlert.status).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="告警标题" :span="2">
            {{ currentAlert.title }}
          </el-descriptions-item>
          <el-descriptions-item label="通知渠道" :span="2">
            <el-tag
              v-for="channel in (currentAlert.channelType || '').split(',')"
              :key="channel"
              size="small"
              style="margin-right: 4px"
            >
              {{ getChannelLabel(channel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="接收人" :span="2">
            {{ currentAlert.recipient || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="发送时间" :span="2">
            {{ formatTime(currentAlert.sentAt) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentAlert.errorMessage" label="错误信息" :span="2">
            <el-text type="danger">{{ currentAlert.errorMessage }}</el-text>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">告警内容</el-divider>
        <div class="alert-content markdown-body" v-html="renderMarkdown(currentAlert.content)"></div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
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

.alert-detail {
  max-height: 600px;
  overflow-y: auto;
}

.alert-content {
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
  min-height: 100px;
  max-height: 400px;
  overflow-y: auto;
}

/* Markdown样式 */
.markdown-body {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4 {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
}

.markdown-body h1 {
  font-size: 24px;
}

.markdown-body h2 {
  font-size: 20px;
}

.markdown-body h3 {
  font-size: 18px;
}

.markdown-body h4 {
  font-size: 16px;
}

.markdown-body p {
  margin-bottom: 8px;
}

.markdown-body strong {
  font-weight: 600;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 24px;
  margin-bottom: 8px;
}

.markdown-body li {
  margin-bottom: 4px;
}

.markdown-body code {
  padding: 2px 4px;
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}

.markdown-body pre {
  padding: 12px;
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
  overflow-x: auto;
}

.markdown-body pre code {
  padding: 0;
  background-color: transparent;
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 8px;
}

.markdown-body table th,
.markdown-body table td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.markdown-body table th {
  background-color: rgba(0, 0, 0, 0.05);
  font-weight: 600;
}

.markdown-body blockquote {
  margin: 8px 0;
  padding-left: 12px;
  border-left: 4px solid #ddd;
  color: #666;
}
</style>
