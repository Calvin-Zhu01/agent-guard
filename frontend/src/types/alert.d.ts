/**
 * 告警相关类型定义
 *
 * @author zhuhx
 */

/** 告警类型 */
export type AlertType = 'COST' | 'ERROR_RATE' | 'APPROVAL' | 'SYSTEM'

/** 通知渠道类型 */
export type NotificationChannelType = 'EMAIL' | 'WEBHOOK'

/** 告警发送状态 */
export type AlertStatus = 'SUCCESS' | 'FAILED'

/** 告警规则信息 */
export interface AlertRule {
  id: string
  name: string
  type: AlertType
  threshold: number
  channelType: NotificationChannelType
  channelConfig: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

/** 创建告警规则请求 */
export interface AlertRuleCreateDTO {
  name: string
  type: AlertType
  threshold?: number
  channelType: NotificationChannelType
  channelConfig: string
}

/** 更新告警规则请求 */
export interface AlertRuleUpdateDTO {
  name?: string
  type?: AlertType
  threshold?: number
  channelType?: NotificationChannelType
  channelConfig?: string
  enabled?: boolean
}

/** 告警历史信息 */
export interface AlertHistory {
  id: string
  ruleId: string
  type: AlertType
  title: string
  content: string
  recipient: string
  channelType: NotificationChannelType
  status: AlertStatus
  errorMessage: string
  createdAt: string
}

/** 告警规则列表查询参数 */
export interface AlertRuleListParams {
  current: number
  size: number
  keyword?: string
  type?: AlertType
  enabled?: boolean
}

/** 告警历史列表查询参数 */
export interface AlertHistoryListParams {
  current: number
  size: number
  type?: AlertType
  status?: AlertStatus
  startTime?: string
  endTime?: string
}
