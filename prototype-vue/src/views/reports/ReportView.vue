<template>
  <section>
    <div class="page-title">
      <div>
        <h1>数据报表</h1>
        <p class="page-subtitle">支持按日期、广告和计划筛选报表，并在右侧查看聚合指标和演示结论。</p>
      </div>
    </div>

    <div class="permission-note">
      当前报表为演示级 mock 数据，重点用于说明“投放数据可量化、可复盘、可追踪”的系统价值。
    </div>

    <div class="card filter-card">
      <div class="filter-grid">
        <input v-model.trim="filters.keyword" class="input" placeholder="搜索广告 ID / 计划 ID / 日期" />
        <input v-model.trim="filters.date" class="input" placeholder="按日期筛选，例如 2026-04-10" />
        <select v-model="filters.adId" class="select">
          <option value="">全部广告</option>
          <option v-for="ad in adOptions" :key="ad" :value="ad">{{ ad }}</option>
        </select>
        <select v-model="filters.planId" class="select">
          <option value="">全部计划</option>
          <option v-for="plan in planOptions" :key="plan" :value="plan">{{ plan }}</option>
        </select>
      </div>
    </div>

    <div class="card-grid">
      <div class="card">
        <div>总曝光</div>
        <div class="metric-value">{{ totals.impressions }}</div>
      </div>
      <div class="card">
        <div>总点击</div>
        <div class="metric-value">{{ totals.clicks }}</div>
      </div>
      <div class="card">
        <div>总转化</div>
        <div class="metric-value">{{ totals.conversions }}</div>
      </div>
      <div class="card">
        <div>转化金额</div>
        <div class="metric-value">{{ totals.amount }}</div>
      </div>
    </div>

    <div v-if="filteredReports.length === 0" class="empty-state">
      当前筛选条件下没有报表记录，请调整广告、计划或日期条件。
    </div>

    <div v-else class="content-columns">
      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>日期</th>
              <th>广告 ID</th>
              <th>曝光</th>
              <th>点击</th>
              <th>互动</th>
              <th>转化</th>
              <th>金额</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in filteredReports"
              :key="row.id"
              :class="{ selected: selectedReport?.id === row.id }"
              @click="selectedReportId = row.id"
            >
              <td>{{ row.date }}</td>
              <td>{{ row.adId }}</td>
              <td>{{ row.impressions }}</td>
              <td>{{ row.clicks }}</td>
              <td>{{ row.interactions }}</td>
              <td>{{ row.conversions }}</td>
              <td>{{ row.amount }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card drawer-card">
        <template v-if="selectedReport">
          <div class="drawer-head">
            <div>
              <h3>{{ selectedReport.adId }}</h3>
              <p class="muted">{{ selectedReport.date }} / {{ selectedReport.planId }}</p>
            </div>
          </div>

          <table class="table compact">
            <tbody>
              <tr><th>曝光</th><td>{{ selectedReport.impressions }}</td></tr>
              <tr><th>点击</th><td>{{ selectedReport.clicks }}</td></tr>
              <tr><th>互动</th><td>{{ selectedReport.interactions }}</td></tr>
              <tr><th>转化</th><td>{{ selectedReport.conversions }}</td></tr>
              <tr><th>金额</th><td>{{ selectedReport.amount }}</td></tr>
            </tbody>
          </table>

          <div class="editor-panel">
            <h4>演示解读</h4>
            <p class="muted">
              该记录可用于展示：广告不是只“播了没有”，而是能继续追踪曝光、点击、互动和转化金额。
            </p>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useWorkflowStore } from '@/stores/workflow'

const workflow = useWorkflowStore()

const filters = reactive({
  keyword: '',
  date: '',
  adId: '',
  planId: ''
})

const selectedReportId = ref('')

const adOptions = Array.from(new Set(workflow.reports.map((item) => item.adId)))
const planOptions = Array.from(new Set(workflow.reports.map((item) => item.planId)))

const filteredReports = computed(() =>
  workflow.reports.filter((item) => {
    const keyword = filters.keyword.toLowerCase()
    const hitKeyword =
      !keyword ||
      item.adId.toLowerCase().includes(keyword) ||
      item.planId.toLowerCase().includes(keyword) ||
      item.date.toLowerCase().includes(keyword)
    const hitDate = !filters.date || item.date.includes(filters.date)
    const hitAd = !filters.adId || item.adId === filters.adId
    const hitPlan = !filters.planId || item.planId === filters.planId
    return hitKeyword && hitDate && hitAd && hitPlan
  })
)

const selectedReport = computed(
  () => filteredReports.value.find((item) => item.id === selectedReportId.value) ?? filteredReports.value[0]
)

const totals = computed(() =>
  filteredReports.value.reduce(
    (acc, item) => {
      acc.impressions += item.impressions
      acc.clicks += item.clicks
      acc.conversions += item.conversions
      acc.amount += item.amount
      return acc
    },
    { impressions: 0, clicks: 0, conversions: 0, amount: 0 }
  )
)

watch(filteredReports, (list) => {
  if (!list.length) {
    selectedReportId.value = ''
    return
  }
  if (!list.find((item) => item.id === selectedReportId.value)) {
    selectedReportId.value = list[0].id
  }
}, { immediate: true })
</script>
