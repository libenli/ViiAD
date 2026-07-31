<template>
  <section>
    <div class="page-title">
      <div>
        <h1>设备详情</h1>
        <p class="page-subtitle">支持查看设备状态、当前广告，并在有权限时维护或删除设备。</p>
      </div>
      <RouterLink class="btn secondary" to="/devices">返回列表</RouterLink>
    </div>

    <div v-if="device" class="content-columns">
      <div class="card">
        <table class="table">
          <tbody>
            <tr><th>设备编号</th><td>{{ device.code }}</td></tr>
            <tr><th>位置</th><td>{{ device.building }} / {{ device.floor }}</td></tr>
            <tr><th>区域</th><td>{{ device.region }}</td></tr>
            <tr><th>状态</th><td>{{ labels[device.status] }}</td></tr>
            <tr><th>当前广告</th><td>{{ device.currentAdId || '-' }}</td></tr>
            <tr><th>最后心跳</th><td>{{ device.lastHeartbeatAt }}</td></tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>设备维护</h3>
        <template v-if="canManage">
          <div class="editor-grid">
            <label>
              <span>楼宇</span>
              <input v-model.trim="form.building" class="input" />
            </label>
            <label>
              <span>楼层</span>
              <input v-model.trim="form.floor" class="input" />
            </label>
            <label>
              <span>区域</span>
              <input v-model.trim="form.region" class="input" />
            </label>
            <label>
              <span>屏幕规格</span>
              <input v-model.trim="form.screenSize" class="input" />
            </label>
          </div>

          <div class="toolbar-group" style="margin-top: 12px">
            <button class="btn secondary" :disabled="isSaving || isDeleting" @click="changeStatus('online')">设为在线</button>
            <button class="btn secondary" :disabled="isSaving || isDeleting" @click="changeStatus('offline')">设为离线</button>
            <button class="btn secondary" :disabled="isSaving || isDeleting" @click="changeStatus('fault')">标记故障</button>
          </div>

          <div class="toolbar-group" style="margin-top: 12px">
            <button class="btn" :disabled="isSaving || isDeleting" @click="save">
              {{ isSaving ? '保存中...' : '保存修改' }}
            </button>
            <button class="btn secondary danger-trigger" :disabled="isSaving || isDeleting" @click="confirmDelete = !confirmDelete">
              {{ confirmDelete ? '取消删除' : '删除设备' }}
            </button>
          </div>

          <div v-if="confirmDelete" class="danger-zone">
            <strong>确认删除该设备？</strong>
            <p>若设备当前仍在投放计划中使用，系统会阻止删除。删除成功后将返回设备列表。</p>
            <div class="toolbar-group">
              <button class="btn danger-btn" :disabled="isDeleting" @click="removeDevice">
                {{ isDeleting ? '删除中...' : '确认删除' }}
              </button>
              <button class="btn secondary" :disabled="isDeleting" @click="confirmDelete = false">取消</button>
            </div>
          </div>
        </template>
        <p v-else class="muted">当前角色只有查看权限，没有设备维护权限。</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { deviceStatusLabels as labels } from '@/config/workflow'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'
import { useAppStore } from '@/stores/app'

const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()
const device = computed(() => workflow.devices.find((item) => item.id === route.params.id))

const form = reactive({
  building: '',
  floor: '',
  region: '',
  screenSize: ''
})

const isSaving = ref(false)
const isDeleting = ref(false)
const confirmDelete = ref(false)

const canManage = computed(() =>
  ['ops', 'area_admin', 'super_admin'].includes(authStore.role ?? '')
)

watch(
  device,
  (current) => {
    if (!current) return
    form.building = current.building
    form.floor = current.floor
    form.region = current.region
    form.screenSize = current.screenSize
  },
  { immediate: true }
)

function validate() {
  if (!form.building || !form.floor || !form.region || !form.screenSize) {
    return '请先完善楼宇、楼层、区域和屏幕规格。'
  }
  return ''
}

function changeStatus(status: 'online' | 'offline' | 'fault') {
  if (!device.value || !authStore.currentUser || !canManage.value) return

  workflow.updateDeviceStatus(device.value.id, status)

  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: 'update_device_status',
    targetType: 'device',
    targetId: device.value.id,
    detail: `详情页更新设备状态为 ${labels[status]}`
  })

  appStore.showToast({
    type: 'success',
    title: '设备状态已更新',
    detail: `设备 ${device.value.code} 已切换为${labels[status]}。`
  })
}

async function save() {
  if (isSaving.value || !device.value || !authStore.currentUser || !canManage.value) return

  const message = validate()
  if (message) {
    appStore.showToast({
      type: 'warning',
      title: '设备未保存',
      detail: message
    })
    return
  }

  try {
    isSaving.value = true

    const updated = workflow.updateDevice(device.value.id, {
      building: form.building,
      floor: form.floor,
      region: form.region,
      screenSize: form.screenSize
    })

    if (!updated) {
      throw new Error('设备不存在，无法继续保存。')
    }

    auditStore.addLog({
      actorId: authStore.currentUser.id,
      actorRole: authStore.currentUser.role,
      action: 'update_device',
      targetType: 'device',
      targetId: device.value.id,
      detail: `详情页更新设备：${device.value.code}`
    })

    appStore.showToast({
      type: 'success',
      title: '设备信息已保存',
      detail: `设备 ${device.value.code} 的维护信息已更新。`
    })
  } catch (err) {
    appStore.showToast({
      type: 'error',
      title: '设备保存失败',
      detail: err instanceof Error ? err.message : '保存设备时发生未知错误。'
    })
  } finally {
    isSaving.value = false
  }
}

async function removeDevice() {
  if (isDeleting.value || !device.value || !authStore.currentUser || !canManage.value) return

  try {
    isDeleting.value = true
    const deleted = workflow.removeDevice(device.value.id)

    auditStore.addLog({
      actorId: authStore.currentUser.id,
      actorRole: authStore.currentUser.role,
      action: 'delete_device',
      targetType: 'device',
      targetId: deleted.id,
      detail: `删除设备：${deleted.code}`
    })

    appStore.showToast({
      type: 'success',
      title: '设备已删除',
      detail: `设备 ${deleted.code} 已移除，正在返回列表页。`
    })

    await router.push('/devices')
  } catch (err) {
    appStore.showToast({
      type: 'error',
      title: '设备删除失败',
      detail: err instanceof Error ? err.message : '删除设备时发生未知错误。'
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
