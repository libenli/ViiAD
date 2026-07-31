<template>
  <AppPage eyebrow="广告业务" :title="isEdit ? '编辑素材' : '上传素材'" :stats="stats">
    <el-form ref="formRef" class="entity-form" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="所属广告ID" prop="adId">
        <el-input-number v-model="form.adId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="素材名称" prop="materialName">
        <el-input v-model="form.materialName" maxlength="80" show-word-limit placeholder="请输入素材名称" />
      </el-form-item>
      <el-form-item label="素材类型" prop="materialType">
        <el-select v-model="form.materialType" placeholder="请选择素材类型">
          <el-option v-for="item in materialTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="素材文件" prop="fileUrl">
        <div class="upload-field">
          <el-alert
            class="upload-hint"
            type="info"
            show-icon
            :closable="false"
            title="支持 JPG/PNG/GIF/WebP 图片、MP4/WebM 视频、HTML/ZIP；图片建议不超过 20MB，视频不超过 500MB。"
          />
          <el-upload
            drag
            :show-file-list="false"
            :http-request="handleMaterialUpload"
            :before-upload="beforeMaterialUpload"
          >
            <div class="upload-copy">
              <strong>{{ uploadingMaterial ? '正在上传素材...' : '点击或拖拽上传图片 / 视频 / H5文件' }}</strong>
              <span>上传成功后会自动生成 OSS 地址，并自动识别类型、大小、尺寸和时长</span>
            </div>
          </el-upload>
          <el-progress
            v-if="uploadingMaterial || materialUploadProgress > 0"
            :percentage="materialUploadProgress"
            :status="materialUploadProgress === 100 ? 'success' : undefined"
          />
          <el-alert v-if="materialUploadError" type="error" show-icon :closable="false" :title="materialUploadError">
            <template #default>
              <el-button size="small" type="danger" plain @click="retryMaterialUpload">重新上传</el-button>
            </template>
          </el-alert>
          <el-input v-model="form.fileUrl" placeholder="上传后自动生成，也可手动粘贴已有 OSS 地址" />
          <div v-if="form.fileUrl" class="preview-card">
            <div class="preview-head">
              <strong>素材预览</strong>
              <el-link :href="form.fileUrl" target="_blank" type="primary">新窗口打开</el-link>
            </div>
            <img v-if="form.materialType === 'image'" :src="form.fileUrl" alt="素材预览" />
            <video v-else-if="form.materialType === 'video'" :src="form.fileUrl" controls />
            <div v-else class="h5-preview">
              <span>H5 / ZIP 素材</span>
              <small>保存后可进入详情页查看最终地址</small>
            </div>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="封面地址">
        <div class="upload-field">
          <el-upload
            :show-file-list="false"
            :http-request="handleCoverUpload"
            :before-upload="beforeCoverUpload"
          >
            <el-button :loading="uploadingCover">上传封面</el-button>
          </el-upload>
          <el-progress
            v-if="uploadingCover || coverUploadProgress > 0"
            :percentage="coverUploadProgress"
            :status="coverUploadProgress === 100 ? 'success' : undefined"
          />
          <el-alert v-if="coverUploadError" type="error" show-icon :closable="false" :title="coverUploadError">
            <template #default>
              <el-button size="small" type="danger" plain @click="retryCoverUpload">重新上传封面</el-button>
            </template>
          </el-alert>
          <el-input v-model="form.coverUrl" placeholder="可选，用于视频或H5预览" />
          <img v-if="form.coverUrl" class="cover-preview" :src="form.coverUrl" alt="封面预览" />
        </div>
      </el-form-item>
      <el-form-item label="文件大小">
        <el-input-number v-model="form.fileSize" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item label="宽度">
        <el-input-number v-model="form.width" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item label="高度">
        <el-input-number v-model="form.height" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item label="视频时长">
        <el-input-number v-model="form.durationSeconds" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item class="form-actions">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存
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

const route = useRoute()
const router = useRouter()
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

const id = computed(() => Number(route.params.id))
const isEdit = computed(() => Boolean(route.params.id))

const form = reactive<AdMaterialPayload>({
  adId: 1,
  materialName: '',
  materialType: 'image',
  fileUrl: '',
  fileSize: undefined,
  durationSeconds: undefined,
  width: 1920,
  height: 1080,
  coverUrl: ''
})

const rules: FormRules = {
  adId: [{ required: true, message: '请输入所属广告ID', trigger: 'change' }],
  materialName: [{ required: true, message: '请输入素材名称', trigger: 'blur' }],
  materialType: [{ required: true, message: '请选择素材类型', trigger: 'change' }],
  fileUrl: [{ required: true, message: '请上传素材文件或填写文件地址', trigger: 'blur' }]
}

const stats = computed(() => [
  { label: '表单模式', value: isEdit.value ? '编辑' : '上传' },
  { label: '默认状态', value: '草稿' },
  { label: '主流程', value: '素材' },
  { label: '下一步', value: '审核' }
])

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
    ElMessage.warning('文件仍在上传中，请上传完成后再保存')
    return
  }
  await formRef.value?.validate()
  saving.value = true
  try {
    const result = isEdit.value ? await updateMaterial(id.value, form) : await createMaterial(form)
    ElMessage.success(isEdit.value ? '素材已更新' : '素材已创建')
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
    ElMessage.warning('请上传图片、视频、HTML 或 ZIP 素材文件')
    return false
  }
  const maxSize = getMaterialMaxSize(file)
  if (file.size > maxSize) {
    ElMessage.warning(`文件过大，当前限制为 ${formatFileSize(maxSize)}`)
    return false
  }
  return true
}

function beforeCoverUpload(file: File) {
  coverUploadError.value = ''
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.warning('封面请上传图片文件')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('封面图片不能超过 10MB')
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
    ElMessage.success('素材文件上传成功')
    options?.onSuccess?.(result)
  } catch (error) {
    materialUploadError.value = '素材上传失败，请检查网络或 OSS 配置后重试'
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
    ElMessage.success('封面上传成功')
    options?.onSuccess?.(result)
  } catch (error) {
    coverUploadError.value = '封面上传失败，请稍后重试'
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

onMounted(loadDetail)
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
</style>
