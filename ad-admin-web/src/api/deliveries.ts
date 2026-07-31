import { request, type ApiResponse } from '@/utils/request'
import type { PageResult } from './ads'
export { deliveryRecordStatusMap as deliveryStatusMap, deliveryTypeMap } from '@/config/status'

export interface AdDeliveryRecord {
  id: number
  planId: number
  deviceId: number
  deliveryType: string
  deliveryStatus: string
  responseMsg?: string
  retryCount?: number
  deliveryTime?: string
}

export interface AdDeliveryQuery {
  planId?: number
  deviceId?: number
  deliveryType?: string
  deliveryStatus?: string
  page?: number
  size?: number
}

export function fetchDeliveries(params: AdDeliveryQuery) {
  return request.get('/deliveries', { params }) as Promise<ApiResponse<PageResult<AdDeliveryRecord>>>
}

export function fetchDeliveryDetail(id: number) {
  return request.get(`/deliveries/${id}`) as Promise<ApiResponse<AdDeliveryRecord>>
}

export function markDeliverySuccess(id: number, responseMsg?: string) {
  return request.post(`/deliveries/${id}/success`, { responseMsg }) as Promise<ApiResponse<AdDeliveryRecord>>
}

export function markDeliveryFailed(id: number, responseMsg?: string) {
  return request.post(`/deliveries/${id}/fail`, { responseMsg }) as Promise<ApiResponse<AdDeliveryRecord>>
}

export function retryDelivery(id: number) {
  return request.post(`/deliveries/${id}/retry`) as Promise<ApiResponse<AdDeliveryRecord>>
}
