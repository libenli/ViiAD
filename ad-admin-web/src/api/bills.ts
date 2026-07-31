import { request, type ApiResponse } from '@/utils/request'
import type { PageResult } from './ads'
export { billStatusMap, billTypeMap } from '@/config/status'

export interface AdBill {
  id: number
  billNo: string
  billType: string
  advertiserId?: number
  agentId?: number
  billMonth: string
  amountTotal: number
  amountPaid: number
  status: string
  confirmTime?: string
  payTime?: string
  createTime?: string
  updateTime?: string
}

export interface AdBillDetail {
  id: number
  billId: number
  adId?: number
  planId?: number
  itemName: string
  itemAmount: number
  itemCount: number
}

export interface AdBillQuery {
  billMonth?: string
  billType?: string
  advertiserId?: number
  agentId?: number
  status?: string
  page?: number
  size?: number
}

export function fetchBills(params: AdBillQuery) {
  return request.get('/bills', { params }) as Promise<ApiResponse<PageResult<AdBill>>>
}

export function fetchBillDetails(id: number) {
  return request.get(`/bills/${id}/details`) as Promise<ApiResponse<AdBillDetail[]>>
}

export function generateBills(billMonth: string) {
  return request.post('/bills/generate', { billMonth }) as Promise<ApiResponse<AdBill[]>>
}

export function confirmBill(id: number) {
  return request.post(`/bills/${id}/confirm`) as Promise<ApiResponse<AdBill>>
}

export function payBill(id: number) {
  return request.post(`/bills/${id}/pay`) as Promise<ApiResponse<AdBill>>
}
