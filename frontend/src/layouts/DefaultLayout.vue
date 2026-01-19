<script setup lang="ts">
/**
 * 默认布局组件
 *
 * @author zhuhx
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/auth'
import { countAlertHistory } from '@/api/alert'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const loggingOut = ref(false)

// 告警徽章数量（失败的告警数）
const alertBadgeCount = ref(0)
let alertPollingTimer: ReturnType<typeof setInterval> | null = null

const activeMenu = computed(() => route.path)

const menuItems = [
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/agents', title: 'Agent管理', icon: 'Monitor' },
  { path: '/logs', title: '调用日志', icon: 'Document' },
  { path: '/policies', title: '策略管理', icon: 'Lock' },
  { path: '/approvals', title: '审批中心', icon: 'Checked' },
  { path: '/alerts', title: '告警管理', icon: 'Bell', badge: true },
  { path: '/stats', title: '成本分析', icon: 'TrendCharts' },
  { path: '/settings', title: '系统设置', icon: 'Setting' }
]

/**
 * 获取失败告警数量
 */
async function fetchAlertBadgeCount() {
  try {
    // 获取最近24小时内失败的告警数量
    const now = new Date()
    const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000)
    const startTime = formatDateTime(yesterday)
    const endTime = formatDateTime(now)
    
    const count = await countAlertHistory({ status: 'FAILED', startTime, endTime })
    alertBadgeCount.value = count || 0
  } catch {
    // 静默失败
  }
}

/**
 * 格式化日期时间
 */
function formatDateTime(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 退出登录
 */
async function handleLogout() {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    loggingOut.value = true
    
    try {
      // 调用后端退出接口（可选，主要是清除服务端 session）
      await logoutApi()
    } catch {
      // 即使后端退出失败，也继续清除本地状态
    }
    
    // 清除本地登录状态
    userStore.logout()
    
    ElMessage.success('已退出登录')
    
    // 跳转到登录页
    router.push('/login')
  } catch {
    // 用户取消退出
  } finally {
    loggingOut.value = false
  }
}

onMounted(() => {
  // 初始获取告警数量
  fetchAlertBadgeCount()
  // 每分钟轮询一次
  alertPollingTimer = setInterval(fetchAlertBadgeCount, 60000)
})

onUnmounted(() => {
  if (alertPollingTimer) {
    clearInterval(alertPollingTimer)
    alertPollingTimer = null
  }
})
</script>

<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <span v-if="!isCollapse">AgentGuard</span>
        <span v-else>AG</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#409eff"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-badge 
            v-if="item.badge && alertBadgeCount > 0" 
            :value="alertBadgeCount" 
            :max="99"
            class="menu-badge"
          >
            <el-icon><component :is="item.icon" /></el-icon>
          </el-badge>
          <el-icon v-else><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="28" class="user-avatar">
                {{ userStore.username?.charAt(0)?.toUpperCase() || 'U' }}
              </el-avatar>
              <span class="user-name">{{ userStore.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon>
                  {{ userStore.userInfo?.email || '未设置邮箱' }}
                </el-dropdown-item>
                <el-dropdown-item divided :loading="loggingOut" @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #001529;
  transition: width 0.3s;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background-color: #409eff;
  color: #fff;
  font-size: 14px;
}

.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main {
  background: #f0f2f5;
  padding: 20px;
}

.menu-badge :deep(.el-badge__content) {
  top: 2px;
  right: 2px;
}
</style>
