<script setup lang="ts">
/**
 * 代码块组件
 * 支持语法高亮和复制功能
 *
 * @author zhuhx
 */
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'

interface Props {
  code: string
  language?: string
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  language: 'json',
  title: ''
})

// 复制代码到剪贴板
const copyCode = async () => {
  try {
    await navigator.clipboard.writeText(props.code)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

// JSON 语法高亮
const highlightedCode = computed(() => {
  if (props.language.toLowerCase() !== 'json') {
    return props.code
  }

  // 简单的 JSON 语法高亮
  return props.code
    .replace(/"([^"]+)":/g, '<span style="color: #e96900">"$1"</span>:') // 键名
    .replace(/:\s*"([^"]*)"/g, (match) => {
      // 保持原始空格，不添加额外空格
      const colonAndSpaces = match.match(/:\s*/)?.[0] || ': '
      const stringValue = match.match(/"([^"]*)"/)?.[1] || ''
      return `${colonAndSpaces}<span style="color: #50a14f">"${stringValue}"</span>`
    }) // 字符串值
    .replace(/:\s*(\d+\.?\d*)(,|\s*\}|\s*\])/g, (match, num, suffix) => {
      // 只匹配独立的数字值，不匹配字符串内的数字
      const colonAndSpaces = match.match(/:\s*/)?.[0] || ': '
      return `${colonAndSpaces}<span style="color: #0184bc">${num}</span>${suffix}`
    }) // 数字值
    .replace(/:\s*(true|false|null)(,|\s*\}|\s*\])/g, (match, value, suffix) => {
      const colonAndSpaces = match.match(/:\s*/)?.[0] || ': '
      return `${colonAndSpaces}<span style="color: #a626a4">${value}</span>${suffix}`
    }) // 布尔值和null
})
</script>

<template>
  <div class="code-block">
    <el-button
      :icon="CopyDocument"
      class="copy-button"
      size="small"
      @click="copyCode"
    />
    <pre class="code-block-content" v-html="highlightedCode"></pre>
  </div>
</template>

<style scoped>
.code-block {
  position: relative;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.copy-button {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #dcdfe6;
  color: #606266;
}

.copy-button:hover {
  background: #ffffff;
  border-color: #409eff;
  color: #409eff;
}

.code-block:hover .copy-button {
  opacity: 1;
}

.code-block-content {
  margin: 0;
  padding: 15px;
  font-size: 13px;
  color: #606266;
  overflow-x: auto;
  line-height: 1.6;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  background: transparent;
}
</style>
