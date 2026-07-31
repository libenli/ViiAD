export interface AppMenuItem {
  title: string
  path: string
  icon: string
}

export const appMenus: AppMenuItem[] = [
  { title: '工作台', path: '/dashboard', icon: 'DataBoard' },
  { title: '用户管理', path: '/system/users', icon: 'User' },
  { title: '权限管理', path: '/system/roles', icon: 'Setting' },
  { title: '设备管理', path: '/devices', icon: 'Monitor' },
  { title: '代理商', path: '/partners/agents', icon: 'UserFilled' },
  { title: '广告主', path: '/partners/advertisers', icon: 'OfficeBuilding' },
  { title: '广告列表', path: '/ads', icon: 'Promotion' },
  { title: '素材管理', path: '/materials', icon: 'Picture' },
  { title: '投放计划', path: '/plans', icon: 'Calendar' },
  { title: '下发记录', path: '/deliveries', icon: 'Connection' },
  { title: '工单反馈', path: '/workorders', icon: 'Service' },
  { title: '数据报表', path: '/reports/overview', icon: 'TrendCharts' },
  { title: '账单结算', path: '/finance/bills', icon: 'Wallet' },
  { title: '操作日志', path: '/system/oper-logs', icon: 'Document' }
]
