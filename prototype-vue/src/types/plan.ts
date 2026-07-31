export interface DeliveryPlan {
  id: string
  adId: string
  materialIds: string[]
  region: string
  buildingIds: string[]
  deviceIds: string[]
  startAt: string
  endAt: string
  targeting: {
    timeSlots: string[]
    audienceTags: string[]
    lbsEnabled: boolean
  }
  status: 'draft' | 'pending_schedule' | 'scheduled' | 'live' | 'paused' | 'finished'
  operatorId?: string
}
