<template>
  <AppPage :eyebrow="locale.t('page.bills.finance')" :title="locale.t('page.bills.title')" :stats="stats">
    <template #actions>
      <el-date-picker v-if="user.hasPermission('bill:confirm')" v-model="generateMonth" type="month" value-format="YYYY-MM" :placeholder="locale.t('page.bills.generateMonth')" />
      <el-button v-if="user.hasPermission('bill:confirm')" type="primary" :loading="generating" @click="handleGenerate">{{ locale.t('page.bills.generateDemo') }}</el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.bills.billMonth')">
        <el-date-picker v-model="query.billMonth" type="month" value-format="YYYY-MM" :placeholder="locale.t('page.bills.monthPlaceholder')" />
      </el-form-item>
      <el-form-item v-if="user.isPlatformScope" :label="locale.t('page.bills.advertiserId')">
        <el-input-number v-model="query.advertiserId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.bills.status')">
        <el-select v-model="query.status" clearable :placeholder="locale.t('page.bills.allStatus')" style="width: 140px">
          <el-option v-for="(item, key) in billStatusMap" :key="key" :label="getStatusLabel('bill', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadBills">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="billNo" :label="locale.t('page.bills.billNo')" min-width="190" />
      <el-table-column :label="locale.t('page.bills.billType')" width="120">
        <template #default="{ row }">{{ getStringStatusLabel('billType', row.billType, billTypeMap[row.billType] || row.billType) }}</template>
      </el-table-column>
      <el-table-column prop="advertiserId" :label="locale.t('page.bills.advertiserId')" width="110" />
      <el-table-column prop="billMonth" :label="locale.t('page.bills.month')" width="110" />
      <el-table-column :label="locale.t('page.bills.amountTotal')" width="130">
        <template #default="{ row }">¥{{ formatMoney(row.amountTotal) }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.bills.amountPaid')" width="130">
        <template #default="{ row }">¥{{ formatMoney(row.amountPaid) }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.bills.status')" width="110">
        <template #default="{ row }">
          <el-tag :type="billStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ getStatusLabel('bill', row.status, billStatusMap[row.status]?.label || row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="confirmTime" :label="locale.t('page.bills.confirmTime')" min-width="170" />
      <el-table-column prop="payTime" :label="locale.t('page.bills.payTime')" min-width="170" />
      <el-table-column :label="locale.t('common.operation')" width="205" fixed="right" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetails(row)">{{ locale.t('page.bills.details') }}</el-button>
          <el-button v-if="user.hasPermission('bill:confirm') && row.status === 'pending'" link type="success" @click="handleConfirm(row.id)">{{ locale.t('page.bills.confirm') }}</el-button>
          <el-button v-if="user.hasPermission('bill:pay') && row.status !== 'paid'" link type="warning" @click="openPayDialog(row)">{{ locale.t('page.bills.markPaid') }}</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="locale.t('page.bills.empty')" />
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

    <el-drawer v-model="drawerVisible" :title="locale.t('page.bills.drawerTitle')" size="560px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item :label="locale.t('page.bills.billNo')">{{ current.billNo }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.bills.advertiserId')">{{ current.advertiserId || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.bills.billMonth')">{{ current.billMonth }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.bills.amountTotal')">¥{{ formatMoney(current.amountTotal) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.bills.paymentVoucherNo')">{{ current.paymentVoucherNo || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.bills.paymentVoucherUrl')">
          <el-link v-if="current.paymentVoucherUrl" type="primary" :href="current.paymentVoucherUrl" target="_blank">
            {{ locale.t('page.bills.viewVoucher') }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="details" class="data-table detail-table" row-key="id">
        <el-table-column prop="itemName" :label="locale.t('page.bills.itemName')" min-width="180" />
        <el-table-column prop="adId" :label="locale.t('page.reports.adId')" width="90" />
        <el-table-column prop="planId" :label="locale.t('page.reports.planId')" width="90" />
        <el-table-column prop="itemCount" :label="locale.t('page.reports.playCount')" width="100" />
        <el-table-column :label="locale.t('page.bills.amount')" width="110">
          <template #default="{ row }">¥{{ formatMoney(row.itemAmount) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="payDialogVisible" :title="locale.t('page.bills.payConfirmTitle')" width="520px" destroy-on-close>
      <el-form ref="payFormRef" :model="payForm" :rules="payRules" label-width="120px">
        <el-form-item :label="locale.t('page.bills.paymentVoucherNo')" prop="paymentVoucherNo">
          <el-input v-model.trim="payForm.paymentVoucherNo" :placeholder="locale.t('page.bills.paymentVoucherNoPlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.bills.paymentVoucherUrl')" prop="paymentVoucherUrl">
          <div class="voucher-upload">
            <el-upload
              :show-file-list="false"
              :http-request="handleVoucherUpload"
              :before-upload="beforeVoucherUpload"
            >
              <el-button :loading="voucherUploading">{{ locale.t('page.bills.uploadVoucher') }}</el-button>
            </el-upload>
            <el-input v-model="payForm.paymentVoucherUrl" :placeholder="locale.t('page.bills.paymentVoucherUrlPlaceholder')" />
            <el-image
              v-if="payForm.paymentVoucherUrl"
              class="voucher-preview"
              :src="payForm.paymentVoucherUrl"
              :preview-src-list="[payForm.paymentVoucherUrl]"
              fit="cover"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialogVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="paying" @click="handlePay">{{ locale.t('page.bills.markPaid') }}</el-button>
      </template>
    </el-dialog>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { uploadFile } from '@/api/files'
import { useLocaleStore } from '@/stores/locale'
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
const locale = useLocaleStore()
const user = useUserStore()
const generating = ref(false)
const drawerVisible = ref(false)
const payDialogVisible = ref(false)
const records = ref<AdBill[]>([])
const details = ref<AdBillDetail[]>([])
const current = ref<AdBill>()
const total = ref(0)
const generateMonth = ref(new Date().toISOString().slice(0, 7))
const paying = ref(false)
const voucherUploading = ref(false)
const payBillId = ref<number>()
const payFormRef = ref<FormInstance>()
const payForm = reactive({
  paymentVoucherNo: '',
  paymentVoucherUrl: ''
})

const query = reactive({
  billMonth: '',
  advertiserId: undefined as number | undefined,
  status: '',
  page: 1,
  size: 10
})

const stats = computed(() => [
  { label: getStatusLabel('bill', 'pending', '待确认'), value: records.value.filter((item) => item.status === 'pending').length },
  { label: getStatusLabel('bill', 'confirmed', '待支付'), value: records.value.filter((item) => item.status === 'confirmed').length },
  { label: getStatusLabel('bill', 'paid', '已支付'), value: records.value.filter((item) => item.status === 'paid').length },
  { label: locale.t('page.bills.currentAmount'), value: `¥${formatMoney(records.value.reduce((sum, item) => sum + Number(item.amountTotal || 0), 0))}` }
])

const payRules = computed<FormRules>(() => ({
  paymentVoucherNo: [{ required: true, message: locale.t('page.bills.paymentVoucherNoRequired'), trigger: 'blur' }],
  paymentVoucherUrl: [{ required: true, message: locale.t('page.bills.paymentVoucherUrlRequired'), trigger: 'change' }]
}))

function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function getStringStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

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
    ElMessage.warning(locale.t('page.bills.monthRequired'))
    return
  }
  generating.value = true
  try {
    const result = await generateBills(generateMonth.value)
    ElMessage.success(locale.t('page.bills.generated').replace('{count}', String(result.data.length)))
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
  ElMessage.success(locale.t('page.bills.confirmed'))
  loadBills()
}

function openPayDialog(row: AdBill) {
  payBillId.value = row.id
  payForm.paymentVoucherNo = row.paymentVoucherNo || ''
  payForm.paymentVoucherUrl = row.paymentVoucherUrl || ''
  payDialogVisible.value = true
}

function beforeVoucherUpload(file: File) {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning(locale.t('page.bills.voucherImageOnly'))
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning(locale.t('page.bills.voucherTooLarge'))
    return false
  }
  return true
}

async function handleVoucherUpload(options: UploadRequestOptions) {
  voucherUploading.value = true
  try {
    const result = await uploadFile(options.file as File, 'bill-vouchers')
    payForm.paymentVoucherUrl = result.data.url
    options.onSuccess?.(result)
    ElMessage.success(locale.t('page.bills.voucherUploaded'))
  } catch (error) {
    options.onError?.(error as never)
  } finally {
    voucherUploading.value = false
  }
}

async function handlePay() {
  if (!payBillId.value) {
    return
  }
  await payFormRef.value?.validate()
  paying.value = true
  try {
    await payBill(payBillId.value, {
      paymentVoucherNo: payForm.paymentVoucherNo,
      paymentVoucherUrl: payForm.paymentVoucherUrl
    })
    ElMessage.success(locale.t('page.bills.paid'))
    payDialogVisible.value = false
    loadBills()
  } finally {
    paying.value = false
  }
}

onMounted(loadBills)
</script>

<style scoped>
.detail-table {
  margin-top: 18px;
}

.voucher-upload {
  display: grid;
  width: 100%;
  gap: 10px;
}

.voucher-preview {
  width: 180px;
  height: 110px;
  border: 1px solid rgba(83, 229, 255, 0.18);
  border-radius: 10px;
}
</style>
