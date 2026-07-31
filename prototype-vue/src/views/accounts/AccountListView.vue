<template>
  <section>
    <div class="page-title">
      <div>
        <h1>账号管理</h1>
        <p class="page-subtitle">按角色、状态和区域查看账号，并在右侧面板快速确认账号范围与权限边界。</p>
      </div>
    </div>

    <div class="permission-note">
      当前页面以“查看和演示角色结构”为主，暂未开放真实新增/删除账号动作。
    </div>

    <div class="card filter-card">
      <div class="filter-grid">
        <input v-model.trim="filters.keyword" class="input" placeholder="搜索账号 / 姓名 / 区域 / 主体" />
        <select v-model="filters.role" class="select">
          <option value="">全部角色</option>
          <option v-for="item in roleOptions" :key="item" :value="item">
            {{ roleLabels[item] }}
          </option>
        </select>
        <select v-model="filters.status" class="select">
          <option value="">全部状态</option>
          <option value="active">正常</option>
          <option value="disabled">禁用</option>
        </select>
        <input v-model.trim="filters.scope" class="input" placeholder="按区域 / 主体筛选" />
      </div>
    </div>

    <div class="card-grid">
      <div class="card">
        <div>账号总数</div>
        <div class="metric-value">{{ filteredUsers.length }}</div>
      </div>
      <div class="card">
        <div>正常账号</div>
        <div class="metric-value">{{ activeCount }}</div>
      </div>
      <div class="card">
        <div>管理类角色</div>
        <div class="metric-value">{{ adminLikeCount }}</div>
      </div>
      <div class="card">
        <div>业务类角色</div>
        <div class="metric-value">{{ businessLikeCount }}</div>
      </div>
    </div>

    <div v-if="filteredUsers.length === 0" class="empty-state">
      当前筛选条件下没有账号数据，请调整角色、状态或区域筛选条件。
    </div>

    <div v-else class="content-columns">
      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>账号</th>
              <th>姓名</th>
              <th>角色</th>
              <th>状态</th>
              <th>区域 / 主体</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="user in filteredUsers"
              :key="user.id"
              :class="{ selected: selectedUser?.id === user.id }"
              @click="selectedUserId = user.id"
            >
              <td>{{ user.username }}</td>
              <td>{{ user.name }}</td>
              <td>{{ roleLabels[user.role] }}</td>
              <td><span class="status" :class="`status-${user.status}`">{{ user.status === 'active' ? '正常' : '禁用' }}</span></td>
              <td>{{ user.region || user.companyId || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card drawer-card">
        <template v-if="selectedUser">
          <div class="drawer-head">
            <div>
              <h3>{{ selectedUser.name }}</h3>
              <p class="muted">{{ selectedUser.username }} / {{ roleLabels[selectedUser.role] }}</p>
            </div>
          </div>

          <table class="table compact">
            <tbody>
              <tr><th>角色</th><td>{{ roleLabels[selectedUser.role] }}</td></tr>
              <tr><th>状态</th><td>{{ selectedUser.status === 'active' ? '正常' : '禁用' }}</td></tr>
              <tr><th>区域</th><td>{{ selectedUser.region || '-' }}</td></tr>
              <tr><th>主体</th><td>{{ selectedUser.companyId || '-' }}</td></tr>
            </tbody>
          </table>

          <div class="editor-panel">
            <h4>角色说明</h4>
            <p class="muted">{{ roleDescriptions[selectedUser.role] }}</p>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { mockUsers } from '@/mock/users'
import { roleLabels, type RoleCode } from '@/config/roles'

const filters = reactive({
  keyword: '',
  role: '',
  status: '',
  scope: ''
})

const selectedUserId = ref('')
const roleOptions = mockUsers.map((item) => item.role).filter((value, index, array) => array.indexOf(value) === index)

const roleDescriptions: Record<RoleCode, string> = {
  super_admin: '负责全局账号、系统配置和流程总控。',
  developer: '负责开发和技术调试，不处理业务数据。',
  ops: '负责设备监控、故障处理和大屏运行维护。',
  area_admin: '负责区域内账号、广告和设备的统筹。',
  business: '负责广告运营、计划管理和业务推进。',
  reviewer: '负责素材审核和合规把关。',
  finance: '负责账单、结算和发票相关操作。',
  analyst: '负责投放、转化和设备数据分析。',
  agent: '负责代理广告主完成投放业务。',
  advertiser: '负责广告需求发起和投放结果查看。',
  service: '负责咨询、反馈和工单跟进。',
  audit: '负责日志追踪、权限核查和审计分析。',
  audience: '负责浏览广告和发起互动反馈。'
}

const filteredUsers = computed(() =>
  mockUsers.filter((user) => {
    const keyword = filters.keyword.toLowerCase()
    const scope = `${user.region || ''} ${user.companyId || ''}`.toLowerCase()
    const hitKeyword =
      !keyword ||
      user.username.toLowerCase().includes(keyword) ||
      user.name.toLowerCase().includes(keyword) ||
      scope.includes(keyword)
    const hitRole = !filters.role || user.role === filters.role
    const hitStatus = !filters.status || user.status === filters.status
    const hitScope = !filters.scope || scope.includes(filters.scope.toLowerCase())
    return hitKeyword && hitRole && hitStatus && hitScope
  })
)

const selectedUser = computed(
  () => filteredUsers.value.find((item) => item.id === selectedUserId.value) ?? filteredUsers.value[0]
)

const activeCount = computed(() => filteredUsers.value.filter((item) => item.status === 'active').length)
const adminLikeCount = computed(() =>
  filteredUsers.value.filter((item) => ['super_admin', 'area_admin', 'audit'].includes(item.role)).length
)
const businessLikeCount = computed(() =>
  filteredUsers.value.filter((item) => ['business', 'agent', 'advertiser', 'reviewer', 'finance'].includes(item.role)).length
)

watch(filteredUsers, (list) => {
  if (!list.length) {
    selectedUserId.value = ''
    return
  }
  if (!list.find((item) => item.id === selectedUserId.value)) {
    selectedUserId.value = list[0].id
  }
}, { immediate: true })
</script>
