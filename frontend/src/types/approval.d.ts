/**
 * 审批相关类型定义
 *
 * @author zhuhx
 */

/** 审批状态 */
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED'

/** 审批请求信息 */
export interface Approval {
  id: string
  policyId: string
  policyName: string
  agentId: string
  agentName: string
  requestData: string
  status: ApprovalStatus
  approverId: string
  approvedAt: string
  remark: string
  expiresAt: string
  createdAt: string
}

/** 创建审批请求 */
export interface ApprovalCreateDTO {
  policyId: string
  agentId: string
  requestData: string
  expireMinutes?: number
}

/** 审批操作请求 */
export interface ApprovalActionDTO {
  remark?: string
}

/** 审批列表查询参数 */
export interface ApprovalListParams {
  current: number
  size: number
  status?: ApprovalStatus
}
