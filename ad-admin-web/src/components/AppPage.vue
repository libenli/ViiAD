<template>
  <section class="page-block">
    <div class="page-toolbar">
      <div>
        <p class="eyebrow">{{ eyebrow }}</p>
        <h2>{{ title }}</h2>
      </div>
      <div class="page-actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="stat-grid">
      <div v-for="item in stats" :key="item.label" class="stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

    <el-card class="work-card" shadow="never">
      <slot />
    </el-card>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  stats: Array<{ label: string; value: string | number }>
}>()
</script>
