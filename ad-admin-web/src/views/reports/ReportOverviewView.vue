<template>
  <AppPage eyebrow="数据报表" title="报表概览" :stats="stats">
    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="广告ID">
        <el-input-number v-model="query.adId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="计划ID">
        <el-input-number v-model="query.planId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="设备ID">
        <el-input-number v-model="query.deviceId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadReports">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="report-grid">
      <div class="report-panel">
        <span>播放日志</span>
        <strong>{{ overview.logCount }}</strong>
        <small>成功下发后自动生成</small>
      </div>
      <div class="report-panel">
        <span>总播放时长</span>
        <strong>{{ formatDuration(overview.playDuration) }}</strong>
        <small>按设备播放日志聚合</small>
      </div>
    </div>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="id" label="日志ID" width="90" />
      <el-table-column prop="playDate" label="播放日期" width="130" />
      <el-table-column prop="adId" label="广告ID" width="100" />
      <el-table-column prop="planId" label="计划ID" width="100" />
      <el-table-column prop="materialId" label="素材ID" width="100" />
      <el-table-column prop="deviceId" label="设备ID" width="100" />
      <el-table-column prop="playCount" label="播放次数" width="120" />
      <el-table-column label="播放时长" width="130">
        <template #default="{ row }">{{ formatDuration(row.playDuration) }}</template>
      </el-table-column>
      <el-table-column label="来源" width="120">
        <template #default="{ row }">{{ sourceTypeMap[row.sourceType] || row.sourceType }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="生成时间" min-width="170" />
      <template #empty>
        <el-empty description="暂无播放日志，可先在下发记录中将待下发记录模拟为成功" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadPlayLogs"
        @size-change="loadPlayLogs"
      />
    </div>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { fetchPlayLogs, fetchReportOverview, sourceTypeMap, type PlayLog, type ReportOverview } from '@/api/reports'

const loading = ref(false)
const records = ref<PlayLog[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | ''>('')
const overview = reactive<ReportOverview>({
  playCount: 0,
  exposureCount: 0,
  activeDeviceCount: 0,
  playDuration: 0,
  logCount: 0,
  completionRate: '0%'
})

const query = reactive({
  adId: undefined as number | undefined,
  planId: undefined as number | undefined,
  deviceId: undefined as number | undefined,
  startDate: '',
  endDate: '',
  page: 1,
  size: 10
})

const stats = computed(() => [
  { label: '曝光量', value: overview.exposureCount },
  { label: '播放次数', value: overview.playCount },
  { label: '活跃设备', value: overview.activeDeviceCount },
  { label: '完成率', value: overview.completionRate }
])

function syncDateRange() {
  query.startDate = Array.isArray(dateRange.value) ? dateRange.value[0] : ''
  query.endDate = Array.isArray(dateRange.value) ? dateRange.value[1] : ''
}

function formatDuration(seconds?: number) {
  const value = seconds || 0
  if (value < 60) {
    return `${value}s`
  }
  const minutes = Math.floor(value / 60)
  const rest = value % 60
  return rest ? `${minutes}m ${rest}s` : `${minutes}m`
}

async function loadOverview() {
  syncDateRange()
  const result = await fetchReportOverview(query)
  Object.assign(overview, result.data)
}

async function loadPlayLogs() {
  syncDateRange()
  loading.value = true
  try {
    const result = await fetchPlayLogs(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

async function loadReports() {
  query.page = 1
  await Promise.all([loadOverview(), loadPlayLogs()])
}

function resetQuery() {
  query.adId = undefined
  query.planId = undefined
  query.deviceId = undefined
  query.startDate = ''
  query.endDate = ''
  query.page = 1
  dateRange.value = ''
  loadReports()
}

onMounted(loadReports)
</script>

<style scoped>
.report-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.report-panel {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(8, 23, 32, 0.86);
}

.report-panel span,
.report-panel small {
  color: rgba(213, 240, 255, 0.62);
}

.report-panel strong {
  color: #ffffff;
  font-size: 28px;
}
</style>
