import type { Material } from '@/types/material'

export const mockMaterials: Material[] = [
  {
    id: 'mat-001',
    adId: 'ad-001',
    name: '轻食主视觉图',
    description: '主 KV 图片，适配 16:9 横屏。',
    type: 'image',
    url: 'https://placehold.co/600x400?text=Light+Meal',
    uploaderId: 'u9',
    status: 'approved',
    reviewedBy: 'u6',
    createdAt: '2026-04-08T10:20:00Z'
  },
  {
    id: 'mat-002',
    adId: 'ad-002',
    name: '咖啡月卡视频',
    description: '15 秒竖改横宣传视频。',
    type: 'video',
    url: 'https://placehold.co/600x400?text=Coffee+Video',
    uploaderId: 'u10',
    status: 'pending_review',
    createdAt: '2026-04-09T09:10:00Z'
  }
]
