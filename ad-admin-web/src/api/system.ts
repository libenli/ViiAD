import { request, type ApiResponse } from '@/utils/request'
import type { PageResult } from './ads'

export interface SysUser {
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  userType?: string
  advertiserId?: number
  agentId?: number
  status: string
  createTime?: string
}

export interface SysUserPayload {
  username: string
  password?: string
  realName: string
  phone?: string
  email?: string
  userType?: string
  advertiserId?: number
  agentId?: number
  status?: string
  roleIds?: number[]
}

export interface SysRole {
  id: number
  roleCode: string
  roleName: string
  status: string
  remark?: string
}

export interface SysMenu {
  id: number
  menuName: string
  menuPath: string
  permissionCode?: string
  icon?: string
  sortNo?: number
  status: string
}

export interface SysUserQuery {
  keyword?: string
  status?: string
  userType?: string
  page?: number
  size?: number
}

export interface SysLoginLog {
  id: number
  username: string
  userId?: number
  loginStatus: string
  failReason?: string
  ipAddress?: string
  userAgent?: string
  loginTime?: string
}

export interface SysLoginLogQuery {
  username?: string
  loginStatus?: string
  page?: number
  size?: number
}

export interface SysOperLog {
  id: number
  moduleName?: string
  businessType?: string
  requestUri?: string
  requestMethod?: string
  operatorName?: string
  operatorIp?: string
  requestParam?: string
  responseResult?: string
  costTime?: number
  status: number
  errorMsg?: string
  createTime?: string
}

export interface SysOperLogQuery {
  moduleName?: string
  businessType?: string
  operatorName?: string
  status?: number
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

export function fetchSystemUsers(params: SysUserQuery) {
  return request.get('/system/users', { params }) as Promise<ApiResponse<PageResult<SysUser>>>
}

export function createSystemUser(data: SysUserPayload) {
  return request.post('/system/users', data) as Promise<ApiResponse<SysUser>>
}

export function updateSystemUser(id: number, data: SysUserPayload) {
  return request.put(`/system/users/${id}`, data) as Promise<ApiResponse<SysUser>>
}

export function enableSystemUser(id: number) {
  return request.post(`/system/users/${id}/enable`) as Promise<ApiResponse<SysUser>>
}

export function disableSystemUser(id: number) {
  return request.post(`/system/users/${id}/disable`) as Promise<ApiResponse<SysUser>>
}

export function resetSystemUserPassword(id: number, password: string) {
  return request.post(`/system/users/${id}/reset-password`, { password }) as Promise<ApiResponse<SysUser>>
}

export function fetchUserRoleIds(id: number) {
  return request.get(`/system/users/${id}/roles`) as Promise<ApiResponse<number[]>>
}

export function saveUserRoles(id: number, roleIds: number[]) {
  return request.put(`/system/users/${id}/roles`, { roleIds }) as Promise<ApiResponse<void>>
}

export function fetchSystemRoles() {
  return request.get('/system/roles') as Promise<ApiResponse<SysRole[]>>
}

export function fetchSystemMenus() {
  return request.get('/system/menus') as Promise<ApiResponse<SysMenu[]>>
}

export function fetchRoleMenuIds(roleId: number) {
  return request.get(`/system/roles/${roleId}/menus`) as Promise<ApiResponse<number[]>>
}

export function saveRoleMenus(roleId: number, menuIds: number[]) {
  return request.put(`/system/roles/${roleId}/menus`, { menuIds }) as Promise<ApiResponse<void>>
}

export function fetchLoginLogs(params: SysLoginLogQuery) {
  return request.get('/system/login-logs', { params }) as Promise<ApiResponse<PageResult<SysLoginLog>>>
}

export function fetchOperLogs(params: SysOperLogQuery) {
  return request.get('/system/oper-logs', { params }) as Promise<ApiResponse<PageResult<SysOperLog>>>
}
