<template>
  <div class="entry-page">
    <div class="entry-grid">
      <section class="hero-panel">
        <p class="eyebrow">SECURE ACCESS</p>
        <h1>百万大屏广告系统</h1>
        <p class="lead">
          这是原型工程的登录入口。
        </p>

        <div class="feature-list">
          <div class="feature-item">
            <strong>13 类角色演示</strong>
            <span>支持从不同岗位视角查看广告、素材、计划、设备、账单与日志。</span>
          </div>
          <div class="feature-item">
            <strong>主线流程可走通</strong>
            <span>广告创建、素材审核、计划上线、设备联动、账单结算均可演示。</span>
          </div>
          <div class="feature-item">
            <strong>本地假数据安全演示</strong>
            <span>所有数据均为原型 mock 数据，可随时重置，不影响真实业务。</span>
          </div>
        </div>
      </section>

      <section class="login-card">
        <div class="card-head">
          <h2>账号登录</h2>
          <p class="muted">建议正式演示优先从 `/demo` 入口进入，日常调试可直接在这里登录。</p>
        </div>

        <div class="form">
          <label>
            <span>账号</span>
            <input v-model="username" class="input" placeholder="例如 advertiser01" />
          </label>
          <label>
            <span>密码</span>
            <input v-model="password" class="input" type="password" placeholder="默认 123456" />
          </label>
          <button class="btn" @click="submit">进入系统</button>
        </div>

        <p v-if="error" class="error">{{ error }}</p>

        <div class="links">
          <RouterLink class="entry-link" to="/demo">进入演示入口</RouterLink>
          <RouterLink class="entry-link" to="/switch-role">查看全部角色账号</RouterLink>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const username = ref('admin')
const password = ref('123456')
const error = ref('')

function submit() {
  try {
    authStore.login(username.value, password.value)
    router.push('/')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败'
  }
}
</script>

<style scoped>
.entry-page {
  min-height: 100vh;
  padding: 36px;
  display: grid;
  place-items: center;
}

.entry-grid {
  width: min(1180px, 100%);
  display: grid;
  grid-template-columns: 1.2fr 0.86fr;
  gap: 24px;
}

.hero-panel,
.login-card {
  position: relative;
  overflow: hidden;
  border-radius: 28px;
  border: 1px solid rgba(120, 176, 255, 0.16);
  background:
    linear-gradient(180deg, rgba(15, 30, 52, 0.92), rgba(8, 18, 34, 0.96));
  box-shadow:
    0 24px 60px rgba(2, 8, 20, 0.42),
    inset 0 1px 0 rgba(180, 224, 255, 0.06);
  backdrop-filter: blur(18px);
}

.hero-panel::before,
.login-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top right, rgba(43, 200, 255, 0.18), transparent 30%),
    linear-gradient(135deg, rgba(93, 124, 255, 0.08), transparent 42%);
}

.hero-panel {
  padding: 40px;
}

.login-card {
  padding: 32px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #7ce9ff;
  font-weight: 700;
  letter-spacing: 0.18em;
  font-size: 12px;
}

.hero-panel h1,
.card-head h2 {
  margin: 0;
  color: #f4fbff;
}

.hero-panel h1 {
  font-size: 42px;
  line-height: 1.08;
}

.lead {
  margin: 14px 0 0;
  max-width: 620px;
  color: #9bb8da;
  font-size: 16px;
  line-height: 1.75;
}

.feature-list {
  display: grid;
  gap: 14px;
  margin-top: 28px;
}

.feature-item {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(120, 176, 255, 0.12);
  background: rgba(13, 27, 48, 0.66);
}

.feature-item strong {
  display: block;
  margin-bottom: 6px;
  color: #eef8ff;
}

.feature-item span {
  color: #8da8ca;
  font-size: 14px;
}

.card-head p {
  margin: 8px 0 0;
}

.form {
  display: grid;
  gap: 14px;
  margin: 24px 0 18px;
}

label span {
  display: block;
  margin-bottom: 8px;
  color: #b5cae6;
  font-size: 13px;
}

.links {
  display: grid;
  gap: 10px;
}

.entry-link {
  display: block;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(120, 176, 255, 0.12);
  background: rgba(12, 25, 45, 0.56);
  color: #dcecff;
}

.entry-link:hover {
  background: rgba(19, 40, 71, 0.7);
}

.error {
  margin: 0 0 16px;
  color: #ff8f95;
}
</style>
