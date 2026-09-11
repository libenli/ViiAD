import { request, type ApiResponse } from '@/utils/request'
import type { PageResult } from './ads'
export { planDeliveryStatusMap as deliveryStatusMap, scheduleStatusMap } from '@/config/status'

export interface AdPlan {
  id: number
  planCode: string
  planName: string
  adId: number
  regionCode?: string
  startTime: string
  endTime: string
  scheduleStatus: string
  deliveryStatus: string
  operatorId?: number
  createUserId?: number
  materialIds?: number[]
  deviceIds?: number[]
  createTime?: string
  updateTime?: string
}

export interface AdPlanDeviceReceipt {
  deviceId: number
  deviceCode: string
  deviceName: string
  onlineStatus?: string
  faultStatus?: string
  lastOnlineTime?: string
  deliveryRecordId?: number
  deliveryStatus: string
  responseMsg?: string
  requestId?: string
  deliveryTime?: string
  ackTime?: string
  ackMessage?: string
  playStatus: string
  lastPlayTime?: string
  playErrorMessage?: string
  playCount?: number
}

export interface AdPlanQuery {
  keyword?: string
  adId?: number
  regionCode?: string
  scheduleStatus?: string
  deliveryStatus?: string
  page?: number
  size?: number
}

export interface AdPlanPayload {
  planName: string
  adId: number
  regionCode?: string
  startTime: string
  endTime: string
  materialIds: number[]
  deviceIds: number[]
  operatorId?: number
}

export function fetchPlans(params: AdPlanQuery) {
  return request.get('/plans', { params }) as Promise<ApiResponse<PageResult<AdPlan>>>
}

export function fetchPlanDetail(id: number) {
  return request.get(`/plans/${id}`) as Promise<ApiResponse<AdPlan>>
}

export function fetchPlanDeviceReceipts(id: number) {
  return request.get(`/plans/${id}/device-receipts`) as Promise<ApiResponse<AdPlanDeviceReceipt[]>>
}

export function createPlan(data: AdPlanPayload) {
  return request.post('/plans', data) as Promise<ApiResponse<AdPlan>>
}

export function updatePlan(id: number, data: AdPlanPayload) {
  return request.put(`/plans/${id}`, data) as Promise<ApiResponse<AdPlan>>
}

export function schedulePlan(id: number) {
  return request.post(`/plans/${id}/schedule`) as Promise<ApiResponse<AdPlan>>
}

export function startPlan(id: number) {
  return request.post(`/plans/${id}/start`) as Promise<ApiResponse<AdPlan>>
}

export function pausePlan(id: number) {
  return request.post(`/plans/${id}/pause`) as Promise<ApiResponse<AdPlan>>
}

export function finishPlan(id: number) {
  return request.post(`/plans/${id}/finish`) as Promise<ApiResponse<AdPlan>>
}
