import type { Workorder } from '@/types/workorder'

export const mockWorkorders: Workorder[] = [
  { id: 'wo-001', type: 'device_fault', sourceRole: 'system', sourceId: 'device-002', assigneeId: 'u3', relatedId: 'device-002', status: 'open', priority: 'high', createdAt: '2026-04-10T08:00:00Z' },
  { id: 'wo-002', type: 'content_issue', sourceRole: 'business', sourceId: 'u5', assigneeId: 'u11', relatedId: 'ad-002', status: 'processing', priority: 'medium', createdAt: '2026-04-10T09:15:00Z' }
]
