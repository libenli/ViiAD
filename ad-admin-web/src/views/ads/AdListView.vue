<template>
  <AppPage eyebrow="广告业务" title="广告列表" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('ad:edit')" type="primary" :icon="Plus" @click="router.push('/ads/create')">
        新建广告
      </el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="广告名称 / 编号" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 150px">
          <el-option v-for="(item, key) in adStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.adType" clearable placeholder="全部类型" style="width: 150px">
          <el-option v-for="item in adTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="区域">
        <el-input v-model="query.regionCode" clearable placeholder="如 华东 / 上海" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadAds">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="adCode" label="广告编号" min-width="170" />
      <el-table-column prop="adName" label="广告名称" min-width="220" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ getAdTypeLabel(row.adType) }}</template>
      </el-table-column>
      <el-table-column prop="regionCode" label="区域" width="130" />
      <el-table-column prop="budgetAmount" label="预算" width="130">
        <template #default="{ row }">￥{{ row.budgetAmount || 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="adStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ adStatusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" min-width="210" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/ads/${row.id}`)">详情</el-button>
          <el-button
            v-if="user.hasPermission('ad:edit') && (row.status === 'draft' || row.status === 'rejected')"
            link
            type="primary"
            @click="router.push(`/ads/${row.id}/edit`)"
          >
            编辑
          </el-button>
          <el-button
            v-if="user.hasPermission('ad:submit') && (row.status === 'draft' || row.status === 'rejected')"
            link
            type="warning"
            @click="handleSubmit(row.id)"
          >
            提交
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadAds"
        @size-change="loadAds"
      />
    </div>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { adStatusMap, adTypeOptions, fetchAds, submitAd, type AdOrder } from '@/api/ads'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const records = ref<AdOrder[]>([])
const total = ref(0)

const query = reactive({
  keyword: '',
  status: '',
  adType: '',
  regionCode: '',
  page: 1,
  size: 10
})

const stats = computed(() => [
  { label: '全部广告', value: total.value },
  { label: '草稿', value: records.value.filter((item) => item.status === 'draft').length },
  { label: '待审核', value: records.value.filter((item) => item.status === 'submitted').length },
  { label: '已通过', value: records.value.filter((item) => item.status === 'approved').length }
])

function getAdTypeLabel(value: string) {
  return adTypeOptions.find((item) => item.value === value)?.label || value
}

async function loadAds() {
  loading.value = true
  try {
    const result = await fetchAds(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.status = ''
  query.adType = ''
  query.regionCode = ''
  query.page = 1
  loadAds()
}

async function handleSubmit(id: number) {
  await submitAd(id)
  ElMessage.success('已提交审核')
  loadAds()
}

onMounted(loadAds)
</script>
