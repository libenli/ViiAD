<template>
  <div class="demo-page">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">BOSS DEMO MODE</p>
        <h1>百万大屏广告系统</h1>
        <p class="lead">
          可以直接点击角色卡片进入系统，不需要记账号密码，
          更适合现场快速切换视角、走业务主线、讲清系统价值。
        </p>

        <div class="hero-actions">
          <button class="btn" @click="enter('admin')">从全局总览进入</button>
          <button class="btn secondary" @click="resetDemo">恢复初始演示数据</button>
        </div>

        <p v-if="message" class="message">{{ message }}</p>
      </div>

      <div class="hero-panel">
        <h3>推荐演示顺序</h3>
        <ol>
          <li>广告主创建广告需求</li>
          <li>广告主上传素材</li>
          <li>审核岗完成素材审核</li>
          <li>商务运营创建投放计划并上线</li>
          <li>运维查看设备和大屏播放</li>
          <li>财务查看账单与支付状态</li>
          <li>审计查看全流程日志</li>
        </ol>
      </div>
    </section>

    <section class="highlight-grid">
      <div class="highlight-card">
        <div>主线链路</div>
        <strong>广告 -> 素材 -> 计划 -> 播放 -> 账单 -> 审计</strong>
      </div>
      <div class="highlight-card">
        <div>演示方式</div>
        <strong>多角色切换 + 假数据流转</strong>
      </div>
      <div class="highlight-card">
        <div>运行环境</div>
        <strong>本地原型，无需后端联调</strong>
      </div>
    </section>

    <section class="role-section">
      <div class="section-head">
        <div>
          <h2>快速进入角色</h2>
          <p class="page-subtitle">优先选择下列 7 个核心演示角色，最容易把系统主线讲清楚。</p>
        </div>
      </div>

      <div class="role-grid">
        <button
          v-for="card in roleCards"
          :key="card.username"
          class="role-card"
          @click="enter(card.username)"
        >
          <span class="role-tag">{{ card.tag }}</span>
          <strong>{{ card.title }}</strong>
          <span class="role-user">{{ card.username }}</span>
          <small>{{ card.tip }}</small>
        </button>
      </div>
    </section>

    <section class="steps">
      <div class="step-card">
        <h3>操作方式</h3>
        <ul>
          <li>点击角色卡片可直接进入对应工作台。</li>
          <li>系统右上角可随时切换角色或恢复演示数据。</li>
          <li>刷新页面不会丢失当前登录状态。</li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuditStore } from '@/stores/audit'

const router = useRouter()
const authStore = useAuthStore()
const workflowStore = useWorkflowStore()
const auditStore = useAuditStore()
const message = ref('')

const roleCards = [
  { tag: '总览', title: '超级管理员', username: 'admin', tip: '先看全局工作台、菜单和整体链路。' },
  { tag: '需求', title: '广告主', username: 'advertiser01', tip: '演示广告创建和素材提交流程。' },
  { tag: '审核', title: '审核岗', username: 'review01', tip: '演示素材审核通过或驳回。' },
  { tag: '投放', title: '商务运营', username: 'biz01', tip: '演示计划创建、排期与上线。' },
  { tag: '运维', title: '运维工程师', username: 'ops01', tip: '演示设备状态和大屏播放。' },
  { tag: '结算', title: '财务账号', username: 'finance01', tip: '演示账单确认与支付登记。' },
  { tag: '审计', title: '审计账号', username: 'audit01', tip: '演示日志追踪和全流程留痕。' }
] as const

function enter(username: string) {
  authStore.quickLogin(username)
  router.push('/')
}

function resetDemo() {
  workflowStore.resetDemoData()
  auditStore.resetDemoData()
  message.value = '演示数据已恢复到初始状态。'
}
</script>

<style scoped>
.demo-page {
  max-width: 1240px;
  margin: 0 auto;
  padding: 40px 24px 56px;
}

.hero {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.hero-copy,
.hero-panel,
.highlight-card,
.step-card {
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

.hero-copy::before,
.hero-panel::before,
.highlight-card::before,
.step-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top right, rgba(43, 200, 255, 0.16), transparent 28%),
    linear-gradient(135deg, rgba(93, 124, 255, 0.08), transparent 44%);
}

.hero-copy,
.hero-panel,
.step-card {
  padding: 28px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #7ce9ff;
  font-weight: 700;
  letter-spacing: 0.18em;
  font-size: 12px;
}

.hero-copy h1 {
  margin: 0;
  font-size: 44px;
  color: #f4fbff;
}

.lead {
  margin: 14px 0 0;
  color: #9bb8da;
  font-size: 16px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}

.message {
  margin-top: 14px;
  color: #5df0aa;
}

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 26px;
}

.highlight-card {
  padding: 18px 20px;
  animation: highlightPulse 6s ease-in-out infinite;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.highlight-card:nth-child(2) {
  animation-delay: 1.4s;
}

.highlight-card:nth-child(3) {
  animation-delay: 2.6s;
}

.highlight-card:hover {
  transform: translateY(-2px);
  border-color: rgba(43, 200, 255, 0.22);
  box-shadow:
    0 22px 44px rgba(2, 8, 20, 0.38),
    0 0 24px rgba(43, 200, 255, 0.08);
}

.highlight-card div {
  color: #88a9cf;
  font-size: 13px;
}

.highlight-card strong {
  display: block;
  margin-top: 10px;
  color: #f1f8ff;
  line-height: 1.6;
}

.role-section {
  margin-bottom: 28px;
}

.section-head {
  margin-bottom: 14px;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.role-card {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 20px;
  border: 1px solid rgba(120, 176, 255, 0.14);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(16, 33, 58, 0.94), rgba(10, 22, 41, 0.98));
  box-shadow:
    0 14px 30px rgba(2, 8, 20, 0.28),
    inset 0 1px 0 rgba(180, 224, 255, 0.04);
  cursor: pointer;
  text-align: left;
  color: #e7f5ff;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
  animation: roleCardPulse 6.4s ease-in-out infinite;
}

.role-card::before,
.role-card::after {
  content: "";
  position: absolute;
  pointer-events: none;
}

.role-card::before {
  inset: 0;
  background:
    radial-gradient(circle at top right, rgba(43, 200, 255, 0.14), transparent 28%),
    linear-gradient(145deg, rgba(93, 124, 255, 0.08), transparent 46%);
}

.role-card::after {
  top: -30%;
  left: -45%;
  width: 58%;
  height: 180%;
  transform: rotate(18deg);
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(255, 255, 255, 0.02) 42%,
    rgba(43, 200, 255, 0.14) 50%,
    transparent 66%
  );
  opacity: 0.22;
  animation: roleCardSweep 9s ease-in-out infinite;
}

.role-card:nth-child(2n) {
  animation-delay: 1s;
}

.role-card:nth-child(3n) {
  animation-delay: 2.1s;
}

.role-card:nth-child(4n) {
  animation-delay: 3s;
}

.role-card:hover {
  transform: translateY(-3px);
  border-color: rgba(43, 200, 255, 0.24);
  box-shadow:
    0 20px 36px rgba(2, 8, 20, 0.34),
    0 0 0 1px rgba(43, 200, 255, 0.1) inset;
}

.role-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 50px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(43, 200, 255, 0.14);
  color: #86ebff;
  font-size: 12px;
  font-weight: 700;
}

.role-user {
  color: #6ddcff;
  font-weight: 600;
}

.role-card small {
  color: #8fa9c9;
  line-height: 1.7;
}

.steps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

ol,
ul {
  margin: 0;
  padding-left: 20px;
  color: #dbe9ff;
  line-height: 1.9;
}

@keyframes roleCardPulse {
  0%,
  100% {
    box-shadow:
      0 14px 30px rgba(2, 8, 20, 0.28),
      inset 0 1px 0 rgba(180, 224, 255, 0.04);
  }
  50% {
    box-shadow:
      0 18px 36px rgba(2, 8, 20, 0.34),
      inset 0 1px 0 rgba(180, 224, 255, 0.04),
      0 0 18px rgba(43, 200, 255, 0.08);
  }
}

@keyframes roleCardSweep {
  0%,
  100% {
    transform: translateX(-8%) rotate(18deg);
    opacity: 0.1;
  }
  52% {
    opacity: 0.12;
  }
  68% {
    transform: translateX(205%) rotate(18deg);
    opacity: 0.24;
  }
  69% {
    opacity: 0;
  }
}

@keyframes highlightPulse {
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
  .highlight-card,
  .role-card,
  .role-card::after {
    animation: none;
  }
}
</style>
