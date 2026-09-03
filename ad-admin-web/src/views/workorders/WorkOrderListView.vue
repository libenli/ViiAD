<template>
  <AppPage :eyebrow="locale.t('page.workorders.serviceLoop')" :title="locale.t('page.workorders.title')" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('workOrder:operate')" type="primary" :icon="Plus" @click="dialogVisible = true">{{ locale.t('page.workorders.create') }}</el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.workorders.keyword')">
        <el-input v-model="query.keyword" clearable :placeholder="locale.t('page.workorders.keywordPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.workorders.source')">
        <el-select v-model="query.sourceType" clearable :placeholder="locale.t('page.workorders.allSources')" style="width: 140px">
          <el-option v-for="(label, key) in workOrderSourceMap" :key="key" :label="getStringStatusLabel('workOrderSource', String(key), label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.workorders.priority')">
        <el-select v-model="query.priority" clearable :placeholder="locale.t('page.workorders.allPriorities')" style="width: 140px">
          <el-option v-for="(item, key) in workOrderPriorityMap" :key="key" :label="getStatusLabel('workOrderPriority', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.workorders.status')">
        <el-select v-model="query.status" clearable :placeholder="locale.t('page.workorders.allStatus')" style="width: 140px">
          <el-option v-for="(item, key) in workOrderStatusMap" :key="key" :label="getStatusLabel('workOrder', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadOrders">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="workNo" :label="locale.t('page.workorders.workNo')" min-width="180" />
      <el-table-column prop="title" :label="locale.t('page.workorders.workTitle')" min-width="220" show-overflow-tooltip />
      <el-table-column :label="locale.t('page.workorders.source')" width="110">
        <template #default="{ row }">{{ getStringStatusLabel('workOrderSource', row.sourceType, workOrderSourceMap[row.sourceType] || row.sourceType) }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.workorders.priority')" width="100">
        <template #default="{ row }">
          <el-tag :type="workOrderPriorityMap[row.priority]?.type || 'info'" effect="dark">
            {{ getStatusLabel('workOrderPriority', row.priority, workOrderPriorityMap[row.priority]?.label || row.priority) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.workorders.status')" width="110">
        <template #default="{ row }">
          <el-tag :type="workOrderStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ getStatusLabel('workOrder', row.status, workOrderStatusMap[row.status]?.label || row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assigneeId" :label="locale.t('page.workorders.assigneeId')" width="110" />
      <el-table-column prop="relatedPlanId" :label="locale.t('page.workorders.relatedPlan')" width="110" />
      <el-table-column prop="createTime" :label="locale.t('page.workorders.createTime')" min-width="170" />
      <el-table-column :label="locale.t('common.operation')" width="205" fixed="right" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ locale.t('common.detail') }}</el-button>
          <el-button v-if="user.hasPermission('workOrder:operate') && row.status === 'open'" link type="primary" @click="openAssign(row)">{{ locale.t('page.workorders.assign') }}</el-button>
          <el-button v-if="user.hasPermission('workOrder:operate') && (row.status === 'assigned' || row.status === 'open')" link type="warning" @click="handleStart(row.id)">{{ locale.t('page.workorders.start') }}</el-button>
          <el-button v-if="user.hasPermission('workOrder:operate') && row.status !== 'closed'" link type="success" @click="handleClose(row.id)">{{ locale.t('page.workorders.close') }}</el-button>
          <el-button v-if="user.hasPermission('workOrder:operate') && row.status === 'closed'" link type="success" @click="handleReopen(row.id)">{{ locale.t('page.workorders.reopen') }}</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="locale.t('page.workorders.empty')" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadOrders"
        @size-change="loadOrders"
      />
    </div>

    <el-drawer v-model="drawerVisible" :title="locale.t('page.workorders.drawerTitle')" size="460px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item :label="locale.t('page.workorders.workNo')">{{ current.workNo }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.workorders.workTitle')">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.workorders.source')">{{ getStringStatusLabel('workOrderSource', current.sourceType, workOrderSourceMap[current.sourceType] || current.sourceType) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.workorders.priority')">{{ getStatusLabel('workOrderPriority', current.priority, workOrderPriorityMap[current.priority]?.label || current.priority) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.workorders.status')">{{ getStatusLabel('workOrder', current.status, workOrderStatusMap[current.status]?.label || current.status) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.workorders.assigneeId')">{{ current.assigneeId || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.workorders.relatedPlan')">{{ current.relatedPlanId || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.workorders.content')">{{ current.content || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.workorders.closeTime')">{{ current.closeTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="locale.t('page.workorders.create')" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item :label="locale.t('page.workorders.workTitle')" required>
          <el-input v-model="form.title" :placeholder="locale.t('page.workorders.titlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.workorders.priority')">
          <el-select v-model="form.priority" class="wide-control">
            <el-option v-for="(item, key) in workOrderPriorityMap" :key="key" :label="getStatusLabel('workOrderPriority', String(key), item.label)" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item :label="locale.t('page.workorders.assigneeId')">
          <el-select v-model="form.assigneeId" class="wide-control" clearable filterable :loading="userLoading" :placeholder="locale.t('page.workorders.assigneePlaceholder')">
            <el-option v-for="item in assigneeOptions" :key="item.id" :label="getUserLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="locale.t('page.workorders.relatedPlan')">
          <el-select v-model="form.relatedPlanId" class="wide-control" clearable filterable :loading="planLoading" :placeholder="locale.t('page.workorders.planPlaceholder')">
            <el-option v-for="plan in planOptions" :key="plan.id" :label="getPlanLabel(plan)" :value="plan.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="locale.t('page.workorders.content')">
          <el-input v-model="form.content" type="textarea" :rows="4" :placeholder="locale.t('page.workorders.contentPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button v-if="user.hasPermission('workOrder:operate')" type="primary" :loading="saving" @click="handleCreate">{{ locale.t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" :title="locale.t('page.workorders.assignTitle')" width="460px">
      <el-form label-width="100px">
        <el-form-item :label="locale.t('page.workorders.assigneeId')" required>
          <el-select v-model="assignForm.assigneeId" class="wide-control" filterable :loading="userLoading" :placeholder="locale.t('page.workorders.assigneePlaceholder')">
            <el-option v-for="item in assigneeOptions" :key="item.id" :label="getUserLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">{{ locale.t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { useLocaleStore } from '@/stores/locale'
import { useUserStore } from '@/stores/user'
import {
  assignWorkOrder,
  closeWorkOrder,
  createWorkOrder,
  fetchWorkOrders,
  reopenWorkOrder,
  startWorkOrder,
  workOrderPriorityMap,
  workOrderSourceMap,
  workOrderStatusMap,
  type WorkOrder
} from '@/api/workorders'
import { fetchPlans, type AdPlan } from '@/api/plans'
import { fetchSystemUsers, type SysUser } from '@/api/system'

const loading = ref(false)
const locale = useLocaleStore()
const user = useUserStore()
const saving = ref(false)
const drawerVisible = ref(false)
const dialogVisible = ref(false)
const assignDialogVisible = ref(false)
const assigning = ref(false)
const records = ref<WorkOrder[]>([])
const current = ref<WorkOrder>()
const total = ref(0)
const userLoading = ref(false)
const planLoading = ref(false)
const assigneeOptions = ref<SysUser[]>([])
const planOptions = ref<AdPlan[]>([])

const query = reactive({
  keyword: '',
  sourceType: '',
  priority: '',
  status: '',
  page: 1,
  size: 10
})

const form = reactive({
  title: '',
  content: '',
  priority: 'normal',
  assigneeId: undefined as number | undefined,
  relatedPlanId: undefined as number | undefined
})

const assignForm = reactive({
  id: undefined as number | undefined,
  assigneeId: undefined as number | undefined
})

const stats = computed(() => [
  { label: getStatusLabel('workOrder', 'open', '待处理'), value: records.value.filter((item) => item.status === 'open').length },
  { label: getStatusLabel('workOrder', 'processing', '处理中'), value: records.value.filter((item) => item.status === 'processing').length },
  { label: getStatusLabel('workOrder', 'closed', '已关闭'), value: records.value.filter((item) => item.status === 'closed').length },
  { label: locale.t('page.workorders.highPriority'), value: records.value.filter((item) => item.priority === 'high').length }
])

function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function getStringStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function getUserLabel(item: SysUser) {
  return `${item.realName}（${item.username}）`
}

function getPlanLabel(plan: AdPlan) {
  return `${plan.planName}（${plan.planCode}）`
}

async function loadOrders() {
  loading.value = true
  try {
    const result = await fetchWorkOrders(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

async function loadAssignees() {
  userLoading.value = true
  try {
    const result = await fetchSystemUsers({ status: 'active', userType: 'platform', page: 1, size: 200 })
    assigneeOptions.value = result.data.records
  } finally {
    userLoading.value = false
  }
}

async function loadPlans() {
  planLoading.value = true
  try {
    const result = await fetchPlans({ page: 1, size: 200 })
    planOptions.value = result.data.records
  } finally {
    planLoading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.sourceType = ''
  query.priority = ''
  query.status = ''
  query.page = 1
  loadOrders()
}

function openDetail(row: WorkOrder) {
  current.value = row
  drawerVisible.value = true
}

function openAssign(row: WorkOrder) {
  assignForm.id = row.id
  assignForm.assigneeId = row.assigneeId
  assignDialogVisible.value = true
}

async function handleCreate() {
  if (!form.title) {
    ElMessage.warning(locale.t('page.workorders.titleRequired'))
    return
  }
  saving.value = true
  try {
    await createWorkOrder(form)
    ElMessage.success(locale.t('page.workorders.created'))
    dialogVisible.value = false
    Object.assign(form, { title: '', content: '', priority: 'normal', assigneeId: undefined, relatedPlanId: undefined })
    loadOrders()
  } finally {
    saving.value = false
  }
}

async function handleAssign() {
  if (!assignForm.id || !assignForm.assigneeId) {
    ElMessage.warning(locale.t('page.workorders.assigneeError'))
    return
  }
  assigning.value = true
  try {
    await assignWorkOrder(assignForm.id, assignForm.assigneeId)
    ElMessage.success(locale.t('page.workorders.assigned'))
    assignDialogVisible.value = false
    loadOrders()
  } finally {
    assigning.value = false
  }
}

async function handleStart(id: number) {
  await startWorkOrder(id)
  ElMessage.success(locale.t('page.workorders.started'))
  loadOrders()
}

async function handleClose(id: number) {
  const result = await ElMessageBox.prompt(locale.t('page.workorders.closePrompt'), locale.t('page.workorders.closeTitle'), {
    inputPlaceholder: locale.t('page.workorders.closePlaceholder')
  })
  await closeWorkOrder(id, result.value)
  ElMessage.success(locale.t('page.workorders.closed'))
  loadOrders()
}

async function handleReopen(id: number) {
  await ElMessageBox.confirm(locale.t('page.workorders.reopenConfirm'), locale.t('page.workorders.reopenTitle'), { type: 'warning' })
  await reopenWorkOrder(id)
  ElMessage.success(locale.t('page.workorders.reopened'))
  loadOrders()
}

onMounted(() => {
  loadOrders()
  loadAssignees()
  loadPlans()
})
</script>

<style scoped>
.wide-control {
  width: 100%;
}
</style>
