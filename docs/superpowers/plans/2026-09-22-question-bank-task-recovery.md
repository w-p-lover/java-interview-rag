# Question Bank Task Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让题库生成任务记录 Worker 心跳，并在执行实例失联后最多自动补投一次，同时阻止旧 attempt 写入题目或终态。

**Architecture:** 题库任务继续以 `BankGenerationTask` 和 MySQL 乐观锁作为业务事实源；Worker、attempt、心跳和恢复次数同时写入独立列，心跳更新不递增业务版本，避免与逐题事务保存冲突。单实例定时扫描只恢复超过租约时间的 `PROCESSING` 任务，恢复时原子校验旧 attempt 和最后心跳；资料导入任务仍保持重启安全失败，等待阶段 9.2 解决索引副作用后再自动补投。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus、MySQL 5.7、JUnit 5、Mockito

**Spec:** `docs/09-后续开发路线.md` 阶段 9.1“统一异步任务恢复”

## Global Constraints

- 所有项目写入仅发生在 `D:\IdeaProjects\java-interview-rag`。
- 不新增 Redis、MQ 或跨进程 Worker；当前恢复协议只面向单 storage 实例。
- 不在测试中调用真实 Chat/Embedding，不自动恢复资料索引任务。
- 心跳更新不得抢占题目保存使用的业务乐观锁版本。
- 最多自动恢复一次；耗尽后进入明确 `RECOVERY_EXHAUSTED` 终态。

---

### Task 1: Persist Worker Lease Metadata

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/bank/BankGenerationTask.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/BankGenerationTaskEntity.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/BankGenerationTaskMapper.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/MySqlQuestionBankStore.java`
- Modify: `deploy/mysql/init/030_knowledge_question_bank.sql`
- Create: `deploy/mysql/init/036_question_bank_task_recovery.sql`

**Interfaces:**
- Produces: `heartbeat(taskId, attemptId, workerId, heartbeatAt)` 和 `recoverStale(replacement, expectedVersion, expectedAttemptId, staleBefore)`。

- [ ] **Step 1: Extend task metadata**

```java
String workerId;
LocalDateTime startedAt;
LocalDateTime lastHeartbeatAt;
int recoveryCount;
```

- [ ] **Step 2: Add lease-specific mapper operations**

```java
int heartbeat(String taskId, String attemptId, String workerId, LocalDateTime heartbeatAt);
int recoverStale(BankGenerationTaskEntity task, long expectedVersion,
                 String expectedAttemptId, LocalDateTime staleBefore);
```

- [ ] **Step 3: Add MySQL 5.7 migration**

```sql
ALTER TABLE t_knowledge_bank_generation_task
  ADD COLUMN attempt_id VARCHAR(36) NULL,
  ADD COLUMN worker_id VARCHAR(100) NULL,
  ADD COLUMN started_at DATETIME(6) NULL,
  ADD COLUMN last_heartbeat_at DATETIME(6) NULL,
  ADD COLUMN recovery_count INT UNSIGNED NOT NULL DEFAULT 0;
```

- [ ] **Step 4: Compile the affected module**

Run: `mvnw.cmd -DskipTests compile`

Expected: `BUILD SUCCESS`.

### Task 2: Heartbeat And Bounded Recovery

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/bank/BankGenerationRecoveryPolicy.java`
- Modify: `src/main/java/com/javainterview/rag/interview/bank/BankGenerationService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/QuestionBankConfiguration.java`
- Test: `src/test/java/com/javainterview/rag/interview/bank/BankGenerationServiceTest.java`

**Interfaces:**
- Consumes: Task 1 的 `heartbeat` 与 `recoverStale` 原子更新。
- Produces: `scanStaleTasks()`，供定时扫描和确定性测试调用。

- [ ] **Step 1: Write recovery tests**

```java
assertTrue(firstRecovery.status() == BankGenerationTask.Status.QUEUED);
assertEquals(1, firstRecovery.recoveryCount());
assertEquals("RECOVERY_EXHAUSTED", exhausted.errorCode());
```

- [ ] **Step 2: Start and stop attempt heartbeats**

任务进入 `PROCESSING` 后登记 Worker、开始时间和首次心跳；执行结束时取消对应心跳句柄。

- [ ] **Step 3: Scan stale attempts**

扫描 `lastHeartbeatAt <= now - staleTimeout` 的任务；恢复未超限任务，超限任务失败，成功原子更新后才补投。

- [ ] **Step 4: Preserve old-attempt fencing**

恢复会清空旧 Worker/attempt；旧 Worker 的 `saveGenerated`、完成和失败继续因 attempt/version 不匹配返回未命中。

- [ ] **Step 5: Run focused tests**

Run: `mvnw.cmd -Dtest=BankGenerationServiceTest test`

Expected: existing partial-success resume and new stale-recovery cases all pass.

### Task 3: API Contract And Current Facts

**Files:**
- Modify: `src/test/java/com/javainterview/rag/interview/web/QuestionBankControllerTest.java`
- Modify: `docs/04-当前进度.md`

**Interfaces:**
- Consumes: `BankGenerationTask` 新增运行元数据。
- Produces: 查询任务时可见 Worker、心跳和恢复次数，不增加新 API。

- [ ] **Step 1: Verify response metadata**

```java
andExpect(jsonPath("$.recoveryCount").value(0));
```

- [ ] **Step 2: Run minimal regression tests**

Run: `mvnw.cmd -Dtest=BankGenerationServiceTest,QuestionBankControllerTest test`

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Update current progress and inspect diff**

Run: `git diff --check`

Expected: no whitespace errors; documentation states that document-index side effects remain outside this closure.

- [ ] **Step 4: Commit the verified closure**

```bash
git add <directly-related-files>
git commit -m "feat: 支持题库任务失联恢复"
```
