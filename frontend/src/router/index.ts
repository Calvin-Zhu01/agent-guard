/**
 * 路由配置
 *
 * @author zhuhx
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 白名单路由（不需要登录）
const whiteList = ['/login']

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/auth/Login.vue'),
    meta: { 
      requiresAuth: false,
      title: '登录'
    }
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/dashboard/Index.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'agents',
        name: 'AgentList',
        component: () => import('@/pages/agent/Index.vue'),
        meta: { title: 'Agent管理' }
      },
      {
        path: 'logs',
        name: 'LogList',
        component: () => import('@/pages/log/Index.vue'),
        meta: { title: '调用日志' }
      },
      {
        path: 'policies',
        name: 'PolicyList',
        component: () => import('@/pages/policy/Index.vue'),
        meta: { title: '策略管理' }
      },
      {
        path: 'approvals',
        name: 'ApprovalList',
        component: () => import('@/pages/approval/Index.vue'),
        meta: { title: '审批中心' }
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/pages/stats/Index.vue'),
        meta: { title: '成本分析' }
      },
      {
        path: 'alerts',
        name: 'AlertList',
        component: () => import('@/pages/alert/Index.vue'),
        meta: { title: '告警管理' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/pages/settings/Index.vue'),
        meta: { title: '系统设置' }
      }
    ]
  },
  // 404 页面
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  // 设置页面标题
  const title = to.meta.title as string
  document.title = title ? `${title} - AgentGuard` : 'AgentGuard'
  
  // 检查是否在白名单中
  if (whiteList.includes(to.path)) {
    // 如果已登录且访问登录页，重定向到首页
    const userStore = useUserStore()
    if (userStore.isLoggedIn && to.path === '/login') {
      next('/dashboard')
      return
    }
    next()
    return
  }
  
  // 检查登录状态
  const userStore = useUserStore()
  
  if (!userStore.isLoggedIn) {
    // 未登录，重定向到登录页
    next({
      path: '/login',
      query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined
    })
    return
  }
  
  // 如果有 Token 但没有用户信息，尝试获取
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // 获取用户信息失败，重定向到登录页
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }
  }
  
  next()
})

export default router
