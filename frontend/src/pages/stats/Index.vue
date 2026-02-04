<script setup lang="ts">
/**
 * 成本分析页面
 *
 * @author zhuhx
 */
import { ref, onMounted, watch, computed } from 'vue'
import * as echarts from 'echarts'
import { getStatsOverview, getCostTrends, getTopAgents, getCurrentBudget } from '@/api/stats'
import type { StatsOverview, CostTrend, AgentCostRank, BudgetWithUsage } from '@/types/stats'

// 日期范围
const dateRange = ref<[Date, Date] | null>(null)

// 数据状态
const loading = ref(false)
const overview = ref<StatsOverview | null>(null)
const trends = ref<CostTrend[]>([])
const topAgents = ref<AgentCostRank[]>([])
const budget = ref<BudgetWithUsage | null>(null)

// 图表实例
let chartInstance: echarts.ECharts | null = null
const chartRef = ref<HTMLDivElement | null>(null)

// 格式化日期为 yyyy-MM-dd
const formatDate = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 获取查询参数
const getQueryParams = () => {
  if (dateRange.value && dateRange.value[0] && dateRange.value[1]) {
    return {
      startDate: formatDate(dateRange.value[0]),
      endDate: formatDate(dateRange.value[1])
    }
  }
  // 默认最近30天
  const endDate = new Date()
  const startDate = new Date()
  startDate.setDate(startDate.getDate() - 30)
  return {
    startDate: formatDate(startDate),
    endDate: formatDate(endDate)
  }
}

// 加载成本概览
const loadOverview = async () => {
  try {
    const params = getQueryParams()
    overview.value = await getStatsOverview(params)
  } catch (error) {
    console.error('加载成本概览失败:', error)
  }
}

// 加载成本趋势
const loadTrends = async () => {
  try {
    const params = getQueryParams()
    trends.value = await getCostTrends(params)
    renderChart()
  } catch (error) {
    console.error('加载成本趋势失败:', error)
  }
}

// 加载 TOP Agent
const loadTopAgents = async () => {
  try {
    const params = { ...getQueryParams(), limit: 10 }
    topAgents.value = await getTopAgents(params)
  } catch (error) {
    console.error('加载TOP Agent失败:', error)
  }
}

// 加载预算信息
const loadBudget = async () => {
  try {
    budget.value = await getCurrentBudget()
  } catch (error) {
    console.error('加载预算信息失败:', error)
    // 预算可能未配置，不显示错误
  }
}

// 加载所有数据
const loadAllData = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadOverview(),
      loadTrends(),
      loadTopAgents(),
      loadBudget()
    ])
  } finally {
    loading.value = false
  }
}

// 渲染图表
const renderChart = () => {
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const dates = trends.value.map(item => item.date)
  const costs = trends.value.map(item => item.totalCost)
  const tokens = trends.value.map(item => item.totalTokens)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['成本 ($)', 'Token 消耗']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates
    },
    yAxis: [
      {
        type: 'value',
        name: '成本 ($)',
        position: 'left',
        axisLabel: {
          formatter: '${value}'
        }
      },
      {
        type: 'value',
        name: 'Token',
        position: 'right',
        axisLabel: {
          formatter: (value: number) => {
            if (value >= 1000000) return (value / 1000000).toFixed(1) + 'M'
            if (value >= 1000) return (value / 1000).toFixed(1) + 'K'
            return value.toString()
          }
        }
      }
    ],
    series: [
      {
        name: '成本 ($)',
        type: 'line',
        smooth: true,
        data: costs,
        itemStyle: {
          color: '#409eff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        }
      },
      {
        name: 'Token 消耗',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: tokens,
        itemStyle: {
          color: '#67c23a'
        }
      }
    ]
  }

  chartInstance.setOption(option)
}

// 预算使用百分比
const budgetPercentage = computed(() => {
  if (!budget.value) return 0
  return Math.min(budget.value.usagePercentage, 100)
})

// 预算进度条状态
const budgetStatus = computed(() => {
  if (!budget.value) return ''
  if (budget.value.overBudget) return 'exception'
  if (budget.value.alertTriggered) return 'warning'
  return ''
})

// 格式化数字
const formatNumber = (num: number): string => {
  if (num >= 1000000) return (num / 1000000).toFixed(2) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(2) + 'K'
  return num.toLocaleString()
}

// 格式化金额
const formatCost = (cost: number): string => {
  return '$' + cost.toFixed(2)
}

// 监听日期范围变化
watch(dateRange, () => {
  loadOverview()
  loadTrends()
  loadTopAgents()
})

// 窗口大小变化时重新渲染图表
const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  loadAllData()
  window.addEventListener('resize', handleResize)
})
</script>

<template>
  <div class="stats-page" v-loading="loading">
    <!-- 成本概览卡片 -->
    <el-row :gutter="20" class="overview-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="overview-card" shadow="hover">
          <div class="overview-icon" style="background: linear-gradient(135deg, #409eff, #66b1ff)">
            <el-icon size="24"><Money /></el-icon>
          </div>
          <div class="overview-content">
            <div class="overview-label">总成本</div>
            <div class="overview-value">{{ formatCost(overview?.totalCost || 0) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="overview-card" shadow="hover">
          <div class="overview-icon" style="background: linear-gradient(135deg, #67c23a, #85ce61)">
            <el-icon size="24"><Coin /></el-icon>
          </div>
          <div class="overview-content">
            <div class="overview-label">总 Token</div>
            <div class="overview-value">{{ formatNumber(overview?.totalTokens || 0) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="overview-card" shadow="hover">
          <div class="overview-icon" style="background: linear-gradient(135deg, #e6a23c, #f0c78a)">
            <el-icon size="24"><Connection /></el-icon>
          </div>
          <div class="overview-content">
            <div class="overview-label">总调用次数</div>
            <div class="overview-value">{{ formatNumber(overview?.totalCalls || 0) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="overview-card" shadow="hover">
          <div class="overview-icon" style="background: linear-gradient(135deg, #909399, #b4b4b4)">
            <el-icon size="24"><Monitor /></el-icon>
          </div>
          <div class="overview-content">
            <div class="overview-label">Agent 数量</div>
            <div class="overview-value">{{ overview?.agentCount || 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预算进度 -->
    <el-card v-if="budget" class="budget-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>{{ budget.month }} 预算使用情况</span>
          <el-tag v-if="budget.overBudget" type="danger">已超预算</el-tag>
          <el-tag v-else-if="budget.alertTriggered" type="warning">接近预算</el-tag>
          <el-tag v-else type="success">正常</el-tag>
        </div>
      </template>
      <div class="budget-content">
        <div class="budget-info">
          <div class="budget-item">
            <span class="budget-label">预算上限</span>
            <span class="budget-value">{{ formatCost(budget.limitAmount) }}</span>
          </div>
          <div class="budget-item">
            <span class="budget-label">已使用</span>
            <span class="budget-value" :class="{ 'over-budget': budget.overBudget }">
              {{ formatCost(budget.usedAmount) }}
            </span>
          </div>
          <div class="budget-item">
            <span class="budget-label">剩余</span>
            <span class="budget-value">{{ formatCost(budget.remainingAmount) }}</span>
          </div>
          <div class="budget-item">
            <span class="budget-label">告警阈值</span>
            <span class="budget-value">{{ (budget.alertThreshold * 100).toFixed(0) }}%</span>
          </div>
        </div>
        <el-progress
          :percentage="budgetPercentage"
          :stroke-width="20"
          :status="budgetStatus"
          :format="(p: number) => p.toFixed(1) + '%'"
        />
      </div>
    </el-card>

    <!-- 成本趋势图表 -->
    <el-card class="chart-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>成本趋势</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="default"
            value-format="YYYY-MM-DD"
            :shortcuts="[
              { text: '最近7天', value: () => { const end = new Date(); const start = new Date(); start.setDate(start.getDate() - 7); return [start, end] } },
              { text: '最近30天', value: () => { const end = new Date(); const start = new Date(); start.setDate(start.getDate() - 30); return [start, end] } },
              { text: '本月', value: () => { const end = new Date(); const start = new Date(end.getFullYear(), end.getMonth(), 1); return [start, end] } }
            ]"
          />
        </div>
      </template>
      <div ref="chartRef" class="chart-container"></div>
    </el-card>

    <!-- TOP Agent 排行 -->
    <el-card class="rank-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>TOP Agent 成本排行</span>
        </div>
      </template>
      <el-table :data="topAgents" stripe>
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.rank === 1" type="danger" effect="dark" round>{{ row.rank }}</el-tag>
            <el-tag v-else-if="row.rank === 2" type="warning" effect="dark" round>{{ row.rank }}</el-tag>
            <el-tag v-else-if="row.rank === 3" type="success" effect="dark" round>{{ row.rank }}</el-tag>
            <span v-else>{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="agentName" label="Agent 名称" min-width="150" />
        <el-table-column label="总成本" min-width="120" align="right">
          <template #default="{ row }">
            <span class="cost-value">{{ formatCost(row.totalCost) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Token 消耗" min-width="120" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.totalTokens) }}
          </template>
        </el-table-column>
        <el-table-column label="调用次数" min-width="100" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.apiCalls) }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="topAgents.length === 0" description="暂无数据" />
    </el-card>
  </div>
</template>

<style scoped>
.overview-row {
  margin-bottom: 20px;
}

.overview-card {
  display: flex;
  align-items: center;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
}

.overview-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 20px;
}

.overview-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-right: 16px;
  flex-shrink: 0;
}

.overview-content {
  flex: 1;
}

.overview-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.overview-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.budget-card {
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
}

.budget-card :deep(.el-card__header) {
  border-radius: 12px 12px 0 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.budget-content {
  padding: 10px 0;
}

.budget-info {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

.budget-item {
  text-align: center;
}

.budget-label {
  display: block;
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.budget-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.budget-value.over-budget {
  color: #f56c6c;
}

.chart-card {
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
}

.chart-card :deep(.el-card__header) {
  border-radius: 12px 12px 0 0;
}

.chart-container {
  height: 350px;
  width: 100%;
}

.rank-card {
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
}

.rank-card :deep(.el-card__header) {
  border-radius: 12px 12px 0 0;
}

.cost-value {
  font-weight: 600;
  color: #409eff;
}
</style>
