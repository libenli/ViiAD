import { request, type ApiResponse } from '@/utils/request'
import type { PageResult } from './ads'
export { materialStatusMap, materialTypeOptions } from '@/config/status'

export interface AdMaterial {
  id: number
  materialCode: string
  adId: number
  materialName: string
  materialType: string
  fileUrl: string
  fileSize?: number
  durationSeconds?: number
  width?: number
  height?: number
  coverUrl?: string
  status: string
  reviewUserId?: number
  reviewComment?: string
  reviewTime?: string
  uploaderId?: number
  createTime?: string
  updateTime?: string
}

export interface AdMaterialQuery {
  keyword?: string
  adId?: number
  materialType?: string
  status?: string
  page?: number
  size?: number
}

export interface AdMaterialPayload {
  adId: number
  materialName: string
  materialType: string
  fileUrl: string
  fileSize?: number
  durationSeconds?: number
  width?: number
  height?: number
  coverUrl?: string
}

export function fetchMaterials(params: AdMaterialQuery) {
  return request.get('/materials', { params }) as Promise<ApiResponse<PageResult<AdMaterial>>>
}

export function fetchMaterialDetail(id: number) {
  return request.get(`/materials/${id}`) as Promise<ApiResponse<AdMaterial>>
}

export function createMaterial(data: AdMaterialPayload) {
  return request.post('/materials', data) as Promise<ApiResponse<AdMaterial>>
}

export function updateMaterial(id: number, data: AdMaterialPayload) {
  return request.put(`/materials/${id}`, data) as Promise<ApiResponse<AdMaterial>>
}

export function submitMaterial(id: number) {
  return request.post(`/materials/${id}/submit`) as Promise<ApiResponse<AdMaterial>>
}

export function approveMaterial(id: number, comment?: string) {
  return request.post(`/materials/${id}/approve`, { comment }) as Promise<ApiResponse<AdMaterial>>
}

export function rejectMaterial(id: number, comment: string) {
  return request.post(`/materials/${id}/reject`, { comment }) as Promise<ApiResponse<AdMaterial>>
}

