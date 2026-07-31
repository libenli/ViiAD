import type { DeliveryPlan } from '@/types/plan'

export const mockPlans: DeliveryPlan[] = [
  {
    id: 'plan-001',
    adId: 'ad-001',
    materialIds: ['mat-001'],
    region: '上海-徐汇',
    buildingIds: ['building-001'],
    deviceIds: ['device-001', 'device-002'],
    startAt: '2026-04-11T09:00:00Z',
    endAt: '2026-04-18T18:00:00Z',
    targeting: {
      timeSlots: ['09:00-12:00', '12:00-14:00'],
      audienceTags: ['白领', '午餐人群'],
      lbsEnabled: true
    },
    status: 'scheduled',
    operatorId: 'u5'
  },
  {
    id: 'plan-002',
    adId: 'ad-002',
    materialIds: ['mat-002'],
    region: '上海-浦东',
    buildingIds: ['building-002'],
    deviceIds: ['device-003'],
    startAt: '2026-04-10T08:00:00Z',
    endAt: '2026-04-15T19:00:00Z',
    targeting: {
      timeSlots: ['08:00-10:00', '14:00-18:00'],
      audienceTags: ['通勤', '下午茶'],
      lbsEnabled: false
    },
    status: 'live',
    operatorId: 'u5'
  }
]
