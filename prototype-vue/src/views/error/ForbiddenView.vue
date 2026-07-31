<template>
  <div class="login-page">
    <div class="login-card">
      <h1>403</h1>
      <p class="muted">当前角色没有访问这个页面的权限。</p>
      <RouterLink class="btn secondary" to="/">返回首页</RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
}

.login-card {
  padding: 32px;
  border-radius: 24px;
  background: #fff;
}
</style>
