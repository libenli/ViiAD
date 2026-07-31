<template>
  <AppPage eyebrow="权限管理" title="权限管理" :stats="stats">
    <div class="role-auth-layout">
      <section class="role-list panel-card">
        <div class="panel-title">
          <div>
            <p>角色列表</p>
            <strong>选择一个角色进行授权</strong>
          </div>
          <el-button :icon="Refresh" circle @click="loadAll" />
        </div>

        <el-skeleton v-if="loading" :rows="5" animated />
        <el-empty v-else-if="!roles.length" description="暂无角色，请先执行 RBAC 初始化脚本" />
        <button
          v-for="role in roles"
          v-else
          :key="role.id"
          class="role-card"
          :class="{ active: selectedRoleId === role.id }"
          type="button"
          @click="selectRole(role.id)"
        >
          <span>{{ role.roleName }}</span>
          <em>{{ role.roleCode }}</em>
          <small>{{ role.remark || '暂无说明' }}</small>
        </button>
      </section>

      <section class="permission-panel panel-card">
        <div class="panel-title permission-title">
          <div>
            <p>权限配置</p>
            <strong>{{ selectedRole?.roleName || '未选择角色' }}</strong>
          </div>
          <div class="toolbar">
            <el-button :disabled="!selectedRoleId" @click="checkMenuOnly">只选菜单</el-button>
            <el-button :disabled="!selectedRoleId" @click="checkAll">全选</el-button>
            <el-button :disabled="!selectedRoleId" @click="clearChecked">清空</el-button>
            <el-button
              type="primary"
              :loading="saving"
              :disabled="!selectedRoleId || !user.hasPermission('system:role:grant')"
              @click="handleSave"
            >
              保存授权
            </el-button>
          </div>
        </div>

        <el-alert
          v-if="!user.hasPermission('system:role:grant')"
          title="当前账号没有角色授权权限，只能查看权限配置。"
          type="warning"
          show-icon
          :closable="false"
        />

        <el-empty v-if="!selectedRoleId" description="请先从左侧选择角色" />
        <template v-else>
          <div class="permission-section">
            <h3>菜单权限</h3>
            <el-checkbox-group v-model="checkedMenuIds" :disabled="!user.hasPermission('system:role:grant')">
              <div class="permission-grid">
                <el-checkbox v-for="item in menuPermissions" :key="item.id" :label="item.id" border>
                  <span>{{ getPermissionLabel(item) }}</span>
                  <small>{{ item.permissionCode }}</small>
                </el-checkbox>
              </div>
            </el-checkbox-group>
          </div>

          <div class="permission-section">
            <h3>按钮 / 操作权限</h3>
            <el-checkbox-group v-model="checkedMenuIds" :disabled="!user.hasPermission('system:role:grant')">
              <div class="permission-grid action-grid">
                <el-checkbox v-for="item in actionPermissions" :key="item.id" :label="item.id" border>
                  <span>{{ getPermissionLabel(item) }}</span>
                  <small>{{ item.permissionCode }}</small>
                </el-checkbox>
              </div>
            </el-checkbox-group>
          </div>
        </template>
      </section>
    </div>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import {
  fetchRoleMenuIds,
  fetchSystemMenus,
  fetchSystemRoles,
  saveRoleMenus,
  type SysMenu,
  type SysRole
} from '@/api/system'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const loading = ref(false)
const saving = ref(false)
const roles = ref<SysRole[]>([])
const menus = ref<SysMenu[]>([])
const selectedRoleId = ref<number>()
const checkedMenuIds = ref<number[]>([])

const selectedRole = computed(() => roles.value.find((item) => item.id === selectedRoleId.value))
const menuPermissions = computed(() => menus.value.filter((item) => item.menuPath?.startsWith('/')))
const actionPermissions = computed(() => menus.value.filter((item) => item.menuPath?.startsWith('#')))
const stats = computed(() => [
  { label: '角色数量', value: roles.value.length },
  { label: '菜单权限', value: menuPermissions.value.length },
  { label: '操作权限', value: actionPermissions.value.length },
  { label: '已勾选', value: checkedMenuIds.value.length }
])

const labelMap: Record<string, string> = {
  'dashboard:view': '工作台',
  'ad:view': '广告列表',
  'material:view': '素材管理',
  'plan:view': '投放计划',
  'device:view': '设备管理',
  'delivery:view': '下发记录',
  'report:view': '数据报表',
  'bill:view': '账单结算',
  'workOrder:view': '工单反馈',
  'advertiser:view': '广告主',
  'agent:view': '代理商',
  'system:user:view': '系统用户',
  'system:role:view': '权限管理',
  'system:log:view': '操作日志',
  'ad:edit': '广告新建/编辑',
  'ad:submit': '广告提交审核',
  'ad:audit': '广告审核',
  'material:edit': '素材上传/编辑',
  'material:submit': '素材提交审核',
  'material:audit': '素材审核',
  'plan:edit': '计划新建/编辑',
  'plan:schedule': '计划排期',
  'plan:delivery': '计划投放控制',
  'device:manage': '设备维护',
  'delivery:operate': '下发操作',
  'bill:confirm': '账单生成/确认',
  'bill:pay': '账单支付',
  'workOrder:operate': '工单处理',
  'partner:manage': '广告主/代理商维护',
  'system:role:grant': '角色授权'
}

function getPermissionLabel(item: SysMenu) {
  return labelMap[item.permissionCode || ''] || item.menuName || item.menuPath
}

async function loadAll() {
  loading.value = true
  try {
    const [roleResult, menuResult] = await Promise.all([fetchSystemRoles(), fetchSystemMenus()])
    roles.value = roleResult.data
    menus.value = menuResult.data
    if (!selectedRoleId.value && roles.value.length) {
      await selectRole(roles.value[0].id)
    }
  } finally {
    loading.value = false
  }
}

async function selectRole(roleId: number) {
  selectedRoleId.value = roleId
  const result = await fetchRoleMenuIds(roleId)
  checkedMenuIds.value = result.data
}

function checkMenuOnly() {
  checkedMenuIds.value = menuPermissions.value.map((item) => item.id)
}

function checkAll() {
  checkedMenuIds.value = menus.value.map((item) => item.id)
}

function clearChecked() {
  checkedMenuIds.value = []
}

async function handleSave() {
  if (!selectedRoleId.value) {
    return
  }
  saving.value = true
  try {
    await saveRoleMenus(selectedRoleId.value, checkedMenuIds.value)
    ElMessage.success('权限配置已保存')
    if (user.userInfo?.roles.some((role) => role === selectedRole.value?.roleCode)) {
      await user.fetchUserInfo()
    }
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.role-auth-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
}

.panel-card {
  border: 1px solid rgba(91, 214, 255, 0.18);
  border-radius: 18px;
  background: rgba(7, 22, 31, 0.78);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.24);
  padding: 18px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-title p {
  margin: 0 0 6px;
  color: #7db7d7;
  font-size: 13px;
}

.panel-title strong {
  color: #f6fbff;
  font-size: 20px;
}

.role-card {
  display: block;
  width: 100%;
  margin-bottom: 12px;
  padding: 15px;
  border: 1px solid rgba(119, 225, 255, 0.16);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.035);
  color: #dbeafe;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.role-card:hover,
.role-card.active {
  border-color: rgba(83, 229, 255, 0.72);
  background: linear-gradient(135deg, rgba(42, 192, 255, 0.18), rgba(59, 130, 246, 0.08));
  transform: translateY(-1px);
}

.role-card span,
.role-card em,
.role-card small {
  display: block;
}

.role-card span {
  font-weight: 700;
}

.role-card em {
  margin: 6px 0;
  color: #7dd3fc;
  font-style: normal;
}

.role-card small {
  color: #94a3b8;
  line-height: 1.5;
}

.permission-title {
  align-items: flex-start;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.permission-section {
  margin-top: 18px;
}

.permission-section h3 {
  margin: 0 0 12px;
  color: #e2f5ff;
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.permission-grid :deep(.el-checkbox) {
  height: auto;
  min-height: 64px;
  margin: 0;
  padding: 12px;
  align-items: flex-start;
}

.permission-grid :deep(.el-checkbox__label) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #e5f7ff;
}

.permission-grid small {
  color: #7ca8bd;
}

.action-grid :deep(.el-checkbox) {
  border-color: rgba(81, 207, 102, 0.24);
}

@media (max-width: 1080px) {
  .role-auth-layout {
    grid-template-columns: 1fr;
  }

  .permission-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .permission-grid {
    grid-template-columns: 1fr;
  }
}
</style>
