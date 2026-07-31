<template>
  <AppPage eyebrow="广告业务" title="广告详情" :stats="stats">
    <template #actions>
      <el-button @click="router.push('/ads')">返回列表</el-button>
      <el-button
        v-if="ad && user.hasPermission('ad:edit') && (ad.status === 'draft' || ad.status === 'rejected')"
        type="primary"
        @click="router.push(`/ads/${ad.id}/edit`)"
      >
        编辑
      </el-button>
      <el-button
        v-if="ad && user.hasPermission('ad:submit') && (ad.status === 'draft' || ad.status === 'rejected')"
        type="warning"
        @click="handleSubmit"
      >
        提交审核
      </el-button>
      <el-button v-if="user.hasPermission('ad:audit') && ad?.status === 'submitted'" type="success" @click="handleApprove">
        审核通过
      </el-button>
      <el-button v-if="user.hasPermission('ad:audit') && ad?.status === 'submitted'" type="danger" @click="handleReject">
        驳回
      </el-button>
    </template>

    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else-if="ad">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="广告编号">{{ ad.adCode }}</el-descriptions-item>
        <el-descriptions-item label="广告名称">{{ ad.adName }}</el-descriptions-item>
        <el-descriptions-item label="广告主ID">{{ ad.advertiserId }}</el-descriptions-item>
        <el-descriptions-item label="代理商ID">{{ ad.agentId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="广告类型">{{ getAdTypeLabel(ad.adType) }}</el-descriptions-item>
        <el-descriptions-item label="投放目标">{{ ad.objective || '-' }}</el-descriptions-item>
        <el-descriptions-item label="投放区域">{{ ad.regionCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预算金额">￥{{ ad.budgetAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="adStatusMap[ad.status]?.type || 'info'" effect="dark">
            {{ adStatusMap[ad.status]?.label || ad.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ ad.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">{{ ad.auditComment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="广告说明" :span="2">{{ ad.description || '-' }}</el-descriptions-item>
      </el-descriptions>
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

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const ad = ref<AdOrder>()
const id = computed(() => Number(route.params.id))

const stats = computed(() => [
  { label: '当前状态', value: ad.value ? adStatusMap[ad.value.status]?.label || ad.value.status : '-' },
  { label: '广告类型', value: ad.value ? getAdTypeLabel(ad.value.adType) : '-' },
  { label: '预算金额', value: ad.value ? `￥${ad.value.budgetAmount || 0}` : '-' },
  { label: '广告主ID', value: ad.value?.advertiserId || '-' }
])

function getAdTypeLabel(value: string) {
  return adTypeOptions.find((item) => item.value === value)?.label || value
}

async function loadDetail() {
  loading.value = true
  try {
    const result = await fetchAdDetail(id.value)
    ad.value = result.data
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  await submitAd(id.value)
  ElMessage.success('已提交审核')
  loadDetail()
}

async function handleApprove() {
  await approveAd(id.value)
  ElMessage.success('审核已通过')
  loadDetail()
}

async function handleReject() {
  const result = await ElMessageBox.prompt('请输入驳回原因', '驳回广告', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '驳回原因不能为空'
  })
  await rejectAd(id.value, result.value)
  ElMessage.success('广告已驳回')
  loadDetail()
}

onMounted(loadDetail)
</script>
