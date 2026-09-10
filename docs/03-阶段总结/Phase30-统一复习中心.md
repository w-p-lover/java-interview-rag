# Phase30：统一复习中心

## 功能升级

- 新增 Vue `/review` 独立复习中心，汇总面试复习路径和知识复习路径。
- 展示全部推荐、今日与逾期、已经逾期、已有练习记录四类摘要。
- 支持全部、今日与逾期、后续计划、面试复习、知识复习五种筛选。
- 两类路径通过 `Promise.allSettled` 独立加载，任一路失败不阻断另一路。
- 面试任务深链到 `sessionId + taskId`，知识任务深链到 `reviewTaskId` 并自动展开、高亮和定位。
- 最近面试会话 ID 的 localStorage 读取规则提取为共享工具，仍保持最多 8 场、去重和长度校验。

## 主要文件

- `frontend/src/views/ReviewCenterView.vue`：复习中心页面、摘要、筛选、状态和任务导航。
- `frontend/src/composables/useUnifiedReviewCenter.ts`：双路径加载、统一模型、排序和统计。
- `frontend/src/utils/interviewSessionStorage.ts`：最近面试会话 ID 的共享浏览器存储契约。
- `frontend/src/components/WorkspaceHeader.vue`、`frontend/src/router/index.ts`：复习中心导航与路由。
- `frontend/src/views/KnowledgeAnswerView.vue`、`frontend/src/components/knowledge/KnowledgeReviewTaskList.vue`：知识任务深链恢复、展开和高亮。

## 验证

- `npm run build` 通过，包含 `vue-tsc --noEmit` 和 Vite 生产构建。
- `git diff --check` 通过。

## 边界

- 不新增用户身份、数据库表或后端聚合接口。
- 面试路径只读取当前浏览器保存的最近最多 8 场会话；知识任务仍沿用当前单用户接口。
- 统一中心只聚合展示和导航，不合并两类后端任务模型，也不改变各自 1/3/7 天间隔算法。
- 加载路径不调用 Chat 或 Embedding。
