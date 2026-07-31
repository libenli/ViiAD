export type RoleCode =
  | 'super_admin'
  | 'developer'
  | 'ops'
  | 'area_admin'
  | 'business'
  | 'reviewer'
  | 'finance'
  | 'analyst'
  | 'agent'
  | 'advertiser'
  | 'service'
  | 'audit'
  | 'audience'

export const roleLabels: Record<RoleCode, string> = {
  super_admin: '超级管理员',
  developer: '开发工程师',
  ops: '运维工程师',
  area_admin: '区域管理员',
  business: '业务运营',
  reviewer: '广告素材审核',
  finance: '财务账号',
  analyst: '数据分析师',
  agent: '广告代理商',
  advertiser: '广告主',
  service: '客服账号',
  audit: '审计账号',
  audience: '受众账号',
}
