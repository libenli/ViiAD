<template>
  <AppPage eyebrow="设备投放" title="设备管理" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('device:manage')" type="primary" :icon="Plus" @click="router.push('/devices/create')">
        新建设备
      </el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="设备名称 / 编号 / IP" />
      </el-form-item>
      <el-form-item label="楼宇ID">
        <el-input-number v-model="query.buildingId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="在线状态">
        <el-select v-model="query.onlineStatus" clearable placeholder="全部在线" style="width: 140px">
          <el-option v-for="(item, key) in onlineStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="故障状态">
        <el-select v-model="query.faultStatus" clearable placeholder="全部故障" style="width: 140px">
          <el-option v-for="(item, key) in faultStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="设备状态">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option v-for="(item, key) in deviceStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadDevices">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="deviceCode" label="设备编号" min-width="180" />
      <el-table-column prop="deviceName" label="设备名称" min-width="180" />
      <el-table-column prop="buildingId" label="楼宇ID" width="100" />
      <el-table-column prop="floorNo" label="楼层" width="90" />
      <el-table-column prop="screenSize" label="屏幕" width="120" />
      <el-table-column prop="resolution" label="分辨率" width="130" />
      <el-table-column prop="ipAddress" label="IP地址" min-width="140" />
      <el-table-column label="在线" width="90">
        <template #default="{ row }">
          <el-tag :type="onlineStatusMap[row.onlineStatus]?.type || 'info'" effect="dark">
            {{ onlineStatusMap[row.onlineStatus]?.label || row.onlineStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="故障" width="90">
        <template #default="{ row }">
          <el-tag :type="faultStatusMap[row.faultStatus]?.type || 'info'" effect="dark">
            {{ faultStatusMap[row.faultStatus]?.label || row.faultStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="deviceStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ deviceStatusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentPlanId" label="当前计划" width="110" />
      <el-table-column prop="lastOnlineTime" label="最后在线" min-width="170" />
      <el-table-column label="操作" min-width="330" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/devices/${row.id}`)">详情</el-button>
          <el-button v-if="user.hasPermission('device:manage')" link type="primary" @click="router.push(`/devices/${row.id}/edit`)">编辑</el-button>
          <el-button v-if="user.hasPermission('device:manage') && row.onlineStatus !== 'online' && row.status === 'active'" link type="success" @click="handleOnline(row.id)">
            上线
          </el-button>
          <el-button v-if="user.hasPermission('device:manage') && row.onlineStatus === 'online'" link type="warning" @click="handleOffline(row.id)">
            离线
          </el-button>
          <el-button v-if="user.hasPermission('device:manage') && row.faultStatus !== 'fault'" link type="danger" @click="handleFault(row.id)">
            标故障
          </el-button>
          <el-button v-if="user.hasPermission('device:manage') && row.faultStatus === 'fault'" link type="success" @click="handleRepair(row.id)">
            恢复
          </el-button>
          <el-button v-if="user.hasPermission('device:manage') && row.status === 'active'" link type="warning" @click="handleDisable(row.id)">
            停用
          </el-button>
          <el-button v-else-if="user.hasPermission('device:manage')" link type="success" @click="handleEnable(row.id)">启用</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无设备，可先新建设备用于投放计划绑定" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadDevices"
        @size-change="loadDevices"
      />
    </div>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { useUserStore } from '@/stores/user'
import {
  deviceStatusMap,
  disableDevice,
  enableDevice,
  faultStatusMap,
  fetchDevices,
  markDeviceFault,
  onlineStatusMap,
  repairDevice,
  setDeviceOffline,
  setDeviceOnline,
  type AdDevice
} from '@/api/devices'

const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const records = ref<AdDevice[]>([])
const total = ref(0)

const query = reactive({
  keyword: '',
  buildingId: undefined as number | undefined,
  onlineStatus: '',
  faultStatus: '',
  status: '',
  page: 1,
  size: 10
})

const stats = computed(() => [
  { label: '全部设备', value: total.value },
  { label: '在线', value: records.value.filter((item) => item.onlineStatus === 'online').length },
  { label: '离线', value: records.value.filter((item) => item.onlineStatus === 'offline').length },
  { label: '故障', value: records.value.filter((item) => item.faultStatus === 'fault').length }
])

async function loadDevices() {
  loading.value = true
  try {
    const result = await fetchDevices(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.buildingId = undefined
  query.onlineStatus = ''
  query.faultStatus = ''
  query.status = ''
  query.page = 1
  loadDevices()
}

async function handleOnline(id: number) {
  await setDeviceOnline(id)
  ElMessage.success('设备已上线')
  loadDevices()
}

async function handleOffline(id: number) {
  await setDeviceOffline(id)
  ElMessage.success('设备已离线')
  loadDevices()
}

async function handleFault(id: number) {
  await ElMessageBox.confirm('确认将该设备标记为故障吗？故障设备不能加入新的投放计划。', '标记故障', {
    type: 'warning'
  })
  await markDeviceFault(id)
  ElMessage.success('设备已标记为故障')
  loadDevices()
}

async function handleRepair(id: number) {
  await repairDevice(id)
  ElMessage.success('设备已恢复正常')
  loadDevices()
}

async function handleDisable(id: number) {
  await ElMessageBox.confirm('确认停用该设备吗？停用后会自动离线，且不能加入投放计划。', '停用设备', {
    type: 'warning'
  })
  await disableDevice(id)
  ElMessage.success('设备已停用')
  loadDevices()
}

async function handleEnable(id: number) {
  await enableDevice(id)
  ElMessage.success('设备已启用')
  loadDevices()
}

onMounted(loadDevices)
</script>
