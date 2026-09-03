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
    <el-descriptions v-else-if="plan" :column="2" border>
      <el-descriptions-item :label="locale.t('page.plans.code')">{{ plan.planCode }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.plans.name')">{{ plan.planName }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.plans.relatedAdId')">{{ plan.adId }}</el-descriptions-item>
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
      <el-descriptions-item :label="locale.t('page.reports.materialId')">{{ (plan.materialIds || []).join(', ') || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.reports.deviceId')">{{ (plan.deviceIds || []).join(', ') || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.plans.operatorId')">{{ plan.operatorId || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="locale.t('page.plans.createTime')">{{ plan.createTime || '-' }}</el-descriptions-item>
    </el-descriptions>
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
  fetchPlanDetail,
  finishPlan,
  pausePlan,
  schedulePlan,
  scheduleStatusMap,
  startPlan,
  type AdPlan
} from '@/api/plans'
import { useLocaleStore } from '@/stores/locale'

const route = useRoute()
const router = useRouter()
const locale = useLocaleStore()
const user = useUserStore()
const loading = ref(false)
const acting = ref(false)
const plan = ref<AdPlan>()

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

const canEdit = computed(() => Boolean(plan.value && ['draft', 'scheduled'].includes(plan.value.scheduleStatus) && !['live', 'finished'].includes(plan.value.deliveryStatus)))
const canStart = computed(() => plan.value?.scheduleStatus === 'scheduled' && ['not_started', 'paused'].includes(plan.value.deliveryStatus))
const canFinish = computed(() => plan.value?.scheduleStatus === 'scheduled' && ['not_started', 'live', 'paused'].includes(plan.value.deliveryStatus))

async function loadDetail() {
  loading.value = true
  try {
    const result = await fetchPlanDetail(Number(route.params.id))
    plan.value = result.data
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
    ElMessage.success(actionMap[action].message)
  } finally {
    acting.value = false
  }
}

onMounted(loadDetail)
</script>
