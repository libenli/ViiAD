<template>
  <AppPage :eyebrow="locale.t('page.users.title')" :title="locale.t('page.users.title')" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('system:user:manage')" type="primary" :icon="Plus" @click="openCreate">
        {{ locale.t('page.users.create') }}
      </el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.users.keyword')">
        <el-input v-model="query.keyword" clearable :placeholder="locale.t('page.users.keywordPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.users.userType')">
        <el-select v-model="query.userType" clearable :placeholder="locale.t('page.users.allTypes')" style="width: 140px">
          <el-option :label="getUserTypeLabel('platform')" value="platform" />
          <el-option :label="getUserTypeLabel('advertiser')" value="advertiser" />
          <el-option :label="getUserTypeLabel('agent')" value="agent" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.users.status')">
        <el-select v-model="query.status" clearable :placeholder="locale.t('page.users.allStatus')" style="width: 140px">
          <el-option :label="getStatusLabel('partner', 'active', '启用')" value="active" />
          <el-option :label="getStatusLabel('partner', 'disabled', '停用')" value="disabled" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadUsers">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="users" class="data-table" row-key="id">
      <el-table-column prop="username" :label="locale.t('page.users.username')" min-width="140" />
      <el-table-column prop="realName" :label="locale.t('page.users.realName')" min-width="130" />
      <el-table-column :label="locale.t('page.users.type')" width="120">
        <template #default="{ row }">{{ getUserTypeLabel(row.userType) }}</template>
      </el-table-column>
      <el-table-column prop="phone" :label="locale.t('page.users.phone')" min-width="130" />
      <el-table-column prop="email" :label="locale.t('page.users.email')" min-width="170" />
      <el-table-column prop="advertiserId" :label="locale.t('page.users.advertiserId')" width="110" />
      <el-table-column prop="agentId" :label="locale.t('page.users.agentId')" width="110" />
      <el-table-column :label="locale.t('page.users.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'warning'" effect="dark">
            {{ getStatusLabel('partner', row.status, row.status === 'active' ? '启用' : '停用') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="locale.t('page.users.createTime')" min-width="170" />
      <el-table-column :label="locale.t('common.operation')" width="200" fixed="right" class-name="operation-column">
        <template #default="{ row }">
          <el-button v-if="user.hasPermission('system:user:manage')" link type="primary" @click="openEdit(row)">
            {{ locale.t('common.edit') }}
          </el-button>
          <el-button
            v-if="user.hasPermission('system:user:manage') && row.status === 'active'"
            link
            type="warning"
            @click="handleDisable(row.id)"
          >
            {{ locale.t('page.users.disable') }}
          </el-button>
          <el-button
            v-else-if="user.hasPermission('system:user:manage')"
            link
            type="success"
            @click="handleEnable(row.id)"
          >
            {{ locale.t('page.users.enable') }}
          </el-button>
          <el-button v-if="user.hasPermission('system:user:manage')" link type="danger" @click="handleResetPassword(row)">
            {{ locale.t('page.users.resetPassword') }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="locale.t('page.users.empty')" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadUsers"
        @size-change="loadUsers"
      />
    </div>

    <section v-if="user.hasPermission('system:user:manage')" class="login-log-panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">{{ locale.t('page.users.accountSecurity') }}</p>
          <h3>{{ locale.t('page.users.loginLogs') }}</h3>
        </div>
        <el-form class="filter-form compact" :model="logQuery" inline>
          <el-form-item :label="locale.t('page.users.username')">
            <el-input v-model="logQuery.username" clearable :placeholder="locale.t('page.users.loginAccount')" style="width: 150px" />
          </el-form-item>
          <el-form-item :label="locale.t('page.users.result')">
            <el-select v-model="logQuery.loginStatus" clearable :placeholder="locale.t('page.users.allResults')" style="width: 120px">
              <el-option :label="locale.t('status.loginResult.success')" value="success" />
              <el-option :label="locale.t('status.loginResult.fail')" value="fail" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadLoginLogs">{{ locale.t('common.search') }}</el-button>
            <el-button :icon="Refresh" @click="resetLogQuery">{{ locale.t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="logLoading" :data="loginLogs" class="data-table log-table" row-key="id">
        <el-table-column prop="username" :label="locale.t('page.users.username')" min-width="130" />
        <el-table-column :label="locale.t('page.users.result')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.loginStatus === 'success' ? 'success' : 'danger'" effect="dark">
              {{ row.loginStatus === 'success' ? locale.t('status.loginResult.success') : locale.t('status.loginResult.fail') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" :label="locale.t('page.users.ipAddress')" min-width="140" />
        <el-table-column prop="failReason" :label="locale.t('page.users.failReason')" min-width="180">
          <template #default="{ row }">{{ row.failReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="loginTime" :label="locale.t('page.users.loginTime')" min-width="170" />
        <el-table-column prop="userAgent" :label="locale.t('page.users.userAgent')" min-width="260" show-overflow-tooltip />
        <template #empty>
          <el-empty :description="locale.t('page.users.loginLogEmpty')" />
        </template>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="logQuery.page"
          v-model:page-size="logQuery.size"
          background
          layout="total, prev, pager, next"
          :total="logTotal"
          @current-change="loadLoginLogs"
          @size-change="loadLoginLogs"
        />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" :title="editingId ? locale.t('page.users.edit') : locale.t('page.users.create')" size="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="locale.t('page.users.loginAccount')" prop="username">
          <el-input v-model="form.username" :placeholder="locale.t('page.users.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item v-if="!editingId" :label="locale.t('page.users.initialPassword')">
          <el-input v-model="form.password" :placeholder="locale.t('page.users.passwordDefault')" show-password />
        </el-form-item>
        <el-form-item :label="locale.t('page.users.realName')" prop="realName">
          <el-input v-model="form.realName" :placeholder="locale.t('page.users.realNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.users.userType')">
          <el-select v-model="form.userType" style="width: 100%">
            <el-option :label="getUserTypeLabel('platform')" value="platform" />
            <el-option :label="getUserTypeLabel('advertiser')" value="advertiser" />
            <el-option :label="getUserTypeLabel('agent')" value="agent" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.userType === 'advertiser'" :label="locale.t('page.users.advertiserId')" prop="advertiserId">
          <el-select v-model="form.advertiserId" filterable :loading="advertiserLoading" :placeholder="locale.t('page.users.advertiserPlaceholder')" style="width: 100%">
            <el-option v-for="item in advertiserOptions" :key="item.id" :label="getAdvertiserLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.userType === 'agent'" :label="locale.t('page.users.agentId')" prop="agentId">
          <el-select v-model="form.agentId" filterable :loading="agentLoading" :placeholder="locale.t('page.users.agentPlaceholder')" style="width: 100%">
            <el-option v-for="item in agentOptions" :key="item.id" :label="getAgentLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="locale.t('page.users.phone')" prop="phone">
          <el-input v-model="form.phone" :placeholder="locale.t('page.users.phonePlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.users.email')" prop="email">
          <el-input v-model="form.email" :placeholder="locale.t('page.users.emailPlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('page.users.status')">
          <el-radio-group v-model="form.status">
            <el-radio-button label="active">{{ getStatusLabel('partner', 'active', '启用') }}</el-radio-button>
            <el-radio-button label="disabled">{{ getStatusLabel('partner', 'disabled', '停用') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="locale.t('page.users.role')">
          <el-select v-model="form.roleIds" multiple clearable :placeholder="locale.t('page.users.selectRole')" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="getRoleName(role)" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ locale.t('common.save') }}</el-button>
      </template>
    </el-drawer>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import {
  createSystemUser,
  disableSystemUser,
  enableSystemUser,
  fetchLoginLogs,
  fetchSystemRoles,
  fetchSystemUsers,
  fetchUserRoleIds,
  resetSystemUserPassword,
  updateSystemUser,
  type SysLoginLog,
  type SysRole,
  type SysUser,
  type SysUserPayload
} from '@/api/system'
import { useLocaleStore } from '@/stores/locale'
import { useUserStore } from '@/stores/user'
import { isValidEmail, isValidPhone } from '@/utils/validators'
import { fetchAdvertisers, fetchAgents, type Advertiser, type Agent } from '@/api/partners'

const locale = useLocaleStore()
const user = useUserStore()
const loading = ref(false)
const saving = ref(false)
const drawerVisible = ref(false)
const editingId = ref<number>()
const users = ref<SysUser[]>([])
const roles = ref<SysRole[]>([])
const loginLogs = ref<SysLoginLog[]>([])
const advertiserOptions = ref<Advertiser[]>([])
const agentOptions = ref<Agent[]>([])
const total = ref(0)
const logTotal = ref(0)
const formRef = ref<FormInstance>()
const advertiserLoading = ref(false)
const agentLoading = ref(false)
const query = reactive({ keyword: '', userType: '', status: '', page: 1, size: 10 })
const logQuery = reactive({ username: '', loginStatus: '', page: 1, size: 8 })
const logLoading = ref(false)

const form = reactive<SysUserPayload>({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  userType: 'platform',
  advertiserId: undefined,
  agentId: undefined,
  status: 'active',
  roleIds: []
})

const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: locale.t('page.users.usernameRequired'), trigger: 'blur' }],
  realName: [{ required: true, message: locale.t('page.users.realNameRequired'), trigger: 'blur' }],
  advertiserId: [{ required: form.userType === 'advertiser', message: locale.t('page.users.advertiserRequired'), trigger: 'change' }],
  agentId: [{ required: form.userType === 'agent', message: locale.t('page.users.agentRequired'), trigger: 'change' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }]
}))

const stats = computed(() => [
  { label: locale.t('page.users.total'), value: total.value },
  { label: getStatusLabel('partner', 'active', '启用'), value: users.value.filter((item) => item.status === 'active').length },
  { label: getStatusLabel('partner', 'disabled', '停用'), value: users.value.filter((item) => item.status === 'disabled').length },
  { label: locale.t('page.users.roleCount'), value: roles.value.length }
])

function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function getUserTypeLabel(value?: string) {
  return value ? locale.t(`status.userType.${value}`, value) : '-'
}

function getRoleName(role: SysRole) {
  return locale.t(`role.${role.roleCode}.name`, role.roleName)
}

function getAdvertiserLabel(item: Advertiser) {
  return `${item.advertiserName}（${item.advertiserCode}）`
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

function resetForm() {
  editingId.value = undefined
  Object.assign(form, {
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    userType: 'platform',
    advertiserId: undefined,
    agentId: undefined,
    status: 'active',
    roleIds: []
  })
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await fetchSystemUsers(query)
    users.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

async function loadLoginLogs() {
  if (!user.hasPermission('system:user:manage')) {
    return
  }
  logLoading.value = true
  try {
    const result = await fetchLoginLogs(logQuery)
    loginLogs.value = result.data.records
    logTotal.value = result.data.total
  } finally {
    logLoading.value = false
  }
}

async function loadRoles() {
  const result = await fetchSystemRoles()
  roles.value = result.data
}

async function loadPartners() {
  advertiserLoading.value = true
  agentLoading.value = true
  try {
    const [advertiserResult, agentResult] = await Promise.all([
      fetchAdvertisers({ status: 'active', page: 1, size: 200 }),
      fetchAgents({ status: 'active', page: 1, size: 200 })
    ])
    advertiserOptions.value = advertiserResult.data.records
    agentOptions.value = agentResult.data.records
  } finally {
    advertiserLoading.value = false
    agentLoading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { keyword: '', userType: '', status: '', page: 1 })
  loadUsers()
}

function resetLogQuery() {
  Object.assign(logQuery, { username: '', loginStatus: '', page: 1 })
  loadLoginLogs()
}

function openCreate() {
  resetForm()
  drawerVisible.value = true
}

async function openEdit(row: SysUser) {
  resetForm()
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    realName: row.realName,
    phone: row.phone || '',
    email: row.email || '',
    userType: row.userType || 'platform',
    advertiserId: row.advertiserId,
    agentId: row.agentId,
    status: row.status || 'active',
    roleIds: []
  })
  const roleResult = await fetchUserRoleIds(row.id)
  form.roleIds = roleResult.data
  drawerVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (payload.userType !== 'advertiser') {
      payload.advertiserId = undefined
    }
    if (payload.userType !== 'agent') {
      payload.agentId = undefined
    }
    if (editingId.value) {
      delete payload.password
      await updateSystemUser(editingId.value, payload)
      ElMessage.success(locale.t('page.users.updated'))
    } else {
      await createSystemUser(payload)
      ElMessage.success(locale.t('page.users.created'))
    }
    drawerVisible.value = false
    loadUsers()
  } finally {
    saving.value = false
  }
}

async function handleDisable(id: number) {
  await ElMessageBox.confirm(locale.t('page.users.disableConfirm'), locale.t('page.users.disableTitle'), { type: 'warning' })
  await disableSystemUser(id)
  ElMessage.success(locale.t('page.users.disabled'))
  loadUsers()
}

async function handleEnable(id: number) {
  await enableSystemUser(id)
  ElMessage.success(locale.t('page.users.enabled'))
  loadUsers()
}

async function handleResetPassword(row: SysUser) {
  const result = await ElMessageBox.prompt(locale.t('page.users.resetPrompt').replace('{username}', row.username), locale.t('page.users.resetPassword'), {
    confirmButtonText: locale.t('page.users.resetConfirm'),
    cancelButtonText: locale.t('common.cancel'),
    inputValue: 'admin123',
    inputPattern: /\S{6,}/,
    inputErrorMessage: locale.t('page.users.passwordMin')
  })
  await resetSystemUserPassword(row.id, result.value)
  ElMessage.success(locale.t('page.users.passwordReset'))
}

onMounted(() => {
  loadUsers()
  loadRoles()
  loadLoginLogs()
  loadPartners()
})

watch(
  () => form.userType,
  (value) => {
    if (value !== 'advertiser') {
      form.advertiserId = undefined
    }
    if (value !== 'agent') {
      form.agentId = undefined
    }
  }
)
</script>

<style scoped>
.login-log-panel {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid rgba(83, 229, 255, 0.14);
  border-radius: 14px;
  background: rgba(5, 18, 26, 0.68);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.panel-head h3 {
  margin: 4px 0 0;
  color: #f4fbff;
  font-size: 18px;
}

.compact {
  margin-bottom: 0;
}

.log-table {
  margin-top: 6px;
}
</style>
