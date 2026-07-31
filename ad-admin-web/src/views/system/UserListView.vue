<template>
  <AppPage eyebrow="用户管理" title="用户管理" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('system:user:manage')" type="primary" :icon="Plus" @click="openCreate">
        新建用户
      </el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="账号 / 姓名" />
      </el-form-item>
      <el-form-item label="用户类型">
        <el-select v-model="query.userType" clearable placeholder="全部类型" style="width: 140px">
          <el-option label="平台用户" value="platform" />
          <el-option label="广告主用户" value="advertiser" />
          <el-option label="代理商用户" value="agent" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option label="启用" value="active" />
          <el-option label="停用" value="disabled" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadUsers">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="users" class="data-table" row-key="id">
      <el-table-column prop="username" label="账号" min-width="140" />
      <el-table-column prop="realName" label="姓名" min-width="130" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ userTypeMap[row.userType] || row.userType || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="email" label="邮箱" min-width="170" />
      <el-table-column prop="advertiserId" label="广告主ID" width="110" />
      <el-table-column prop="agentId" label="代理商ID" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'warning'" effect="dark">
            {{ row.status === 'active' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" min-width="260" class-name="operation-column">
        <template #default="{ row }">
          <el-button v-if="user.hasPermission('system:user:manage')" link type="primary" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button
            v-if="user.hasPermission('system:user:manage') && row.status === 'active'"
            link
            type="warning"
            @click="handleDisable(row.id)"
          >
            停用
          </el-button>
          <el-button
            v-else-if="user.hasPermission('system:user:manage')"
            link
            type="success"
            @click="handleEnable(row.id)"
          >
            启用
          </el-button>
          <el-button v-if="user.hasPermission('system:user:manage')" link type="danger" @click="handleResetPassword(row)">
            重置密码
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无用户，请先新建用户或执行 RBAC 初始化脚本" />
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
          <p class="eyebrow">账号安全</p>
          <h3>登录日志</h3>
        </div>
        <el-form class="filter-form compact" :model="logQuery" inline>
          <el-form-item label="账号">
            <el-input v-model="logQuery.username" clearable placeholder="登录账号" style="width: 150px" />
          </el-form-item>
          <el-form-item label="结果">
            <el-select v-model="logQuery.loginStatus" clearable placeholder="全部" style="width: 120px">
              <el-option label="成功" value="success" />
              <el-option label="失败" value="fail" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadLoginLogs">查询</el-button>
            <el-button :icon="Refresh" @click="resetLogQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="logLoading" :data="loginLogs" class="data-table log-table" row-key="id">
        <el-table-column prop="username" label="账号" min-width="130" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.loginStatus === 'success' ? 'success' : 'danger'" effect="dark">
              {{ row.loginStatus === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP 地址" min-width="140" />
        <el-table-column prop="failReason" label="失败原因" min-width="180">
          <template #default="{ row }">{{ row.failReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="loginTime" label="登录时间" min-width="170" />
        <el-table-column prop="userAgent" label="浏览器标识" min-width="260" show-overflow-tooltip />
        <template #empty>
          <el-empty description="暂无登录日志" />
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

    <el-drawer v-model="drawerVisible" :title="editingId ? '编辑用户' : '新建用户'" size="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="初始密码">
          <el-input v-model="form.password" placeholder="默认 admin123" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="form.userType" style="width: 100%">
            <el-option label="平台用户" value="platform" />
            <el-option label="广告主用户" value="advertiser" />
            <el-option label="代理商用户" value="agent" />
          </el-select>
        </el-form-item>
        <el-form-item label="广告主ID">
          <el-input-number v-model="form.advertiserId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="代理商ID">
          <el-input-number v-model="form.agentId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button label="active">启用</el-radio-button>
            <el-radio-button label="disabled">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple clearable placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-drawer>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const loading = ref(false)
const saving = ref(false)
const drawerVisible = ref(false)
const editingId = ref<number>()
const users = ref<SysUser[]>([])
const roles = ref<SysRole[]>([])
const loginLogs = ref<SysLoginLog[]>([])
const total = ref(0)
const logTotal = ref(0)
const formRef = ref<FormInstance>()
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

const rules: FormRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const userTypeMap: Record<string, string> = {
  platform: '平台用户',
  advertiser: '广告主用户',
  agent: '代理商用户'
}

const stats = computed(() => [
  { label: '全部用户', value: total.value },
  { label: '启用', value: users.value.filter((item) => item.status === 'active').length },
  { label: '停用', value: users.value.filter((item) => item.status === 'disabled').length },
  { label: '角色数量', value: roles.value.length }
])

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
    if (editingId.value) {
      delete payload.password
      await updateSystemUser(editingId.value, payload)
      ElMessage.success('用户已更新')
    } else {
      await createSystemUser(payload)
      ElMessage.success('用户已创建')
    }
    drawerVisible.value = false
    loadUsers()
  } finally {
    saving.value = false
  }
}

async function handleDisable(id: number) {
  await ElMessageBox.confirm('确认停用该账号吗？停用后将无法登录。', '停用账号', { type: 'warning' })
  await disableSystemUser(id)
  ElMessage.success('账号已停用')
  loadUsers()
}

async function handleEnable(id: number) {
  await enableSystemUser(id)
  ElMessage.success('账号已启用')
  loadUsers()
}

async function handleResetPassword(row: SysUser) {
  const result = await ElMessageBox.prompt(`请输入 ${row.username} 的新密码`, '重置密码', {
    confirmButtonText: '确认重置',
    cancelButtonText: '取消',
    inputValue: 'admin123',
    inputPattern: /\S{6,}/,
    inputErrorMessage: '密码至少 6 位'
  })
  await resetSystemUserPassword(row.id, result.value)
  ElMessage.success('密码已重置')
}

onMounted(() => {
  loadUsers()
  loadRoles()
  loadLoginLogs()
})
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
