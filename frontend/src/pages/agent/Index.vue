<script setup lang="ts">
/**
 * Agent 列表页面
 *
 * @author zhuhx
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Hide, CopyDocument } from '@element-plus/icons-vue'
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
const activeTab = ref('basic')
const formData = ref<AgentCreateDTO>({
  name: '',
  description: ''
})

// 策略绑定相关
const allPolicies = ref<Policy[]>([])
const selectedPolicyIds = ref<string[]>([])

// 密钥可见性状态管理
const visibleKeys = ref<Set<string>>(new Set())

/**
 * 脱敏显示 API Key
 */
function maskApiKey(apiKey: string): string {
  if (!apiKey || apiKey.length < 10) return apiKey
  const prefixLength = 7
  const suffixLength = 4
  const prefix = apiKey.substring(0, prefixLength)
  const suffix = apiKey.substring(apiKey.length - suffixLength)
  const masked = '*'.repeat(16)
  return `${prefix}${masked}${suffix}`
}

/**
 * 切换密钥可见性
 */
function toggleKeyVisibility(agentId: string) {
  if (visibleKeys.value.has(agentId)) {
    visibleKeys.value.delete(agentId)
  } else {
    visibleKeys.value.add(agentId)
  }
}

/**
 * 判断密钥是否可见
 */
function isKeyVisible(agentId: string): boolean {
  return visibleKeys.value.has(agentId)
}

/**
 * 复制密钥到剪贴板
 */
async function copyApiKey(apiKey: string) {
  try {
    await navigator.clipboard.writeText(apiKey)
    ElMessage.success('密钥已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
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

async function fetchAllPolicies() {
  try {
    // 只获取 Agent 级策略
    const res = await policyApi.getPolicyList({ current: 1, size: 100, scope: 'AGENT' })
    allPolicies.value = res.records
  } catch (e) {
    // error handled by interceptor
  }
}

function resetForm() {
  formData.value = {
    name: '',
    description: ''
  }
  selectedPolicyIds.value = []
  activeTab.value = 'basic'
  isEditMode.value = false
  editingAgentId.value = null
}

async function handleOpenCreate() {
  resetForm()
  // 立即打开对话框，提升响应速度
  dialogVisible.value = true
  // 异步加载策略列表
  loading.value = true
  try {
    await fetchAllPolicies()
  } finally {
    loading.value = false
  }
}

async function handleOpenEdit(agent: Agent) {
  isEditMode.value = true
  editingAgentId.value = agent.id
  formData.value = {
    name: agent.name,
    description: agent.description || ''
  }

  // 立即打开对话框，提升响应速度
  dialogVisible.value = true

  // 并行加载策略数据
  loading.value = true
  try {
    const [_, boundPolicies] = await Promise.all([
      fetchAllPolicies(),
      agentApi.getAgentPolicies(agent.id)
    ])
    selectedPolicyIds.value = boundPolicies.map(p => p.id)
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  try {
    if (isEditMode.value && editingAgentId.value) {
      // 更新 Agent 基本信息
      const updateData: AgentUpdateDTO = {
        name: formData.value.name,
        description: formData.value.description
      }
      await agentApi.updateAgent(editingAgentId.value, updateData)
      
      // 更新策略绑定
      await updatePolicyBindings(editingAgentId.value)
      
      ElMessage.success('更新成功')
    } else {
      // 创建 Agent
      const newAgent = await agentApi.createAgent(formData.value)
      
      // 绑定策略
      if (selectedPolicyIds.value.length > 0) {
        await Promise.all(
          selectedPolicyIds.value.map(policyId => 
            agentApi.bindPolicy(newAgent.id, policyId)
          )
        )
      }
      
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    resetForm()
    fetchData()
  } catch (e) {
    // error handled by interceptor
  }
}

/**
 * 更新策略绑定（对比差异，只调用需要的接口）
 */
async function updatePolicyBindings(agentId: string) {
  const currentPolicies = await agentApi.getAgentPolicies(agentId)
  const currentPolicyIds = new Set(currentPolicies.map(p => p.id))
  const newPolicyIds = new Set(selectedPolicyIds.value)
  
  // 需要解绑的策略
  const toUnbind = [...currentPolicyIds].filter(id => !newPolicyIds.has(id))
  // 需要绑定的策略
  const toBind = [...newPolicyIds].filter(id => !currentPolicyIds.has(id))
  
  await Promise.all([
    ...toUnbind.map(policyId => agentApi.unbindPolicy(agentId, policyId)),
    ...toBind.map(policyId => agentApi.bindPolicy(agentId, policyId))
  ])
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

const policyTypeMap: Record<string, { label: string; type: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  ACCESS_CONTROL: { label: '访问控制', type: 'primary' },
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
        <el-table-column prop="name" label="名称" width="200" />
        <el-table-column label="密钥" width="280">
          <template #default="{ row }">
            <div class="api-key-cell">
              <span class="api-key-text">
                {{ isKeyVisible(row.id) ? row.apiKey : maskApiKey(row.apiKey) }}
              </span>
              <div class="api-key-actions">
                <el-icon
                  class="action-icon"
                  @click="toggleKeyVisibility(row.id)"
                  :title="isKeyVisible(row.id) ? '隐藏密钥' : '显示密钥'"
                >
                  <View v-if="!isKeyVisible(row.id)" />
                  <Hide v-else />
                </el-icon>
                <el-icon
                  class="action-icon"
                  @click="copyApiKey(row.apiKey)"
                  title="复制密钥"
                >
                  <CopyDocument />
                </el-icon>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="绑定策略" width="120">
          <template #default="{ row }">
            <el-popover
              v-if="row.policies && row.policies.length > 0"
              placement="top"
              :width="300"
              trigger="hover"
            >
              <template #reference>
                <el-tag type="primary" style="cursor: pointer">
                  {{ row.policies.length }} 个策略
                </el-tag>
              </template>
              <div class="policy-popover">
                <div class="policy-popover-title">绑定的策略</div>
                <div
                  v-for="policy in row.policies"
                  :key="policy.id"
                  class="policy-popover-item"
                >
                  <span :class="{ 'policy-disabled': !policy.enabled }">
                    {{ policy.name }}
                  </span>
                  <el-tag
                    :type="policy.enabled ? 'success' : 'info'"
                    size="small"
                    style="margin-left: 8px"
                  >
                    {{ policy.enabled ? '已启用' : '未启用' }}
                  </el-tag>
                </div>
              </div>
            </el-popover>
            <span v-else style="color: var(--el-text-color-secondary)">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
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
      width="600px"
      @close="handleDialogClose"
    >
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="formData" label-width="100px" style="padding: 20px 0">
            <el-form-item label="名称" required>
              <el-input v-model="formData.name" placeholder="请输入Agent名称" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述信息" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="策略绑定" name="policy">
          <el-form label-width="100px" style="padding: 20px 0" v-loading="loading">
            <el-form-item label="绑定策略">
              <el-select
                v-model="selectedPolicyIds"
                multiple
                placeholder="选择要绑定的 Agent 级策略"
                style="width: 100%"
                filterable
                collapse-tags
                collapse-tags-tooltip
                :disabled="loading"
              >
                <el-option
                  v-for="policy in allPolicies"
                  :key="policy.id"
                  :label="policy.name"
                  :value="policy.id"
                >
                  <div style="display: flex; align-items: center; justify-content: space-between">
                    <span>{{ policy.name }}</span>
                    <el-tag :type="getPolicyTypeTagType(policy.type)" size="small">
                      {{ getPolicyTypeLabel(policy.type) }}
                    </el-tag>
                  </div>
                </el-option>
              </el-select>
              <div style="margin-top: 8px; font-size: 12px; color: var(--el-text-color-secondary)">
                全局策略自动对所有 Agent 生效，无需手动绑定
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">{{ isEditMode ? '保存' : '确定' }}</el-button>
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

.api-key-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 6px;
  background-color: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
}

.api-key-text {
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  color: var(--el-text-color-regular);
  scrollbar-width: none;
}

.api-key-text::-webkit-scrollbar {
  display: none;
}

.api-key-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.action-icon {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  font-size: 16px;
  transition: color 0.3s;
}

.action-icon:hover {
  color: var(--el-color-primary);
}

.policy-popover {
  max-height: 300px;
  overflow-y: auto;
}

.policy-popover-title {
  font-weight: 600;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.policy-popover-item {
  padding: 6px 0;
  color: var(--el-text-color-regular);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.policy-popover-item:not(:last-child) {
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.policy-disabled {
  color: var(--el-text-color-secondary);
  text-decoration: line-through;
}
</style>
