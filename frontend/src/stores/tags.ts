/**
 * 标签栏状态管理
 *
 * @author zhuhx
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { TagItem } from '@/types/tags'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

const TAGS_KEY = 'agentguard_tags'

export const useTagsStore = defineStore('tags', () => {
  // 状态
  const visitedTags = ref<TagItem[]>(restoreTags())
  const activeTagPath = ref<string>('')

  // 计算属性
  const activeTag = computed(() =>
    visitedTags.value.find(tag => tag.path === activeTagPath.value)
  )

  /**
   * 从 localStorage 恢复标签
   */
  function restoreTags(): TagItem[] {
    const stored = localStorage.getItem(TAGS_KEY)
    if (stored) {
      try {
        const tags = JSON.parse(stored)
        // 始终确保仪表盘标签存在且在首位
        const hasDashboard = tags.some((t: TagItem) => t.path === '/dashboard')
        if (!hasDashboard) {
          return [createDashboardTag(), ...tags]
        }
        return tags
      } catch {
        return [createDashboardTag()]
      }
    }
    return [createDashboardTag()]
  }

  /**
   * 创建仪表盘标签（永久标签）
   */
  function createDashboardTag(): TagItem {
    return {
      path: '/dashboard',
      title: '仪表盘',
      name: 'Dashboard',
      closeable: false,
      fullPath: '/dashboard'
    }
  }

  /**
   * 保存标签到 localStorage
   */
  function saveTags() {
    localStorage.setItem(TAGS_KEY, JSON.stringify(visitedTags.value))
  }

  /**
   * 从路由添加标签
   */
  function addTag(route: RouteLocationNormalizedLoaded) {
    // 跳过登录页和其他非主要路由
    if (!route.meta.title || route.path === '/login') {
      return
    }

    const title = route.meta.title as string
    const existingTag = visitedTags.value.find(tag => tag.path === route.path)

    if (!existingTag) {
      const newTag: TagItem = {
        path: route.path,
        title,
        name: route.name as string,
        closeable: route.path !== '/dashboard', // 仪表盘不可关闭
        fullPath: route.fullPath
      }
      visitedTags.value.push(newTag)
      saveTags()
    }

    activeTagPath.value = route.path
  }

  /**
   * 根据路径移除标签
   */
  function removeTag(path: string) {
    const index = visitedTags.value.findIndex(tag => tag.path === path)
    if (index > -1) {
      const tag = visitedTags.value[index]
      // 不允许移除仪表盘
      if (!tag.closeable) {
        return null
      }
      visitedTags.value.splice(index, 1)
      saveTags()

      // 返回下一个要导航到的标签
      if (activeTagPath.value === path) {
        // 如果关闭的是当前激活标签，导航到前一个或后一个
        if (visitedTags.value[index]) {
          return visitedTags.value[index]
        } else if (visitedTags.value[index - 1]) {
          return visitedTags.value[index - 1]
        }
      }
    }
    return null
  }

  /**
   * 移除其他标签（保留当前和仪表盘）
   */
  function removeOtherTags(path: string) {
    visitedTags.value = visitedTags.value.filter(
      tag => !tag.closeable || tag.path === path
    )
    saveTags()
  }

  /**
   * 移除所有标签（仪表盘除外）
   */
  function removeAllTags() {
    visitedTags.value = visitedTags.value.filter(tag => !tag.closeable)
    saveTags()
    return visitedTags.value[0] // 返回仪表盘
  }

  /**
   * 移除右侧标签
   */
  function removeRightTags(path: string) {
    const index = visitedTags.value.findIndex(tag => tag.path === path)
    if (index > -1) {
      visitedTags.value = visitedTags.value.slice(0, index + 1)
      saveTags()
    }
  }

  /**
   * 设置激活标签
   */
  function setActiveTag(path: string) {
    activeTagPath.value = path
  }

  return {
    // 状态
    visitedTags,
    activeTagPath,
    // 计算属性
    activeTag,
    // 方法
    addTag,
    removeTag,
    removeOtherTags,
    removeAllTags,
    removeRightTags,
    setActiveTag
  }
})
