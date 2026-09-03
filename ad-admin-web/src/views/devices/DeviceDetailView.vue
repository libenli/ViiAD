<template>
  <AppPage :eyebrow="locale.t('page.deviceDelivery')" :title="locale.t('page.devices.detailTitle')" :stats="stats">
    <template #actions>
      <el-button @click="router.push('/devices')">{{ locale.t('common.backToList') }}</el-button>
      <el-button v-if="device && user.hasPermission('device:manage')" type="primary" @click="router.push(`/devices/${device.id}/edit`)">{{ locale.t('common.edit') }}</el-button>
      <el-button v-if="user.hasPermission('device:manage') && device?.onlineStatus !== 'online' && device?.status === 'active'" type="success" :loading="acting" @click="handleAction('online')">
        {{ locale.t('page.devices.goOnline') }}
      </el-button>
      <el-button v-if="user.hasPermission('device:manage') && device?.onlineStatus === 'online'" type="warning" :loading="acting" @click="handleAction('offline')">
        {{ locale.t('page.devices.goOffline') }}
      </el-button>
      <el-button v-if="user.hasPermission('device:manage') && device?.faultStatus !== 'fault'" type="danger" :loading="acting" @click="handleAction('fault')">
        {{ locale.t('page.devices.markFaultFull') }}
      </el-button>
      <el-button v-if="user.hasPermission('device:manage') && device?.faultStatus === 'fault'" type="success" :loading="acting" @click="handleAction('repair')">
        {{ locale.t('page.devices.repairFull') }}
      </el-button>
    </template>

    <el-skeleton v-if="loading" :rows="8" animated />
    <el-descriptions v-else-if="device" :column="2" border>
      <el-descriptions-item :label="locale.t('page.devices.code')">{{ device.deviceCode }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.name')">{{ device.deviceName }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.buildingId')">{{ device.buildingId || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.floor')">{{ device.floorNo || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.screenSize')">{{ device.screenSize || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.resolution')">{{ device.resolution || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.ip')">{{ device.ipAddress || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.mac')">{{ device.macAddress || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.onlineStatus')">
        <el-tag :type="onlineStatusMap[device.onlineStatus]?.type || 'info'" effect="dark">
          {{ getOnlineLabel(device.onlineStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.faultStatus')">
        <el-tag :type="faultStatusMap[device.faultStatus]?.type || 'info'" effect="dark">
          {{ getFaultLabel(device.faultStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.deviceStatus')">
        <el-tag :type="deviceStatusMap[device.status]?.type || 'info'" effect="dark">
          {{ getDeviceStatusLabel(device.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.currentPlanId')">{{ device.currentPlanId || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.lastOnlineTime')">{{ device.lastOnlineTime || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.devices.createTime')">{{ device.createTime || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-empty v-else :description="locale.t('page.devices.deviceNotFound')" />
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppPage from '@/components/AppPage.vue'
import { useUserStore } from '@/stores/user'
import {
  deviceStatusMap,
  faultStatusMap,
  fetchDeviceDetail,
  markDeviceFault,
  onlineStatusMap,
  repairDevice,
  setDeviceOffline,
  setDeviceOnline,
  type AdDevice
} from '@/api/devices'
import { useLocaleStore } from '@/stores/locale'

const route = useRoute()
const router = useRouter()
const locale = useLocaleStore()
const user = useUserStore()
const loading = ref(false)
const acting = ref(false)
const device = ref<AdDevice>()

const stats = computed(() => [
  { label: locale.t('page.devices.onlineStatus'), value: device.value ? getOnlineLabel(device.value.onlineStatus) : '-' },
  { label: locale.t('page.devices.faultStatus'), value: device.value ? getFaultLabel(device.value.faultStatus) : '-' },
  { label: locale.t('page.devices.deviceStatus'), value: device.value ? getDeviceStatusLabel(device.value.status) : '-' },
  { label: locale.t('page.devices.currentPlan'), value: device.value?.currentPlanId || '-' }
])

function getOnlineLabel(value: string) {
  return locale.t(`status.online.${value}`, onlineStatusMap[value]?.label || value)
}

function getFaultLabel(value: string) {
  return locale.t(`status.fault.${value}`, faultStatusMap[value]?.label || value)
}

function getDeviceStatusLabel(value: string) {
  return locale.t(`status.device.${value}`, deviceStatusMap[value]?.label || value)
}

async function loadDetail() {
  loading.value = true
  try {
    const result = await fetchDeviceDetail(Number(route.params.id))
    device.value = result.data
  } finally {
    loading.value = false
  }
}

async function handleAction(action: 'online' | 'offline' | 'fault' | 'repair') {
  if (!device.value) {
    return
  }
  if (action === 'fault') {
    await ElMessageBox.confirm(locale.t('page.devices.faultConfirm'), locale.t('page.devices.faultConfirmTitle'), { type: 'warning' })
  }
  acting.value = true
  try {
    const actionMap = {
      online: { request: setDeviceOnline, message: locale.t('page.devices.onlineDone') },
      offline: { request: setDeviceOffline, message: locale.t('page.devices.offlineDone') },
      fault: { request: markDeviceFault, message: locale.t('page.devices.faultDone') },
      repair: { request: repairDevice, message: locale.t('page.devices.repairDone') }
    }
    const result = await actionMap[action].request(device.value.id)
    device.value = result.data
    ElMessage.success(actionMap[action].message)
  } finally {
    acting.value = false
  }
}

onMounted(loadDetail)
</script>
