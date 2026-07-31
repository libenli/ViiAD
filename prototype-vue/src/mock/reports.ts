import type { ReportRecord } from '@/types/report'

export const mockReports: ReportRecord[] = [
  { id: 'rep-001', adId: 'ad-001', planId: 'plan-001', date: '2026-04-09', impressions: 12300, clicks: 420, interactions: 188, conversions: 59, amount: 5480 },
  { id: 'rep-002', adId: 'ad-002', planId: 'plan-002', date: '2026-04-10', impressions: 9800, clicks: 360, interactions: 145, conversions: 37, amount: 3210 }
]
