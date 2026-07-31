export interface Device {
  id: string
  code: string
  building: string
  floor: string
  region: string
  screenSize: string
  status: 'online' | 'offline' | 'fault'
  currentPlanId?: string
  currentAdId?: string
  lastHeartbeatAt: string
}
