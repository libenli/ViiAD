<template>
  <AppPage :eyebrow="locale.t('menu.reports')" :title="locale.t('page.reports.title')" :stats="stats">
    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.reports.adId')">
        <el-select v-model="query.adId" clearable filterable :loading="adLoading" :placeholder="locale.t('page.reports.adPlaceholder')" style="width: 220px">
          <el-option v-for="ad in adOptions" :key="ad.id" :label="getAdLabel(ad)" :value="ad.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.reports.planId')">
        <el-select v-model="query.planId" clearable filterable :loading="planLoading" :placeholder="locale.t('page.reports.planPlaceholder')" style="width: 220px">
          <el-option v-for="plan in planOptions" :key="plan.id" :label="getPlanLabel(plan)" :value="plan.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.reports.deviceId')">
        <el-select v-model="query.deviceId" clearable filterable :loading="deviceLoading" :placeholder="locale.t('page.reports.devicePlaceholder')" style="width: 220px">
          <el-option v-for="device in deviceOptions" :key="device.id" :label="getDeviceLabel(device)" :value="device.id" />
        </el-select>
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
      <el-table-column :label="locale.t('page.reports.adId')" min-width="190">
        <template #default="{ row }">
          <div class="entity-cell">
            <strong>{{ getAdName(row.adId) }}</strong>
            <span>{{ getAdCode(row.adId) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.reports.planId')" min-width="190">
        <template #default="{ row }">
          <div class="entity-cell">
            <strong>{{ getPlanName(row.planId) }}</strong>
            <span>{{ getPlanCode(row.planId) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.reports.materialId')" min-width="190">
        <template #default="{ row }">
          <div class="entity-cell">
            <strong>{{ getMaterialName(row.materialId) }}</strong>
            <span>{{ getMaterialCode(row.materialId) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.reports.deviceId')" min-width="190">
        <template #default="{ row }">
          <div class="entity-cell">
            <strong>{{ getDeviceName(row.deviceId) }}</strong>
            <span>{{ getDeviceCode(row.deviceId) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.reports.playStatus')" width="130">
        <template #default="{ row }">
          <el-tag :type="getPlayStatusType(row.playStatus)" effect="dark">
            {{ getPlayStatusLabel(row.playStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="playStartTime" :label="locale.t('page.reports.playStartTime')" min-width="170" />
      <el-table-column prop="playEndTime" :label="locale.t('page.reports.playEndTime')" min-width="170" />
      <el-table-column prop="playCount" :label="locale.t('page.reports.playCount')" width="120" />
      <el-table-column :label="locale.t('page.reports.playDuration')" width="130">
        <template #default="{ row }">{{ formatDuration(row.playDuration) }}</template>
      </el-table-column>
      <el-table-column prop="errorMessage" :label="locale.t('page.reports.errorMessage')" min-width="190" show-overflow-tooltip />
      <el-table-column prop="requestId" :label="locale.t('page.deliveries.requestId')" min-width="180" show-overflow-tooltip />
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
import { fetchAds, type AdOrder } from '@/api/ads'
import { fetchDevices, type AdDevice } from '@/api/devices'
import { fetchMaterials, type AdMaterial } from '@/api/materials'
import { fetchPlans, type AdPlan } from '@/api/plans'
import { fetchPlayLogs, fetchReportOverview, sourceTypeMap, type PlayLog, type ReportOverview } from '@/api/reports'
import { useLocaleStore } from '@/stores/locale'

const loading = ref(false)
const adLoading = ref(false)
const planLoading = ref(false)
const deviceLoading = ref(false)
const locale = useLocaleStore()
const records = ref<PlayLog[]>([])
const adOptions = ref<AdOrder[]>([])
const planOptions = ref<AdPlan[]>([])
const materialOptions = ref<AdMaterial[]>([])
const deviceOptions = ref<AdDevice[]>([])
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

function getPlayStatusType(value?: string) {
  if (value === 'playing') {
    return 'success'
  }
  if (value === 'completed') {
    return 'primary'
  }
  if (value === 'failed') {
    return 'danger'
  }
  if (value === 'paused') {
    return 'warning'
  }
  return 'info'
}

function getPlayStatusLabel(value?: string) {
  return locale.t(`status.playStatus.${value || 'not_started'}`, value || '-')
}

function getAdLabel(ad: AdOrder) {
  return `${ad.adName}（${ad.adCode}）`
}

function getPlanLabel(plan: AdPlan) {
  return `${plan.planName}（${plan.planCode}）`
}

function getDeviceLabel(device: AdDevice) {
  return `${device.deviceName}（${device.deviceCode}）`
}

function findAd(adId?: number) {
  return adOptions.value.find((item) => item.id === adId)
}

function findPlan(planId?: number) {
  return planOptions.value.find((item) => item.id === planId)
}

function findMaterial(materialId?: number) {
  return materialOptions.value.find((item) => item.id === materialId)
}

function findDevice(deviceId?: number) {
  return deviceOptions.value.find((item) => item.id === deviceId)
}

function getAdName(adId?: number) {
  return findAd(adId)?.adName || (adId ? `#${adId}` : '-')
}

function getAdCode(adId?: number) {
  return findAd(adId)?.adCode || (adId ? `ID ${adId}` : '-')
}

function getPlanName(planId?: number) {
  return findPlan(planId)?.planName || (planId ? `#${planId}` : '-')
}

function getPlanCode(planId?: number) {
  return findPlan(planId)?.planCode || (planId ? `ID ${planId}` : '-')
}

function getMaterialName(materialId?: number) {
  return findMaterial(materialId)?.materialName || (materialId ? `#${materialId}` : '-')
}

function getMaterialCode(materialId?: number) {
  return findMaterial(materialId)?.materialCode || (materialId ? `ID ${materialId}` : '-')
}

function getDeviceName(deviceId?: number) {
  return findDevice(deviceId)?.deviceName || (deviceId ? `#${deviceId}` : '-')
}

function getDeviceCode(deviceId?: number) {
  return findDevice(deviceId)?.deviceCode || (deviceId ? `ID ${deviceId}` : '-')
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

async function loadReferenceData() {
  adLoading.value = true
  planLoading.value = true
  deviceLoading.value = true
  try {
    const [ads, plans, materials, devices] = await Promise.all([
      fetchAds({ page: 1, size: 500 }),
      fetchPlans({ page: 1, size: 500 }),
      fetchMaterials({ page: 1, size: 500 }),
      fetchDevices({ page: 1, size: 500 })
    ])
    adOptions.value = ads.data.records
    planOptions.value = plans.data.records
    materialOptions.value = materials.data.records
    deviceOptions.value = devices.data.records
  } finally {
    adLoading.value = false
    planLoading.value = false
    deviceLoading.value = false
  }
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

onMounted(() => {
  loadReferenceData()
  loadReports()
})
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
