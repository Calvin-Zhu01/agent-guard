<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style>
#app {
  width: 100%;
  height: 100vh;
}

body {
  margin: 0;
  padding: 0;
}

/* 移除所有元素的focus outline */
*:focus,
*:focus-visible {
  outline: none !important;
}

/* 移除按钮的focus outline */
button:focus,
button:focus-visible,
.el-button:focus,
.el-button:focus-visible {
  outline: none !important;
}

/* 移除链接的focus outline */
a:focus,
a:focus-visible {
  outline: none !important;
}

/* 移除下拉菜单的focus outline */
.el-dropdown:focus,
.el-dropdown:focus-visible,
.el-dropdown-link:focus,
.el-dropdown-link:focus-visible {
  outline: none !important;
}

/* 移除输入框的focus outline */
input:focus,
input:focus-visible,
textarea:focus,
textarea:focus-visible {
  outline: none !important;
}
</style>
