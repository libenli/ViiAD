<template>
  <AppPage eyebrow="广告业务" title="素材详情" :stats="stats">
    <template #actions>
      <el-button @click="router.push('/materials')">返回列表</el-button>
      <el-button
        v-if="material && user.hasPermission('material:edit') && (material.status === 'draft' || material.status === 'rejected')"
        type="primary"
        @click="router.push(`/materials/${material.id}/edit`)"
      >
        编辑
      </el-button>
      <el-button
        v-if="material && user.hasPermission('material:submit') && (material.status === 'draft' || material.status === 'rejected')"
        type="warning"
        @click="handleSubmit"
      >
        提交审核
      </el-button>
      <el-button v-if="user.hasPermission('material:audit') && material?.status === 'pending_review'" type="success" @click="handleApprove">
        审核通过
      </el-button>
      <el-button v-if="user.hasPermission('material:audit') && material?.status === 'pending_review'" type="danger" @click="handleReject">
        驳回
      </el-button>
    </template>

    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else-if="material">
      <div class="detail-grid">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="素材编号">{{ material.materialCode }}</el-descriptions-item>
          <el-descriptions-item label="素材名称">{{ material.materialName }}</el-descriptions-item>
          <el-descriptions-item label="所属广告ID">{{ material.adId }}</el-descriptions-item>
          <el-descriptions-item label="素材类型">{{ getMaterialTypeLabel(material.materialType) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="materialStatusMap[material.status]?.type || 'info'" effect="dark">
              {{ materialStatusMap[material.status]?.label || material.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="上传人">{{ material.uploaderId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="尺寸">
            {{ material.width && material.height ? `${material.width}x${material.height}` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="视频时长">{{ material.durationSeconds ? `${material.durationSeconds}s` : '-' }}</el-descriptions-item>
          <el-descriptions-item label="文件地址" :span="2">
            <a :href="material.fileUrl" target="_blank" rel="noreferrer">{{ material.fileUrl }}</a>
          </el-descriptions-item>
          <el-descriptions-item label="封面地址" :span="2">{{ material.coverUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">{{ material.reviewComment || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="preview-panel">
          <img v-if="material.materialType === 'image'" :src="material.fileUrl" alt="素材预览" />
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

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const material = ref<AdMaterial>()
const id = computed(() => Number(route.params.id))

const stats = computed(() => [
  { label: '当前状态', value: material.value ? materialStatusMap[material.value.status]?.label || material.value.status : '-' },
  { label: '素材类型', value: material.value ? getMaterialTypeLabel(material.value.materialType) : '-' },
  { label: '所属广告', value: material.value?.adId || '-' },
  { label: '审核人', value: material.value?.reviewUserId || '-' }
])

function getMaterialTypeLabel(value: string) {
  return materialTypeOptions.find((item) => item.value === value)?.label || value
}

async function loadDetail() {
  loading.value = true
  try {
    const result = await fetchMaterialDetail(id.value)
    material.value = result.data
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  await submitMaterial(id.value)
  ElMessage.success('已提交审核')
  loadDetail()
}

async function handleApprove() {
  await approveMaterial(id.value)
  ElMessage.success('审核已通过')
  loadDetail()
}

async function handleReject() {
  const result = await ElMessageBox.prompt('请输入驳回原因', '驳回素材', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '驳回原因不能为空'
  })
  await rejectMaterial(id.value, result.value)
  ElMessage.success('素材已驳回')
  loadDetail()
}

onMounted(loadDetail)
</script>
