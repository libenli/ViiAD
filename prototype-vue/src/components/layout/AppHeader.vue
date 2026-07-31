<template>
  <header class="header">
    <div>
      <strong>百万大屏广告系统</strong>
      <div class="muted">演示版 / 本地运行</div>
    </div>
    <div class="header-actions">
      <div class="muted">{{ roleLabel }} / {{ authStore.currentUser?.name }}</div>
      <button class="btn secondary" @click="resetDemo">恢复演示数据</button>
      <RouterLink class="btn secondary" to="/demo">演示入口</RouterLink>
      <RouterLink class="btn secondary" to="/switch-role">切换角色</RouterLink>
      <button class="btn secondary" @click="logout">退出</button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuditStore } from '@/stores/audit'
import { roleLabels } from '@/config/roles'

const authStore = useAuthStore()
const workflowStore = useWorkflowStore()
const auditStore = useAuditStore()
const router = useRouter()

const roleLabel = computed(() =>
  authStore.role ? roleLabels[authStore.role] : '未登录'
)

function logout() {
  authStore.logout()
  router.push('/demo')
}

function resetDemo() {
  workflowStore.resetDemoData()
  auditStore.resetDemoData()
  window.alert('演示数据已恢复到初始状态。')
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(120, 176, 255, 0.14);
  background:
    linear-gradient(180deg, rgba(7, 17, 31, 0.94), rgba(8, 20, 35, 0.82));
  backdrop-filter: blur(18px);
  box-shadow: 0 12px 28px rgba(2, 8, 20, 0.28);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.header strong {
  font-size: 18px;
  letter-spacing: 0.08em;
  color: #ecf7ff;
  text-shadow: 0 0 16px rgba(43, 200, 255, 0.18);
}
</style>
