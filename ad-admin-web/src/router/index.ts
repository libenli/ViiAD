import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
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

function getRoutePath(parentPath: string, routePath: string) {
  if (routePath.startsWith('/')) {
    return routePath
  }
  return `${parentPath.replace(/\/$/, '')}/${routePath}`.replace(/\/+/g, '/')
}

function getFirstAccessiblePath(user: ReturnType<typeof useUserStore>) {
  const rootRoute = constantRoutes.find((route) => route.path === '/')
  const childRoutes = rootRoute?.children || []
  const sortedRoutes = [
    ...childRoutes.filter((route) => route.path === 'dashboard'),
    ...childRoutes.filter((route) => route.path !== 'dashboard')
  ]
  const accessible = sortedRoutes.find((route) => canAccessRoute(route, user))
  return accessible ? getRoutePath('/', accessible.path) : '/401'
}

function canAccessRoute(route: RouteRecordRaw, user: ReturnType<typeof useUserStore>) {
  const permission = route.meta?.permission
  return !permission || user.hasPermission(String(permission))
}

function normalizeRedirect(redirect: unknown) {
  const target = Array.isArray(redirect) ? redirect[0] : redirect
  if (typeof target !== 'string' || !target.startsWith('/') || target.startsWith('//') || target.startsWith('/login')) {
    return ''
  }
  return target
}

router.beforeEach(async (to) => {
  const token = getToken()
  const user = useUserStore(pinia)
  if (!whiteList.includes(to.path) && !token) {
    return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }
  if (to.path === '/login' && token) {
    if (!user.userInfo) {
      try {
        await user.fetchUserInfo()
      } catch {
        user.logout()
        return true
      }
    }
    return getFirstAccessiblePath(user)
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
    const fallbackPath = getFirstAccessiblePath(user)
    return fallbackPath !== to.path ? fallbackPath : '/401'
  }
  return true
})

export { getFirstAccessiblePath, normalizeRedirect }
export default router
