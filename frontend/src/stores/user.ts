/**
 * 用户状态管理
 *
 * @author zhuhx
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/user'
import { getCurrentUser } from '@/api/auth'

const TOKEN_KEY = 'agentguard_token'
const USER_KEY = 'agentguard_user'

export const useUserStore = defineStore('user', () => {
  // 从 localStorage 恢复状态
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<User | null>(restoreUserInfo())

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const username = computed(() => userInfo.value?.username || '')

  /**
   * 从 localStorage 恢复用户信息
   */
  function restoreUserInfo(): User | null {
    const stored = localStorage.getItem(USER_KEY)
    if (stored) {
      try {
        return JSON.parse(stored)
      } catch {
        return null
      }
    }
    return null
  }

  /**
   * 设置 Token
   */
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem(TOKEN_KEY, newToken)
  }

  /**
   * 设置用户信息
   */
  function setUserInfo(info: User) {
    userInfo.value = info
    localStorage.setItem(USER_KEY, JSON.stringify(info))
  }

  /**
   * 清除 Token
   */
  function clearToken() {
    token.value = ''
    localStorage.removeItem(TOKEN_KEY)
  }

  /**
   * 清除用户信息
   */
  function clearUserInfo() {
    userInfo.value = null
    localStorage.removeItem(USER_KEY)
  }

  /**
   * 退出登录
   */
  function logout() {
    clearToken()
    clearUserInfo()
  }

  /**
   * 获取当前用户信息（从服务器）
   */
  async function fetchUserInfo(): Promise<User | null> {
    if (!token.value) {
      return null
    }
    try {
      const user = await getCurrentUser()
      setUserInfo(user)
      return user
    } catch {
      // Token 无效，清除登录状态
      logout()
      return null
    }
  }

  /**
   * 检查登录状态
   * 如果有 Token 但没有用户信息，尝试获取用户信息
   */
  async function checkAuth(): Promise<boolean> {
    if (!token.value) {
      return false
    }
    if (!userInfo.value) {
      const user = await fetchUserInfo()
      return !!user
    }
    return true
  }

  return {
    // 状态
    token,
    userInfo,
    // 计算属性
    isLoggedIn,
    isAdmin,
    username,
    // 方法
    setToken,
    setUserInfo,
    clearToken,
    clearUserInfo,
    logout,
    fetchUserInfo,
    checkAuth
  }
})
