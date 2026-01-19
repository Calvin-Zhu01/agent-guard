<script setup lang="ts">
/**
 * 系统设置页面
 *
 * @author zhuhx
 */
import { ref } from 'vue'

const settings = ref({
  alertEmail: 'admin@example.com',
  budgetLimit: 100,
  alertThreshold: 80
})
</script>

<template>
  <div class="settings-page">
    <el-card>
      <template #header>系统设置</template>

      <el-tabs>
        <el-tab-pane label="告警设置">
          <el-form :model="settings" label-width="120px" style="max-width: 500px">
            <el-form-item label="告警邮箱">
              <el-input v-model="settings.alertEmail" />
            </el-form-item>
            <el-form-item label="月度预算">
              <el-input-number v-model="settings.budgetLimit" :min="0" :step="10" />
              <span style="margin-left: 10px">美元</span>
            </el-form-item>
            <el-form-item label="告警阈值">
              <el-slider v-model="settings.alertThreshold" :min="50" :max="100" show-input />
            </el-form-item>
            <el-form-item>
              <el-button type="primary">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="用户管理">
          <el-empty description="功能开发中" />
        </el-tab-pane>
        <el-tab-pane label="API密钥">
          <el-empty description="功能开发中" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>
