<template>
  <section>
    <div class="page-title">
      <div>
        <h1>工单中心</h1>
        <p class="page-subtitle">
          统一查看故障、内容、账号和反馈类工单，并在右侧抽屉中完成分派与状态处理。
        </p>
      </div>
    </div>

    <div v-if="readonlyHint" class="permission-note">
      {{ readonlyHint }}
    </div>

    <div class="card filter-card">
      <div class="filter-grid">
        <input v-model.trim="filters.keyword" class="input" placeholder="搜索工单 ID / 来源 / 关联对象" />
        <select v-model="filters.status" class="select">
          <option value="">全部状态</option>
          <option value="open">待处理</option>
          <option value="processing">处理中</option>
          <option value="resolved">已完成</option>
        </select>
        <select v-model="filters.priority" class="select">
          <option value="">全部优先级</option>
          <option value="high">高</option>
          <option value="medium">中</option>
          <option value="low">低</option>
        </select>
        <select v-model="filters.type" class="select">
          <option value="">全部类型</option>
          <option value="device_fault">设备故障</option>
          <option value="content_issue">内容问题</option>
          <option value="account_issue">账号问题</option>
          <option value="feedback_issue">反馈处理</option>
        </select>
      </div>
    </div>

    <div class="card-grid">
      <div class="card">
        <div>工单总数</div>
        <div class="metric-value">{{ filteredWorkorders.length }}</div>
      </div>
      <div class="card">
        <div>待处理</div>
        <div class="metric-value">{{ openCount }}</div>
      </div>
      <div class="card">
        <div>处理中</div>
        <div class="metric-value">{{ processingCount }}</div>
      </div>
      <div class="card">
        <div>已完成</div>
        <div class="metric-value">{{ resolvedCount }}</div>
      </div>
    </div>

    <div v-if="filteredWorkorders.length === 0" class="empty-state">
      当前筛选条件下没有工单记录，请调整类型、优先级或状态条件后再查看。
    </div>

    <div v-else class="content-columns">
      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>工单 ID</th>
              <th>类型</th>
              <th>来源</th>
              <th>优先级</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in pagedWorkorders"
              :key="item.id"
              :class="{ selected: selectedWorkorder?.id === item.id }"
              @click="selectedWorkorderId = item.id"
            >
              <td>{{ item.id }}</td>
              <td>{{ typeLabels[item.type] }}</td>
              <td>{{ item.sourceRole }}</td>
              <td>{{ priorityLabels[item.priority] }}</td>
              <td>
                <span class="status" :class="statusClass(item.status)">
                  {{ statusLabels[item.status] }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="pageCount > 1" class="pagination">
          <button
            v-for="page in pageCount"
            :key="page"
            class="page-chip"
            :class="{ active: page === pageIndex }"
            @click="pageIndex = page"
          >
            {{ page }}
          </button>
        </div>
      </div>

      <div class="card drawer-card">
        <template v-if="selectedWorkorder">
          <div class="drawer-head">
            <div>
              <h3>{{ selectedWorkorder.id }}</h3>
              <p class="muted">
                {{ typeLabels[selectedWorkorder.type] }} / {{ selectedWorkorder.sourceRole }}
              </p>
            </div>
          </div>

          <table class="table compact">
            <tbody>
              <tr><th>状态</th><td>{{ statusLabels[selectedWorkorder.status] }}</td></tr>
              <tr><th>优先级</th><td>{{ priorityLabels[selectedWorkorder.priority] }}</td></tr>
              <tr><th>处理人</th><td>{{ selectedWorkorder.assigneeId || '-' }}</td></tr>
              <tr><th>关联对象</th><td>{{ selectedWorkorder.relatedId || '-' }}</td></tr>
              <tr><th>创建时间</th><td>{{ selectedWorkorder.createdAt }}</td></tr>
            </tbody>
          </table>

          <div class="editor-panel">
            <h4>快捷操作</h4>
            <div class="toolbar-group">
              <button v-if="canOperate" class="btn secondary" @click="changeStatus('processing')">
                开始处理
              </button>
              <button v-if="canOperate" class="btn secondary" @click="changeStatus('resolved')">
                标记完成
              </button>
              <button v-if="canOperate" class="btn secondary" @click="toggleEdit">
                {{ editing ? '取消编辑' : '编辑工单' }}
              </button>
            </div>
          </div>

          <div v-if="editing && editForm" class="editor-panel">
            <h4>右侧快速编辑</h4>
            <div class="editor-grid">
              <label>
                <span>优先级</span>
                <select v-model="editForm.priority" class="select">
                  <option value="high">高</option>
                  <option value="medium">中</option>
                  <option value="low">低</option>
                </select>
              </label>
              <label>
                <span>处理人 ID</span>
                <input v-model.trim="editForm.assigneeId" class="input" placeholder="例如：u3" />
              </label>
            </div>
            <div class="toolbar-group" style="margin-top: 12px">
              <button class="btn" @click="saveEdit">保存修改</button>
            </div>
          </div>
        </template>

        <div v-else class="empty-state drawer-empty">
          请选择一条工单，在右侧查看处理信息和操作入口。
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'

const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()

const statusLabels = {
  open: '待处理',
  processing: '处理中',
  resolved: '已完成'
} as const

const typeLabels = {
  device_fault: '设备故障',
  content_issue: '内容问题',
  account_issue: '账号问题',
  feedback_issue: '反馈处理'
} as const

const priorityLabels = {
  high: '高',
  medium: '中',
  low: '低'
} as const

const filters = reactive({
  keyword: '',
  status: '',
  priority: '',
  type: ''
})

const selectedWorkorderId = ref('')
const pageIndex = ref(1)
const pageSize = 6
const editing = ref(false)
const editForm = ref<null | { priority: 'high' | 'medium' | 'low'; assigneeId: string }>(null)

const canOperate = computed(() =>
  ['service', 'ops', 'area_admin', 'super_admin'].includes(authStore.role ?? '')
)

const readonlyHint = computed(() => {
  if (canOperate.value) {
    return '当前角色可直接处理工单，适合演示客服受理、运维介入和闭环完成。'
  }
  return '当前角色在工单页以查看为主，如需执行分派和处理动作，请切换到客服、运维、区域管理员或超级管理员账号。'
})

const filteredWorkorders = computed(() =>
  workflow.workorders.filter((item) => {
    const keyword = filters.keyword.toLowerCase()
    const hitKeyword =
      !keyword ||
      item.id.toLowerCase().includes(keyword) ||
      item.sourceRole.toLowerCase().includes(keyword) ||
      (item.relatedId || '').toLowerCase().includes(keyword)
    const hitStatus = !filters.status || item.status === filters.status
    const hitPriority = !filters.priority || item.priority === filters.priority
    const hitType = !filters.type || item.type === filters.type
    return hitKeyword && hitStatus && hitPriority && hitType
  })
)

const pageCount = computed(() => Math.max(1, Math.ceil(filteredWorkorders.value.length / pageSize)))
const pagedWorkorders = computed(() =>
  filteredWorkorders.value.slice((pageIndex.value - 1) * pageSize, pageIndex.value * pageSize)
)

const selectedWorkorder = computed(
  () =>
    filteredWorkorders.value.find((item) => item.id === selectedWorkorderId.value) ??
    filteredWorkorders.value[0]
)

const openCount = computed(() => filteredWorkorders.value.filter((item) => item.status === 'open').length)
const processingCount = computed(() =>
  filteredWorkorders.value.filter((item) => item.status === 'processing').length
)
const resolvedCount = computed(() =>
  filteredWorkorders.value.filter((item) => item.status === 'resolved').length
)

watch(
  filteredWorkorders,
  (list) => {
    if (!list.length) {
      selectedWorkorderId.value = ''
      editing.value = false
      editForm.value = null
      return
    }
    if (!list.find((item) => item.id === selectedWorkorderId.value)) {
      selectedWorkorderId.value = list[0].id
    }
    if (pageIndex.value > pageCount.value) {
      pageIndex.value = pageCount.value
    }
  },
  { immediate: true }
)

function statusClass(status: keyof typeof statusLabels) {
  if (status === 'resolved') return 'status-confirmed'
  if (status === 'processing') return 'status-live'
  return 'status-pending'
}

function changeStatus(status: 'processing' | 'resolved') {
  if (!selectedWorkorder.value || !authStore.currentUser) return
  workflow.updateWorkorderStatus(selectedWorkorder.value.id, status)
  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: `workorder_${status}`,
    targetType: 'workorder',
    targetId: selectedWorkorder.value.id,
    detail: `工单状态更新为 ${statusLabels[status]}`
  })
}

function toggleEdit() {
  if (!selectedWorkorder.value) return
  editing.value = !editing.value
  if (editing.value) {
    editForm.value = {
      priority: selectedWorkorder.value.priority,
      assigneeId: selectedWorkorder.value.assigneeId || ''
    }
  } else {
    editForm.value = null
  }
}

function saveEdit() {
  if (!selectedWorkorder.value || !editForm.value || !authStore.currentUser) return
  workflow.updateWorkorder(selectedWorkorder.value.id, {
    priority: editForm.value.priority,
    assigneeId: editForm.value.assigneeId || undefined
  })
  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: 'update_workorder',
    targetType: 'workorder',
    targetId: selectedWorkorder.value.id,
    detail: `更新工单优先级和处理人：${selectedWorkorder.value.id}`
  })
  toggleEdit()
}
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.filter-grid,
.editor-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.selected {
  background: rgba(43, 200, 255, 0.08);
  box-shadow: inset 2px 0 0 #2bc8ff;
}

.drawer-card {
  min-height: 420px;
}

.drawer-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.editor-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #edf2f7;
}

.compact th,
.compact td {
  padding: 10px 12px;
}

label span {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #425466;
}

.drawer-empty {
  min-height: 280px;
}
</style>
