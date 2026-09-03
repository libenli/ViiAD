<template>
  <div class="admin-shell">
    <aside class="admin-sidebar" :class="{ collapsed: app.sidebarCollapsed }">
      <div class="brand">
        <div class="brand-mark">VA</div>
        <div v-if="!app.sidebarCollapsed" class="brand-text">
          <strong>ViiAD</strong>
        </div>
      </div>

      <el-scrollbar>
        <el-menu
          :default-active="route.path"
          router
          class="side-menu"
          background-color="transparent"
          text-color="#b9c7de"
          active-text-color="#ffffff"
        >
          <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ locale.t(item.titleKey, item.title) }}</span>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
    </aside>

    <section class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <el-button text :icon="app.sidebarCollapsed ? Expand : Fold" @click="app.toggleSidebar" />
        </div>
        <div class="header-actions">
          <LanguageSwitch class="header-language" compact />
          <el-dropdown>
            <span class="user-trigger">
              <el-avatar :size="28" class="role-avatar" :style="avatarStyle">{{ avatarText }}</el-avatar>
              {{ user.userInfo?.realName || locale.t('common.user') }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openPasswordDialog">{{ locale.t('common.changePassword') }}</el-dropdown-item>
                <el-dropdown-item @click="handleLogout">{{ locale.t('common.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content-area">
        <RouterView />
      </main>
    </section>

    <el-dialog v-model="passwordDialogVisible" :title="locale.t('passwordDialog.title')" width="420px" destroy-on-close>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item :label="locale.t('passwordDialog.oldPassword')" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" show-password :placeholder="locale.t('passwordDialog.oldPasswordPlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('passwordDialog.newPassword')" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" show-password :placeholder="locale.t('passwordDialog.newPasswordPlaceholder')" />
        </el-form-item>
        <el-form-item :label="locale.t('passwordDialog.confirmPassword')" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" show-password :placeholder="locale.t('passwordDialog.confirmPasswordPlaceholder')" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="passwordDialogVisible = false">{{ locale.t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="handleChangePassword">{{ locale.t('passwordDialog.confirmChange') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Expand, Fold } from '@element-plus/icons-vue'
import { changePassword } from '@/api/auth'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { appMenus } from '@/config/menus'
import { useAppStore } from '@/stores/app'
import { useLocaleStore } from '@/stores/locale'
import { useUserStore } from '@/stores/user'

const app = useAppStore()
const locale = useLocaleStore()
const user = useUserStore()
const route = useRoute()
const router = useRouter()
const passwordDialogVisible = ref(false)
const passwordSaving = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const avatarText = computed(() => {
  const roleCode = user.userInfo?.activeRoleCode || user.userInfo?.roles?.[0] || ''
  if (roleCode) {
    return roleCode.replace(/[^a-zA-Z]/g, '').slice(0, 2).toUpperCase() || 'VA'
  }
  const realName = user.userInfo?.realName || locale.t('common.user')
  return realName.slice(0, 1)
})

const avatarStyle = computed(() => {
  const roleCode = (user.userInfo?.activeRoleCode || user.userInfo?.roles?.[0] || '').toLowerCase()
  const userType = user.userInfo?.userType || 'platform'
  const palette = getAvatarPalette(roleCode, userType)
  return {
    background: `linear-gradient(135deg, ${palette[0]}, ${palette[1]})`,
    boxShadow: `0 0 18px ${palette[2]}`
  }
})

const passwordRules = computed<FormRules>(() => ({
  oldPassword: [{ required: true, message: locale.t('passwordDialog.oldPasswordRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: locale.t('passwordDialog.newPasswordRequired'), trigger: 'blur' },
    { min: 6, message: locale.t('passwordDialog.newPasswordMin'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: locale.t('passwordDialog.confirmPasswordRequired'), trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error(locale.t('passwordDialog.passwordMismatch')))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}))

const visibleMenus = computed(() => {
  const paths = user.userInfo?.menus || []
  const roles = user.userInfo?.roles || []
  const isSuperAdmin = roles.some((role) => ['super_admin', 'admin', 'ADM'].includes(role))
  if (!paths.length || isSuperAdmin) {
    return appMenus
  }
  return appMenus.filter((item) => paths.includes(item.path))
})

onMounted(() => {
  if (user.token && !user.userInfo) {
    user.fetchUserInfo()
  }
})

function handleLogout() {
  user.logout()
  router.replace('/login')
}

function openPasswordDialog() {
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
  passwordDialogVisible.value = true
}

function getAvatarPalette(roleCode: string, userType: string) {
  if (['super_admin', 'admin', 'adm'].includes(roleCode)) {
    return ['#41d9ff', '#2f7cff', 'rgba(65, 217, 255, 0.34)']
  }
  if (roleCode.includes('finance') || roleCode === 'fin') {
    return ['#f7c948', '#e08a1e', 'rgba(247, 201, 72, 0.32)']
  }
  if (roleCode.includes('agent') || roleCode === 'agt' || userType === 'agent') {
    return ['#70e000', '#2f9e44', 'rgba(112, 224, 0, 0.26)']
  }
  if (roleCode.includes('advertiser') || roleCode === 'adv' || userType === 'advertiser') {
    return ['#ff8a5b', '#ef476f', 'rgba(255, 138, 91, 0.28)']
  }
  if (roleCode.includes('auditor') || roleCode === 'adt') {
    return ['#b197fc', '#7048e8', 'rgba(177, 151, 252, 0.28)']
  }
  if (roleCode.includes('operator') || roleCode === 'opr') {
    return ['#4dabf7', '#15aabf', 'rgba(77, 171, 247, 0.28)']
  }
  return ['#7dd3fc', '#14b8a6', 'rgba(125, 211, 252, 0.24)']
}

async function handleChangePassword() {
  await passwordFormRef.value?.validate()
  passwordSaving.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success(locale.t('passwordDialog.changed'))
    passwordDialogVisible.value = false
    handleLogout()
  } finally {
    passwordSaving.value = false
  }
}
</script>

<style scoped>
.scope-tag {
  border-color: rgba(83, 229, 255, 0.32);
  background: rgba(34, 197, 94, 0.16);
  color: #d9fff0;
}

.role-avatar {
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.02em;
}
</style>
