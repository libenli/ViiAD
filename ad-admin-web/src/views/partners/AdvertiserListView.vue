<template>
  <AppPage eyebrow="账号主体" title="广告主" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('partner:manage')" type="primary" :icon="Plus" @click="openCreate">新建广告主</el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="广告主 / 公司 / 编号" />
      </el-form-item>
      <el-form-item label="来源">
        <el-select v-model="query.sourceType" clearable placeholder="全部来源" style="width: 140px">
          <el-option v-for="(label, key) in advertiserSourceMap" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option v-for="(item, key) in partnerStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadAdvertisers">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="advertiserCode" label="广告主编号" min-width="170" />
      <el-table-column prop="advertiserName" label="广告主名称" min-width="180" />
      <el-table-column prop="companyName" label="公司名称" min-width="180" />
      <el-table-column prop="contactName" label="联系人" width="110" />
      <el-table-column prop="contactPhone" label="电话" min-width="140" />
      <el-table-column label="来源" width="120">
        <template #default="{ row }">{{ advertiserSourceMap[row.sourceType] || row.sourceType || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="partnerStatusMap[row.status]?.type || 'info'" effect="dark">
            {{ partnerStatusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" min-width="220" class-name="operation-column">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="user.hasPermission('partner:manage')" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="user.hasPermission('partner:manage') && row.status === 'active'" link type="warning" @click="handleDisable(row.id)">停用</el-button>
          <el-button v-else-if="user.hasPermission('partner:manage')" link type="success" @click="handleEnable(row.id)">启用</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无广告主，可先新建广告主后创建广告" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadAdvertisers"
        @size-change="loadAdvertisers"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="广告主详情" size="460px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="编号">{{ current.advertiserCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ current.advertiserName }}</el-descriptions-item>
        <el-descriptions-item label="公司">{{ current.companyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ current.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ current.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ current.contactEmail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ advertiserSourceMap[current.sourceType || ''] || current.sourceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ current.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑广告主' : '新建广告主'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="广告主名称" required>
          <el-input v-model="form.advertiserName" placeholder="请输入广告主名称" />
        </el-form-item>
        <el-form-item label="公司名称">
          <el-input v-model="form.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.contactEmail" placeholder="请输入联系邮箱" />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="form.sourceType" class="wide-control">
            <el-option v-for="(label, key) in advertiserSourceMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import AppPage from '@/components/AppPage.vue'
import { useUserStore } from '@/stores/user'
import {
  advertiserSourceMap,
  createAdvertiser,
  disableAdvertiser,
  enableAdvertiser,
  fetchAdvertisers,
  partnerStatusMap,
  updateAdvertiser,
  type Advertiser,
  type AdvertiserPayload
} from '@/api/partners'

const loading = ref(false)
const user = useUserStore()
const saving = ref(false)
const drawerVisible = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const records = ref<Advertiser[]>([])
const current = ref<Advertiser>()
const total = ref(0)

const query = reactive({ keyword: '', sourceType: '', status: '', page: 1, size: 10 })
const form = reactive<AdvertiserPayload>({
  advertiserName: '',
  companyName: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  sourceType: 'platform',
  remark: ''
})

const stats = computed(() => [
  { label: '全部广告主', value: total.value },
  { label: '启用', value: records.value.filter((item) => item.status === 'active').length },
  { label: '停用', value: records.value.filter((item) => item.status === 'disabled').length },
  { label: '平台招商', value: records.value.filter((item) => item.sourceType === 'platform').length }
])

async function loadAdvertisers() {
  loading.value = true
  try {
    const result = await fetchAdvertisers(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { keyword: '', sourceType: '', status: '', page: 1 })
  loadAdvertisers()
}

function resetForm() {
  editingId.value = undefined
  Object.assign(form, { advertiserName: '', companyName: '', contactName: '', contactPhone: '', contactEmail: '', sourceType: 'platform', remark: '' })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Advertiser) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

function openDetail(row: Advertiser) {
  current.value = row
  drawerVisible.value = true
}

async function handleSave() {
  if (!form.advertiserName) {
    ElMessage.warning('请输入广告主名称')
    return
  }
  saving.value = true
  try {
    editingId.value ? await updateAdvertiser(editingId.value, form) : await createAdvertiser(form)
    ElMessage.success('广告主已保存')
    dialogVisible.value = false
    loadAdvertisers()
  } finally {
    saving.value = false
  }
}

async function handleDisable(id: number) {
  await ElMessageBox.confirm('确认停用该广告主吗？', '停用广告主', { type: 'warning' })
  await disableAdvertiser(id)
  ElMessage.success('广告主已停用')
  loadAdvertisers()
}

async function handleEnable(id: number) {
  await enableAdvertiser(id)
  ElMessage.success('广告主已启用')
  loadAdvertisers()
}

onMounted(loadAdvertisers)
</script>

<style scoped>
.wide-control {
  width: 100%;
}
</style>
