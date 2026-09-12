# Knowledge Index Task Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an asynchronous knowledge import/rebuild task flow that gives the user a task ID, visible lifecycle state, deterministic completion/failure details, and a retryable frontend experience while preserving the existing synchronous endpoints.

**Architecture:** Multipart requests are validated and read into a bounded in-memory task payload, then submitted to a Spring-managed bounded executor. A task service persists low-sensitivity metadata and lifecycle state through a local in-memory store or MySQL store, invokes the existing `MarkdownContentImporter`, and records success/failure without persisting document正文. The Vue workbench uses the task endpoint for new imports, polls by task ID with bounded backoff, keeps the existing chunk/result renderer, and exposes the existing synchronous endpoint only as a compatibility fallback.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring MVC, MyBatis-Plus/MySQL, Spring `ThreadPoolTaskExecutor`, Vue 3 Composition API, TypeScript, Vite.

**Spec:** `docs/09-后续开发路线.md` 的“全链路可靠性”和“多存储失败补偿”未完成项；`docs/04-当前进度.md` 当前快照。

## Global Constraints

- 所有写入仅发生在 `D:\IdeaProjects\java-interview-rag`；`blog-springboot` 只读参考。
- 保留现有 `POST /api/knowledge/documents/import` 与 `POST /api/knowledge/documents/rebuild` 同步兼容接口。
- 不引入 MQ、Quartz、Redis 队列或通用任务平台；任务执行器必须有界并拒绝过载。
- 不把上传正文或真实简历、密钥写入数据库、日志或指标；任务记录只保存来源、操作、版本、状态、错误类型和时间。
- 新增代码注释、异常和用户提示使用简体中文；只修改本功能直接相关文件。
- 验证只覆盖本轮可达行为：任务状态流转、过载/失败边界、前端类型构建和最小接口测试。

### Task 1: Define task domain, storage contract, and API payloads

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/task/KnowledgeDocumentTask.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/task/KnowledgeDocumentTaskStatus.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/task/KnowledgeDocumentTaskOperation.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/task/KnowledgeDocumentTaskStore.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/task/InMemoryKnowledgeDocumentTaskStore.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/web/KnowledgeDocumentTaskResponse.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/web/KnowledgeDocumentTaskSubmitResponse.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/task/InMemoryKnowledgeDocumentTaskStoreTest.java`

**Interfaces:**
- `KnowledgeDocumentTaskStore.create(...)` produces a task with `QUEUED` status.
- `find(taskId)` returns the current task or empty.
- `markRunning`, `markSucceeded`, and `markFailed` perform idempotent terminal-state updates.
- API responses expose task ID, source, operation, status, version, chunk count, format, warning count, error type, and timestamps, never document正文.

- [ ] **Step 1: Write the failing lifecycle test**

```java
@Test
void storesQueuedTaskAndAllowsOnlyForwardLifecycleTransitions() {
    KnowledgeDocumentTaskStore store = new InMemoryKnowledgeDocumentTaskStore();
    KnowledgeDocumentTask queued = store.create("task-1", "java/basic.md",
            KnowledgeDocumentTaskOperation.IMPORT);

    assertThat(queued.status()).isEqualTo(KnowledgeDocumentTaskStatus.QUEUED);
    store.markRunning("task-1");
    store.markSucceeded("task-1", "version-1", 2, "MARKDOWN", 0);
    store.markFailed("task-1", "IllegalStateException");

    assertThat(store.find("task-1").orElseThrow().status())
            .isEqualTo(KnowledgeDocumentTaskStatus.SUCCEEDED);
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `./mvnw.cmd -q -Dtest=InMemoryKnowledgeDocumentTaskStoreTest test`

Expected: compilation failure because the task domain and store do not exist.

- [ ] **Step 3: Implement the domain and in-memory store**

Use immutable records for task snapshots and a synchronized `LinkedHashMap` for local development. Terminal states (`SUCCEEDED`, `FAILED`) ignore later lifecycle updates so duplicate executor callbacks cannot overwrite the user-visible result.

- [ ] **Step 4: Run the focused test and verify it passes**

Run: `./mvnw.cmd -q -Dtest=InMemoryKnowledgeDocumentTaskStoreTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/javainterview/rag/knowledge/task src/main/java/com/javainterview/rag/knowledge/web/KnowledgeDocumentTaskResponse.java src/main/java/com/javainterview/rag/knowledge/web/KnowledgeDocumentTaskSubmitResponse.java src/test/java/com/javainterview/rag/knowledge/task/InMemoryKnowledgeDocumentTaskStoreTest.java
git commit -m "feat: 增加知识资料任务领域模型"
```

### Task 2: Implement bounded asynchronous task execution and controller endpoints

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/task/KnowledgeDocumentTaskService.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/task/KnowledgeDocumentTaskExecutorConfiguration.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownDocumentController.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/web/KnowledgeDocumentTaskControllerTest.java`

**Interfaces:**
- `POST /api/knowledge/documents/import-tasks` accepts the same multipart fields as the synchronous import endpoint and returns HTTP 202 with `{taskId,status,source,operation}`.
- `POST /api/knowledge/documents/rebuild-tasks` accepts the same multipart fields and returns HTTP 202.
- `GET /api/knowledge/documents/tasks/{taskId}` returns the current task snapshot.
- `KnowledgeDocumentTaskService.submitImport/submitRebuild` validates file/source, creates a task, and schedules the existing importer; the uploaded bytes remain only in the bounded in-memory job until completion.
- Executor defaults: core 2, max 2, queue 8, rejection mapped to HTTP 429 with a Chinese message.

- [ ] **Step 1: Add controller tests for 202, polling, and rejected work**

Cover: task submission returns a non-empty ID; polling returns `QUEUED`/`RUNNING`/`SUCCEEDED` after the importer completes; a missing task returns 404; executor rejection returns 429 and does not create a phantom task.

- [ ] **Step 2: Run the focused controller test and verify it fails**

Run: `./mvnw.cmd -q -Dtest=KnowledgeDocumentTaskControllerTest test`

Expected: missing endpoint/service or HTTP status mismatch.

- [ ] **Step 3: Implement the bounded executor and service**

Read the multipart bytes once, create a task, submit a `Runnable`, and call the existing `MarkdownContentImporter.importContent`/`rebuildContent`. On success record version/chunk count/format; on `RuntimeException` record only exception simple name and keep the task retryable from the UI. Catch `TaskRejectedException` before task creation where possible.

- [ ] **Step 4: Register local and storage task stores**

Add a local `InMemoryKnowledgeDocumentTaskStore` bean and a storage bean placeholder using the same interface only after the MySQL implementation in Task 3 is available; both profiles must expose the service without changing the synchronous importer beans.

- [ ] **Step 5: Run focused backend tests**

Run: `./mvnw.cmd -q -Dtest=KnowledgeDocumentTaskControllerTest,MarkdownDocumentControllerTest test`

Expected: PASS, with existing synchronous tests unchanged.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/javainterview/rag/knowledge/task src/main/java/com/javainterview/rag/knowledge/web/MarkdownDocumentController.java src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java src/test/java/com/javainterview/rag/knowledge/web/KnowledgeDocumentTaskControllerTest.java
git commit -m "feat: 支持知识资料异步索引任务"
```

### Task 3: Persist task metadata for storage profile and define restart failure behavior

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeDocumentTaskEntity.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeDocumentTaskMapper.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/persistence/MySqlKnowledgeDocumentTaskStore.java`
- Create: `deploy/mysql/init/022_knowledge_document_task.sql`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/persistence/MySqlKnowledgeDocumentTaskStoreTest.java`

**Interfaces:**
- MySQL stores task metadata and lifecycle state using `task_id` as primary key.
- `RUNNING` tasks found during application startup are converted to `FAILED` with error type `APPLICATION_RESTARTED`; no document正文 is reconstructed or retried automatically.
- Re-submission remains an explicit user action with a new multipart upload, preserving the no-payload-persistence boundary.

- [ ] **Step 1: Write mapper/store tests**

Assert SQL mapping preserves status, version, chunk count, and error type, and that terminal updates are idempotent.

- [ ] **Step 2: Add migration and MySQL store**

Use the existing MySQL naming style and a compact table with columns for task ID, source, operation, status, document version, chunk count, format, warning count, error type, created/updated/completed timestamps.

- [ ] **Step 3: Add startup recovery hook**

Expose `failRunningTasksAfterRestart()` on the store and call it once from a storage-profile `ApplicationRunner`; do not touch Qdrant/Lucene or trigger imports.

- [ ] **Step 4: Run focused persistence tests**

Run: `./mvnw.cmd -q -Dtest=MySqlKnowledgeDocumentTaskStoreTest test`

Expected: PASS without requiring a live MySQL instance (mapper interactions are mocked).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/javainterview/rag/knowledge/persistence deploy/mysql/init/022_knowledge_document_task.sql src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java src/test/java/com/javainterview/rag/knowledge/persistence/MySqlKnowledgeDocumentTaskStoreTest.java
git commit -m "feat: 持久化知识资料任务状态"
```

### Task 4: Connect the Vue workbench to task polling and retryable failure UX

**Files:**
- Modify: `frontend/src/api/knowledge.ts`
- Create: `frontend/src/components/KnowledgeDocumentTaskPanel.vue`
- Modify: `frontend/src/views/KnowledgeWorkbenchView.vue`
- Modify: `frontend/src/components/ImportPanel.vue`
- Test: `frontend` typecheck/build.

**Interfaces:**
- `submitKnowledgeDocumentTask(file, source, operation)` returns a task submission response.
- `getKnowledgeDocumentTask(taskId)` returns the task snapshot.
- Workbench polling stops at `SUCCEEDED` or `FAILED`, caps at 30 attempts, and uses 500ms → 1500ms bounded delay.
- On success, the existing `ChunkLedger` receives the task result metadata; on failure, the panel shows error type and a “重新选择文件重试” action without clearing the source field.

- [ ] **Step 1: Add API types and task panel**

Represent only status/result metadata; never render or store uploaded正文 in task state.

- [ ] **Step 2: Switch new import submission to the task endpoint**

Keep the existing import form and demo document behavior. The parent view owns polling and maps task terminal states to the existing workbench state.

- [ ] **Step 3: Add bounded polling and retry reset**

Use `setTimeout`, clear the timer on unmount, stop after 30 attempts, and make retry return to file selection rather than replaying the old payload.

- [ ] **Step 4: Run frontend validation**

Run: `npm run build` in `frontend`.

Expected: PASS with TypeScript checking and Vite production build.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/knowledge.ts frontend/src/components/KnowledgeDocumentTaskPanel.vue frontend/src/views/KnowledgeWorkbenchView.vue frontend/src/components/ImportPanel.vue
git commit -m "feat: 接入知识资料任务轮询"
```

### Task 5: Synchronize facts, add phase summary, and perform the minimum end-to-end verification

**Files:**
- Modify: `docs/04-当前进度.md`
- Modify: `docs/09-后续开发路线.md`
- Create: `docs/03-阶段总结/Phase35-知识资料异步任务中心.md`

- [ ] **Step 1: Run the smallest relevant backend tests and frontend build**

Run the focused task/store/controller tests, existing synchronous document controller tests, `npm run build`, and `git diff --check`.

- [ ] **Step 2: Verify diff scope and branch state**

Run `git status --short`, `git diff --stat`, and inspect staged file names. Do not stage build output or unrelated user changes.

- [ ] **Step 3: Update current progress and roadmap**

Record the new user flow, bounded executor limits, restart behavior, and explicit boundary that uploaded正文 is not persisted and queued work is not auto-replayed after restart.

- [ ] **Step 4: Write the phase summary**

List changed files, tests, commit IDs, behavior changes, and follow-up risk: no MQ/distributed queue, in-memory payload lifetime, and live storage polling not browser-verified.

- [ ] **Step 5: Commit documentation and final integration**

```bash
git add docs/04-当前进度.md docs/09-后续开发路线.md docs/03-阶段总结/Phase35-知识资料异步任务中心.md docs/superpowers/plans/2026-09-12-knowledge-index-task-center.md
git commit -m "docs: 记录知识资料任务中心闭环"
```

## Self-Review Checklist

- [ ] The synchronous import/rebuild endpoints remain unchanged and their tests still pass.
- [ ] New task endpoints never return uploaded正文 or write it to MySQL/logs.
- [ ] Task transitions are monotonic and duplicate callbacks cannot overwrite terminal state.
- [ ] Executor saturation returns a clear 429 instead of silently dropping work.
- [ ] Restart behavior is explicit: persisted `RUNNING` tasks become failed and require a new upload.
- [ ] Frontend polling is bounded, cancellable, and leaves a retry path after failure.
- [ ] Documentation distinguishes implemented behavior from deferred MQ/distributed queue work.
