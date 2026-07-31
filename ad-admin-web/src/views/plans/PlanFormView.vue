<template>
  <AppPage eyebrow="广告业务" :title="isEdit ? '编辑投放计划' : '新建投放计划'" :stats="stats">
    <el-form ref="formRef" class="detail-form" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="计划名称" prop="planName">
        <el-input v-model="form.planName" maxlength="80" show-word-limit placeholder="如：华东商圈7月品牌曝光计划" />
      </el-form-item>
      <el-form-item label="关联广告" prop="adId">
        <el-select
          v-model="form.adId"
          class="wide-control"
          filterable
          :loading="adLoading"
          placeholder="请选择已审核通过的广告"
        >
          <el-option v-for="ad in adOptions" :key="ad.id" :label="getAdLabel(ad)" :value="ad.id">
            <div class="option-row">
              <span>{{ ad.adName }}</span>
              <small>{{ ad.adCode }} / {{ ad.regionCode || '未配置区域' }}</small>
            </div>
          </el-option>
        </el-select>
        <p class="field-tip">
          仅展示审核通过的广告；选择广告后会自动带出投放区域并刷新可选素材。
        </p>
      </el-form-item>
      <el-form-item label="投放区域">
        <el-input v-model="form.regionCode" placeholder="如 华东 / 上海 / 商圈A" />
      </el-form-item>
      <el-form-item label="投放时间" required>
        <div class="date-row">
          <el-form-item prop="startTime">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="开始时间"
            />
          </el-form-item>
          <span>至</span>
          <el-form-item prop="endTime">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="结束时间"
            />
          </el-form-item>
        </div>
      </el-form-item>
      <el-form-item label="投放素材" prop="materialIds">
        <el-select
          v-model="form.materialIds"
          class="wide-control"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          :disabled="!form.adId"
          :loading="materialLoading"
          :no-data-text="form.adId ? '当前广告暂无已审核素材' : '请先选择广告'"
          placeholder="请选择当前广告下已审核素材"
        >
          <el-option
            v-for="material in materialOptions"
            :key="material.id"
            :label="getMaterialLabel(material)"
            :value="material.id"
          >
            <div class="option-row">
              <span>{{ material.materialName }}</span>
              <small>{{ getMaterialTypeLabel(material.materialType) }} / {{ material.materialCode }}</small>
            </div>
          </el-option>
        </el-select>
        <p class="field-tip">
          {{ form.adId ? '仅展示当前广告下审核通过的素材。' : '请先选择广告，系统会自动加载该广告下已审核素材。' }}
        </p>
      </el-form-item>
      <el-form-item label="投放设备" prop="deviceIds">
        <el-select
          v-model="form.deviceIds"
          class="wide-control"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          :loading="deviceLoading"
          placeholder="请选择启用且正常的设备"
        >
          <el-option v-for="device in deviceOptions" :key="device.id" :label="getDeviceLabel(device)" :value="device.id">
            <div class="device-option">
              <span>{{ device.deviceName }}</span>
              <small>{{ device.deviceCode }} / {{ device.ipAddress || '未配置IP' }}</small>
            </div>
          </el-option>
        </el-select>
        <p class="field-tip">
          仅展示启用、非故障设备；如没有候选设备，请先到设备管理中新建设备并恢复为正常状态。
        </p>
      </el-form-item>
      <el-form-item label="运营人员ID">
        <el-input-number v-model="form.operatorId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item class="form-actions">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存计划' }}
        </el-button>
        <el-button @click="router.push('/plans')">取消</el-button>
      </el-form-item>
    </el-form>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import AppPage from '@/components/AppPage.vue'
import { createPlan, fetchPlanDetail, updatePlan, type AdPlanPayload } from '@/api/plans'
import { fetchDevices, type AdDevice } from '@/api/devices'
import { fetchMaterials, materialTypeOptions, type AdMaterial } from '@/api/materials'
import { fetchAds, type AdOrder } from '@/api/ads'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const adLoading = ref(false)
const materialLoading = ref(false)
const deviceLoading = ref(false)
const adOptions = ref<AdOrder[]>([])
const materialOptions = ref<AdMaterial[]>([])
const deviceOptions = ref<AdDevice[]>([])
const isEdit = computed(() => Boolean(route.params.id))

const form = reactive({
  planName: '',
  adId: undefined as number | undefined,
  regionCode: '',
  startTime: '',
  endTime: '',
  materialIds: [] as number[],
  deviceIds: [] as number[],
  operatorId: undefined as number | undefined
})

const stats = computed(() => [
  { label: '表单模式', value: isEdit.value ? '编辑' : '新建' },
  { label: '默认排期', value: '草稿' },
  { label: '可选广告', value: adOptions.value.length },
  { label: '可选素材', value: materialOptions.value.length },
  { label: '可选设备', value: deviceOptions.value.length }
])

const rules: FormRules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  adId: [{ required: true, message: '请选择广告', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  materialIds: [{ type: 'array', required: true, message: '请选择投放素材', trigger: 'change' }],
  deviceIds: [{ type: 'array', required: true, message: '请选择投放设备', trigger: 'change' }]
}

function buildPayload(): AdPlanPayload {
  if (!form.adId) {
    throw new Error('请先选择广告')
  }
  if (!form.materialIds.length) {
    throw new Error('请至少选择一个投放素材')
  }
  if (!form.deviceIds.length) {
    throw new Error('请至少选择一台投放设备')
  }
  return {
    planName: form.planName,
    adId: form.adId as number,
    regionCode: form.regionCode,
    startTime: form.startTime,
    endTime: form.endTime,
    materialIds: form.materialIds,
    deviceIds: form.deviceIds,
    operatorId: form.operatorId
  }
}

function getAdLabel(ad: AdOrder) {
  return `${ad.adName}（${ad.adCode}）`
}

function getMaterialLabel(material: AdMaterial) {
  return `${material.materialName}（${material.materialCode}）`
}

function getMaterialTypeLabel(value: string) {
  return materialTypeOptions.find((item) => item.value === value)?.label || value
}

function getDeviceLabel(device: AdDevice) {
  return `${device.deviceName}（${device.deviceCode}）`
}

async function loadApprovedAds() {
  adLoading.value = true
  try {
    const result = await fetchAds({
      status: 'approved',
      page: 1,
      size: 200
    })
    adOptions.value = result.data.records
  } finally {
    adLoading.value = false
  }
}

async function loadApprovedMaterials(adId?: number) {
  if (!adId) {
    materialOptions.value = []
    return
  }
  materialLoading.value = true
  try {
    const result = await fetchMaterials({
      adId,
      status: 'approved',
      page: 1,
      size: 200
    })
    materialOptions.value = result.data.records
  } finally {
    materialLoading.value = false
  }
}

async function loadAvailableDevices() {
  deviceLoading.value = true
  try {
    const result = await fetchDevices({
      status: 'active',
      faultStatus: 'normal',
      page: 1,
      size: 200
    })
    deviceOptions.value = result.data.records
  } finally {
    deviceLoading.value = false
  }
}

async function loadDetail() {
  if (!isEdit.value) {
    return
  }
  const result = await fetchPlanDetail(Number(route.params.id))
  const plan = result.data
  form.planName = plan.planName
  form.adId = plan.adId
  form.regionCode = plan.regionCode || ''
  form.startTime = plan.startTime
  form.endTime = plan.endTime
  form.materialIds = plan.materialIds || []
  form.deviceIds = plan.deviceIds || []
  form.operatorId = plan.operatorId
}

async function handleSave() {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    const result = isEdit.value
      ? await updatePlan(Number(route.params.id), payload)
      : await createPlan(payload)
    ElMessage.success(isEdit.value ? '计划已保存' : '计划已创建')
    router.push(`/plans/${result.data.id}`)
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadApprovedAds()
  await loadAvailableDevices()
  await loadDetail()
})

watch(
  () => form.adId,
  async (adId, oldAdId) => {
    if (oldAdId !== undefined && adId !== oldAdId) {
      form.materialIds = []
    }
    const selectedAd = adOptions.value.find((item) => item.id === adId)
    if (selectedAd) {
      form.regionCode = selectedAd.regionCode || ''
    }
    await loadApprovedMaterials(adId)
  }
)
</script>

<style scoped>
.date-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.date-row > span {
  line-height: 32px;
  color: rgba(213, 240, 255, 0.75);
}

.wide-control {
  width: 100%;
}

.option-row,
.device-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.option-row small,
.device-option small {
  color: rgba(213, 240, 255, 0.58);
}

.field-tip {
  margin: 6px 0 0;
  color: rgba(213, 240, 255, 0.58);
  font-size: 12px;
  line-height: 1.6;
}
</style>
