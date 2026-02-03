/**
 * Axios 请求封装
 *
 * @author zhuhx
 */
import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// Token 存储 key
const TOKEN_KEY = 'agentguard_token'

/**
 * 获取 Token
 * 注意：这里直接从 localStorage 获取，避免循环依赖
 */
function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/**
 * 清除登录状态
 */
function clearAuth(): void {
  // 清除 localStorage
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('agentguard_user')

  // 使用动态导入避免循环依赖
  // 同步清除 Pinia store 的状态
  import('@/stores/user').then(({ useUserStore }) => {
    const userStore = useUserStore()
    userStore.clearToken()
    userStore.clearUserInfo()
  })
}

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data
    
    // 成功响应
    if (code === 200) {
      return data
    }
    
    // 业务错误
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message
    
    // 处理 401 未授权错误
    if (status === 401) {
      clearAuth()
      
      // 避免重复跳转
      if (router.currentRoute.value.path !== '/login') {
        ElMessage.error('登录已过期，请重新登录')
        
        // 保存当前路径，登录后跳转回来
        const currentPath = router.currentRoute.value.fullPath
        router.push({
          path: '/login',
          query: currentPath !== '/' ? { redirect: currentPath } : undefined
        })
      }
      
      return Promise.reject(new Error('未授权'))
    }
    
    // 处理 403 禁止访问
    if (status === 403) {
      ElMessage.error('没有权限访问该资源')
      return Promise.reject(new Error('禁止访问'))
    }
    
    // 处理 404 未找到
    if (status === 404) {
      ElMessage.error('请求的资源不存在')
      return Promise.reject(new Error('资源不存在'))
    }
    
    // 处理 500 服务器错误
    if (status >= 500) {
      ElMessage.error('服务器错误，请稍后重试')
      return Promise.reject(new Error('服务器错误'))
    }
    
    // 其他错误
    ElMessage.error(message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
