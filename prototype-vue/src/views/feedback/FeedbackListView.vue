<template>
  <section>
    <div class="page-title">
      <div>
        <h1>互动反馈</h1>
        <p class="page-subtitle">支持按分类、状态、广告筛选反馈，并在右侧查看反馈详情与演示结论。</p>
      </div>
    </div>

    <div class="permission-note" v-if="readonlyHint">
      {{ readonlyHint }}
    </div>

    <div class="card filter-card">
      <div class="filter-grid">
        <input v-model.trim="filters.keyword" class="input" placeholder="搜索反馈 ID / 内容 / 广告 ID" />
        <select v-model="filters.category" class="select">
          <option value="">全部分类</option>
          <option v-for="item in categoryOptions" :key="item" :value="item">{{ categoryLabels[item] }}</option>
        </select>
        <select v-model="filters.status" class="select">
          <option value="">全部状态</option>
          <option value="new">新建</option>
          <option value="processing">处理中</option>
          <option value="closed">已关闭</option>
        </select>
        <input v-model.trim="filters.adId" class="input" placeholder="按广告 ID 筛选" />
      </div>
    </div>

    <div class="card-grid">
      <div class="card">
        <div>反馈总数</div>
        <div class="metric-value">{{ filteredFeedback.length }}</div>
      </div>
      <div class="card">
        <div>新建</div>
        <div class="metric-value">{{ newCount }}</div>
      </div>
      <div class="card">
        <div>处理中</div>
        <div class="metric-value">{{ processingCount }}</div>
      </div>
      <div class="card">
        <div>已关闭</div>
        <div class="metric-value">{{ closedCount }}</div>
      </div>
    </div>

    <div v-if="filteredFeedback.length === 0" class="empty-state">
      当前筛选条件下没有反馈记录，请调整分类、状态或广告条件。
    </div>

    <div v-else class="content-columns">
      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>反馈 ID</th>
              <th>分类</th>
              <th>广告 ID</th>
              <th>内容</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in filteredFeedback"
              :key="item.id"
              :class="{ selected: selectedFeedback?.id === item.id }"
              @click="selectedFeedbackId = item.id"
            >
              <td>{{ item.id }}</td>
              <td>{{ categoryLabels[item.category] }}</td>
              <td>{{ item.adId || '-' }}</td>
              <td>{{ item.content }}</td>
              <td>{{ statusLabels[item.status] }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card drawer-card">
        <template v-if="selectedFeedback">
          <div class="drawer-head">
            <div>
              <h3>{{ selectedFeedback.id }}</h3>
              <p class="muted">{{ categoryLabels[selectedFeedback.category] }} / {{ selectedFeedback.adId || '无广告' }}</p>
            </div>
          </div>

          <table class="table compact">
            <tbody>
              <tr><th>分类</th><td>{{ categoryLabels[selectedFeedback.category] }}</td></tr>
              <tr><th>状态</th><td>{{ statusLabels[selectedFeedback.status] }}</td></tr>
              <tr><th>受众</th><td>{{ selectedFeedback.audienceId }}</td></tr>
              <tr><th>创建时间</th><td>{{ selectedFeedback.createdAt }}</td></tr>
            </tbody>
          </table>

          <div class="editor-panel">
            <h4>反馈内容</h4>
            <p class="muted">{{ selectedFeedback.content }}</p>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'

const authStore = useAuthStore()
const workflow = useWorkflowStore()

const filters = reactive({
  keyword: '',
  category: '',
  status: '',
  adId: ''
})

const selectedFeedbackId = ref('')

const categoryLabels = {
  comment: '评论',
  like: '点赞',
  favorite: '收藏',
  complaint: '投诉',
  service: '服务反馈'
} as const

const statusLabels = {
  new: '新建',
  processing: '处理中',
  closed: '已关闭'
} as const

const categoryOptions = Array.from(new Set(workflow.feedback.map((item) => item.category)))

const readonlyHint = computed(() => {
  if (authStore.role === 'audience') return '当前角色主要用于查看自己的互动记录，客服和运营角色更适合展示反馈跟进视角。'
  if (authStore.role === 'service') return '当前页面以客服跟踪反馈为主，暂未开放批量处理动作。'
  return ''
})

const filteredFeedback = computed(() =>
  workflow.feedback.filter((item) => {
    const keyword = filters.keyword.toLowerCase()
    const hitKeyword =
      !keyword ||
      item.id.toLowerCase().includes(keyword) ||
      item.content.toLowerCase().includes(keyword) ||
      (item.adId || '').toLowerCase().includes(keyword)
    const hitCategory = !filters.category || item.category === filters.category
    const hitStatus = !filters.status || item.status === filters.status
    const hitAd = !filters.adId || (item.adId || '').includes(filters.adId)
    return hitKeyword && hitCategory && hitStatus && hitAd
  })
)

const selectedFeedback = computed(
  () =>
    filteredFeedback.value.find((item) => item.id === selectedFeedbackId.value) ??
    filteredFeedback.value[0]
)

const newCount = computed(() => filteredFeedback.value.filter((item) => item.status === 'new').length)
const processingCount = computed(() => filteredFeedback.value.filter((item) => item.status === 'processing').length)
const closedCount = computed(() => filteredFeedback.value.filter((item) => item.status === 'closed').length)

watch(filteredFeedback, (list) => {
  if (!list.length) {
    selectedFeedbackId.value = ''
    return
  }
  if (!list.find((item) => item.id === selectedFeedbackId.value)) {
    selectedFeedbackId.value = list[0].id
  }
}, { immediate: true })
</script>
