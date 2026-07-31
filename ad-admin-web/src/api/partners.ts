import { request, type ApiResponse } from '@/utils/request'
import type { PageResult } from './ads'
export { advertiserSourceMap, partnerStatusMap } from '@/config/status'

export interface Advertiser {
  id: number
  advertiserCode: string
  advertiserName: string
  companyName?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  ownerUserId?: number
  status: string
  sourceType?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface Agent {
  id: number
  agentCode: string
  agentName: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  ownerUserId?: number
  status: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface AdvertiserPayload {
  advertiserName: string
  companyName?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  ownerUserId?: number
  sourceType?: string
  remark?: string
}

export interface AgentPayload {
  agentName: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  ownerUserId?: number
  remark?: string
}

export interface PartnerQuery {
  keyword?: string
  status?: string
  sourceType?: string
  ownerUserId?: number
  page?: number
  size?: number
}

export function fetchAdvertisers(params: PartnerQuery) {
  return request.get('/advertisers', { params }) as Promise<ApiResponse<PageResult<Advertiser>>>
}

export function createAdvertiser(data: AdvertiserPayload) {
  return request.post('/advertisers', data) as Promise<ApiResponse<Advertiser>>
}

export function updateAdvertiser(id: number, data: AdvertiserPayload) {
  return request.put(`/advertisers/${id}`, data) as Promise<ApiResponse<Advertiser>>
}

export function enableAdvertiser(id: number) {
  return request.post(`/advertisers/${id}/enable`) as Promise<ApiResponse<Advertiser>>
}

export function disableAdvertiser(id: number) {
  return request.post(`/advertisers/${id}/disable`) as Promise<ApiResponse<Advertiser>>
}

export function fetchAgents(params: PartnerQuery) {
  return request.get('/agents', { params }) as Promise<ApiResponse<PageResult<Agent>>>
}

export function createAgent(data: AgentPayload) {
  return request.post('/agents', data) as Promise<ApiResponse<Agent>>
}

export function updateAgent(id: number, data: AgentPayload) {
  return request.put(`/agents/${id}`, data) as Promise<ApiResponse<Agent>>
}

export function enableAgent(id: number) {
  return request.post(`/agents/${id}/enable`) as Promise<ApiResponse<Agent>>
}

export function disableAgent(id: number) {
  return request.post(`/agents/${id}/disable`) as Promise<ApiResponse<Agent>>
}
