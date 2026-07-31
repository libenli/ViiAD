<template>
  <div class="forbidden-page">
    <section class="forbidden-card">
      <div class="status-code">401</div>
      <h1>暂无访问权限</h1>
      <p>当前账号没有访问该页面的权限。如需继续操作，请联系系统管理员调整角色授权。</p>
      <div class="actions">
        <el-button type="primary" @click="$router.replace('/dashboard')">返回工作台</el-button>
        <el-button @click="$router.back()">返回上一页</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.forbidden-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 30% 20%, rgba(46, 213, 255, 0.18), transparent 34%),
    radial-gradient(circle at 70% 70%, rgba(74, 222, 128, 0.12), transparent 28%),
    #061018;
}

.forbidden-card {
  width: min(560px, 100%);
  padding: 42px;
  border: 1px solid rgba(125, 211, 252, 0.24);
  border-radius: 26px;
  background: rgba(8, 24, 34, 0.86);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.35);
  text-align: center;
}

.status-code {
  display: inline-grid;
  place-items: center;
  width: 94px;
  height: 94px;
  margin-bottom: 18px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.25), rgba(34, 197, 94, 0.12));
  color: #e0faff;
  font-size: 30px;
  font-weight: 800;
  box-shadow: inset 0 0 22px rgba(125, 211, 252, 0.18);
}

h1 {
  margin: 0;
  color: #f8fbff;
  font-size: 30px;
}

p {
  margin: 14px auto 26px;
  max-width: 420px;
  color: #9fb6c8;
  line-height: 1.8;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
