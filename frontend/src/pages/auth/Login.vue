<script setup lang="ts">
/**
 * 登录页面
 *
 * @author zhuhx
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref<FormInstance>()
const rememberMe = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为 2-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const response = await login({
      username: form.username,
      password: form.password
    })

    // 保存 Token 和用户信息
    userStore.setToken(response.token)
    userStore.setUserInfo(response.user)

    ElMessage.success('登录成功')

    // 跳转到仪表盘或之前访问的页面
    const redirect = router.currentRoute.value.query.redirect as string
    router.push(redirect || '/dashboard')
  } catch (error: any) {
    // 错误已在请求拦截器中处理
  } finally {
    loading.value = false
  }
}

// 回车键登录
function handleKeyup(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    handleLogin()
  }
}
</script>

<template>
  <div class="login-container">
    <!-- 左侧品牌区域 -->
    <div class="brand-section">
      <div class="brand-content">
        <div class="logo-wrapper">
          <img src="/logo-dark.svg" alt="AgentGuard Logo" class="logo-image" />
        </div>
        <h1 class="brand-title">AgentGuard</h1>
        <p class="brand-subtitle">AI Agent 治理与监控平台</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon :size="20"><Check /></el-icon>
            <span>可控访问管理</span>
          </div>
          <div class="feature-item">
            <el-icon :size="20"><Check /></el-icon>
            <span>全链路审计日志</span>
          </div>
          <div class="feature-item">
            <el-icon :size="20"><Check /></el-icon>
            <span>成本追踪分析</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单区域 -->
    <div class="form-section">
      <div class="login-box">
        <div class="login-header">
          <h2 class="login-title">欢迎登录</h2>
          <p class="login-desc">请输入您的账号信息</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @submit.prevent="handleLogin"
          @keyup="handleKeyup"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <el-link type="primary" :underline="false">忘记密码?</el-link>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-button"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <el-divider>
            <span class="divider-text">测试账号</span>
          </el-divider>
          <div class="test-account">
            <el-tag type="info" size="small">admin / admin123</el-tag>
          </div>
        </div>
      </div>

      <div class="copyright">
        <span>© 2024 AgentGuard. All rights reserved.</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  overflow: hidden;
}

/* 左侧品牌区域 */
.brand-section {
  flex: 1;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 50%, #7e8ba3 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  position: relative;
  overflow: hidden;
}

.brand-section::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: moveBackground 20s linear infinite;
}

@keyframes moveBackground {
  0% {
    transform: translate(0, 0);
  }
  100% {
    transform: translate(50px, 50px);
  }
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
}

.logo-wrapper {
  margin-bottom: 24px;
  animation: fadeInDown 0.8s ease-out;
}

.logo-image {
  width: 120px;
  height: 120px;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.2));
}

.brand-title {
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 16px;
  letter-spacing: 2px;
  animation: fadeInDown 0.8s ease-out 0.2s both;
}

.brand-subtitle {
  font-size: 20px;
  margin: 0 0 48px;
  opacity: 0.9;
  font-weight: 300;
  animation: fadeInDown 0.8s ease-out 0.4s both;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-start;
  max-width: 300px;
  margin: 0 auto;
  animation: fadeInUp 0.8s ease-out 0.6s both;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  opacity: 0.95;
}

/* 右侧表单区域 */
.form-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 60px;
  position: relative;
}

.login-box {
  width: 100%;
  max-width: 440px;
  padding: 48px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  animation: fadeInRight 0.8s ease-out;
}

.login-header {
  margin-bottom: 32px;
  text-align: center;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px;
}

.login-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.login-form {
  margin-top: 32px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.login-footer {
  margin-top: 32px;
}

.divider-text {
  color: #9ca3af;
  font-size: 12px;
  padding: 0 12px;
}

.test-account {
  text-align: center;
  margin-top: 16px;
}

.copyright {
  position: absolute;
  bottom: 24px;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
}

/* 动画 */
@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInRight {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .brand-section {
    display: none;
  }

  .form-section {
    flex: 1;
  }
}

@media (max-width: 640px) {
  .form-section {
    padding: 24px;
  }

  .login-box {
    padding: 32px 24px;
  }

  .login-title {
    font-size: 24px;
  }
}
</style>
