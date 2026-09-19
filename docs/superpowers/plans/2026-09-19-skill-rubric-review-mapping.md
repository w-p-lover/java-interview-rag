# Skill Rubric And Review Mapping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让面试 Skill 真正约束正式评分和复习评分，并把低分评分项转换为可持久化、可见的专项复习维度。

**Architecture:** 在现有三项分数模型之上增加版本化 `InterviewSkillRubric` 目录，不增加第二套评价结果。会话服务把配置快照中的 Skill 传给既有评价器；复习任务生成时根据三项分数和同一 Rubric 确定弱项维度，并通过现有 MySQL/内存存储及复习卡展示。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI、MyBatis Plus、MySQL 5.7、Vue 3、TypeScript。

**Spec:** `docs/09-后续开发路线.md` 的“8.3 Skill、Rubric 与专项训练”。

## Global Constraints

- 所有写入仅发生在 `D:\IdeaProjects\java-interview-rag`。
- 不增加模型调用次数，不改变现有三项分数和总分计算协议。
- 旧复习任务缺少新字段时仍能读取；数据库变更使用独立增量 SQL。
- 前端只扩展现有 `InterviewReviewTaskPanel`，保持 props down、events up。

## File Structure

- `interview/evaluation/InterviewSkillRubric.java`：保存 Rubric 版本、Skill 和三项可解释评分关注点，并计算弱项维度。
- `interview/evaluation/InterviewSkillRubricCatalog.java`：集中维护当前版本的五类 Skill/通用 Rubric。
- `InterviewAnswerEvaluator`、`InterviewAnswerEvaluationService`、`ObservableInterviewAnswerEvaluator`：把 Skill 上下文送入现有评分调用和可观测包装器。
- `InterviewSessionService`、`InterviewReviewTaskService`：分别在正式评分和复习评分时传递 Skill；生成任务时映射弱项维度。
- `InterviewReviewTask` 及 MySQL entity/mapper/store：保存 `rubricVersion` 与 `weakDimensions`。
- `deploy/mysql/init/031_interview_review_task_rubric.sql`：为 storage 增加两个可空兼容字段。
- `frontend/src/api/interview.ts`、`InterviewReviewTaskPanel.vue`：显示专项 Rubric 和薄弱维度；组件仍只负责复习任务展示与事件上抛。

### Task 1: Skill Rubric Enters Evaluation

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewSkillRubric.java`
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewSkillRubricCatalog.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluator.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/ObservableInterviewAnswerEvaluator.java`
- Modify: `src/main/java/com/javainterview/rag/interview/session/InterviewSessionService.java`
- Test: `src/test/java/com/javainterview/rag/interview/evaluation/InterviewSkillRubricCatalogTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/session/InterviewSessionServiceTest.java`

**Interfaces:**
- Produces: `InterviewSkillRubricCatalog.forSkill(InterviewSkill)` 和评价器的 Skill-aware overload。
- Consumes: 现有 `InterviewSkill` 与三项 `InterviewAnswerEvaluation` 分数。

- [x] **Step 1: Write the failing Rubric catalog test**

```java
assertNotEquals(catalog.forSkill(InterviewSkill.JAVA_CORE).completenessFocus(),
        catalog.forSkill(InterviewSkill.SYSTEM_DESIGN).completenessFocus());
assertEquals("interview-skill-rubric-v1", catalog.forSkill(InterviewSkill.RAG_AI).version());
```

- [x] **Step 2: Run the focused test and confirm the missing types fail compilation**

Run: `mvnw.cmd -Dtest=InterviewSkillRubricCatalogTest test`

- [x] **Step 3: Implement the immutable catalog and Skill-aware evaluator overloads**

```java
InterviewAnswerAssessment evaluate(InterviewPreparationProfile profile,
        InterviewQuestion question, String answer, InterviewSkill skill);
```

`InterviewAnswerEvaluationService` resolves the profile and appends its three focus descriptions to the existing system instructions; no new model call or output field is added.

- [x] **Step 4: Pass `session.configuration().skill()` from formal and review evaluation callers**

The observable wrapper must delegate the same Skill-aware method so production wiring does not drop the context.

- [x] **Step 5: Run the catalog, evaluator, observable wrapper and session tests**

Expected: all focused tests pass and existing generic evaluator callers remain source compatible through default methods.

### Task 2: Persist Weak Review Dimensions

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTask.java`
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTaskService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewReviewTaskEntity.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewReviewTaskMapper.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/MySqlInterviewReviewTaskStore.java`
- Create: `deploy/mysql/init/031_interview_review_task_rubric.sql`
- Test: `src/test/java/com/javainterview/rag/interview/review/InterviewReviewTaskServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/persistence/MySqlInterviewReviewTaskStoreTest.java`

**Interfaces:**
- Consumes: `InterviewSkillRubric.weakDimensions(InterviewAnswerEvaluation)`.
- Produces: `InterviewReviewTask.rubricVersion()` 与 `weakDimensions()`。

- [x] **Step 1: Add failing task-generation assertions**

```java
assertEquals("interview-skill-rubric-v1", task.rubricVersion());
assertEquals(List.of("并发与边界"), task.weakDimensions());
```

- [x] **Step 2: Map every score below 80 to its Skill-specific focus**

If a task has gaps but all three scores are at least 80, map the lowest-scoring focus so every generated task remains actionable. Preserve dimension order and remove duplicates.

- [x] **Step 3: Add nullable storage columns and legacy read defaults**

```sql
ALTER TABLE t_interview_review_task
    ADD COLUMN rubric_version VARCHAR(100) NULL COMMENT '专项评分规则版本',
    ADD COLUMN weak_dimensions_json TEXT NULL COMMENT '低分专项维度 JSON';
```

Rows with null columns read as the legacy rubric version and an empty dimension list; new rows always write both values.

- [x] **Step 4: Run review service and MySQL store tests**

Expected: new task round-trip keeps the version/dimensions and legacy rows remain readable.

### Task 3: Show The Training Target

**Files:**
- Modify: `frontend/src/api/interview.ts`
- Modify: `frontend/src/components/interview/InterviewReviewTaskPanel.vue`

**Interfaces:**
- Consumes: `InterviewReviewTask.rubricVersion` and `weakDimensions` from the existing task API.
- Produces: no new events; visual output remains inside the existing review item.

- [x] **Step 1: Extend the typed API contract**

```ts
rubricVersion: string;
weakDimensions: string[];
```

- [x] **Step 2: Render dimensions before free-text gaps**

Use a semantic list headed “专项薄弱维度”; hide the block for legacy tasks with an empty list. Keep the existing task action and attempt components unchanged.

- [x] **Step 3: Run `npm run build`**

Expected: Vue type checking and production bundling pass.

### Task 4: Verify And Record The Closure

**Files:**
- Modify: `docs/04-当前进度.md`

- [x] **Step 1: Run the smallest relevant backend test set**

Run the catalog, evaluation service, session service, review service, persistence store and controller tests under JDK 21 with repo-local Maven/temp directories.

- [x] **Step 2: Check `git diff --check` and review only directly related files**

Expected: no whitespace errors and no unrelated changes.

- [x] **Step 3: Update current progress with facts and the next code problem**

Record the new Skill-aware scoring/review behavior, its storage migration requirement, verification result and the next 8.3 gap. Do not claim paid model quality without a paid run.

- [x] **Step 4: Commit the verified closure**

```text
feat: 接入 Skill 专项评分与复习映射
```
