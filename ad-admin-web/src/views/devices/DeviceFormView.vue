<template>
  <AppPage :eyebrow="locale.t('page.deviceDelivery')" :title="isEdit ? locale.t('page.devices.editTitle') : locale.t('page.devices.createTitle')" :stats="stats">
    <el-form ref="formRef" class="detail-form" :model="form" :rules="rules" label-width="120px">
      <el-form-item :label="locale.t('page.devices.deviceNumber')" prop="deviceCode">
        <el-input v-model="form.deviceCode" maxlength="32" :placeholder="locale.t('page.devices.deviceCodePlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.devices.name')" prop="deviceName">
        <el-input v-model="form.deviceName" maxlength="80" show-word-limit :placeholder="locale.t('page.devices.deviceNamePlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.devices.floor')">
        <el-input v-model="form.floorNo" :placeholder="locale.t('page.devices.floorPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.devices.screenSize')">
        <el-input v-model="form.screenSize" :placeholder="locale.t('page.devices.screenPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.devices.resolution')">
        <el-input v-model="form.resolution" :placeholder="locale.t('page.devices.resolutionPlaceholder')" />
      </el-form-item>
      <!-- <el-form-item :label="locale.t('page.devices.ip')">
        <el-input v-model="form.ipAddress" :placeholder="locale.t('page.devices.ipPlaceholder')" />
      </el-form-item>
      <el-form-item :label="locale.t('page.devices.mac')">
        <el-input v-model="form.macAddress" :placeholder="locale.t('page.devices.macPlaceholder')" />
      </el-form-item> -->
      <el-form-item class="form-actions">
        <el-button @click="router.back()">{{ locale.t('common.back') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ saving ? locale.t('page.plans.saving') : locale.t('page.devices.saveDevice') }}
        </el-button>
      </el-form-item>
    </el-form>
  </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import AppPage from '@/components/AppPage.vue'
import { createDevice, fetchDeviceDetail, updateDevice, type AdDevicePayload } from '@/api/devices'
import { useLocaleStore } from '@/stores/locale'

const route = useRoute()
const router = useRouter()
const locale = useLocaleStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const isEdit = computed(() => Boolean(route.params.id))

const form = reactive({
  deviceCode: '',
  deviceName: '',
  floorNo: '',
  screenSize: '',
  resolution: '',
  ipAddress: '',
  macAddress: ''
})

const stats = computed(() => [
  { label: locale.t('page.ads.formMode'), value: isEdit.value ? locale.t('page.ads.editMode') : locale.t('page.ads.newMode') },
  { label: locale.t('page.ads.defaultStatus'), value: locale.t('status.device.active') },
  { label: locale.t('page.devices.defaultOnline'), value: locale.t('status.online.offline') },
  { label: locale.t('page.devices.defaultFault'), value: locale.t('status.fault.normal') }
])

const rules = computed<FormRules>(() => ({
  deviceCode: [{ required: true, message: locale.t('page.devices.deviceCodeRequired'), trigger: 'blur' }],
  deviceName: [{ required: true, message: locale.t('page.devices.deviceNameRequired'), trigger: 'blur' }]
}))

function buildPayload(): AdDevicePayload {
  return {
    deviceCode: form.deviceCode,
    deviceName: form.deviceName,
    floorNo: form.floorNo,
    screenSize: form.screenSize,
    resolution: form.resolution,
    ipAddress: form.ipAddress,
    macAddress: form.macAddress
  }
}

async function loadDetail() {
  if (!isEdit.value) {
    return
  }
  const result = await fetchDeviceDetail(Number(route.params.id))
  const device = result.data
  form.deviceCode = device.deviceCode || ''
  form.deviceName = device.deviceName
  form.floorNo = device.floorNo || ''
  form.screenSize = device.screenSize || ''
  form.resolution = device.resolution || ''
  form.ipAddress = device.ipAddress || ''
  form.macAddress = device.macAddress || ''
}

async function handleSave() {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  saving.value = true
  try {
    const result = isEdit.value
      ? await updateDevice(Number(route.params.id), buildPayload())
      : await createDevice(buildPayload())
    ElMessage.success(isEdit.value ? locale.t('page.devices.saved') : locale.t('page.devices.created'))
    router.push(`/devices/${result.data.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(loadDetail)
</script>
