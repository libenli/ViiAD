<template>
  <AppPage eyebrow="审计管理" title="操作日志" :stats="stats">
    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="业务模块">
        <el-input v-model="query.moduleName" clearable placeholder="如 广告管理 / 素材管理" />
      </el-form-item>
      <el-form-item label="动作">
        <el-select v-model="query.businessType" clearable placeholder="全部动作" style="width: 150px">
          <el-option v-for="item in businessTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作人">
        <el-input v-model="query.operatorName" clearable placeholder="账号" style="width: 140px" />
      </el-form-item>
      <el-form-item label="结果">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间">
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          range-separator="至"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadLogs">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="logs" class="data-table" row-key="id" @row-click="openDetail">
      <el-table-column prop="moduleName" label="模块" min-width="130" />
      <el-table-column label="动作" width="120">
        <template #default="{ row }">{{ businessTypeLabel(row.businessType) }}</template>
      </el-table-column>
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="operLogStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ operLogStatusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operatorName" label="操作人" min-width="120">
        <template #default="{ row }">{{ row.operatorName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="operatorIp" label="IP 地址" min-width="130" />
      <el-table-column prop="requestMethod" label="方法" width="90" />
      <el-table-column prop="requestUri" label="请求地址" min-width="220" show-overflow-tooltip />
      <el-table-column prop="costTime" label="耗时(ms)" width="110" />
      <el-table-column prop="createTime" label="操作时间" min-width="170" />
      <el-table-column prop="errorMsg" label="异常信息" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.errorMsg || '-' }}</template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无操作日志，执行一次业务查询或保存后会自动生成" />
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

    <el-drawer v-model="drawerVisible" title="日志详情" size="620px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="模块">{{ current.moduleName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ businessTypeLabel(current.businessType) }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="operLogStatusMap[current.status]?.type || 'info'" effect="dark">
            {{ operLogStatusMap[current.status]?.label || current.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ current.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ current.operatorIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求">{{ current.requestMethod }} {{ current.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ current.costTime || 0 }} ms</el-descriptions-item>
        <el-descriptions-item label="时间">{{ current.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <section class="log-json-block">
        <h3>请求参数</h3>
        <pre>{{ prettyText(current?.requestParam) }}</pre>
      </section>
      <section class="log-json-block">
        <h3>响应结果</h3>
        <pre>{{ prettyText(current?.responseResult) }}</pre>
      </section>
      <section v-if="current?.errorMsg" class="log-json-block danger">
        <h3>异常信息</h3>
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

const loading = ref(false)
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

const businessTypeOptions = [
  { label: '查询', value: 'QUERY' },
  { label: '新增', value: 'CREATE' },
  { label: '修改', value: 'UPDATE' },
  { label: '提交', value: 'SUBMIT' },
  { label: '审核通过', value: 'APPROVE' },
  { label: '驳回', value: 'REJECT' },
  { label: '排期', value: 'SCHEDULE' },
  { label: '开始投放', value: 'START' },
  { label: '暂停', value: 'PAUSE' },
  { label: '结束', value: 'FINISH' },
  { label: '上传', value: 'UPLOAD' },
  { label: '生成', value: 'GENERATE' },
  { label: '确认', value: 'CONFIRM' },
  { label: '支付', value: 'PAY' }
]

const businessTypeMap = businessTypeOptions.reduce<Record<string, string>>((map, item) => {
  map[item.value] = item.label
  return map
}, {})

const stats = computed(() => [
  { label: '日志总数', value: total.value },
  { label: '本页成功', value: logs.value.filter((item) => item.status === 1).length },
  { label: '本页失败', value: logs.value.filter((item) => item.status === 0).length },
  { label: '平均耗时', value: `${averageCost.value} ms` }
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
  return value ? businessTypeMap[value] || value : '-'
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
