<template>
  <section>
    <div class="page-title">
      <div>
        <h1>广告管理</h1>
        <p class="page-subtitle">
          统一查看广告需求、投放状态与归属信息，并通过右侧面板快速进入详情、素材和计划。
        </p>
      </div>
      <RouterLink v-if="canCreate" class="btn" to="/ads/new">新建广告</RouterLink>
    </div>

    <div v-if="readonlyHint" class="permission-note">
      {{ readonlyHint }}
    </div>

    <div class="card filter-card">
      <div class="filter-grid">
        <input
          v-model.trim="filters.keyword"
          class="input"
          placeholder="搜索广告标题 / ID / 区域"
        />
        <select v-model="filters.status" class="select">
          <option value="">全部状态</option>
          <option value="draft">草稿</option>
          <option value="submitted">已提交</option>
          <option value="planning">筹备中</option>
          <option value="live">投放中</option>
          <option value="finished">已结束</option>
        </select>
        <select v-model="filters.objective" class="select">
          <option value="">全部目标</option>
          <option value="exposure">品牌曝光</option>
          <option value="conversion">转化成交</option>
          <option value="engagement">互动引流</option>
        </select>
        <input v-model.trim="filters.region" class="input" placeholder="按区域筛选" />
      </div>
    </div>

    <div class="card-grid">
      <div class="card">
        <div>当前可见广告</div>
        <div class="metric-value">{{ filteredAds.length }}</div>
      </div>
      <div class="card">
        <div>待推进</div>
        <div class="metric-value">{{ pendingCount }}</div>
      </div>
      <div class="card">
        <div>筹备中</div>
        <div class="metric-value">{{ planningCount }}</div>
      </div>
      <div class="card">
        <div>投放中</div>
        <div class="metric-value">{{ liveCount }}</div>
      </div>
    </div>

    <div v-if="filteredAds.length === 0" class="empty-state">
      当前筛选条件下没有广告数据，请调整状态、目标或区域条件后再查看。
    </div>

    <div v-else class="content-columns">
      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>广告 ID</th>
              <th>标题</th>
              <th>区域</th>
              <th>目标</th>
              <th>预算</th>
              <th>状态</th>
              <th>素材数</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="ad in filteredAds"
              :key="ad.id"
              :class="{ selected: selectedAd?.id === ad.id }"
              @click="selectedAdId = ad.id"
            >
              <td><RouterLink :to="`/ads/${ad.id}`">{{ ad.id }}</RouterLink></td>
              <td>{{ ad.title }}</td>
              <td>{{ ad.region }}</td>
              <td>{{ objectiveLabels[ad.objective] }}</td>
              <td>{{ ad.budget }}</td>
              <td>
                <span class="status" :class="statusClass(ad.status)">
                  {{ statusLabels[ad.status] }}
                </span>
              </td>
              <td>{{ ad.materialIds.length }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card drawer-card">
        <template v-if="selectedAd">
          <div class="drawer-head">
            <div>
              <h3>{{ selectedAd.title }}</h3>
              <p class="muted">{{ selectedAd.id }} / {{ selectedAd.region }}</p>
            </div>
            <RouterLink class="btn secondary" :to="`/ads/${selectedAd.id}`">进入详情</RouterLink>
          </div>

          <table class="table compact">
            <tbody>
              <tr><th>投放目标</th><td>{{ objectiveLabels[selectedAd.objective] }}</td></tr>
              <tr><th>状态</th><td>{{ statusLabels[selectedAd.status] }}</td></tr>
              <tr><th>预算</th><td>{{ selectedAd.budget }}</td></tr>
              <tr><th>计划</th><td>{{ selectedAd.planId || '未创建' }}</td></tr>
              <tr><th>说明</th><td>{{ selectedAd.description || '暂无说明' }}</td></tr>
            </tbody>
          </table>

          <div class="editor-panel">
            <h4>快捷操作</h4>
            <div class="toolbar-group">
              <button v-if="canEditSelected" class="btn secondary" @click="openEdit">编辑广告</button>
              <RouterLink class="btn secondary" :to="`/materials?adId=${selectedAd.id}`">
                查看素材
              </RouterLink>
              <RouterLink class="btn secondary" :to="`/plans?adId=${selectedAd.id}`">
                查看计划
              </RouterLink>
            </div>
          </div>

          <div v-if="editing && draft" class="editor-panel">
            <h4>右侧快速编辑</h4>
            <div class="editor-grid">
              <label>
                <span>广告标题</span>
                <input v-model.trim="draft.title" class="input" />
              </label>
              <label>
                <span>投放区域</span>
                <input v-model.trim="draft.region" class="input" />
              </label>
              <label>
                <span>投放目标</span>
                <select v-model="draft.objective" class="select">
                  <option value="exposure">品牌曝光</option>
                  <option value="conversion">转化成交</option>
                  <option value="engagement">互动引流</option>
                </select>
              </label>
              <label>
                <span>预算</span>
                <input v-model.number="draft.budget" class="input" type="number" min="1" />
              </label>
            </div>

            <label class="block-label">
              <span>说明</span>
              <textarea v-model.trim="draft.description" class="textarea" />
            </label>

            <div class="toolbar-group" style="margin-top: 12px">
              <button class="btn" @click="saveEdit">保存修改</button>
              <button class="btn secondary" @click="cancelEdit">取消</button>
            </div>
          </div>
        </template>

        <div v-else class="empty-state drawer-empty">
          请选择一条广告，在右侧查看详情和操作入口。
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
import { canCreateByRole, canEditAd, visibleAdsByRole } from '@/config/entity-access'

const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const route = useRoute()

const objectiveLabels = {
  exposure: '品牌曝光',
  conversion: '转化成交',
  engagement: '互动引流'
} as const

const statusLabels = {
  draft: '草稿',
  submitted: '已提交',
  planning: '筹备中',
  live: '投放中',
  finished: '已结束'
} as const

const filters = reactive({
  keyword: '',
  status: '',
  objective: '',
  region: ''
})

const selectedAdId = ref('')
const editing = ref(false)
const draft = ref<null | {
  title: string
  description: string
  objective: 'exposure' | 'conversion' | 'engagement'
  region: string
  budget: number
}>(null)

const canCreate = computed(() => canCreateByRole(authStore.role))

const readonlyHint = computed(() => {
  if (authStore.role === 'audience') {
    return '当前角色以浏览已投放广告为主，不提供新建和编辑能力，可用于演示受众侧看到的内容范围。'
  }
  if (!canCreate.value) {
    return '当前角色在广告页以查看和协同为主，如需修改广告内容，请切换到广告主、代理商、商务或超级管理员账号。'
  }
  return ''
})

const visibleAds = computed(() =>
  visibleAdsByRole(authStore.role, workflow.ads, authStore.currentUser?.id)
)

const filteredAds = computed(() =>
  visibleAds.value.filter((item) => {
    const keyword = filters.keyword.toLowerCase()
    const hitKeyword =
      !keyword ||
      item.id.toLowerCase().includes(keyword) ||
      item.title.toLowerCase().includes(keyword) ||
      item.region.toLowerCase().includes(keyword)
    const hitStatus = !filters.status || item.status === filters.status
    const hitObjective = !filters.objective || item.objective === filters.objective
    const hitRegion = !filters.region || item.region.includes(filters.region)
    return hitKeyword && hitStatus && hitObjective && hitRegion
  })
)

const selectedAd = computed(
  () => filteredAds.value.find((item) => item.id === selectedAdId.value) ?? filteredAds.value[0]
)

const canEditSelected = computed(
  () =>
    !!selectedAd.value &&
    canEditAd(authStore.role, selectedAd.value, authStore.currentUser?.id)
)

const pendingCount = computed(() =>
  filteredAds.value.filter((item) => ['draft', 'submitted'].includes(item.status)).length
)
const planningCount = computed(() =>
  filteredAds.value.filter((item) => item.status === 'planning').length
)
const liveCount = computed(() => filteredAds.value.filter((item) => item.status === 'live').length)

watch(
  () => route.query.adId,
  (adId) => {
    if (typeof adId === 'string') {
      selectedAdId.value = adId
    }
  },
  { immediate: true }
)

watch(
  filteredAds,
  (list) => {
    if (!list.length) {
      selectedAdId.value = ''
      editing.value = false
      draft.value = null
      return
    }
    if (!list.find((item) => item.id === selectedAdId.value)) {
      selectedAdId.value = list[0].id
    }
  },
  { immediate: true }
)

function statusClass(status: keyof typeof statusLabels) {
  if (status === 'live') return 'status-live'
  if (status === 'finished') return 'status-confirmed'
  return 'status-pending'
}

function openEdit() {
  if (!selectedAd.value) return
  editing.value = true
  draft.value = {
    title: selectedAd.value.title,
    description: selectedAd.value.description || '',
    objective: selectedAd.value.objective,
    region: selectedAd.value.region,
    budget: selectedAd.value.budget
  }
}

function cancelEdit() {
  editing.value = false
  draft.value = null
}

function saveEdit() {
  if (!selectedAd.value || !draft.value || !authStore.currentUser) return

  workflow.updateAd(selectedAd.value.id, {
    title: draft.value.title,
    description: draft.value.description,
    objective: draft.value.objective,
    region: draft.value.region,
    budget: draft.value.budget
  })

  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: 'update_ad',
    targetType: 'ad',
    targetId: selectedAd.value.id,
    detail: `更新广告信息：${draft.value.title}`
  })

  cancelEdit()
}
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
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

.compact th,
.compact td {
  padding: 10px 12px;
}

.editor-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #edf2f7;
}

.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.block-label {
  display: block;
  margin-top: 12px;
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
