<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <p class="eyebrow">ViiAD</p>
        <h1>大屏广告投放与运营平台</h1>
        <p>覆盖广告、素材、计划、设备、报表和结算的运营管理平台。</p>
      </div>

      <el-form class="login-form" :model="form" label-position="top" @submit.prevent @keydown.enter.prevent="handleLogin">
        <h2>系统登录</h2>
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="admin@mysher.com" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="admin123" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '登录' }}
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const loading = ref(false)

const form = reactive({
  username: 'admin@mysher.com',
  password: 'admin123'
})

async function handleLogin() {
  if (loading.value) {
    return
  }
  loading.value = true
  try {
    const result = await login(form)
    user.setToken(result.data.token)
    await user.fetchUserInfo()
    router.replace(String(route.query.redirect || '/dashboard'))
  } catch (error) {
    ElMessage.error('登录失败，请检查账号密码或确认已执行 RBAC 数据库补丁')
  } finally {
    loading.value = false
  }
}
</script>
