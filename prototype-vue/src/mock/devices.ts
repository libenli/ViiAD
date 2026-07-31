import type { Device } from '@/types/device'

export const mockDevices: Device[] = [
  {
    id: 'device-001',
    code: 'SH-XH-001',
    building: '港汇中心',
    floor: '1F',
    region: '上海-徐汇',
    screenSize: '55寸',
    status: 'online',
    currentPlanId: 'plan-001',
    currentAdId: 'ad-001',
    lastHeartbeatAt: '2026-04-10T07:58:00Z'
  },
  {
    id: 'device-002',
    code: 'SH-XH-002',
    building: '港汇中心',
    floor: 'B1',
    region: '上海-徐汇',
    screenSize: '65寸',
    status: 'fault',
    currentPlanId: 'plan-001',
    currentAdId: 'ad-001',
    lastHeartbeatAt: '2026-04-10T07:50:00Z'
  },
  {
    id: 'device-003',
    code: 'SH-PD-003',
    building: '陆家嘴金融广场',
    floor: '1F',
    region: '上海-浦东',
    screenSize: '75寸',
    status: 'online',
    currentPlanId: 'plan-002',
    currentAdId: 'ad-002',
    lastHeartbeatAt: '2026-04-10T08:05:00Z'
  }
]
