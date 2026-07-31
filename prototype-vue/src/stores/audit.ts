import { defineStore } from 'pinia'
import { mockLogs } from '@/mock/logs'

function createLogId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  return `log-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

export const useAuditStore = defineStore('audit', {
  state: () => ({
    logs: structuredClone(mockLogs)
  }),
  actions: {
    addLog(payload: {
      actorId: string
      actorRole: string
      action: string
      targetType: string
      targetId: string
      detail?: string
    }) {
      this.logs.unshift({
        id: createLogId(),
        time: new Date().toISOString(),
        ...payload
      })
    },
    resetDemoData() {
      this.logs = structuredClone(mockLogs)
    }
  }
})
