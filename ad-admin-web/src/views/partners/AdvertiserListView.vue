<template>
  <AppPage :eyebrow="locale.t('page.partners.subject')" :title="locale.t('page.partners.advertiserTitle')" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('partner:manage')" type="primary" :icon="Plus" @click="openCreate">{{ locale.t('page.partners.createAdvertiser') }}</el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.partners.keyword')">
        <el-input v-model="query.keyword" clearable :placeholder="locale.t('page.partners.advertiserKeywordPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.partners.source')">
        <el-select v-model="query.sourceType" clearable :placeholder="locale.t('page.partners.allSources')" style="width: 140px">
          <el-option v-for="(label, key) in advertiserSourceMap" :key="key" :label="getStringLabel('advertiserSource', String(key), label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.partners.status')">
        <el-select v-model="query.status" clearable :placeholder="locale.t('page.partners.allStatus')" style="width: 140px">
          <el-option v-for="(item, key) in partnerStatusMap" :key="key" :label="getStatusLabel('partner', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadAdvertisers">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="advertiserCode" :label="locale.t('page.partners.advertiserCode')" min-width="170" />
      <el-table-column prop="advertiserName" :label="locale.t('page.partners.advertiserName')" min-width="180" />
      <el-table-column prop="companyName" :label="locale.t('page.partners.companyName')" min-width="180" />
      <el-table-column prop="contactName" :label="locale.t('page.partners.contactName')" width="110" />
      <el-table-column prop="contactPhone" :label="locale.t('page.partners.contactPhone')" min-width="140" />
      <el-table-column :label="locale.t('page.partners.source')" width="120">
        <template #default="{ row }">{{ getStringLabel('advertiserSource', row.sourceType || '', advertiserSourceMap[row.sourceType] || row.sourceType || '-') }}</template>
      </el-table-column>
      <el-table-column prop="agentId" :label="locale.t('page.partners.agentId')" width="110">
        <template #default="{ row }">{{ row.agentId || '-' }}</template>
      </el-table-column>
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
        <el-empty :description="locale.t('page.partners.advertiserEmpty')" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadAdvertisers"
        @size-change="loadAdvertisers"
      />
    </div>

    <el-drawer v-model="drawerVisible" :title="locale.t('page.partners.advertiserDrawer')" size="460px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item :label="locale.t('page.partners.code')">{{ current.advertiserCode }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.name')">{{ current.advertiserName }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.company')">{{ current.companyName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.contactName')">{{ current.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.contactPhone')">{{ current.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.email')">{{ current.contactEmail || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.source')">{{ getStringLabel('advertiserSource', current.sourceType || '', advertiserSourceMap[current.sourceType || ''] || current.sourceType || '-') }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.agentId')">{{ current.agentId || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.partners.remark')">{{ current.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="editingId ? locale.t('page.partners.editAdvertiser') : locale.t('page.partners.createAdvertiser')" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="locale.t('page.partners.advertiserName')" prop="advertiserName">
          <el-input v-model="form.advertiserName" :placeholder="locale.t('page.partners.advertiserNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.partners.companyName')">
          <el-input v-model="form.companyName" :placeholder="locale.t('page.partners.companyPlaceholder')" />
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
        <el-form-item :label="locale.t('page.partners.source')">
          <el-select v-model="form.sourceType" class="wide-control">
            <el-option v-for="(label, key) in advertiserSourceMap" :key="key" :label="getStringLabel('advertiserSource', String(key), label)" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.sourceType === 'agent'" :label="locale.t('page.partners.agentId')" prop="agentId">
          <el-select v-model="form.agentId" class="wide-control" filterable :loading="agentLoading" :placeholder="locale.t('page.partners.agentPlaceholder')">
            <el-option v-for="item in agentOptions" :key="item.id" :label="getAgentLabel(item)" :value="item.id" />
          </el-select>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { useLocaleStore } from '@/stores/locale'
import { useUserStore } from '@/stores/user'
import {
  advertiserSourceMap,
  createAdvertiser,
  disableAdvertiser,
  enableAdvertiser,
  fetchAgents,
  fetchAdvertisers,
  partnerStatusMap,
  updateAdvertiser,
  type Agent,
  type Advertiser,
  type AdvertiserPayload
} from '@/api/partners'
import { isValidEmail, isValidPhone } from '@/utils/validators'

const loading = ref(false)
const locale = useLocaleStore()
const user = useUserStore()
const saving = ref(false)
const drawerVisible = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const records = ref<Advertiser[]>([])
const current = ref<Advertiser>()
const agentOptions = ref<Agent[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()
const agentLoading = ref(false)

const query = reactive({ keyword: '', sourceType: '', status: '', page: 1, size: 10 })
const form = reactive<AdvertiserPayload>({
  advertiserName: '',
  companyName: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  sourceType: 'platform',
  agentId: undefined,
  remark: ''
})

const stats = computed(() => [
  { label: locale.t('page.partners.totalAdvertisers'), value: total.value },
  { label: getStatusLabel('partner', 'active', '启用'), value: records.value.filter((item) => item.status === 'active').length },
  { label: getStatusLabel('partner', 'disabled', '停用'), value: records.value.filter((item) => item.status === 'disabled').length },
  { label: getStringLabel('advertiserSource', 'platform', '平台招商'), value: records.value.filter((item) => item.sourceType === 'platform').length }
])

const rules = computed<FormRules>(() => ({
  advertiserName: [{ required: true, message: locale.t('page.partners.advertiserNameRequired'), trigger: 'blur' }],
  agentId: [{ required: form.sourceType === 'agent', message: locale.t('page.partners.agentRequired'), trigger: 'change' }],
  contactPhone: [{ validator: validatePhone, trigger: 'blur' }],
  contactEmail: [{ validator: validateEmail, trigger: 'blur' }]
}))

function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function getStringLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function getAgentLabel(item: Agent) {
  return `${item.agentName}（${item.agentCode}）`
}

function validatePhone(_rule: unknown, value: string, callback: (error?: Error) => void) {
  callback(isValidPhone(value) ? undefined : new Error(locale.t('common.invalidPhone')))
}

function validateEmail(_rule: unknown, value: string, callback: (error?: Error) => void) {
  callback(isValidEmail(value) ? undefined : new Error(locale.t('common.invalidEmail')))
}

async function loadAdvertisers() {
  loading.value = true
  try {
    const result = await fetchAdvertisers(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

async function loadAgents() {
  agentLoading.value = true
  try {
    const result = await fetchAgents({ status: 'active', page: 1, size: 200 })
    agentOptions.value = result.data.records
  } finally {
    agentLoading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { keyword: '', sourceType: '', status: '', page: 1 })
  loadAdvertisers()
}

function resetForm() {
  editingId.value = undefined
  Object.assign(form, { advertiserName: '', companyName: '', contactName: '', contactPhone: '', contactEmail: '', sourceType: 'platform', agentId: undefined, remark: '' })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Advertiser) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

function openDetail(row: Advertiser) {
  current.value = row
  drawerVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (payload.sourceType !== 'agent') {
      payload.agentId = undefined
    }
    editingId.value ? await updateAdvertiser(editingId.value, payload) : await createAdvertiser(payload)
    ElMessage.success(locale.t('page.partners.advertiserSaved'))
    dialogVisible.value = false
    loadAdvertisers()
  } finally {
    saving.value = false
  }
}

async function handleDisable(id: number) {
  await ElMessageBox.confirm(locale.t('page.partners.disableAdvertiserConfirm'), locale.t('page.partners.disableAdvertiserTitle'), { type: 'warning' })
  await disableAdvertiser(id)
  ElMessage.success(locale.t('page.partners.advertiserDisabled'))
  loadAdvertisers()
}

async function handleEnable(id: number) {
  await enableAdvertiser(id)
  ElMessage.success(locale.t('page.partners.advertiserEnabled'))
  loadAdvertisers()
}

onMounted(() => {
  loadAdvertisers()
  loadAgents()
})

watch(
  () => form.sourceType,
  (value) => {
    if (value !== 'agent') {
      form.agentId = undefined
    }
  }
)
</script>

<style scoped>
.wide-control {
  width: 100%;
}
</style>
