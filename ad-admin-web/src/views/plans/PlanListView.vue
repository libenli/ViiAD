<template>
  <AppPage :eyebrow="locale.t('page.business')" :title="locale.t('page.plans.title')" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('plan:edit')" type="primary" :icon="Plus" @click="router.push('/plans/create')">
        {{ locale.t('page.plans.create') }}
      </el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.plans.keyword')">
        <el-input v-model="query.keyword" clearable :placeholder="locale.t('page.plans.keywordPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.plans.adId')">
        <el-input-number v-model="query.adId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.plans.scheduleStatus')">
        <el-select v-model="query.scheduleStatus" clearable :placeholder="locale.t('page.plans.allSchedule')" style="width: 150px">
          <el-option v-for="(item, key) in scheduleStatusMap" :key="key" :label="getStatusLabel('schedule', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.plans.deliveryStatus')">
        <el-select v-model="query.deliveryStatus" clearable :placeholder="locale.t('page.plans.allDelivery')" style="width: 150px">
          <el-option v-for="(item, key) in deliveryStatusMap" :key="key" :label="getStatusLabel('delivery', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadPlans">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="planCode" :label="locale.t('page.plans.code')" min-width="180" />
      <el-table-column prop="planName" :label="locale.t('page.plans.name')" min-width="220" />
      <el-table-column prop="adId" :label="locale.t('page.plans.adId')" width="100" />
      <el-table-column prop="regionCode" :label="locale.t('page.plans.region')" min-width="140" />
      <el-table-column :label="locale.t('page.plans.period')" min-width="260">
        <template #default="{ row }">
          {{ row.startTime || '-' }} 至 {{ row.endTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.plans.schedule')" width="110">
        <template #default="{ row }">
          <el-tag :type="scheduleStatusMap[row.scheduleStatus]?.type || 'info'" effect="dark">
            {{ getStatusLabel('schedule', row.scheduleStatus, scheduleStatusMap[row.scheduleStatus]?.label || row.scheduleStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.plans.delivery')" width="110">
        <template #default="{ row }">
          <el-tag :type="deliveryStatusMap[row.deliveryStatus]?.type || 'info'" effect="dark">
            {{ getStatusLabel('delivery', row.deliveryStatus, deliveryStatusMap[row.deliveryStatus]?.label || row.deliveryStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="locale.t('page.plans.createTime')" min-width="170" />
      <el-table-column :label="locale.t('common.operation')" width="220" fixed="right" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/plans/${row.id}`)">{{ locale.t('common.detail') }}</el-button>
          <el-button v-if="user.hasPermission('plan:edit') && canEdit(row)" link type="primary" @click="router.push(`/plans/${row.id}/edit`)">
            {{ locale.t('common.edit') }}
          </el-button>
          <el-button v-if="user.hasPermission('plan:schedule') && row.scheduleStatus === 'draft'" link type="warning" @click="handleSchedule(row.id)">
            {{ locale.t('page.plans.submitSchedule') }}
          </el-button>
          <el-button v-if="user.hasPermission('plan:delivery') && canStart(row)" link type="success" @click="handleStart(row.id)">
            {{ locale.t('page.plans.start') }}
          </el-button>
          <el-button v-if="user.hasPermission('plan:delivery') && row.deliveryStatus === 'live'" link type="warning" @click="handlePause(row.id)">
            {{ locale.t('page.plans.pause') }}
          </el-button>
          <el-button v-if="user.hasPermission('plan:delivery') && canFinish(row)" link type="danger" @click="handleFinish(row.id)">
            {{ locale.t('page.plans.finish') }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="locale.t('page.plans.empty')" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadPlans"
        @size-change="loadPlans"
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
  deliveryStatusMap,
  fetchPlans,
  finishPlan,
  pausePlan,
  schedulePlan,
  scheduleStatusMap,
  startPlan,
  type AdPlan
} from '@/api/plans'

const router = useRouter()
const locale = useLocaleStore()
const user = useUserStore()
const loading = ref(false)
const records = ref<AdPlan[]>([])
const total = ref(0)

const query = reactive({
  keyword: '',
  adId: undefined as number | undefined,
  scheduleStatus: '',
  deliveryStatus: '',
  page: 1,
  size: 10
})

const stats = computed(() => [
  { label: locale.t('page.plans.total'), value: total.value },
  { label: getStatusLabel('schedule', 'draft', '草稿'), value: records.value.filter((item) => item.scheduleStatus === 'draft').length },
  { label: getStatusLabel('schedule', 'scheduled', '已排期'), value: records.value.filter((item) => item.scheduleStatus === 'scheduled').length },
  { label: getStatusLabel('delivery', 'live', '投放中'), value: records.value.filter((item) => item.deliveryStatus === 'live').length }
])

function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function canEdit(row: AdPlan) {
  return ['draft', 'scheduled'].includes(row.scheduleStatus) && !['live', 'finished'].includes(row.deliveryStatus)
}

function canStart(row: AdPlan) {
  return row.scheduleStatus === 'scheduled' && ['not_started', 'paused'].includes(row.deliveryStatus)
}

function canFinish(row: AdPlan) {
  return row.scheduleStatus === 'scheduled' && ['not_started', 'live', 'paused'].includes(row.deliveryStatus)
}

async function loadPlans() {
  loading.value = true
  try {
    const result = await fetchPlans(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.adId = undefined
  query.scheduleStatus = ''
  query.deliveryStatus = ''
  query.page = 1
  loadPlans()
}

async function handleSchedule(id: number) {
  await schedulePlan(id)
  ElMessage.success(locale.t('page.plans.scheduled'))
  loadPlans()
}

async function handleStart(id: number) {
  await ElMessageBox.confirm(
    locale.t('page.plans.startConfirm'),
    locale.t('page.plans.startConfirmTitle'),
    { type: 'warning' }
  )
  await startPlan(id)
  ElMessage.success(locale.t('page.plans.started'))
  loadPlans()
}

async function handlePause(id: number) {
  await pausePlan(id)
  ElMessage.success(locale.t('page.plans.paused'))
  loadPlans()
}

async function handleFinish(id: number) {
  await finishPlan(id)
  ElMessage.success(locale.t('page.plans.finished'))
  loadPlans()
}

onMounted(loadPlans)
</script>
