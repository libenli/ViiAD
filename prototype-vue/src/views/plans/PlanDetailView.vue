<template>
  <section>
    <div class="page-title">
      <div>
        <h1>计划详情</h1>
        <p class="page-subtitle">查看计划定向规则、设备范围，并在有权限时编辑或删除计划。</p>
      </div>
      <RouterLink class="btn secondary" to="/plans">返回列表</RouterLink>
    </div>

    <div v-if="plan" class="content-columns">
      <div class="card">
        <table class="table">
          <tbody>
            <tr><th>计划 ID</th><td>{{ plan.id }}</td></tr>
            <tr><th>关联广告</th><td>{{ plan.adId }}</td></tr>
            <tr><th>区域</th><td>{{ plan.region }}</td></tr>
            <tr><th>设备范围</th><td>{{ plan.deviceIds.join(', ') || '-' }}</td></tr>
            <tr><th>楼宇范围</th><td>{{ plan.buildingIds.join(', ') || '-' }}</td></tr>
            <tr><th>时段</th><td>{{ plan.targeting.timeSlots.join(' / ') || '-' }}</td></tr>
            <tr><th>人群标签</th><td>{{ plan.targeting.audienceTags.join(' / ') || '-' }}</td></tr>
            <tr><th>LBS</th><td>{{ plan.targeting.lbsEnabled ? '开启' : '关闭' }}</td></tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>编辑计划</h3>
        <template v-if="canEdit">
          <div class="editor-grid">
            <label>
              <span>区域</span>
              <input v-model.trim="form.region" class="input" />
            </label>
            <label>
              <span>楼宇 ID</span>
              <input v-model.trim="form.buildingIds" class="input" />
            </label>
            <label>
              <span>设备 ID</span>
              <input v-model.trim="form.deviceIds" class="input" />
            </label>
            <label>
              <span>时段</span>
              <input v-model.trim="form.timeSlots" class="input" />
            </label>
            <label>
              <span>人群标签</span>
              <input v-model.trim="form.audienceTags" class="input" />
            </label>
            <label class="switch-row">
              <input v-model="form.lbsEnabled" type="checkbox" />
              <span>启用 LBS</span>
            </label>
          </div>

          <div class="toolbar-group" style="margin-top: 12px">
            <button class="btn" :disabled="isSaving || isDeleting" @click="save">
              {{ isSaving ? '保存中...' : '保存修改' }}
            </button>
            <button class="btn secondary danger-trigger" :disabled="isSaving || isDeleting" @click="confirmDelete = !confirmDelete">
              {{ confirmDelete ? '取消删除' : '删除计划' }}
            </button>
          </div>

          <div v-if="confirmDelete" class="danger-zone">
            <strong>确认删除该计划？</strong>
            <p>若计划仍处于投放中，系统会阻止删除。删除成功后将返回计划列表。</p>
            <div class="toolbar-group">
              <button class="btn danger-btn" :disabled="isDeleting" @click="removePlan">
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
import { canEditPlan } from '@/config/entity-access'

const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()
const plan = computed(() => workflow.plans.find((item) => item.id === route.params.id))

const canEdit = computed(
  () => !!plan.value && canEditPlan(authStore.role, plan.value, workflow.ads, authStore.currentUser?.id)
)

const form = reactive({
  region: '',
  buildingIds: '',
  deviceIds: '',
  timeSlots: '',
  audienceTags: '',
  lbsEnabled: true
})

const isSaving = ref(false)
const isDeleting = ref(false)
const confirmDelete = ref(false)

watch(
  plan,
  (current) => {
    if (!current) return
    form.region = current.region
    form.buildingIds = current.buildingIds.join(', ')
    form.deviceIds = current.deviceIds.join(', ')
    form.timeSlots = current.targeting.timeSlots.join(', ')
    form.audienceTags = current.targeting.audienceTags.join(', ')
    form.lbsEnabled = current.targeting.lbsEnabled
  },
  { immediate: true }
)

function parseList(input: string) {
  return input
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function validate() {
  if (!form.region) {
    return '请先填写投放区域。'
  }
  return ''
}

async function save() {
  if (isSaving.value || !plan.value || !authStore.currentUser) return

  const message = validate()
  if (message) {
    appStore.showToast({
      type: 'warning',
      title: '计划未保存',
      detail: message
    })
    return
  }

  try {
    isSaving.value = true

    const updated = workflow.updatePlan(plan.value.id, {
      region: form.region,
      buildingIds: parseList(form.buildingIds),
      deviceIds: parseList(form.deviceIds),
      targeting: {
        timeSlots: parseList(form.timeSlots),
        audienceTags: parseList(form.audienceTags),
        lbsEnabled: form.lbsEnabled
      }
    })

    if (!updated) {
      throw new Error('计划不存在，无法继续保存。')
    }

    auditStore.addLog({
      actorId: authStore.currentUser.id,
      actorRole: authStore.currentUser.role,
      action: 'update_plan',
      targetType: 'plan',
      targetId: plan.value.id,
      detail: `详情页更新计划：${plan.value.id}`
    })

    appStore.showToast({
      type: 'success',
      title: '计划配置已保存',
      detail: `计划 ${plan.value.id} 的配置已经更新。`
    })
  } catch (err) {
    appStore.showToast({
      type: 'error',
      title: '计划保存失败',
      detail: err instanceof Error ? err.message : '保存计划时发生未知错误。'
    })
  } finally {
    isSaving.value = false
  }
}

async function removePlan() {
  if (isDeleting.value || !plan.value || !authStore.currentUser) return

  try {
    isDeleting.value = true
    const deleted = workflow.removePlan(plan.value.id)

    auditStore.addLog({
      actorId: authStore.currentUser.id,
      actorRole: authStore.currentUser.role,
      action: 'delete_plan',
      targetType: 'plan',
      targetId: deleted.id,
      detail: `删除计划：${deleted.id}`
    })

    appStore.showToast({
      type: 'success',
      title: '计划已删除',
      detail: `计划 ${deleted.id} 已移除，正在返回列表页。`
    })

    await router.push('/plans')
  } catch (err) {
    appStore.showToast({
      type: 'error',
      title: '计划删除失败',
      detail: err instanceof Error ? err.message : '删除计划时发生未知错误。'
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

.switch-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
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
