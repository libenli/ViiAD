export const materialStatusLabels = {
  uploaded: '已上传',
  pending_review: '待审核',
  approved: '已通过',
  rejected: '已驳回',
} as const

export const planStatusLabels = {
  draft: '草稿',
  pending_schedule: '待排期',
  scheduled: '已排期',
  live: '投放中',
  paused: '已暂停',
  finished: '已结束',
} as const

export const deviceStatusLabels = {
  online: '在线',
  offline: '离线',
  fault: '故障',
} as const

export const billStatusLabels = {
  pending: '待确认',
  confirmed: '已确认',
  paid: '已支付',
} as const

export const workorderStatusLabels = {
  open: '待处理',
  processing: '处理中',
  resolved: '已完成',
} as const
