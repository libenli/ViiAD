<template>
  <Transition name="toast-fade">
    <div v-if="toast" class="toast" :class="`toast-${toast.type}`">
      <div class="toast-mark">{{ badgeMap[toast.type] }}</div>
      <div class="toast-body">
        <strong>{{ toast.title }}</strong>
        <p v-if="toast.detail">{{ toast.detail }}</p>
      </div>
      <button class="toast-close" @click="appStore.clearToast">×</button>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const toast = computed(() => appStore.toast)

const badgeMap = {
  success: 'OK',
  info: 'FYI',
  warning: '!',
  error: 'ERR'
} as const

let timer: ReturnType<typeof setTimeout> | null = null

watch(
  toast,
  (current) => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }

    if (!current) return

    timer = setTimeout(() => {
      appStore.clearToast()
    }, 2600)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (timer) clearTimeout(timer)
})
</script>

<style scoped>
.toast {
  position: fixed;
  top: 22px;
  right: 24px;
  z-index: 1200;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: start;
  gap: 14px;
  min-width: 320px;
  max-width: 420px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(120, 176, 255, 0.2);
  background:
    linear-gradient(180deg, rgba(15, 31, 55, 0.95), rgba(7, 18, 34, 0.98));
  box-shadow:
    0 20px 40px rgba(2, 8, 20, 0.42),
    0 0 28px rgba(43, 200, 255, 0.08);
  backdrop-filter: blur(18px);
}

.toast-success {
  border-color: rgba(70, 232, 158, 0.28);
}

.toast-info {
  border-color: rgba(43, 200, 255, 0.24);
}

.toast-warning {
  border-color: rgba(255, 209, 102, 0.28);
}

.toast-error {
  border-color: rgba(255, 108, 122, 0.3);
}

.toast-mark {
  min-width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 800;
  background: rgba(43, 200, 255, 0.14);
  color: #9ef3ff;
}

.toast-success .toast-mark {
  background: rgba(87, 240, 163, 0.14);
  color: #7ff6b8;
}

.toast-warning .toast-mark {
  background: rgba(255, 209, 102, 0.14);
  color: #ffe08f;
}

.toast-error .toast-mark {
  background: rgba(255, 108, 122, 0.14);
  color: #ff9ea8;
}

.toast-body strong {
  display: block;
  color: #f2f9ff;
  font-size: 14px;
}

.toast-body p {
  margin: 6px 0 0;
  color: #8fa8ca;
  font-size: 13px;
  line-height: 1.6;
}

.toast-close {
  border: 0;
  background: transparent;
  color: #88a3c7;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  padding: 2px;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.22s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px) translateX(10px);
}
</style>
