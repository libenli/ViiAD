<template>
  <AppPage eyebrow="服务闭环" title="工单反馈" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('workOrder:operate')" type="primary" :icon="Plus" @click="dialogVisible = true">新建工单</el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="工单标题 / 编号" />
      </el-form-item>
      <el-form-item label="来源">
        <el-select v-model="query.sourceType" clearable placeholder="全部来源" style="width: 140px">
          <el-option v-for="(label, key) in workOrderSourceMap" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级">
        <el-select v-model="query.priority" clearable placeholder="全部优先级" style="width: 140px">
          <el-option v-for="(item, key) in workOrderPriorityMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option v-for="(item, key) in workOrderStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadOrders">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="workNo" label="工单编号" min-width="180" />
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column label="来源" width="110">
        <template #default="{ row }">{{ workOrderSourceMap[row.sourceType] || row.sourceType }}</template>
      </el-table-column>
      <el-table-column label="优先级" width="100">
        <template #default="{ row }">
          <el-tag :type="workOrderPriorityMap[row.priority]?.type || 'info'" effect="dark">
            {{ workOrderPriorityMap[row.priority]?.label || row.priority }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="workOrderStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ workOrderStatusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assigneeId" label="处理人ID" width="110" />
      <el-table-column prop="relatedPlanId" label="关联计划" width="110" />
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" min-width="260" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="user.hasPermission('workOrder:operate') && row.status === 'open'" link type="primary" @click="handleAssign(row.id)">分派</el-button>
          <el-button v-if="user.hasPermission('workOrder:operate') && (row.status === 'assigned' || row.status === 'open')" link type="warning" @click="handleStart(row.id)">处理</el-button>
          <el-button v-if="user.hasPermission('workOrder:operate') && row.status !== 'closed'" link type="success" @click="handleClose(row.id)">关闭</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无工单，下发失败会自动生成系统工单，也可以手动创建" />
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

    <el-drawer v-model="drawerVisible" title="工单详情" size="460px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="工单编号">{{ current.workNo }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ workOrderSourceMap[current.sourceType] || current.sourceType }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ workOrderPriorityMap[current.priority]?.label || current.priority }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ workOrderStatusMap[current.status]?.label || current.status }}</el-descriptions-item>
        <el-descriptions-item label="处理人ID">{{ current.assigneeId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联计划">{{ current.relatedPlanId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ current.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关闭时间">{{ current.closeTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="dialogVisible" title="新建工单" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入工单标题" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" class="wide-control">
            <el-option v-for="(item, key) in workOrderPriorityMap" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人ID">
          <el-input-number v-model="form.assigneeId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="关联计划">
          <el-input-number v-model="form.relatedPlanId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入问题描述或处理要求" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="user.hasPermission('workOrder:operate')" type="primary" :loading="saving" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { useUserStore } from '@/stores/user'
import {
  assignWorkOrder,
  closeWorkOrder,
  createWorkOrder,
  fetchWorkOrders,
  startWorkOrder,
  workOrderPriorityMap,
  workOrderSourceMap,
  workOrderStatusMap,
  type WorkOrder
} from '@/api/workorders'

const loading = ref(false)
const user = useUserStore()
const saving = ref(false)
const drawerVisible = ref(false)
const dialogVisible = ref(false)
const records = ref<WorkOrder[]>([])
const current = ref<WorkOrder>()
const total = ref(0)

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

const stats = computed(() => [
  { label: '待处理', value: records.value.filter((item) => item.status === 'open').length },
  { label: '处理中', value: records.value.filter((item) => item.status === 'processing').length },
  { label: '已关闭', value: records.value.filter((item) => item.status === 'closed').length },
  { label: '高优先级', value: records.value.filter((item) => item.priority === 'high').length }
])

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

async function handleCreate() {
  if (!form.title) {
    ElMessage.warning('请输入工单标题')
    return
  }
  saving.value = true
  try {
    await createWorkOrder(form)
    ElMessage.success('工单已创建')
    dialogVisible.value = false
    Object.assign(form, { title: '', content: '', priority: 'normal', assigneeId: undefined, relatedPlanId: undefined })
    loadOrders()
  } finally {
    saving.value = false
  }
}

async function handleAssign(id: number) {
  const result = await ElMessageBox.prompt('请输入处理人ID', '分派工单', {
    inputPattern: /^[1-9]\d*$/,
    inputErrorMessage: '请输入有效处理人ID'
  })
  await assignWorkOrder(id, Number(result.value))
  ElMessage.success('工单已分派')
  loadOrders()
}

async function handleStart(id: number) {
  await startWorkOrder(id)
  ElMessage.success('工单已进入处理中')
  loadOrders()
}

async function handleClose(id: number) {
  const result = await ElMessageBox.prompt('请输入处理结果', '关闭工单', {
    inputPlaceholder: '如：已重启终端并恢复下发'
  })
  await closeWorkOrder(id, result.value)
  ElMessage.success('工单已关闭')
  loadOrders()
}

onMounted(loadOrders)
</script>

<style scoped>
.wide-control {
  width: 100%;
}
</style>
