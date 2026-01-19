<script setup lang="ts">
/**
 * Agent 列表页面
 *
 * @author zhuhx
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as agentApi from '@/api/agent'
import * as policyApi from '@/api/policy'
import type { Agent, AgentCreateDTO, AgentUpdateDTO } from '@/types/agent'
import type { Policy } from '@/types/policy'

const loading = ref(false)
const agents = ref<Agent[]>([])
const total = ref(0)
const queryParams = ref({
  current: 1,
  size: 10,
  keyword: ''
})

const dialogVisible = ref(false)
const isEditMode = ref(false)
const editingAgentId = ref<string | null>(null)
const formData = ref<AgentCreateDTO>({
  name: '',
  type: 'OTHER',
  department: '',
  environment: 'TEST',
  description: ''
})

// 策略绑定相关
const policyDialogVisible = ref(false)
const currentAgent = ref<Agent | null>(null)
const boundPolicies = ref<Policy[]>([])
const allPolicies = ref<Policy[]>([])
const policyLoading = ref(false)
const selectedPolicyId = ref('')

const agentTypes = [
  { label: '客服', value: 'CUSTOMER_SERVICE' },
  { label: '财务', value: 'FINANCE' },
  { label: '运营', value: 'OPERATION' },
  { label: '内部工具', value: 'INTERNAL' },
  { label: '其他', value: 'OTHER' }
]

function getAgentTypeText(type: string): string {
  const found = agentTypes.find(t => t.value === type)
  return found ? found.label : type
}

async function fetchData() {
  loading.value = true
  try {
    const res = await agentApi.getAgentList(queryParams.value)
    agents.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  formData.value = {
    name: '',
    type: 'OTHER',
    department: '',
    environment: 'TEST',
    description: ''
  }
  isEditMode.value = false
  editingAgentId.value = null
}

function handleOpenCreate() {
  resetForm()
  dialogVisible.value = true
}

async function handleOpenEdit(agent: Agent) {
  isEditMode.value = true
  editingAgentId.value = agent.id
  formData.value = {
    name: agent.name,
    type: agent.type,
    department: agent.department || '',
    environment: agent.environment || 'TEST',
    description: agent.description || ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (isEditMode.value && editingAgentId.value) {
      const updateData: AgentUpdateDTO = {
        name: formData.value.name,
        type: formData.value.type,
        department: formData.value.department,
        environment: formData.value.environment,
        description: formData.value.description
      }
      await agentApi.updateAgent(editingAgentId.value, updateData)
      ElMessage.success('更新成功')
    } else {
      await agentApi.createAgent(formData.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    resetForm()
    fetchData()
  } catch (e) {
    // error handled by interceptor
  }
}

async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm('确定要删除该Agent吗？删除后无法恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await agentApi.deleteAgent(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    // user cancelled or error handled by interceptor
  }
}

function handleSearch() {
  queryParams.value.current = 1
  fetchData()
}

function handlePageChange(page: number) {
  queryParams.value.current = page
  fetchData()
}

function handleDialogClose() {
  resetForm()
}

// ==================== 策略绑定相关 ====================

async function handleOpenPolicyDialog(agent: Agent) {
  currentAgent.value = agent
  policyDialogVisible.value = true
  selectedPolicyId.value = ''
  await Promise.all([fetchBoundPolicies(agent.id), fetchAllPolicies()])
}

async function fetchBoundPolicies(agentId: string) {
  policyLoading.value = true
  try {
    boundPolicies.value = await agentApi.getAgentPolicies(agentId)
  } finally {
    policyLoading.value = false
  }
}

async function fetchAllPolicies() {
  try {
    // 只获取 Agent 级策略（scope = 'AGENT'），全局策略自动生效无需绑定
    const res = await policyApi.getPolicyList({ current: 1, size: 100, scope: 'AGENT' })
    allPolicies.value = res.records
  } catch (e) {
    // error handled by interceptor
  }
}

/** 获取可绑定的策略（排除已绑定的） */
function getAvailablePolicies() {
  const boundIds = new Set(boundPolicies.value.map(p => p.id))
  return allPolicies.value.filter(p => !boundIds.has(p.id))
}

async function handleBindPolicy() {
  if (!selectedPolicyId.value || !currentAgent.value) return
  try {
    await agentApi.bindPolicy(currentAgent.value.id, selectedPolicyId.value)
    ElMessage.success('绑定成功')
    selectedPolicyId.value = ''
    await fetchBoundPolicies(currentAgent.value.id)
  } catch (e) {
    // error handled by interceptor
  }
}

async function handleUnbindPolicy(policyId: string) {
  if (!currentAgent.value) return
  try {
    await ElMessageBox.confirm('确定要解绑该策略吗？', '解绑确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await agentApi.unbindPolicy(currentAgent.value.id, policyId)
    ElMessage.success('解绑成功')
    await fetchBoundPolicies(currentAgent.value.id)
  } catch (e) {
    // user cancelled or error handled by interceptor
  }
}

const policyTypeMap: Record<string, { label: string; type: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  ACCESS_CONTROL: { label: '访问控制', type: 'primary' },
  CONTENT_PROTECTION: { label: '内容保护', type: 'warning' },
  APPROVAL: { label: '审批', type: 'danger' },
  RATE_LIMIT: { label: '频率限制', type: 'info' }
}

function getPolicyTypeLabel(type: string) {
  return policyTypeMap[type]?.label || type
}

function getPolicyTypeTagType(type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  return policyTypeMap[type]?.type || 'info'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="agent-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Agent 列表</span>
          <el-button type="primary" @click="handleOpenCreate">新建 Agent</el-button>
        </div>
      </template>

      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item>
          <el-input v-model="queryParams.keyword" placeholder="搜索Agent名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="agents" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            {{ getAgentTypeText(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="environment" label="环境" width="100">
          <template #default="{ row }">
            <el-tag :type="row.environment === 'PRODUCTION' ? 'danger' : 'info'">
              {{ row.environment === 'PRODUCTION' ? '生产' : '测试' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '活跃' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleOpenPolicyDialog(row)">策略</el-button>
            <el-button link type="primary" @click="handleOpenEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
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

    <!-- 新建/编辑 Agent 弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditMode ? '编辑 Agent' : '新建 Agent'"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="formData.name" placeholder="请输入Agent名称" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="formData.type" style="width: 100%">
            <el-option v-for="t in agentTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="formData.department" placeholder="请输入所属部门" />
        </el-form-item>
        <el-form-item label="环境">
          <el-radio-group v-model="formData.environment">
            <el-radio value="TEST">测试</el-radio>
            <el-radio value="PRODUCTION">生产</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">{{ isEditMode ? '保存' : '确定' }}</el-button>
      </template>
    </el-dialog>

    <!-- 策略绑定弹窗 -->
    <el-dialog
      v-model="policyDialogVisible"
      :title="`管理策略 - ${currentAgent?.name || ''}`"
      width="650px"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #title>
          <span>全局策略自动对所有 Agent 生效，无需手动绑定。此处仅管理 Agent 级策略的绑定关系。</span>
        </template>
      </el-alert>

      <div class="policy-bind-section">
        <div class="bind-form">
          <el-select
            v-model="selectedPolicyId"
            placeholder="选择要绑定的 Agent 级策略"
            style="width: 350px"
            filterable
            :no-data-text="'暂无可绑定的 Agent 级策略'"
          >
            <el-option
              v-for="policy in getAvailablePolicies()"
              :key="policy.id"
              :label="policy.name"
              :value="policy.id"
            >
              <span>{{ policy.name }}</span>
              <el-tag :type="getPolicyTypeTagType(policy.type)" size="small" style="margin-left: 8px">
                {{ getPolicyTypeLabel(policy.type) }}
              </el-tag>
            </el-option>
          </el-select>
          <el-button
            type="primary"
            :disabled="!selectedPolicyId"
            @click="handleBindPolicy"
            style="margin-left: 12px"
          >
            绑定
          </el-button>
        </div>
      </div>

      <el-divider content-position="left">已绑定的 Agent 级策略</el-divider>

      <el-table :data="boundPolicies" v-loading="policyLoading" stripe max-height="300">
        <el-table-column prop="name" label="策略名称" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getPolicyTypeTagType(row.type)" size="small">
              {{ getPolicyTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="动作" width="100" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleUnbindPolicy(row.id)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template v-if="boundPolicies.length === 0 && !policyLoading">
        <el-empty description="暂无绑定的 Agent 级策略" :image-size="60" />
      </template>

      <template #footer>
        <el-button @click="policyDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.policy-bind-section {
  margin-bottom: 16px;
}

.bind-form {
  display: flex;
  align-items: center;
}
</style>
