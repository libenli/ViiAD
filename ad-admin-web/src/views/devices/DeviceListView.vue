<template>
  <AppPage :eyebrow="locale.t('page.deviceDelivery')" :title="locale.t('page.devices.title')" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('device:manage')" type="primary" :icon="Plus" @click="router.push('/devices/create')">
        {{ locale.t('page.devices.create') }}
      </el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.devices.keyword')">
        <el-input v-model="query.keyword" clearable :placeholder="locale.t('page.devices.keywordPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.devices.buildingId')">
        <el-input-number v-model="query.buildingId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.devices.onlineStatus')">
        <el-select v-model="query.onlineStatus" clearable :placeholder="locale.t('page.devices.allOnline')" style="width: 140px">
          <el-option v-for="(item, key) in onlineStatusMap" :key="key" :label="getStatusLabel('online', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.devices.faultStatus')">
        <el-select v-model="query.faultStatus" clearable :placeholder="locale.t('page.devices.allFault')" style="width: 140px">
          <el-option v-for="(item, key) in faultStatusMap" :key="key" :label="getStatusLabel('fault', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.devices.deviceStatus')">
        <el-select v-model="query.status" clearable :placeholder="locale.t('page.devices.allStatus')" style="width: 140px">
          <el-option v-for="(item, key) in deviceStatusMap" :key="key" :label="getStatusLabel('device', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadDevices">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="deviceCode" :label="locale.t('page.devices.code')" min-width="180" />
      <el-table-column prop="deviceName" :label="locale.t('page.devices.name')" min-width="180" />
      <el-table-column prop="buildingId" :label="locale.t('page.devices.buildingId')" width="100" />
      <el-table-column prop="floorNo" :label="locale.t('page.devices.floor')" width="90" />
      <el-table-column prop="screenSize" :label="locale.t('page.devices.screen')" width="120" />
      <el-table-column prop="resolution" :label="locale.t('page.devices.resolution')" width="130" />
      <el-table-column prop="ipAddress" :label="locale.t('page.devices.ip')" min-width="140" />
      <el-table-column prop="provinceName" :label="locale.t('page.devices.province')" width="100" />
      <el-table-column prop="cityName" :label="locale.t('page.devices.city')" width="100" />
      <el-table-column prop="regionName" :label="locale.t('page.devices.region')" min-width="160" />
      <el-table-column :label="locale.t('page.devices.online')" width="90">
        <template #default="{ row }">
          <el-tag :type="onlineStatusMap[row.onlineStatus]?.type || 'info'" effect="dark">
            {{ getStatusLabel('online', row.onlineStatus, onlineStatusMap[row.onlineStatus]?.label || row.onlineStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.devices.fault')" width="90">
        <template #default="{ row }">
          <el-tag :type="faultStatusMap[row.faultStatus]?.type || 'info'" effect="dark">
            {{ getStatusLabel('fault', row.faultStatus, faultStatusMap[row.faultStatus]?.label || row.faultStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.devices.status')" width="90">
        <template #default="{ row }">
          <el-tag :type="deviceStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ getStatusLabel('device', row.status, deviceStatusMap[row.status]?.label || row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentPlanId" :label="locale.t('page.devices.currentPlan')" width="110" />
      <el-table-column prop="lastOnlineTime" :label="locale.t('page.devices.lastOnline')" min-width="170" />
      <el-table-column :label="locale.t('common.operation')" width="260" fixed="right" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/devices/${row.id}`)">{{ locale.t('common.detail') }}</el-button>
          <el-button v-if="user.hasPermission('device:manage')" link type="primary" @click="router.push(`/devices/${row.id}/edit`)">{{ locale.t('common.edit') }}</el-button>
          <el-button v-if="user.hasPermission('device:manage') && row.onlineStatus !== 'online' && row.status === 'active'" link type="success" @click="handleOnline(row.id)">
            {{ locale.t('page.devices.goOnline') }}
          </el-button>
          <el-button v-if="user.hasPermission('device:manage') && row.onlineStatus === 'online'" link type="warning" @click="handleOffline(row.id)">
            {{ locale.t('page.devices.goOffline') }}
          </el-button>
          <el-button v-if="user.hasPermission('device:manage') && row.faultStatus !== 'fault'" link type="danger" @click="handleFault(row.id)">
            {{ locale.t('page.devices.markFault') }}
          </el-button>
          <el-button v-if="user.hasPermission('device:manage') && row.faultStatus === 'fault'" link type="success" @click="handleRepair(row.id)">
            {{ locale.t('page.devices.repair') }}
          </el-button>
          <el-button v-if="user.hasPermission('device:manage') && row.status === 'active'" link type="warning" @click="handleDisable(row.id)">
            {{ locale.t('page.devices.disable') }}
          </el-button>
          <el-button v-else-if="user.hasPermission('device:manage')" link type="success" @click="handleEnable(row.id)">{{ locale.t('page.devices.enable') }}</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="locale.t('page.devices.empty')" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadDevices"
        @size-change="loadDevices"
      />
    </div>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { useLocaleStore } from '@/stores/locale'
import { useUserStore } from '@/stores/user'
import {
  deviceStatusMap,
  disableDevice,
  enableDevice,
  faultStatusMap,
  fetchDevices,
  markDeviceFault,
  onlineStatusMap,
  repairDevice,
  setDeviceOffline,
  setDeviceOnline,
  type AdDevice
} from '@/api/devices'

const router = useRouter()
const locale = useLocaleStore()
const user = useUserStore()
const loading = ref(false)
const records = ref<AdDevice[]>([])
const total = ref(0)

const query = reactive({
  keyword: '',
  buildingId: undefined as number | undefined,
  onlineStatus: '',
  faultStatus: '',
  status: '',
  page: 1,
  size: 10
})

const stats = computed(() => [
  { label: locale.t('page.devices.total'), value: total.value },
  { label: getStatusLabel('online', 'online', '在线'), value: records.value.filter((item) => item.onlineStatus === 'online').length },
  { label: getStatusLabel('online', 'offline', '离线'), value: records.value.filter((item) => item.onlineStatus === 'offline').length },
  { label: getStatusLabel('fault', 'fault', '故障'), value: records.value.filter((item) => item.faultStatus === 'fault').length }
])

function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

async function loadDevices() {
  loading.value = true
  try {
    const result = await fetchDevices(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.buildingId = undefined
  query.onlineStatus = ''
  query.faultStatus = ''
  query.status = ''
  query.page = 1
  loadDevices()
}

async function handleOnline(id: number) {
  await setDeviceOnline(id)
  ElMessage.success(locale.t('page.devices.onlineDone'))
  loadDevices()
}

async function handleOffline(id: number) {
  await setDeviceOffline(id)
  ElMessage.success(locale.t('page.devices.offlineDone'))
  loadDevices()
}

async function handleFault(id: number) {
  await ElMessageBox.confirm(locale.t('page.devices.faultConfirm'), locale.t('page.devices.faultConfirmTitle'), {
    type: 'warning'
  })
  await markDeviceFault(id)
  ElMessage.success(locale.t('page.devices.faultDone'))
  loadDevices()
}

async function handleRepair(id: number) {
  await repairDevice(id)
  ElMessage.success(locale.t('page.devices.repairDone'))
  loadDevices()
}

async function handleDisable(id: number) {
  await ElMessageBox.confirm(locale.t('page.devices.disableConfirm'), locale.t('page.devices.disableConfirmTitle'), {
    type: 'warning'
  })
  await disableDevice(id)
  ElMessage.success(locale.t('page.devices.disabledDone'))
  loadDevices()
}

async function handleEnable(id: number) {
  await enableDevice(id)
  ElMessage.success(locale.t('page.devices.enabledDone'))
  loadDevices()
}

onMounted(loadDevices)
</script>


