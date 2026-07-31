import type { FeedbackRecord } from '@/types/feedback'

export const mockFeedback: FeedbackRecord[] = [
  { id: 'fb-001', audienceId: 'u13', adId: 'ad-002', category: 'favorite', content: '这个咖啡活动不错，已收藏。', status: 'closed', createdAt: '2026-04-10T09:02:00Z' },
  { id: 'fb-002', audienceId: 'u13', adId: 'ad-001', category: 'service', content: '扫码后跳转页加载有点慢。', status: 'processing', createdAt: '2026-04-10T09:10:00Z' }
]
