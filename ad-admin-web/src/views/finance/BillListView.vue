<template>
  <AppPage eyebrow="财务结算" title="账单结算" :stats="stats">
    <template #actions>
      <el-date-picker v-if="user.hasPermission('bill:confirm')" v-model="generateMonth" type="month" value-format="YYYY-MM" placeholder="选择生成月份" />
      <el-button v-if="user.hasPermission('bill:confirm')" type="primary" :loading="generating" @click="handleGenerate">生成演示账单</el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="账单月份">
        <el-date-picker v-model="query.billMonth" type="month" value-format="YYYY-MM" placeholder="选择月份" />
      </el-form-item>
      <el-form-item v-if="user.isPlatformScope" label="广告主ID">
        <el-input-number v-model="query.advertiserId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="账单状态">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option v-for="(item, key) in billStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadBills">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="billNo" label="账单编号" min-width="190" />
      <el-table-column label="账单类型" width="120">
        <template #default="{ row }">{{ billTypeMap[row.billType] || row.billType }}</template>
      </el-table-column>
      <el-table-column prop="advertiserId" label="广告主ID" width="110" />
      <el-table-column prop="billMonth" label="月份" width="110" />
      <el-table-column label="应收金额" width="130">
        <template #default="{ row }">¥{{ formatMoney(row.amountTotal) }}</template>
      </el-table-column>
      <el-table-column label="已收金额" width="130">
        <template #default="{ row }">¥{{ formatMoney(row.amountPaid) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="billStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ billStatusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="confirmTime" label="确认时间" min-width="170" />
      <el-table-column prop="payTime" label="支付时间" min-width="170" />
      <el-table-column label="操作" min-width="230" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetails(row)">明细</el-button>
          <el-button v-if="user.hasPermission('bill:confirm') && row.status === 'pending'" link type="success" @click="handleConfirm(row.id)">确认</el-button>
          <el-button v-if="user.hasPermission('bill:pay') && row.status !== 'paid'" link type="warning" @click="handlePay(row.id)">标记已支付</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无账单，可先在报表产生播放日志后生成演示账单" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadBills"
        @size-change="loadBills"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="账单明细" size="560px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="账单编号">{{ current.billNo }}</el-descriptions-item>
        <el-descriptions-item label="广告主ID">{{ current.advertiserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账单月份">{{ current.billMonth }}</el-descriptions-item>
        <el-descriptions-item label="应收金额">¥{{ formatMoney(current.amountTotal) }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="details" class="data-table detail-table" row-key="id">
        <el-table-column prop="itemName" label="计费项目" min-width="180" />
        <el-table-column prop="adId" label="广告ID" width="90" />
        <el-table-column prop="planId" label="计划ID" width="90" />
        <el-table-column prop="itemCount" label="播放次数" width="100" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ formatMoney(row.itemAmount) }}</template>
        </el-table-column>
      </el-table>
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
  billStatusMap,
  billTypeMap,
  confirmBill,
  fetchBillDetails,
  fetchBills,
  generateBills,
  payBill,
  type AdBill,
  type AdBillDetail
} from '@/api/bills'

const loading = ref(false)
const user = useUserStore()
const generating = ref(false)
const drawerVisible = ref(false)
const records = ref<AdBill[]>([])
const details = ref<AdBillDetail[]>([])
const current = ref<AdBill>()
const total = ref(0)
const generateMonth = ref(new Date().toISOString().slice(0, 7))

const query = reactive({
  billMonth: '',
  advertiserId: undefined as number | undefined,
  status: '',
  page: 1,
  size: 10
})

const stats = computed(() => [
  { label: '待确认', value: records.value.filter((item) => item.status === 'pending').length },
  { label: '待支付', value: records.value.filter((item) => item.status === 'confirmed').length },
  { label: '已支付', value: records.value.filter((item) => item.status === 'paid').length },
  { label: '当前金额', value: `¥${formatMoney(records.value.reduce((sum, item) => sum + Number(item.amountTotal || 0), 0))}` }
])

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

async function loadBills() {
  loading.value = true
  try {
    const result = await fetchBills(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.billMonth = ''
  query.advertiserId = undefined
  query.status = ''
  query.page = 1
  loadBills()
}

async function handleGenerate() {
  if (!generateMonth.value) {
    ElMessage.warning('请先选择账单月份')
    return
  }
  generating.value = true
  try {
    const result = await generateBills(generateMonth.value)
    ElMessage.success(`已生成/读取 ${result.data.length} 张账单`)
    query.billMonth = generateMonth.value
    loadBills()
  } finally {
    generating.value = false
  }
}

async function openDetails(row: AdBill) {
  current.value = row
  const result = await fetchBillDetails(row.id)
  details.value = result.data
  drawerVisible.value = true
}

async function handleConfirm(id: number) {
  await confirmBill(id)
  ElMessage.success('账单已确认')
  loadBills()
}

async function handlePay(id: number) {
  await ElMessageBox.confirm('确认将该账单标记为已支付吗？', '支付确认', { type: 'warning' })
  await payBill(id)
  ElMessage.success('账单已标记为已支付')
  loadBills()
}

onMounted(loadBills)
</script>

<style scoped>
.detail-table {
  margin-top: 18px;
}
</style>
