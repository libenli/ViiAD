<template>
  <AppPage :eyebrow="locale.t('page.deviceDelivery')" :title="locale.t('page.deliveries.title')" :stats="stats">
    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.deliveries.planId')">
        <el-input-number v-model="query.planId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.deliveries.deviceId')">
        <el-input-number v-model="query.deviceId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.deliveries.deliveryType')">
        <el-select v-model="query.deliveryType" clearable :placeholder="locale.t('page.deliveries.allTypes')" style="width: 140px">
          <el-option v-for="(item, key) in deliveryTypeMap" :key="key" :label="getStatusLabel('deliveryType', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.deliveries.deliveryStatus')">
        <el-select v-model="query.deliveryStatus" clearable :placeholder="locale.t('page.deliveries.allStatus')" style="width: 140px">
          <el-option v-for="(item, key) in deliveryStatusMap" :key="key" :label="getStatusLabel('deliveryRecord', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadDeliveries">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="id" :label="locale.t('page.deliveries.recordId')" width="90" />
      <el-table-column prop="planId" :label="locale.t('page.deliveries.planId')" width="110" />
      <el-table-column prop="deviceId" :label="locale.t('page.deliveries.deviceId')" width="110" />
      <el-table-column :label="locale.t('page.deliveries.type')" width="100">
        <template #default="{ row }">
          <el-tag :type="deliveryTypeMap[row.deliveryType]?.type || 'info'" effect="dark">
            {{ getStatusLabel('deliveryType', row.deliveryType, deliveryTypeMap[row.deliveryType]?.label || row.deliveryType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.deliveries.status')" width="110">
        <template #default="{ row }">
          <el-tag :type="deliveryStatusMap[row.deliveryStatus]?.type || 'info'" effect="dark">
            {{ getStatusLabel('deliveryRecord', row.deliveryStatus, deliveryStatusMap[row.deliveryStatus]?.label || row.deliveryStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="responseMsg" :label="locale.t('page.deliveries.responseMsg')" min-width="260" show-overflow-tooltip />
      <el-table-column prop="retryCount" :label="locale.t('page.deliveries.retryCount')" width="100" />
      <el-table-column prop="deliveryTime" :label="locale.t('page.deliveries.deliveryTime')" min-width="170" />
      <el-table-column :label="locale.t('common.operation')" width="215" fixed="right" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ locale.t('common.detail') }}</el-button>
          <el-button v-if="user.hasPermission('delivery:operate') && row.deliveryStatus === 'pending'" link type="success" @click="handleSuccess(row.id)">
            {{ locale.t('page.deliveries.simulateSuccess') }}
          </el-button>
          <el-button v-if="user.hasPermission('delivery:operate') && row.deliveryStatus === 'pending'" link type="danger" @click="handleFail(row.id)">
            {{ locale.t('page.deliveries.simulateFailed') }}
          </el-button>
          <el-button v-if="user.hasPermission('delivery:operate') && row.deliveryStatus === 'failed'" link type="warning" @click="handleRetry(row.id)">
            {{ locale.t('page.deliveries.retry') }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="locale.t('page.deliveries.empty')" />
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

    <el-drawer v-model="drawerVisible" :title="locale.t('page.deliveries.drawerTitle')" size="420px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item :label="locale.t('page.deliveries.recordId')">{{ current.id }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.deliveries.planId')">{{ current.planId }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.deliveries.deviceId')">{{ current.deviceId }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.deliveries.deliveryType')">{{ getStatusLabel('deliveryType', current.deliveryType, deliveryTypeMap[current.deliveryType]?.label || current.deliveryType) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.deliveries.deliveryStatus')">
          <el-tag :type="deliveryStatusMap[current.deliveryStatus]?.type || 'info'" effect="dark">
            {{ getStatusLabel('deliveryRecord', current.deliveryStatus, deliveryStatusMap[current.deliveryStatus]?.label || current.deliveryStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.deliveries.responseMsg')">{{ current.responseMsg || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.deliveries.retryCount')">{{ current.retryCount || 0 }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.deliveries.deliveryTime')">{{ current.deliveryTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { useLocaleStore } from '@/stores/locale'
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
const locale = useLocaleStore()
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
  { label: locale.t('page.deliveries.total'), value: total.value },
  { label: getStatusLabel('deliveryRecord', 'pending', '待下发'), value: records.value.filter((item) => item.deliveryStatus === 'pending').length },
  { label: getStatusLabel('deliveryRecord', 'success', '成功'), value: records.value.filter((item) => item.deliveryStatus === 'success').length },
  { label: getStatusLabel('deliveryRecord', 'failed', '失败'), value: records.value.filter((item) => item.deliveryStatus === 'failed').length }
])

function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

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
  await markDeliverySuccess(id, locale.t('page.deliveries.successResponse'))
  ElMessage.success(locale.t('page.deliveries.markedSuccess'))
  loadDeliveries()
}

async function handleFail(id: number) {
  await ElMessageBox.confirm(locale.t('page.deliveries.failConfirm'), locale.t('page.deliveries.failConfirmTitle'), { type: 'warning' })
  await markDeliveryFailed(id, locale.t('page.deliveries.failedResponse'))
  ElMessage.success(locale.t('page.deliveries.markedFailed'))
  loadDeliveries()
}

async function handleRetry(id: number) {
  await retryDelivery(id)
  ElMessage.success(locale.t('page.deliveries.retried'))
  loadDeliveries()
}

onMounted(loadDeliveries)
</script>
