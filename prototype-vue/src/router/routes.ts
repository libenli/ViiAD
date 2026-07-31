import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '工作台' } },
      { path: 'accounts', name: 'accounts', component: () => import('@/views/accounts/AccountListView.vue'), meta: { title: '账号管理', roles: ['super_admin', 'area_admin'] } },
      { path: 'ads', name: 'ads', component: () => import('@/views/ads/AdListView.vue'), meta: { title: '广告管理' } },
      { path: 'ads/new', name: 'ad-create', component: () => import('@/views/ads/AdCreateView.vue'), meta: { title: '新建广告', roles: ['advertiser', 'agent', 'business'] } },
      { path: 'ads/:id', name: 'ad-detail', component: () => import('@/views/ads/AdDetailView.vue'), meta: { title: '广告详情' } },
      { path: 'materials', name: 'materials', component: () => import('@/views/materials/MaterialListView.vue'), meta: { title: '素材管理' } },
      { path: 'materials/:id', name: 'material-detail', component: () => import('@/views/materials/MaterialDetailView.vue'), meta: { title: '素材详情' } },
      { path: 'plans', name: 'plans', component: () => import('@/views/plans/PlanListView.vue'), meta: { title: '投放计划' } },
      { path: 'plans/new', name: 'plan-create', component: () => import('@/views/plans/PlanCreateView.vue'), meta: { title: '新建计划', roles: ['advertiser', 'agent', 'business'] } },
      { path: 'plans/:id', name: 'plan-detail', component: () => import('@/views/plans/PlanDetailView.vue'), meta: { title: '计划详情' } },
      { path: 'devices', name: 'devices', component: () => import('@/views/devices/DeviceListView.vue'), meta: { title: '设备管理' } },
      { path: 'devices/:id', name: 'device-detail', component: () => import('@/views/devices/DeviceDetailView.vue'), meta: { title: '设备详情' } },
      { path: 'workorders', name: 'workorders', component: () => import('@/views/workorders/WorkorderListView.vue'), meta: { title: '工单中心' } },
      { path: 'reports', name: 'reports', component: () => import('@/views/reports/ReportView.vue'), meta: { title: '数据报表' } },
      { path: 'bills', name: 'bills', component: () => import('@/views/bills/BillListView.vue'), meta: { title: '账单结算' } },
      { path: 'logs', name: 'logs', component: () => import('@/views/logs/LogListView.vue'), meta: { title: '日志审计', roles: ['audit', 'super_admin'] } },
      { path: 'feedback', name: 'feedback', component: () => import('@/views/feedback/FeedbackListView.vue'), meta: { title: '互动反馈' } },
      { path: 'screen-player', name: 'screen-player', component: () => import('@/views/screen/ScreenPlayerView.vue'), meta: { title: '大屏播放模拟', roles: ['ops', 'super_admin', 'area_admin'] } },
      { path: 'profile', name: 'profile', component: () => import('@/views/profile/ProfileView.vue'), meta: { title: '个人中心' } }
    ]
  },
  { path: '/demo', name: 'demo', component: () => import('@/views/demo/DemoEntranceView.vue') },
  { path: '/login', name: 'login', component: () => import('@/views/login/LoginView.vue') },
  { path: '/switch-role', name: 'switch-role', component: () => import('@/views/login/SwitchRoleView.vue') },
  { path: '/403', name: '403', component: () => import('@/views/error/ForbiddenView.vue') },
  { path: '/:pathMatch(.*)*', name: '404', component: () => import('@/views/error/NotFoundView.vue') }
]
