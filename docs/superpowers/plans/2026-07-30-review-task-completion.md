# Review Task Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让已生成的面试复习任务可以被幂等地标记为已完成，并在后端和面试复盘页面保留首次完成时间。

**Architecture:** 在现有复习任务聚合中增加 `COMPLETED` 状态和可空 `completedAt`，通过任务级 `PATCH` 接口调用 MySQL 原子更新。Vue composable 负责请求并替换单条任务，展示组件只接收状态并向上发出完成事件。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus、MySQL 5.7/8、Vue 3 Composition API、TypeScript、Axios。

## Global Constraints

- 所有写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- 保留用户对 `src/main/resources/application.yml` 的未提交端口修改，不暂存、不覆盖。
- 新增或修改的代码注释、异常和用户提示使用简体中文。
- 只实现 `PENDING -> COMPLETED`，不增加重开、提醒、计划日期、跨会话汇总或新依赖。
- 验证只覆盖复习任务后端测试和前端生产构建。

---

### Task 1: 复习任务领域状态与存储更新

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTask.java`
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTaskStatus.java`
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTaskStore.java`
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTaskService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewReviewTaskEntity.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewReviewTaskMapper.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/MySqlInterviewReviewTaskStore.java`
- Test: `src/test/java/com/javainterview/rag/interview/review/InterviewReviewTaskServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/persistence/MySqlInterviewReviewTaskStoreTest.java`

**Interfaces:**
- Consumes: 已存在的会话 ID、任务 ID 和 MySQL 复习任务记录。
- Produces: `InterviewReviewTaskService.complete(sessionId, taskId)` 和 `InterviewReviewTaskStore.complete(sessionId, taskId, completedAt)`。

- [x] **Step 1: 编写失败测试**

  在服务测试中断言完成操作校验会话、传递完成时间并返回更新后的任务；在存储测试中断言更新后按会话和任务 ID 读取，找不到时返回空。

- [x] **Step 2: 运行 RED 验证**

  Run: `mvnw.cmd -Dtest=InterviewReviewTaskServiceTest,MySqlInterviewReviewTaskStoreTest test`

  Expected: 因 `complete` 契约、`COMPLETED` 状态和 `completedAt` 尚不存在而编译失败。

- [x] **Step 3: 实现最小领域与存储逻辑**

  `InterviewReviewTask` 增加 `LocalDateTime completedAt` 并校验状态与时间一致；Mapper 使用 `COALESCE(completed_at, #{completedAt})` 保留首次完成时间，再按 `session_id + id` 查询任务。

- [x] **Step 4: 运行相关测试**

  Run: `mvnw.cmd -Dtest=InterviewReviewTaskServiceTest,MySqlInterviewReviewTaskStoreTest test`

  Expected: 两个测试类全部通过。

### Task 2: HTTP 完成接口

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewTaskController.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewReviewTaskControllerTest.java`

**Interfaces:**
- Consumes: `InterviewReviewTaskService.complete(sessionId, taskId)`。
- Produces: `PATCH /api/interviews/sessions/{sessionId}/review-tasks/{taskId}/complete`，返回更新后的任务；任务不存在返回 404。

- [x] **Step 1: 编写控制器失败测试**

  使用 MockMvc 调用 `PATCH`，断言响应状态为 `COMPLETED` 且包含 `completedAt`；模拟缺失任务并断言 404。

- [x] **Step 2: 运行 RED 验证**

  Run: `mvnw.cmd -Dtest=InterviewReviewTaskControllerTest test`

  Expected: 因 PATCH 映射尚不存在返回 405 或编译失败。

- [x] **Step 3: 增加最小接口映射**

  控制器新增 `@PatchMapping("/{taskId}/complete")` 并复用既有 400/404 异常映射。

- [x] **Step 4: 运行三个复习任务测试类**

  Run: `mvnw.cmd -Dtest=InterviewReviewTaskServiceTest,MySqlInterviewReviewTaskStoreTest,InterviewReviewTaskControllerTest test`

  Expected: 三个测试类全部通过。

### Task 3: 数据库迁移与 Vue 完成交互

**Files:**
- Create: `deploy/mysql/init/007_complete_interview_review_task.sql`
- Modify: `frontend/src/api/interview.ts`
- Modify: `frontend/src/composables/useInterviewSession.ts`
- Modify: `frontend/src/components/interview/InterviewReviewTaskPanel.vue`
- Modify: `frontend/src/views/InterviewView.vue`

**Interfaces:**
- Consumes: 后端 PATCH 接口和现有 `InterviewReviewTask[]`。
- Produces: `completeInterviewReviewTask(sessionId, taskId)`、composable 的 `completeReviewTask(taskId)`、组件 `complete` 事件。

- [x] **Step 1: 增加兼容迁移**

  `007` 为已存在的 `t_interview_review_task` 增加可空 `completed_at DATETIME(6)`；不修改已执行的 `006`。

- [x] **Step 2: 更新前端 API 与状态**

  类型允许 `PENDING | COMPLETED` 并增加 `completedAt: string | null`；完成请求成功后按 ID 替换数组中的单条任务，失败信息保留在复习任务独立错误状态中。

- [x] **Step 3: 更新复习任务组件**

  待完成任务显示“标记已复习”按钮；已完成任务显示状态和本地格式化完成时间。组件通过 typed emit 上报任务 ID，不直接修改 props。

- [x] **Step 4: 运行前端构建**

  Run: `cd frontend && npm run build`

  Expected: `vue-tsc --noEmit` 与 Vite production build 均通过。

### Task 4: 文档、迁移执行与收尾

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 已验证的代码、构建结果和迁移执行结果。
- Produces: 与真实状态一致的阶段 7 记录。

- [x] **Step 1: 执行 007 迁移并核对列**

  只读复用现有安全配置连接独立 `java_interview_rag` 数据库，执行 `007`，确认 `completed_at` 可空且原有任务数据不被改写。

- [x] **Step 2: 更新长期文档**

  记录状态流、首次完成时间幂等语义、接口、迁移、验证和明确未实现范围；开发记录只追加。

- [x] **Step 3: 检查差异并提交**

  只暂存本轮文件，排除 `src/main/resources/application.yml`，提交信息使用 `feat: 完成复习任务状态流转`。
