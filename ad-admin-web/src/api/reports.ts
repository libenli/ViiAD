import { request, type ApiResponse } from '@/utils/request'
import type { PageResult } from './ads'
export { sourceTypeMap } from '@/config/status'

export interface PlayLog {
  id: number
  adId: number
  planId: number
  materialId?: number
  deviceId: number
  playDate: string
  playCount: number
  playDuration: number
  playStatus?: string
  playStartTime?: string
  playEndTime?: string
  sourceType: string
  requestId?: string
  errorMessage?: string
  createTime?: string
}

export interface ReportQuery {
  adId?: number
  planId?: number
  deviceId?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface ReportOverview {
  playCount: number
  exposureCount: number
  activeDeviceCount: number
  playDuration: number
  logCount: number
  completionRate: string
}

export function fetchReportOverview(params: ReportQuery) {
  return request.get('/reports/overview', { params }) as Promise<ApiResponse<ReportOverview>>
}

export function fetchPlayLogs(params: ReportQuery) {
  return request.get('/play-logs', { params }) as Promise<ApiResponse<PageResult<PlayLog>>>
}
