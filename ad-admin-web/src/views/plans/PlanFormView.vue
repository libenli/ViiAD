<template>
  <AppPage :eyebrow="locale.t('page.business')" :title="isEdit ? locale.t('page.plans.editTitle') : locale.t('page.plans.createTitle')" :stats="stats">
    <el-form ref="formRef" class="detail-form" :model="form" :rules="rules" label-width="120px">
      <el-form-item :label="locale.t('page.plans.name')" prop="planName">
        <el-input v-model="form.planName" maxlength="80" show-word-limit :placeholder="locale.t('page.plans.keywordPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.plans.relatedAdId')" prop="adId">
        <el-select
          v-model="form.adId"
          class="wide-control"
          filterable
          :loading="adLoading"
          :placeholder="locale.t('page.plans.adPlaceholder')"
        >
          <el-option v-for="ad in adOptions" :key="ad.id" :label="getAdLabel(ad)" :value="ad.id">
            <div class="option-row">
              <span>{{ ad.adName }}</span>
              <small>{{ ad.adCode }} / {{ ad.regionCode || locale.t('page.plans.noRegion') }}</small>
            </div>
          </el-option>
        </el-select>
        <p class="field-tip">
          {{ locale.t('page.plans.adTip') }}
        </p>
      </el-form-item>
      <el-form-item :label="locale.t('page.plans.region')">
        <el-input v-model="form.regionCode" :placeholder="locale.t('page.plans.regionPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.plans.time')" required>
        <div class="date-row">
          <el-form-item prop="startTime">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disablePastDate"
              :disabled-hours="disabledStartHours"
              :disabled-minutes="disabledStartMinutes"
              :disabled-seconds="disabledStartSeconds"
              :placeholder="locale.t('page.plans.startTime')"
            />
          </el-form-item>
          <span>{{ locale.t('page.plans.to') }}</span>
          <el-form-item prop="endTime">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disableEndDate"
              :disabled-hours="disabledEndHours"
              :disabled-minutes="disabledEndMinutes"
              :disabled-seconds="disabledEndSeconds"
              :placeholder="locale.t('page.plans.endTime')"
            />
          </el-form-item>
        </div>
      </el-form-item>
      <el-form-item :label="locale.t('page.plans.materialIds')" prop="materialIds">
        <el-select
          v-model="form.materialIds"
          class="wide-control"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          :disabled="!form.adId"
          :loading="materialLoading"
          :no-data-text="form.adId ? locale.t('page.plans.materialNoData') : locale.t('page.plans.chooseAdFirst')"
          :placeholder="locale.t('page.plans.materialPlaceholder')"
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
          {{ form.adId ? locale.t('page.plans.materialTipReady') : locale.t('page.plans.materialTipEmpty') }}
        </p>
      </el-form-item>
      <el-form-item :label="locale.t('page.plans.deviceIds')" prop="deviceIds">
        <el-select
          v-model="form.deviceIds"
          class="wide-control"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          :loading="deviceLoading"
          :placeholder="locale.t('page.plans.devicePlaceholder')"
        >
          <el-option v-for="device in deviceOptions" :key="device.id" :label="getDeviceLabel(device)" :value="device.id">
            <div class="device-option">
              <span>{{ device.deviceName }}</span>
              <small>{{ device.deviceCode }} / {{ device.ipAddress || locale.t('page.plans.noIp') }}</small>
            </div>
          </el-option>
        </el-select>
        <p class="field-tip">
          {{ locale.t('page.plans.deviceTip') }}
        </p>
      </el-form-item>
      <el-form-item :label="locale.t('page.plans.operatorId')">
        <el-input-number v-model="form.operatorId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item class="form-actions">
        <el-button @click="router.back()">{{ locale.t('common.back') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ saving ? locale.t('page.plans.saving') : locale.t('page.plans.savePlan') }}
        </el-button>
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
import { useLocaleStore } from '@/stores/locale'

const route = useRoute()
const router = useRouter()
const locale = useLocaleStore()
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
  { label: locale.t('page.ads.formMode'), value: isEdit.value ? locale.t('page.ads.editMode') : locale.t('page.ads.newMode') },
  { label: locale.t('page.plans.defaultSchedule'), value: locale.t('status.schedule.draft') },
  { label: locale.t('page.plans.availableAds'), value: adOptions.value.length },
  { label: locale.t('page.plans.availableMaterials'), value: materialOptions.value.length },
  { label: locale.t('page.plans.availableDevices'), value: deviceOptions.value.length }
])

const rules = computed<FormRules>(() => ({
  planName: [{ required: true, message: locale.t('page.plans.planNameRequired'), trigger: 'blur' }],
  adId: [{ required: true, message: locale.t('page.plans.adRequired'), trigger: 'change' }],
  startTime: [
    { required: true, message: locale.t('page.plans.startRequired'), trigger: 'change' },
    { validator: validateStartTime, trigger: 'change' }
  ],
  endTime: [
    { required: true, message: locale.t('page.plans.endRequired'), trigger: 'change' },
    { validator: validateEndTime, trigger: 'change' }
  ],
  materialIds: [{ type: 'array', required: true, message: locale.t('page.plans.materialsRequired'), trigger: 'change' }],
  deviceIds: [{ type: 'array', required: true, message: locale.t('page.plans.devicesRequired'), trigger: 'change' }]
}))

function buildPayload(): AdPlanPayload {
  if (!form.adId) {
    throw new Error(locale.t('page.plans.chooseAdFirst'))
  }
  if (!form.materialIds.length) {
    throw new Error(locale.t('page.plans.chooseOneMaterial'))
  }
  if (!form.deviceIds.length) {
    throw new Error(locale.t('page.plans.chooseOneDevice'))
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

function parseDateTime(value?: string) {
  if (!value) {
    return undefined
  }
  const date = new Date(value.replace(/-/g, '/'))
  return Number.isNaN(date.getTime()) ? undefined : date
}

function startOfToday(date = new Date()) {
  const copy = new Date(date)
  copy.setHours(0, 0, 0, 0)
  return copy
}

function range(end: number, start = 0) {
  return Array.from({ length: Math.max(end - start, 0) }, (_, index) => index + start)
}

function isSameDay(left?: Date, right = new Date()) {
  return Boolean(left && startOfToday(left).getTime() === startOfToday(right).getTime())
}

function selectedStartDate() {
  return parseDateTime(form.startTime)
}

function selectedEndDate() {
  return parseDateTime(form.endTime)
}

function currentDateTimeFloor() {
  const date = new Date()
  return date
}

function disablePastDate(date: Date) {
  return date.getTime() < startOfToday().getTime()
}

function disableEndDate(date: Date) {
  const start = selectedStartDate()
  return start ? date.getTime() < startOfToday(start).getTime() : disablePastDate(date)
}

function disabledStartHours() {
  if (isEdit.value || !isSameDay(selectedStartDate())) {
    return []
  }
  return range(currentDateTimeFloor().getHours())
}

function disabledStartMinutes(hour: number) {
  const now = currentDateTimeFloor()
  if (isEdit.value || !isSameDay(selectedStartDate()) || hour !== now.getHours()) {
    return []
  }
  return range(now.getMinutes())
}

function disabledStartSeconds(hour: number, minute: number) {
  const now = currentDateTimeFloor()
  if (isEdit.value || !isSameDay(selectedStartDate()) || hour !== now.getHours() || minute !== now.getMinutes()) {
    return []
  }
  return range(now.getSeconds())
}

function disabledEndHours() {
  const start = selectedStartDate()
  const end = selectedEndDate()
  if (!start || !isSameDay(end, start)) {
    return []
  }
  return range(start.getHours())
}

function disabledEndMinutes(hour: number) {
  const start = selectedStartDate()
  const end = selectedEndDate()
  if (!start || !isSameDay(end, start) || hour !== start.getHours()) {
    return []
  }
  return range(start.getMinutes())
}

function disabledEndSeconds(hour: number, minute: number) {
  const start = selectedStartDate()
  const end = selectedEndDate()
  if (!start || !isSameDay(end, start) || hour !== start.getHours() || minute !== start.getMinutes()) {
    return []
  }
  return range(start.getSeconds() + 1)
}

function validateStartTime(_rule: unknown, value: string, callback: (error?: Error) => void) {
  const start = parseDateTime(value)
  if (!value || isEdit.value) {
    callback()
    return
  }
  callback(start && start.getTime() < currentDateTimeFloor().getTime() ? new Error(locale.t('page.plans.startNotPast')) : undefined)
}

function validateEndTime(_rule: unknown, value: string, callback: (error?: Error) => void) {
  const start = selectedStartDate()
  const end = parseDateTime(value)
  if (!start || !end) {
    callback()
    return
  }
  callback(end.getTime() <= start.getTime() ? new Error(locale.t('page.plans.endAfterStart')) : undefined)
}

function getAdLabel(ad: AdOrder) {
  return `${ad.adName}（${ad.adCode}）`
}

function getMaterialLabel(material: AdMaterial) {
  return `${material.materialName}（${material.materialCode}）`
}

function getMaterialTypeLabel(value: string) {
  return locale.t(`status.materialType.${value}`, materialTypeOptions.find((item) => item.value === value)?.label || value)
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
    ElMessage.success(isEdit.value ? locale.t('page.plans.saved') : locale.t('page.plans.created'))
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

watch(
  () => form.startTime,
  () => {
    if (form.endTime && selectedStartDate() && selectedEndDate() && selectedEndDate()!.getTime() <= selectedStartDate()!.getTime()) {
      form.endTime = ''
    }
    formRef.value?.validateField('endTime')
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
