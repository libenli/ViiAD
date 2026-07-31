<template>
  <section>
    <div class="page-title">
      <div>
        <h1>大屏播放模拟</h1>
        <p class="page-subtitle">用网页模拟终端大屏播放与异常提示。</p>
      </div>
    </div>
    <div class="screen">
      <div class="screen-inner">
        <h2>当前播放广告</h2>
        <p>{{ currentAd?.title || '暂无投放广告' }}</p>
        <div class="muted">设备：{{ currentDevice?.code || '-' }}</div>
        <div class="muted">状态：{{ currentDevice?.status || '-' }}</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useWorkflowStore } from '@/stores/workflow'

const workflow = useWorkflowStore()
const currentDevice = computed(() => workflow.devices[0])
const currentAd = computed(() =>
  workflow.ads.find((item) => item.id === currentDevice.value?.currentAdId)
)
</script>

<style scoped>
.screen {
  background: #0c121d;
  border-radius: 24px;
  padding: 28px;
}

.screen-inner {
  min-height: 420px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  color: #fff;
  background:
    linear-gradient(135deg, rgba(26, 86, 219, 0.92), rgba(12, 18, 29, 0.85)),
    url('https://placehold.co/1200x700?text=Screen+Player') center/cover;
}
</style>
