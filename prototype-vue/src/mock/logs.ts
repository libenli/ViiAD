import type { AuditLog } from '@/types/log'

export const mockLogs: AuditLog[] = [
  { id: 'log-001', time: '2026-04-10T08:10:00Z', actorId: 'u6', actorRole: 'reviewer', action: 'approve_material', targetType: 'material', targetId: 'mat-001', detail: '素材审核通过' },
  { id: 'log-002', time: '2026-04-10T08:20:00Z', actorId: 'u5', actorRole: 'business', action: 'schedule_plan', targetType: 'plan', targetId: 'plan-001', detail: '完成投放排期' },
  { id: 'log-003', time: '2026-04-10T08:35:00Z', actorId: 'u3', actorRole: 'ops', action: 'receive_workorder', targetType: 'workorder', targetId: 'wo-001', detail: '接收设备故障工单' }
]
