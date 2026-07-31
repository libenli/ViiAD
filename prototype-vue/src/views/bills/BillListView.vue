<template>
  <section>
    <div class="page-title">
      <div>
        <h1>账单结算</h1>
        <p class="page-subtitle">
          统一查看计划账单、结算状态和发票申请，并在右侧完成确认与支付登记。
        </p>
      </div>
    </div>

    <div v-if="readonlyHint" class="permission-note">
      {{ readonlyHint }}
    </div>

    <div class="card filter-card">
      <div class="filter-grid">
        <input v-model.trim="filters.keyword" class="input" placeholder="搜索账单 ID / 计划 ID" />
        <select v-model="filters.status" class="select">
          <option value="">全部状态</option>
          <option value="pending">待确认</option>
          <option value="confirmed">已确认</option>
          <option value="paid">已支付</option>
        </select>
        <input v-model.trim="filters.planId" class="input" placeholder="按计划 ID 筛选" />
        <input v-model.trim="filters.advertiserId" class="input" placeholder="按广告主 ID 筛选" />
      </div>
    </div>

    <div class="card-grid">
      <div class="card">
        <div>账单总数</div>
        <div class="metric-value">{{ filteredBills.length }}</div>
      </div>
      <div class="card">
        <div>待确认</div>
        <div class="metric-value">{{ pendingCount }}</div>
      </div>
      <div class="card">
        <div>已确认</div>
        <div class="metric-value">{{ confirmedCount }}</div>
      </div>
      <div class="card">
        <div>已支付</div>
        <div class="metric-value">{{ paidCount }}</div>
      </div>
    </div>

    <div v-if="filteredBills.length === 0" class="empty-state">
      当前筛选条件下没有账单记录，请调整账单状态、计划或广告主条件后再查看。
    </div>

    <div v-else class="content-columns">
      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>账单 ID</th>
              <th>计划 ID</th>
              <th>金额</th>
              <th>佣金</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="bill in pagedBills"
              :key="bill.id"
              :class="{ selected: selectedBill?.id === bill.id }"
              @click="selectedBillId = bill.id"
            >
              <td>{{ bill.id }}</td>
              <td>{{ bill.planId }}</td>
              <td>{{ bill.amount }}</td>
              <td>{{ bill.commission || '-' }}</td>
              <td>
                <span class="status" :class="statusClass(bill.status)">
                  {{ statusLabels[bill.status] }}
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
        <template v-if="selectedBill">
          <div class="drawer-head">
            <div>
              <h3>{{ selectedBill.id }}</h3>
              <p class="muted">计划 {{ selectedBill.planId }} / 广告主 {{ selectedBill.advertiserId }}</p>
            </div>
          </div>

          <table class="table compact">
            <tbody>
              <tr><th>账单状态</th><td>{{ statusLabels[selectedBill.status] }}</td></tr>
              <tr><th>金额</th><td>{{ selectedBill.amount }}</td></tr>
              <tr><th>佣金</th><td>{{ selectedBill.commission || '-' }}</td></tr>
              <tr><th>发票申请</th><td>{{ selectedBill.invoiceRequested ? '已申请' : '未申请' }}</td></tr>
              <tr><th>创建时间</th><td>{{ selectedBill.createdAt }}</td></tr>
            </tbody>
          </table>

          <div class="editor-panel">
            <h4>快捷操作</h4>
            <div class="toolbar-group">
              <button v-if="canOperate" class="btn secondary" @click="updateStatus('confirmed')">
                确认账单
              </button>
              <button v-if="canOperate" class="btn secondary" @click="updateStatus('paid')">
                登记支付
              </button>
            </div>
          </div>

          <div class="editor-panel">
            <h4>演示说明</h4>
            <p class="muted">
              这一侧面板适合展示：广告投放结束后，账单可以按计划快速对账、确认并沉淀支付记录。
            </p>
          </div>
        </template>

        <div v-else class="empty-state drawer-empty">
          请选择一条账单，在右侧查看结算详情和操作入口。
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
  pending: '待确认',
  confirmed: '已确认',
  paid: '已支付'
} as const

const filters = reactive({
  keyword: '',
  status: '',
  planId: '',
  advertiserId: ''
})

const selectedBillId = ref('')
const pageIndex = ref(1)
const pageSize = 6

const canOperate = computed(() =>
  ['finance', 'business', 'super_admin'].includes(authStore.role ?? '')
)

const readonlyHint = computed(() => {
  if (canOperate.value) {
    return '当前角色可直接完成账单确认与支付登记，适合演示结算闭环。'
  }
  return '当前角色在账单页以查看为主，如需执行结算动作，请切换到财务、商务或超级管理员账号。'
})

const filteredBills = computed(() =>
  workflow.bills.filter((item) => {
    const keyword = filters.keyword.toLowerCase()
    const hitKeyword =
      !keyword ||
      item.id.toLowerCase().includes(keyword) ||
      item.planId.toLowerCase().includes(keyword)
    const hitStatus = !filters.status || item.status === filters.status
    const hitPlan = !filters.planId || item.planId.includes(filters.planId)
    const hitAdvertiser = !filters.advertiserId || item.advertiserId.includes(filters.advertiserId)
    return hitKeyword && hitStatus && hitPlan && hitAdvertiser
  })
)

const pageCount = computed(() => Math.max(1, Math.ceil(filteredBills.value.length / pageSize)))
const pagedBills = computed(() =>
  filteredBills.value.slice((pageIndex.value - 1) * pageSize, pageIndex.value * pageSize)
)

const selectedBill = computed(
  () => filteredBills.value.find((item) => item.id === selectedBillId.value) ?? filteredBills.value[0]
)

const pendingCount = computed(() => filteredBills.value.filter((item) => item.status === 'pending').length)
const confirmedCount = computed(() =>
  filteredBills.value.filter((item) => item.status === 'confirmed').length
)
const paidCount = computed(() => filteredBills.value.filter((item) => item.status === 'paid').length)

watch(
  filteredBills,
  (list) => {
    if (!list.length) {
      selectedBillId.value = ''
      return
    }
    if (!list.find((item) => item.id === selectedBillId.value)) {
      selectedBillId.value = list[0].id
    }
    if (pageIndex.value > pageCount.value) {
      pageIndex.value = pageCount.value
    }
  },
  { immediate: true }
)

function statusClass(status: keyof typeof statusLabels) {
  if (status === 'paid') return 'status-confirmed'
  if (status === 'confirmed') return 'status-live'
  return 'status-pending'
}

function updateStatus(status: 'confirmed' | 'paid') {
  if (!selectedBill.value || !authStore.currentUser) return
  workflow.updateBillStatus(selectedBill.value.id, status)
  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: `bill_${status}`,
    targetType: 'bill',
    targetId: selectedBill.value.id,
    detail: `账单状态更新为 ${statusLabels[status]}`
  })
}
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.filter-grid {
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

.drawer-empty {
  min-height: 280px;
}
</style>
