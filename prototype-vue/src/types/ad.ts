export interface AdDemand {
  id: string
  title: string
  description?: string
  advertiserId: string
  agentId?: string
  type: 'image' | 'video' | 'interactive'
  objective: 'exposure' | 'conversion' | 'engagement'
  region: string
  budget: number
  status: 'draft' | 'submitted' | 'planning' | 'live' | 'finished'
  materialIds: string[]
  planId?: string
  createdAt: string
}
