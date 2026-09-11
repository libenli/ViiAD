<template>
  <AppPage :eyebrow="locale.t('page.business')" :title="locale.t('page.plans.detailTitle')" :stats="stats">
    <template #actions>
      <el-button @click="router.push('/plans')">{{ locale.t('common.backToList') }}</el-button>
      <el-button v-if="plan && user.hasPermission('plan:edit') && canEdit" type="primary" @click="router.push(`/plans/${plan.id}/edit`)">{{ locale.t('common.edit') }}</el-button>
      <el-button v-if="user.hasPermission('plan:schedule') && plan?.scheduleStatus === 'draft'" type="warning" :loading="acting" @click="handleAction('schedule')">
        {{ locale.t('page.plans.submitSchedule') }}
      </el-button>
      <el-button v-if="plan && user.hasPermission('plan:delivery') && canStart" type="success" :loading="acting" @click="handleAction('start')">
        {{ locale.t('page.plans.start') }}
      </el-button>
      <el-button v-if="user.hasPermission('plan:delivery') && plan?.deliveryStatus === 'live'" type="warning" :loading="acting" @click="handleAction('pause')">
        {{ locale.t('page.plans.pause') }}
      </el-button>
      <el-button v-if="plan && user.hasPermission('plan:delivery') && canFinish" type="danger" :loading="acting" @click="handleAction('finish')">
        {{ locale.t('page.plans.finish') }}
      </el-button>
    </template>

    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else-if="plan">
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="locale.t('page.plans.code')">{{ plan.planCode }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.plans.name')">{{ plan.planName }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.plans.relatedAdId')">{{ getAdText(plan.adId) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.plans.region')">{{ plan.regionCode || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.plans.scheduleStatus')">
          <el-tag :type="scheduleStatusMap[plan.scheduleStatus]?.type || 'info'" effect="dark">
            {{ getScheduleLabel(plan.scheduleStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.plans.deliveryStatus')">
          <el-tag :type="deliveryStatusMap[plan.deliveryStatus]?.type || 'info'" effect="dark">
            {{ getDeliveryLabel(plan.deliveryStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.plans.startTime')">{{ plan.startTime }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.plans.endTime')">{{ plan.endTime }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.reports.materialId')">
          <div v-if="plan.materialIds?.length" class="linked-entity-list">
            <div v-for="materialId in plan.materialIds" :key="materialId" class="linked-entity-item">
              <strong>{{ getMaterialName(materialId) }}</strong>
              <span>{{ getMaterialCode(materialId) }}</span>
            </div>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.reports.deviceId')">
          <div v-if="plan.deviceIds?.length" class="linked-entity-list">
            <div v-for="deviceId in plan.deviceIds" :key="deviceId" class="linked-entity-item">
              <strong>{{ getDeviceName(deviceId) }}</strong>
              <span>{{ getDeviceCode(deviceId) }}</span>
            </div>
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.plans.operatorId')">{{ getUserText(plan.operatorId) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.plans.createTime')">{{ plan.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <section class="receipt-section">
        <div class="receipt-head">
          <div>
            <!-- <p class="eyebrow">{{ locale.t('page.plans.receiptEyebrow') }}</p> -->
            <h3>{{ locale.t('page.plans.receiptTitle') }}</h3>
          </div>
          <span>{{ locale.t('page.plans.receiptCount').replace('{count}', String(deviceReceipts.length)) }}</span>
        </div>
        <el-table :data="deviceReceipts" class="data-table" row-key="deviceId">
          <el-table-column :label="locale.t('page.devices.name')" min-width="190">
            <template #default="{ row }">
              <div class="entity-cell">
                <strong>{{ row.deviceName }}</strong>
                <span>{{ row.deviceCode }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="locale.t('page.devices.online')" width="110">
            <template #default="{ row }">
              <el-tag :type="onlineStatusMap[row.onlineStatus]?.type || 'info'" effect="dark">
                {{ getOnlineLabel(row.onlineStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="locale.t('page.deliveries.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="getReceiptDeliveryType(row.deliveryStatus)" effect="dark">
                {{ getReceiptDeliveryLabel(row.deliveryStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="locale.t('page.plans.playStatus')" width="130">
            <template #default="{ row }">
              <el-tag :type="getPlayStatusType(row.playStatus)" effect="dark">
                {{ getPlayStatusLabel(row.playStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="requestId" :label="locale.t('page.deliveries.requestId')" min-width="180" show-overflow-tooltip />
          <el-table-column prop="ackTime" :label="locale.t('page.deliveries.ackTime')" min-width="170" />
          <el-table-column prop="ackMessage" :label="locale.t('page.deliveries.ackMessage')" min-width="220" show-overflow-tooltip />
          <el-table-column prop="lastPlayTime" :label="locale.t('page.plans.lastPlayTime')" min-width="170" />
          <el-table-column prop="playErrorMessage" :label="locale.t('page.plans.playError')" min-width="180" show-overflow-tooltip />
          <el-table-column prop="playCount" :label="locale.t('page.reports.playCount')" width="110" />
          <template #empty>
            <el-empty :description="locale.t('page.plans.receiptEmpty')" />
          </template>
        </el-table>
      </section>
    </template>
    <el-empty v-else :description="locale.t('page.plans.planNotFound')" />
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppPage from '@/components/AppPage.vue'
import { useUserStore } from '@/stores/user'
import {
  deliveryStatusMap,
  fetchPlanDeviceReceipts,
  fetchPlanDetail,
  finishPlan,
  pausePlan,
  schedulePlan,
  scheduleStatusMap,
  startPlan,
  type AdPlan,
  type AdPlanDeviceReceipt
} from '@/api/plans'
import { fetchAdDetail, type AdOrder } from '@/api/ads'
import { fetchDevices, onlineStatusMap, type AdDevice } from '@/api/devices'
import { fetchMaterials, type AdMaterial } from '@/api/materials'
import { fetchSystemUsers, type SysUser } from '@/api/system'
import { useLocaleStore } from '@/stores/locale'

const route = useRoute()
const router = useRouter()
const locale = useLocaleStore()
const user = useUserStore()
const loading = ref(false)
const acting = ref(false)
const plan = ref<AdPlan>()
const ad = ref<AdOrder>()
const materialOptions = ref<AdMaterial[]>([])
const deviceOptions = ref<AdDevice[]>([])
const deviceReceipts = ref<AdPlanDeviceReceipt[]>([])
const userOptions = ref<SysUser[]>([])

const stats = computed(() => [
  { label: locale.t('page.plans.currentSchedule'), value: plan.value ? getScheduleLabel(plan.value.scheduleStatus) : '-' },
  { label: locale.t('page.plans.deliveryStatus'), value: plan.value ? getDeliveryLabel(plan.value.deliveryStatus) : '-' },
  { label: locale.t('page.plans.materialCount'), value: plan.value?.materialIds?.length || 0 },
  { label: locale.t('page.plans.deviceCount'), value: plan.value?.deviceIds?.length || 0 }
])

function getScheduleLabel(value: string) {
  return locale.t(`status.schedule.${value}`, scheduleStatusMap[value]?.label || value)
}

function getDeliveryLabel(value: string) {
  return locale.t(`status.delivery.${value}`, deliveryStatusMap[value]?.label || value)
}

function getOnlineLabel(value?: string) {
  return value ? locale.t(`status.online.${value}`, onlineStatusMap[value]?.label || value) : '-'
}

function getReceiptDeliveryType(value?: string) {
  if (value === 'success') {
    return 'success'
  }
  if (value === 'failed') {
    return 'danger'
  }
  if (value === 'pending') {
    return 'warning'
  }
  return 'info'
}

function getReceiptDeliveryLabel(value?: string) {
  return locale.t(`status.deliveryReceipt.${value || 'not_delivered'}`, value || '-')
}

function getPlayStatusType(value?: string) {
  if (value === 'playing') {
    return 'success'
  }
  if (value === 'played') {
    return 'primary'
  }
  if (value === 'waiting_play') {
    return 'warning'
  }
  if (value === 'blocked') {
    return 'danger'
  }
  return 'info'
}

function getPlayStatusLabel(value?: string) {
  return locale.t(`status.playStatus.${value || 'not_started'}`, value || '-')
}

function getAdText(adId?: number) {
  const current = ad.value
  if (current && current.id === adId) {
    return `${current.adName} / ${current.adCode}`
  }
  return adId ? `#${adId}` : '-'
}

function findMaterial(materialId?: number) {
  return materialOptions.value.find((item) => item.id === materialId)
}

function findDevice(deviceId?: number) {
  return deviceOptions.value.find((item) => item.id === deviceId)
}

function findUser(userId?: number) {
  return userOptions.value.find((item) => item.id === userId)
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

function getUserText(userId?: number) {
  const item = findUser(userId)
  return item ? `${item.realName} / ${item.username}` : (userId ? `#${userId}` : '-')
}

async function loadReferenceData(adId: number) {
  try {
    const [adResult, materials, devices, users] = await Promise.all([
      fetchAdDetail(adId),
      fetchMaterials({ page: 1, size: 500 }),
      fetchDevices({ page: 1, size: 500 }),
      fetchSystemUsers({ page: 1, size: 500 })
    ])
    ad.value = adResult.data
    materialOptions.value = materials.data.records
    deviceOptions.value = devices.data.records
    userOptions.value = users.data.records
  } catch {
    ad.value = undefined
    materialOptions.value = []
    deviceOptions.value = []
    userOptions.value = []
  }
}

async function loadDeviceReceipts(planId: number) {
  try {
    const result = await fetchPlanDeviceReceipts(planId)
    deviceReceipts.value = result.data
  } catch {
    deviceReceipts.value = []
  }
}

const canEdit = computed(() => Boolean(plan.value && ['draft', 'scheduled'].includes(plan.value.scheduleStatus) && !['live', 'finished'].includes(plan.value.deliveryStatus)))
const canStart = computed(() => plan.value?.scheduleStatus === 'scheduled' && ['not_started', 'paused'].includes(plan.value.deliveryStatus))
const canFinish = computed(() => plan.value?.scheduleStatus === 'scheduled' && ['not_started', 'live', 'paused'].includes(plan.value.deliveryStatus))

async function loadDetail() {
  loading.value = true
  try {
    const result = await fetchPlanDetail(Number(route.params.id))
    plan.value = result.data
    await Promise.all([loadReferenceData(result.data.adId), loadDeviceReceipts(result.data.id)])
  } finally {
    loading.value = false
  }
}

async function handleAction(action: 'schedule' | 'start' | 'pause' | 'finish') {
  if (!plan.value) {
    return
  }
  acting.value = true
  try {
    const actionMap = {
      schedule: { request: schedulePlan, message: locale.t('page.plans.scheduled') },
      start: { request: startPlan, message: locale.t('page.plans.startedDelivery') },
      pause: { request: pausePlan, message: locale.t('page.plans.paused') },
      finish: { request: finishPlan, message: locale.t('page.plans.finished') }
    }
    const result = await actionMap[action].request(plan.value.id)
    plan.value = result.data
    await loadDeviceReceipts(result.data.id)
    ElMessage.success(actionMap[action].message)
  } finally {
    acting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.receipt-section {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid rgba(83, 229, 255, 0.14);
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(6, 28, 39, 0.86), rgba(3, 14, 22, 0.72)),
    rgba(5, 18, 26, 0.68);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.receipt-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.receipt-head h3 {
  margin: 4px 0 0;
  color: #f4fbff;
  font-size: 18px;
  line-height: 1.35;
}

.receipt-head span {
  color: rgba(213, 240, 255, 0.68);
  font-size: 14px;
}
</style>
