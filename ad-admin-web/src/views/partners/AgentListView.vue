<template>
  <AppPage :eyebrow="locale.t('page.partners.subject')" :title="locale.t('page.partners.agentTitle')" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('partner:manage')" type="primary" :icon="Plus" @click="openCreate">{{ locale.t('page.partners.createAgent') }}</el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.partners.keyword')">
        <el-input v-model="query.keyword" clearable :placeholder="locale.t('page.partners.agentKeywordPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.partners.status')">
        <el-select v-model="query.status" clearable :placeholder="locale.t('page.partners.allStatus')" style="width: 140px">
          <el-option v-for="(item, key) in partnerStatusMap" :key="key" :label="getStatusLabel('partner', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadAgents">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="agentCode" :label="locale.t('page.partners.agentCode')" min-width="170" />
      <el-table-column prop="agentName" :label="locale.t('page.partners.agentName')" min-width="190" />
      <el-table-column prop="contactName" :label="locale.t('page.partners.contactName')" width="110" />
      <el-table-column prop="contactPhone" :label="locale.t('page.partners.contactPhone')" min-width="140" />
      <el-table-column prop="contactEmail" :label="locale.t('page.partners.contactEmail')" min-width="170" />
      <el-table-column :label="locale.t('page.partners.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="partnerStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ getStatusLabel('partner', row.status, partnerStatusMap[row.status]?.label || row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="locale.t('page.partners.createTime')" min-width="170" />
      <el-table-column :label="locale.t('common.operation')" width="170" fixed="right" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ locale.t('common.detail') }}</el-button>
          <el-button v-if="user.hasPermission('partner:manage')" link type="primary" @click="openEdit(row)">{{ locale.t('common.edit') }}</el-button>
          <el-button v-if="user.hasPermission('partner:manage') && row.status === 'active'" link type="warning" @click="handleDisable(row.id)">{{ locale.t('page.partners.disable') }}</el-button>
          <el-button v-else-if="user.hasPermission('partner:manage')" link type="success" @click="handleEnable(row.id)">{{ locale.t('page.partners.enable') }}</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="locale.t('page.partners.agentEmpty')" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadAgents"
        @size-change="loadAgents"
      />
    </div>

    <el-drawer v-model="drawerVisible" :title="locale.t('page.partners.agentDrawer')" size="560px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item :label="locale.t('page.partners.code')">{{ current.agentCode }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.name')">{{ current.agentName }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.contactName')">{{ current.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.contactPhone')">{{ current.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.email')">{{ current.contactEmail || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.remark')">{{ current.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <section class="wallet-panel">
        <div class="panel-head">
          <h3>{{ locale.t('page.wallet.accountOverview') }}</h3>
          <el-button v-if="user.hasPermission('wallet:recharge:create') && !walletAccount" link type="primary" :loading="walletLoading" @click="handleEnsureWallet">
            {{ locale.t('page.wallet.ensureAccount') }}
          </el-button>
        </div>
        <el-skeleton v-if="walletLoading" :rows="3" animated />
        <el-descriptions v-else-if="walletAccount" :column="2" border>
          <el-descriptions-item :label="locale.t('page.wallet.availableBalance')">¥{{ formatMoney(walletAccount.availableBalance) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.frozenBalance')">¥{{ formatMoney(walletAccount.frozenBalance) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.totalRecharge')">¥{{ formatMoney(walletAccount.totalRecharge) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.wallet.totalConsumed')">¥{{ formatMoney(walletAccount.totalConsumed) }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else :description="locale.t('page.wallet.accountEmpty')" />
        <el-table :data="walletCharges" class="data-table compact-table" row-key="id">
          <el-table-column prop="chargeNo" :label="locale.t('page.wallet.chargeNo')" min-width="160" show-overflow-tooltip />
          <el-table-column :label="locale.t('page.wallet.chargeAmount')" width="110">
            <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column :label="locale.t('page.bills.status')" width="100">
            <template #default="{ row }">{{ locale.t(`status.playCharge.${row.status}`, row.status) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="editingId ? locale.t('page.partners.editAgent') : locale.t('page.partners.createAgent')" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="locale.t('page.partners.agentName')" prop="agentName">
          <el-input v-model="form.agentName" :placeholder="locale.t('page.partners.agentNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.partners.contactName')">
          <el-input v-model="form.contactName" :placeholder="locale.t('page.partners.contactPlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.partners.contactPhone')" prop="contactPhone">
          <el-input v-model="form.contactPhone" :placeholder="locale.t('page.partners.phonePlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.partners.contactEmail')" prop="contactEmail">
          <el-input v-model="form.contactEmail" :placeholder="locale.t('page.partners.emailPlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.partners.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="locale.t('page.partners.remarkPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ locale.t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { useLocaleStore } from '@/stores/locale'
import { useUserStore } from '@/stores/user'
import {
  createAgent,
  disableAgent,
  enableAgent,
  fetchAgents,
  partnerStatusMap,
  updateAgent,
  type Agent,
  type AgentPayload
} from '@/api/partners'
import { ensureWalletAccount, fetchPlayCharges, fetchWalletAccounts, type PlayCharge, type WalletAccount } from '@/api/wallet'
import { isValidEmail, isValidPhone } from '@/utils/validators'

const loading = ref(false)
const locale = useLocaleStore()
const user = useUserStore()
const saving = ref(false)
const drawerVisible = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const records = ref<Agent[]>([])
const current = ref<Agent>()
const walletAccount = ref<WalletAccount>()
const walletCharges = ref<PlayCharge[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()
const walletLoading = ref(false)

const query = reactive({ keyword: '', status: '', page: 1, size: 10 })
const form = reactive<AgentPayload>({
  agentName: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  remark: ''
})

const stats = computed(() => [
  { label: locale.t('page.partners.totalAgents'), value: total.value },
  { label: getStatusLabel('partner', 'active', '启用'), value: records.value.filter((item) => item.status === 'active').length },
  { label: getStatusLabel('partner', 'disabled', '停用'), value: records.value.filter((item) => item.status === 'disabled').length },
  { label: locale.t('page.partners.currentPage'), value: records.value.length }
])

const rules = computed<FormRules>(() => ({
  agentName: [{ required: true, message: locale.t('page.partners.agentNameRequired'), trigger: 'blur' }],
  contactPhone: [{ validator: validatePhone, trigger: 'blur' }],
  contactEmail: [{ validator: validateEmail, trigger: 'blur' }]
}))

function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function validatePhone(_rule: unknown, value: string, callback: (error?: Error) => void) {
  callback(isValidPhone(value) ? undefined : new Error(locale.t('common.invalidPhone')))
}

function validateEmail(_rule: unknown, value: string, callback: (error?: Error) => void) {
  callback(isValidEmail(value) ? undefined : new Error(locale.t('common.invalidEmail')))
}

function formatMoney(value?: number) {
  return Number(value || 0).toFixed(2)
}

async function loadAgents() {
  loading.value = true
  try {
    const result = await fetchAgents(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { keyword: '', status: '', page: 1 })
  loadAgents()
}

function resetForm() {
  editingId.value = undefined
  Object.assign(form, { agentName: '', contactName: '', contactPhone: '', contactEmail: '', remark: '' })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Agent) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

function openDetail(row: Agent) {
  current.value = row
  if (user.hasPermission('wallet:view')) {
    loadWallet(row.id)
  }
  drawerVisible.value = true
}

async function loadWallet(ownerId: number) {
  walletLoading.value = true
  walletAccount.value = undefined
  walletCharges.value = []
  try {
    const [accounts, charges] = await Promise.all([
      fetchWalletAccounts({ ownerType: 'agent', ownerId, page: 1, size: 1 }),
      user.hasPermission('wallet:charge:view')
        ? fetchPlayCharges({ ownerType: 'agent', ownerId, page: 1, size: 5 })
        : Promise.resolve({ data: { records: [] } })
    ])
    walletAccount.value = accounts.data.records[0]
    walletCharges.value = charges.data.records
  } finally {
    walletLoading.value = false
  }
}

async function handleEnsureWallet() {
  if (!current.value) {
    return
  }
  walletLoading.value = true
  try {
    walletAccount.value = (await ensureWalletAccount('agent', current.value.id)).data
    ElMessage.success(locale.t('page.wallet.accountEnsured'))
  } finally {
    walletLoading.value = false
  }
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    editingId.value ? await updateAgent(editingId.value, form) : await createAgent(form)
    ElMessage.success(locale.t('page.partners.agentSaved'))
    dialogVisible.value = false
    loadAgents()
  } finally {
    saving.value = false
  }
}

async function handleDisable(id: number) {
  await ElMessageBox.confirm(locale.t('page.partners.disableAgentConfirm'), locale.t('page.partners.disableAgentTitle'), { type: 'warning' })
  await disableAgent(id)
  ElMessage.success(locale.t('page.partners.agentDisabled'))
  loadAgents()
}

async function handleEnable(id: number) {
  await enableAgent(id)
  ElMessage.success(locale.t('page.partners.agentEnabled'))
  loadAgents()
}

onMounted(loadAgents)
</script>

<style scoped>
.wallet-panel {
  margin-top: 18px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-head h3 {
  margin: 0;
  color: #f4fbff;
  font-size: 16px;
}

.compact-table {
  margin-top: 12px;
}
</style>
