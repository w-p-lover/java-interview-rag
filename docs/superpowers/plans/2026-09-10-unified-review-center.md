# 统一复习中心 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增独立复习中心，把浏览器最近面试会话的复习路径与知识问答复习路径合并展示，并能深链到具体训练任务。

**Architecture:** 前端并行调用现有两类只读路径 API，在组合式函数中归一化为统一推荐项，不改变后端存储和算法。新路由页面负责筛选、排序、汇总和导航；知识问答页补齐 `reviewTaskId` 深链定位，面试页继续复用已有 `sessionId + taskId` 深链。

**Tech Stack:** Vue 3.5、TypeScript、Vue Router、现有 Spring Boot REST API

**Spec:** `docs/09-后续开发路线.md`

## Global Constraints

- 所有项目写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- 不新增数据库表、用户身份、模型调用、Embedding 调用或跨模块公共后端抽象。
- 浏览器最多读取现有最近 8 场面试会话 ID；知识任务继续使用现有全局单用户接口。
- 两类路径任一路失败时仍展示另一路结果，不把局部失败升级为整个页面不可用。
- 只运行一次前端生产构建和差异检查，不扩展无关测试。

---

### Task 1: 最近会话 ID 共享读取

**Files:**
- Create: `frontend/src/utils/interviewSessionStorage.ts`
- Modify: `frontend/src/composables/useInterviewSession.ts`

**Interfaces:**
- Produces: `readRecentInterviewSessionIds(): string[]`
- Produces: `writeRecentInterviewSessionIds(sessionIds: readonly string[]): void`

- [x] **Step 1:** 将现有 localStorage key、去重、长度校验和最多 8 条规则移动到纯工具文件。
- [x] **Step 2:** 让 `useInterviewSession` 复用共享工具，保持现有最近会话行为不变。

### Task 2: 统一复习路径状态

**Files:**
- Create: `frontend/src/composables/useUnifiedReviewCenter.ts`

**Interfaces:**
- Produces: `UnifiedReviewItem`，包含 `kind`、任务标识、标题、上下文、得分、日期、到期状态、原因和深链参数。
- Produces: `load()`，并行请求 `getInterviewReviewPath` 与 `getKnowledgeReviewPath`。

- [x] **Step 1:** 从共享工具读取最近面试会话 ID。
- [x] **Step 2:** 使用 `Promise.allSettled` 独立加载两类路径，保留各自错误。
- [x] **Step 3:** 将推荐项归一化并按逾期、今日、后续日期和类型稳定排序。
- [x] **Step 4:** 聚合全部、到期、逾期、面试和知识任务数量。

### Task 3: 统一复习中心页面与导航

**Files:**
- Create: `frontend/src/views/ReviewCenterView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/WorkspaceHeader.vue`

**Interfaces:**
- Consumes: `useUnifiedReviewCenter()`
- Produces: `/review` 路由和导航入口。

- [x] **Step 1:** 增加 `/review` 懒加载路由与 `WorkspaceHeader` 的 review 模式。
- [x] **Step 2:** 页面展示任务总数、今日到期、逾期和已练习数量。
- [x] **Step 3:** 增加全部、今日、后续、面试、知识筛选和刷新动作。
- [x] **Step 4:** 面试任务跳转 `/interview?sessionId=...&taskId=...`，知识任务跳转 `/answer?reviewTaskId=...`。
- [x] **Step 5:** 为无会话、无任务、单路失败和整页加载提供独立状态。

### Task 4: 知识复习任务深链恢复

**Files:**
- Modify: `frontend/src/views/KnowledgeAnswerView.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeReviewTaskList.vue`

**Interfaces:**
- Consumes: 查询参数 `reviewTaskId`。
- Produces: DOM 锚点 `knowledge-review-task-${taskId}` 和高亮状态。

- [x] **Step 1:** 知识问答页读取并限制 `reviewTaskId` 查询参数长度。
- [x] **Step 2:** 加载任务后滚动并聚焦对应节点，不存在时回退任务列表标题。
- [x] **Step 3:** 任务列表接收 `highlightedTaskId`，为目标任务增加稳定 ID、焦点和高亮样式。

### Task 5: 文档、验证与提交

**Files:**
- Modify: `docs/04-当前进度.md`
- Modify: `docs/09-后续开发路线.md`

- [x] **Step 1:** 记录统一复习中心的真实能力、单用户边界和零模型调用事实。
- [x] **Step 2:** 执行 `npm run build`。
- [x] **Step 3:** 执行 `git diff --check`，审查只包含当前功能文件。
- [x] **Step 4:** 使用 `feat: 增加统一复习中心` 提交完整闭环。
