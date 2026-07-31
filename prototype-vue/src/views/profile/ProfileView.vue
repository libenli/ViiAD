<template>
  <section>
    <div class="page-title">
      <div>
        <h1>个人中心</h1>
        <p class="page-subtitle">统一承载账号信息、密码修改和登录记录。</p>
      </div>
    </div>
    <div class="card" v-if="authStore.currentUser">
      <p><strong>{{ authStore.currentUser.name }}</strong></p>
      <p>账号：{{ authStore.currentUser.username }}</p>
      <p>角色：{{ roleLabels[authStore.currentUser.role] }}</p>
      <p>状态：{{ authStore.currentUser.status }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { roleLabels } from '@/config/roles'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>
