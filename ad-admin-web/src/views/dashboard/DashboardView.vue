<template>
  <AppPage eyebrow="运营概览" title="工作台" :stats="stats">
    <template #actions>
      <el-button :loading="loading" :icon="Refresh" @click="loadDashboard">刷新数据</el-button>
    </template>

    <section class="hero-panel">
      <div>
        <p class="eyebrow">MRD AD COMMAND CENTER</p>
        <h3>ViiAD 运行驾驶舱</h3>
        <span>聚合广告、素材、计划、设备、下发、报表和工单关键状态，快速判断今天是否跑得顺。</span>
      </div>
      <div class="hero-metrics">
        <strong>{{ overview.completionRate }}</strong>
        <small>投放完成率</small>
      </div>
    </section>

    <div class="dashboard-grid">
      <section class="chart-card wide">
        <div class="card-head">
          <div>
            <p class="eyebrow">近 7 日</p>
            <h3>播放趋势</h3>
          </div>
          <el-tag type="success" effect="dark">播放 {{ formatNumber(overview.playCount) }}</el-tag>
        </div>
        <div ref="trendChartRef" class="chart"></div>
      </section>

      <section class="chart-card">
        <div class="card-head">
          <div>
            <p class="eyebrow">广告状态</p>
            <h3>广告分布</h3>
          </div>
        </div>
        <div ref="adChartRef" class="chart small"></div>
      </section>

      <section class="chart-card">
        <div class="card-head">
          <div>
            <p class="eyebrow">设备状态</p>
            <h3>在线与故障</h3>
          </div>
        </div>
        <div ref="deviceChartRef" class="chart small"></div>
      </section>

      <section class="chart-card">
        <div class="card-head">
          <div>
            <p class="eyebrow">投放计划</p>
            <h3>计划状态</h3>
          </div>
        </div>
        <div class="status-list">
          <div v-for="item in planStatusItems" :key="item.label" class="status-row">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em :style="{ width: item.percent + '%' }"></em>
          </div>
        </div>
      </section>

      <section class="chart-card">
        <div class="card-head">
          <div>
            <p class="eyebrow">待办提醒</p>
            <h3>需要关注</h3>
          </div>
        </div>
        <div class="todo-list">
          <div v-for="item in todoItems" :key="item.label" class="todo-item" :class="item.level">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>
      </section>
    </div>
  </AppPage>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { fetchAds, adStatusMap, type AdOrder } from '@/api/ads'
import { fetchMaterials, type AdMaterial } from '@/api/materials'
import { fetchPlans, type AdPlan } from '@/api/plans'
import { fetchDevices, type AdDevice } from '@/api/devices'
import { fetchDeliveries, type AdDeliveryRecord } from '@/api/deliveries'
import { fetchReportOverview, fetchPlayLogs, type PlayLog, type ReportOverview } from '@/api/reports'
import { fetchWorkOrders, type WorkOrder } from '@/api/workorders'
import { useUserStore } from '@/stores/user'
import type { ApiResponse } from '@/utils/request'
import type { PageResult } from '@/api/ads'

type ChartInstance = echarts.ECharts | null

const user = useUserStore()
const loading = ref(false)
const trendChartRef = ref<HTMLDivElement>()
const adChartRef = ref<HTMLDivElement>()
const deviceChartRef = ref<HTMLDivElement>()
let trendChart: ChartInstance = null
let adChart: ChartInstance = null
let deviceChart: ChartInstance = null

const overview = reactive<ReportOverview>({
  playCount: 0,
  exposureCount: 0,
  activeDeviceCount: 0,
  playDuration: 0,
  logCount: 0,
  completionRate: '0%'
})

const ads = ref<AdOrder[]>([])
const materials = ref<AdMaterial[]>([])
const plans = ref<AdPlan[]>([])
const devices = ref<AdDevice[]>([])
const deliveries = ref<AdDeliveryRecord[]>([])
const playLogs = ref<PlayLog[]>([])
const workOrders = ref<WorkOrder[]>([])

const dateRange = computed(() => {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 6)
  return { startDate: formatDate(start), endDate: formatDate(end) }
})

const stats = computed(() => [
  { label: '曝光量', value: formatNumber(overview.exposureCount) },
  { label: '播放次数', value: formatNumber(overview.playCount) },
  { label: '在线设备', value: devices.value.filter((item) => item.onlineStatus === 'online').length },
  { label: '待办异常', value: todoItems.value.reduce((sum, item) => sum + item.value, 0) }
])

const planStatusItems = computed(() => {
  const total = Math.max(plans.value.length, 1)
  const items = [
    { label: '草稿计划', value: plans.value.filter((item) => item.scheduleStatus === 'draft').length },
    { label: '已排期', value: plans.value.filter((item) => item.scheduleStatus === 'scheduled').length },
    { label: '投放中', value: plans.value.filter((item) => item.deliveryStatus === 'live').length },
    { label: '已暂停', value: plans.value.filter((item) => item.deliveryStatus === 'paused').length }
  ]
  return items.map((item) => ({ ...item, percent: Math.max(6, Math.round((item.value / total) * 100)) }))
})

const todoItems = computed(() => [
  {
    label: '待审广告',
    value: ads.value.filter((item) => item.status === 'submitted').length,
    hint: '影响素材与计划创建',
    level: 'warning'
  },
  {
    label: '待审素材',
    value: materials.value.filter((item) => item.status === 'pending_review').length,
    hint: '影响计划可选素材',
    level: 'warning'
  },
  {
    label: '下发失败',
    value: deliveries.value.filter((item) => item.deliveryStatus === 'failed').length,
    hint: '建议转工单处理',
    level: 'danger'
  },
  {
    label: '未关闭工单',
    value: workOrders.value.filter((item) => item.status !== 'closed').length,
    hint: '设备或下发异常待跟进',
    level: 'primary'
  }
])

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatNumber(value?: number) {
  const current = value || 0
  if (current >= 10000) {
    return `${(current / 10000).toFixed(1)}万`
  }
  return current.toLocaleString()
}

async function guardedLoad<T>(permission: string, loader: () => Promise<T>, fallback: T) {
  if (!user.hasPermission(permission)) {
    return fallback
  }
  try {
    return await loader()
  } catch {
    return fallback
  }
}

function successResponse<T>(data: T): ApiResponse<T> {
  return { code: 200, message: 'success', data }
}

function emptyPage<T>(): ApiResponse<PageResult<T>> {
  return successResponse({ records: [], total: 0, page: 1, size: 200 })
}

async function loadDashboard() {
  loading.value = true
  try {
    const range = dateRange.value
    const [overviewResult, playLogResult, adsResult, materialsResult, plansResult, devicesResult, deliveriesResult, workOrdersResult] =
      await Promise.all([
        guardedLoad('report:view', () => fetchReportOverview(range), successResponse({ ...overview })),
        guardedLoad('report:view', () => fetchPlayLogs({ ...range, page: 1, size: 200 }), emptyPage<PlayLog>()),
        guardedLoad('ad:view', () => fetchAds({ page: 1, size: 200 }), emptyPage<AdOrder>()),
        guardedLoad('material:view', () => fetchMaterials({ page: 1, size: 200 }), emptyPage<AdMaterial>()),
        guardedLoad('plan:view', () => fetchPlans({ page: 1, size: 200 }), emptyPage<AdPlan>()),
        guardedLoad('device:view', () => fetchDevices({ page: 1, size: 200 }), emptyPage<AdDevice>()),
        guardedLoad('delivery:view', () => fetchDeliveries({ page: 1, size: 200 }), emptyPage<AdDeliveryRecord>()),
        guardedLoad('workOrder:view', () => fetchWorkOrders({ page: 1, size: 200 }), emptyPage<WorkOrder>())
      ])

    Object.assign(overview, overviewResult.data)
    playLogs.value = playLogResult.data.records
    ads.value = adsResult.data.records
    materials.value = materialsResult.data.records
    plans.value = plansResult.data.records
    devices.value = devicesResult.data.records
    deliveries.value = deliveriesResult.data.records
    workOrders.value = workOrdersResult.data.records
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  renderTrendChart()
  renderAdChart()
  renderDeviceChart()
}

function renderTrendChart() {
  if (!trendChartRef.value) {
    return
  }
  trendChart = trendChart || echarts.init(trendChartRef.value)
  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date()
    date.setDate(date.getDate() - (6 - index))
    return formatDate(date)
  })
  const grouped = days.reduce<Record<string, number>>((map, day) => {
    map[day] = 0
    return map
  }, {})
  playLogs.value.forEach((item) => {
    grouped[item.playDate] = (grouped[item.playDate] || 0) + (item.playCount || 0)
  })
  trendChart.setOption({
    color: ['#39d6ff'],
    grid: { left: 32, right: 18, top: 28, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: days.map((item) => item.slice(5)),
      axisLine: { lineStyle: { color: 'rgba(141, 212, 255, 0.25)' } },
      axisLabel: { color: '#9fc8dc' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(141, 212, 255, 0.1)' } },
      axisLabel: { color: '#9fc8dc' }
    },
    series: [
      {
        name: '播放次数',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(57, 214, 255, 0.38)' },
            { offset: 1, color: 'rgba(57, 214, 255, 0.02)' }
          ])
        },
        data: days.map((day) => grouped[day])
      }
    ]
  })
}

function renderAdChart() {
  if (!adChartRef.value) {
    return
  }
  adChart = adChart || echarts.init(adChartRef.value)
  const statusValues = ['draft', 'submitted', 'approved', 'rejected']
  adChart.setOption({
    color: ['#6b7cff', '#ffb84d', '#44d17a', '#ff6f7d'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#a9ccdc' } },
    series: [
      {
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '44%'],
        label: { color: '#dff7ff' },
        data: statusValues.map((status) => ({
          name: adStatusMap[status]?.label || status,
          value: ads.value.filter((item) => item.status === status).length
        }))
      }
    ]
  })
}

function renderDeviceChart() {
  if (!deviceChartRef.value) {
    return
  }
  deviceChart = deviceChart || echarts.init(deviceChartRef.value)
  deviceChart.setOption({
    color: ['#44d17a', '#7f92a3', '#ff6f7d'],
    grid: { left: 28, right: 18, top: 20, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['在线', '离线', '故障'],
      axisLine: { lineStyle: { color: 'rgba(141, 212, 255, 0.25)' } },
      axisLabel: { color: '#9fc8dc' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(141, 212, 255, 0.1)' } },
      axisLabel: { color: '#9fc8dc' }
    },
    series: [
      {
        type: 'bar',
        barWidth: 28,
        itemStyle: { borderRadius: [8, 8, 0, 0] },
        data: [
          devices.value.filter((item) => item.onlineStatus === 'online').length,
          devices.value.filter((item) => item.onlineStatus === 'offline').length,
          devices.value.filter((item) => item.faultStatus === 'fault').length
        ]
      }
    ]
  })
}

function resizeCharts() {
  trendChart?.resize()
  adChart?.resize()
  deviceChart?.resize()
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  adChart?.dispose()
  deviceChart?.dispose()
})
</script>

<style scoped>
.hero-panel {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
  padding: 22px;
  overflow: hidden;
  border: 1px solid rgba(83, 229, 255, 0.18);
  border-radius: 18px;
  background:
    radial-gradient(circle at 12% 20%, rgba(83, 229, 255, 0.16), transparent 34%),
    linear-gradient(135deg, rgba(8, 32, 45, 0.96), rgba(3, 14, 22, 0.88));
}

.hero-panel::after {
  position: absolute;
  inset: auto -80px -120px auto;
  width: 260px;
  height: 260px;
  border-radius: 999px;
  background: rgba(75, 203, 255, 0.1);
  filter: blur(4px);
  content: '';
}

.hero-panel h3 {
  margin: 4px 0 8px;
  color: #f4fbff;
  font-size: 24px;
}

.hero-panel span,
.hero-metrics small {
  color: rgba(213, 240, 255, 0.66);
}

.hero-metrics {
  position: relative;
  display: grid;
  min-width: 140px;
  place-items: center;
  padding: 18px;
  border: 1px solid rgba(68, 209, 122, 0.22);
  border-radius: 16px;
  background: rgba(6, 28, 26, 0.72);
}

.hero-metrics strong {
  color: #54f28c;
  font-size: 34px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-card {
  min-height: 310px;
  padding: 18px;
  border: 1px solid rgba(83, 229, 255, 0.14);
  border-radius: 16px;
  background: rgba(5, 18, 26, 0.72);
}

.chart-card.wide {
  grid-column: span 2;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.card-head h3 {
  margin: 4px 0 0;
  color: #f4fbff;
  font-size: 18px;
}

.chart {
  height: 300px;
}

.chart.small {
  height: 250px;
}

.status-list,
.todo-list {
  display: grid;
  gap: 12px;
}

.status-row {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  padding: 14px;
  overflow: hidden;
  border: 1px solid rgba(83, 229, 255, 0.12);
  border-radius: 12px;
  background: rgba(2, 12, 18, 0.48);
}

.status-row span,
.todo-item span,
.todo-item small {
  color: rgba(213, 240, 255, 0.66);
}

.status-row strong,
.todo-item strong {
  color: #ffffff;
  font-size: 20px;
}

.status-row em {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 3px;
  background: linear-gradient(90deg, #39d6ff, transparent);
}

.todo-item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 10px;
  padding: 14px;
  border: 1px solid rgba(83, 229, 255, 0.12);
  border-radius: 12px;
  background: rgba(2, 12, 18, 0.48);
}

.todo-item small {
  grid-column: 1 / -1;
}

.todo-item.warning {
  border-color: rgba(255, 184, 77, 0.22);
}

.todo-item.danger {
  border-color: rgba(255, 111, 125, 0.24);
}

.todo-item.primary {
  border-color: rgba(57, 214, 255, 0.2);
}

@media (max-width: 1080px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.wide {
    grid-column: span 1;
  }
}

@media (max-width: 720px) {
  .hero-panel {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
