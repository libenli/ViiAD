<template>
  <AppPage eyebrow="广告业务" title="投放计划" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('plan:edit')" type="primary" :icon="Plus" @click="router.push('/plans/create')">
        新建计划
      </el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="计划名称 / 编号" />
      </el-form-item>
      <el-form-item label="广告ID">
        <el-input-number v-model="query.adId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="排期状态">
        <el-select v-model="query.scheduleStatus" clearable placeholder="全部排期" style="width: 150px">
          <el-option v-for="(item, key) in scheduleStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="投放状态">
        <el-select v-model="query.deliveryStatus" clearable placeholder="全部投放" style="width: 150px">
          <el-option v-for="(item, key) in deliveryStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadPlans">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="planCode" label="计划编号" min-width="180" />
      <el-table-column prop="planName" label="计划名称" min-width="220" />
      <el-table-column prop="adId" label="广告ID" width="100" />
      <el-table-column prop="regionCode" label="投放区域" min-width="140" />
      <el-table-column label="投放周期" min-width="260">
        <template #default="{ row }">
          {{ row.startTime || '-' }} 至 {{ row.endTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="排期" width="110">
        <template #default="{ row }">
          <el-tag :type="scheduleStatusMap[row.scheduleStatus]?.type || 'info'" effect="dark">
            {{ scheduleStatusMap[row.scheduleStatus]?.label || row.scheduleStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="投放" width="110">
        <template #default="{ row }">
          <el-tag :type="deliveryStatusMap[row.deliveryStatus]?.type || 'info'" effect="dark">
            {{ deliveryStatusMap[row.deliveryStatus]?.label || row.deliveryStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" min-width="300" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/plans/${row.id}`)">详情</el-button>
          <el-button v-if="user.hasPermission('plan:edit') && canEdit(row)" link type="primary" @click="router.push(`/plans/${row.id}/edit`)">
            编辑
          </el-button>
          <el-button v-if="user.hasPermission('plan:schedule') && row.scheduleStatus === 'draft'" link type="warning" @click="handleSchedule(row.id)">
            提交排期
          </el-button>
          <el-button v-if="user.hasPermission('plan:delivery') && canStart(row)" link type="success" @click="handleStart(row.id)">
            启动
          </el-button>
          <el-button v-if="user.hasPermission('plan:delivery') && row.deliveryStatus === 'live'" link type="warning" @click="handlePause(row.id)">
            暂停
          </el-button>
          <el-button v-if="user.hasPermission('plan:delivery') && canFinish(row)" link type="danger" @click="handleFinish(row.id)">
            结束
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无投放计划，可先创建已审核广告和素材后再新建计划" />
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
  { label: '全部计划', value: total.value },
  { label: '草稿', value: records.value.filter((item) => item.scheduleStatus === 'draft').length },
  { label: '已排期', value: records.value.filter((item) => item.scheduleStatus === 'scheduled').length },
  { label: '投放中', value: records.value.filter((item) => item.deliveryStatus === 'live').length }
])

function canEdit(row: AdPlan) {
  return row.scheduleStatus === 'draft' && row.deliveryStatus !== 'live'
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
  ElMessage.success('计划已提交排期')
  loadPlans()
}

async function handleStart(id: number) {
  await ElMessageBox.confirm(
    '启动后系统会自动下发到计划设备：在线设备模拟成功并生成播放日志，离线设备模拟失败并生成工单。确认启动吗？',
    '启动投放',
    { type: 'warning' }
  )
  await startPlan(id)
  ElMessage.success('计划已启动，可到下发记录、数据报表、工单反馈查看联动结果')
  loadPlans()
}

async function handlePause(id: number) {
  await pausePlan(id)
  ElMessage.success('计划已暂停')
  loadPlans()
}

async function handleFinish(id: number) {
  await finishPlan(id)
  ElMessage.success('计划已结束')
  loadPlans()
}

onMounted(loadPlans)
</script>
