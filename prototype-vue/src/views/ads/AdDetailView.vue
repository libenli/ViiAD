<template>
  <section>
    <div class="page-title">
      <div>
        <h1>广告详情</h1>
        <p class="page-subtitle">查看广告基本信息，并在有权限时直接修改或删除广告。</p>
      </div>
      <RouterLink class="btn secondary" to="/ads">返回列表</RouterLink>
    </div>

    <div v-if="ad" class="content-columns">
      <div class="card">
        <h3>{{ ad.title }}</h3>
        <p class="muted">{{ ad.description || '暂无说明' }}</p>
        <table class="table">
          <tbody>
            <tr><th>广告 ID</th><td>{{ ad.id }}</td></tr>
            <tr><th>投放区域</th><td>{{ ad.region }}</td></tr>
            <tr><th>预算</th><td>{{ ad.budget }}</td></tr>
            <tr><th>状态</th><td>{{ ad.status }}</td></tr>
            <tr><th>关联计划</th><td>{{ ad.planId || '-' }}</td></tr>
            <tr><th>关联素材</th><td>{{ ad.materialIds.join(', ') || '-' }}</td></tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>编辑广告</h3>
        <template v-if="canEdit">
          <div class="editor-grid">
            <label>
              <span>标题</span>
              <input v-model.trim="form.title" class="input" />
            </label>
            <label>
              <span>区域</span>
              <input v-model.trim="form.region" class="input" />
            </label>
            <label>
              <span>目标</span>
              <select v-model="form.objective" class="select">
                <option value="exposure">品牌曝光</option>
                <option value="conversion">转化成交</option>
                <option value="engagement">互动引流</option>
              </select>
            </label>
            <label>
              <span>预算</span>
              <input v-model.number="form.budget" class="input" type="number" min="1" />
            </label>
          </div>

          <label class="block-label">
            <span>说明</span>
            <textarea v-model.trim="form.description" class="textarea" />
          </label>

          <div class="toolbar-group" style="margin-top: 12px">
            <button class="btn" :disabled="isSaving || isDeleting" @click="save">
              {{ isSaving ? '保存中...' : '保存修改' }}
            </button>
            <button class="btn secondary danger-trigger" :disabled="isSaving || isDeleting" @click="confirmDelete = !confirmDelete">
              {{ confirmDelete ? '取消删除' : '删除广告' }}
            </button>
          </div>

          <div v-if="confirmDelete" class="danger-zone">
            <strong>确认删除该广告？</strong>
            <p>删除后将返回广告列表。若该广告仍有关联素材或计划，系统会阻止删除并提示原因。</p>
            <div class="toolbar-group">
              <button class="btn danger-btn" :disabled="isDeleting" @click="removeAd">
                {{ isDeleting ? '删除中...' : '确认删除' }}
              </button>
              <button class="btn secondary" :disabled="isDeleting" @click="confirmDelete = false">取消</button>
            </div>
          </div>
        </template>
        <p v-else class="muted">当前角色只有查看权限，没有编辑权限。</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'
import { useAppStore } from '@/stores/app'
import { canEditAd } from '@/config/entity-access'

const route = useRoute()
const router = useRouter()
const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const appStore = useAppStore()

const ad = computed(() => workflow.ads.find((item) => item.id === route.params.id))
const canEdit = computed(
  () => !!ad.value && canEditAd(authStore.role, ad.value, authStore.currentUser?.id)
)

const form = reactive({
  title: '',
  description: '',
  objective: 'conversion' as 'exposure' | 'conversion' | 'engagement',
  region: '',
  budget: 0
})

const isSaving = ref(false)
const isDeleting = ref(false)
const confirmDelete = ref(false)

watch(
  ad,
  (current) => {
    if (!current) return
    form.title = current.title
    form.description = current.description || ''
    form.objective = current.objective
    form.region = current.region
    form.budget = current.budget
  },
  { immediate: true }
)

function validate() {
  if (!form.title || !form.region || !form.budget) {
    return '请先完善标题、区域和预算。'
  }
  return ''
}

async function save() {
  if (isSaving.value || !ad.value || !authStore.currentUser) return

  const message = validate()
  if (message) {
    appStore.showToast({
      type: 'warning',
      title: '广告未保存',
      detail: message
    })
    return
  }

  try {
    isSaving.value = true

    const updated = workflow.updateAd(ad.value.id, {
      title: form.title,
      description: form.description,
      objective: form.objective,
      region: form.region,
      budget: form.budget
    })

    if (!updated) {
      throw new Error('广告不存在，无法继续保存。')
    }

    auditStore.addLog({
      actorId: authStore.currentUser.id,
      actorRole: authStore.currentUser.role,
      action: 'update_ad',
      targetType: 'ad',
      targetId: ad.value.id,
      detail: `详情页更新广告：${form.title}`
    })

    appStore.showToast({
      type: 'success',
      title: '广告信息已保存',
      detail: `广告 ${ad.value.id} 的修改已生效。`
    })
  } catch (err) {
    appStore.showToast({
      type: 'error',
      title: '广告保存失败',
      detail: err instanceof Error ? err.message : '保存广告时发生未知错误。'
    })
  } finally {
    isSaving.value = false
  }
}

async function removeAd() {
  if (isDeleting.value || !ad.value || !authStore.currentUser) return

  try {
    isDeleting.value = true
    const deleted = workflow.removeAd(ad.value.id)

    auditStore.addLog({
      actorId: authStore.currentUser.id,
      actorRole: authStore.currentUser.role,
      action: 'delete_ad',
      targetType: 'ad',
      targetId: deleted.id,
      detail: `删除广告：${deleted.title}`
    })

    appStore.showToast({
      type: 'success',
      title: '广告已删除',
      detail: `广告 ${deleted.id} 已移除，正在返回列表页。`
    })

    await router.push('/ads')
  } catch (err) {
    appStore.showToast({
      type: 'error',
      title: '广告删除失败',
      detail: err instanceof Error ? err.message : '删除广告时发生未知错误。'
    })
  } finally {
    isDeleting.value = false
    confirmDelete.value = false
  }
}
</script>

<style scoped>
.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.block-label {
  display: block;
  margin-top: 12px;
}

.danger-zone {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(255, 108, 122, 0.22);
  background: rgba(66, 18, 28, 0.28);
}

.danger-zone strong {
  display: block;
  color: #ffd1d6;
}

.danger-zone p {
  margin: 8px 0 12px;
  color: #e7b8bf;
  line-height: 1.6;
}

.danger-trigger {
  border-color: rgba(255, 108, 122, 0.18);
}

.danger-btn {
  background: linear-gradient(135deg, #ff7584, #ff9a63);
}

label span {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #425466;
}
</style>
