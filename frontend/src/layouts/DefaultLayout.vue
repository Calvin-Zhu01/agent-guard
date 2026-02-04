<script setup lang="ts">
/**
 * 默认布局组件
 *
 * @author zhuhx
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useTagsStore } from '@/stores/tags'
import { logout as logoutApi } from '@/api/auth'
import TagsBar from '@/components/TagsBar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tagsStore = useTagsStore()

const isCollapse = ref(false)
const loggingOut = ref(false)

const activeMenu = computed(() => route.path)

const menuItems = [
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/agents', title: 'Agent管理', icon: 'Monitor' },
  { path: '/logs', title: '调用日志', icon: 'Document' },
  { path: '/policies', title: '策略管理', icon: 'Lock' },
  { path: '/approvals', title: '审批中心', icon: 'Checked' },
  { path: '/stats', title: '成本分析', icon: 'TrendCharts' },
  { path: '/settings', title: '系统设置', icon: 'Setting' }
]

// 根据用户角色过滤菜单项
const visibleMenuItems = computed(() => {
  return menuItems.filter(item => {
    // 如果菜单项没有角色限制，所有人可见
    if (!item.roles || item.roles.length === 0) {
      return true
    }
    // 如果有角色限制，检查用户是否是管理员
    return item.roles.includes('ADMIN') && userStore.isAdmin
  })
})

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
  // 初始化当前路由标签
  tagsStore.addTag(route)
})

// 监听路由变化，添加标签
router.afterEach((to) => {
  tagsStore.addTag(to)
})
</script>

<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '256px'" class="aside">
      <div class="logo" @click="router.push('/dashboard')">
        <img 
          v-if="!isCollapse" 
          src="/logo-horizontal.svg" 
          alt="AgentGuard" 
          class="logo-horizontal"
        />
        <img 
          v-else 
          src="/logo.svg" 
          alt="AG" 
          class="logo-icon"
        />
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#409eff"
      >
        <el-menu-item v-for="item in visibleMenuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
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

      <!-- 标签栏 -->
      <TagsBar />

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
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 15px 5px;
  cursor: pointer;
  transition: all 0.3s;
}

.logo:hover {
  opacity: 0.8;
}

.logo-horizontal {
  height: 64px;
  width: auto;
  max-width: 100%;
  object-fit: contain;
}

.logo-icon {
  height: 52px;
  width: 52px;
  object-fit: contain;
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

/* 菜单项基础样式 - 添加圆角 */
.aside :deep(.el-menu-item) {
  border-radius: 8px;
  margin: 4px 8px;
}

/* 普通菜单项悬浮样式 - 灰色背景 */
.aside :deep(.el-menu-item:hover:not(.is-active)) {
  background-color: rgba(255, 255, 255, 0.15) !important;
}

/* 选中的菜单项 - 深蓝色背景 */
.aside :deep(.el-menu-item.is-active) {
  background-color: #335075 !important;
}

/* 选中的菜单项悬浮时保持深蓝色 */
.aside :deep(.el-menu-item.is-active:hover) {
  background-color: #1e4d8b !important;
}
</style>
