<template>
  <AppPage eyebrow="设备投放" title="下发记录" :stats="stats">
    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="计划ID">
        <el-input-number v-model="query.planId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="设备ID">
        <el-input-number v-model="query.deviceId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="下发类型">
        <el-select v-model="query.deliveryType" clearable placeholder="全部类型" style="width: 140px">
          <el-option v-for="(item, key) in deliveryTypeMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="下发状态">
        <el-select v-model="query.deliveryStatus" clearable placeholder="全部状态" style="width: 140px">
          <el-option v-for="(item, key) in deliveryStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadDeliveries">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="id" label="记录ID" width="90" />
      <el-table-column prop="planId" label="计划ID" width="110" />
      <el-table-column prop="deviceId" label="设备ID" width="110" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="deliveryTypeMap[row.deliveryType]?.type || 'info'" effect="dark">
            {{ deliveryTypeMap[row.deliveryType]?.label || row.deliveryType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="deliveryStatusMap[row.deliveryStatus]?.type || 'info'" effect="dark">
            {{ deliveryStatusMap[row.deliveryStatus]?.label || row.deliveryStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="responseMsg" label="响应信息" min-width="260" show-overflow-tooltip />
      <el-table-column prop="retryCount" label="重试次数" width="100" />
      <el-table-column prop="deliveryTime" label="下发时间" min-width="170" />
      <el-table-column label="操作" min-width="260" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="user.hasPermission('delivery:operate') && row.deliveryStatus === 'pending'" link type="success" @click="handleSuccess(row.id)">
            模拟成功
          </el-button>
          <el-button v-if="user.hasPermission('delivery:operate') && row.deliveryStatus === 'pending'" link type="danger" @click="handleFail(row.id)">
            模拟失败
          </el-button>
          <el-button v-if="user.hasPermission('delivery:operate') && row.deliveryStatus === 'failed'" link type="warning" @click="handleRetry(row.id)">
            重试
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无下发记录，启动投放计划后会自动生成待下发记录" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadDeliveries"
        @size-change="loadDeliveries"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="下发详情" size="420px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="记录ID">{{ current.id }}</el-descriptions-item>
        <el-descriptions-item label="计划ID">{{ current.planId }}</el-descriptions-item>
        <el-descriptions-item label="设备ID">{{ current.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="下发类型">{{ deliveryTypeMap[current.deliveryType]?.label || current.deliveryType }}</el-descriptions-item>
        <el-descriptions-item label="下发状态">
          <el-tag :type="deliveryStatusMap[current.deliveryStatus]?.type || 'info'" effect="dark">
            {{ deliveryStatusMap[current.deliveryStatus]?.label || current.deliveryStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="响应信息">{{ current.responseMsg || '-' }}</el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ current.retryCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="下发时间">{{ current.deliveryTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { useUserStore } from '@/stores/user'
import {
  deliveryStatusMap,
  deliveryTypeMap,
  fetchDeliveries,
  markDeliveryFailed,
  markDeliverySuccess,
  retryDelivery,
  type AdDeliveryRecord
} from '@/api/deliveries'

const loading = ref(false)
const user = useUserStore()
const drawerVisible = ref(false)
const records = ref<AdDeliveryRecord[]>([])
const current = ref<AdDeliveryRecord>()
const total = ref(0)

const query = reactive({
  planId: undefined as number | undefined,
  deviceId: undefined as number | undefined,
  deliveryType: '',
  deliveryStatus: '',
  page: 1,
  size: 10
})

const stats = computed(() => [
  { label: '全部记录', value: total.value },
  { label: '待下发', value: records.value.filter((item) => item.deliveryStatus === 'pending').length },
  { label: '成功', value: records.value.filter((item) => item.deliveryStatus === 'success').length },
  { label: '失败', value: records.value.filter((item) => item.deliveryStatus === 'failed').length }
])

async function loadDeliveries() {
  loading.value = true
  try {
    const result = await fetchDeliveries(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.planId = undefined
  query.deviceId = undefined
  query.deliveryType = ''
  query.deliveryStatus = ''
  query.page = 1
  loadDeliveries()
}

function openDetail(row: AdDeliveryRecord) {
  current.value = row
  drawerVisible.value = true
}

async function handleSuccess(id: number) {
  await markDeliverySuccess(id, '演示模式：设备已成功接收投放计划')
  ElMessage.success('下发已标记为成功')
  loadDeliveries()
}

async function handleFail(id: number) {
  await ElMessageBox.confirm('确认将该下发记录标记为失败吗？', '模拟失败', { type: 'warning' })
  await markDeliveryFailed(id, '演示模式：设备未响应，请检查网络或终端状态')
  ElMessage.success('下发已标记为失败')
  loadDeliveries()
}

async function handleRetry(id: number) {
  await retryDelivery(id)
  ElMessage.success('已重新加入下发队列')
  loadDeliveries()
}

onMounted(loadDeliveries)
</script>
