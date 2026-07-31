<template>
  <AppPage eyebrow="广告业务" :title="isEdit ? '编辑广告' : '新建广告'" :stats="stats">
    <el-form ref="formRef" class="entity-form" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="广告名称" prop="adName">
        <el-input v-model="form.adName" maxlength="80" show-word-limit placeholder="请输入广告名称" />
      </el-form-item>
      <el-form-item label="广告主" prop="advertiserId">
        <el-select v-model="form.advertiserId" filterable :loading="partnerLoading" placeholder="请选择启用广告主">
          <el-option v-for="item in advertisers" :key="item.id" :label="getAdvertiserLabel(item)" :value="item.id">
            <div class="partner-option">
              <span>{{ item.advertiserName }}</span>
              <small>{{ item.advertiserCode }} / {{ item.companyName || '未配置公司' }}</small>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="代理商">
        <el-select v-model="form.agentId" clearable filterable :loading="partnerLoading" placeholder="请选择代理商（可选）">
          <el-option v-for="item in agents" :key="item.id" :label="getAgentLabel(item)" :value="item.id">
            <div class="partner-option">
              <span>{{ item.agentName }}</span>
              <small>{{ item.agentCode }}</small>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="广告类型" prop="adType">
        <el-select v-model="form.adType" placeholder="请选择广告类型">
          <el-option v-for="item in adTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="投放目标">
        <el-select v-model="form.objective" clearable placeholder="请选择投放目标">
          <el-option v-for="item in objectiveOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="投放区域">
        <el-input v-model="form.regionCode" placeholder="如 华东 / 上海" />
      </el-form-item>
      <el-form-item label="预算金额">
        <el-input-number v-model="form.budgetAmount" :min="0" :precision="2" controls-position="right" />
      </el-form-item>
      <el-form-item label="广告说明">
        <el-input v-model="form.description" type="textarea" :rows="5" maxlength="500" show-word-limit />
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
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AppPage from '@/components/AppPage.vue'
import { adTypeOptions, createAd, fetchAdDetail, objectiveOptions, updateAd, type AdOrderPayload } from '@/api/ads'
import { fetchAdvertisers, fetchAgents, type Advertiser, type Agent } from '@/api/partners'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const partnerLoading = ref(false)
const advertisers = ref<Advertiser[]>([])
const agents = ref<Agent[]>([])

const id = computed(() => Number(route.params.id))
const isEdit = computed(() => Boolean(route.params.id))

const form = reactive<AdOrderPayload>({
  adName: '',
  advertiserId: undefined as unknown as number,
  agentId: undefined,
  adType: 'image',
  objective: 'exposure',
  regionCode: '',
  budgetAmount: 0,
  description: ''
})

const rules: FormRules = {
  adName: [{ required: true, message: '请输入广告名称', trigger: 'blur' }],
  advertiserId: [{ required: true, message: '请选择广告主', trigger: 'change' }],
  adType: [{ required: true, message: '请选择广告类型', trigger: 'change' }]
}

const stats = computed(() => [
  { label: '表单模式', value: isEdit.value ? '编辑' : '新建' },
  { label: '默认状态', value: '草稿' },
  { label: '主流程', value: '广告' },
  { label: '下一步', value: '素材' }
])

async function loadDetail() {
  if (!isEdit.value) return
  const result = await fetchAdDetail(id.value)
  Object.assign(form, {
    adName: result.data.adName,
    advertiserId: result.data.advertiserId,
    agentId: result.data.agentId,
    adType: result.data.adType,
    objective: result.data.objective,
    regionCode: result.data.regionCode,
    budgetAmount: result.data.budgetAmount,
    description: result.data.description
  })
}

function getAdvertiserLabel(item: Advertiser) {
  return `${item.advertiserName}（${item.advertiserCode}）`
}

function getAgentLabel(item: Agent) {
  return `${item.agentName}（${item.agentCode}）`
}

async function loadPartners() {
  partnerLoading.value = true
  try {
    const [advertiserResult, agentResult] = await Promise.all([
      fetchAdvertisers({ status: 'active', page: 1, size: 200 }),
      fetchAgents({ status: 'active', page: 1, size: 200 })
    ])
    advertisers.value = advertiserResult.data.records
    agents.value = agentResult.data.records
  } finally {
    partnerLoading.value = false
  }
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const result = isEdit.value ? await updateAd(id.value, form) : await createAd(form)
    ElMessage.success(isEdit.value ? '广告已更新' : '广告已创建')
    router.replace(`/ads/${result.data.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadPartners()
  await loadDetail()
})
</script>

<style scoped>
.partner-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.partner-option small {
  color: rgba(213, 240, 255, 0.58);
}
</style>
