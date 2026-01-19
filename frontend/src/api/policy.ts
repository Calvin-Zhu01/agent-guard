/**
 * 策略管理 API 请求封装
 *
 * @author zhuhx
 */
import request from '@/utils/request'
import type { Policy, PolicyCreateDTO, PolicyUpdateDTO, PolicyListParams } from '@/types/policy'
import type { PageResult } from '@/types/api'

/**
 * 获取策略列表
 */
export function getPolicyList(params: PolicyListParams): Promise<PageResult<Policy>> {
  return request.get('/policies', { params })
}

/**
 * 获取策略详情
 */
export function getPolicyById(id: string): Promise<Policy> {
  return request.get(`/policies/${id}`)
}

/**
 * 创建策略
 */
export function createPolicy(data: PolicyCreateDTO): Promise<Policy> {
  return request.post('/policies', data)
}

/**
 * 更新策略
 */
export function updatePolicy(id: string, data: PolicyUpdateDTO): Promise<Policy> {
  return request.put(`/policies/${id}`, data)
}

/**
 * 删除策略
 */
export function deletePolicy(id: string): Promise<void> {
  return request.delete(`/policies/${id}`)
}

/**
 * 启用策略
 */
export function enablePolicy(id: string): Promise<void> {
  return request.post(`/policies/${id}/enable`)
}

/**
 * 停用策略
 */
export function disablePolicy(id: string): Promise<void> {
  return request.post(`/policies/${id}/disable`)
}
