# VAD Vue Prototype

这是基于文档整理出来的 Vue 版快速原型骨架，目标是先把“大屏广告系统”的主线流程在网页端走通。

## 老板演示版

- 演示入口页：`/demo`
- 一键启动脚本：`start-demo.bat`
- 演示说明文档：[BOSS_DEMO_GUIDE.md](./BOSS_DEMO_GUIDE.md)

## 已包含内容

- Vue 3 + Vite + TypeScript + Vue Router + Pinia
- 13 类角色假账号
- 配置驱动的菜单系统
- 路由权限控制
- 主线业务 mock 数据
- 可运行的最小壳页面

## 演示账号

- `admin / 123456`
- `advertiser01 / 123456`
- `agent01 / 123456`
- `review01 / 123456`
- `biz01 / 123456`
- `ops01 / 123456`
- `analyst01 / 123456`
- `finance01 / 123456`
- `audit01 / 123456`
- 其他账号可在 `/switch-role` 查看

## 推荐下一步

1. 先细化 `ads / materials / plans / devices / reports / bills / logs`
2. 再把页面上的按钮接成真正的 store action
3. 最后再决定是否接真实后端

## 启动

```bash
npm install
npm run dev
```

或直接双击：

```text
start-demo.bat
```
