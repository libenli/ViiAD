# 百万广告系统前端工程

正式版前端骨架，技术栈为 Vue 3、Vite、TypeScript、Element Plus、Pinia、Vue Router。

## 启动

```bash
npm install
npm run dev
```

默认地址：

```text
http://localhost:5174
```

## 当前包含

- 登录页
- 深色后台主框架
- 左侧菜单
- 路由守卫
- Pinia 状态
- Axios 请求封装
- 广告、素材、计划、设备、下发、报表、账单、工单、账号主体、系统管理页面入口

## UI 约束

- 本系统采用深色科技后台主题，所有表单、表格、详情描述、分页、弹窗、抽屉都必须保持暗底高对比。
- 新增 Element Plus 组件时，避免出现白底浅字或浅底浅字；如果组件默认样式不符合，需要在 `src/styles/index.scss` 增加全局暗色覆盖。
