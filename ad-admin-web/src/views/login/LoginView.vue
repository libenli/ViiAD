<template>
  <main class="login-page viiad-login">
    <LanguageSwitch class="login-language" />

    <section class="login-stage">
      <div class="scene-panel">
        <div class="scene-frame">
          <div class="scene-grid"></div>
          <div class="scene-orbit scene-orbit-a"></div>
          <div class="scene-orbit scene-orbit-b"></div>
          <div class="scene-copy">
            <!-- <p>{{ t.sceneEyebrow }}</p> -->
            <h1>{{ t.sceneTitle }}</h1>
            <span>{{ t.sceneDesc }}</span>
          </div>
        </div>
      </div>

      <el-form class="login-card" :model="passwordForm" label-position="top" @submit.prevent @keydown.enter.prevent="handleSubmit">
        <header class="login-brand">
          <div class="login-logo">VA</div>
          <div>
            <strong>ViiAD</strong>
            <span>{{ t.systemName }}</span>
          </div>
        </header>

        <div class="login-tabs">
          <button :class="{ active: activeTab === 'password' }" type="button" @click="activeTab = 'password'">
            {{ t.passwordLogin }}
          </button>
          <button :class="{ active: activeTab === 'code' }" type="button" @click="activeTab = 'code'">
            {{ t.codeLogin }}
          </button>
        </div>

        <div v-if="activeTab === 'password'" class="login-form-body">
          <el-form-item :label="t.account">
            <el-input v-model.trim="passwordForm.username" size="large" :placeholder="t.accountPlaceholder" clearable />
          </el-form-item>
          <el-form-item :label="t.password">
            <el-input v-model="passwordForm.password" size="large" type="password" show-password :placeholder="t.passwordPlaceholder" />
          </el-form-item>
          <el-form-item :label="t.captcha">
            <div class="captcha-row">
              <el-input v-model.trim="passwordForm.captcha" size="large" :placeholder="t.captchaPlaceholder" />
              <button class="captcha-preview" type="button" :disabled="captchaLoading" @click="loadCaptcha">
                <img v-if="captchaImage" :src="captchaImage" :alt="t.captcha" />
                <span v-else>{{ captchaLoading ? '...' : '2k9w' }}</span>
              </button>
            </div>
          </el-form-item>
          <div class="login-options">
            <el-checkbox v-model="passwordForm.rememberMe">{{ t.rememberMe }}</el-checkbox>
            <button type="button" @click="openForgotDialog">{{ t.forgotPassword }}</button>
          </div>
        </div>

        <div v-else class="login-form-body">
          <div class="code-type-switch">
            <button :class="{ active: codeForm.type === 'phone' }" type="button" @click="switchCodeType('phone')">
              {{ t.phoneCode }}
            </button>
            <button :class="{ active: codeForm.type === 'email' }" type="button" @click="switchCodeType('email')">
              {{ t.emailCode }}
            </button>
          </div>

          <el-form-item v-if="codeForm.type === 'phone'" :label="t.countryCode">
            <el-select v-model="codeForm.countryCode" size="large" filterable>
              <el-option v-for="item in countryOptions" :key="item.value" :label="item.label[language]" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item :label="codeForm.type === 'phone' ? t.phone : t.email">
            <el-input v-model.trim="codeForm.target" size="large" :placeholder="codeForm.type === 'phone' ? t.phonePlaceholder : t.emailPlaceholder" clearable />
          </el-form-item>

          <el-form-item :label="t.verifyCode">
            <div class="captcha-row">
              <el-input v-model.trim="codeForm.code" size="large" maxlength="6" :placeholder="codeForm.type === 'phone' ? t.smsCodePlaceholder : t.emailCodePlaceholder" />
              <el-button class="send-code-button" :disabled="countdown > 0" :loading="sendingCode" @click="sendCode">
                {{ countdown > 0 ? t.resendIn.replace('{seconds}', String(countdown)) : t.sendCode }}
              </el-button>
            </div>
          </el-form-item>
          <div class="login-options single">
            <button type="button" @click="openForgotDialog">{{ t.forgotPassword }}</button>
          </div>
        </div>

        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleSubmit">
          {{ loading ? t.loggingIn : t.login }}
        </el-button>

        <footer class="login-footer">
          {{ common.copyright }}<br />
          {{ common.allRightsReserved }}
        </footer>
      </el-form>
    </section>

    <el-dialog v-model="roleDialogVisible" :title="t.selectRoleTitle" width="460px" :close-on-click-modal="false" :show-close="false">
      <p class="role-dialog-desc">{{ t.selectRoleDesc }}</p>
      <div class="role-option-grid">
        <button
          v-for="role in pendingRoles"
          :key="role.roleId"
          :class="{ active: selectedRoleId === role.roleId }"
          type="button"
          @click="selectedRoleId = role.roleId"
        >
          <strong>{{ getRoleName(role) }}</strong>
          <span>{{ role.roleCode }}</span>
        </button>
      </div>
      <template #footer>
        <el-button type="primary" :loading="selectingRole" :disabled="!selectedRoleId" @click="handleSelectRole">
          {{ t.enterSystem }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="forgotDialogVisible" :title="t.forgotPasswordTitle" width="520px" class="forgot-dialog" :close-on-click-modal="false">
      <div class="code-type-switch forgot-type-switch">
        <button :class="{ active: forgotForm.type === 'phone' }" type="button" @click="switchForgotType('phone')">
          {{ t.phoneCode }}
        </button>
        <button :class="{ active: forgotForm.type === 'email' }" type="button" @click="switchForgotType('email')">
          {{ t.emailCode }}
        </button>
      </div>

      <el-form label-position="top" @submit.prevent @keydown.enter.prevent="handleForgotPrimary">
        <el-form-item v-if="forgotForm.type === 'phone'" :label="t.countryCode">
          <el-select v-model="forgotForm.countryCode" size="large" filterable :disabled="forgotStep === 'reset'">
            <el-option v-for="item in countryOptions" :key="item.value" :label="item.label[language]" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item :label="forgotForm.type === 'phone' ? t.phone : t.email">
          <el-input
            v-model.trim="forgotForm.target"
            size="large"
            :placeholder="forgotForm.type === 'phone' ? t.phonePlaceholder : t.emailPlaceholder"
            :disabled="forgotStep === 'reset'"
            clearable
          />
        </el-form-item>

        <el-form-item v-if="forgotStep === 'verify'" :label="t.verifyCode">
          <div class="captcha-row">
            <el-input v-model.trim="forgotForm.code" size="large" maxlength="6" :placeholder="forgotForm.type === 'phone' ? t.smsCodePlaceholder : t.emailCodePlaceholder" />
            <el-button class="send-code-button" :disabled="forgotCountdown > 0" :loading="forgotSendingCode" @click="sendForgotCode">
              {{ forgotCountdown > 0 ? t.resendIn.replace('{seconds}', String(forgotCountdown)) : t.sendCode }}
            </el-button>
          </div>
        </el-form-item>

        <template v-else>
          <el-form-item :label="t.newPassword">
            <el-input v-model="forgotForm.password" size="large" type="password" show-password :placeholder="t.newPasswordPlaceholder" />
          </el-form-item>
          <el-form-item :label="t.confirmPassword">
            <el-input v-model="forgotForm.confirmPassword" size="large" type="password" show-password :placeholder="t.confirmPasswordPlaceholder" />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="forgotDialogVisible = false">{{ t.cancel }}</el-button>
        <el-button type="primary" :loading="forgotLoading" @click="handleForgotPrimary">
          {{ forgotStep === 'verify' ? t.nextStep : t.resetPassword }}
        </el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  codeLogin,
  fetchCaptcha,
  passwordLogin,
  resetForgotPassword,
  selectRole,
  sendForgotPasswordCode,
  sendVerificationCode,
  validateForgotPasswordCode,
  type AuthRoleOption,
  type LoginResult
} from '@/api/auth'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { countryCodeOptions } from '@/locales/country-codes'
import { getFirstAccessiblePath, normalizeRedirect } from '@/router'
import { useLocaleStore } from '@/stores/locale'
import { useUserStore } from '@/stores/user'

type CodeType = 'phone' | 'email'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const locale = useLocaleStore()
const activeTab = ref<'password' | 'code'>('password')
const loading = ref(false)
const sendingCode = ref(false)
const captchaLoading = ref(false)
const selectingRole = ref(false)
const roleDialogVisible = ref(false)
const forgotDialogVisible = ref(false)
const forgotSendingCode = ref(false)
const forgotLoading = ref(false)
const forgotStep = ref<'verify' | 'reset'>('verify')
const pendingTempToken = ref('')
const pendingRoles = ref<AuthRoleOption[]>([])
const selectedRoleId = ref<number>()
const countdown = ref(0)
const forgotCountdown = ref(0)
const captchaUuid = ref('')
const captchaImage = ref('')
const forgotResetToken = ref('')
let timer: number | undefined
let forgotTimer: number | undefined

const passwordForm = reactive({
  username: 'admin@mysher.com',
  password: 'admin123',
  captcha: '',
  rememberMe: false
})

const codeForm = reactive({
  type: 'phone' as CodeType,
  countryCode: '+86',
  target: '',
  code: ''
})

const forgotForm = reactive({
  type: 'phone' as CodeType,
  countryCode: '+86',
  target: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const language = computed(() => locale.language)
const common = computed(() => locale.messages.common)
const t = computed(() => locale.messages.login)
const countryOptions = countryCodeOptions

function handleSubmit() {
  if (activeTab.value === 'password') {
    handlePasswordLogin()
    return
  }
  handleCodeLogin()
}

function getRoleName(role: AuthRoleOption) {
  return locale.t(`role.${role.roleCode}.name`, role.roleName)
}

async function handlePasswordLogin() {
  if (loading.value) {
    return
  }
  if (!passwordForm.username || !passwordForm.password) {
    ElMessage.warning(t.value.passwordRequired)
    return
  }
  if (!passwordForm.captcha || !captchaUuid.value) {
    ElMessage.warning(t.value.captchaPlaceholder)
    return
  }
  loading.value = true
  try {
    const result = await passwordLogin({
      username: passwordForm.username,
      password: passwordForm.password,
      captchaUuid: captchaUuid.value,
      captchaCode: passwordForm.captcha,
      locale: language.value
    })
    await handleLoginResult(result.data)
  } catch (error) {
    ElMessage.error(t.value.loginFailed)
    passwordForm.captcha = ''
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

function handleCodeLogin() {
  if (!codeForm.target || !codeForm.code) {
    ElMessage.warning(t.value.codeTargetRequired)
    return
  }
  if (loading.value) {
    return
  }
  loading.value = true
  codeLogin({
    type: codeForm.type,
    countryCode: codeForm.countryCode,
    target: codeForm.target,
    code: codeForm.code,
    locale: language.value
  }).then(async (result) => {
    await handleLoginResult(result.data)
  }).catch(() => {
    ElMessage.error(t.value.codeLoginFailed)
  }).finally(() => {
    loading.value = false
  })
}

async function handleLoginResult(data: LoginResult) {
  if (data.needRoleSelect) {
    pendingTempToken.value = data.tempToken || ''
    pendingRoles.value = data.roles || []
    selectedRoleId.value = pendingRoles.value[0]?.roleId
    roleDialogVisible.value = true
    return
  }
  if (!data.token) {
    ElMessage.error(t.value.loginFailed)
    return
  }
  user.setToken(data.token)
  await user.fetchUserInfo()
  router.replace(normalizeRedirect(route.query.redirect) || getFirstAccessiblePath(user))
}

async function handleSelectRole() {
  if (!pendingTempToken.value || !selectedRoleId.value) {
    return
  }
  selectingRole.value = true
  try {
    const result = await selectRole({
      tempToken: pendingTempToken.value,
      roleId: selectedRoleId.value
    })
    roleDialogVisible.value = false
    await handleLoginResult(result.data)
  } finally {
    selectingRole.value = false
  }
}

async function sendCode() {
  if (!codeForm.target) {
    ElMessage.warning(t.value.codeTargetRequired)
    return
  }
  sendingCode.value = true
  try {
    await sendVerificationCode({
      type: codeForm.type,
      countryCode: codeForm.countryCode,
      target: codeForm.target,
      scene: 'login',
      locale: language.value
    })
    ElMessage.success(t.value.codeSent)
    startCountdown()
  } finally {
    sendingCode.value = false
  }
}

function switchCodeType(type: CodeType) {
  if (codeForm.type === type) {
    return
  }
  codeForm.type = type
  codeForm.target = ''
  codeForm.code = ''
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const result = await fetchCaptcha()
    captchaUuid.value = result.data.uuid
    captchaImage.value = result.data.image
  } finally {
    captchaLoading.value = false
  }
}

function startCountdown() {
  window.clearInterval(timer)
  countdown.value = 60
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(timer)
      timer = undefined
    }
  }, 1000)
}

function openForgotDialog() {
  forgotForm.type = activeTab.value === 'code' ? codeForm.type : 'email'
  forgotForm.countryCode = codeForm.countryCode || '+86'
  forgotForm.target = activeTab.value === 'code' ? codeForm.target : passwordForm.username
  forgotForm.code = ''
  forgotForm.password = ''
  forgotForm.confirmPassword = ''
  forgotResetToken.value = ''
  forgotStep.value = 'verify'
  forgotDialogVisible.value = true
}

function switchForgotType(type: CodeType) {
  if (forgotStep.value === 'reset') {
    return
  }
  forgotForm.type = type
  forgotForm.target = ''
  forgotForm.code = ''
}

async function sendForgotCode() {
  if (!forgotForm.target) {
    ElMessage.warning(t.value.codeTargetRequired)
    return
  }
  forgotSendingCode.value = true
  try {
    await sendForgotPasswordCode({
      type: forgotForm.type,
      countryCode: forgotForm.countryCode,
      target: forgotForm.target,
      locale: language.value
    })
    ElMessage.success(t.value.codeSent)
    startForgotCountdown()
  } finally {
    forgotSendingCode.value = false
  }
}

async function handleForgotPrimary() {
  if (forgotStep.value === 'verify') {
    await validateForgotCode()
    return
  }
  await resetForgot()
}

async function validateForgotCode() {
  if (!forgotForm.target || !forgotForm.code) {
    ElMessage.warning(t.value.forgotCodeRequired)
    return
  }
  forgotLoading.value = true
  try {
    const result = await validateForgotPasswordCode({
      type: forgotForm.type,
      countryCode: forgotForm.countryCode,
      target: forgotForm.target,
      code: forgotForm.code,
      locale: language.value
    })
    forgotResetToken.value = result.data.resetToken
    forgotStep.value = 'reset'
  } finally {
    forgotLoading.value = false
  }
}

async function resetForgot() {
  if (!forgotForm.password || !forgotForm.confirmPassword) {
    ElMessage.warning(t.value.newPasswordRequired)
    return
  }
  if (forgotForm.password.length < 6) {
    ElMessage.warning(t.value.newPasswordMin)
    return
  }
  if (forgotForm.password !== forgotForm.confirmPassword) {
    ElMessage.warning(t.value.passwordNotMatch)
    return
  }
  forgotLoading.value = true
  try {
    await resetForgotPassword({
      resetToken: forgotResetToken.value,
      password: forgotForm.password
    })
    ElMessage.success(t.value.resetPasswordSuccess)
    forgotDialogVisible.value = false
    passwordForm.username = forgotForm.type === 'email' ? forgotForm.target : passwordForm.username
    passwordForm.password = ''
    passwordForm.captcha = ''
    activeTab.value = 'password'
    loadCaptcha()
  } finally {
    forgotLoading.value = false
  }
}

function startForgotCountdown() {
  window.clearInterval(forgotTimer)
  forgotCountdown.value = 60
  forgotTimer = window.setInterval(() => {
    forgotCountdown.value -= 1
    if (forgotCountdown.value <= 0) {
      window.clearInterval(forgotTimer)
      forgotTimer = undefined
    }
  }, 1000)
}

locale.initLanguage()

onMounted(loadCaptcha)

onBeforeUnmount(() => {
  window.clearInterval(timer)
  window.clearInterval(forgotTimer)
})
</script>

<style scoped>
.viiad-login {
  position: relative;
  align-items: stretch;
  padding: 28px;
  background:
    radial-gradient(circle at 20% 20%, rgba(57, 198, 214, 0.14), transparent 26%),
    radial-gradient(circle at 78% 24%, rgba(99, 212, 144, 0.12), transparent 24%),
    linear-gradient(135deg, #071016 0%, #081923 54%, #061017 100%);
}

.login-language {
  position: fixed;
  top: 26px;
  right: 34px;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #7dddec;
  font-size: 15px;
}

.login-language button {
  padding: 0;
  border: 0;
  color: #8bb8c8;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.login-language button.active,
.login-language button:hover {
  color: #ffffff;
}

.login-stage {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 456px;
  width: min(1440px, 100%);
  min-height: calc(100vh - 56px);
  margin: 0 auto;
  border: 1px solid rgba(125, 193, 215, 0.22);
  background: rgba(4, 13, 19, 0.62);
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.26);
}

.scene-panel {
  min-width: 0;
  padding: 22px;
}

.scene-frame {
  position: relative;
  display: grid;
  place-items: center;
  height: 100%;
  min-height: 680px;
  overflow: hidden;
  border: 1px solid rgba(125, 193, 215, 0.26);
  background:
    linear-gradient(135deg, rgba(57, 198, 214, 0.12), transparent 34%),
    radial-gradient(circle at 70% 64%, rgba(99, 212, 144, 0.12), transparent 28%),
    rgba(6, 18, 25, 0.92);
}

.scene-grid {
  position: absolute;
  inset: 0;
  opacity: 0.36;
  background-image:
    linear-gradient(rgba(125, 193, 215, 0.12) 1px, transparent 1px),
    linear-gradient(90deg, rgba(125, 193, 215, 0.12) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: radial-gradient(circle at center, #000 0%, transparent 72%);
}

.scene-orbit {
  position: absolute;
  border: 1px solid rgba(57, 198, 214, 0.3);
  border-radius: 999px;
  filter: drop-shadow(0 0 18px rgba(57, 198, 214, 0.22));
}

.scene-orbit-a {
  width: 44%;
  height: 44%;
  transform: rotate(-18deg);
}

.scene-orbit-b {
  width: 62%;
  height: 24%;
  border-color: rgba(99, 212, 144, 0.22);
  transform: rotate(16deg);
}

.scene-copy {
  position: relative;
  z-index: 1;
  max-width: 520px;
  padding: 44px;
  text-align: center;
  border: 1px solid rgba(125, 193, 215, 0.22);
  background: rgba(7, 20, 28, 0.72);
  backdrop-filter: blur(10px);
}

.scene-copy p {
  margin: 0 0 16px;
  color: #7dddec;
  letter-spacing: 0.16em;
}

.scene-copy h1 {
  margin: 0;
  color: #ffffff;
  font-size: 38px;
  line-height: 1.25;
}

.scene-copy span {
  display: block;
  margin-top: 18px;
  color: #9fb9c4;
  line-height: 1.8;
}

.login-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 680px;
  padding: 72px 48px 40px;
  border-left: 1px solid rgba(125, 193, 215, 0.2);
  background: rgba(7, 16, 22, 0.94);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 54px;
}

.login-logo {
  display: grid;
  place-items: center;
  width: 74px;
  height: 74px;
  border: 4px solid #25b7e7;
  color: #ffffff;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.login-brand strong {
  display: block;
  color: #ffffff;
  font-size: 32px;
  line-height: 1.2;
}

.login-brand span {
  display: block;
  margin-top: 6px;
  color: #e8f5f8;
  font-size: 24px;
}

.login-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.login-tabs button,
.code-type-switch button {
  height: 42px;
  border: 0;
  color: #7fb2c4;
  background: transparent;
  cursor: pointer;
  font-size: 21px;
}

.login-tabs button.active,
.login-tabs button:hover,
.code-type-switch button.active,
.code-type-switch button:hover {
  color: #58cce3;
}

.login-tabs button.active::after,
.code-type-switch button.active::after {
  display: block;
  width: 34px;
  height: 2px;
  margin: 8px auto 0;
  background: #58cce3;
  content: "";
}

.login-form-body {
  display: grid;
  gap: 2px;
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 12px;
  width: 100%;
}

.captcha-preview {
  display: grid;
  place-items: center;
  height: 40px;
  border: 1px solid rgba(120, 188, 205, 0.28);
  color: #f5b544;
  background:
    linear-gradient(12deg, transparent 43%, rgba(125, 193, 215, 0.38) 44%, transparent 46%),
    rgba(8, 24, 34, 0.96);
  cursor: pointer;
  font-family: "Consolas", monospace;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-shadow: 2px 1px #4aa3ff, -1px -1px rgba(255, 255, 255, 0.5);
}

.captcha-preview img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-preview:disabled {
  cursor: wait;
  opacity: 0.72;
}

.send-code-button {
  width: 132px;
  height: 40px;
}

.code-type-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 6px;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  margin: -2px 0 22px;
}

.login-options.single {
  justify-content: flex-end;
}

.login-options button {
  border: 0;
  color: #58cce3;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
}

.login-button {
  width: 100%;
  height: 48px;
  margin-top: 2px;
  font-size: 18px;
}

.login-footer {
  margin-top: auto;
  padding-top: 54px;
  color: #8da8b5;
  text-align: center;
  line-height: 1.7;
  letter-spacing: 0.02em;
}

.role-dialog-desc {
  margin: 0 0 18px;
  color: #a8c3cf;
  line-height: 1.7;
}

.role-option-grid {
  display: grid;
  gap: 12px;
}

.role-option-grid button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 58px;
  padding: 12px 14px;
  border: 1px solid rgba(125, 193, 215, 0.22);
  border-radius: 8px;
  color: #d8e9ef;
  background: rgba(8, 24, 34, 0.92);
  cursor: pointer;
  text-align: left;
}

.role-option-grid button.active,
.role-option-grid button:hover {
  border-color: rgba(57, 198, 214, 0.72);
  background: rgba(18, 48, 60, 0.98);
}

.role-option-grid strong {
  font-size: 16px;
}

.role-option-grid span {
  color: #7dddec;
  font-size: 12px;
  text-transform: uppercase;
}

@media (max-width: 1100px) {
  .login-stage {
    grid-template-columns: 1fr;
  }

  .scene-panel {
    display: none;
  }

  .login-card {
    border-left: 0;
  }
}
</style>
