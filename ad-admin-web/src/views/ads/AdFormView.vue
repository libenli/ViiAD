<template>
  <AppPage :eyebrow="locale.t('page.business')" :title="isEdit ? locale.t('page.ads.editTitle') : locale.t('page.ads.createTitle')" :stats="stats">
    <el-form ref="formRef" class="entity-form" :model="form" :rules="rules" label-width="110px">
      <el-form-item :label="locale.t('page.ads.name')" prop="adName">
        <el-input
          v-model="form.adName"
          maxlength="80"
          show-word-limit
          :placeholder="locale.t('page.ads.adNameRequired')"
          @input="form.adName = sanitizeBusinessText(form.adName)"
        />
      </el-form-item>
      <el-form-item :label="locale.t('page.ads.advertiser')" prop="advertiserId">
        <el-select v-model="form.advertiserId" filterable :loading="partnerLoading" :placeholder="locale.t('page.ads.advertiserPlaceholder')">
          <el-option v-for="item in advertisers" :key="item.id" :label="getAdvertiserLabel(item)" :value="item.id">
            <div class="partner-option">
              <span>{{ item.advertiserName }}</span>
              <small>{{ item.advertiserCode }} / {{ item.companyName || locale.t('page.ads.noCompany') }}</small>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.ads.agent')">
        <el-select v-model="form.agentId" clearable filterable :loading="partnerLoading" :placeholder="locale.t('page.ads.agentPlaceholder')">
          <el-option v-for="item in agents" :key="item.id" :label="getAgentLabel(item)" :value="item.id">
            <div class="partner-option">
              <span>{{ item.agentName }}</span>
              <small>{{ item.agentCode }}</small>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.ads.type')" prop="adType">
        <el-select v-model="form.adType" :placeholder="locale.t('page.ads.adTypeRequired')">
          <el-option v-for="item in adTypeOptions" :key="item.value" :label="getAdTypeLabel(item.value)" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.ads.objective')">
        <el-select v-model="form.objective" clearable :placeholder="locale.t('page.ads.objectivePlaceholder')">
          <el-option v-for="item in objectiveOptions" :key="item.value" :label="getObjectiveLabel(item.value, item.label)" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="locale.t('page.ads.region')" prop="regionCode">
        <el-cascader
          v-model="form.regionCode"
          :options="chinaRegionOptions"
          :props="regionProps"
          clearable
          filterable
          :placeholder="locale.t('page.ads.regionPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="locale.t('page.ads.budget')">
        <el-input-number v-model="form.budgetAmount" :min="0" :precision="2" controls-position="right" />
      </el-form-item>
      <el-form-item :label="locale.t('page.ads.description')">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="5"
          maxlength="500"
          show-word-limit
          @input="form.description = sanitizeBusinessText(form.description)"
        />
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
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AppPage from '@/components/AppPage.vue'
import { adTypeOptions, createAd, fetchAdDetail, objectiveOptions, updateAd, type AdOrderPayload } from '@/api/ads'
import { fetchAdvertisers, fetchAgents, type Advertiser, type Agent } from '@/api/partners'
import { chinaRegionOptions } from '@/config/regions'
import { useLocaleStore } from '@/stores/locale'
import { sanitizeBusinessText } from '@/utils/text'

const route = useRoute()
const router = useRouter()
const locale = useLocaleStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const partnerLoading = ref(false)
const advertisers = ref<Advertiser[]>([])
const agents = ref<Agent[]>([])
const regionProps = { checkStrictly: true, emitPath: false }

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

const rules = computed<FormRules>(() => ({
  adName: [{ required: true, message: locale.t('page.ads.adNameRequired'), trigger: 'blur' }],
  advertiserId: [{ required: true, message: locale.t('page.ads.advertiserRequired'), trigger: 'change' }],
  adType: [{ required: true, message: locale.t('page.ads.adTypeRequired'), trigger: 'change' }],
  regionCode: [{ required: true, message: locale.t('page.ads.regionRequired'), trigger: 'change' }]
}))

const stats = computed(() => [
  { label: locale.t('page.ads.formMode'), value: isEdit.value ? locale.t('page.ads.editMode') : locale.t('page.ads.newMode') },
  { label: locale.t('page.ads.defaultStatus'), value: locale.t('status.ad.draft') },
  { label: locale.t('page.ads.mainFlow'), value: locale.t('menu.ads') },
  { label: locale.t('page.ads.nextStep'), value: locale.t('page.ads.materialStep') }
])

function getAdTypeLabel(value: string) {
  return locale.t(`status.adType.${value}`, adTypeOptions.find((item) => item.value === value)?.label || value)
}

function getObjectiveLabel(value: string, fallback: string) {
  return locale.t(`status.objective.${value}`, fallback)
}

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
    ElMessage.success(isEdit.value ? locale.t('page.ads.updated') : locale.t('page.ads.created'))
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
