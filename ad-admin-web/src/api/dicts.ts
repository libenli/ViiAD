import { request, type ApiResponse } from '@/utils/request'

export interface DictOption {
  label: string
  value: string
  type?: string
}

export type DictMap = Record<string, DictOption[]>

export function fetchDicts() {
  return request.get('/system/dicts') as Promise<ApiResponse<DictMap>>
}

export function fetchDictItems(code: string) {
  return request.get(`/system/dicts/${code}`) as Promise<ApiResponse<DictOption[]>>
}
