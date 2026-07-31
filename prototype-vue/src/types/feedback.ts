export interface FeedbackRecord {
  id: string
  audienceId: string
  adId?: string
  category: 'comment' | 'like' | 'favorite' | 'complaint' | 'service'
  content: string
  status: 'new' | 'processing' | 'closed'
  createdAt: string
}
