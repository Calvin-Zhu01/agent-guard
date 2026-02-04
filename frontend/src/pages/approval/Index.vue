<script setup lang="ts">
/**
 * 审批中心页面
 *
 * @author zhuhx
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as approvalApi from '@/api/approval'
import type { Approval, ApprovalStatus } from '@/types/approval'

// 列表数据
const loading = ref(false)
const approvals = ref<Approval[]>([])
const total = ref(0)
const queryParams = ref({
  current: 1,
  size: 10,
  status: undefined as ApprovalStatus | undefined,
  approvalId: undefined as string | undefined
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentApproval = ref<Approval | null>(null)

// 操作加载状态
const actionLoading = ref(false)

// Tag 颜色类型
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

// 审批状态映射
const approvalStatusMap: Record<ApprovalStatus, { label: string; color: TagType }> = {
  PENDING: { label: '待审批', color: 'warning' },
  APPROVED: { label: '已批准', color: 'success' },
  REJECTED: { label: '已拒绝', color: 'danger' },
  EXPIRED: { label: '已过期', color: 'info' }
}

// 审批状态选项
const approvalStatusOptions: { label: string; value: ApprovalStatus }[] = [
  { label: '待审批', value: 'PENDING' },
  { label: '已批准', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已过期', value: 'EXPIRED' }
]

/**
 * 获取审批状态显示信息
 */
function getStatusInfo(status: ApprovalStatus) {
  return approvalStatusMap[status] || { label: status, color: 'default' }
}

/**
 * 获取审批列表
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await approvalApi.getApprovalList(queryParams.value)
    approvals.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/**
 * 打开详情对话框
 */
async function handleViewDetail(approval: Approval) {
  try {
    const detail = await approvalApi.getApprovalById(approval.id)
    currentApproval.value = detail
    detailDialogVisible.value = true
  } catch (e) {
    // error handled by interceptor
  }
}

/**
 * 格式化请求数据用于显示
 */
function formatRequestData(data: string): string {
  if (!data) return '-'
  try {
    const parsed = JSON.parse(data)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return data
  }
}

/**
 * 批准审批请求
 */
async function handleApprove(approval: Approval) {
  try {
    await ElMessageBox.confirm(
      `确定要批准该审批请求吗？`,
      '批准确认',
      {
        type: 'warning',
        confirmButtonText: '确定批准',
        cancelButtonText: '取消'
      }
    )
    actionLoading.value = true
    await approvalApi.approveApproval(approval.id)
    ElMessage.success('批准成功')
    detailDialogVisible.value = false
    fetchData()
  } catch (e) {
    // user cancelled or error handled by interceptor
  } finally {
    actionLoading.value = false
  }
}

/**
 * 拒绝审批请求
 */
async function handleReject(approval: Approval) {
  try {
    const { value: remark } = await ElMessageBox.prompt(
      '请输入拒绝原因（可选）',
      '拒绝确认',
      {
        type: 'warning',
        confirmButtonText: '确定拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入拒绝原因'
      }
    )
    actionLoading.value = true
    await approvalApi.rejectApproval(approval.id, { remark: remark || undefined })
    ElMessage.success('拒绝成功')
    detailDialogVisible.value = false
    fetchData()
  } catch (e) {
    // user cancelled or error handled by interceptor
  } finally {
    actionLoading.value = false
  }
}

/**
 * 查询
 */
function handleSearch() {
  queryParams.value.current = 1
  fetchData()
}

/**
 * 重置过滤
 */
function handleReset() {
  queryParams.value.status = undefined
  queryParams.value.approvalId = undefined
  queryParams.value.current = 1
  fetchData()
}

/**
 * 分页变化
 */
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

/**
 * 判断是否可以操作（只有待审批状态可以操作）
 */
function canOperate(approval: Approval): boolean {
  return approval.status === 'PENDING'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="approval-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审批中心</span>
        </div>
      </template>

      <!-- 过滤栏 -->
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="审批ID">
          <el-input
            v-model="queryParams.approvalId"
            placeholder="输入审批ID"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryParams.status"
            placeholder="全部状态"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in approvalStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 审批表格 -->
      <el-table :data="approvals" v-loading="loading" stripe>
        <el-table-column prop="id" label="审批ID" width="200" show-overflow-tooltip />
        <el-table-column prop="agentName" label="Agent" width="150">
          <template #default="{ row }">
            {{ row.agentName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="policyName" label="触发策略" min-width="200">
          <template #default="{ row }">
            {{ row.policyName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusInfo(row.status).color" size="small">
              {{ getStatusInfo(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="180" />
        <el-table-column prop="expiresAt" label="过期时间" width="180">
          <template #default="{ row }">
            {{ row.expiresAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="approvedAt" label="处理时间" width="180">
          <template #default="{ row }">
            {{ row.approvedAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">详情</el-button>
            <template v-if="canOperate(row)">
              <el-button link type="success" @click="handleApprove(row)">批准</el-button>
              <el-button link type="danger" @click="handleReject(row)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
      v-model="detailDialogVisible"
      title="审批详情"
      width="700px"
    >
      <template v-if="currentApproval">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="审批ID">
            {{ currentApproval.id }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusInfo(currentApproval.status).color" size="small">
              {{ getStatusInfo(currentApproval.status).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Agent">
            {{ currentApproval.agentName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="触发策略">
            {{ currentApproval.policyName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">
            {{ currentApproval.createdAt }}
          </el-descriptions-item>
          <el-descriptions-item label="过期时间">
            {{ currentApproval.expiresAt || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="处理人" v-if="currentApproval.approverId">
            {{ currentApproval.approverId }}
          </el-descriptions-item>
          <el-descriptions-item label="处理时间" v-if="currentApproval.approvedAt">
            {{ currentApproval.approvedAt }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 申请理由区域 -->
        <div class="detail-section">
          <h4>申请理由</h4>
          <div class="reason-content" :class="{ 'empty-content': !currentApproval.applicationReason }">
            {{ currentApproval.applicationReason || '等待提交中...' }}
          </div>
        </div>

        <!-- 审批备注区域 -->
        <div class="detail-section" v-if="currentApproval.remark">
          <h4>审批备注</h4>
          <div class="remark-content">
            {{ currentApproval.remark }}
          </div>
        </div>

        <!-- 请求数据区域 -->
        <div class="detail-section">
          <h4>请求数据</h4>
          <el-input
            type="textarea"
            :model-value="formatRequestData(currentApproval.requestData)"
            :rows="8"
            readonly
          />
        </div>
      </template>

      <template #footer>
        <template v-if="currentApproval && canOperate(currentApproval)">
          <el-button
            type="success"
            :loading="actionLoading"
            @click="handleApprove(currentApproval)"
          >
            批准
          </el-button>
          <el-button
            type="danger"
            :loading="actionLoading"
            @click="handleReject(currentApproval)"
          >
            拒绝
          </el-button>
        </template>
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

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  margin-bottom: 10px;
  color: #303133;
  font-weight: 500;
  font-size: 14px;
}

.reason-content,
.remark-content {
  padding: 12px;
  background-color: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow-y: auto;
}

.reason-content {
  background-color: #f0f9ff;
  border-color: #b3d8ff;
}

.reason-content.empty-content {
  background-color: #f5f7fa;
  border-color: #dcdfe6;
  color: #909399;
  text-align: center;
}

.remark-content {
  background-color: #fef0f0;
  border-color: #fbc4c4;
}
</style>
