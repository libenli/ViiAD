<template>
  <AppPage eyebrow="设备投放" title="设备详情" :stats="stats">
    <template #actions>
      <el-button @click="router.push('/devices')">返回列表</el-button>
      <el-button v-if="device && user.hasPermission('device:manage')" type="primary" @click="router.push(`/devices/${device.id}/edit`)">编辑</el-button>
      <el-button v-if="user.hasPermission('device:manage') && device?.onlineStatus !== 'online' && device?.status === 'active'" type="success" :loading="acting" @click="handleAction('online')">
        上线
      </el-button>
      <el-button v-if="user.hasPermission('device:manage') && device?.onlineStatus === 'online'" type="warning" :loading="acting" @click="handleAction('offline')">
        离线
      </el-button>
      <el-button v-if="user.hasPermission('device:manage') && device?.faultStatus !== 'fault'" type="danger" :loading="acting" @click="handleAction('fault')">
        标记故障
      </el-button>
      <el-button v-if="user.hasPermission('device:manage') && device?.faultStatus === 'fault'" type="success" :loading="acting" @click="handleAction('repair')">
        恢复正常
      </el-button>
    </template>

    <el-skeleton v-if="loading" :rows="8" animated />
    <el-descriptions v-else-if="device" :column="2" border>
      <el-descriptions-item label="设备编号">{{ device.deviceCode }}</el-descriptions-item>
      <el-descriptions-item label="设备名称">{{ device.deviceName }}</el-descriptions-item>
      <el-descriptions-item label="楼宇ID">{{ device.buildingId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="楼层">{{ device.floorNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="屏幕尺寸">{{ device.screenSize || '-' }}</el-descriptions-item>
      <el-descriptions-item label="分辨率">{{ device.resolution || '-' }}</el-descriptions-item>
      <el-descriptions-item label="IP地址">{{ device.ipAddress || '-' }}</el-descriptions-item>
      <el-descriptions-item label="MAC地址">{{ device.macAddress || '-' }}</el-descriptions-item>
      <el-descriptions-item label="在线状态">
        <el-tag :type="onlineStatusMap[device.onlineStatus]?.type || 'info'" effect="dark">
          {{ onlineStatusMap[device.onlineStatus]?.label || device.onlineStatus }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="故障状态">
        <el-tag :type="faultStatusMap[device.faultStatus]?.type || 'info'" effect="dark">
          {{ faultStatusMap[device.faultStatus]?.label || device.faultStatus }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="设备状态">
        <el-tag :type="deviceStatusMap[device.status]?.type || 'info'" effect="dark">
          {{ deviceStatusMap[device.status]?.label || device.status }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="当前计划ID">{{ device.currentPlanId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="最后在线时间">{{ device.lastOnlineTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ device.createTime || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-empty v-else description="设备不存在" />
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

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const acting = ref(false)
const device = ref<AdDevice>()

const stats = computed(() => [
  { label: '在线状态', value: device.value ? onlineStatusMap[device.value.onlineStatus]?.label || device.value.onlineStatus : '-' },
  { label: '故障状态', value: device.value ? faultStatusMap[device.value.faultStatus]?.label || device.value.faultStatus : '-' },
  { label: '设备状态', value: device.value ? deviceStatusMap[device.value.status]?.label || device.value.status : '-' },
  { label: '当前计划', value: device.value?.currentPlanId || '-' }
])

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
    await ElMessageBox.confirm('确认将该设备标记为故障吗？', '标记故障', { type: 'warning' })
  }
  acting.value = true
  try {
    const actionMap = {
      online: { request: setDeviceOnline, message: '设备已上线' },
      offline: { request: setDeviceOffline, message: '设备已离线' },
      fault: { request: markDeviceFault, message: '设备已标记为故障' },
      repair: { request: repairDevice, message: '设备已恢复正常' }
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
