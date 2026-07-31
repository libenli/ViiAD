import type { RoleCode } from './roles'

export interface MenuItem {
  key: string
  label: string
  route: string
}

export const menuMap: Record<RoleCode, MenuItem[]> = {
  super_admin: [
    { key: 'dashboard', label: '系统概览', route: '/' },
    { key: 'accounts', label: '账号管理', route: '/accounts' },
    { key: 'ads', label: '广告管理', route: '/ads' },
    { key: 'materials', label: '素材管理', route: '/materials' },
    { key: 'plans', label: '投放计划', route: '/plans' },
    { key: 'devices', label: '设备管理', route: '/devices' },
    { key: 'workorders', label: '工单中心', route: '/workorders' },
    { key: 'reports', label: '数据报表', route: '/reports' },
    { key: 'bills', label: '账单结算', route: '/bills' },
    { key: 'logs', label: '日志审计', route: '/logs' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  developer: [
    { key: 'dashboard', label: '开发概览', route: '/' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  ops: [
    { key: 'dashboard', label: '运维概览', route: '/' },
    { key: 'devices', label: '设备管理', route: '/devices' },
    { key: 'workorders', label: '工单中心', route: '/workorders' },
    { key: 'screen-player', label: '大屏模拟', route: '/screen-player' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  area_admin: [
    { key: 'dashboard', label: '区域概览', route: '/' },
    { key: 'accounts', label: '区域账号', route: '/accounts' },
    { key: 'ads', label: '区域广告', route: '/ads' },
    { key: 'plans', label: '区域计划', route: '/plans' },
    { key: 'devices', label: '区域设备', route: '/devices' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  business: [
    { key: 'dashboard', label: '运营概览', route: '/' },
    { key: 'ads', label: '广告管理', route: '/ads' },
    { key: 'materials', label: '素材管理', route: '/materials' },
    { key: 'plans', label: '投放计划', route: '/plans' },
    { key: 'workorders', label: '工单中心', route: '/workorders' },
    { key: 'reports', label: '数据报表', route: '/reports' },
    { key: 'feedback', label: '用户反馈', route: '/feedback' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  reviewer: [
    { key: 'dashboard', label: '审核概览', route: '/' },
    { key: 'materials', label: '素材审核', route: '/materials' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  finance: [
    { key: 'dashboard', label: '财务概览', route: '/' },
    { key: 'bills', label: '账单结算', route: '/bills' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  analyst: [
    { key: 'dashboard', label: '分析概览', route: '/' },
    { key: 'reports', label: '数据报表', route: '/reports' },
    { key: 'devices', label: '设备数据', route: '/devices' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  agent: [
    { key: 'dashboard', label: '代理商概览', route: '/' },
    { key: 'ads', label: '我的广告', route: '/ads' },
    { key: 'materials', label: '我的素材', route: '/materials' },
    { key: 'plans', label: '投放计划', route: '/plans' },
    { key: 'bills', label: '我的账单', route: '/bills' },
    { key: 'workorders', label: '工单管理', route: '/workorders' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  advertiser: [
    { key: 'dashboard', label: '广告主概览', route: '/' },
    { key: 'ads', label: '我的广告', route: '/ads' },
    { key: 'materials', label: '我的素材', route: '/materials' },
    { key: 'plans', label: '投放计划', route: '/plans' },
    { key: 'reports', label: '投放效果', route: '/reports' },
    { key: 'bills', label: '账户结算', route: '/bills' },
    { key: 'workorders', label: '工单管理', route: '/workorders' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  service: [
    { key: 'dashboard', label: '客服概览', route: '/' },
    { key: 'workorders', label: '工单中心', route: '/workorders' },
    { key: 'feedback', label: '咨询反馈', route: '/feedback' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  audit: [
    { key: 'dashboard', label: '审计概览', route: '/' },
    { key: 'logs', label: '日志审计', route: '/logs' },
    { key: 'reports', label: '审计报表', route: '/reports' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
  audience: [
    { key: 'dashboard', label: '首页', route: '/' },
    { key: 'ads', label: '推荐广告', route: '/ads' },
    { key: 'feedback', label: '互动反馈', route: '/feedback' },
    { key: 'profile', label: '个人中心', route: '/profile' },
  ],
}
