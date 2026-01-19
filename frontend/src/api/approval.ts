/**
 * 审批管理 API 请求封装
 *
 * @author zhuhx
 */
import request from '@/utils/request'
import type { Approval, ApprovalActionDTO, ApprovalListParams } from '@/types/approval'
import type { PageResult } from '@/types/api'

/**
 * 获取审批列表
 */
export function getApprovalList(params: ApprovalListParams): Promise<PageResult<Approval>> {
  return request.get('/approvals', { params })
}

/**
 * 获取审批详情
 */
export function getApprovalById(id: string): Promise<Approval> {
  return request.get(`/approvals/${id}`)
}

/**
 * 批准审批请求
 */
export function approveApproval(id: string, data?: ApprovalActionDTO): Promise<Approval> {
  return request.post(`/approvals/${id}/approve`, data)
}

/**
 * 拒绝审批请求
 */
export function rejectApproval(id: string, data?: ApprovalActionDTO): Promise<Approval> {
  return request.post(`/approvals/${id}/reject`, data)
}
