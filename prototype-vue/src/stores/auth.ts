import { defineStore } from 'pinia'
import type { RoleCode } from '@/config/roles'
import type { MockUser } from '@/types/user'
import { mockUsers } from '@/mock/users'

const AUTH_STORAGE_KEY = 'vad-demo-auth'

function loadStoredAuth() {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) {
    return { currentUser: null as MockUser | null, token: '' }
  }

  try {
    const parsed = JSON.parse(raw) as { currentUser: MockUser | null; token: string }
    return {
      currentUser: parsed.currentUser ?? null,
      token: parsed.token ?? ''
    }
  } catch {
    return { currentUser: null as MockUser | null, token: '' }
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => loadStoredAuth(),
  getters: {
    isLoggedIn: (state) => Boolean(state.currentUser),
    role: (state): RoleCode | null => state.currentUser?.role ?? null
  },
  actions: {
    persist() {
      localStorage.setItem(
        AUTH_STORAGE_KEY,
        JSON.stringify({
          currentUser: this.currentUser,
          token: this.token
        })
      )
    },
    login(username: string, password: string) {
      const user = mockUsers.find(
        (item) => item.username === username && item.password === password
      )
      if (!user) {
        throw new Error('账号或密码错误')
      }
      this.currentUser = user
      this.token = `mock-token-${user.id}`
      this.persist()
    },
    quickLogin(username: string) {
      const user = mockUsers.find((item) => item.username === username)
      if (!user) {
        throw new Error('未找到该角色账号')
      }
      this.currentUser = user
      this.token = `mock-token-${user.id}`
      this.persist()
    },
    logout() {
      this.currentUser = null
      this.token = ''
      localStorage.removeItem(AUTH_STORAGE_KEY)
    },
    hasRole(roles?: RoleCode[]) {
      if (!roles?.length) {
        return true
      }
      return !!this.currentUser && roles.includes(this.currentUser.role)
    }
  }
})
