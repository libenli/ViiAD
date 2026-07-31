<template>
  <section>
    <div class="page-title">
      <div>
        <h1>{{ dashboardTitle }}</h1>
        <p class="page-subtitle">{{ subtitle }}</p>
      </div>
    </div>

    <div class="card-grid">
      <div class="card" v-for="metric in metrics" :key="metric.label">
        <div>{{ metric.label }}</div>
        <div class="metric-value">{{ metric.value }}</div>
        <div class="muted" style="margin-top: 8px">{{ metric.hint }}</div>
      </div>
    </div>

    <div class="content-columns">
      <div class="card">
        <h3>{{ primaryTitle }}</h3>
        <table class="table">
          <thead>
            <tr>
              <th>{{ primaryColumns[0] }}</th>
              <th>{{ primaryColumns[1] }}</th>
              <th>{{ primaryColumns[2] }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in primaryRows" :key="row.id">
              <td>{{ row.col1 }}</td>
              <td>{{ row.col2 }}</td>
              <td>{{ row.col3 }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>推荐演示流程</h3>
        <ol class="demo-flow">
          <li>广告主创建广告</li>
          <li>广告主上传素材</li>
          <li>审核员审核通过素材</li>
          <li>业务运营创建并上线计划</li>
          <li>运维查看设备和大屏播放</li>
          <li>财务查看账单</li>
          <li>审计查看日志追溯</li>
        </ol>

        <h3 style="margin-top: 20px">快捷入口</h3>
        <div class="quick-links">
          <RouterLink
            v-for="link in quickLinks"
            :key="link.route"
            :to="link.route"
            class="quick-link"
          >
            {{ link.label }}
          </RouterLink>
        </div>

        <h3 style="margin-top: 20px">最近日志</h3>
        <ul class="log-list">
          <li v-for="item in recentLogs" :key="item.id">
            <strong>{{ item.action }}</strong>
            <div class="muted">{{ item.detail || item.targetId }}</div>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuditStore } from '@/stores/audit'
import { roleLabels } from '@/config/roles'
import { menuMap } from '@/config/menus'

const authStore = useAuthStore()
const workflow = useWorkflowStore()
const auditStore = useAuditStore()

const dashboardTitle = computed(() =>
  authStore.role ? `${roleLabels[authStore.role]}工作台` : '工作台'
)

const subtitle = computed(() => {
  const role = authStore.role
  if (role === 'reviewer') return '这里主要看待审核素材、审核结果和审核动作。'
  if (role === 'ops') return '这里主要看设备状态、故障工单和大屏播放情况。'
  if (role === 'finance') return '这里主要看账单确认、支付状态和金额汇总。'
  if (role === 'audit') return '这里主要看全流程日志、敏感动作和异常追踪。'
  if (role === 'advertiser' || role === 'agent') return '这里主要看广告、素材、计划和投放结果。'
  return '这里汇总了演示过程中的广告主线、状态流转和当前待办事项。'
})

const recentLogs = computed(() => auditStore.logs.slice(0, 5))
const quickLinks = computed(() =>
  authStore.role ? menuMap[authStore.role].slice(0, 4) : []
)

const metrics = computed(() => {
  const role = authStore.role
  const userId = authStore.currentUser?.id

  if (role === 'reviewer') {
    return [
      { label: '待审核素材', value: workflow.pendingMaterials.length, hint: '进入素材页可直接处理' },
      { label: '已通过素材', value: workflow.approvedMaterials.length, hint: '可用于创建投放计划' },
      { label: '已驳回素材', value: workflow.materials.filter((item) => item.status === 'rejected').length, hint: '等待重新提交' },
      { label: '审核日志', value: auditStore.logs.filter((item) => item.actorRole === 'reviewer').length, hint: '便于演示审计链路' }
    ]
  }

  if (role === 'ops') {
    return [
      { label: '在线设备', value: workflow.devices.filter((item) => item.status === 'online').length, hint: '当前可正常播放' },
      { label: '故障设备', value: workflow.devices.filter((item) => item.status === 'fault').length, hint: '优先查看异常' },
      { label: '投放中计划', value: workflow.livePlans.length, hint: '对应大屏播放内容' },
      { label: '待处理工单', value: workflow.workorders.filter((item) => item.status === 'open').length, hint: '运维主待办' }
    ]
  }

  if (role === 'finance') {
    return [
      { label: '待确认账单', value: workflow.bills.filter((item) => item.status === 'pending').length, hint: '建议先确认后支付' },
      { label: '已确认账单', value: workflow.bills.filter((item) => item.status === 'confirmed').length, hint: '可继续支付' },
      { label: '已支付账单', value: workflow.bills.filter((item) => item.status === 'paid').length, hint: '用于演示结算闭环' },
      { label: '账单总额', value: workflow.bills.reduce((sum, item) => sum + item.amount, 0), hint: '演示级金额汇总' }
    ]
  }

  if (role === 'audit') {
    return [
      { label: '日志总量', value: auditStore.logs.length, hint: '覆盖主线关键动作' },
      { label: '广告动作', value: auditStore.logs.filter((item) => item.targetType === 'ad').length, hint: '广告创建与提交' },
      { label: '素材动作', value: auditStore.logs.filter((item) => item.targetType === 'material').length, hint: '审核链路留痕' },
      { label: '计划动作', value: auditStore.logs.filter((item) => item.targetType === 'plan').length, hint: '排期与上线轨迹' }
    ]
  }

  if (role === 'advertiser' || role === 'agent') {
    const myAds = workflow.ads.filter((item) =>
      role === 'advertiser' ? item.advertiserId === userId : item.agentId === userId
    )
    const adIds = myAds.map((item) => item.id)

    return [
      { label: '我的广告', value: myAds.length, hint: '包含草稿和投放中' },
      { label: '我的素材', value: workflow.materials.filter((item) => adIds.includes(item.adId)).length, hint: '可继续送审' },
      { label: '我的计划', value: workflow.plans.filter((item) => adIds.includes(item.adId)).length, hint: '排期与上线进度' },
      { label: '投放中广告', value: myAds.filter((item) => item.status === 'live').length, hint: '可在报表里看结果' }
    ]
  }

  return [
    { label: '广告总数', value: workflow.ads.length, hint: '所有广告需求入口' },
    { label: '待审核素材', value: workflow.pendingMaterials.length, hint: '素材审核主队列' },
    { label: '投放中计划', value: workflow.livePlans.length, hint: '当前生效的投放计划' },
    {
      label: '待办事项',
      value:
        workflow.pendingMaterials.length +
        workflow.workorders.filter((item) => item.status === 'open').length +
        workflow.bills.filter((item) => item.status === 'pending').length,
      hint: '审核、工单、账单综合待办'
    }
  ]
})

const primaryTitle = computed(() => {
  const role = authStore.role
  if (role === 'reviewer') return '待审核素材队列'
  if (role === 'ops') return '设备与故障队列'
  if (role === 'finance') return '账单处理队列'
  if (role === 'audit') return '最近审计动作'
  if (role === 'advertiser' || role === 'agent') return '我的广告进展'
  return '主线流程检查点'
})

const primaryColumns = computed(() => {
  const role = authStore.role
  if (role === 'reviewer') return ['素材', '状态', '说明']
  if (role === 'ops') return ['设备', '状态', '当前广告']
  if (role === 'finance') return ['账单', '状态', '金额']
  if (role === 'audit') return ['动作', '对象', '时间']
  if (role === 'advertiser' || role === 'agent') return ['广告', '状态', '计划']
  return ['节点', '当前值', '说明']
})

const primaryRows = computed(() => {
  const role = authStore.role
  const userId = authStore.currentUser?.id

  if (role === 'reviewer') {
    return workflow.pendingMaterials.slice(0, 5).map((item) => ({
      id: item.id,
      col1: item.name,
      col2: '待审核',
      col3: item.description || item.adId
    }))
  }

  if (role === 'ops') {
    return workflow.devices.slice(0, 5).map((item) => ({
      id: item.id,
      col1: item.code,
      col2: item.status,
      col3: item.currentAdId || '-'
    }))
  }

  if (role === 'finance') {
    return workflow.bills.slice(0, 5).map((item) => ({
      id: item.id,
      col1: item.id,
      col2: item.status,
      col3: String(item.amount)
    }))
  }

  if (role === 'audit') {
    return auditStore.logs.slice(0, 5).map((item) => ({
      id: item.id,
      col1: item.action,
      col2: `${item.targetType} / ${item.targetId}`,
      col3: item.time
    }))
  }

  if (role === 'advertiser' || role === 'agent') {
    const myAds = workflow.ads.filter((item) =>
      role === 'advertiser' ? item.advertiserId === userId : item.agentId === userId
    )
    return myAds.slice(0, 5).map((item) => ({
      id: item.id,
      col1: item.title,
      col2: item.status,
      col3: item.planId || '未创建计划'
    }))
  }

  return [
    { id: 'row-1', col1: '广告需求', col2: `${workflow.ads.length} 条`, col3: '广告主体已可提交' },
    { id: 'row-2', col1: '素材审核', col2: `${workflow.pendingMaterials.length} 条待审核`, col3: '审核员可直接处理' },
    { id: 'row-3', col1: '投放执行', col2: `${workflow.livePlans.length} 条投放中`, col3: '大屏页可查看播放效果' },
    { id: 'row-4', col1: '财务结算', col2: `${workflow.bills.length} 条账单`, col3: '支持确认与支付' }
  ]
})
</script>

<style scoped>
.quick-links {
  display: grid;
  gap: 10px;
}

.quick-link {
  display: block;
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(43, 200, 255, 0.08), rgba(93, 124, 255, 0.12));
  border: 1px solid rgba(120, 176, 255, 0.12);
  color: #dcecff;
  box-shadow: inset 0 1px 0 rgba(169, 220, 255, 0.04);
}

.quick-link:hover {
  background: linear-gradient(135deg, rgba(43, 200, 255, 0.12), rgba(93, 124, 255, 0.18));
}

.log-list {
  padding-left: 18px;
  color: #d9e8ff;
}

.demo-flow {
  margin: 0;
  padding-left: 20px;
  color: #d9e8ff;
}
</style>
