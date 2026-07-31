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
            <span>{{ item.title }}</span>
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
          <el-dropdown>
            <span class="user-trigger">
              <el-avatar :size="28">管</el-avatar>
              {{ user.userInfo?.realName || '用户' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openPasswordDialog">修改密码</el-dropdown-item>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content-area">
        <RouterView />
      </main>
    </section>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px" destroy-on-close>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="handleChangePassword">确认修改</el-button>
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
import { appMenus } from '@/config/menus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const app = useAppStore()
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

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const visibleMenus = computed(() => {
  const paths = user.userInfo?.menus || []
  if (!paths.length || user.userInfo?.roles.includes('super_admin')) {
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

async function handleChangePassword() {
  await passwordFormRef.value?.validate()
  passwordSaving.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
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
</style>
