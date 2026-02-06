<script setup lang="ts">
/**
 * 系统设置页面
 *
 * @author zhuhx
 */
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Message } from '@element-plus/icons-vue'
import CodeBlock from '@/components/CodeBlock.vue'
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
  customWebhookUrl: '',
  customWebhookSecret: ''
})

// 告警配置
const alertForm = ref<AlertSettings>({
  costAlertEnabled: true,
  costThreshold: 85,
  costAlertCooldownMinutes: 60,
  errorRateAlertEnabled: true,
  errorRateThreshold: 10,
  errorRateWindow: 60,
  errorRateAlertCooldownMinutes: 30,
  approvalReminderEnabled: true,
  approvalReminderMinutes: 30,
  approvalExpirationMinutes: 60,
  approvalReminderCooldownMinutes: 10
})

const emailLoading = ref(false)
const webhookLoading = ref(false)
const alertLoading = ref(false)
const testingEmail = ref(false)

// Tab 状态
const alertActiveTab = ref('cost')
const notifyActiveTab = ref('email')

// Webhook 请求示例代码
const webhookExampleCode = `{
  "title": "告警标题",
  "content": "告警内容详情",
  "timestamp": 1739950503
}`

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

// 监听告警配置标签切换
watch(alertActiveTab, () => {
  loadAlertSettings()
})

// 监听通知配置标签切换
watch(notifyActiveTab, (newTab) => {
  if (newTab === 'email') {
    loadEmailSettings()
  } else if (newTab === 'webhook') {
    loadWebhookSettings()
  }
})

// 页面加载时，只加载当前激活标签的数据
onMounted(() => {
  // 加载告警配置（默认显示成本告警标签）
  loadAlertSettings()
  // 加载邮件配置（默认显示邮件通知标签）
  loadEmailSettings()
})
</script>

<template>
  <div class="settings-page">
    <el-row :gutter="20" align="top">
      <!-- 左侧：告警配置 -->
      <el-col :span="12">
        <el-card class="settings-card">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Bell /></el-icon>
              <div class="header-text">
                <span class="header-title">告警配置</span>
                <span class="header-desc">成本、错误率和审批提醒设置</span>
              </div>
            </div>
          </template>

          <el-tabs v-model="alertActiveTab">
            <!-- 成本告警 -->
            <el-tab-pane label="成本告警" name="cost">
              <el-form :model="alertForm" label-width="140px">
                <el-form-item label="启用成本告警">
                  <el-switch v-model="alertForm.costAlertEnabled" />
                </el-form-item>

                <el-form-item label="成本告警阈值">
                  <el-slider
                    v-model="alertForm.costThreshold"
                    :min="50"
                    :max="100"
                    :step="5"
                    :disabled="!alertForm.costAlertEnabled"
                    show-input
                  />
                  <div class="form-tip">当预算使用达到此百分比时触发告警</div>
                </el-form-item>

                <el-form-item label="通知冷却时间">
                  <el-input-number
                    v-model="alertForm.costAlertCooldownMinutes"
                    :min="10"
                    :max="1440"
                    :step="10"
                    :disabled="!alertForm.costAlertEnabled"
                  />
                  <span class="unit-text">分钟</span>
                  <div class="form-tip">避免短时间内重复发送告警</div>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 错误率告警 -->
            <el-tab-pane label="错误率告警" name="errorRate">
              <el-form :model="alertForm" label-width="140px">
                <el-form-item label="启用错误率告警">
                  <el-switch v-model="alertForm.errorRateAlertEnabled" />
                </el-form-item>

                <el-form-item label="错误率阈值">
                  <el-slider
                    v-model="alertForm.errorRateThreshold"
                    :min="1"
                    :max="50"
                    :step="1"
                    :disabled="!alertForm.errorRateAlertEnabled"
                    show-input
                  />
                  <div class="form-tip">当错误率超过此百分比时触发告警</div>
                </el-form-item>

                <el-form-item label="统计时间窗口">
                  <el-input-number
                    v-model="alertForm.errorRateWindow"
                    :min="10"
                    :max="1440"
                    :step="10"
                    :disabled="!alertForm.errorRateAlertEnabled"
                  />
                  <span class="unit-text">分钟</span>
                  <div class="form-tip">在此时间窗口内统计错误率</div>
                </el-form-item>

                <el-form-item label="通知冷却时间">
                  <el-input-number
                    v-model="alertForm.errorRateAlertCooldownMinutes"
                    :min="5"
                    :max="1440"
                    :step="5"
                    :disabled="!alertForm.errorRateAlertEnabled"
                  />
                  <span class="unit-text">分钟</span>
                  <div class="form-tip">避免短时间内重复发送告警</div>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 审批提醒 -->
            <el-tab-pane label="审批提醒" name="approval">
              <el-form :model="alertForm" label-width="140px">
                <el-form-item label="启用审批提醒">
                  <el-switch v-model="alertForm.approvalReminderEnabled" />
                </el-form-item>

                <el-form-item label="审批记录过期时间">
                  <el-input-number
                    v-model="alertForm.approvalExpirationMinutes"
                    :min="5"
                    :max="1440"
                    :step="5"
                    :disabled="!alertForm.approvalReminderEnabled"
                  />
                  <span class="unit-text">分钟</span>
                  <div class="form-tip">新建审批记录的默认过期时间</div>
                </el-form-item>

                <el-form-item label="提醒提前时间">
                  <el-input-number
                    v-model="alertForm.approvalReminderMinutes"
                    :min="5"
                    :max="1440"
                    :step="5"
                    :disabled="!alertForm.approvalReminderEnabled"
                  />
                  <span class="unit-text">分钟</span>
                  <div class="form-tip">在审批即将过期前多久发送提醒</div>
                </el-form-item>

                <el-form-item label="通知冷却时间">
                  <el-input-number
                    v-model="alertForm.approvalReminderCooldownMinutes"
                    :min="5"
                    :max="1440"
                    :step="5"
                    :disabled="!alertForm.approvalReminderEnabled"
                  />
                  <span class="unit-text">分钟</span>
                  <div class="form-tip">避免短时间内重复发送提醒</div>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <div class="card-footer">
            <el-button type="primary" :loading="alertLoading" @click="saveAlertSettings">
              保存配置
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：消息通知 -->
      <el-col :span="12">
        <el-card class="settings-card">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Message /></el-icon>
              <div class="header-text">
                <span class="header-title">消息通知</span>
                <span class="header-desc">邮件和Webhook通知渠道配置</span>
              </div>
            </div>
          </template>

          <el-tabs v-model="notifyActiveTab">
            <!-- 邮件通知配置 -->
            <el-tab-pane label="邮件通知" name="email">
              <el-form :model="emailForm" label-width="120px">
                <el-form-item label="启用邮件通知">
                  <el-switch v-model="emailForm.enabled" />
                </el-form-item>

                <el-divider content-position="left">SMTP服务器配置</el-divider>

                <el-form-item label="SMTP服务器">
                  <el-input
                    v-model="emailForm.smtpHost"
                    :disabled="!emailForm.enabled"
                    placeholder="例如: smtp.gmail.com"
                  />
                </el-form-item>

                <el-form-item label="SMTP端口">
                  <div class="port-ssl-group">
                    <el-input-number
                      v-model="emailForm.smtpPort"
                      :min="1"
                      :max="65535"
                      :disabled="!emailForm.enabled"
                    />
                    <div class="ssl-switch">
                      <span class="ssl-label">启用SSL</span>
                      <el-switch v-model="emailForm.sslEnabled" :disabled="!emailForm.enabled" />
                    </div>
                  </div>
                </el-form-item>

                <el-form-item label="SMTP用户名">
                  <el-input
                    v-model="emailForm.username"
                    :disabled="!emailForm.enabled"
                    placeholder="邮箱账号"
                  />
                </el-form-item>

                <el-form-item label="SMTP密码">
                  <el-input
                    v-model="emailForm.password"
                    :disabled="!emailForm.enabled"
                    type="password"
                    placeholder="邮箱密码或授权码"
                    show-password
                  />
                </el-form-item>

                <el-divider content-position="left">收发人信息</el-divider>

                <el-form-item label="发件人邮箱">
                  <el-input
                    v-model="emailForm.fromEmail"
                    :disabled="!emailForm.enabled"
                    placeholder="例如: noreply@agentguard.com"
                  />
                </el-form-item>

                <el-form-item label="发件人名称">
                  <el-input
                    v-model="emailForm.fromName"
                    :disabled="!emailForm.enabled"
                    placeholder="例如: AgentGuard"
                  />
                </el-form-item>

                <el-form-item label="收件人邮箱">
                  <el-input
                    v-model="emailForm.defaultRecipients"
                    :disabled="!emailForm.enabled"
                    placeholder="多个邮箱用逗号分隔"
                  />
                  <div class="form-tip">告警邮件将发送到这些邮箱地址</div>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- Webhook通知配置 -->
            <el-tab-pane label="Webhook通知" name="webhook">
              <el-form :model="webhookForm" label-width="120px">
                <el-collapse accordion>
                  <!-- 钉钉 -->
                  <el-collapse-item name="dingtalk">
                    <template #title>
                      <div class="collapse-title">
                        <span>钉钉</span>
                        <el-switch
                          v-model="webhookForm.dingTalkEnabled"
                          @click.stop
                        />
                      </div>
                    </template>

                    <el-form-item label="Webhook地址">
                      <el-input
                        v-model="webhookForm.dingTalkWebhook"
                        :disabled="!webhookForm.dingTalkEnabled"
                        placeholder="https://oapi.dingtalk.com/robot/send?access_token=..."
                      />
                    </el-form-item>

                    <el-form-item label="签名密钥">
                      <el-input
                        v-model="webhookForm.dingTalkSecret"
                        :disabled="!webhookForm.dingTalkEnabled"
                        type="password"
                        placeholder="可选，用于签名验证"
                        show-password
                      />
                      <div class="form-tip">如果钉钉机器人启用了签名验证，请填写密钥</div>
                    </el-form-item>
                  </el-collapse-item>

                  <!-- 企业微信 -->
                  <el-collapse-item name="wecom">
                    <template #title>
                      <div class="collapse-title">
                        <span>企业微信</span>
                        <el-switch
                          v-model="webhookForm.weComEnabled"
                          @click.stop
                        />
                      </div>
                    </template>

                    <el-form-item label="Webhook地址">
                      <el-input
                        v-model="webhookForm.weComWebhook"
                        :disabled="!webhookForm.weComEnabled"
                        placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..."
                      />
                    </el-form-item>
                  </el-collapse-item>

                  <!-- 自定义Webhook -->
                  <el-collapse-item name="custom">
                    <template #title>
                      <div class="collapse-title">
                        <span>自定义Webhook</span>
                        <el-switch
                          v-model="webhookForm.customWebhookEnabled"
                          @click.stop
                        />
                      </div>
                    </template>

                    <el-form-item label="Webhook地址">
                      <el-input
                        v-model="webhookForm.customWebhookUrl"
                        :disabled="!webhookForm.customWebhookEnabled"
                        placeholder="https://your-webhook-url.com"
                      />
                      <div class="form-tip">
                        只支持HTTPS，系统将以POST方式发送通知
                      </div>
                    </el-form-item>

                    <el-form-item label="接口凭证">
                      <el-input
                        v-model="webhookForm.customWebhookSecret"
                        :disabled="!webhookForm.customWebhookEnabled"
                        type="password"
                        placeholder="可选，用于验证请求合法性"
                        show-password
                      />
                      <div class="form-tip">
                        密钥将以Bearer方式添加到请求头中
                      </div>
                    </el-form-item>

                    <el-divider content-position="left">Webhook请求结构说明</el-divider>

                    <CodeBlock :code="webhookExampleCode" language="json" />

                    <div class="webhook-desc">
                      <p><strong>title:</strong> 告警标题</p>
                      <p><strong>content:</strong> 告警内容，支持换行</p>
                      <p><strong>timestamp:</strong> Unix时间戳</p>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <div class="card-footer">
            <!-- 邮件通知按钮 -->
            <template v-if="notifyActiveTab === 'email'">
              <el-button type="primary" :loading="emailLoading" @click="saveEmailSettings">
                保存配置
              </el-button>
              <el-button
                :loading="testingEmail"
                :disabled="!emailForm.enabled"
                @click="testEmail"
              >
                测试连接
              </el-button>
            </template>
            <!-- Webhook通知按钮 -->
            <template v-else-if="notifyActiveTab === 'webhook'">
              <el-button type="primary" :loading="webhookLoading" @click="saveWebhookSettings">
                保存配置
              </el-button>
            </template>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.settings-card {
  border-radius: 12px;
  overflow: hidden;
}

.settings-card :deep(.el-card__header) {
  border-radius: 12px 12px 0 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 24px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 8px;
  border-radius: 8px;
}

.header-text {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.card-footer {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
  margin-left: 10px;
}

.unit-text {
  margin-left: 10px;
  color: #606266;
}

.collapse-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 10px;
}

.webhook-desc {
  color: #606266;
  font-size: 12px;
  line-height: 1.8;
}

.webhook-desc p {
  margin: 5px 0;
}

.port-ssl-group {
  display: flex;
  align-items: center;
  gap: 20px;
}

.ssl-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ssl-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

:deep(.el-divider__text) {
  font-size: 15px;
  font-weight: 600;
}
</style>
