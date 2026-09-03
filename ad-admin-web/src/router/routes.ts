import type { RouteRecordRaw } from 'vue-router'

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', titleKey: 'route.login' }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台', titleKey: 'route.dashboard', icon: 'DataBoard', permission: 'dashboard:view' }
      },
      {
        path: 'ads',
        name: 'AdList',
        component: () => import('@/views/ads/AdListView.vue'),
        meta: { title: '广告列表', titleKey: 'route.adList', icon: 'Promotion', permission: 'ad:view' }
      },
      {
        path: 'ads/create',
        name: 'AdCreate',
        component: () => import('@/views/ads/AdFormView.vue'),
        meta: { title: '新建广告', titleKey: 'route.adCreate', icon: 'Promotion', permission: 'ad:edit' }
      },
      {
        path: 'ads/:id',
        name: 'AdDetail',
        component: () => import('@/views/ads/AdDetailView.vue'),
        meta: { title: '广告详情', titleKey: 'route.adDetail', icon: 'Promotion', permission: 'ad:view' }
      },
      {
        path: 'ads/:id/edit',
        name: 'AdEdit',
        component: () => import('@/views/ads/AdFormView.vue'),
        meta: { title: '编辑广告', titleKey: 'route.adEdit', icon: 'Promotion', permission: 'ad:edit' }
      },
      {
        path: 'materials',
        name: 'MaterialList',
        component: () => import('@/views/materials/MaterialListView.vue'),
        meta: { title: '素材管理', titleKey: 'route.materialList', icon: 'Picture', permission: 'material:view' }
      },
      {
        path: 'materials/create',
        name: 'MaterialCreate',
        component: () => import('@/views/materials/MaterialFormView.vue'),
        meta: { title: '上传素材', titleKey: 'route.materialCreate', icon: 'Picture', permission: 'material:edit' }
      },
      {
        path: 'materials/:id',
        name: 'MaterialDetail',
        component: () => import('@/views/materials/MaterialDetailView.vue'),
        meta: { title: '素材详情', titleKey: 'route.materialDetail', icon: 'Picture', permission: 'material:view' }
      },
      {
        path: 'materials/:id/edit',
        name: 'MaterialEdit',
        component: () => import('@/views/materials/MaterialFormView.vue'),
        meta: { title: '编辑素材', titleKey: 'route.materialEdit', icon: 'Picture', permission: 'material:edit' }
      },
      {
        path: 'plans',
        name: 'PlanList',
        component: () => import('@/views/plans/PlanListView.vue'),
        meta: { title: '投放计划', titleKey: 'route.planList', icon: 'Calendar', permission: 'plan:view' }
      },
      {
        path: 'plans/create',
        name: 'PlanCreate',
        component: () => import('@/views/plans/PlanFormView.vue'),
        meta: { title: '新建投放计划', titleKey: 'route.planCreate', icon: 'Calendar', permission: 'plan:edit' }
      },
      {
        path: 'plans/:id',
        name: 'PlanDetail',
        component: () => import('@/views/plans/PlanDetailView.vue'),
        meta: { title: '投放计划详情', titleKey: 'route.planDetail', icon: 'Calendar', permission: 'plan:view' }
      },
      {
        path: 'plans/:id/edit',
        name: 'PlanEdit',
        component: () => import('@/views/plans/PlanFormView.vue'),
        meta: { title: '编辑投放计划', titleKey: 'route.planEdit', icon: 'Calendar', permission: 'plan:edit' }
      },
      {
        path: 'devices',
        name: 'DeviceList',
        component: () => import('@/views/devices/DeviceListView.vue'),
        meta: { title: '设备管理', titleKey: 'route.deviceList', icon: 'Monitor', permission: 'device:view' }
      },
      {
        path: 'devices/create',
        name: 'DeviceCreate',
        component: () => import('@/views/devices/DeviceFormView.vue'),
        meta: { title: '新建设备', titleKey: 'route.deviceCreate', icon: 'Monitor', permission: 'device:manage' }
      },
      {
        path: 'devices/:id',
        name: 'DeviceDetail',
        component: () => import('@/views/devices/DeviceDetailView.vue'),
        meta: { title: '设备详情', titleKey: 'route.deviceDetail', icon: 'Monitor', permission: 'device:view' }
      },
      {
        path: 'devices/:id/edit',
        name: 'DeviceEdit',
        component: () => import('@/views/devices/DeviceFormView.vue'),
        meta: { title: '编辑设备', titleKey: 'route.deviceEdit', icon: 'Monitor', permission: 'device:manage' }
      },
      {
        path: 'deliveries',
        name: 'DeliveryList',
        component: () => import('@/views/deliveries/DeliveryListView.vue'),
        meta: { title: '下发记录', titleKey: 'route.deliveryList', icon: 'Connection', permission: 'delivery:view' }
      },
      {
        path: 'reports/overview',
        name: 'ReportOverview',
        component: () => import('@/views/reports/ReportOverviewView.vue'),
        meta: { title: '数据报表', titleKey: 'route.reportOverview', icon: 'TrendCharts', permission: 'report:view' }
      },
      {
        path: 'finance/bills',
        name: 'BillList',
        component: () => import('@/views/finance/BillListView.vue'),
        meta: { title: '账单结算', titleKey: 'route.billList', icon: 'Wallet', permission: 'bill:view' }
      },
      {
        path: 'workorders',
        name: 'WorkOrderList',
        component: () => import('@/views/workorders/WorkOrderListView.vue'),
        meta: { title: '工单反馈', titleKey: 'route.workOrderList', icon: 'Service', permission: 'workOrder:view' }
      },
      {
        path: 'partners/advertisers',
        name: 'AdvertiserList',
        component: () => import('@/views/partners/AdvertiserListView.vue'),
        meta: { title: '广告主', titleKey: 'route.advertiserList', icon: 'OfficeBuilding', permission: 'advertiser:view' }
      },
      {
        path: 'partners/agents',
        name: 'AgentList',
        component: () => import('@/views/partners/AgentListView.vue'),
        meta: { title: '代理商', titleKey: 'route.agentList', icon: 'UserFilled', permission: 'agent:view' }
      },
      {
        path: 'system/users',
        name: 'UserList',
        component: () => import('@/views/system/UserListView.vue'),
        meta: { title: '用户管理', titleKey: 'route.userList', icon: 'User', permission: 'system:user:view' }
      }
      ,
      {
        path: 'system/roles',
        name: 'RolePermission',
        component: () => import('@/views/system/RolePermissionView.vue'),
        meta: { title: '权限管理', titleKey: 'route.rolePermission', icon: 'Setting', permission: 'system:role:view' }
      },
      {
        path: 'system/oper-logs',
        name: 'OperLog',
        component: () => import('@/views/system/OperLogView.vue'),
        meta: { title: '操作日志', titleKey: 'route.operLog', icon: 'Document', permission: 'system:log:view' }
      }
    ]
  },
  {
    path: '/401',
    name: 'Forbidden',
    component: () => import('@/views/error/ForbiddenView.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFoundView.vue')
  }
]
