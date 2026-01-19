<script setup lang="ts">
/**
 * 策略管理页面
 *
 * @author zhuhx
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as policyApi from '@/api/policy'
import type { Policy, PolicyCreateDTO, PolicyUpdateDTO, PolicyType, PolicyAction, PolicyScope } from '@/types/policy'

// 列表数据
const loading = ref(false)
const policies = ref<Policy[]>([])
const total = ref(0)
const queryParams = ref({
  current: 1,
  size: 10,
  keyword: '',
  type: undefined as PolicyType | undefined,
  scope: undefined as PolicyScope | undefined,
  sortBy: 'priority_desc' as 'priority_desc' | 'priority_asc' | 'updated_desc' | 'updated_asc'
})

// 对话框状态
const dialogVisible = ref(false)
const isEditMode = ref(false)
const editingPolicyId = ref<string | null>(null)
const formLoading = ref(false)

// 表单数据
const formData = ref({
  name: '',
  description: '',
  type: 'ACCESS_CONTROL' as PolicyType,
  priority: 0,
  scope: 'GLOBAL' as PolicyScope,
  // 访问控制配置
  accessControl: {
    mode: 'simple' as 'simple' | 'advanced',
    urlPattern: '',
    method: 'ALL',
    action: 'DENY' as 'ALLOW' | 'DENY',
    customConditions: ''
  },
  // 内容保护配置
  contentProtection: {
    mode: 'simple' as 'simple' | 'advanced',
    sensitiveFields: [] as string[],
    sensitiveKeywords: '',
    customConditions: ''
  },
  // 人工审批配置
  approval: {
    mode: 'simple' as 'simple' | 'advanced',
    simpleType: 'amount_threshold' as 'amount_threshold' | 'url_match',
    amountField: 'amount',
    amountThreshold: 10000,
    urlPattern: '',
    customConditions: ''
  },
  // 频率限制配置
  rateLimit: {
    mode: 'simple' as 'simple' | 'advanced',
    urlPattern: '',
    method: 'ALL',
    windowSeconds: 60,
    maxRequests: 100,
    customConditions: ''
  }
})

// Tag 颜色类型
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

// 策略类型映射
const policyTypeMap: Record<PolicyType, { label: string; color: TagType; desc: string }> = {
  ACCESS_CONTROL: { label: '访问控制', color: 'primary', desc: '控制 Agent 对特定 API 的访问权限' },
  CONTENT_PROTECTION: { label: '内容保护', color: 'success', desc: '对敏感信息进行脱敏处理' },
  APPROVAL: { label: '人工审批', color: 'warning', desc: '高风险操作需要人工审批后执行' },
  RATE_LIMIT: { label: '频率限制', color: 'info', desc: '限制 Agent 的请求频率' }
}

// 策略动作映射
const policyActionMap: Record<PolicyAction, { label: string; color: TagType }> = {
  ALLOW: { label: '允许', color: 'success' },
  DENY: { label: '拒绝', color: 'danger' },
  APPROVAL: { label: '审批', color: 'warning' },
  MASK: { label: '脱敏', color: 'info' },
  RATE_LIMIT: { label: '限流', color: 'warning' }
}

// 策略作用域映射
const policyScopeMap: Record<PolicyScope, { label: string; color: TagType }> = {
  GLOBAL: { label: '全局', color: 'primary' },
  AGENT: { label: 'Agent级', color: 'success' }
}

// 策略作用域选项
const policyScopeOptions: { label: string; value: PolicyScope }[] = [
  { label: '全局策略', value: 'GLOBAL' },
  { label: 'Agent级策略', value: 'AGENT' }
]

// 策略类型选项
const policyTypeOptions: { label: string; value: PolicyType; desc: string }[] = [
  { label: '访问控制', value: 'ACCESS_CONTROL', desc: '控制 Agent 对特定 API 的访问权限' },
  { label: '内容保护', value: 'CONTENT_PROTECTION', desc: '对敏感信息进行脱敏处理' },
  { label: '人工审批', value: 'APPROVAL', desc: '高风险操作需要人工审批后执行' },
  { label: '频率限制', value: 'RATE_LIMIT', desc: '限制 Agent 的请求频率' }
]

// HTTP 方法选项
const httpMethodOptions = [
  { label: '全部方法', value: 'ALL' },
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'PATCH', value: 'PATCH' }
]

// 敏感字段选项
const sensitiveFieldOptions = [
  { label: '手机号', value: 'phone' },
  { label: '身份证号', value: 'idCard' },
  { label: '银行卡号', value: 'bankCard' },
  { label: '邮箱', value: 'email' },
  { label: '地址', value: 'address' },
  { label: '姓名', value: 'name' }
]

// 审批触发条件 - 简单模式预设
const approvalSimpleTypes = [
  { label: '金额超过阈值', value: 'amount_threshold', desc: '当请求中的金额字段超过设定值时触发' },
  { label: 'URL 路径匹配', value: 'url_match', desc: '当请求 URL 匹配指定模式时触发' }
]

/**
 * 根据策略类型获取对应的动作
 */
const computedAction = computed<PolicyAction>(() => {
  switch (formData.value.type) {
    case 'ACCESS_CONTROL':
      if (formData.value.accessControl.mode === 'advanced') {
        // 高级模式从 JSON 中读取 action
        try {
          const conditions = JSON.parse(formData.value.accessControl.customConditions || '{}')
          return conditions.action || 'DENY'
        } catch {
          return 'DENY'
        }
      }
      return formData.value.accessControl.action
    case 'CONTENT_PROTECTION':
      return 'MASK'
    case 'APPROVAL':
      return 'APPROVAL'
    case 'RATE_LIMIT':
      return 'RATE_LIMIT'
    default:
      return 'DENY'
  }
})

/**
 * 根据策略类型生成条件 JSON（包含 action）
 */
function buildConditionsJson(): string {
  const type = formData.value.type
  let conditions: Record<string, unknown> = {}

  switch (type) {
    case 'ACCESS_CONTROL':
      if (formData.value.accessControl.mode === 'advanced') {
        try {
          conditions = JSON.parse(formData.value.accessControl.customConditions || '{}')
        } catch {
          conditions = {}
        }
      } else {
        conditions = {
          urlPattern: formData.value.accessControl.urlPattern,
          method: formData.value.accessControl.method,
          action: formData.value.accessControl.action
        }
      }
      break
    case 'CONTENT_PROTECTION':
      if (formData.value.contentProtection.mode === 'advanced') {
        try {
          conditions = JSON.parse(formData.value.contentProtection.customConditions || '{}')
        } catch {
          conditions = {}
        }
      } else {
        conditions = {
          sensitiveFields: formData.value.contentProtection.sensitiveFields,
          sensitiveKeywords: formData.value.contentProtection.sensitiveKeywords
            .split(',')
            .map(s => s.trim())
            .filter(s => s),
          action: 'MASK'
        }
      }
      break
    case 'APPROVAL':
      if (formData.value.approval.mode === 'advanced') {
        try {
          conditions = JSON.parse(formData.value.approval.customConditions || '{}')
        } catch {
          conditions = {}
        }
      } else if (formData.value.approval.simpleType === 'amount_threshold') {
        conditions = {
          bodyConditions: [{
            field: formData.value.approval.amountField,
            operator: 'gt',
            value: formData.value.approval.amountThreshold
          }],
          action: 'APPROVAL'
        }
      } else if (formData.value.approval.simpleType === 'url_match') {
        conditions = {
          urlPattern: formData.value.approval.urlPattern,
          action: 'APPROVAL'
        }
      }
      break
    case 'RATE_LIMIT':
      if (formData.value.rateLimit.mode === 'advanced') {
        try {
          conditions = JSON.parse(formData.value.rateLimit.customConditions || '{}')
        } catch {
          conditions = {}
        }
      } else {
        conditions = {
          urlPattern: formData.value.rateLimit.urlPattern,
          method: formData.value.rateLimit.method,
          windowSeconds: formData.value.rateLimit.windowSeconds,
          maxRequests: formData.value.rateLimit.maxRequests,
          action: 'RATE_LIMIT'
        }
      }
      break
  }

  // 确保高级模式下也有 action
  if (!conditions.action) {
    conditions.action = computedAction.value
  }

  return JSON.stringify(conditions)
}

/**
 * 从条件 JSON 解析到表单
 */
function parseConditionsJson(type: PolicyType, conditionsStr: string) {
  if (!conditionsStr) return

  try {
    const conditions = JSON.parse(conditionsStr)
    
    switch (type) {
      case 'ACCESS_CONTROL':
        // 从 JSON 中读取 action
        if (conditions.action) {
          formData.value.accessControl.action = conditions.action as 'ALLOW' | 'DENY'
        }
        // 判断是否为高级模式（有 bodyConditions 或 headerConditions）
        if (conditions.bodyConditions || conditions.headerConditions) {
          formData.value.accessControl.mode = 'advanced'
          formData.value.accessControl.customConditions = JSON.stringify(conditions, null, 2)
        } else {
          formData.value.accessControl.mode = 'simple'
          formData.value.accessControl.urlPattern = conditions.urlPattern || ''
          formData.value.accessControl.method = conditions.method || 'ALL'
        }
        break
      case 'CONTENT_PROTECTION':
        // 判断是否为高级模式（有非标准字段）
        const standardFields = ['sensitiveFields', 'sensitiveKeywords', 'action']
        const hasAdvancedFields = Object.keys(conditions).some(k => !standardFields.includes(k))
        if (hasAdvancedFields) {
          formData.value.contentProtection.mode = 'advanced'
          formData.value.contentProtection.customConditions = JSON.stringify(conditions, null, 2)
        } else {
          formData.value.contentProtection.mode = 'simple'
          formData.value.contentProtection.sensitiveFields = conditions.sensitiveFields || []
          formData.value.contentProtection.sensitiveKeywords = 
            Array.isArray(conditions.sensitiveKeywords) 
              ? conditions.sensitiveKeywords.join(', ') 
              : ''
        }
        break
      case 'APPROVAL':
        // 判断是否为高级模式（排除 action 字段判断）
        const approvalKeys = Object.keys(conditions).filter(k => k !== 'action')
        if (approvalKeys.length === 1 && conditions.bodyConditions?.length === 1 && conditions.bodyConditions[0].operator === 'gt') {
          formData.value.approval.mode = 'simple'
          formData.value.approval.simpleType = 'amount_threshold'
          formData.value.approval.amountField = conditions.bodyConditions[0].field || 'amount'
          formData.value.approval.amountThreshold = conditions.bodyConditions[0].value || 10000
        } else if (approvalKeys.length === 1 && conditions.urlPattern) {
          formData.value.approval.mode = 'simple'
          formData.value.approval.simpleType = 'url_match'
          formData.value.approval.urlPattern = conditions.urlPattern || ''
        } else {
          formData.value.approval.mode = 'advanced'
          formData.value.approval.customConditions = JSON.stringify(conditions, null, 2)
        }
        break
      case 'RATE_LIMIT':
        // 判断是否为高级模式（有 bodyConditions 或 headerConditions）
        if (conditions.bodyConditions || conditions.headerConditions) {
          formData.value.rateLimit.mode = 'advanced'
          formData.value.rateLimit.customConditions = JSON.stringify(conditions, null, 2)
        } else {
          formData.value.rateLimit.mode = 'simple'
          formData.value.rateLimit.urlPattern = conditions.urlPattern || ''
          formData.value.rateLimit.method = conditions.method || 'ALL'
          formData.value.rateLimit.windowSeconds = conditions.windowSeconds || 60
          formData.value.rateLimit.maxRequests = conditions.maxRequests || 100
        }
        break
    }
  } catch (e) {
    console.warn('解析条件 JSON 失败:', e)
  }
}

/**
 * 获取策略类型显示信息
 */
function getPolicyTypeInfo(type: PolicyType) {
  return policyTypeMap[type] || { label: type, color: 'default', desc: '' }
}

/**
 * 获取策略动作显示信息
 */
function getPolicyActionInfo(action: PolicyAction) {
  return policyActionMap[action] || { label: action, color: 'default' }
}

/**
 * 获取策略作用域显示信息
 */
function getPolicyScopeInfo(scope: PolicyScope) {
  return policyScopeMap[scope] || { label: scope, color: 'default' }
}

/**
 * 格式化操作符显示
 */
function formatOperator(op: string): string {
  const opMap: Record<string, string> = {
    eq: '=', ne: '≠', gt: '>', lt: '<', gte: '≥', lte: '≤',
    contains: '包含', not_contains: '不包含', regex: '匹配'
  }
  return opMap[op] || op
}

/**
 * 格式化条件显示
 */
function formatConditions(type: PolicyType, conditionsStr: string): string {
  if (!conditionsStr) return '-'
  
  try {
    const conditions = JSON.parse(conditionsStr)
    const parts: string[] = []
    
    switch (type) {
      case 'ACCESS_CONTROL': {
        // 基础：方法 + URL
        const method = conditions.method || 'ALL'
        const url = conditions.urlPattern || '*'
        parts.push(`${method} ${url}`)
        
        // Header 条件
        if (conditions.headerConditions?.length) {
          const headerParts = conditions.headerConditions.map((h: any) => 
            `${h.field} ${formatOperator(h.operator)} ${h.value}`
          )
          parts.push(`Header: ${headerParts.join(', ')}`)
        }
        
        // Body 条件
        if (conditions.bodyConditions?.length) {
          const bodyParts = conditions.bodyConditions.map((b: any) => 
            `${b.field} ${formatOperator(b.operator)} ${b.value}`
          )
          parts.push(`Body: ${bodyParts.join(', ')}`)
        }
        
        return parts.join(' | ')
      }
      
      case 'CONTENT_PROTECTION': {
        // 敏感字段
        if (conditions.sensitiveFields?.length) {
          parts.push(`字段: ${conditions.sensitiveFields.join(', ')}`)
        }
        // 敏感关键词
        if (conditions.sensitiveKeywords?.length) {
          parts.push(`关键词: ${conditions.sensitiveKeywords.join(', ')}`)
        }
        // 脱敏规则
        if (conditions.maskRules?.length) {
          const ruleTypes = conditions.maskRules.map((r: any) => r.type || r.field).join(', ')
          parts.push(`规则: ${ruleTypes}`)
        }
        return parts.length ? parts.join(' | ') : '-'
      }
      
      case 'APPROVAL': {
        // URL 模式
        if (conditions.urlPattern) {
          parts.push(`URL: ${conditions.urlPattern}`)
        }
        // Body 条件
        if (conditions.bodyConditions?.length) {
          const bodyParts = conditions.bodyConditions.map((b: any) => 
            `${b.field} ${formatOperator(b.operator)} ${b.value}`
          )
          parts.push(bodyParts.join(', '))
        }
        // Header 条件
        if (conditions.headerConditions?.length) {
          const headerParts = conditions.headerConditions.map((h: any) => 
            `${h.field} ${formatOperator(h.operator)} ${h.value}`
          )
          parts.push(`Header: ${headerParts.join(', ')}`)
        }
        return parts.length ? parts.join(' | ') : '自定义条件'
      }
      
      case 'RATE_LIMIT': {
        // URL 和方法
        const method = conditions.method || 'ALL'
        const url = conditions.urlPattern || '*'
        parts.push(`${method} ${url}`)

        // 限流配置
        if (conditions.windowSeconds && conditions.maxRequests) {
          parts.push(`${conditions.windowSeconds}秒/${conditions.maxRequests}次`)
        }

        // Header 条件
        if (conditions.headerConditions?.length) {
          const headerParts = conditions.headerConditions.map((h: any) =>
            `${h.field} ${formatOperator(h.operator)} ${h.value}`
          )
          parts.push(`Header: ${headerParts.join(', ')}`)
        }

        // Body 条件
        if (conditions.bodyConditions?.length) {
          const bodyParts = conditions.bodyConditions.map((b: any) =>
            `${b.field} ${formatOperator(b.operator)} ${b.value}`
          )
          parts.push(`Body: ${bodyParts.join(', ')}`)
        }

        return parts.length ? parts.join(' | ') : '-'
      }
      
      default:
        return '-'
    }
  } catch {
    return '-'
  }
}

/**
 * 获取策略列表
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await policyApi.getPolicyList(queryParams.value)
    policies.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/**
 * 重置表单
 */
function resetForm() {
  formData.value = {
    name: '',
    description: '',
    type: 'ACCESS_CONTROL',
    priority: 0,
    scope: 'GLOBAL',
    accessControl: {
      mode: 'simple',
      urlPattern: '',
      method: 'ALL',
      action: 'DENY',
      customConditions: ''
    },
    contentProtection: {
      mode: 'simple',
      sensitiveFields: [],
      sensitiveKeywords: '',
      customConditions: ''
    },
    approval: {
      mode: 'simple',
      simpleType: 'amount_threshold',
      amountField: 'amount',
      amountThreshold: 10000,
      urlPattern: '',
      customConditions: ''
    },
    rateLimit: {
      mode: 'simple',
      urlPattern: '',
      method: 'ALL',
      windowSeconds: 60,
      maxRequests: 100,
      customConditions: ''
    }
  }
  isEditMode.value = false
  editingPolicyId.value = null
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
function handleOpenEdit(policy: Policy) {
  isEditMode.value = true
  editingPolicyId.value = policy.id
  
  // 基础信息
  formData.value.name = policy.name
  formData.value.description = policy.description || ''
  formData.value.type = policy.type
  formData.value.priority = policy.priority || 0
  formData.value.scope = policy.scope || 'GLOBAL'
  
  // 根据动作设置访问控制的 action
  if (policy.type === 'ACCESS_CONTROL') {
    formData.value.accessControl.action = policy.action as 'ALLOW' | 'DENY'
  }
  
  // 解析条件
  parseConditionsJson(policy.type, policy.conditions || '')
  
  dialogVisible.value = true
}

/**
 * 提交表单
 */
async function handleSubmit() {
  if (!formData.value.name?.trim()) {
    ElMessage.warning('请输入策略名称')
    return
  }
  
  // 验证必填项
  if (formData.value.type === 'ACCESS_CONTROL') {
    if (formData.value.accessControl.mode === 'simple' && !formData.value.accessControl.urlPattern) {
      ElMessage.warning('请输入 URL 匹配模式')
      return
    }
    if (formData.value.accessControl.mode === 'advanced') {
      if (!formData.value.accessControl.customConditions) {
        ElMessage.warning('请输入自定义条件 JSON')
        return
      }
      try {
        const conditions = JSON.parse(formData.value.accessControl.customConditions)
        if (!conditions.action || !['ALLOW', 'DENY'].includes(conditions.action)) {
          ElMessage.warning('请在 JSON 中指定 action 字段（ALLOW 或 DENY）')
          return
        }
      } catch {
        ElMessage.warning('自定义条件 JSON 格式无效')
        return
      }
    }
  }
  if (formData.value.type === 'CONTENT_PROTECTION') {
    if (formData.value.contentProtection.mode === 'advanced') {
      if (!formData.value.contentProtection.customConditions) {
        ElMessage.warning('请输入自定义条件 JSON')
        return
      }
      try {
        JSON.parse(formData.value.contentProtection.customConditions)
      } catch {
        ElMessage.warning('自定义条件 JSON 格式无效')
        return
      }
    }
  }
  if (formData.value.type === 'APPROVAL') {
    if (formData.value.approval.mode === 'advanced') {
      if (!formData.value.approval.customConditions) {
        ElMessage.warning('请输入自定义条件 JSON')
        return
      }
      try {
        JSON.parse(formData.value.approval.customConditions)
      } catch {
        ElMessage.warning('自定义条件 JSON 格式无效')
        return
      }
    } else {
      const { simpleType, amountThreshold, urlPattern } = formData.value.approval
      if (simpleType === 'amount_threshold' && (!amountThreshold || amountThreshold <= 0)) {
        ElMessage.warning('请输入有效的金额阈值')
        return
      }
      if (simpleType === 'url_match' && !urlPattern) {
        ElMessage.warning('请输入 URL 匹配模式')
        return
      }
    }
  }
  if (formData.value.type === 'RATE_LIMIT') {
    if (formData.value.rateLimit.mode === 'advanced') {
      if (!formData.value.rateLimit.customConditions) {
        ElMessage.warning('请输入自定义条件 JSON')
        return
      }
      try {
        JSON.parse(formData.value.rateLimit.customConditions)
      } catch {
        ElMessage.warning('自定义条件 JSON 格式无效')
        return
      }
    }
  }
  
  formLoading.value = true
  try {
    const submitData: PolicyCreateDTO = {
      name: formData.value.name,
      description: formData.value.description,
      type: formData.value.type,
      conditions: buildConditionsJson(),
      action: computedAction.value,
      priority: formData.value.priority,
      scope: formData.value.scope
    }
    
    if (isEditMode.value && editingPolicyId.value) {
      await policyApi.updatePolicy(editingPolicyId.value, submitData as PolicyUpdateDTO)
      ElMessage.success('更新成功')
    } else {
      await policyApi.createPolicy(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    resetForm()
    fetchData()
  } finally {
    formLoading.value = false
  }
}

/**
 * 删除策略
 */
async function handleDelete(policy: Policy) {
  try {
    await ElMessageBox.confirm(
      `确定要删除策略「${policy.name}」吗？删除后无法恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
    )
    await policyApi.deletePolicy(policy.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    // user cancelled or error handled by interceptor
  }
}

/**
 * 切换策略启用状态
 */
async function handleToggleEnabled(policy: Policy) {
  // 注意：el-switch 的 @change 事件触发时，policy.enabled 已经是切换后的新值
  const action = policy.enabled ? '启用' : '停用'
  try {
    if (policy.enabled) {
      await policyApi.enablePolicy(policy.id)
    } else {
      await policyApi.disablePolicy(policy.id)
    }
    ElMessage.success(`${action}成功`)
    fetchData()
  } catch (e) {
    // error handled by interceptor, revert switch state
    policy.enabled = !policy.enabled
  }
}

/**
 * 搜索
 */
function handleSearch() {
  queryParams.value.current = 1
  fetchData()
}

/**
 * 重置搜索
 */
function handleReset() {
  queryParams.value.keyword = ''
  queryParams.value.type = undefined
  queryParams.value.scope = undefined
  queryParams.value.sortBy = 'priority_desc'
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
 * 对话框关闭
 */
function handleDialogClose() {
  resetForm()
}

onMounted(() => {
  fetchData()
})
</script>


<template>
  <div class="policy-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>策略列表</span>
          <el-button type="primary" @click="handleOpenCreate">新建策略</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item>
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索策略名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="queryParams.type"
            placeholder="策略类型"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in policyTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="queryParams.scope"
            placeholder="作用域"
            clearable
            style="width: 130px"
          >
            <el-option
              v-for="item in policyScopeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="queryParams.sortBy"
            placeholder="排序方式"
            style="width: 150px"
          >
            <el-option label="优先级降序" value="priority_desc" />
            <el-option label="优先级升序" value="priority_asc" />
            <el-option label="修改时间降序" value="updated_desc" />
            <el-option label="修改时间升序" value="updated_asc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 策略表格 -->
      <el-table :data="policies" v-loading="loading" stripe>
        <el-table-column prop="name" label="策略名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getPolicyTypeInfo(row.type).color" size="small">
              {{ getPolicyTypeInfo(row.type).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scope" label="作用域" width="90">
          <template #default="{ row }">
            <el-tag :type="getPolicyScopeInfo(row.scope).color" size="small">
              {{ getPolicyScopeInfo(row.scope).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规则配置" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ formatConditions(row.type, row.conditions) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="动作" width="80">
          <template #default="{ row }">
            <el-tag :type="getPolicyActionInfo(row.action).color" size="small">
              {{ getPolicyActionInfo(row.action).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="70" align="center" />
        <el-table-column prop="enabled" label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="handleToggleEnabled(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="修改时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleOpenEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.current"
        :page-size="queryParams.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditMode ? '编辑策略' : '新建策略'"
      width="650px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" label-width="100px">
        <!-- 基础信息 -->
        <el-form-item label="策略名称" required>
          <el-input v-model="formData.name" placeholder="请输入策略名称" maxlength="100" />
        </el-form-item>
        
        <el-form-item label="策略类型" required>
          <el-select v-model="formData.type" style="width: 100%" :disabled="isEditMode">
            <el-option
              v-for="item in policyTypeOptions"
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
          <div class="type-hint">{{ getPolicyTypeInfo(formData.type).desc }}</div>
        </el-form-item>

        <el-form-item label="作用域" required>
          <el-radio-group v-model="formData.scope">
            <el-radio value="GLOBAL">
              <el-tag type="primary" size="small">全局</el-tag>
              <span class="radio-desc">对所有 Agent 生效</span>
            </el-radio>
            <el-radio value="AGENT">
              <el-tag type="success" size="small">Agent级</el-tag>
              <span class="radio-desc">仅对绑定的 Agent 生效</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 访问控制配置 -->
        <template v-if="formData.type === 'ACCESS_CONTROL'">
          <el-divider content-position="left">访问控制配置</el-divider>
          
          <el-form-item label="配置模式">
            <el-radio-group v-model="formData.accessControl.mode">
              <el-radio value="simple">简单模式</el-radio>
              <el-radio value="advanced">高级模式</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 简单模式 -->
          <template v-if="formData.accessControl.mode === 'simple'">
            <el-form-item label="URL 模式" required>
              <el-input 
                v-model="formData.accessControl.urlPattern" 
                placeholder="例如：/api/payment.* 或 /api/admin/*"
              />
              <div class="field-hint">支持正则表达式匹配</div>
            </el-form-item>
            <el-form-item label="HTTP 方法">
              <el-select v-model="formData.accessControl.method" style="width: 200px">
                <el-option
                  v-for="item in httpMethodOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </template>

          <!-- 高级模式 -->
          <template v-else>
            <el-form-item label="条件 JSON" required>
              <el-input
                v-model="formData.accessControl.customConditions"
                type="textarea"
                :rows="8"
                placeholder='示例：
{
  "urlPattern": "/api/admin/.*",
  "method": "DELETE",
  "action": "DENY",
  "headerConditions": [
    { "field": "X-Role", "operator": "ne", "value": "admin" }
  ]
}'
              />
            </el-form-item>
            <el-collapse>
              <el-collapse-item title="条件配置说明">
                <div class="help-content">
                  <p><strong>支持的条件字段：</strong></p>
                  <ul>
                    <li><code>urlPattern</code> - URL 正则匹配</li>
                    <li><code>method</code> - HTTP 方法（GET/POST/PUT/DELETE/PATCH）</li>
                    <li><code>action</code> - 执行动作（ALLOW/DENY）</li>
                    <li><code>bodyConditions</code> - 请求体字段条件数组</li>
                    <li><code>headerConditions</code> - 请求头条件数组</li>
                  </ul>
                  
                  <p><strong>bodyConditions / headerConditions 格式：</strong></p>
                  <pre class="code-block">[{ "field": "字段名", "operator": "运算符", "value": "值" }]</pre>
                  <p>字段名支持嵌套路径，如 <code>user.profile.name</code></p>
                  
                  <p><strong>支持的运算符：</strong></p>
                  <ul>
                    <li><code>eq</code> - 等于 | <code>ne</code> - 不等于</li>
                    <li><code>gt</code> - 大于 | <code>gte</code> - 大于等于</li>
                    <li><code>lt</code> - 小于 | <code>lte</code> - 小于等于</li>
                    <li><code>contains</code> - 包含 | <code>matches</code> - 正则匹配</li>
                    <li><code>in</code> - 在列表中 | <code>notIn</code> - 不在列表中</li>
                    <li><code>isNull</code> - 为空 | <code>isNotNull</code> - 不为空</li>
                  </ul>
                  
                  <p><strong>示例：</strong>拒绝非管理员的删除操作</p>
                  <pre class="code-block">{
  "urlPattern": "/api/.*",
  "method": "DELETE",
  "action": "DENY",
  "headerConditions": [
    { "field": "X-Role", "operator": "ne", "value": "admin" }
  ]
}</pre>
                  <p class="example-explain">
                    当请求同时满足以下条件时触发：URL 匹配 /api/ 开头、HTTP 方法为 DELETE、请求头 X-Role 不等于 admin
                  </p>
                </div>
              </el-collapse-item>
            </el-collapse>
          </template>

          <!-- 简单模式才显示执行动作选择 -->
          <el-form-item v-if="formData.accessControl.mode === 'simple'" label="执行动作" required>
            <el-radio-group v-model="formData.accessControl.action">
              <el-radio value="ALLOW">
                <el-tag type="success" size="small">允许</el-tag>
                <span class="radio-desc">匹配的请求将被放行</span>
              </el-radio>
              <el-radio value="DENY">
                <el-tag type="danger" size="small">拒绝</el-tag>
                <span class="radio-desc">匹配的请求将被拦截</span>
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- 内容保护配置 -->
        <template v-if="formData.type === 'CONTENT_PROTECTION'">
          <el-divider content-position="left">内容保护配置</el-divider>
          
          <el-form-item label="配置模式">
            <el-radio-group v-model="formData.contentProtection.mode">
              <el-radio value="simple">简单模式</el-radio>
              <el-radio value="advanced">高级模式</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 简单模式 -->
          <template v-if="formData.contentProtection.mode === 'simple'">
            <el-form-item label="敏感字段">
              <el-checkbox-group v-model="formData.contentProtection.sensitiveFields">
                <el-checkbox 
                  v-for="item in sensitiveFieldOptions" 
                  :key="item.value" 
                  :value="item.value"
                >
                  {{ item.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="敏感关键词">
              <el-input 
                v-model="formData.contentProtection.sensitiveKeywords" 
                placeholder="多个关键词用逗号分隔，例如：密码, 身份证, 银行卡"
              />
            </el-form-item>
          </template>

          <!-- 高级模式 -->
          <template v-else>
            <el-form-item label="条件 JSON" required>
              <el-input
                v-model="formData.contentProtection.customConditions"
                type="textarea"
                :rows="8"
                placeholder='示例：
{
  "sensitiveFields": ["phone", "idCard", "bankCard"],
  "sensitiveKeywords": ["密码", "身份证"],
  "urlPattern": "/api/user/.*",
  "maskRules": {
    "phone": { "start": 3, "end": 4, "char": "*" }
  }
}'
              />
            </el-form-item>
            <el-collapse>
              <el-collapse-item title="条件配置说明">
                <div class="help-content">
                  <p><strong>支持的配置字段：</strong></p>
                  <ul>
                    <li><code>sensitiveFields</code> - 需要脱敏的字段名数组</li>
                    <li><code>sensitiveKeywords</code> - 敏感关键词数组</li>
                    <li><code>urlPattern</code> - 仅对匹配的 URL 生效</li>
                    <li><code>maskRules</code> - 自定义脱敏规则</li>
                  </ul>
                  <p><strong>示例：</strong>自定义手机号脱敏规则</p>
                  <pre class="code-block">{
  "sensitiveFields": ["phone"],
  "maskRules": {
    "phone": { "start": 3, "end": 4, "char": "*" }
  }
}</pre>
                  <p class="example-explain">手机号保留前3位和后4位，中间用*替换：138****1234</p>
                </div>
              </el-collapse-item>
            </el-collapse>
          </template>

          <el-alert type="info" :closable="false" show-icon>
            匹配到敏感信息时，系统将自动进行脱敏处理（如：138****1234）
          </el-alert>
        </template>

        <!-- 人工审批配置 -->
        <template v-if="formData.type === 'APPROVAL'">
          <el-divider content-position="left">审批触发条件</el-divider>
          
          <el-form-item label="配置模式">
            <el-radio-group v-model="formData.approval.mode">
              <el-radio value="simple">简单模式</el-radio>
              <el-radio value="advanced">高级模式</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 简单模式 -->
          <template v-if="formData.approval.mode === 'simple'">
            <el-form-item label="触发类型" required>
              <el-select v-model="formData.approval.simpleType" style="width: 200px">
                <el-option 
                  v-for="item in approvalSimpleTypes" 
                  :key="item.value" 
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <div class="field-hint">{{ approvalSimpleTypes.find(t => t.value === formData.approval.simpleType)?.desc }}</div>
            </el-form-item>

            <!-- 金额阈值配置 -->
            <template v-if="formData.approval.simpleType === 'amount_threshold'">
              <el-form-item label="金额字段" required>
                <el-input 
                  v-model="formData.approval.amountField" 
                  placeholder="请求体中的金额字段名，如 amount 或 data.amount"
                  style="width: 300px"
                />
                <div class="field-hint">支持嵌套字段，如 payment.amount</div>
              </el-form-item>
              <el-form-item label="阈值" required>
                <el-input-number 
                  v-model="formData.approval.amountThreshold" 
                  :min="1" 
                  :max="99999999"
                  :precision="2"
                  style="width: 200px"
                />
                <div class="field-hint">当字段值大于此阈值时，需要人工审批</div>
              </el-form-item>
            </template>

            <!-- URL 匹配配置 -->
            <template v-if="formData.approval.simpleType === 'url_match'">
              <el-form-item label="URL 模式" required>
                <el-input 
                  v-model="formData.approval.urlPattern" 
                  placeholder="例如：/api/admin/.* 或 /api/payment/transfer"
                />
                <div class="field-hint">支持正则表达式，匹配的请求需要审批</div>
              </el-form-item>
            </template>
          </template>

          <!-- 高级模式 -->
          <template v-else>
            <el-form-item label="条件 JSON" required>
              <el-input
                v-model="formData.approval.customConditions"
                type="textarea"
                :rows="8"
                placeholder='示例：
{
  "urlPattern": "/api/transfer.*",
  "method": "POST",
  "bodyConditions": [
    { "field": "amount", "operator": "gt", "value": 10000 }
  ]
}'
              />
            </el-form-item>
            <el-collapse>
              <el-collapse-item title="条件配置说明">
                <div class="help-content">
                  <p><strong>支持的条件字段：</strong></p>
                  <ul>
                    <li><code>urlPattern</code> - URL 正则匹配</li>
                    <li><code>method</code> - HTTP 方法（GET/POST/PUT/DELETE/PATCH）</li>
                    <li><code>bodyConditions</code> - 请求体字段条件数组</li>
                    <li><code>headerConditions</code> - 请求头条件数组</li>
                  </ul>
                  
                  <p><strong>bodyConditions / headerConditions 格式：</strong></p>
                  <pre class="code-block">[{ "field": "字段名", "operator": "运算符", "value": "值" }]</pre>
                  <p>字段名支持嵌套路径，如 <code>user.profile.name</code></p>
                  
                  <p><strong>支持的运算符：</strong></p>
                  <ul>
                    <li><code>eq</code> - 等于 | <code>ne</code> - 不等于</li>
                    <li><code>gt</code> - 大于 | <code>gte</code> - 大于等于</li>
                    <li><code>lt</code> - 小于 | <code>lte</code> - 小于等于</li>
                    <li><code>contains</code> - 包含 | <code>matches</code> - 正则匹配</li>
                    <li><code>in</code> - 在列表中 | <code>notIn</code> - 不在列表中</li>
                    <li><code>isNull</code> - 为空 | <code>isNotNull</code> - 不为空</li>
                  </ul>
                  
                  <p><strong>完整示例：</strong></p>
                  <pre class="code-block">{
  "urlPattern": "/api/payment/.*",
  "method": "POST",
  "bodyConditions": [
    { "field": "amount", "operator": "gt", "value": 10000 },
    { "field": "type", "operator": "in", "value": ["transfer", "withdraw"] }
  ],
  "headerConditions": [
    { "field": "X-Risk-Level", "operator": "eq", "value": "high" }
  ]
}</pre>
                  <p class="example-explain">
                    <strong>示例含义：</strong>当请求同时满足以下所有条件时触发审批：<br/>
                    1. URL 匹配 <code>/api/payment/</code> 开头的路径<br/>
                    2. HTTP 方法为 POST<br/>
                    3. 请求体中 amount 字段大于 10000<br/>
                    4. 请求体中 type 字段为 transfer 或 withdraw<br/>
                    5. 请求头 X-Risk-Level 等于 high
                  </p>
                </div>
              </el-collapse-item>
            </el-collapse>
          </template>

          <el-alert type="warning" :closable="false" show-icon style="margin-top: 16px">
            满足条件的请求将暂停执行，等待管理员在审批中心进行审批
          </el-alert>
        </template>

        <!-- 频率限制配置 -->
        <template v-if="formData.type === 'RATE_LIMIT'">
          <el-divider content-position="left">频率限制配置</el-divider>
          
          <el-form-item label="配置模式">
            <el-radio-group v-model="formData.rateLimit.mode">
              <el-radio value="simple">简单模式</el-radio>
              <el-radio value="advanced">高级模式</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 简单模式 -->
          <template v-if="formData.rateLimit.mode === 'simple'">
            <el-form-item label="URL 模式">
              <el-input
                v-model="formData.rateLimit.urlPattern"
                placeholder="例如：/api/ai/.* 或 /api/llm/*"
              />
              <div class="field-hint">支持正则表达式匹配，留空表示匹配所有 URL</div>
            </el-form-item>
            <el-form-item label="HTTP 方法">
              <el-select v-model="formData.rateLimit.method" style="width: 200px">
                <el-option
                  v-for="item in httpMethodOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="时间窗口" required>
              <el-input-number
                v-model="formData.rateLimit.windowSeconds"
                :min="1"
                :max="86400"
              />
              <span class="unit-text">秒</span>
            </el-form-item>
            <el-form-item label="最大请求数" required>
              <el-input-number
                v-model="formData.rateLimit.maxRequests"
                :min="1"
                :max="100000"
              />
              <span class="unit-text">次</span>
            </el-form-item>
            <el-alert type="info" :closable="false" show-icon>
              匹配 {{ formData.rateLimit.method === 'ALL' ? '所有方法' : formData.rateLimit.method }} {{ formData.rateLimit.urlPattern || '所有URL' }} 的请求，在 {{ formData.rateLimit.windowSeconds }} 秒内最多允许 {{ formData.rateLimit.maxRequests }} 次
            </el-alert>
          </template>

          <!-- 高级模式 -->
          <template v-else>
            <el-form-item label="条件 JSON" required>
              <el-input
                v-model="formData.rateLimit.customConditions"
                type="textarea"
                :rows="8"
                placeholder='示例：
{
  "urlPattern": "/api/ai/.*",
  "method": "POST",
  "windowSeconds": 60,
  "maxRequests": 10,
  "headerConditions": [
    { "field": "X-Agent-Id", "operator": "eq", "value": "agent-001" }
  ]
}'
              />
            </el-form-item>
            <el-collapse>
              <el-collapse-item title="条件配置说明">
                <div class="help-content">
                  <p><strong>支持的配置字段：</strong></p>
                  <ul>
                    <li><code>urlPattern</code> - URL 正则匹配</li>
                    <li><code>method</code> - HTTP 方法（GET/POST/PUT/DELETE/PATCH）</li>
                    <li><code>windowSeconds</code> - 时间窗口（秒）</li>
                    <li><code>maxRequests</code> - 最大请求数</li>
                    <li><code>bodyConditions</code> - 请求体字段条件数组</li>
                    <li><code>headerConditions</code> - 请求头条件数组</li>
                  </ul>

                  <p><strong>bodyConditions / headerConditions 格式：</strong></p>
                  <pre class="code-block">[{ "field": "字段名", "operator": "运算符", "value": "值" }]</pre>

                  <p><strong>支持的运算符：</strong></p>
                  <ul>
                    <li><code>eq</code> - 等于 | <code>ne</code> - 不等于</li>
                    <li><code>gt</code> - 大于 | <code>gte</code> - 大于等于</li>
                    <li><code>lt</code> - 小于 | <code>lte</code> - 小于等于</li>
                    <li><code>contains</code> - 包含 | <code>matches</code> - 正则匹配</li>
                    <li><code>in</code> - 在列表中 | <code>notIn</code> - 不在列表中</li>
                  </ul>

                  <p><strong>示例：</strong>特定 Agent 每分钟最多调用 AI 接口 10 次</p>
                  <pre class="code-block">{
  "urlPattern": "/api/ai/.*",
  "method": "POST",
  "windowSeconds": 60,
  "maxRequests": 10,
  "headerConditions": [
    { "field": "X-Agent-Id", "operator": "eq", "value": "agent-001" }
  ]
}</pre>
                  <p class="example-explain">
                    当请求同时满足以下条件时触发限流：<br/>
                    1. URL 匹配 <code>/api/ai/</code> 开头的路径<br/>
                    2. HTTP 方法为 POST<br/>
                    3. 请求头 X-Agent-Id 等于 agent-001<br/>
                    该 Agent 在 60 秒内最多允许 10 次请求
                  </p>
                </div>
              </el-collapse-item>
            </el-collapse>
          </template>
        </template>

        <el-divider />

        <!-- 通用配置 -->
        <el-form-item label="优先级">
          <el-input-number v-model="formData.priority" :min="0" :max="9999" />
          <span class="field-hint" style="margin-left: 10px">数值越大优先级越高，优先匹配</span>
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="请输入策略描述（可选）"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">
          {{ isEditMode ? '保存' : '确定' }}
        </el-button>
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

.type-option {
  display: flex;
  flex-direction: column;
}

.type-desc {
  font-size: 12px;
  color: #909399;
}

.type-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.field-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.radio-desc {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.unit-text {
  margin-left: 8px;
  color: #606266;
}

.conditions-text {
  font-size: 13px;
  color: #606266;
}

.trigger-radio {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 12px;
  padding: 8px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  width: 100%;
}

.trigger-radio:hover {
  border-color: #409eff;
}

.trigger-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.help-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}

.help-content ul {
  margin: 8px 0;
  padding-left: 20px;
}

.help-content code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: monospace;
}

.code-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  overflow-x: auto;
  white-space: pre;
  margin: 8px 0;
}

.example-explain {
  background: #fdf6ec;
  border-left: 3px solid #e6a23c;
  padding: 12px;
  margin-top: 12px;
  border-radius: 0 4px 4px 0;
  line-height: 1.8;
}

.example-explain code {
  background: #fff;
  border: 1px solid #e6a23c;
}

:deep(.el-radio) {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

:deep(.el-alert) {
  margin-top: 10px;
}
</style>
