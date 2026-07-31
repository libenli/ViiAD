export type TagType = 'info' | 'warning' | 'success' | 'danger' | 'primary'

export interface StatusMeta {
  label: string
  type: TagType
}

export interface OptionItem {
  label: string
  value: string
}

export const adStatusMap: Record<string, StatusMeta> = {
  draft: { label: '草稿', type: 'info' },
  submitted: { label: '待审核', type: 'warning' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已驳回', type: 'danger' },
  planning: { label: '计划中', type: 'primary' },
  delivering: { label: '投放中', type: 'success' },
  paused: { label: '已暂停', type: 'warning' },
  finished: { label: '已结束', type: 'info' }
}

export const adTypeOptions: OptionItem[] = [
  { label: '图片广告', value: 'image' },
  { label: '视频广告', value: 'video' },
  { label: '互动广告', value: 'interactive' }
]

export const objectiveOptions: OptionItem[] = [
  { label: '曝光', value: 'exposure' },
  { label: '转化', value: 'conversion' },
  { label: '互动', value: 'engagement' }
]

export const materialTypeOptions: OptionItem[] = [
  { label: '图片素材', value: 'image' },
  { label: '视频素材', value: 'video' },
  { label: 'H5素材', value: 'h5' }
]

export const materialStatusMap: Record<string, StatusMeta> = {
  draft: { label: '草稿', type: 'info' },
  pending_review: { label: '待审核', type: 'warning' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已驳回', type: 'danger' }
}

export const scheduleStatusMap: Record<string, StatusMeta> = {
  draft: { label: '草稿', type: 'info' },
  pending_schedule: { label: '待排期', type: 'warning' },
  scheduled: { label: '已排期', type: 'success' }
}

export const planDeliveryStatusMap: Record<string, StatusMeta> = {
  not_started: { label: '未开始', type: 'info' },
  live: { label: '投放中', type: 'success' },
  paused: { label: '已暂停', type: 'warning' },
  finished: { label: '已结束', type: 'primary' }
}

export const deliveryRecordStatusMap: Record<string, StatusMeta> = {
  pending: { label: '待下发', type: 'warning' },
  success: { label: '成功', type: 'success' },
  failed: { label: '失败', type: 'danger' }
}

export const deliveryTypeMap: Record<string, StatusMeta> = {
  auto: { label: '自动', type: 'primary' },
  manual: { label: '手动', type: 'info' }
}

export const onlineStatusMap: Record<string, StatusMeta> = {
  online: { label: '在线', type: 'success' },
  offline: { label: '离线', type: 'info' }
}

export const faultStatusMap: Record<string, StatusMeta> = {
  normal: { label: '正常', type: 'success' },
  fault: { label: '故障', type: 'danger' }
}

export const deviceStatusMap: Record<string, StatusMeta> = {
  active: { label: '启用', type: 'success' },
  disabled: { label: '停用', type: 'warning' }
}

export const billStatusMap: Record<string, StatusMeta> = {
  pending: { label: '待确认', type: 'warning' },
  confirmed: { label: '待支付', type: 'primary' },
  paid: { label: '已支付', type: 'success' }
}

export const billTypeMap: Record<string, string> = {
  advertiser: '广告主账单',
  agent: '代理商账单'
}

export const workOrderStatusMap: Record<string, StatusMeta> = {
  open: { label: '待处理', type: 'warning' },
  assigned: { label: '已分派', type: 'primary' },
  processing: { label: '处理中', type: 'primary' },
  closed: { label: '已关闭', type: 'success' }
}

export const workOrderPriorityMap: Record<string, StatusMeta> = {
  low: { label: '低', type: 'info' },
  normal: { label: '普通', type: 'primary' },
  high: { label: '高', type: 'danger' }
}

export const workOrderSourceMap: Record<string, string> = {
  manual: '人工创建',
  delivery: '下发异常'
}

export const partnerStatusMap: Record<string, StatusMeta> = {
  active: { label: '启用', type: 'success' },
  disabled: { label: '停用', type: 'warning' }
}

export const advertiserSourceMap: Record<string, string> = {
  platform: '平台招商',
  agent: '代理商引入',
  direct: '直客'
}

export const sourceTypeMap: Record<string, string> = {
  device: '设备上报',
  demo: '演示生成'
}

export const operLogStatusMap: Record<number, StatusMeta> = {
  1: { label: '成功', type: 'success' },
  0: { label: '失败', type: 'danger' }
}
