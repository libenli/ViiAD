<template>
  <AppPage :eyebrow="locale.t('page.logs.audit')" :title="locale.t('page.logs.title')" :stats="stats">
    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.logs.moduleName')">
        <el-input v-model="query.moduleName" clearable :placeholder="locale.t('page.logs.modulePlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.logs.action')">
        <el-select v-model="query.businessType" clearable :placeholder="locale.t('page.logs.allActions')" style="width: 150px">
          <el-option v-for="item in businessTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.logs.operator')">
        <el-input v-model="query.operatorName" clearable :placeholder="locale.t('page.logs.operatorPlaceholder')" style="width: 140px" />
      </el-form-item>
      <el-form-item :label="locale.t('page.logs.result')">
        <el-select v-model="query.status" clearable :placeholder="locale.t('page.logs.allResults')" style="width: 120px">
          <el-option :label="locale.t('status.operLog.success')" :value="1" />
          <el-option :label="locale.t('status.operLog.failed')" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.logs.time')">
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          :start-placeholder="locale.t('page.logs.startTime')"
          :end-placeholder="locale.t('page.logs.endTime')"
          :range-separator="locale.t('page.logs.rangeSeparator')"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadLogs">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="logs" class="data-table" row-key="id" @row-click="openDetail">
      <el-table-column prop="moduleName" :label="locale.t('page.logs.module')" min-width="130" />
      <el-table-column :label="locale.t('page.logs.action')" width="120">
        <template #default="{ row }">{{ businessTypeLabel(row.businessType) }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.logs.result')" width="100">
        <template #default="{ row }">
          <el-tag :type="operLogStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ row.status === 1 ? locale.t('status.operLog.success') : locale.t('status.operLog.failed') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operatorName" :label="locale.t('page.logs.operator')" min-width="120">
        <template #default="{ row }">{{ row.operatorName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="operatorIp" :label="locale.t('page.logs.ipAddress')" min-width="130" />
      <el-table-column prop="requestMethod" :label="locale.t('page.logs.method')" width="90" />
      <el-table-column prop="requestUri" :label="locale.t('page.logs.requestUri')" min-width="220" show-overflow-tooltip />
      <el-table-column prop="costTime" :label="locale.t('page.logs.costTime')" width="110" />
      <el-table-column prop="createTime" :label="locale.t('page.logs.operationTime')" min-width="170" />
      <el-table-column prop="errorMsg" :label="locale.t('page.logs.errorMsg')" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.errorMsg || '-' }}</template>
      </el-table-column>
      <template #empty>
        <el-empty :description="locale.t('page.logs.empty')" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadLogs"
        @size-change="loadLogs"
      />
    </div>

    <el-drawer v-model="drawerVisible" :title="locale.t('page.logs.drawerTitle')" size="620px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item :label="locale.t('page.logs.module')">{{ current.moduleName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.logs.action')">{{ businessTypeLabel(current.businessType) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.logs.result')">
          <el-tag :type="operLogStatusMap[current.status]?.type || 'info'" effect="dark">
            {{ current.status === 1 ? locale.t('status.operLog.success') : locale.t('status.operLog.failed') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.logs.operator')">{{ current.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.logs.ip')">{{ current.operatorIp || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.logs.request')">{{ current.requestMethod }} {{ current.requestUri }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.logs.cost')">{{ current.costTime || 0 }} ms</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.logs.time')">{{ current.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <section class="log-json-block">
        <h3>{{ locale.t('page.logs.requestParam') }}</h3>
        <pre>{{ prettyText(current?.requestParam) }}</pre>
      </section>
      <section class="log-json-block">
        <h3>{{ locale.t('page.logs.responseResult') }}</h3>
        <pre>{{ prettyText(current?.responseResult) }}</pre>
      </section>
      <section v-if="current?.errorMsg" class="log-json-block danger">
        <h3>{{ locale.t('page.logs.errorMsg') }}</h3>
        <pre>{{ current.errorMsg }}</pre>
      </section>
    </el-drawer>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { fetchOperLogs, type SysOperLog } from '@/api/system'
import { operLogStatusMap } from '@/config/status'
import { useLocaleStore } from '@/stores/locale'

const loading = ref(false)
const locale = useLocaleStore()
const logs = ref<SysOperLog[]>([])
const total = ref(0)
const drawerVisible = ref(false)
const current = ref<SysOperLog>()
const timeRange = ref<string[]>([])

const query = reactive({
  moduleName: '',
  businessType: '',
  operatorName: '',
  status: undefined as number | undefined,
  startTime: '',
  endTime: '',
  page: 1,
  size: 10
})

const businessTypeValues = ['QUERY', 'CREATE', 'UPDATE', 'SUBMIT', 'APPROVE', 'REJECT', 'SCHEDULE', 'START', 'PAUSE', 'FINISH', 'UPLOAD', 'GENERATE', 'CONFIRM', 'PAY']
const businessTypeOptions = computed(() => businessTypeValues.map((value) => ({ label: businessTypeLabel(value), value })))

const stats = computed(() => [
  { label: locale.t('page.logs.total'), value: total.value },
  { label: locale.t('page.logs.pageSuccess'), value: logs.value.filter((item) => item.status === 1).length },
  { label: locale.t('page.logs.pageFailed'), value: logs.value.filter((item) => item.status === 0).length },
  { label: locale.t('page.logs.averageCost'), value: `${averageCost.value} ms` }
])

const averageCost = computed(() => {
  if (!logs.value.length) {
    return 0
  }
  const totalCost = logs.value.reduce((sum, item) => sum + (item.costTime || 0), 0)
  return Math.round(totalCost / logs.value.length)
})

function normalizeTimeRange() {
  query.startTime = timeRange.value?.[0] || ''
  query.endTime = timeRange.value?.[1] || ''
}

async function loadLogs() {
  normalizeTimeRange()
  loading.value = true
  try {
    const result = await fetchOperLogs(query)
    logs.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, {
    moduleName: '',
    businessType: '',
    operatorName: '',
    status: undefined,
    startTime: '',
    endTime: '',
    page: 1
  })
  timeRange.value = []
  loadLogs()
}

function openDetail(row: SysOperLog) {
  current.value = row
  drawerVisible.value = true
}

function businessTypeLabel(value?: string) {
  return value ? locale.t(`status.businessType.${value}`, value) : '-'
}

function prettyText(value?: string) {
  if (!value) {
    return '-'
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

onMounted(loadLogs)
</script>

<style scoped>
.log-json-block {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid rgba(83, 229, 255, 0.14);
  border-radius: 12px;
  background: rgba(3, 15, 23, 0.76);
}

.log-json-block h3 {
  margin: 0 0 10px;
  color: #eafaff;
  font-size: 15px;
}

.log-json-block pre {
  max-height: 260px;
  margin: 0;
  overflow: auto;
  color: #cfeeff;
  white-space: pre-wrap;
  word-break: break-all;
}

.log-json-block.danger {
  border-color: rgba(255, 111, 111, 0.28);
}
</style>
