<template>
  <AppPage :eyebrow="locale.t('page.business')" :title="locale.t('page.materials.detailTitle')" :stats="stats">
    <template #actions>
      <el-button @click="router.push('/materials')">{{ locale.t('common.backToList') }}</el-button>
      <el-button
        v-if="material && user.hasPermission('material:edit') && (material.status === 'draft' || material.status === 'rejected')"
        type="primary"
        @click="router.push(`/materials/${material.id}/edit`)"
      >
        {{ locale.t('common.edit') }}
      </el-button>
      <el-button
        v-if="material && user.hasPermission('material:submit') && (material.status === 'draft' || material.status === 'rejected')"
        type="warning"
        @click="handleSubmit"
      >
        {{ locale.t('page.ads.submitReview') }}
      </el-button>
      <el-button v-if="user.hasPermission('material:audit') && material?.status === 'pending_review'" type="success" @click="handleApprove">
        {{ locale.t('page.ads.approve') }}
      </el-button>
      <el-button v-if="user.hasPermission('material:audit') && material?.status === 'pending_review'" type="danger" @click="handleReject">
        {{ locale.t('page.ads.reject') }}
      </el-button>
    </template>

    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else-if="material">
      <div class="detail-grid">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="locale.t('page.materials.code')">{{ material.materialCode }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.name')">{{ material.materialName }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.adOwnerId')">{{ getAdText(material.adId) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.type')">{{ getMaterialTypeLabel(material.materialType) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.status')">
            <el-tag :type="materialStatusMap[material.status]?.type || 'info'" effect="dark">
              {{ getStatusLabel(material.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.uploader')">{{ getUserText(material.uploaderId) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.reviewUser')">{{ getUserText(material.reviewUserId) }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.size')">
            {{ material.width && material.height ? `${material.width}x${material.height}` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.videoDuration')">{{ material.durationSeconds ? `${material.durationSeconds}s` : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.fileUrl')" :span="2">
            <a :href="material.fileUrl" target="_blank" rel="noreferrer">{{ material.fileUrl }}</a>
          </el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.coverUrl')" :span="2">{{ material.coverUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="locale.t('page.materials.reviewComment')" :span="2">{{ material.reviewComment || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="preview-panel">
          <img v-if="material.materialType === 'image'" :src="material.fileUrl" :alt="locale.t('page.materials.preview')" />
          <video v-else-if="material.materialType === 'video'" :src="material.fileUrl" controls />
          <div v-else class="h5-preview">H5</div>
        </div>
      </div>
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
  approveMaterial,
  fetchMaterialDetail,
  materialStatusMap,
  materialTypeOptions,
  rejectMaterial,
  submitMaterial,
  type AdMaterial
} from '@/api/materials'
import { fetchAdDetail, type AdOrder } from '@/api/ads'
import { fetchSystemUsers, type SysUser } from '@/api/system'
import { useLocaleStore } from '@/stores/locale'

const route = useRoute()
const router = useRouter()
const locale = useLocaleStore()
const user = useUserStore()
const loading = ref(false)
const material = ref<AdMaterial>()
const ad = ref<AdOrder>()
const userOptions = ref<SysUser[]>([])
const id = computed(() => Number(route.params.id))

const stats = computed(() => [
  { label: locale.t('page.materials.currentStatus'), value: material.value ? getStatusLabel(material.value.status) : '-' },
  { label: locale.t('page.materials.type'), value: material.value ? getMaterialTypeLabel(material.value.materialType) : '-' },
  { label: locale.t('page.materials.materialOwner'), value: getAdName(material.value?.adId) },
  { label: locale.t('page.materials.reviewUser'), value: getUserName(material.value?.reviewUserId) }
])

function getMaterialTypeLabel(value: string) {
  return locale.t(`status.materialType.${value}`, materialTypeOptions.find((item) => item.value === value)?.label || value)
}

function getStatusLabel(value: string) {
  return locale.t(`status.material.${value}`, materialStatusMap[value]?.label || value)
}

function getAdName(adId?: number) {
  const current = ad.value
  if (current && current.id === adId) {
    return current.adName
  }
  return adId ? `#${adId}` : '-'
}

function getAdText(adId?: number) {
  const current = ad.value
  if (current && current.id === adId) {
    return `${current.adName} / ${current.adCode}`
  }
  return adId ? `#${adId}` : '-'
}

function findUser(userId?: number) {
  return userOptions.value.find((item) => item.id === userId)
}

function getUserName(userId?: number) {
  return findUser(userId)?.realName || (userId ? `#${userId}` : '-')
}

function getUserText(userId?: number) {
  const item = findUser(userId)
  return item ? `${item.realName} / ${item.username}` : (userId ? `#${userId}` : '-')
}

async function loadUsers() {
  try {
    const result = await fetchSystemUsers({ page: 1, size: 500 })
    userOptions.value = result.data.records
  } catch {
    userOptions.value = []
  }
}

async function loadAd(adId: number) {
  try {
    const result = await fetchAdDetail(adId)
    ad.value = result.data
  } catch {
    ad.value = undefined
  }
}

async function loadDetail() {
  loading.value = true
  try {
    const result = await fetchMaterialDetail(id.value)
    material.value = result.data
    await Promise.all([loadAd(result.data.adId), loadUsers()])
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  await submitMaterial(id.value)
  ElMessage.success(locale.t('page.materials.submitted'))
  loadDetail()
}

async function handleApprove() {
  await approveMaterial(id.value)
  ElMessage.success(locale.t('page.materials.approved'))
  loadDetail()
}

async function handleReject() {
  const result = await ElMessageBox.prompt(locale.t('page.materials.rejectPrompt'), locale.t('page.materials.rejectTitle'), {
    confirmButtonText: locale.t('page.materials.rejectConfirm'),
    cancelButtonText: locale.t('common.cancel'),
    inputPattern: /\S+/,
    inputErrorMessage: locale.t('page.materials.rejectRequired')
  })
  await rejectMaterial(id.value, result.value)
  ElMessage.success(locale.t('page.materials.rejected'))
  loadDetail()
}

onMounted(loadDetail)
</script>
