<template>
  <el-result icon="error" title="404" :sub-title="locale.t('page.error.notFound')">
    <template #extra>
      <el-button type="primary" @click="$router.replace('/dashboard')">{{ locale.t('page.error.backDashboard') }}</el-button>
    </template>
  </el-result>
</template>

<script setup lang="ts">
import { useLocaleStore } from '@/stores/locale'

const locale = useLocaleStore()
</script>
