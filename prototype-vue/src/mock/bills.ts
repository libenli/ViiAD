import type { Bill } from '@/types/bill'

export const mockBills: Bill[] = [
  { id: 'bill-001', advertiserId: 'u10', agentId: 'u9', planId: 'plan-001', amount: 15000, commission: 1200, invoiceRequested: true, status: 'pending', createdAt: '2026-04-09T12:00:00Z' },
  { id: 'bill-002', advertiserId: 'u10', planId: 'plan-002', amount: 9000, invoiceRequested: false, status: 'confirmed', createdAt: '2026-04-10T09:30:00Z' }
]
