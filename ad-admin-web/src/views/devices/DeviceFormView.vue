<template>
  <AppPage eyebrow="设备投放" :title="isEdit ? '编辑设备' : '新建设备'" :stats="stats">
    <el-form ref="formRef" class="detail-form" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="设备名称" prop="deviceName">
        <el-input v-model="form.deviceName" maxlength="80" show-word-limit placeholder="如：上海万象城北门大屏" />
      </el-form-item>
      <el-form-item label="楼宇ID">
        <el-input-number v-model="form.buildingId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="楼层">
        <el-input v-model="form.floorNo" placeholder="如 B1 / 1F / 12F" />
      </el-form-item>
      <el-form-item label="屏幕尺寸">
        <el-input v-model="form.screenSize" placeholder="如 55寸 / 86寸 / LED大屏" />
      </el-form-item>
      <el-form-item label="分辨率">
        <el-input v-model="form.resolution" placeholder="如 1920x1080" />
      </el-form-item>
      <el-form-item label="IP地址">
        <el-input v-model="form.ipAddress" placeholder="如 192.168.1.18" />
      </el-form-item>
      <el-form-item label="MAC地址">
        <el-input v-model="form.macAddress" placeholder="如 AA:BB:CC:DD:EE:FF" />
      </el-form-item>
      <el-form-item class="form-actions">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存设备' }}
        </el-button>
        <el-button @click="router.push('/devices')">取消</el-button>
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

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const isEdit = computed(() => Boolean(route.params.id))

const form = reactive({
  deviceName: '',
  buildingId: undefined as number | undefined,
  floorNo: '',
  screenSize: '',
  resolution: '',
  ipAddress: '',
  macAddress: ''
})

const stats = computed(() => [
  { label: '表单模式', value: isEdit.value ? '编辑' : '新建' },
  { label: '默认状态', value: '启用' },
  { label: '默认在线', value: '离线' },
  { label: '默认故障', value: '正常' }
])

const rules: FormRules = {
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

function buildPayload(): AdDevicePayload {
  return {
    deviceName: form.deviceName,
    buildingId: form.buildingId,
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
  form.deviceName = device.deviceName
  form.buildingId = device.buildingId
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
    ElMessage.success(isEdit.value ? '设备已保存' : '设备已创建')
    router.push(`/devices/${result.data.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(loadDetail)
</script>
