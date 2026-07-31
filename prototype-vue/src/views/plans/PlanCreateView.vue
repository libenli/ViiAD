<template>
  <section>
    <div class="page-title">
      <div>
        <h1>新建投放计划</h1>
        <p class="page-subtitle">基于已存在广告和已审核通过素材创建计划，成功后会自动进入详情页。</p>
      </div>
    </div>

    <div class="content-columns">
      <div class="card">
        <div class="form-grid">
          <label>
            <span>关联广告</span>
            <select v-model="form.adId" class="select">
              <option value="">请选择广告</option>
              <option v-for="ad in availableAds" :key="ad.id" :value="ad.id">
                {{ ad.id }} / {{ ad.title }}
              </option>
            </select>
          </label>

          <label>
            <span>投放区域</span>
            <input v-model.trim="form.region" class="input" placeholder="例如：上海徐汇" />
          </label>

          <label>
            <span>开始时间</span>
            <input v-model="form.startAt" class="input" type="datetime-local" />
          </label>

          <label>
            <span>结束时间</span>
            <input v-model="form.endAt" class="input" type="datetime-local" />
          </label>

          <label>
            <span>楼宇 ID 列表</span>
            <input v-model.trim="form.buildingIds" class="input" placeholder="building-001, building-002" />
          </label>

          <label>
            <span>设备 ID 列表</span>
            <input v-model.trim="form.deviceIds" class="input" placeholder="device-001, device-002" />
          </label>

          <label>
            <span>时段</span>
            <input v-model.trim="form.timeSlots" class="input" placeholder="09:00-12:00, 12:00-14:00" />
          </label>

          <label>
            <span>人群标签</span>
            <input v-model.trim="form.audienceTags" class="input" placeholder="白领, 午餐, 下午茶" />
          </label>

          <label style="grid-column: 1 / -1">
            <span>已通过素材</span>
            <div class="checkbox-grid">
              <label v-for="item in approvedMaterialsForAd" :key="item.id" class="check-item">
                <input v-model="form.materialIds" :value="item.id" type="checkbox" />
                <span>{{ item.id }} / {{ item.name }}</span>
              </label>
            </div>
          </label>
        </div>

        <label class="switch-row">
          <input v-model="form.lbsEnabled" type="checkbox" />
          <span>启用 LBS 定向</span>
        </label>

        <p v-if="error" class="error-text">{{ error }}</p>

        <div class="toolbar-group" style="margin-top: 16px">
          <button class="btn" :disabled="isSubmitting" @click="submitPlan">
            {{ isSubmitting ? '创建中...' : '创建计划' }}
          </button>
        </div>
      </div>

      <div class="card">
        <h3>创建规则</h3>
        <ul>
          <li>只有当前广告下“已审核通过”的素材才能加入计划。</li>
          <li>计划创建后默认进入“待排期”状态，后续可在列表或详情页继续操作。</li>
          <li>创建成功后建议直接在详情页核对设备范围、时段和人群定向。</li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuthStore } from '@/stores/auth'
import { useAuditStore } from '@/stores/audit'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const appStore = useAppStore()

const form = reactive({
  adId: '',
  region: '',
  buildingIds: '',
  deviceIds: '',
  startAt: '',
  endAt: '',
  timeSlots: '',
  audienceTags: '',
  materialIds: [] as string[],
  lbsEnabled: true
})

const error = ref('')
const isSubmitting = ref(false)

const availableAds = computed(() => {
  const role = authStore.role
  const userId = authStore.currentUser?.id

  if (role === 'advertiser') {
    return workflow.ads.filter((item) => item.advertiserId === userId)
  }

  if (role === 'agent') {
    return workflow.ads.filter((item) => item.agentId === userId)
  }

  return workflow.ads
})

const approvedMaterialsForAd = computed(() =>
  workflow.materials.filter((item) => item.adId === form.adId && item.status === 'approved')
)

function parseList(input: string) {
  return input
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

async function submitPlan() {
  if (isSubmitting.value) return

  error.value = ''

  if (!authStore.currentUser) {
    appStore.showToast({
      type: 'warning',
      title: '计划未能创建',
      detail: '当前登录状态异常，请重新登录后再试。'
    })
    return
  }

  if (!form.adId || !form.region || !form.startAt || !form.endAt) {
    error.value = '请先填写广告、区域和起止时间。'
  } else if (form.materialIds.length === 0) {
    error.value = '请至少选择一个已审核通过的素材。'
  }

  if (error.value) {
    appStore.showToast({
      type: 'warning',
      title: '计划未能创建',
      detail: error.value
    })
    return
  }

  try {
    isSubmitting.value = true

    const plan = workflow.createPlan({
      adId: form.adId,
      materialIds: form.materialIds,
      region: form.region,
      buildingIds: parseList(form.buildingIds),
      deviceIds: parseList(form.deviceIds),
      startAt: form.startAt,
      endAt: form.endAt,
      targeting: {
        timeSlots: parseList(form.timeSlots),
        audienceTags: parseList(form.audienceTags),
        lbsEnabled: form.lbsEnabled
      },
      operatorId: authStore.currentUser.id
    })

    auditStore.addLog({
      actorId: authStore.currentUser.id,
      actorRole: authStore.currentUser.role,
      action: 'create_plan',
      targetType: 'plan',
      targetId: plan.id,
      detail: `创建投放计划：${plan.id}`
    })

    appStore.showToast({
      type: 'success',
      title: '投放计划已创建',
      detail: `计划 ${plan.id} 已生成，正在进入详情页。`
    })

    await router.push(`/plans/${plan.id}`)
  } catch (err) {
    appStore.showToast({
      type: 'error',
      title: '计划创建失败',
      detail: err instanceof Error ? err.message : '创建计划时发生未知错误。'
    })
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

label span {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #425466;
}

.checkbox-grid {
  display: grid;
  gap: 8px;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.switch-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}

.error-text {
  color: #cc3d3d;
}
</style>
