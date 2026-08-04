# Interview Review Schedule Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为单场面试的复习任务增加可选计划日期、会话内状态/日期筛选和现有页面内的日期管理能力。

**Architecture:** 在现有 `InterviewReviewTask`、MyBatis Store 和会话级 REST 路径上增加 `LocalDate plannedReviewDate`，不新增调度器、提醒服务或跨会话查询。Vue composable 继续作为服务端状态唯一来源，日期编辑拆成纯子组件，任务面板只负责会话内筛选与展示。

**Tech Stack:** Java 17、Spring Boot 3.5、MyBatis-Plus、MySQL 5.7、Vue 3.5、TypeScript、Vite 8。

## Global Constraints

- 所有仓库文件写入仅发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅可只读参考。
- 保留用户已有的 `src/main/resources/application.yml` 端口改动，不修改、不暂存。
- 不调用 Chat、Embedding 或 Qdrant，不引入提醒调度器、间隔重复算法、跨会话查询或新依赖。
- 新增或修改的代码注释默认使用简体中文，仅保留必要注释。

---

### Task 1: 计划日期领域与持久化契约

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTask.java`
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTaskStore.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewReviewTaskEntity.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewReviewTaskMapper.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/MySqlInterviewReviewTaskStore.java`
- Create: `deploy/mysql/init/011_schedule_interview_review_task.sql`
- Test: `src/test/java/com/javainterview/rag/interview/persistence/MySqlInterviewReviewTaskStoreTest.java`

**Interfaces:**
- Produces: `InterviewReviewTask.plannedReviewDate(): LocalDate`，以及 Store 的筛选读取和 `schedule(...)` 原子更新能力。

- [x] **Step 1: 写入失败测试**

  为 MySQL Store 增加三类断言：任务实体映射保留计划日期；查询把 `status/from/to` 原样传给 Mapper；设置或清除日期后重新读取并返回最新任务。

- [x] **Step 2: 运行持久化测试确认 RED**

  Run: `cmd.exe /d /c mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=MySqlInterviewReviewTaskStoreTest test`

  Expected: 因 `plannedReviewDate`、筛选读取和 `schedule` 契约尚不存在而编译失败。

- [x] **Step 3: 实现最小持久化能力**

  `InterviewReviewTask` 新增可空 `LocalDate plannedReviewDate`；Mapper 使用 MyBatis 动态 SQL，只在参数存在时追加状态和日期范围条件，并按 `planned_review_date IS NULL`、计划日期、题号稳定排序；`schedule` 仅更新同一 `session_id + task_id` 的计划日期。

  迁移内容固定为：

  ```sql
  ALTER TABLE t_interview_review_task
      ADD COLUMN planned_review_date DATE NULL COMMENT '计划复习日期' AFTER status,
      ADD KEY idx_session_status_planned_date (session_id, status, planned_review_date);
  ```

- [x] **Step 4: 运行持久化测试确认 GREEN**

  Run: `cmd.exe /d /c mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=MySqlInterviewReviewTaskStoreTest test`

  Expected: 直接相关测试通过。

### Task 2: 会话级查询和日期更新 API

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTaskService.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewScheduleRequest.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewTaskController.java`
- Test: `src/test/java/com/javainterview/rag/interview/review/InterviewReviewTaskServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewReviewTaskControllerTest.java`

**Interfaces:**
- Consumes: Task 1 的 `findBySessionId(sessionId, status, from, to)` 与 `schedule(sessionId, taskId, date)`。
- Produces: `GET /api/interviews/sessions/{sessionId}/review-tasks?status=&plannedDateFrom=&plannedDateTo=` 和 `PATCH /api/interviews/sessions/{sessionId}/review-tasks/{taskId}/schedule`。

- [x] **Step 1: 写入失败测试**

  覆盖合法筛选转发、结束日期早于开始日期返回 400、非法状态返回 400、设置/清除计划日期、缺失任务返回 404。请求体固定为：

  ```json
  { "plannedReviewDate": "2026-08-08" }
  ```

  清除日期使用：

  ```json
  { "plannedReviewDate": null }
  ```

- [x] **Step 2: 运行服务与控制器测试确认 RED**

  Run: `cmd.exe /d /c mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=InterviewReviewTaskServiceTest,InterviewReviewTaskControllerTest test`

  Expected: 因筛选和日期更新方法不存在而编译失败。

- [x] **Step 3: 实现最小服务和接口**

  服务先恢复并验证会话，再规范化可选状态、校验日期范围，并把任务 ID 去空白后调用 Store。控制器使用 Spring 的 `LocalDate` 参数绑定；不校验计划日期必须晚于今天，避免因时区或过期任务阻止用户修正已有计划。

- [x] **Step 4: 运行服务与控制器测试确认 GREEN**

  Run: `cmd.exe /d /c mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=InterviewReviewTaskServiceTest,InterviewReviewTaskControllerTest test`

  Expected: 直接相关测试通过，未触发模型调用。

### Task 3: 现有复习清单内的计划日期交互

**Files:**
- Modify: `frontend/src/api/interview.ts`
- Modify: `frontend/src/composables/useInterviewSession.ts`
- Create: `frontend/src/components/interview/InterviewReviewScheduleControl.vue`
- Modify: `frontend/src/components/interview/InterviewReviewTaskPanel.vue`
- Modify: `frontend/src/views/InterviewView.vue`

**Interfaces:**
- Consumes: Task 2 的筛选 GET 与 schedule PATCH。
- Produces: 状态分段筛选、计划日期范围输入、任务行内日期设置/清除，以及显式 loading/error 状态。

- [x] **Step 1: 扩展类型与 API**

  `InterviewReviewTask` 增加 `plannedReviewDate: string | null`；新增 typed query 和 `scheduleInterviewReviewTask(sessionId, taskId, plannedReviewDate)`，日期筛选只发送非空参数。

- [x] **Step 2: 保持服务端为唯一事实来源**

  composable 保存当前筛选条件；筛选变化后重新请求当前会话任务；设置或清除日期成功后替换单条任务，不在浏览器推断数据库状态。

- [x] **Step 3: 增加聚焦子组件和面板控件**

  `InterviewReviewScheduleControl` 只接收当前日期、busy 状态并发出 `schedule`；使用原生 `input[type=date]`、保存按钮和清除图标按钮。任务面板使用现有 token，筛选控件保持紧凑，不增加卡片嵌套或实现说明文案。

- [x] **Step 4: 运行前端类型检查和生产构建**

  Run: `npm run build`（workdir `frontend`）

  Expected: `vue-tsc --noEmit` 与 Vite production build 均通过。

### Task 4: 迁移验证、文档和提交

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/03-阶段路线.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/superpowers/plans/2026-08-04-interview-review-schedule.md`

**Interfaces:**
- Produces: 与真实实现、验证和遗留边界一致的阶段 7 事实记录。

- [x] **Step 1: 在独立数据库执行并核对 011**

  只读加载现有连接配置，在 `java_interview_rag` 数据库执行迁移；核对 `planned_review_date` 类型、可空性和联合索引。不得写入业务库或调用模型。

- [x] **Step 2: 运行最小最终验证**

  后端只运行复习任务 service/store/controller 相关测试；前端运行一次 `npm run build`；再执行 `git diff --check`。

- [x] **Step 3: 更新文档事实**

  记录参考项目仍以 attempt 事实为基础、当前仓库采纳可选日期和会话内查询、不采纳提醒调度器；明确真实 UI 流程未进行付费模型验证。

- [x] **Step 4: 检查并提交直接相关文件**

  确认 `src/main/resources/application.yml` 未暂存，提交信息使用 `feat: 增加复习计划日期`。
