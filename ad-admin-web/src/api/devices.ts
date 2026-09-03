import { request, type ApiResponse } from '@/utils/request'
import type { PageResult } from './ads'
export { deviceStatusMap, faultStatusMap, onlineStatusMap } from '@/config/status'

export interface AdDevice {
  id: number
  deviceCode: string
  deviceName: string
  buildingId?: number
  floorNo?: string
  screenSize?: string
  resolution?: string
  ipAddress?: string
  provinceName?: string
  cityName?: string
  regionName?: string
  macAddress?: string
  onlineStatus: string
  faultStatus: string
  lastOnlineTime?: string
  currentPlanId?: number
  status: string
  createTime?: string
  updateTime?: string
}

export interface AdDeviceQuery {
  keyword?: string
  buildingId?: number
  onlineStatus?: string
  faultStatus?: string
  status?: string
  currentPlanId?: number
  page?: number
  size?: number
}

export interface AdDevicePayload {
  deviceCode: string
  deviceName: string
  buildingId?: number
  floorNo?: string
  screenSize?: string
  resolution?: string
  ipAddress?: string
  macAddress?: string
}

export function fetchDevices(params: AdDeviceQuery) {
  return request.get('/devices', { params }) as Promise<ApiResponse<PageResult<AdDevice>>>
}

export function fetchDeviceDetail(id: number) {
  return request.get(`/devices/${id}`) as Promise<ApiResponse<AdDevice>>
}

export function createDevice(data: AdDevicePayload) {
  return request.post('/devices', data) as Promise<ApiResponse<AdDevice>>
}

export function updateDevice(id: number, data: AdDevicePayload) {
  return request.put(`/devices/${id}`, data) as Promise<ApiResponse<AdDevice>>
}

export function setDeviceOnline(id: number) {
  return request.post(`/devices/${id}/online`) as Promise<ApiResponse<AdDevice>>
}

export function setDeviceOffline(id: number) {
  return request.post(`/devices/${id}/offline`) as Promise<ApiResponse<AdDevice>>
}

export function markDeviceFault(id: number) {
  return request.post(`/devices/${id}/fault`) as Promise<ApiResponse<AdDevice>>
}

export function repairDevice(id: number) {
  return request.post(`/devices/${id}/repair`) as Promise<ApiResponse<AdDevice>>
}

export function enableDevice(id: number) {
  return request.post(`/devices/${id}/enable`) as Promise<ApiResponse<AdDevice>>
}

export function disableDevice(id: number) {
  return request.post(`/devices/${id}/disable`) as Promise<ApiResponse<AdDevice>>
}
