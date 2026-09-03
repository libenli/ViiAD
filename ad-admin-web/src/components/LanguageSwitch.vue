<template>
  <div class="language-switch" :class="{ compact }">
    <button :class="{ active: locale.language === 'zh-CN' }" type="button" @click="locale.setLanguage('zh-CN')">
      {{ locale.messages.common.chinese }}
    </button>
    <span>|</span>
    <button :class="{ active: locale.language === 'en-US' }" type="button" @click="locale.setLanguage('en-US')">
      {{ locale.messages.common.english }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { useLocaleStore } from '@/stores/locale'

defineProps<{
  compact?: boolean
}>()

const locale = useLocaleStore()
</script>

<style scoped>
.language-switch {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #7dddec;
  font-size: 15px;
}

.language-switch.compact {
  gap: 8px;
  font-size: 13px;
}

.language-switch button {
  padding: 0;
  border: 0;
  color: #8bb8c8;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.language-switch button.active,
.language-switch button:hover {
  color: #ffffff;
}
</style>
