<script setup lang="ts">
/**
 * 标签栏组件 - 显示已访问页面的标签
 *
 * @author zhuhx
 */
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTagsStore } from '@/stores/tags'
import type { TagItem } from '@/types/tags'
import { Close, Refresh, CircleClose, Right, FolderDelete } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const tagsStore = useTagsStore()

// 右键菜单状态
const contextMenuVisible = ref(false)
const contextMenuStyle = ref({ left: '0px', top: '0px' })
const selectedTag = ref<TagItem | null>(null)

// 可滚动容器引用
const scrollContainer = ref<HTMLElement | null>(null)

/**
 * 处理标签点击 - 导航到路由
 */
function handleTagClick(tag: TagItem) {
  if (route.path !== tag.path) {
    router.push(tag.fullPath || tag.path)
  }
}

/**
 * 处理标签关闭
 */
function handleTagClose(tag: TagItem) {
  const nextTag = tagsStore.removeTag(tag.path)
  if (nextTag && route.path === tag.path) {
    router.push(nextTag.fullPath || nextTag.path)
  }
}

/**
 * 处理右键点击 - 显示上下文菜单
 */
function handleContextMenu(event: MouseEvent, tag: TagItem) {
  event.preventDefault()
  selectedTag.value = tag
  contextMenuVisible.value = true
  contextMenuStyle.value = {
    left: `${event.clientX}px`,
    top: `${event.clientY}px`
  }
}

/**
 * 关闭上下文菜单
 */
function closeContextMenu() {
  contextMenuVisible.value = false
  selectedTag.value = null
}

/**
 * 上下文菜单操作
 */
function handleRefresh() {
  router.go(0) // 刷新当前页面
  closeContextMenu()
}

function handleCloseOthers() {
  if (selectedTag.value) {
    tagsStore.removeOtherTags(selectedTag.value.path)
    if (route.path !== selectedTag.value.path) {
      router.push(selectedTag.value.fullPath || selectedTag.value.path)
    }
  }
  closeContextMenu()
}

function handleCloseAll() {
  const dashboardTag = tagsStore.removeAllTags()
  router.push(dashboardTag.fullPath || dashboardTag.path)
  closeContextMenu()
}

function handleCloseRight() {
  if (selectedTag.value) {
    tagsStore.removeRightTags(selectedTag.value.path)
  }
  closeContextMenu()
}

/**
 * 检查标签是否激活
 */
function isActive(tag: TagItem): boolean {
  return route.path === tag.path
}

/**
 * 滚动到激活的标签
 */
async function scrollToActiveTag() {
  await nextTick()
  if (scrollContainer.value) {
    const activeTag = scrollContainer.value.querySelector('.tag-item.active')
    if (activeTag) {
      activeTag.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' })
    }
  }
}

// 监听路由变化以滚动到激活标签
watch(() => route.path, () => {
  scrollToActiveTag()
})

// 点击外部时关闭上下文菜单
onMounted(() => {
  document.addEventListener('click', closeContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
})
</script>

<template>
  <div class="tags-bar">
    <div class="tags-container" ref="scrollContainer">
      <div
        v-for="tag in tagsStore.visitedTags"
        :key="tag.path"
        class="tag-item"
        :class="{ active: isActive(tag) }"
        @click="handleTagClick(tag)"
        @contextmenu="handleContextMenu($event, tag)"
      >
        <span class="tag-title">{{ tag.title }}</span>
        <el-icon
          v-if="tag.closeable"
          class="tag-close"
          @click.stop="handleTagClose(tag)"
        >
          <Close />
        </el-icon>
      </div>
    </div>

    <!-- Context Menu -->
    <Teleport to="body">
      <div
        v-show="contextMenuVisible"
        class="context-menu"
        :style="contextMenuStyle"
        @click.stop
      >
        <div class="context-menu-item" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </div>
        <div
          v-if="selectedTag?.closeable"
          class="context-menu-item"
          @click="handleTagClose(selectedTag)"
        >
          <el-icon><Close /></el-icon>
          <span>关闭</span>
        </div>
        <div class="context-menu-item" @click="handleCloseOthers">
          <el-icon><CircleClose /></el-icon>
          <span>关闭其他</span>
        </div>
        <div class="context-menu-item" @click="handleCloseRight">
          <el-icon><Right /></el-icon>
          <span>关闭右侧</span>
        </div>
        <div class="context-menu-item" @click="handleCloseAll">
          <el-icon><FolderDelete /></el-icon>
          <span>关闭全部</span>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.tags-bar {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 10px;
  height: 40px;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.tags-container {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  overflow-y: hidden;
  flex: 1;
  height: 100%;
  align-items: center;
  /* 隐藏滚动条但保留滚动功能 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

.tags-container::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  height: 28px;
  background: #f4f4f5;
  border: 1px solid #e4e7ed;
  border-radius: 3px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
  transition: all 0.2s;
  flex-shrink: 0;
}

.tag-item:hover {
  background: #e9e9eb;
  border-color: #d3d4d6;
}

.tag-item.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.tag-title {
  line-height: 1;
}

.tag-close {
  font-size: 12px;
  border-radius: 50%;
  transition: all 0.2s;
}

.tag-close:hover {
  background: rgba(0, 0, 0, 0.1);
}

.tag-item.active .tag-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 上下文菜单 */
.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  z-index: 3000;
  min-width: 120px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.context-menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.context-menu-item .el-icon {
  font-size: 14px;
}
</style>
