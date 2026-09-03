<template>
  <AppPage :eyebrow="locale.t('page.business')" :title="locale.t('page.ads.title')" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('ad:edit')" type="primary" :icon="Plus" @click="router.push('/ads/create')">
        {{ locale.t('page.ads.create') }}
      </el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.ads.keyword')">
        <el-input v-model="query.keyword" clearable :placeholder="locale.t('page.ads.keywordPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.ads.status')">
        <el-select v-model="query.status" clearable :placeholder="locale.t('page.ads.allStatus')" style="width: 150px">
          <el-option v-for="(item, key) in adStatusMap" :key="key" :label="getStatusLabel('ad', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.ads.type')">
        <el-select v-model="query.adType" clearable :placeholder="locale.t('page.ads.allTypes')" style="width: 150px">
          <el-option v-for="item in adTypeOptions" :key="item.value" :label="getAdTypeLabel(item.value)" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.ads.region')">
        <el-input v-model="query.regionCode" clearable :placeholder="locale.t('page.ads.regionPlaceholder')" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadAds">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="adCode" :label="locale.t('page.ads.code')" min-width="170" />
      <el-table-column prop="adName" :label="locale.t('page.ads.name')" min-width="220" />
      <el-table-column :label="locale.t('page.ads.type')" width="110">
        <template #default="{ row }">{{ getAdTypeLabel(row.adType) }}</template>
      </el-table-column>
      <el-table-column prop="regionCode" :label="locale.t('page.ads.region')" width="130" />
      <el-table-column prop="budgetAmount" :label="locale.t('page.ads.budget')" width="130">
        <template #default="{ row }">￥{{ row.budgetAmount || 0 }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.ads.status')" width="110">
        <template #default="{ row }">
          <el-tag :type="adStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ getStatusLabel('ad', row.status, adStatusMap[row.status]?.label || row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="locale.t('page.ads.createTime')" min-width="170" />
      <el-table-column :label="locale.t('common.operation')" width="180" fixed="right" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/ads/${row.id}`)">{{ locale.t('common.detail') }}</el-button>
          <el-button
            v-if="user.hasPermission('ad:edit') && (row.status === 'draft' || row.status === 'rejected')"
            link
            type="primary"
            @click="router.push(`/ads/${row.id}/edit`)"
          >
            {{ locale.t('common.edit') }}
          </el-button>
          <el-button
            v-if="user.hasPermission('ad:submit') && (row.status === 'draft' || row.status === 'rejected')"
            link
            type="warning"
            @click="handleSubmit(row.id)"
          >
            {{ locale.t('page.ads.submit') }}
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
import { useLocaleStore } from '@/stores/locale'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const locale = useLocaleStore()
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
  { label: locale.t('page.ads.total'), value: total.value },
  { label: getStatusLabel('ad', 'draft', '草稿'), value: records.value.filter((item) => item.status === 'draft').length },
  { label: getStatusLabel('ad', 'submitted', '待审核'), value: records.value.filter((item) => item.status === 'submitted').length },
  { label: getStatusLabel('ad', 'approved', '已通过'), value: records.value.filter((item) => item.status === 'approved').length }
])

function getAdTypeLabel(value: string) {
  return locale.t(`status.adType.${value}`, adTypeOptions.find((item) => item.value === value)?.label || value)
}

function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
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
  ElMessage.success(locale.t('page.ads.submitted'))
  loadAds()
}

onMounted(loadAds)
</script>
