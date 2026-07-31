<template>
  <div class="layout-shell">
    <AppHeader />
    <div class="body">
      <AppSidebar />
      <main class="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
</script>

<style scoped>
.layout-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 100% 0%, rgba(43, 200, 255, 0.08), transparent 24%),
    radial-gradient(circle at 0% 20%, rgba(93, 124, 255, 0.08), transparent 28%);
}

.body {
  display: flex;
}

.main {
  flex: 1;
  padding: 24px;
  position: relative;
}

.main::before {
  content: "";
  position: absolute;
  inset: 18px 18px 18px 0;
  border-radius: 28px 0 0 28px;
  border: 1px solid rgba(120, 176, 255, 0.08);
  background: linear-gradient(180deg, rgba(9, 20, 36, 0.16), rgba(9, 20, 36, 0));
  pointer-events: none;
}
</style>
