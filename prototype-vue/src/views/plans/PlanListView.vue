<template>
  <section>
    <div class="page-title">
      <div>
        <h1>投放计划</h1>
        <p class="page-subtitle">
          统一查看排期、设备、区域和定向配置，并在右侧操作面板快速调整计划状态。
        </p>
      </div>
      <RouterLink v-if="canCreate" class="btn" to="/plans/new">新建计划</RouterLink>
    </div>

    <div v-if="readonlyHint" class="permission-note">
      {{ readonlyHint }}
    </div>

    <div class="card filter-card">
      <div class="filter-grid">
        <input
          v-model.trim="filters.keyword"
          class="input"
          placeholder="搜索计划 ID / 广告 ID / 区域"
        />
        <select v-model="filters.status" class="select">
          <option value="">全部状态</option>
          <option value="pending_schedule">待排期</option>
          <option value="scheduled">已排期</option>
          <option value="live">投放中</option>
          <option value="paused">已暂停</option>
          <option value="finished">已结束</option>
        </select>
        <select v-model="filters.adId" class="select">
          <option value="">全部广告</option>
          <option v-for="ad in availableAds" :key="ad.id" :value="ad.id">{{ ad.id }}</option>
        </select>
        <input v-model.trim="filters.region" class="input" placeholder="按区域筛选" />
      </div>
    </div>

    <div class="card-grid">
      <div class="card">
        <div>计划总数</div>
        <div class="metric-value">{{ filteredPlans.length }}</div>
      </div>
      <div class="card">
        <div>待排期</div>
        <div class="metric-value">{{ pendingCount }}</div>
      </div>
      <div class="card">
        <div>已排期</div>
        <div class="metric-value">{{ scheduledCount }}</div>
      </div>
      <div class="card">
        <div>投放中</div>
        <div class="metric-value">{{ liveCount }}</div>
      </div>
    </div>

    <div v-if="filteredPlans.length === 0" class="empty-state">
      当前筛选条件下没有计划记录，请调整广告、区域或状态条件后再查看。
    </div>

    <div v-else class="content-columns">
      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>计划 ID</th>
              <th>广告 ID</th>
              <th>区域</th>
              <th>设备数</th>
              <th>投放时间</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="plan in filteredPlans"
              :key="plan.id"
              :class="{ selected: selectedPlan?.id === plan.id }"
              @click="selectedPlanId = plan.id"
            >
              <td><RouterLink :to="`/plans/${plan.id}`">{{ plan.id }}</RouterLink></td>
              <td>{{ plan.adId }}</td>
              <td>{{ plan.region }}</td>
              <td>{{ plan.deviceIds.length }}</td>
              <td>{{ plan.startAt.slice(0, 10) }} - {{ plan.endAt.slice(0, 10) }}</td>
              <td>
                <span class="status" :class="statusClass(plan.status)">
                  {{ statusLabels[plan.status] }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card drawer-card">
        <template v-if="selectedPlan">
          <div class="drawer-head">
            <div>
              <h3>{{ selectedPlan.id }}</h3>
              <p class="muted">{{ selectedPlan.adId }} / {{ selectedPlan.region }}</p>
            </div>
            <RouterLink class="btn secondary" :to="`/plans/${selectedPlan.id}`">进入详情</RouterLink>
          </div>

          <table class="table compact">
            <tbody>
              <tr><th>状态</th><td>{{ statusLabels[selectedPlan.status] }}</td></tr>
              <tr><th>楼宇范围</th><td>{{ selectedPlan.buildingIds.join('，') || '-' }}</td></tr>
              <tr><th>设备范围</th><td>{{ selectedPlan.deviceIds.join('，') || '-' }}</td></tr>
              <tr><th>时段</th><td>{{ selectedPlan.targeting.timeSlots.join('，') || '-' }}</td></tr>
              <tr><th>人群标签</th><td>{{ selectedPlan.targeting.audienceTags.join(' / ') || '-' }}</td></tr>
            </tbody>
          </table>

          <div class="editor-panel">
            <h4>快捷操作</h4>
            <div class="toolbar-group">
              <button v-if="canOperateSelected" class="btn secondary" @click="changeStatus('scheduled')">
                排期
              </button>
              <button v-if="canOperateSelected" class="btn secondary" @click="changeStatus('live')">
                上线
              </button>
              <button v-if="canOperateSelected" class="btn secondary" @click="changeStatus('paused')">
                暂停
              </button>
              <button v-if="canOperateSelected" class="btn secondary" @click="changeStatus('finished')">
                结束
              </button>
              <button v-if="canEditSelected" class="btn secondary" @click="toggleEdit">
                {{ editing ? '取消编辑' : '编辑计划' }}
              </button>
            </div>
          </div>

          <div v-if="editing && editForm" class="editor-panel">
            <h4>右侧快速编辑</h4>
            <div class="editor-grid">
              <label>
                <span>区域</span>
                <input v-model.trim="editForm.region" class="input" />
              </label>
              <label>
                <span>楼宇 ID</span>
                <input v-model.trim="editForm.buildingIds" class="input" />
              </label>
              <label>
                <span>设备 ID</span>
                <input v-model.trim="editForm.deviceIds" class="input" />
              </label>
              <label>
                <span>时段</span>
                <input v-model.trim="editForm.timeSlots" class="input" />
              </label>
              <label>
                <span>人群标签</span>
                <input v-model.trim="editForm.audienceTags" class="input" />
              </label>
              <label class="checkbox-label">
                <input v-model="editForm.lbsEnabled" type="checkbox" />
                <span>启用 LBS 定向</span>
              </label>
            </div>
            <div class="toolbar-group" style="margin-top: 12px">
              <button class="btn" @click="saveEdit">保存修改</button>
            </div>
          </div>
        </template>

        <div v-else class="empty-state drawer-empty">
          请选择一条计划，在右侧查看排期信息和操作入口。
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'
import { canCreateByRole, canEditPlan, visiblePlansByRole } from '@/config/entity-access'

const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const route = useRoute()

const statusLabels = {
  draft: '草稿',
  pending_schedule: '待排期',
  scheduled: '已排期',
  live: '投放中',
  paused: '已暂停',
  finished: '已结束'
} as const

const filters = reactive({
  keyword: '',
  status: '',
  adId: '',
  region: ''
})

const selectedPlanId = ref('')
const editing = ref(false)
const editForm = ref<null | {
  region: string
  buildingIds: string
  deviceIds: string
  timeSlots: string
  audienceTags: string
  lbsEnabled: boolean
}>(null)

const canCreate = computed(() => canCreateByRole(authStore.role))

const readonlyHint = computed(() => {
  if (['ops', 'area_admin'].includes(authStore.role ?? '')) {
    return '当前角色可重点演示排期、上线和暂停等运营动作，右侧面板可直接处理计划状态。'
  }
  if (!canCreate.value) {
    return '当前角色在计划页以查看为主，如需创建或修改计划，请切换到广告主、代理商、商务、运维或超级管理员账号。'
  }
  return ''
})

const availableAds = computed(() => {
  const role = authStore.role
  const userId = authStore.currentUser?.id

  if (role === 'advertiser') {
    return workflow.ads.filter((item) => item.advertiserId === userId)
  }
  if (role === 'agent') {
    return workflow.ads.filter((item) => item.agentId === userId)
  }
  return workflow.ads
})

const visiblePlans = computed(() =>
  visiblePlansByRole(authStore.role, workflow.plans, workflow.ads, authStore.currentUser?.id)
)

const filteredPlans = computed(() =>
  visiblePlans.value.filter((item) => {
    const keyword = filters.keyword.toLowerCase()
    const hitKeyword =
      !keyword ||
      item.id.toLowerCase().includes(keyword) ||
      item.adId.toLowerCase().includes(keyword) ||
      item.region.toLowerCase().includes(keyword)
    const hitStatus = !filters.status || item.status === filters.status
    const hitAd = !filters.adId || item.adId === filters.adId
    const hitRegion = !filters.region || item.region.includes(filters.region)
    return hitKeyword && hitStatus && hitAd && hitRegion
  })
)

const selectedPlan = computed(
  () => filteredPlans.value.find((item) => item.id === selectedPlanId.value) ?? filteredPlans.value[0]
)

const canEditSelected = computed(
  () =>
    !!selectedPlan.value &&
    canEditPlan(authStore.role, selectedPlan.value, workflow.ads, authStore.currentUser?.id)
)

const canOperateSelected = computed(
  () =>
    !!selectedPlan.value &&
    ['ops', 'area_admin', 'business', 'super_admin'].includes(authStore.role ?? '')
)

const pendingCount = computed(() =>
  filteredPlans.value.filter((item) => item.status === 'pending_schedule').length
)
const scheduledCount = computed(() =>
  filteredPlans.value.filter((item) => item.status === 'scheduled').length
)
const liveCount = computed(() => filteredPlans.value.filter((item) => item.status === 'live').length)

watch(
  () => route.query.adId,
  (adId) => {
    if (typeof adId === 'string') {
      filters.adId = adId
    }
  },
  { immediate: true }
)

watch(
  filteredPlans,
  (list) => {
    if (!list.length) {
      selectedPlanId.value = ''
      editing.value = false
      editForm.value = null
      return
    }
    if (!list.find((item) => item.id === selectedPlanId.value)) {
      selectedPlanId.value = list[0].id
    }
  },
  { immediate: true }
)

function statusClass(status: keyof typeof statusLabels) {
  if (status === 'live') return 'status-live'
  if (status === 'finished') return 'status-confirmed'
  if (status === 'paused') return 'status-offline'
  return 'status-pending'
}

function parseList(input: string) {
  return input
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function changeStatus(status: 'scheduled' | 'live' | 'paused' | 'finished') {
  if (!selectedPlan.value || !authStore.currentUser) return
  workflow.updatePlanStatus(selectedPlan.value.id, status)
  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: `plan_${status}`,
    targetType: 'plan',
    targetId: selectedPlan.value.id,
    detail: `计划状态更新为 ${statusLabels[status]}`
  })
}

function toggleEdit() {
  if (!selectedPlan.value) return
  editing.value = !editing.value
  if (editing.value) {
    editForm.value = {
      region: selectedPlan.value.region,
      buildingIds: selectedPlan.value.buildingIds.join(', '),
      deviceIds: selectedPlan.value.deviceIds.join(', '),
      timeSlots: selectedPlan.value.targeting.timeSlots.join(', '),
      audienceTags: selectedPlan.value.targeting.audienceTags.join(', '),
      lbsEnabled: selectedPlan.value.targeting.lbsEnabled
    }
  } else {
    editForm.value = null
  }
}

function saveEdit() {
  if (!selectedPlan.value || !editForm.value || !authStore.currentUser) return

  workflow.updatePlan(selectedPlan.value.id, {
    region: editForm.value.region,
    buildingIds: parseList(editForm.value.buildingIds),
    deviceIds: parseList(editForm.value.deviceIds),
    targeting: {
      timeSlots: parseList(editForm.value.timeSlots),
      audienceTags: parseList(editForm.value.audienceTags),
      lbsEnabled: editForm.value.lbsEnabled
    }
  })

  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: 'update_plan',
    targetType: 'plan',
    targetId: selectedPlan.value.id,
    detail: `更新计划配置：${selectedPlan.value.id}`
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
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 12px;
}

.editor-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.selected {
  background: rgba(43, 200, 255, 0.08);
  box-shadow: inset 2px 0 0 #2bc8ff;
}

.drawer-card {
  min-height: 440px;
}

.drawer-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.compact th,
.compact td {
  padding: 10px 12px;
}

.editor-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #edf2f7;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
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
