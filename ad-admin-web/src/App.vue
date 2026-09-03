<template>
  <el-config-provider :locale="elementLocale">
    <RouterView />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { useLocaleStore } from '@/stores/locale'

const route = useRoute()
const locale = useLocaleStore()
const elementLocale = computed(() => (locale.language === 'zh-CN' ? zhCn : en))

watchEffect(() => {
  const titleKey = typeof route.meta.titleKey === 'string' ? route.meta.titleKey : ''
  const fallback = typeof route.meta.title === 'string' ? route.meta.title : 'ViiAD'
  const pageTitle = titleKey ? locale.t(titleKey, fallback) : fallback
  document.title = pageTitle ? `${pageTitle} - ViiAD` : 'ViiAD'
})
</script>
