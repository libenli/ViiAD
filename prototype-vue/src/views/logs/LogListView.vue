<template>
  <section>
    <div class="page-title">
      <div>
        <h1>日志审计</h1>
        <p class="page-subtitle">所有主线动作都会沉淀到这里，便于演示审计追溯和异常排查。</p>
      </div>
    </div>

    <div class="card-grid">
      <div class="card">
        <div>日志总数</div>
        <div class="metric-value">{{ auditStore.logs.length }}</div>
      </div>
      <div class="card">
        <div>广告动作</div>
        <div class="metric-value">{{ adLogCount }}</div>
      </div>
      <div class="card">
        <div>素材动作</div>
        <div class="metric-value">{{ materialLogCount }}</div>
      </div>
      <div class="card">
        <div>计划动作</div>
        <div class="metric-value">{{ planLogCount }}</div>
      </div>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th>时间</th>
          <th>操作人</th>
          <th>动作</th>
          <th>对象类型</th>
          <th>对象 ID</th>
          <th>详情</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="log in auditStore.logs" :key="log.id">
          <td>{{ log.time }}</td>
          <td>{{ log.actorRole }} / {{ log.actorId }}</td>
          <td>{{ log.action }}</td>
          <td>{{ log.targetType }}</td>
          <td>{{ log.targetId }}</td>
          <td>{{ log.detail }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuditStore } from '@/stores/audit'

const auditStore = useAuditStore()

const adLogCount = computed(() =>
  auditStore.logs.filter((item) => item.targetType === 'ad').length
)
const materialLogCount = computed(() =>
  auditStore.logs.filter((item) => item.targetType === 'material').length
)
const planLogCount = computed(() =>
  auditStore.logs.filter((item) => item.targetType === 'plan').length
)
</script>
