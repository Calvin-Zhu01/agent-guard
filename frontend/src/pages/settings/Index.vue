<script setup lang="ts">
/**
 * 系统设置页面
 *
 * @author zhuhx
 */
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getEmailSettings,
  updateEmailSettings,
  testEmailSettings,
  getWebhookSettings,
  updateWebhookSettings,
  getAlertSettings,
  updateAlertSettings,
  type EmailSettings,
  type WebhookSettings,
  type AlertSettings
} from '@/api/settings'

// 邮件配置
const emailForm = ref<EmailSettings>({
  enabled: false,
  smtpHost: '',
  smtpPort: 587,
  fromEmail: '',
  fromName: 'AgentGuard',
  username: '',
  password: '',
  sslEnabled: true,
  defaultRecipients: ''
})

// Webhook配置
const webhookForm = ref<WebhookSettings>({
  dingTalkEnabled: false,
  dingTalkWebhook: '',
  dingTalkSecret: '',
  weComEnabled: false,
  weComWebhook: '',
  customWebhookEnabled: false,
  customWebhookUrl: ''
})

// 告警配置
const alertForm = ref<AlertSettings>({
  costAlertEnabled: true,
  costThreshold: 85,
  errorRateAlertEnabled: true,
  errorRateThreshold: 10,
  errorRateWindow: 60,
  approvalReminderEnabled: true,
  approvalReminderMinutes: 30
})

const emailLoading = ref(false)
const webhookLoading = ref(false)
const alertLoading = ref(false)
const testingEmail = ref(false)

// 加载邮件配置
const loadEmailSettings = async () => {
  try {
    const data = await getEmailSettings()
    emailForm.value = data
  } catch (error) {
    console.error('加载邮件配置失败', error)
  }
}

// 保存邮件配置
const saveEmailSettings = async () => {
  emailLoading.value = true
  try {
    await updateEmailSettings(emailForm.value)
    ElMessage.success('邮件配置保存成功')
  } catch (error) {
    ElMessage.error('邮件配置保存失败')
  } finally {
    emailLoading.value = false
  }
}

// 测试邮件配置
const testEmail = async () => {
  testingEmail.value = true
  try {
    const success = await testEmailSettings(emailForm.value)
    if (success) {
      ElMessage.success('邮件配置测试成功')
    } else {
      ElMessage.error('邮件配置测试失败，请检查配置')
    }
  } catch (error) {
    ElMessage.error('邮件配置测试失败')
  } finally {
    testingEmail.value = false
  }
}

// 加载Webhook配置
const loadWebhookSettings = async () => {
  try {
    const data = await getWebhookSettings()
    webhookForm.value = data
  } catch (error) {
    console.error('加载Webhook配置失败', error)
  }
}

// 保存Webhook配置
const saveWebhookSettings = async () => {
  webhookLoading.value = true
  try {
    await updateWebhookSettings(webhookForm.value)
    ElMessage.success('Webhook配置保存成功')
  } catch (error) {
    ElMessage.error('Webhook配置保存失败')
  } finally {
    webhookLoading.value = false
  }
}

// 加载告警配置
const loadAlertSettings = async () => {
  try {
    const data = await getAlertSettings()
    alertForm.value = data
  } catch (error) {
    console.error('加载告警配置失败', error)
  }
}

// 保存告警配置
const saveAlertSettings = async () => {
  alertLoading.value = true
  try {
    await updateAlertSettings(alertForm.value)
    ElMessage.success('告警配置保存成功')
  } catch (error) {
    ElMessage.error('告警配置保存失败')
  } finally {
    alertLoading.value = false
  }
}

onMounted(() => {
  loadEmailSettings()
  loadWebhookSettings()
  loadAlertSettings()
})
</script>

<template>
  <div class="settings-page">
    <el-card>
      <template #header>
        <span>系统设置</span>
      </template>

      <el-tabs>
        <!-- 邮件通知配置 -->
        <el-tab-pane label="邮件通知">
          <el-form :model="emailForm" label-width="140px" style="max-width: 600px">
            <el-form-item label="启用邮件通知">
              <el-switch v-model="emailForm.enabled" />
            </el-form-item>

            <el-divider content-position="left">SMTP服务器配置</el-divider>

            <el-form-item label="SMTP服务器">
              <el-input v-model="emailForm.smtpHost" placeholder="例如: smtp.gmail.com" />
            </el-form-item>

            <el-form-item label="SMTP端口">
              <el-input-number v-model="emailForm.smtpPort" :min="1" :max="65535" />
            </el-form-item>

            <el-form-item label="启用SSL">
              <el-switch v-model="emailForm.sslEnabled" />
            </el-form-item>

            <el-form-item label="SMTP用户名">
              <el-input v-model="emailForm.username" placeholder="邮箱账号" />
            </el-form-item>

            <el-form-item label="SMTP密码">
              <el-input
                v-model="emailForm.password"
                type="password"
                placeholder="邮箱密码或授权码"
                show-password
              />
            </el-form-item>

            <el-divider content-position="left">发件人信息</el-divider>

            <el-form-item label="发件人邮箱">
              <el-input v-model="emailForm.fromEmail" placeholder="例如: noreply@agentguard.com" />
            </el-form-item>

            <el-form-item label="发件人名称">
              <el-input v-model="emailForm.fromName" placeholder="例如: AgentGuard" />
            </el-form-item>

            <el-form-item label="默认收件人">
              <el-input
                v-model="emailForm.defaultRecipients"
                placeholder="多个邮箱用逗号分隔"
              />
              <div style="color: #909399; font-size: 12px; margin-top: 5px">
                告警邮件将发送到这些邮箱地址
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="emailLoading" @click="saveEmailSettings">
                保存配置
              </el-button>
              <el-button :loading="testingEmail" @click="testEmail">测试连接</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Webhook通知配置 -->
        <el-tab-pane label="Webhook通知">
          <el-form :model="webhookForm" label-width="140px" style="max-width: 600px">
            <el-divider content-position="left">钉钉机器人</el-divider>

            <el-form-item label="启用钉钉通知">
              <el-switch v-model="webhookForm.dingTalkEnabled" />
            </el-form-item>

            <el-form-item label="Webhook地址">
              <el-input
                v-model="webhookForm.dingTalkWebhook"
                placeholder="https://oapi.dingtalk.com/robot/send?access_token=..."
              />
            </el-form-item>

            <el-form-item label="签名密钥">
              <el-input
                v-model="webhookForm.dingTalkSecret"
                type="password"
                placeholder="可选，用于签名验证"
                show-password
              />
            </el-form-item>

            <el-divider content-position="left">企业微信机器人</el-divider>

            <el-form-item label="启用企业微信通知">
              <el-switch v-model="webhookForm.weComEnabled" />
            </el-form-item>

            <el-form-item label="Webhook地址">
              <el-input
                v-model="webhookForm.weComWebhook"
                placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..."
              />
            </el-form-item>

            <el-divider content-position="left">自定义Webhook</el-divider>

            <el-form-item label="启用自定义Webhook">
              <el-switch v-model="webhookForm.customWebhookEnabled" />
            </el-form-item>

            <el-form-item label="Webhook地址">
              <el-input
                v-model="webhookForm.customWebhookUrl"
                placeholder="https://your-webhook-url.com"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="webhookLoading" @click="saveWebhookSettings">
                保存配置
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 告警配置 -->
        <el-tab-pane label="告警配置">
          <el-form :model="alertForm" label-width="160px" style="max-width: 600px">
            <el-divider content-position="left">成本告警</el-divider>

            <el-form-item label="启用成本告警">
              <el-switch v-model="alertForm.costAlertEnabled" />
            </el-form-item>

            <el-form-item label="成本告警阈值">
              <el-slider
                v-model="alertForm.costThreshold"
                :min="50"
                :max="100"
                :step="5"
                show-input
              />
              <div style="color: #909399; font-size: 12px; margin-top: 5px">
                当预算使用达到此百分比时触发告警
              </div>
            </el-form-item>

            <el-divider content-position="left">错误率告警</el-divider>

            <el-form-item label="启用错误率告警">
              <el-switch v-model="alertForm.errorRateAlertEnabled" />
            </el-form-item>

            <el-form-item label="错误率阈值">
              <el-slider
                v-model="alertForm.errorRateThreshold"
                :min="1"
                :max="50"
                :step="1"
                show-input
              />
              <div style="color: #909399; font-size: 12px; margin-top: 5px">
                当错误率超过此百分比时触发告警
              </div>
            </el-form-item>

            <el-form-item label="统计时间窗口">
              <el-input-number v-model="alertForm.errorRateWindow" :min="10" :max="1440" :step="10" />
              <span style="margin-left: 10px">分钟</span>
              <div style="color: #909399; font-size: 12px; margin-top: 5px">
                在此时间窗口内统计错误率
              </div>
            </el-form-item>

            <el-divider content-position="left">审批提醒</el-divider>

            <el-form-item label="启用审批提醒">
              <el-switch v-model="alertForm.approvalReminderEnabled" />
            </el-form-item>

            <el-form-item label="提醒提前时间">
              <el-input-number
                v-model="alertForm.approvalReminderMinutes"
                :min="5"
                :max="1440"
                :step="5"
              />
              <span style="margin-left: 10px">分钟</span>
              <div style="color: #909399; font-size: 12px; margin-top: 5px">
                在审批即将过期前多久发送提醒
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="alertLoading" @click="saveAlertSettings">
                保存配置
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.settings-page {
  padding: 20px;
}
</style>
