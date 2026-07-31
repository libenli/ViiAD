<template>
  <aside class="sidebar">
    <RouterLink
      v-for="item in menuItems"
      :key="item.key"
      :to="item.route"
      class="menu-item"
      :class="{ active: route.path === item.route }"
    >
      {{ item.label }}
    </RouterLink>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { menuMap } from '@/config/menus'

const authStore = useAuthStore()
const route = useRoute()

const menuItems = computed(() =>
  authStore.role ? menuMap[authStore.role] : []
)
</script>

<style scoped>
.sidebar {
  width: 240px;
  min-height: calc(100vh - 73px);
  padding: 18px 14px;
  background:
    linear-gradient(180deg, rgba(8, 18, 32, 0.96), rgba(7, 16, 28, 0.98));
  border-right: 1px solid rgba(120, 176, 255, 0.1);
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.03);
}

.menu-item {
  display: block;
  padding: 12px 14px;
  margin-bottom: 8px;
  border-radius: 12px;
  color: rgba(219, 233, 255, 0.74);
  border: 1px solid transparent;
  transition: background-color 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.menu-item.active,
.menu-item:hover {
  background: linear-gradient(135deg, rgba(43, 200, 255, 0.12), rgba(93, 124, 255, 0.16));
  color: #f2f8ff;
  border-color: rgba(43, 200, 255, 0.14);
  transform: translateX(2px);
}
</style>
