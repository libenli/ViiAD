import { request, type ApiResponse } from '@/utils/request'
export { adStatusMap, adTypeOptions, objectiveOptions } from '@/config/status'

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface AdOrder {
  id: number
  adCode: string
  adName: string
  advertiserId: number
  agentId?: number
  adType: string
  objective?: string
  regionCode?: string
  budgetAmount?: number
  description?: string
  status: string
  submitTime?: string
  auditUserId?: number
  auditComment?: string
  auditTime?: string
  createTime?: string
  updateTime?: string
}

export interface AdOrderQuery {
  keyword?: string
  status?: string
  adType?: string
  regionCode?: string
  page?: number
  size?: number
}

export interface AdOrderPayload {
  adName: string
  advertiserId: number
  agentId?: number
  adType: string
  objective?: string
  regionCode?: string
  budgetAmount?: number
  description?: string
}

export function fetchAds(params: AdOrderQuery) {
  return request.get('/ads', { params }) as Promise<ApiResponse<PageResult<AdOrder>>>
}

export function fetchAdDetail(id: number) {
  return request.get(`/ads/${id}`) as Promise<ApiResponse<AdOrder>>
}

export function createAd(data: AdOrderPayload) {
  return request.post('/ads', data) as Promise<ApiResponse<AdOrder>>
}

export function updateAd(id: number, data: AdOrderPayload) {
  return request.put(`/ads/${id}`, data) as Promise<ApiResponse<AdOrder>>
}

export function submitAd(id: number) {
  return request.post(`/ads/${id}/submit`) as Promise<ApiResponse<AdOrder>>
}

export function approveAd(id: number, comment?: string) {
  return request.post(`/ads/${id}/approve`, { comment }) as Promise<ApiResponse<AdOrder>>
}

export function rejectAd(id: number, comment: string) {
  return request.post(`/ads/${id}/reject`, { comment }) as Promise<ApiResponse<AdOrder>>
}

