<template>
  <AppPage eyebrow="广告业务" title="投放计划详情" :stats="stats">
    <template #actions>
      <el-button @click="router.push('/plans')">返回列表</el-button>
      <el-button v-if="plan && user.hasPermission('plan:edit') && canEdit" type="primary" @click="router.push(`/plans/${plan.id}/edit`)">编辑</el-button>
      <el-button v-if="user.hasPermission('plan:schedule') && plan?.scheduleStatus === 'draft'" type="warning" :loading="acting" @click="handleAction('schedule')">
        提交排期
      </el-button>
      <el-button v-if="plan && user.hasPermission('plan:delivery') && canStart" type="success" :loading="acting" @click="handleAction('start')">
        启动投放
      </el-button>
      <el-button v-if="user.hasPermission('plan:delivery') && plan?.deliveryStatus === 'live'" type="warning" :loading="acting" @click="handleAction('pause')">
        暂停
      </el-button>
      <el-button v-if="plan && user.hasPermission('plan:delivery') && canFinish" type="danger" :loading="acting" @click="handleAction('finish')">
        结束
      </el-button>
    </template>

    <el-skeleton v-if="loading" :rows="8" animated />
    <el-descriptions v-else-if="plan" :column="2" border>
      <el-descriptions-item label="计划编号">{{ plan.planCode }}</el-descriptions-item>
      <el-descriptions-item label="计划名称">{{ plan.planName }}</el-descriptions-item>
      <el-descriptions-item label="关联广告ID">{{ plan.adId }}</el-descriptions-item>
      <el-descriptions-item label="投放区域">{{ plan.regionCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="排期状态">
        <el-tag :type="scheduleStatusMap[plan.scheduleStatus]?.type || 'info'" effect="dark">
          {{ scheduleStatusMap[plan.scheduleStatus]?.label || plan.scheduleStatus }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="投放状态">
        <el-tag :type="deliveryStatusMap[plan.deliveryStatus]?.type || 'info'" effect="dark">
          {{ deliveryStatusMap[plan.deliveryStatus]?.label || plan.deliveryStatus }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="开始时间">{{ plan.startTime }}</el-descriptions-item>
      <el-descriptions-item label="结束时间">{{ plan.endTime }}</el-descriptions-item>
      <el-descriptions-item label="素材ID">{{ (plan.materialIds || []).join(', ') || '-' }}</el-descriptions-item>
      <el-descriptions-item label="设备ID">{{ (plan.deviceIds || []).join(', ') || '-' }}</el-descriptions-item>
      <el-descriptions-item label="运营人员ID">{{ plan.operatorId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ plan.createTime || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-empty v-else description="计划不存在" />
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

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const acting = ref(false)
const plan = ref<AdPlan>()

const stats = computed(() => [
  { label: '当前排期', value: plan.value ? scheduleStatusMap[plan.value.scheduleStatus]?.label || plan.value.scheduleStatus : '-' },
  { label: '投放状态', value: plan.value ? deliveryStatusMap[plan.value.deliveryStatus]?.label || plan.value.deliveryStatus : '-' },
  { label: '素材数量', value: plan.value?.materialIds?.length || 0 },
  { label: '设备数量', value: plan.value?.deviceIds?.length || 0 }
])

const canEdit = computed(() => plan.value?.scheduleStatus === 'draft' && plan.value.deliveryStatus !== 'live')
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
      schedule: { request: schedulePlan, message: '计划已提交排期' },
      start: { request: startPlan, message: '计划已启动投放' },
      pause: { request: pausePlan, message: '计划已暂停' },
      finish: { request: finishPlan, message: '计划已结束' }
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
