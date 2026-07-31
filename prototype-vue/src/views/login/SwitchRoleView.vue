<template>
  <div class="entry-page">
    <div class="switch-shell">
      <section class="shell-head">
        <div>
          <p class="eyebrow">ROLE HUB</p>
          <h1>角色快捷切换</h1>
          <p class="page-subtitle">
            用于演示中快速切换不同岗位视角。点击任意角色即可直接进入对应工作台。
          </p>
        </div>
        <RouterLink class="btn secondary" to="/demo">返回演示入口</RouterLink>
      </section>

      <section class="summary-grid">
        <div class="summary-card">
          <div>角色总数</div>
          <div class="metric-value">{{ users.length }}</div>
        </div>
        <div class="summary-card">
          <div>核心演示角色</div>
          <div class="metric-value">7</div>
        </div>
        <div class="summary-card">
          <div>推荐方式</div>
          <div class="metric-text">按岗位链路逐步切换</div>
        </div>
      </section>

      <section class="role-table-card">
        <table class="table">
          <thead>
            <tr>
              <th>账号</th>
              <th>姓名</th>
              <th>角色</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.username }}</td>
              <td>{{ user.name }}</td>
              <td>{{ roleLabels[user.role] }}</td>
              <td>
                <span class="status status-active">
                  {{ user.status === 'active' ? '正常' : '禁用' }}
                </span>
              </td>
              <td>
                <button class="btn secondary" @click="enter(user.username)">进入工作台</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { mockUsers } from '@/mock/users'
import { roleLabels } from '@/config/roles'

const router = useRouter()
const authStore = useAuthStore()
const users = mockUsers

function enter(username: string) {
  authStore.quickLogin(username)
  router.push('/')
}
</script>

<style scoped>
.entry-page {
  min-height: 100vh;
  padding: 36px;
}

.switch-shell {
  max-width: 1240px;
  margin: 0 auto;
}

.shell-head,
.summary-card,
.role-table-card {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(120, 176, 255, 0.16);
  background:
    linear-gradient(180deg, rgba(15, 30, 52, 0.9), rgba(8, 18, 34, 0.96));
  box-shadow:
    0 24px 60px rgba(2, 8, 20, 0.42),
    inset 0 1px 0 rgba(180, 224, 255, 0.05);
}

.shell-head::before,
.summary-card::before,
.role-table-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top right, rgba(43, 200, 255, 0.15), transparent 28%),
    linear-gradient(135deg, rgba(93, 124, 255, 0.07), transparent 44%);
}

.shell-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 32px;
  margin-bottom: 20px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #7ce9ff;
  font-weight: 700;
  letter-spacing: 0.18em;
  font-size: 12px;
}

.shell-head h1 {
  margin: 0;
  color: #f4fbff;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card {
  padding: 20px;
  animation: summaryPulse 6.2s ease-in-out infinite;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.summary-card:nth-child(2) {
  animation-delay: 1.2s;
}

.summary-card:nth-child(3) {
  animation-delay: 2.4s;
}

.summary-card:hover {
  transform: translateY(-2px);
  border-color: rgba(43, 200, 255, 0.24);
  box-shadow:
    0 26px 56px rgba(2, 8, 20, 0.44),
    inset 0 1px 0 rgba(180, 224, 255, 0.05),
    0 0 18px rgba(43, 200, 255, 0.08);
}

.metric-text {
  margin-top: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #dff4ff;
}

.role-table-card {
  padding: 18px;
}

.table td:last-child {
  width: 160px;
}

@keyframes summaryPulse {
  0%,
  100% {
    box-shadow:
      0 24px 60px rgba(2, 8, 20, 0.42),
      inset 0 1px 0 rgba(180, 224, 255, 0.05);
  }
  50% {
    box-shadow:
      0 24px 60px rgba(2, 8, 20, 0.42),
      inset 0 1px 0 rgba(180, 224, 255, 0.05),
      0 0 18px rgba(43, 200, 255, 0.08);
  }
}

@media (prefers-reduced-motion: reduce) {
  .summary-card {
    animation: none;
  }
}
</style>
