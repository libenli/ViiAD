import { request, type ApiResponse } from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  realName: string
}

export interface AuthInfoResult {
  user: {
    id: number
    username: string
    realName: string
    userType?: string
    advertiserId?: number
    agentId?: number
  }
  roles: string[]
  permissions: string[]
  menus: string[]
  dataScope: string
}

export interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
}

export function login(data: LoginParams) {
  return request.post('/auth/login', data) as Promise<ApiResponse<LoginResult>>
}

export function getUserInfo() {
  return request.get('/auth/info') as Promise<ApiResponse<AuthInfoResult>>
}

export function getUserMenus() {
  return request.get('/auth/menus') as Promise<ApiResponse<string[]>>
}

export function changePassword(data: ChangePasswordParams) {
  return request.post('/auth/change-password', data) as Promise<ApiResponse<void>>
}
