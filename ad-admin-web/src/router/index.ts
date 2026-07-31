import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { pinia } from '@/stores'
import { useUserStore } from '@/stores/user'
import { constantRoutes } from './routes'

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

const whiteList = ['/login', '/401']

router.beforeEach(async (to) => {
  const token = getToken()
  const user = useUserStore(pinia)
  if (!whiteList.includes(to.path) && !token) {
    return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  if (token && !user.userInfo) {
    try {
      await user.fetchUserInfo()
    } catch {
      user.logout()
      return `/login?redirect=${encodeURIComponent(to.fullPath)}`
    }
  }
  const permission = [...to.matched].reverse().find((item) => item.meta.permission)?.meta.permission
  if (permission && !user.hasPermission(String(permission))) {
    return '/401'
  }
  return true
})

export default router
