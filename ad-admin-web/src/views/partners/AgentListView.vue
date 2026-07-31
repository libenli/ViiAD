<template>
  <AppPage eyebrow="账号主体" title="代理商" :stats="stats">
    <template #actions>
      <el-button v-if="user.hasPermission('partner:manage')" type="primary" :icon="Plus" @click="openCreate">新建代理商</el-button>
    </template>

    <el-form class="filter-form" :model="query" inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="代理商 / 编号" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option v-for="(item, key) in partnerStatusMap" :key="key" :label="item.label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadAgents">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" class="data-table" row-key="id">
      <el-table-column prop="agentCode" label="代理商编号" min-width="170" />
      <el-table-column prop="agentName" label="代理商名称" min-width="190" />
      <el-table-column prop="contactName" label="联系人" width="110" />
      <el-table-column prop="contactPhone" label="电话" min-width="140" />
      <el-table-column prop="contactEmail" label="邮箱" min-width="170" />
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
        <el-empty description="暂无代理商，可先新建代理商后关联广告" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadAgents"
        @size-change="loadAgents"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="代理商详情" size="460px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="编号">{{ current.agentCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ current.agentName }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ current.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ current.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ current.contactEmail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ current.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑代理商' : '新建代理商'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="代理商名称" required>
          <el-input v-model="form.agentName" placeholder="请输入代理商名称" />
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
  createAgent,
  disableAgent,
  enableAgent,
  fetchAgents,
  partnerStatusMap,
  updateAgent,
  type Agent,
  type AgentPayload
} from '@/api/partners'

const loading = ref(false)
const user = useUserStore()
const saving = ref(false)
const drawerVisible = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const records = ref<Agent[]>([])
const current = ref<Agent>()
const total = ref(0)

const query = reactive({ keyword: '', status: '', page: 1, size: 10 })
const form = reactive<AgentPayload>({
  agentName: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  remark: ''
})

const stats = computed(() => [
  { label: '全部代理商', value: total.value },
  { label: '启用', value: records.value.filter((item) => item.status === 'active').length },
  { label: '停用', value: records.value.filter((item) => item.status === 'disabled').length },
  { label: '本页新增', value: records.value.length }
])

async function loadAgents() {
  loading.value = true
  try {
    const result = await fetchAgents(query)
    records.value = result.data.records
    total.value = result.data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { keyword: '', status: '', page: 1 })
  loadAgents()
}

function resetForm() {
  editingId.value = undefined
  Object.assign(form, { agentName: '', contactName: '', contactPhone: '', contactEmail: '', remark: '' })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Agent) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

function openDetail(row: Agent) {
  current.value = row
  drawerVisible.value = true
}

async function handleSave() {
  if (!form.agentName) {
    ElMessage.warning('请输入代理商名称')
    return
  }
  saving.value = true
  try {
    editingId.value ? await updateAgent(editingId.value, form) : await createAgent(form)
    ElMessage.success('代理商已保存')
    dialogVisible.value = false
    loadAgents()
  } finally {
    saving.value = false
  }
}

async function handleDisable(id: number) {
  await ElMessageBox.confirm('确认停用该代理商吗？', '停用代理商', { type: 'warning' })
  await disableAgent(id)
  ElMessage.success('代理商已停用')
  loadAgents()
}

async function handleEnable(id: number) {
  await enableAgent(id)
  ElMessage.success('代理商已启用')
  loadAgents()
}

onMounted(loadAgents)
</script>
