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
    <div class="login-box">
      <h2 class="title">AgentGuard</h2>
      <p class="subtitle">AI Agent 治理与监控平台</p>
      
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules"
        @submit.prevent="handleLogin"
        @keyup="handleKeyup"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <span class="hint">默认账号: admin / admin123</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.title {
  text-align: center;
  margin: 0 0 8px;
  color: #333;
}

.subtitle {
  text-align: center;
  color: #999;
  margin: 0 0 30px;
}

.login-footer {
  text-align: center;
  margin-top: 16px;
}

.hint {
  color: #999;
  font-size: 12px;
}
</style>
