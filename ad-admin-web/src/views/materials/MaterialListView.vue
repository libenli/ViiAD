<template>
  <AppPage :eyebrow="locale.t('page.business')" :title="locale.t('page.materials.title')" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('material:edit')" type="primary" :icon="Plus" @click="router.push('/materials/create')">
        {{ locale.t('page.materials.upload') }}
      </el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item :label="locale.t('page.materials.keyword')">
        <el-input v-model="query.keyword" clearable :placeholder="locale.t('page.materials.keywordPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.adId')">
        <el-select v-model="query.adId" clearable filterable :loading="adLoading" :placeholder="locale.t('page.materials.adPlaceholder')" style="width: 220px">
          <el-option v-for="ad in adOptions" :key="ad.id" :label="getAdLabel(ad)" :value="ad.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.status')">
        <el-select v-model="query.status" clearable :placeholder="locale.t('page.materials.allStatus')" style="width: 150px">
          <el-option v-for="(item, key) in materialStatusMap" :key="key" :label="getStatusLabel('material', String(key), item.label)" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.type')">
        <el-select v-model="query.materialType" clearable :placeholder="locale.t('page.materials.allTypes')" style="width: 150px">
          <el-option v-for="item in materialTypeOptions" :key="item.value" :label="getMaterialTypeLabel(item.value)" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadMaterials">{{ locale.t('common.search') }}</el-button>
        <el-button :icon="Refresh" @click="resetQuery">{{ locale.t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="materialCode" :label="locale.t('page.materials.code')" min-width="170" />
      <el-table-column prop="materialName" :label="locale.t('page.materials.name')" min-width="220" />
      <el-table-column :label="locale.t('page.materials.adId')" min-width="190">
        <template #default="{ row }">
          <div class="entity-cell">
            <strong>{{ getAdName(row.adId) }}</strong>
            <span>{{ getAdCode(row.adId) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.materials.type')" width="110">
        <template #default="{ row }">{{ getMaterialTypeLabel(row.materialType) }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.materials.size')" width="130">
        <template #default="{ row }">
          {{ row.width && row.height ? `${row.width}x${row.height}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="locale.t('page.materials.duration')" width="100">
        <template #default="{ row }">{{ row.durationSeconds ? `${row.durationSeconds}s` : '-' }}</template>
      </el-table-column>
      <el-table-column :label="locale.t('page.materials.status')" width="110">
        <template #default="{ row }">
          <el-tag :type="materialStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ getStatusLabel('material', row.status, materialStatusMap[row.status]?.label || row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="locale.t('page.materials.uploadTime')" min-width="170" />
      <el-table-column :label="locale.t('common.operation')" width="190" fixed="right" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/materials/${row.id}`)">{{ locale.t('common.detail') }}</el-button>
          <el-button
            v-if="user.hasPermission('material:edit') && (row.status === 'draft' || row.status === 'rejected')"
            link
            type="primary"
            @click="router.push(`/materials/${row.id}/edit`)"
          >
            {{ locale.t('common.edit') }}
          </el-button>
          <el-button
            v-if="user.hasPermission('material:submit') && (row.status === 'draft' || row.status === 'rejected')"
            link
            type="warning"
            @click="handleSubmit(row.id)"
          >
            {{ locale.t('page.materials.submit') }}
          </el-button>
          <el-button
            v-if="user.hasPermission('material:audit') && row.status === 'pending_review'"
            link
            type="success"
            @click="handleApprove(row.id)"
          >
            {{ locale.t('page.materials.approve') }}
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
        @current-change="loadMaterials"
        @size-change="loadMaterials"
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
import { useLocaleStore } from '@/stores/locale'
import { useUserStore } from '@/stores/user'
import {
  approveMaterial,
  fetchMaterials,
  materialStatusMap,
  materialTypeOptions,
  submitMaterial,
  type AdMaterial
} from '@/api/materials'
import { fetchAds, type AdOrder } from '@/api/ads'

const router = useRouter()
const locale = useLocaleStore()
const user = useUserStore()
const loading = ref(false)
const adLoading = ref(false)
const records = ref<AdMaterial[]>([])
const adOptions = ref<AdOrder[]>([])
const total = ref(0)

const query = reactive({
  keyword: '',
  adId: undefined as number | undefined,
  status: '',
  materialType: '',
  page: 1,
  size: 10
})

const stats = computed(() => [
  { label: locale.t('page.materials.total'), value: total.value },
  { label: getStatusLabel('material', 'draft', '草稿'), value: records.value.filter((item) => item.status === 'draft').length },
  { label: getStatusLabel('material', 'pending_review', '待审核'), value: records.value.filter((item) => item.status === 'pending_review').length },
  { label: getStatusLabel('material', 'approved', '已通过'), value: records.value.filter((item) => item.status === 'approved').length }
])

function getMaterialTypeLabel(value: string) {
  return locale.t(`status.materialType.${value}`, materialTypeOptions.find((item) => item.value === value)?.label || value)
}

function getStatusLabel(group: string, value: string, fallback: string) {
  return locale.t(`status.${group}.${value}`, fallback)
}

function getAdLabel(ad: AdOrder) {
  return `${ad.adName}（${ad.adCode}）`
}

function findAd(adId?: number) {
  return adOptions.value.find((item) => item.id === adId)
}

function getAdName(adId?: number) {
  return findAd(adId)?.adName || (adId ? `#${adId}` : '-')
}

function getAdCode(adId?: number) {
  return findAd(adId)?.adCode || (adId ? `ID ${adId}` : '-')
}

async function loadAds() {
  adLoading.value = true
  try {
    const result = await fetchAds({ page: 1, size: 500 })
    adOptions.value = result.data.records
  } finally {
    adLoading.value = false
  }
}

async function loadMaterials() {
  loading.value = true
  try {
    const result = await fetchMaterials(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.adId = undefined
  query.status = ''
  query.materialType = ''
  query.page = 1
  loadMaterials()
}

async function handleSubmit(id: number) {
  await submitMaterial(id)
  ElMessage.success(locale.t('page.materials.submitted'))
  loadMaterials()
}

async function handleApprove(id: number) {
  await approveMaterial(id)
  ElMessage.success(locale.t('page.materials.approved'))
  loadMaterials()
}

onMounted(() => {
  loadAds()
  loadMaterials()
})
</script>
