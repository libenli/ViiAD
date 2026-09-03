<template>
  <AppPage :eyebrow="locale.t('page.business')" :title="isEdit ? locale.t('page.materials.editTitle') : locale.t('page.materials.upload')" :stats="stats">
    <el-form ref="formRef" class="entity-form" :model="form" :rules="rules" label-width="110px">
      <el-form-item :label="locale.t('page.materials.adOwnerId')" prop="adId">
        <el-select
          v-model="form.adId"
          filterable
          :loading="adLoading"
          :placeholder="locale.t('page.materials.adPlaceholder')"
        >
          <el-option v-for="ad in adOptions" :key="ad.id" :label="getAdLabel(ad)" :value="ad.id">
            <div class="option-row">
              <span>{{ ad.adName }}</span>
              <small>{{ ad.adCode }} / {{ ad.regionCode || locale.t('page.plans.noRegion') }}</small>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.name')" prop="materialName">
        <el-input v-model="form.materialName" maxlength="80" show-word-limit :placeholder="locale.t('page.materials.materialNameRequired')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.type')" prop="materialType">
        <el-select v-model="form.materialType" :placeholder="locale.t('page.materials.materialTypeRequired')">
          <el-option v-for="item in materialTypeOptions" :key="item.value" :label="getMaterialTypeLabel(item.value)" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.file')" prop="fileUrl">
        <div class="upload-field">
          <el-alert
            class="upload-hint"
            type="info"
            show-icon
            :closable="false"
            :title="locale.t('page.materials.fileHint')"
          />
          <el-upload
            drag
            :show-file-list="false"
            :http-request="handleMaterialUpload"
            :before-upload="beforeMaterialUpload"
          >
            <div class="upload-copy">
              <strong>{{ uploadingMaterial ? locale.t('page.materials.uploading') : locale.t('page.materials.uploadCopy') }}</strong>
              <span>{{ locale.t('page.materials.uploadDesc') }}</span>
            </div>
          </el-upload>
          <el-progress
            v-if="uploadingMaterial || materialUploadProgress > 0"
            :percentage="materialUploadProgress"
            :status="materialUploadProgress === 100 ? 'success' : undefined"
          />
          <el-alert v-if="materialUploadError" type="error" show-icon :closable="false" :title="materialUploadError">
            <template #default>
              <el-button size="small" type="danger" plain @click="retryMaterialUpload">{{ locale.t('page.materials.retryUpload') }}</el-button>
            </template>
          </el-alert>
          <el-input v-model="form.fileUrl" :placeholder="locale.t('page.materials.fileUrlPlaceholder')" />
          <div v-if="form.fileUrl" class="preview-card">
            <div class="preview-head">
              <strong>{{ locale.t('page.materials.preview') }}</strong>
              <el-link :href="form.fileUrl" target="_blank" type="primary">{{ locale.t('page.materials.openNewWindow') }}</el-link>
            </div>
            <img v-if="form.materialType === 'image'" :src="form.fileUrl" :alt="locale.t('page.materials.preview')" />
            <video v-else-if="form.materialType === 'video'" :src="form.fileUrl" controls />
            <div v-else class="h5-preview">
              <span>{{ locale.t('page.materials.h5Material') }}</span>
              <small>{{ locale.t('page.materials.savedDetailHint') }}</small>
            </div>
          </div>
        </div>
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.coverUrl')">
        <div class="upload-field">
          <el-upload
            :show-file-list="false"
            :http-request="handleCoverUpload"
            :before-upload="beforeCoverUpload"
          >
            <el-button :loading="uploadingCover">{{ locale.t('page.materials.uploadCover') }}</el-button>
          </el-upload>
          <el-progress
            v-if="uploadingCover || coverUploadProgress > 0"
            :percentage="coverUploadProgress"
            :status="coverUploadProgress === 100 ? 'success' : undefined"
          />
          <el-alert v-if="coverUploadError" type="error" show-icon :closable="false" :title="coverUploadError">
            <template #default>
              <el-button size="small" type="danger" plain @click="retryCoverUpload">{{ locale.t('page.materials.retryCover') }}</el-button>
            </template>
          </el-alert>
          <el-input v-model="form.coverUrl" :placeholder="locale.t('page.materials.coverPlaceholder')" />
          <img v-if="form.coverUrl" class="cover-preview" :src="form.coverUrl" :alt="locale.t('page.materials.coverPreview')" />
        </div>
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.fileSize')">
        <el-input-number v-model="form.fileSize" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.width')">
        <el-input-number v-model="form.width" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.height')">
        <el-input-number v-model="form.height" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.materials.videoDuration')">
        <el-input-number v-model="form.durationSeconds" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item class="form-actions">
        <el-button @click="router.back()">{{ locale.t('common.back') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ locale.t('common.save') }}
        </el-button>
      </el-form-item>
    </el-form>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import AppPage from '@/components/AppPage.vue'
import { uploadFile } from '@/api/files'
import {
  createMaterial,
  fetchMaterialDetail,
  materialTypeOptions,
  updateMaterial,
  type AdMaterialPayload
} from '@/api/materials'
import { fetchAds, type AdOrder } from '@/api/ads'
import { useLocaleStore } from '@/stores/locale'

const route = useRoute()
const router = useRouter()
const locale = useLocaleStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const uploadingMaterial = ref(false)
const uploadingCover = ref(false)
const materialUploadProgress = ref(0)
const coverUploadProgress = ref(0)
const materialUploadError = ref('')
const coverUploadError = ref('')
const lastMaterialFile = ref<File>()
const lastCoverFile = ref<File>()
const adLoading = ref(false)
const adOptions = ref<AdOrder[]>([])

const id = computed(() => Number(route.params.id))
const isEdit = computed(() => Boolean(route.params.id))

const form = reactive<AdMaterialPayload>({
  adId: undefined as unknown as number,
  materialName: '',
  materialType: 'image',
  fileUrl: '',
  fileSize: undefined,
  durationSeconds: undefined,
  width: 1920,
  height: 1080,
  coverUrl: ''
})

const rules = computed<FormRules>(() => ({
  adId: [{ required: true, message: locale.t('page.materials.adIdRequired'), trigger: 'change' }],
  materialName: [{ required: true, message: locale.t('page.materials.materialNameRequired'), trigger: 'blur' }],
  materialType: [{ required: true, message: locale.t('page.materials.materialTypeRequired'), trigger: 'change' }],
  fileUrl: [{ required: true, message: locale.t('page.materials.fileRequired'), trigger: 'blur' }]
}))

const stats = computed(() => [
  { label: locale.t('page.ads.formMode'), value: isEdit.value ? locale.t('page.ads.editMode') : locale.t('page.materials.uploadMode') },
  { label: locale.t('page.ads.defaultStatus'), value: locale.t('status.material.draft') },
  { label: locale.t('page.ads.mainFlow'), value: locale.t('menu.materials') },
  { label: locale.t('page.ads.nextStep'), value: locale.t('page.materials.auditStep') }
])

function getMaterialTypeLabel(value: string) {
  return locale.t(`status.materialType.${value}`, materialTypeOptions.find((item) => item.value === value)?.label || value)
}

function getAdLabel(ad: AdOrder) {
  return `${ad.adName}（${ad.adCode}）`
}

async function loadApprovedAds() {
  adLoading.value = true
  try {
    const result = await fetchAds({ status: 'approved', page: 1, size: 200 })
    adOptions.value = result.data.records
  } finally {
    adLoading.value = false
  }
}

async function loadDetail() {
  if (!isEdit.value) return
  const result = await fetchMaterialDetail(id.value)
  Object.assign(form, {
    adId: result.data.adId,
    materialName: result.data.materialName,
    materialType: result.data.materialType,
    fileUrl: result.data.fileUrl,
    fileSize: result.data.fileSize,
    durationSeconds: result.data.durationSeconds,
    width: result.data.width,
    height: result.data.height,
    coverUrl: result.data.coverUrl
  })
}

async function handleSave() {
  if (uploadingMaterial.value || uploadingCover.value) {
    ElMessage.warning(locale.t('page.materials.uploadingWait'))
    return
  }
  await formRef.value?.validate()
  saving.value = true
  try {
    const result = isEdit.value ? await updateMaterial(id.value, form) : await createMaterial(form)
    ElMessage.success(isEdit.value ? locale.t('page.materials.updated') : locale.t('page.materials.created'))
    router.replace(`/materials/${result.data.id}`)
  } finally {
    saving.value = false
  }
}

function beforeMaterialUpload(file: File) {
  materialUploadError.value = ''
  const isSupported =
    file.type.startsWith('image/') ||
    file.type.startsWith('video/') ||
    file.name.toLowerCase().endsWith('.html') ||
    file.name.toLowerCase().endsWith('.htm') ||
    file.name.toLowerCase().endsWith('.zip')
  if (!isSupported) {
    ElMessage.warning(locale.t('page.materials.unsupportedFile'))
    return false
  }
  const maxSize = getMaterialMaxSize(file)
  if (file.size > maxSize) {
    ElMessage.warning(locale.t('page.materials.fileTooLarge').replace('{size}', formatFileSize(maxSize)))
    return false
  }
  return true
}

function beforeCoverUpload(file: File) {
  coverUploadError.value = ''
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.warning(locale.t('page.materials.coverImageOnly'))
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning(locale.t('page.materials.coverTooLarge'))
    return false
  }
  return true
}

async function handleMaterialUpload(options: UploadRequestOptions) {
  await uploadMaterialFile(options.file as File, options)
}

async function uploadMaterialFile(file: File, options?: UploadRequestOptions) {
  uploadingMaterial.value = true
  materialUploadProgress.value = 0
  materialUploadError.value = ''
  lastMaterialFile.value = file
  try {
    const result = await uploadFile(file, 'files', (percent) => {
      materialUploadProgress.value = percent
    })
    form.fileUrl = result.data.url
    form.fileSize = result.data.size
    applyMaterialType(file)
    await fillMediaMeta(file)
    ElMessage.success(locale.t('page.materials.fileUploaded'))
    options?.onSuccess?.(result)
  } catch (error) {
    materialUploadError.value = locale.t('page.materials.fileUploadFailed')
    options?.onError?.(error as never)
  } finally {
    uploadingMaterial.value = false
  }
}

async function handleCoverUpload(options: UploadRequestOptions) {
  await uploadCoverFile(options.file as File, options)
}

async function uploadCoverFile(file: File, options?: UploadRequestOptions) {
  uploadingCover.value = true
  coverUploadProgress.value = 0
  coverUploadError.value = ''
  lastCoverFile.value = file
  try {
    const result = await uploadFile(file, 'covers', (percent) => {
      coverUploadProgress.value = percent
    })
    form.coverUrl = result.data.url
    ElMessage.success(locale.t('page.materials.coverUploaded'))
    options?.onSuccess?.(result)
  } catch (error) {
    coverUploadError.value = locale.t('page.materials.coverUploadFailed')
    options?.onError?.(error as never)
  } finally {
    uploadingCover.value = false
  }
}

function retryMaterialUpload() {
  if (!lastMaterialFile.value) return
  uploadMaterialFile(lastMaterialFile.value)
}

function retryCoverUpload() {
  if (!lastCoverFile.value) return
  uploadCoverFile(lastCoverFile.value)
}

function applyMaterialType(file: File) {
  if (file.type.startsWith('image/')) {
    form.materialType = 'image'
  } else if (file.type.startsWith('video/')) {
    form.materialType = 'video'
  } else {
    form.materialType = 'h5'
  }
}

function fillMediaMeta(file: File) {
  const url = URL.createObjectURL(file)
  if (file.type.startsWith('image/')) {
    return fillImageMeta(url, true)
  }
  if (file.type.startsWith('video/')) {
    return fillVideoMeta(url, true)
  }
  URL.revokeObjectURL(url)
  return Promise.resolve()
}

function fillImageMeta(url: string, revoke = false) {
  return new Promise<void>((resolve) => {
    const image = new Image()
    image.onload = () => {
      form.width = image.naturalWidth
      form.height = image.naturalHeight
      if (revoke) URL.revokeObjectURL(url)
      resolve()
    }
    image.onerror = () => {
      if (revoke) URL.revokeObjectURL(url)
      resolve()
    }
    image.src = url
  })
}

function fillVideoMeta(url: string, revoke = false) {
  return new Promise<void>((resolve) => {
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      form.width = video.videoWidth
      form.height = video.videoHeight
      form.durationSeconds = Math.round(video.duration)
      if (revoke) URL.revokeObjectURL(url)
      resolve()
    }
    video.onerror = () => {
      if (revoke) URL.revokeObjectURL(url)
      resolve()
    }
    video.src = url
  })
}

function getMaterialMaxSize(file: File) {
  if (file.type.startsWith('image/')) {
    return 20 * 1024 * 1024
  }
  if (file.type.startsWith('video/')) {
    return 500 * 1024 * 1024
  }
  return 200 * 1024 * 1024
}

function formatFileSize(size: number) {
  if (size >= 1024 * 1024) {
    return `${Math.round(size / 1024 / 1024)}MB`
  }
  return `${Math.round(size / 1024)}KB`
}

onMounted(async () => {
  await loadApprovedAds()
  await loadDetail()
})
</script>

<style scoped>
.upload-field {
  width: 100%;
  display: grid;
  gap: 10px;
}

.upload-hint {
  background: rgba(18, 40, 52, 0.78);
}

.upload-copy {
  display: grid;
  gap: 6px;
  color: #e9f8ff;
}

.upload-copy span {
  color: #7f9caf;
  font-size: 13px;
}

.preview-card {
  padding: 14px;
  border: 1px solid rgba(83, 229, 255, 0.16);
  border-radius: 12px;
  background: rgba(2, 12, 18, 0.42);
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #f4fbff;
}

.preview-card img,
.preview-card video {
  width: 100%;
  max-height: 360px;
  object-fit: contain;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.25);
}

.cover-preview {
  width: 180px;
  max-height: 110px;
  object-fit: cover;
  border: 1px solid rgba(83, 229, 255, 0.18);
  border-radius: 10px;
}

.h5-preview {
  min-height: 120px;
  display: grid;
  place-items: center;
  gap: 6px;
  border: 1px dashed rgba(83, 229, 255, 0.28);
  border-radius: 10px;
  color: #dff7ff;
}

.h5-preview small {
  color: #7f9caf;
}

.option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.option-row small {
  color: rgba(213, 240, 255, 0.58);
}
</style>
