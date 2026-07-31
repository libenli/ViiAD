export interface Workorder {
  id: string
  type: 'device_fault' | 'content_issue' | 'account_issue' | 'feedback_issue'
  sourceRole: string
  sourceId: string
  assigneeId?: string
  relatedId?: string
  status: 'open' | 'processing' | 'resolved'
  priority: 'low' | 'medium' | 'high'
  createdAt: string
}
