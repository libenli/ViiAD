import type { AdDemand } from '@/types/ad'

export const mockAds: AdDemand[] = [
  {
    id: 'ad-001',
    title: '午餐轻食套餐推广',
    description: '面向办公楼午餐白领场景的轻食套餐促销。',
    advertiserId: 'u10',
    agentId: 'u9',
    type: 'image',
    objective: 'conversion',
    region: '上海-徐汇',
    budget: 15000,
    status: 'planning',
    materialIds: ['mat-001'],
    planId: 'plan-001',
    createdAt: '2026-04-08T10:00:00Z'
  },
  {
    id: 'ad-002',
    title: '写字楼咖啡月卡活动',
    description: '早高峰和下午茶时段的咖啡月卡活动宣传。',
    advertiserId: 'u10',
    type: 'video',
    objective: 'exposure',
    region: '上海-浦东',
    budget: 9000,
    status: 'live',
    materialIds: ['mat-002'],
    planId: 'plan-002',
    createdAt: '2026-04-09T08:30:00Z'
  }
]
