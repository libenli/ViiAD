export interface AppMenuItem {
  title: string
  titleKey: string
  path: string
  icon: string
}

export const appMenus: AppMenuItem[] = [
  { title: '工作台', titleKey: 'menu.dashboard', path: '/dashboard', icon: 'DataBoard' },
  { title: '用户管理', titleKey: 'menu.users', path: '/system/users', icon: 'User' },
  { title: '权限管理', titleKey: 'menu.roles', path: '/system/roles', icon: 'Setting' },
  { title: '设备管理', titleKey: 'menu.devices', path: '/devices', icon: 'Monitor' },
  { title: '代理商', titleKey: 'menu.agents', path: '/partners/agents', icon: 'UserFilled' },
  { title: '广告主', titleKey: 'menu.advertisers', path: '/partners/advertisers', icon: 'OfficeBuilding' },
  { title: '广告列表', titleKey: 'menu.ads', path: '/ads', icon: 'Promotion' },
  { title: '素材管理', titleKey: 'menu.materials', path: '/materials', icon: 'Picture' },
  { title: '投放计划', titleKey: 'menu.plans', path: '/plans', icon: 'Calendar' },
  { title: '下发记录', titleKey: 'menu.deliveries', path: '/deliveries', icon: 'Connection' },
  { title: '工单反馈', titleKey: 'menu.workorders', path: '/workorders', icon: 'Service' },
  { title: '数据报表', titleKey: 'menu.reports', path: '/reports/overview', icon: 'TrendCharts' },
  { title: '账单结算', titleKey: 'menu.bills', path: '/finance/bills', icon: 'Wallet' },
  { title: '操作日志', titleKey: 'menu.logs', path: '/system/oper-logs', icon: 'Document' }
]
