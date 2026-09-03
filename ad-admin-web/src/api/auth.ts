import { request, type ApiResponse } from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
  captchaUuid?: string
  captchaCode?: string
  locale?: string
}

export interface LoginResult {
  needRoleSelect?: boolean
  tempToken?: string
  roles?: AuthRoleOption[]
  token?: string
  userId: number
  username: string
  realName: string
  activeRoleId?: number
  activeRoleCode?: string
  activeRoleName?: string
}

export interface AuthRoleOption {
  roleId: number
  roleCode: string
  roleName: string
}

export interface CaptchaResult {
  uuid: string
  image: string
}

export interface SendVerificationCodeParams {
  type: 'phone' | 'email'
  countryCode?: string
  target: string
  scene?: string
  locale?: string
}

export interface CodeLoginParams {
  type: 'phone' | 'email'
  countryCode?: string
  target: string
  code: string
  locale?: string
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
  activeRoleId?: number
  activeRoleCode?: string
  activeRoleName?: string
  permissions: string[]
  menus: string[]
  dataScope: string
}

export interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
}

export interface ForgotPasswordCodeParams {
  type: 'phone' | 'email'
  countryCode?: string
  target: string
  locale?: string
}

export interface ForgotPasswordValidateParams extends ForgotPasswordCodeParams {
  code: string
}

export interface ForgotPasswordValidateResult {
  resetToken: string
}

export interface ForgotPasswordResetParams {
  resetToken: string
  password: string
}

export function login(data: LoginParams) {
  return request.post('/auth/login', data) as Promise<ApiResponse<LoginResult>>
}

export function passwordLogin(data: LoginParams) {
  return request.post('/auth/login/password', data) as Promise<ApiResponse<LoginResult>>
}

export function fetchCaptcha() {
  return request.get('/auth/captcha') as Promise<ApiResponse<CaptchaResult>>
}

export function sendVerificationCode(data: SendVerificationCodeParams) {
  return request.post('/auth/verification-code/send', data) as Promise<ApiResponse<void>>
}

export function codeLogin(data: CodeLoginParams) {
  return request.post('/auth/login/code', data) as Promise<ApiResponse<LoginResult>>
}

export function selectRole(data: { tempToken: string; roleId: number }) {
  return request.post('/auth/select-role', data) as Promise<ApiResponse<LoginResult>>
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

export function sendForgotPasswordCode(data: ForgotPasswordCodeParams) {
  return request.post('/auth/forgot-password/code', data) as Promise<ApiResponse<void>>
}

export function validateForgotPasswordCode(data: ForgotPasswordValidateParams) {
  return request.post('/auth/forgot-password/validate', data) as Promise<ApiResponse<ForgotPasswordValidateResult>>
}

export function resetForgotPassword(data: ForgotPasswordResetParams) {
  return request.post('/auth/forgot-password/reset', data) as Promise<ApiResponse<void>>
}
