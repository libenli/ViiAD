<template>
  <section>
    <div class="page-title">
      <div>
        <h1>素材管理</h1>
        <p class="page-subtitle">
          支持上传、审核、筛选和右侧快捷处理，适合演示素材从提交到过审的完整链路。
        </p>
      </div>
    </div>

    <div v-if="readonlyHint" class="permission-note">
      {{ readonlyHint }}
    </div>

    <div class="card">
      <h3>素材提交区</h3>
      <div class="form-grid">
        <label>
          <span>关联广告</span>
          <select v-model="form.adId" class="select" :disabled="!canUpload">
            <option value="">请选择广告</option>
            <option v-for="ad in uploadableAds" :key="ad.id" :value="ad.id">
              {{ ad.id }} / {{ ad.title }}
            </option>
          </select>
        </label>

        <label>
          <span>素材名称</span>
          <input
            v-model.trim="form.name"
            class="input"
            placeholder="例如：首页开屏主视觉"
            :disabled="!canUpload"
          />
        </label>

        <label>
          <span>素材类型</span>
          <select v-model="form.type" class="select" :disabled="!canUpload">
            <option value="image">图片</option>
            <option value="video">视频</option>
          </select>
        </label>

        <label>
          <span>预览 URL</span>
          <input
            v-model.trim="form.url"
            class="input"
            placeholder="可选，不填则自动生成占位图"
            :disabled="!canUpload"
          />
        </label>
      </div>

      <label class="block-label">
        <span>素材说明</span>
        <textarea
          v-model.trim="form.description"
          class="textarea"
          placeholder="补充尺寸、时长、屏幕适配要求等"
          :disabled="!canUpload"
        />
      </label>

      <p v-if="uploadError" class="error-text">{{ uploadError }}</p>
      <p v-if="uploadSuccess" class="success-text">{{ uploadSuccess }}</p>

      <div class="toolbar-group" style="margin-top: 16px">
        <button class="btn" :disabled="!canUpload" @click="submitMaterial">提交审核</button>
        <span v-if="!canUpload" class="muted">当前角色仅可查看或审核，不可上传素材。</span>
      </div>
    </div>

    <div class="card filter-card" style="margin-top: 20px">
      <div class="filter-grid">
        <input
          v-model.trim="filters.keyword"
          class="input"
          placeholder="搜索素材名称 / ID / 广告 ID"
        />
        <select v-model="filters.status" class="select">
          <option value="">全部状态</option>
          <option value="pending_review">待审核</option>
          <option value="approved">已通过</option>
          <option value="rejected">已驳回</option>
        </select>
        <select v-model="filters.type" class="select">
          <option value="">全部类型</option>
          <option value="image">图片</option>
          <option value="video">视频</option>
        </select>
        <select v-model="filters.adId" class="select">
          <option value="">全部广告</option>
          <option v-for="adId in adFilterOptions" :key="adId" :value="adId">{{ adId }}</option>
        </select>
      </div>
    </div>

    <div class="card-grid" style="margin-top: 20px">
      <div class="card">
        <div>可见素材</div>
        <div class="metric-value">{{ filteredMaterials.length }}</div>
      </div>
      <div class="card">
        <div>待审核</div>
        <div class="metric-value">{{ pendingMaterials.length }}</div>
      </div>
      <div class="card">
        <div>已通过</div>
        <div class="metric-value">{{ approvedMaterials.length }}</div>
      </div>
      <div class="card">
        <div>已驳回</div>
        <div class="metric-value">{{ rejectedMaterials.length }}</div>
      </div>
    </div>

    <div v-if="filteredMaterials.length === 0" class="empty-state">
      当前筛选条件下没有素材记录，请调整广告、类型或审核状态后再查看。
    </div>

    <div v-else class="content-columns">
      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>素材 ID</th>
              <th>名称</th>
              <th>关联广告</th>
              <th>类型</th>
              <th>状态</th>
              <th>上传人</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in filteredMaterials"
              :key="item.id"
              :class="{ selected: selectedMaterial?.id === item.id }"
              @click="selectedMaterialId = item.id"
            >
              <td><RouterLink :to="`/materials/${item.id}`">{{ item.id }}</RouterLink></td>
              <td>{{ item.name }}</td>
              <td>{{ item.adId }}</td>
              <td>{{ item.type === 'image' ? '图片' : '视频' }}</td>
              <td>
                <span class="status" :class="statusClass(item.status)">
                  {{ statusLabels[item.status] }}
                </span>
              </td>
              <td>{{ item.uploaderId }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card drawer-card">
        <template v-if="selectedMaterial">
          <div class="drawer-head">
            <div>
              <h3>{{ selectedMaterial.name }}</h3>
              <p class="muted">{{ selectedMaterial.id }} / 广告 {{ selectedMaterial.adId }}</p>
            </div>
            <RouterLink class="btn secondary" :to="`/materials/${selectedMaterial.id}`">
              进入详情
            </RouterLink>
          </div>

          <img
            :src="selectedMaterial.url"
            alt="material"
            style="width: 100%; max-height: 180px; object-fit: cover; border-radius: 12px"
          />

          <table class="table compact" style="margin-top: 12px">
            <tbody>
              <tr><th>状态</th><td>{{ statusLabels[selectedMaterial.status] }}</td></tr>
              <tr><th>类型</th><td>{{ selectedMaterial.type === 'image' ? '图片' : '视频' }}</td></tr>
              <tr><th>审核意见</th><td>{{ selectedMaterial.reviewComment || '暂无' }}</td></tr>
              <tr><th>上传时间</th><td>{{ selectedMaterial.createdAt }}</td></tr>
            </tbody>
          </table>

          <div class="editor-panel">
            <h4>快捷操作</h4>
            <div class="toolbar-group">
              <button v-if="canEditSelected" class="btn secondary" @click="toggleEdit">
                {{ editing ? '取消编辑' : '编辑素材' }}
              </button>
              <button v-if="canReviewSelected" class="btn secondary" @click="approveSelected">
                审核通过
              </button>
              <button v-if="canReviewSelected" class="btn" @click="rejectSelected">驳回素材</button>
            </div>
          </div>

          <div v-if="editing && editForm" class="editor-panel">
            <h4>右侧快速编辑</h4>
            <div class="editor-grid">
              <label>
                <span>素材名称</span>
                <input v-model.trim="editForm.name" class="input" />
              </label>
              <label>
                <span>类型</span>
                <select v-model="editForm.type" class="select">
                  <option value="image">图片</option>
                  <option value="video">视频</option>
                </select>
              </label>
              <label style="grid-column: 1 / -1">
                <span>预览 URL</span>
                <input v-model.trim="editForm.url" class="input" />
              </label>
            </div>

            <label class="block-label">
              <span>素材说明</span>
              <textarea v-model.trim="editForm.description" class="textarea" />
            </label>

            <div class="toolbar-group" style="margin-top: 12px">
              <button class="btn" @click="saveEdit">保存修改</button>
            </div>
          </div>

          <label v-if="canReviewSelected" class="block-label">
            <span>驳回原因</span>
            <textarea
              v-model.trim="reviewComment"
              class="textarea"
              placeholder="例如：分辨率不符合横屏大屏规范"
            />
          </label>
        </template>

        <div v-else class="empty-state drawer-empty">
          请选择一条素材，在右侧查看详情和审核操作。
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
import { canCreateByRole, canEditMaterial, visibleMaterialsByRole } from '@/config/entity-access'

const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const route = useRoute()

const statusLabels = {
  uploaded: '已上传',
  pending_review: '待审核',
  approved: '已通过',
  rejected: '已驳回'
} as const

const form = reactive({
  adId: '',
  name: '',
  type: 'image' as 'image' | 'video',
  url: '',
  description: ''
})

const filters = reactive({
  keyword: '',
  status: '',
  type: '',
  adId: ''
})

const selectedMaterialId = ref('')
const uploadError = ref('')
const uploadSuccess = ref('')
const reviewComment = ref('')
const editing = ref(false)
const editForm = ref<null | {
  name: string
  description: string
  type: 'image' | 'video'
  url: string
}>(null)

const canUpload = computed(() => canCreateByRole(authStore.role))
const canReview = computed(() => ['reviewer', 'super_admin'].includes(authStore.role ?? ''))

const readonlyHint = computed(() => {
  if (canReview.value) {
    return '当前角色可在右侧面板直接完成素材审核，适合演示“提交后由审核岗处理”的流程。'
  }
  if (!canUpload.value) {
    return '当前角色在素材页以查看为主，如需上传素材，请切换到广告主、代理商、商务或超级管理员账号。'
  }
  return ''
})

const visibleMaterials = computed(() =>
  visibleMaterialsByRole(authStore.role, workflow.materials, workflow.ads, authStore.currentUser?.id)
)

const uploadableAds = computed(() => {
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

const adFilterOptions = computed(() =>
  Array.from(new Set(visibleMaterials.value.map((item) => item.adId)))
)

const filteredMaterials = computed(() =>
  visibleMaterials.value.filter((item) => {
    const keyword = filters.keyword.toLowerCase()
    const hitKeyword =
      !keyword ||
      item.id.toLowerCase().includes(keyword) ||
      item.name.toLowerCase().includes(keyword) ||
      item.adId.toLowerCase().includes(keyword)
    const hitStatus = !filters.status || item.status === filters.status
    const hitType = !filters.type || item.type === filters.type
    const hitAd = !filters.adId || item.adId === filters.adId
    return hitKeyword && hitStatus && hitType && hitAd
  })
)

const selectedMaterial = computed(
  () =>
    filteredMaterials.value.find((item) => item.id === selectedMaterialId.value) ??
    filteredMaterials.value[0]
)

const canEditSelected = computed(
  () =>
    !!selectedMaterial.value &&
    canEditMaterial(authStore.role, selectedMaterial.value, workflow.ads, authStore.currentUser?.id)
)

const canReviewSelected = computed(
  () => !!selectedMaterial.value && selectedMaterial.value.status === 'pending_review' && canReview.value
)

const pendingMaterials = computed(() =>
  filteredMaterials.value.filter((item) => item.status === 'pending_review')
)
const approvedMaterials = computed(() =>
  filteredMaterials.value.filter((item) => item.status === 'approved')
)
const rejectedMaterials = computed(() =>
  filteredMaterials.value.filter((item) => item.status === 'rejected')
)

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
  filteredMaterials,
  (list) => {
    if (!list.length) {
      selectedMaterialId.value = ''
      editing.value = false
      editForm.value = null
      return
    }
    if (!list.find((item) => item.id === selectedMaterialId.value)) {
      selectedMaterialId.value = list[0].id
    }
  },
  { immediate: true }
)

function statusClass(status: keyof typeof statusLabels) {
  if (status === 'approved') return 'status-confirmed'
  if (status === 'rejected') return 'status-offline'
  return 'status-pending'
}

function submitMaterial() {
  uploadError.value = ''
  uploadSuccess.value = ''

  if (!authStore.currentUser || !canUpload.value) return

  if (!form.adId || !form.name) {
    uploadError.value = '请先选择关联广告并填写素材名称。'
    return
  }

  const material = workflow.createMaterial({
    adId: form.adId,
    name: form.name,
    description: form.description,
    type: form.type,
    url: form.url || undefined,
    uploaderId: authStore.currentUser.id
  })

  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: 'upload_material',
    targetType: 'material',
    targetId: material.id,
    detail: `提交素材审核：${material.name}`
  })

  uploadSuccess.value = `素材 ${material.id} 已提交审核。`
  form.adId = ''
  form.name = ''
  form.type = 'image'
  form.url = ''
  form.description = ''
  selectedMaterialId.value = material.id
}

function toggleEdit() {
  if (!selectedMaterial.value) return
  editing.value = !editing.value
  if (editing.value) {
    editForm.value = {
      name: selectedMaterial.value.name,
      description: selectedMaterial.value.description || '',
      type: selectedMaterial.value.type,
      url: selectedMaterial.value.url
    }
  } else {
    editForm.value = null
  }
}

function saveEdit() {
  if (!selectedMaterial.value || !editForm.value || !authStore.currentUser) return

  workflow.updateMaterial(selectedMaterial.value.id, {
    name: editForm.value.name,
    description: editForm.value.description,
    type: editForm.value.type,
    url: editForm.value.url
  })

  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: 'update_material',
    targetType: 'material',
    targetId: selectedMaterial.value.id,
    detail: `更新素材信息：${editForm.value.name}`
  })

  toggleEdit()
}

function approveSelected() {
  if (!selectedMaterial.value || !authStore.currentUser) return
  workflow.approveMaterial(selectedMaterial.value.id, authStore.currentUser.id)
  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: 'approve_material',
    targetType: 'material',
    targetId: selectedMaterial.value.id,
    detail: `审核通过：${selectedMaterial.value.name}`
  })
  reviewComment.value = ''
}

function rejectSelected() {
  if (!selectedMaterial.value || !authStore.currentUser) return
  const reason = reviewComment.value || '不符合当前审核规范'
  workflow.rejectMaterial(selectedMaterial.value.id, authStore.currentUser.id, reason)
  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: 'reject_material',
    targetType: 'material',
    targetId: selectedMaterial.value.id,
    detail: `审核驳回：${reason}`
  })
  reviewComment.value = ''
}
</script>

<style scoped>
.form-grid,
.filter-grid,
.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.filter-grid {
  grid-template-columns: 2fr 1fr 1fr 1fr;
}

.drawer-card {
  min-height: 460px;
}

.drawer-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.selected {
  background: rgba(43, 200, 255, 0.08);
  box-shadow: inset 2px 0 0 #2bc8ff;
}

.editor-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #edf2f7;
}

.block-label {
  display: block;
  margin-top: 16px;
}

label span {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #425466;
}

.error-text {
  color: #cc3d3d;
}

.success-text {
  color: #1f8f52;
}

.drawer-empty {
  min-height: 280px;
}
</style>
