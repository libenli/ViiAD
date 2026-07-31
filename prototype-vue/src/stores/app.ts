import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    toast: null as null | {
      id: number
      type: 'success' | 'info' | 'warning' | 'error'
      title: string
      detail?: string
    },
    notifications: [
      { id: 'n1', title: '当前有 1 条素材待审核', read: false },
      { id: 'n2', title: '设备 SH-XH-002 故障待处理', read: false }
    ]
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    showToast(payload: {
      type?: 'success' | 'info' | 'warning' | 'error'
      title: string
      detail?: string
    }) {
      this.toast = {
        id: Date.now(),
        type: payload.type ?? 'success',
        title: payload.title,
        detail: payload.detail
      }
    },
    clearToast() {
      this.toast = null
    }
  }
})
