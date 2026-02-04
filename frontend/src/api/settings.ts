import request from '@/utils/request'

/**
 * 系统设置 API
 */

// ==================== 邮件通知配置 ====================

export interface EmailSettings {
  enabled: boolean
  smtpHost: string
  smtpPort: number
  fromEmail: string
  fromName: string
  username: string
  password: string
  sslEnabled: boolean
  defaultRecipients: string
}

export function getEmailSettings() {
  return request.get<EmailSettings>('/settings/email')
}

export function updateEmailSettings(data: EmailSettings) {
  return request.put('/settings/email', data)
}

export function testEmailSettings(data: EmailSettings) {
  return request.post<boolean>('/settings/email/test', data)
}

// ==================== Webhook通知配置 ====================

export interface WebhookSettings {
  dingTalkEnabled: boolean
  dingTalkWebhook: string
  dingTalkSecret: string
  weComEnabled: boolean
  weComWebhook: string
  customWebhookEnabled: boolean
  customWebhookUrl: string
  customWebhookSecret: string
}

export function getWebhookSettings() {
  return request.get<WebhookSettings>('/settings/webhook')
}

export function updateWebhookSettings(data: WebhookSettings) {
  return request.put('/settings/webhook', data)
}

// ==================== 告警配置 ====================

export interface AlertSettings {
  costAlertEnabled: boolean
  costThreshold: number
  costAlertCooldownMinutes: number
  errorRateAlertEnabled: boolean
  errorRateThreshold: number
  errorRateWindow: number
  errorRateAlertCooldownMinutes: number
  approvalReminderEnabled: boolean
  approvalReminderMinutes: number
  approvalReminderCooldownMinutes: number
}

export function getAlertSettings() {
  return request.get<AlertSettings>('/settings/alert')
}

export function updateAlertSettings(data: AlertSettings) {
  return request.put('/settings/alert', data)
}
