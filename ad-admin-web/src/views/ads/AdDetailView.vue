<template>
  <AppPage :eyebrow="locale.t('page.business')" :title="locale.t('page.ads.detailTitle')" :stats="stats">
    <template #actions>
      <el-button @click="router.push('/ads')">{{ locale.t('common.backToList') }}</el-button>
      <el-button
        v-if="ad && user.hasPermission('ad:edit') && (ad.status === 'draft' || ad.status === 'rejected')"
        type="primary"
        @click="router.push(`/ads/${ad.id}/edit`)"
      >
        {{ locale.t('common.edit') }}
      </el-button>
      <el-button
        v-if="ad && user.hasPermission('ad:submit') && (ad.status === 'draft' || ad.status === 'rejected')"
        type="warning"
        @click="handleSubmit"
      >
        {{ locale.t('page.ads.submitReview') }}
      </el-button>
      <el-button v-if="user.hasPermission('ad:audit') && ad?.status === 'submitted'" type="success" @click="handleApprove">
        {{ locale.t('page.ads.approve') }}
      </el-button>
      <el-button v-if="user.hasPermission('ad:audit') && ad?.status === 'submitted'" type="danger" @click="handleReject">
        {{ locale.t('page.ads.reject') }}
      </el-button>
    </template>

    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else-if="ad">
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="locale.t('page.ads.code')">{{ ad.adCode }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.name')">{{ ad.adName }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.advertiser')">{{ getAdvertiserText(ad.advertiserId) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.agent')">{{ getAgentText(ad.agentId) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.type')">{{ getAdTypeLabel(ad.adType) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.objective')">{{ getObjectiveLabel(ad.objective) }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.region')">{{ ad.regionCode || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.budget')">￥{{ ad.budgetAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.status')">
          <el-tag :type="adStatusMap[ad.status]?.type || 'info'" effect="dark">
            {{ getStatusLabel(ad.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.createTime')">{{ ad.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.auditComment')" :span="2">{{ ad.auditComment || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="locale.t('page.ads.description')" :span="2">{{ ad.description || '-' }}</el-descriptions-item>
      </el-descriptions>

      <section class="preview-section">
        <div class="section-head">
          <div>
            <p class="eyebrow">{{ locale.t('page.ads.previewEyebrow') }}</p>
            <h3>{{ locale.t('page.ads.previewTitle') }}</h3>
          </div>
          <span>{{ locale.t('page.ads.previewCount').replace('{count}', String(materials.length)) }}</span>
        </div>
        <el-empty v-if="!materials.length" :description="locale.t('page.ads.previewEmpty')" />
        <div v-else class="material-preview-grid">
          <article v-for="material in materials" :key="material.id" class="material-preview-card">
            <div class="preview-box">
              <img v-if="material.materialType === 'image'" :src="material.fileUrl" :alt="material.materialName" />
              <video v-else-if="material.materialType === 'video'" :src="material.fileUrl" controls />
              <div v-else class="h5-preview-card">H5</div>
            </div>
            <div class="preview-info">
              <strong>{{ material.materialName }}</strong>
              <span>{{ getMaterialTypeLabel(material.materialType) }} / {{ material.materialCode }}</span>
              <el-link type="primary" :href="material.fileUrl" target="_blank">{{ locale.t('page.ads.openPreview') }}</el-link>
            </div>
          </article>
        </div>
      </section>
    </template>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppPage from '@/components/AppPage.vue'
import { useUserStore } from '@/stores/user'
import {
  adStatusMap,
  adTypeOptions,
  approveAd,
  fetchAdDetail,
  rejectAd,
  submitAd,
  type AdOrder
} from '@/api/ads'
import { fetchMaterials, materialTypeOptions, type AdMaterial } from '@/api/materials'
import { fetchAdvertisers, fetchAgents, type Advertiser, type Agent } from '@/api/partners'
import { useLocaleStore } from '@/stores/locale'

const route = useRoute()
const router = useRouter()
const locale = useLocaleStore()
const user = useUserStore()
const loading = ref(false)
const ad = ref<AdOrder>()
const materials = ref<AdMaterial[]>([])
const advertiserOptions = ref<Advertiser[]>([])
const agentOptions = ref<Agent[]>([])
const id = computed(() => Number(route.params.id))

const stats = computed(() => [
  { label: locale.t('page.ads.currentStatus'), value: ad.value ? getStatusLabel(ad.value.status) : '-' },
  { label: locale.t('page.ads.type'), value: ad.value ? getAdTypeLabel(ad.value.adType) : '-' },
  { label: locale.t('page.ads.budget'), value: ad.value ? `￥${ad.value.budgetAmount || 0}` : '-' },
  { label: locale.t('page.ads.advertiser'), value: getAdvertiserName(ad.value?.advertiserId) }
])

function getAdTypeLabel(value: string) {
  return locale.t(`status.adType.${value}`, adTypeOptions.find((item) => item.value === value)?.label || value)
}

function getObjectiveLabel(value?: string) {
  return value ? locale.t(`status.objective.${value}`, value) : '-'
}

function getMaterialTypeLabel(value: string) {
  return locale.t(`status.materialType.${value}`, materialTypeOptions.find((item) => item.value === value)?.label || value)
}

function getStatusLabel(value: string) {
  return locale.t(`status.ad.${value}`, adStatusMap[value]?.label || value)
}

function findAdvertiser(advertiserId?: number) {
  return advertiserOptions.value.find((item) => item.id === advertiserId)
}

function findAgent(agentId?: number) {
  return agentOptions.value.find((item) => item.id === agentId)
}

function getAdvertiserName(advertiserId?: number) {
  return findAdvertiser(advertiserId)?.advertiserName || (advertiserId ? `#${advertiserId}` : '-')
}

function getAdvertiserText(advertiserId?: number) {
  const advertiser = findAdvertiser(advertiserId)
  return advertiser ? `${advertiser.advertiserName} / ${advertiser.advertiserCode}` : (advertiserId ? `#${advertiserId}` : '-')
}

function getAgentText(agentId?: number) {
  const agent = findAgent(agentId)
  return agent ? `${agent.agentName} / ${agent.agentCode}` : (agentId ? `#${agentId}` : '-')
}

async function loadPartners() {
  try {
    const [advertisers, agents] = await Promise.all([
      fetchAdvertisers({ page: 1, size: 500 }),
      fetchAgents({ page: 1, size: 500 })
    ])
    advertiserOptions.value = advertisers.data.records
    agentOptions.value = agents.data.records
  } catch {
    advertiserOptions.value = []
    agentOptions.value = []
  }
}

async function loadDetail() {
  loading.value = true
  try {
    const result = await fetchAdDetail(id.value)
    ad.value = result.data
    const materialResult = await fetchMaterials({ adId: id.value, page: 1, size: 200 })
    materials.value = materialResult.data.records
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  await submitAd(id.value)
  ElMessage.success(locale.t('page.ads.submitted'))
  loadDetail()
}

async function handleApprove() {
  await approveAd(id.value)
  ElMessage.success(locale.t('page.ads.approved'))
  loadDetail()
}

async function handleReject() {
  const result = await ElMessageBox.prompt(locale.t('page.ads.rejectPrompt'), locale.t('page.ads.rejectTitle'), {
    confirmButtonText: locale.t('page.ads.rejectConfirm'),
    cancelButtonText: locale.t('common.cancel'),
    inputPattern: /\S+/,
    inputErrorMessage: locale.t('page.ads.rejectRequired')
  })
  await rejectAd(id.value, result.value)
  ElMessage.success(locale.t('page.ads.rejected'))
  loadDetail()
}

onMounted(() => {
  loadPartners()
  loadDetail()
})
</script>

<style scoped>
.preview-section {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid rgba(83, 229, 255, 0.14);
  border-radius: 14px;
  background: rgba(5, 18, 26, 0.68);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-head h3 {
  margin: 4px 0 0;
  color: #f4fbff;
  font-size: 18px;
}

.section-head span {
  color: rgba(213, 240, 255, 0.68);
}

.material-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.material-preview-card {
  overflow: hidden;
  border: 1px solid rgba(83, 229, 255, 0.14);
  border-radius: 12px;
  background: rgba(8, 23, 32, 0.96);
}

.preview-box {
  display: grid;
  place-items: center;
  height: 168px;
  background: rgba(3, 12, 18, 0.86);
}

.preview-box img,
.preview-box video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.h5-preview-card {
  display: grid;
  place-items: center;
  width: 92px;
  height: 92px;
  border: 1px solid rgba(57, 198, 214, 0.5);
  border-radius: 18px;
  color: #ffffff;
  background: linear-gradient(135deg, rgba(57, 198, 214, 0.32), rgba(99, 212, 144, 0.18));
  font-size: 28px;
  font-weight: 800;
}

.preview-info {
  display: grid;
  gap: 6px;
  padding: 12px;
}

.preview-info strong {
  color: #f4fbff;
}

.preview-info span {
  color: rgba(213, 240, 255, 0.62);
  font-size: 13px;
}
</style>
