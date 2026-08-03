# 面试复习历史聚合实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于已持久化的复习 attempt，提供并展示单会话、单任务的首次、最近、最好分数与最新未解决遗漏。

**Architecture:** 后端由 `InterviewReviewTaskService` 读取当前会话的复习任务与 attempt，生成只读会话历史，不新增表或复制数据。前端组合式函数以历史接口作为唯一汇总来源，任务面板展示会话概览，单题面板展示该题汇总和既有时间线。

**Tech Stack:** Java 17、Spring Boot 3、JUnit 5、Mockito、Vue 3、TypeScript、Vite。

## Global Constraints

- 所有写入只允许发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅可只读参考。
- 新增或修改的代码注释默认使用简体中文。
- 不调用 Chat、Embedding 或 Qdrant，不产生模型费用。
- 保留用户对 `src/main/resources/application.yml` 的端口改动，不修改、不暂存。
- 不增加跨会话、跨用户汇总，不新增数据库迁移。

---

### Task 1: 后端历史聚合契约

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTaskHistory.java`
- Create: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewSessionHistory.java`
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTaskService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewTaskController.java`
- Test: `src/test/java/com/javainterview/rag/interview/review/InterviewReviewAttemptServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewReviewTaskControllerTest.java`

**Interfaces:**
- Consumes: `InterviewReviewTaskStore.findBySessionId` 与 `InterviewReviewAttemptStore.findByReviewTaskId` 的持久化事实。
- Produces: `InterviewReviewTaskService.findAttemptHistory(String)` 与 `GET /api/interviews/sessions/{sessionId}/review-tasks/history`。

- [x] **Step 1: 写服务和控制器失败用例**

覆盖无 attempt 任务、多个 attempt 的首次/最近/最好分数、最近遗漏、会话总次数和提升任务数。

- [x] **Step 2: 运行针对性测试确认 RED**

Run: `cmd.exe /d /c mvnw.cmd -Dtest=InterviewReviewAttemptServiceTest,InterviewReviewTaskControllerTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

Expected: 新历史类型或服务方法尚不存在导致编译失败。

- [x] **Step 3: 实现最小聚合**

按任务返回完整 attempt 时间线；有 attempt 时计算首次、最近、最好分数与最近评价的 `gaps`，会话层只计算任务数、已练任务数、总 attempt 数和最近分数高于正式回答的任务数。

- [x] **Step 4: 运行针对性测试确认 GREEN**

重复 Step 2 命令，预期所有测试通过。

### Task 2: Vue 历史展示

**Files:**
- Create: `frontend/src/components/interview/InterviewReviewHistorySummary.vue`
- Modify: `frontend/src/api/interview.ts`
- Modify: `frontend/src/composables/useInterviewSession.ts`
- Modify: `frontend/src/components/interview/InterviewReviewTaskPanel.vue`
- Modify: `frontend/src/components/interview/InterviewReviewAttemptPanel.vue`
- Modify: `frontend/src/views/InterviewView.vue`

**Interfaces:**
- Consumes: `InterviewReviewSessionHistory` API 响应。
- Produces: 会话概览、每题首次/最近/最好分数、最新未解决遗漏和既有 attempt 时间线。

- [x] **Step 1: 增加 API 类型与只读历史状态**

由组合式函数加载历史接口，并从服务端任务历史映射现有 `attemptsByTaskId`；提交成功后重新读取持久化历史。

- [x] **Step 2: 增加纯展示组件并接入现有面板**

会话摘要组件只接收历史 props；单题面板新增可空 summary prop，不在前端重新计算评分指标。

- [x] **Step 3: 运行前端构建**

Run: `npm run build`

Expected: `vue-tsc --noEmit` 与 Vite 生产构建通过。

### Task 3: 文档、检查与提交

**Files:**
- Modify: `README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 实际代码、测试和构建结果。
- Produces: 本轮事实记录与下一步候选。

- [x] **Step 1: 同步事实文档**

记录采用单会话边界、聚合字段、参考项目版本、验证结果和未进行真实模型调用。

- [x] **Step 2: 检查差异**

Run: `git diff --check`

Expected: 无空白错误；`application.yml` 只保留用户原有端口差异。

- [x] **Step 3: 提交直接相关文件**

Commit: `feat: 增加面试复习历史聚合`
