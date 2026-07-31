export interface Material {
  id: string
  adId: string
  name: string
  description?: string
  type: 'image' | 'video'
  url: string
  uploaderId: string
  status: 'uploaded' | 'pending_review' | 'approved' | 'rejected'
  reviewComment?: string
  reviewedBy?: string
  createdAt: string
}
