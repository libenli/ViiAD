<template>
  <section>
    <div class="page-title">
      <div>
        <h1>设备管理</h1>
        <p class="page-subtitle">
          查看设备在线情况、当前投放占用和最近心跳状态，并在右侧抽屉中处理运维动作。
        </p>
      </div>
    </div>

    <div v-if="readonlyHint" class="permission-note">
      {{ readonlyHint }}
    </div>

    <div class="card filter-card">
      <div class="filter-grid">
        <input v-model.trim="filters.keyword" class="input" placeholder="搜索设备编号 / 楼宇 / 区域" />
        <select v-model="filters.status" class="select">
          <option value="">全部状态</option>
          <option value="online">在线</option>
          <option value="offline">离线</option>
          <option value="fault">故障</option>
        </select>
        <input v-model.trim="filters.region" class="input" placeholder="按区域筛选" />
        <input v-model.trim="filters.building" class="input" placeholder="按楼宇筛选" />
      </div>
    </div>

    <div class="card-grid">
      <div class="card">
        <div>设备总数</div>
        <div class="metric-value">{{ filteredDevices.length }}</div>
      </div>
      <div class="card">
        <div>在线</div>
        <div class="metric-value">{{ onlineCount }}</div>
      </div>
      <div class="card">
        <div>离线</div>
        <div class="metric-value">{{ offlineCount }}</div>
      </div>
      <div class="card">
        <div>故障</div>
        <div class="metric-value">{{ faultCount }}</div>
      </div>
    </div>

    <div v-if="filteredDevices.length === 0" class="empty-state">
      当前筛选条件下没有设备记录，请调整状态、区域或楼宇条件后再查看。
    </div>

    <div v-else class="content-columns">
      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>设备编号</th>
              <th>楼宇</th>
              <th>区域</th>
              <th>状态</th>
              <th>当前广告</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="device in pagedDevices"
              :key="device.id"
              :class="{ selected: selectedDevice?.id === device.id }"
              @click="selectedDeviceId = device.id"
            >
              <td><RouterLink :to="`/devices/${device.id}`">{{ device.code }}</RouterLink></td>
              <td>{{ device.building }} {{ device.floor }}</td>
              <td>{{ device.region }}</td>
              <td>
                <span class="status" :class="statusClass(device.status)">
                  {{ statusLabels[device.status] }}
                </span>
              </td>
              <td>{{ device.currentAdId || '-' }}</td>
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
        <template v-if="selectedDevice">
          <div class="drawer-head">
            <div>
              <h3>{{ selectedDevice.code }}</h3>
              <p class="muted">{{ selectedDevice.building }} / {{ selectedDevice.floor }}</p>
            </div>
            <RouterLink class="btn secondary" :to="`/devices/${selectedDevice.id}`">进入详情</RouterLink>
          </div>

          <table class="table compact">
            <tbody>
              <tr><th>状态</th><td>{{ statusLabels[selectedDevice.status] }}</td></tr>
              <tr><th>区域</th><td>{{ selectedDevice.region }}</td></tr>
              <tr><th>屏幕规格</th><td>{{ selectedDevice.screenSize }}</td></tr>
              <tr><th>当前计划</th><td>{{ selectedDevice.currentPlanId || '-' }}</td></tr>
              <tr><th>最后心跳</th><td>{{ selectedDevice.lastHeartbeatAt }}</td></tr>
            </tbody>
          </table>

          <div class="editor-panel">
            <h4>快捷操作</h4>
            <div class="toolbar-group">
              <button v-if="canOperate" class="btn secondary" @click="changeStatus('online')">
                设为在线
              </button>
              <button v-if="canOperate" class="btn secondary" @click="changeStatus('offline')">
                设为离线
              </button>
              <button v-if="canOperate" class="btn secondary" @click="changeStatus('fault')">
                标记故障
              </button>
              <button v-if="canOperate" class="btn secondary" @click="toggleEdit">
                {{ editing ? '取消编辑' : '编辑设备' }}
              </button>
            </div>
          </div>

          <div v-if="editing && editForm" class="editor-panel">
            <h4>右侧快速编辑</h4>
            <div class="editor-grid">
              <label>
                <span>楼宇</span>
                <input v-model.trim="editForm.building" class="input" />
              </label>
              <label>
                <span>楼层</span>
                <input v-model.trim="editForm.floor" class="input" />
              </label>
              <label>
                <span>区域</span>
                <input v-model.trim="editForm.region" class="input" />
              </label>
              <label>
                <span>屏幕规格</span>
                <input v-model.trim="editForm.screenSize" class="input" />
              </label>
            </div>
            <div class="toolbar-group" style="margin-top: 12px">
              <button class="btn" @click="saveEdit">保存修改</button>
            </div>
          </div>
        </template>

        <div v-else class="empty-state drawer-empty">
          请选择一台设备，在右侧查看状态和运维动作。
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'

const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()

const statusLabels = {
  online: '在线',
  offline: '离线',
  fault: '故障'
} as const

const filters = reactive({
  keyword: '',
  status: '',
  region: '',
  building: ''
})

const selectedDeviceId = ref('')
const pageIndex = ref(1)
const pageSize = 6
const editing = ref(false)
const editForm = ref<null | {
  building: string
  floor: string
  region: string
  screenSize: string
}>(null)

const canOperate = computed(() =>
  ['ops', 'area_admin', 'super_admin'].includes(authStore.role ?? '')
)

const readonlyHint = computed(() => {
  if (canOperate.value) {
    return '当前角色可重点演示设备在线、离线、故障处理和设备信息维护。'
  }
  return '当前角色在设备页以查看为主，如需执行运维动作，请切换到运维、区域管理员或超级管理员账号。'
})

const filteredDevices = computed(() =>
  workflow.devices.filter((item) => {
    const keyword = filters.keyword.toLowerCase()
    const hitKeyword =
      !keyword ||
      item.code.toLowerCase().includes(keyword) ||
      item.building.toLowerCase().includes(keyword) ||
      item.region.toLowerCase().includes(keyword)
    const hitStatus = !filters.status || item.status === filters.status
    const hitRegion = !filters.region || item.region.includes(filters.region)
    const hitBuilding = !filters.building || item.building.includes(filters.building)
    return hitKeyword && hitStatus && hitRegion && hitBuilding
  })
)

const pageCount = computed(() => Math.max(1, Math.ceil(filteredDevices.value.length / pageSize)))
const pagedDevices = computed(() =>
  filteredDevices.value.slice((pageIndex.value - 1) * pageSize, pageIndex.value * pageSize)
)

const selectedDevice = computed(
  () =>
    filteredDevices.value.find((item) => item.id === selectedDeviceId.value) ??
    filteredDevices.value[0]
)

const onlineCount = computed(() => filteredDevices.value.filter((item) => item.status === 'online').length)
const offlineCount = computed(() => filteredDevices.value.filter((item) => item.status === 'offline').length)
const faultCount = computed(() => filteredDevices.value.filter((item) => item.status === 'fault').length)

watch(
  filteredDevices,
  (list) => {
    if (!list.length) {
      selectedDeviceId.value = ''
      editing.value = false
      editForm.value = null
      return
    }
    if (!list.find((item) => item.id === selectedDeviceId.value)) {
      selectedDeviceId.value = list[0].id
    }
    if (pageIndex.value > pageCount.value) {
      pageIndex.value = pageCount.value
    }
  },
  { immediate: true }
)

function statusClass(status: keyof typeof statusLabels) {
  if (status === 'online') return 'status-live'
  if (status === 'fault') return 'status-offline'
  return 'status-pending'
}

function changeStatus(status: 'online' | 'offline' | 'fault') {
  if (!selectedDevice.value || !authStore.currentUser) return
  workflow.updateDeviceStatus(selectedDevice.value.id, status)
  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: 'update_device_status',
    targetType: 'device',
    targetId: selectedDevice.value.id,
    detail: `设备状态更新为 ${statusLabels[status]}`
  })
}

function toggleEdit() {
  if (!selectedDevice.value) return
  editing.value = !editing.value
  if (editing.value) {
    editForm.value = {
      building: selectedDevice.value.building,
      floor: selectedDevice.value.floor,
      region: selectedDevice.value.region,
      screenSize: selectedDevice.value.screenSize
    }
  } else {
    editForm.value = null
  }
}

function saveEdit() {
  if (!selectedDevice.value || !editForm.value || !authStore.currentUser) return
  workflow.updateDevice(selectedDevice.value.id, {
    building: editForm.value.building,
    floor: editForm.value.floor,
    region: editForm.value.region,
    screenSize: editForm.value.screenSize
  })
  auditStore.addLog({
    actorId: authStore.currentUser.id,
    actorRole: authStore.currentUser.role,
    action: 'update_device',
    targetType: 'device',
    targetId: selectedDevice.value.id,
    detail: `更新设备信息：${selectedDevice.value.code}`
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
