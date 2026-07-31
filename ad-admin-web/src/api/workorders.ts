import { request, type ApiResponse } from '@/utils/request'
import type { PageResult } from './ads'
export { workOrderPriorityMap, workOrderSourceMap, workOrderStatusMap } from '@/config/status'

export interface WorkOrder {
  id: number
  workNo: string
  sourceType: string
  title: string
  content?: string
  priority: string
  status: string
  creatorId?: number
  assigneeId?: number
  relatedAdId?: number
  relatedPlanId?: number
  createTime?: string
  updateTime?: string
  closeTime?: string
}

export interface WorkOrderQuery {
  keyword?: string
  sourceType?: string
  priority?: string
  status?: string
  assigneeId?: number
  relatedPlanId?: number
  page?: number
  size?: number
}

export interface WorkOrderPayload {
  title: string
  content?: string
  priority?: string
  assigneeId?: number
  relatedAdId?: number
  relatedPlanId?: number
}

export function fetchWorkOrders(params: WorkOrderQuery) {
  return request.get('/workorders', { params }) as Promise<ApiResponse<PageResult<WorkOrder>>>
}

export function createWorkOrder(data: WorkOrderPayload) {
  return request.post('/workorders', data) as Promise<ApiResponse<WorkOrder>>
}

export function assignWorkOrder(id: number, assigneeId: number) {
  return request.post(`/workorders/${id}/assign`, { assigneeId }) as Promise<ApiResponse<WorkOrder>>
}

export function startWorkOrder(id: number) {
  return request.post(`/workorders/${id}/start`) as Promise<ApiResponse<WorkOrder>>
}

export function closeWorkOrder(id: number, content?: string) {
  return request.post(`/workorders/${id}/close`, { content }) as Promise<ApiResponse<WorkOrder>>
}
