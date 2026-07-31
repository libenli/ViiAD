<template>
  <section>
    <div class="page-title">
      <div>
        <h1>素材详情</h1>
        <p class="page-subtitle">支持查看素材预览、审核记录，并在有权限时编辑或删除素材。</p>
      </div>
      <RouterLink class="btn secondary" to="/materials">返回列表</RouterLink>
    </div>

    <div v-if="material" class="content-columns">
      <div class="card">
        <img :src="material.url" alt="material" style="width: 100%; border-radius: 14px" />
        <table class="table" style="margin-top: 16px">
          <tbody>
            <tr><th>素材 ID</th><td>{{ material.id }}</td></tr>
            <tr><th>关联广告</th><td>{{ material.adId }}</td></tr>
            <tr><th>状态</th><td>{{ labels[material.status] }}</td></tr>
            <tr><th>审核意见</th><td>{{ material.reviewComment || '暂无' }}</td></tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>编辑素材</h3>
        <template v-if="canEdit">
          <div class="editor-grid">
            <label>
              <span>素材名称</span>
              <input v-model.trim="form.name" class="input" />
            </label>
            <label>
              <span>类型</span>
              <select v-model="form.type" class="select">
                <option value="image">图片</option>
                <option value="video">视频</option>
              </select>
            </label>
            <label style="grid-column: 1 / -1">
              <span>预览 URL</span>
              <input v-model.trim="form.url" class="input" />
            </label>
          </div>

          <label class="block-label">
            <span>素材说明</span>
            <textarea v-model.trim="form.description" class="textarea" />
          </label>

          <div class="toolbar-group" style="margin-top: 12px">
            <button class="btn" :disabled="isSaving || isDeleting" @click="save">
              {{ isSaving ? '保存中...' : '保存修改' }}
            </button>
            <button class="btn secondary danger-trigger" :disabled="isSaving || isDeleting" @click="confirmDelete = !confirmDelete">
              {{ confirmDelete ? '取消删除' : '删除素材' }}
            </button>
          </div>

          <div v-if="confirmDelete" class="danger-zone">
            <strong>确认删除该素材？</strong>
            <p>若该素材已被投放计划引用，系统会阻止删除。删除成功后将返回素材列表。</p>
            <div class="toolbar-group">
              <button class="btn danger-btn" :disabled="isDeleting" @click="removeMaterial">
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
import { materialStatusLabels as labels } from '@/config/workflow'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'
import { useAppStore } from '@/stores/app'
import { canEditMaterial } from '@/config/entity-access'

const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()
const material = computed(() => workflow.materials.find((item) => item.id === route.params.id))

const canEdit = computed(
  () =>
    !!material.value &&
    canEditMaterial(authStore.role, material.value, workflow.ads, authStore.currentUser?.id)
)

const form = reactive({
  name: '',
  description: '',
  type: 'image' as 'image' | 'video',
  url: ''
})

const isSaving = ref(false)
const isDeleting = ref(false)
const confirmDelete = ref(false)

watch(
  material,
  (current) => {
    if (!current) return
    form.name = current.name
    form.description = current.description || ''
    form.type = current.type
    form.url = current.url
  },
  { immediate: true }
)

function validate() {
  if (!form.name || !form.url) {
    return '请先填写素材名称和预览地址。'
  }
  return ''
}

async function save() {
  if (isSaving.value || !material.value || !authStore.currentUser) return

  const message = validate()
  if (message) {
    appStore.showToast({
      type: 'warning',
      title: '素材未保存',
      detail: message
    })
    return
  }

  try {
    isSaving.value = true

    const updated = workflow.updateMaterial(material.value.id, {
      name: form.name,
      description: form.description,
      type: form.type,
      url: form.url
    })

    if (!updated) {
      throw new Error('素材不存在，无法继续保存。')
    }

    auditStore.addLog({
      actorId: authStore.currentUser.id,
      actorRole: authStore.currentUser.role,
      action: 'update_material',
      targetType: 'material',
      targetId: material.value.id,
      detail: `详情页更新素材：${form.name}`
    })

    appStore.showToast({
      type: 'success',
      title: '素材信息已保存',
      detail: `素材 ${material.value.id} 的修改已生效。`
    })
  } catch (err) {
    appStore.showToast({
      type: 'error',
      title: '素材保存失败',
      detail: err instanceof Error ? err.message : '保存素材时发生未知错误。'
    })
  } finally {
    isSaving.value = false
  }
}

async function removeMaterial() {
  if (isDeleting.value || !material.value || !authStore.currentUser) return

  try {
    isDeleting.value = true
    const deleted = workflow.removeMaterial(material.value.id)

    auditStore.addLog({
      actorId: authStore.currentUser.id,
      actorRole: authStore.currentUser.role,
      action: 'delete_material',
      targetType: 'material',
      targetId: deleted.id,
      detail: `删除素材：${deleted.name}`
    })

    appStore.showToast({
      type: 'success',
      title: '素材已删除',
      detail: `素材 ${deleted.id} 已移除，正在返回列表页。`
    })

    await router.push('/materials')
  } catch (err) {
    appStore.showToast({
      type: 'error',
      title: '素材删除失败',
      detail: err instanceof Error ? err.message : '删除素材时发生未知错误。'
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
