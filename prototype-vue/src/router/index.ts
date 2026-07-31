import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { pinia } from '@/app/pinia'
import { useAuthStore } from '@/stores/auth'

export const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore(pinia)

  if (to.name === 'login' || to.name === 'switch-role' || to.name === 'demo') {
    return true
  }

  if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
    return { name: 'login' }
  }

  const roles = to.meta.roles as Parameters<typeof authStore.hasRole>[0]
  if (roles?.length && !authStore.hasRole(roles)) {
    return { name: '403' }
  }

  return true
})
