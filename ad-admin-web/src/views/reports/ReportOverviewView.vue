<template>
  <AppPage :eyebrow="locale.t('menu.reports')" :title="locale.t('page.reports.title')" :stats="stats">
    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.reports.adId')">
        <el-input-number v-model="query.adId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.reports.planId')">
        <el-input-number v-model="query.planId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.reports.deviceId')">
        <el-input-number v-model="query.deviceId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.reports.dateRange')">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          :start-placeholder="locale.t('page.reports.startDate')"
          :end-placeholder="locale.t('page.reports.endDate')"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadReports">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <div class="report-grid">
      <div class="report-panel">
        <span>{{ locale.t('page.reports.playLogs') }}</span>
        <strong>{{ overview.logCount }}</strong>
        <small>{{ locale.t('page.reports.playLogsHint') }}</small>
      </div>
      <div class="report-panel">
        <span>{{ locale.t('page.reports.totalDuration') }}</span>
        <strong>{{ formatDuration(overview.playDuration) }}</strong>
        <small>{{ locale.t('page.reports.totalDurationHint') }}</small>
      </div>
    </div>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="id" :label="locale.t('page.reports.logId')" width="90" />
      <el-table-column prop="playDate" :label="locale.t('page.reports.playDate')" width="130" />
      <el-table-column prop="adId" :label="locale.t('page.reports.adId')" width="100" />
      <el-table-column prop="planId" :label="locale.t('page.reports.planId')" width="100" />
      <el-table-column prop="materialId" :label="locale.t('page.reports.materialId')" width="100" />
      <el-table-column prop="deviceId" :label="locale.t('page.reports.deviceId')" width="100" />
      <el-table-column prop="playCount" :label="locale.t('page.reports.playCount')" width="120" />
      <el-table-column :label="locale.t('page.reports.playDuration')" width="130">
        <template #default="{ row }">{{ formatDuration(row.playDuration) }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.reports.source')" width="120">
        <template #default="{ row }">{{ getSourceTypeLabel(row.sourceType) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" :label="locale.t('page.reports.createTime')" min-width="170" />
      <template #empty>
        <el-empty :description="locale.t('page.reports.empty')" />
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
import { useLocaleStore } from '@/stores/locale'

const loading = ref(false)
const locale = useLocaleStore()
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
  { label: locale.t('page.reports.exposure'), value: overview.exposureCount },
  { label: locale.t('page.reports.playCount'), value: overview.playCount },
  { label: locale.t('page.reports.activeDevices'), value: overview.activeDeviceCount },
  { label: locale.t('page.reports.completionRate'), value: overview.completionRate }
])

function getSourceTypeLabel(value: string) {
  return locale.t(`status.sourceType.${value}`, sourceTypeMap[value] || value)
}

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
