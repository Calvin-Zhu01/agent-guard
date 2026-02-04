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
        <el-table-column prop="content" label="告警内容" min-width="250" show-overflow-tooltip />
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
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
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
</style>
