<template>
  <section>
    <div class="page-title">
      <div>
        <h1>新建广告</h1>
        <p class="page-subtitle">支持保存草稿或直接提交广告，创建成功后会自动进入详情页。</p>
      </div>
    </div>

    <div class="content-columns">
      <div class="card">
        <div class="form-grid">
          <label>
            <span>广告标题</span>
            <input v-model.trim="form.title" class="input" placeholder="例如：写字楼午餐轻食推广" />
          </label>

          <label>
            <span>投放目标</span>
            <select v-model="form.objective" class="select">
              <option value="exposure">品牌曝光</option>
              <option value="conversion">转化成交</option>
              <option value="engagement">互动引流</option>
            </select>
          </label>

          <label>
            <span>广告类型</span>
            <select v-model="form.type" class="select">
              <option value="image">图片广告</option>
              <option value="video">视频广告</option>
              <option value="interactive">互动广告</option>
            </select>
          </label>

          <label>
            <span>投放区域</span>
            <input v-model.trim="form.region" class="input" placeholder="例如：上海徐汇" />
          </label>

          <label>
            <span>预算</span>
            <input v-model.number="form.budget" class="input" type="number" min="1" placeholder="10000" />
          </label>

          <label v-if="showAdvertiserSelector">
            <span>归属广告主</span>
            <select v-model="form.advertiserId" class="select">
              <option value="">请选择广告主</option>
              <option v-for="item in advertiserOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </option>
            </select>
          </label>

          <label v-if="showAgentSelector">
            <span>代理商</span>
            <select v-model="form.agentId" class="select">
              <option value="">无</option>
              <option v-for="item in agentOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </option>
            </select>
          </label>
        </div>

        <label class="block-label">
          <span>广告说明</span>
          <textarea
            v-model.trim="form.description"
            class="textarea"
            placeholder="补充业务背景、受众特点、转化目标等"
          />
        </label>

        <p v-if="error" class="error-text">{{ error }}</p>

        <div class="toolbar-group" style="margin-top: 16px">
          <button
            class="btn secondary"
            :disabled="isSubmitting"
            @click="submit('draft')"
          >
            {{ isSubmitting && submittingAction === 'draft' ? '保存中...' : '保存草稿' }}
          </button>
          <button
            class="btn"
            :disabled="isSubmitting"
            @click="submit('submitted')"
          >
            {{ isSubmitting && submittingAction === 'submitted' ? '提交中...' : '提交广告' }}
          </button>
        </div>
      </div>

      <div class="card">
        <h3>填写建议</h3>
        <ul>
          <li>标题建议直接体现推广场景和卖点，便于后续素材制作和计划排期。</li>
          <li>预算和区域是后续计划创建的重要字段，建议一次填完整。</li>
          <li>创建成功后可继续前往素材页上传素材，再进入计划页完成投放配置。</li>
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
import { mockUsers } from '@/mock/users'

const router = useRouter()
const workflow = useWorkflowStore()
const authStore = useAuthStore()
const auditStore = useAuditStore()
const appStore = useAppStore()

const advertiserOptions = mockUsers.filter((item) => item.role === 'advertiser')
const agentOptions = mockUsers.filter((item) => item.role === 'agent')

const showAdvertiserSelector = computed(() =>
  ['business', 'agent', 'super_admin'].includes(authStore.role ?? '')
)
const showAgentSelector = computed(() =>
  ['business', 'super_admin'].includes(authStore.role ?? '')
)

const form = reactive({
  title: '',
  description: '',
  advertiserId: authStore.role === 'advertiser' ? authStore.currentUser?.id ?? '' : '',
  agentId: authStore.role === 'agent' ? authStore.currentUser?.id ?? '' : '',
  type: 'image' as 'image' | 'video' | 'interactive',
  objective: 'conversion' as 'exposure' | 'conversion' | 'engagement',
  region: '',
  budget: 10000
})

const error = ref('')
const isSubmitting = ref(false)
const submittingAction = ref<null | 'draft' | 'submitted'>(null)

function validate() {
  if (!form.title || !form.region || !form.budget) {
    return '请先填写标题、投放区域和预算。'
  }

  if (!form.advertiserId) {
    return '请明确归属广告主。'
  }

  return ''
}

async function submit(status: 'draft' | 'submitted') {
  if (isSubmitting.value) return

  error.value = validate()
  if (error.value || !authStore.currentUser) {
    appStore.showToast({
      type: 'warning',
      title: '广告未能创建',
      detail: error.value || '当前登录状态异常，请重新登录后再试。'
    })
    return
  }

  try {
    isSubmitting.value = true
    submittingAction.value = status

    const ad = workflow.createAd({
      title: form.title,
      description: form.description,
      advertiserId: form.advertiserId,
      agentId: form.agentId || undefined,
      type: form.type,
      objective: form.objective,
      region: form.region,
      budget: form.budget,
      status
    })

    auditStore.addLog({
      actorId: authStore.currentUser.id,
      actorRole: authStore.currentUser.role,
      action: status === 'draft' ? 'create_ad_draft' : 'submit_ad',
      targetType: 'ad',
      targetId: ad.id,
      detail: `${status === 'draft' ? '保存广告草稿' : '提交广告'}：${ad.title}`
    })

    appStore.showToast({
      type: 'success',
      title: status === 'draft' ? '广告草稿已保存' : '广告已提交成功',
      detail: `广告 ${ad.id} 已创建，正在进入详情页。`
    })

    await router.push(`/ads/${ad.id}`)
  } catch (err) {
    appStore.showToast({
      type: 'error',
      title: '广告创建失败',
      detail: err instanceof Error ? err.message : '创建广告时发生未知错误。'
    })
  } finally {
    isSubmitting.value = false
    submittingAction.value = null
  }
}
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.block-label {
  display: block;
  margin-top: 16px;
}

label span {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #425466;
}

.error-text {
  color: #cc3d3d;
}
</style>
