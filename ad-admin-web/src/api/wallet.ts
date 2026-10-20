import { request, type ApiResponse } from '@/utils/request'
import type { PageResult } from './ads'

export type WalletOwnerType = 'advertiser' | 'agent'
export type RechargePayMethod = 'bank_transfer' | 'wechat' | 'alipay'

export interface WalletAccount {
  id: number
  accountNo: string
  ownerType: WalletOwnerType
  ownerId: number
  availableBalance: number
  frozenBalance: number
  totalRecharge: number
  totalConsumed: number
  creditLimit: number
  status: string
  createTime?: string
  updateTime?: string
}

export interface RechargeOrder {
  id: number
  rechargeNo: string
  accountId: number
  ownerType: WalletOwnerType
  ownerId: number
  payMethod: RechargePayMethod
  amount: number
  paidAmount?: number
  status: string
  transactionNo?: string
  prepayId?: string
  qrCodeUrl?: string
  payMessage?: string
  voucherNo?: string
  voucherUrl?: string
  remark?: string
  auditUserId?: number
  auditTime?: string
  paidTime?: string
  createTime?: string
}


export interface RefundOrder {
  id: number
  refundNo: string
  rechargeOrderId: number
  rechargeNo: string
  accountId: number
  ownerType: WalletOwnerType
  ownerId: number
  payMethod: RechargePayMethod
  refundAmount: number
  status: string
  refundReason?: string
  auditUserId?: number
  auditTime?: string
  auditRemark?: string
  transactionNo?: string
  refundTransactionNo?: string
  refundTime?: string
  failReason?: string
  reservedAmount?: number
  gatewayRefundStatus?: string
  reconcileStatus?: string
  reconcileMessage?: string
  lastSyncTime?: string
  syncRetryCount?: number
  createTime?: string
}

export interface RefundReconcileAction {
  id: number
  refundOrderId: number
  refundNo: string
  actionType: 'sync' | 'confirm_refunded' | 'release_reservation'
  reason: string
  operatorUserId: number
  operatorName?: string
  gatewayRefundStatus?: string
  refundStatusBefore?: string
  refundStatusAfter?: string
  reconcileStatusBefore?: string
  reconcileStatusAfter?: string
  availableBalanceBefore?: number
  availableBalanceAfter?: number
  frozenBalanceBefore?: number
  frozenBalanceAfter?: number
  reservedAmountBefore?: number
  reservedAmountAfter?: number
  resultStatus: string
  resultMessage?: string
  createTime?: string
}

export interface RefundReconcileActionPayload {
  actionType: 'confirm_refunded' | 'release_reservation'
  reason: string
  refundTransactionNo?: string
}
export interface PayCallbackLog {
  id: number
  payMethod: RechargePayMethod
  notifyType: string
  rechargeNo?: string
  rechargeOrderId?: number
  transactionNo?: string
  tradeStatus?: string
  paidAmount?: number
  callbackStatus: string
  rawPayload?: string
  responseBody?: string
  failReason?: string
  processedTime?: string
  createTime?: string
}
export interface WalletTransaction {
  id: number
  transactionNo: string
  accountId: number
  ownerType: WalletOwnerType
  ownerId: number
  transactionType: string
  direction: string
  amount: number
  balanceBefore: number
  balanceAfter: number
  bizType?: string
  bizId?: number
  remark?: string
  createTime?: string
}

export interface BillingRule {
  id: number
  ruleCode: string
  ruleName: string
  materialType: string
  chargeMode: string
  unitPrice: number
  priority: number
  status: string
  remark?: string
}

export interface PlayCharge {
  id: number
  chargeNo: string
  playLogId?: number
  adId?: number
  planId?: number
  materialId?: number
  deviceId?: number
  advertiserId?: number
  agentId?: number
  accountId?: number
  ownerType: WalletOwnerType
  ownerId: number
  ruleId?: number
  chargeMode: string
  unitPrice: number
  quantity: number
  amount: number
  status: string
  failReason?: string
  createTime?: string
}

export interface WalletAccountQuery {
  ownerType?: WalletOwnerType
  ownerId?: number
  status?: string
  page?: number
  size?: number
}

export interface RechargeOrderQuery {
  ownerType?: WalletOwnerType
  ownerId?: number
  payMethod?: string
  status?: string
  rechargeNo?: string
  page?: number
  size?: number
}

export interface CreateRechargeOrderPayload {
  ownerType: WalletOwnerType
  ownerId: number
  payMethod: RechargePayMethod
  amount: number
  transactionNo?: string
  voucherNo?: string
  voucherUrl?: string
  remark?: string
}

export interface ConfirmRechargePayload {
  transactionNo?: string
  remark?: string
}

export interface RechargePaymentResult {
  order: RechargeOrder
  payMethod: RechargePayMethod
  qrCodeUrl: string
  prepayId?: string
  message?: string
}


export interface RefundOrderQuery {
  ownerType?: WalletOwnerType
  ownerId?: number
  payMethod?: string
  status?: string
  refundNo?: string
  rechargeNo?: string
  reconcileStatus?: string
  page?: number
  size?: number
}

export interface CreateRefundOrderPayload {
  rechargeOrderId: number
  refundAmount: number
  refundReason?: string
}

export interface AuditRefundOrderPayload {
  auditRemark?: string
  refundTransactionNo?: string
}

export interface PayCallbackLogQuery {
  payMethod?: string
  rechargeNo?: string
  transactionNo?: string
  callbackStatus?: string
  page?: number
  size?: number
}
export interface PlayChargeQuery {
  accountId?: number
  adId?: number
  planId?: number
  ownerType?: WalletOwnerType
  ownerId?: number
  status?: string
  page?: number
  size?: number
}

export function fetchWalletAccounts(params: WalletAccountQuery) {
  return request.get('/wallet/accounts', { params }) as Promise<ApiResponse<PageResult<WalletAccount>>>
}

export function ensureWalletAccount(ownerType: WalletOwnerType, ownerId: number) {
  return request.post('/wallet/accounts/ensure', { ownerType, ownerId }) as Promise<ApiResponse<WalletAccount>>
}

export function fetchRechargeOrders(params: RechargeOrderQuery) {
  return request.get('/wallet/recharges', { params }) as Promise<ApiResponse<PageResult<RechargeOrder>>>
}

export function createRechargeOrder(data: CreateRechargeOrderPayload) {
  return request.post('/wallet/recharges', data) as Promise<ApiResponse<RechargeOrder>>
}

export function createScanRechargePayment(data: CreateRechargeOrderPayload) {
  return request.post('/wallet/recharges/scan-pay', data) as Promise<ApiResponse<RechargePaymentResult>>
}

export function confirmRechargeOrder(id: number, data: ConfirmRechargePayload) {
  return request.post(`/wallet/recharges/${id}/confirm`, data) as Promise<ApiResponse<RechargeOrder>>
}


export function fetchRefundOrders(params: RefundOrderQuery) {
  return request.get('/wallet/refunds', { params }) as Promise<ApiResponse<PageResult<RefundOrder>>>
}

export function createRefundOrder(data: CreateRefundOrderPayload) {
  return request.post('/wallet/refunds', data) as Promise<ApiResponse<RefundOrder>>
}

export function approveRefundOrder(id: number, data: AuditRefundOrderPayload) {
  return request.post(`/wallet/refunds/${id}/approve`, data) as Promise<ApiResponse<RefundOrder>>
}

export function rejectRefundOrder(id: number, data: AuditRefundOrderPayload) {
  return request.post(`/wallet/refunds/${id}/reject`, data) as Promise<ApiResponse<RefundOrder>>
}
export function syncRefundOrder(id: number) {
  return request.post(`/wallet/refunds/${id}/sync`) as Promise<ApiResponse<RefundOrder>>
}

export function fetchRefundReconcileActions(id: number) {
  return request.get(`/wallet/refunds/${id}/reconcile-actions`) as Promise<ApiResponse<RefundReconcileAction[]>>
}

export function handleRefundReconcileAction(id: number, data: RefundReconcileActionPayload) {
  return request.post(`/wallet/refunds/${id}/reconcile-actions`, data) as Promise<ApiResponse<RefundOrder>>
}

export function fetchPayCallbackLogs(params: PayCallbackLogQuery) {
  return request.get('/wallet/pay-callback-logs', { params }) as Promise<ApiResponse<PageResult<PayCallbackLog>>>
}
export function fetchWalletTransactions(params: WalletAccountQuery & { accountId?: number; transactionType?: string }) {
  return request.get('/wallet/transactions', { params }) as Promise<ApiResponse<PageResult<WalletTransaction>>>
}

export function fetchBillingRules(params: { materialType?: string; chargeMode?: string; status?: string; page?: number; size?: number }) {
  return request.get('/wallet/billing-rules', { params }) as Promise<ApiResponse<PageResult<BillingRule>>>
}

export function fetchPlayCharges(params: PlayChargeQuery) {
  return request.get('/wallet/play-charges', { params }) as Promise<ApiResponse<PageResult<PlayCharge>>>
}
