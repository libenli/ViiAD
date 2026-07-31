import { defineStore } from 'pinia'
import { getUserInfo } from '@/api/auth'
import { getToken, removeToken, setToken } from '@/utils/auth'

export interface UserInfo {
  id: number
  username: string
  realName: string
  userType?: string
  advertiserId?: number
  agentId?: number
  roles: string[]
  permissions: string[]
  menus: string[]
  dataScope?: string
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: null as UserInfo | null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    hasPermission: (state) => (permission: string) => {
      if (!permission) {
        return true
      }
      const roles = state.userInfo?.roles || []
      const permissions = state.userInfo?.permissions || []
      return roles.includes('super_admin') || permissions.includes(permission)
    },
    isPlatformScope: (state) => state.userInfo?.userType === 'platform',
    isBusinessScope: (state) => ['advertiser', 'agent'].includes(state.userInfo?.userType || ''),
    dataScopeLabel: (state) => {
      const userType = state.userInfo?.userType
      if (userType === 'advertiser') {
        return `广告主数据 #${state.userInfo?.advertiserId || '-'}`
      }
      if (userType === 'agent') {
        return `代理商数据 #${state.userInfo?.agentId || '-'}`
      }
      return ''
    }
  },
  actions: {
    setToken(token: string) {
      this.token = token
      setToken(token)
    },
    setUserInfo(userInfo: UserInfo) {
      this.userInfo = userInfo
    },
    async fetchUserInfo() {
      const result = await getUserInfo()
      this.userInfo = {
        ...result.data.user,
        roles: result.data.roles,
        permissions: result.data.permissions,
        menus: result.data.menus,
        dataScope: result.data.dataScope
      }
      return this.userInfo
    },
    logout() {
      this.token = ''
      this.userInfo = null
      removeToken()
    }
  }
})
