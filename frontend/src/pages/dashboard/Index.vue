<script setup lang="ts">
/**
 * 仪表盘页面
 *
 * @author zhuhx
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Coin, TrendCharts, Monitor, Bell, Warning } from '@element-plus/icons-vue'
import { getLogList } from '@/api/log'
import { getStatsOverview } from '@/api/stats'
import { getApprovalList } from '@/api/approval'
import { getAgentList } from '@/api/agent'
import { getRecentAlertHistory, countAlertHistory } from '@/api/alert'
import type { AgentLog } from '@/types/log'
import type { StatsOverview } from '@/types/stats'
import type { AlertHistory, AlertType, AlertStatus } from '@/types/alert'

const router = useRouter()

/** 今日成本概览 */
const todayStats = ref<StatsOverview | null>(null)
/** 本月成本概览 */
const monthStats = ref<StatsOverview | null>(null)
/** Agent 总数 */
const totalAgents = ref(0)
/** 活跃 Agent 数 */
const activeAgents = ref(0)
/** 待审批数量 */
const pendingApprovalCount = ref(0)
/** 统计加载状态 */
const statsLoading = ref(false)

/** 最近日志列表 */
const recentLogs = ref<AgentLog[]>([])
/** 日志加载状态 */
const logsLoading = ref(false)

/** 最近告警列表 */
const recentAlerts = ref<AlertHistory[]>([])
/** 告警加载状态 */
const alertsLoading = ref(false)
/** 失败告警数量 */
const failedAlertCount = ref(0)

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
 * 获取今日日期范围
 */
function getTodayRange() {
  const today = new Date()
  const dateStr = today.toISOString().split('T')[0]
  return { startDate: dateStr, endDate: dateStr }
}

/**
 * 获取本月日期范围
 */
function getMonthRange() {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const startDate = `${year}-${month}-01`
  const endDate = today.toISOString().split('T')[0]
  return { startDate, endDate }
}

/**
 * 获取成本统计数据
 */
async function fetchStats() {
  statsLoading.value = true
  try {
    // 并行获取今日和本月成本
    const [todayRes, monthRes] = await Promise.all([
      getStatsOverview(getTodayRange()),
      getStatsOverview(getMonthRange())
    ])
    todayStats.value = todayRes
    monthStats.value = monthRes
  } catch (error) {
    console.error('获取成本统计失败:', error)
  } finally {
    statsLoading.value = false
  }
}

/**
 * 获取 Agent 统计
 */
async function fetchAgentStats() {
  try {
    const res = await getAgentList({ current: 1, size: 1 })
    totalAgents.value = res.total || 0
    // 活跃 Agent 暂时使用总数，后续可根据实际业务逻辑调整
    activeAgents.value = res.total || 0
  } catch (error) {
    console.error('获取Agent统计失败:', error)
  }
}

/**
 * 获取待审批数量
 */
async function fetchPendingApprovals() {
  try {
    const res = await getApprovalList({ current: 1, size: 1, status: 'PENDING' })
    pendingApprovalCount.value = res.total || 0
  } catch (error) {
    console.error('获取待审批数量失败:', error)
  }
}

/**
 * 获取最近 5 条日志
 */
async function fetchRecentLogs() {
  logsLoading.value = true
  try {
    const res = await getLogList({ current: 1, size: 5 })
    recentLogs.value = res.records || []
  } catch (error) {
    console.error('获取最近日志失败:', error)
  } finally {
    logsLoading.value = false
  }
}

/**
 * 跳转到日志列表页面
 */
function goToLogs() {
  router.push('/logs')
}

/**
 * 跳转到审批中心页面
 */
function goToApprovals() {
  router.push('/approvals')
}

/**
 * 跳转到成本分析页面
 */
function goToStats() {
  router.push('/stats')
}

/**
 * 跳转到告警记录页面
 */
function goToAlerts() {
  router.push('/alerts')
}

/**
 * 获取最近告警
 */
async function fetchRecentAlerts() {
  alertsLoading.value = true
  try {
    const [alerts, failedCount] = await Promise.all([
      getRecentAlertHistory(5),
      countAlertHistory({ status: 'FAILED' })
    ])
    recentAlerts.value = alerts || []
    failedAlertCount.value = failedCount || 0
  } catch (error) {
    console.error('获取最近告警失败:', error)
  } finally {
    alertsLoading.value = false
  }
}

/**
 * 获取告警类型显示信息
 */
function getAlertTypeInfo(type: AlertType) {
  return alertTypeMap[type] || { label: type, color: 'default' }
}

/**
 * 获取告警状态显示信息
 */
function getAlertStatusInfo(status: AlertStatus) {
  return alertStatusMap[status] || { label: status, color: 'default' }
}

/**
 * 格式化时间显示
 */
function formatTime(dateStr: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 获取状态标签类型
 */
function getStatusType(status: string): 'success' | 'danger' | 'warning' {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'BLOCKED':
      return 'danger'
    case 'FAILED':
      return 'warning'
    default:
      return 'warning'
  }
}

/**
 * 获取状态显示文本
 */
function getStatusText(status: string): string {
  switch (status) {
    case 'SUCCESS':
      return '成功'
    case 'BLOCKED':
      return '已拦截'
    case 'FAILED':
      return '失败'
    default:
      return status
  }
}

/**
 * 获取请求类型显示文本
 */
function getRequestTypeText(type: string): string {
  switch (type) {
    case 'API_CALL':
      return 'API调用'
    case 'LLM_CALL':
      return 'LLM调用'
    default:
      return type
  }
}

onMounted(() => {
  fetchStats()
  fetchAgentStats()
  fetchPendingApprovals()
  fetchRecentLogs()
  fetchRecentAlerts()
})
</script>

<template>
  <div class="dashboard">
    <!-- 成本概览卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="goToStats">
          <template #header>
            <div class="card-header-with-icon">
              <el-icon class="header-icon" color="#409eff"><Coin /></el-icon>
              <span>今日成本</span>
            </div>
          </template>
          <div class="stat-value" v-loading="statsLoading">
            ${{ todayStats?.totalCost?.toFixed(2) || '0.00' }}
          </div>
          <div class="stat-sub">
            Token: {{ (todayStats?.totalTokens || 0).toLocaleString() }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" @click="goToStats">
          <template #header>
            <div class="card-header-with-icon">
              <el-icon class="header-icon" color="#67c23a"><TrendCharts /></el-icon>
              <span>本月成本</span>
            </div>
          </template>
          <div class="stat-value" v-loading="statsLoading">
            ${{ monthStats?.totalCost?.toFixed(2) || '0.00' }}
          </div>
          <div class="stat-sub">
            调用: {{ (monthStats?.totalCalls || 0).toLocaleString() }} 次
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header-with-icon">
              <el-icon class="header-icon" color="#e6a23c"><Monitor /></el-icon>
              <span>Agent 总数</span>
            </div>
          </template>
          <div class="stat-value">{{ totalAgents }}</div>
          <div class="stat-sub">
            活跃: {{ activeAgents }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card approval-card" @click="goToApprovals">
          <template #header>
            <div class="card-header-with-icon">
              <el-icon class="header-icon" color="#f56c6c"><Bell /></el-icon>
              <span>待审批</span>
            </div>
          </template>
          <div class="stat-value" :class="{ 'has-pending': pendingApprovalCount > 0 }">
            {{ pendingApprovalCount }}
          </div>
          <div class="stat-sub clickable-hint">
            点击查看详情 →
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>调用趋势</template>
          <div class="chart-placeholder">图表区域 - ECharts</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="approval-list-card">
          <template #header>
            <div class="card-header">
              <span>待审批任务</span>
              <el-button type="primary" link @click="goToApprovals">查看全部</el-button>
            </div>
          </template>
          <div v-if="pendingApprovalCount > 0" class="pending-approval-info">
            <el-icon class="pending-icon" color="#f56c6c"><Warning /></el-icon>
            <span>您有 <strong>{{ pendingApprovalCount }}</strong> 条待审批请求</span>
          </div>
          <el-empty v-else description="暂无待审批任务" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="alert-list-card">
          <template #header>
            <div class="card-header">
              <span>最近告警</span>
              <el-button type="primary" link @click="goToAlerts">更多告警</el-button>
            </div>
          </template>
          <div v-loading="alertsLoading">
            <div v-if="failedAlertCount > 0" class="failed-alert-info">
              <el-icon class="alert-icon" color="#f56c6c"><Warning /></el-icon>
              <span><strong>{{ failedAlertCount }}</strong> 条告警发送失败</span>
            </div>
            <div v-if="recentAlerts.length > 0" class="recent-alerts">
              <div v-for="alert in recentAlerts.slice(0, 3)" :key="alert.id" class="alert-item">
                <el-tag :type="getAlertTypeInfo(alert.type).color" size="small">
                  {{ getAlertTypeInfo(alert.type).label }}
                </el-tag>
                <span class="alert-title">{{ alert.title }}</span>
                <el-tag :type="getAlertStatusInfo(alert.status).color" size="small">
                  {{ getAlertStatusInfo(alert.status).label }}
                </el-tag>
              </div>
            </div>
            <el-empty v-else-if="!alertsLoading" description="暂无告警记录" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近调用日志</span>
              <el-button type="primary" link @click="goToLogs">查看全部</el-button>
            </div>
          </template>
          <el-table
            v-loading="logsLoading"
            :data="recentLogs"
            style="width: 100%"
            @row-click="goToLogs"
            class="clickable-table"
          >
            <el-table-column prop="createdAt" label="时间" width="140">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="agentName" label="Agent" width="150">
              <template #default="{ row }">
                {{ row.agentName || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="requestType" label="类型" width="100">
              <template #default="{ row }">
                {{ getRequestTypeText(row.requestType) }}
              </template>
            </el-table-column>
            <el-table-column prop="endpoint" label="端点" min-width="200" show-overflow-tooltip />
            <el-table-column prop="responseStatus" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.responseStatus)" size="small">
                  {{ getStatusText(row.responseStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="responseTimeMs" label="响应时间" width="100">
              <template #default="{ row }">
                {{ row.responseTimeMs ? `${row.responseTimeMs}ms` : '-' }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!logsLoading && recentLogs.length === 0" description="暂无调用日志" />
        </el-card>
      </el-col>
    </el-row>
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

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.stat-value.has-pending {
  color: #f56c6c;
}

.stat-sub {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.clickable-hint {
  color: #409eff;
}

.card-header-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 18px;
}

.chart-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  background: #f5f5f5;
  border-radius: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.approval-card {
  border-left: 3px solid #f56c6c;
}

.approval-list-card {
  min-height: 200px;
}

.pending-approval-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 20px;
  font-size: 16px;
  color: #606266;
}

.pending-icon {
  font-size: 24px;
}

.clickable-table :deep(tbody tr) {
  cursor: pointer;
}

.clickable-table :deep(tbody tr:hover) {
  background-color: #f5f7fa;
}

.alert-list-card {
  min-height: 200px;
}

.failed-alert-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #fef0f0;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #f56c6c;
}

.alert-icon {
  font-size: 16px;
}

.recent-alerts {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.alert-item:last-child {
  border-bottom: none;
}

.alert-title {
  flex: 1;
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
