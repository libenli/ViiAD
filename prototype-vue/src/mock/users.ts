import type { MockUser } from '@/types/user'

export const mockUsers: MockUser[] = [
  { id: 'u1', username: 'admin', password: '123456', name: '全局管理员', role: 'super_admin', status: 'active' },
  { id: 'u2', username: 'dev01', password: '123456', name: '开发李工', role: 'developer', status: 'active' },
  { id: 'u3', username: 'ops01', password: '123456', name: '运维周工', role: 'ops', status: 'active', region: '上海' },
  { id: 'u4', username: 'area01', password: '123456', name: '华东区域管理员', role: 'area_admin', status: 'active', region: '华东' },
  { id: 'u5', username: 'biz01', password: '123456', name: '业务运营A', role: 'business', status: 'active' },
  { id: 'u6', username: 'review01', password: '123456', name: '审核专员A', role: 'reviewer', status: 'active' },
  { id: 'u7', username: 'finance01', password: '123456', name: '财务张会计', role: 'finance', status: 'active' },
  { id: 'u8', username: 'analyst01', password: '123456', name: '数据分析师A', role: 'analyst', status: 'active' },
  { id: 'u9', username: 'agent01', password: '123456', name: '代理商甲', role: 'agent', status: 'active', companyId: 'c-agent-1' },
  { id: 'u10', username: 'advertiser01', password: '123456', name: '广告主甲', role: 'advertiser', status: 'active', companyId: 'c-adv-1' },
  { id: 'u11', username: 'service01', password: '123456', name: '客服小林', role: 'service', status: 'active' },
  { id: 'u12', username: 'audit01', password: '123456', name: '审计专员A', role: 'audit', status: 'active' },
  { id: 'u13', username: 'audience01', password: '123456', name: '办公楼用户A', role: 'audience', status: 'active' }
]
