<template>
  <AppPage eyebrow="广告业务" title="素材管理" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('material:edit')" type="primary" :icon="Plus" @click="router.push('/materials/create')">
        上传素材
      </el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="素材名称 / 编号" />
      </el-form-item>
      <el-form-item label="广告ID">
        <el-input-number v-model="query.adId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 150px">
          <el-option v-for="(item, key) in materialStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.materialType" clearable placeholder="全部类型" style="width: 150px">
          <el-option v-for="item in materialTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadMaterials">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="materialCode" label="素材编号" min-width="170" />
      <el-table-column prop="materialName" label="素材名称" min-width="220" />
      <el-table-column prop="adId" label="广告ID" width="100" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ getMaterialTypeLabel(row.materialType) }}</template>
      </el-table-column>
      <el-table-column label="尺寸" width="130">
        <template #default="{ row }">
          {{ row.width && row.height ? `${row.width}x${row.height}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="时长" width="100">
        <template #default="{ row }">{{ row.durationSeconds ? `${row.durationSeconds}s` : '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="materialStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ materialStatusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" min-width="170" />
      <el-table-column label="操作" min-width="250" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/materials/${row.id}`)">详情</el-button>
          <el-button
            v-if="user.hasPermission('material:edit') && (row.status === 'draft' || row.status === 'rejected')"
            link
            type="primary"
            @click="router.push(`/materials/${row.id}/edit`)"
          >
            编辑
          </el-button>
          <el-button
            v-if="user.hasPermission('material:submit') && (row.status === 'draft' || row.status === 'rejected')"
            link
            type="warning"
            @click="handleSubmit(row.id)"
          >
            提交
          </el-button>
          <el-button
            v-if="user.hasPermission('material:audit') && row.status === 'pending_review'"
            link
            type="success"
            @click="handleApprove(row.id)"
          >
            通过
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
import { useUserStore } from '@/stores/user'
import {
  approveMaterial,
  fetchMaterials,
  materialStatusMap,
  materialTypeOptions,
  submitMaterial,
  type AdMaterial
} from '@/api/materials'

const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const records = ref<AdMaterial[]>([])
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
  { label: '全部素材', value: total.value },
  { label: '草稿', value: records.value.filter((item) => item.status === 'draft').length },
  { label: '待审核', value: records.value.filter((item) => item.status === 'pending_review').length },
  { label: '已通过', value: records.value.filter((item) => item.status === 'approved').length }
])

function getMaterialTypeLabel(value: string) {
  return materialTypeOptions.find((item) => item.value === value)?.label || value
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
  ElMessage.success('已提交审核')
  loadMaterials()
}

async function handleApprove(id: number) {
  await approveMaterial(id)
  ElMessage.success('审核已通过')
  loadMaterials()
}

onMounted(loadMaterials)
</script>
