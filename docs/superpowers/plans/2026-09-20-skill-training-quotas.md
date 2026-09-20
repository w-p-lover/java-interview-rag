# Skill Training Quotas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让版本化 Skill 分类配额同时约束资料出题和自动组卷，并把复习任务的低分维度直接转换为下一场专项训练重点。

**Architecture:** 在现有 `InterviewSkillRubricCatalog` 之上增加只读 `InterviewSkillTrainingProfile`，分类名称直接复用三个专项评分维度，避免评分和组卷语义漂移。题库生成按基础配额要求模型返回精确分类，随机组卷按基础或低分聚焦配额选择已审核题；前端只读取服务端计划并沿用现有题库、会话和复习链路。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI、Vue 3、TypeScript。

**Spec:** `docs/09-后续开发路线.md` 的“8.3 Skill、Rubric 与专项训练”。

## Global Constraints

- 所有项目写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- 不增加模型调用次数，不新增表，不引入第二套会话或评分系统。
- 人工选题继续由用户决定；分类配额只约束模型生成和自动组卷。
- 显式分类筛选与低分聚焦互斥，避免两个规则同时控制题量。
- 旧题库题目保持可读；不自动改写旧分类。

---

### Task 1: Versioned Skill Training Profile

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/question/InterviewSkillTrainingProfile.java`
- Create: `src/main/java/com/javainterview/rag/interview/question/InterviewSkillTrainingCatalog.java`
- Test: `src/test/java/com/javainterview/rag/interview/question/InterviewSkillTrainingCatalogTest.java`

**Interfaces:**
- Consumes: `InterviewSkillRubricCatalog.forSkill(InterviewSkill)`.
- Produces: `plan(InterviewSkill skill, int questionCount, String focusDimension)`，返回版本、Skill、聚焦维度和有序分类题量。

- [x] **Step 1: Write failing allocation tests**

```java
assertEquals(List.of(1, 1, 1), catalog.plan(JAVA_CORE, 3, null).counts());
assertEquals(List.of(2, 1), catalog.plan(RAG_AI, 3, "边界、评估与恢复").nonZeroCounts());
```

- [x] **Step 2: Implement immutable categories from the Rubric catalog**

Each Skill has three ordered categories whose names are the existing technical, completeness and communication focus names; objectives reuse the matching criteria text.

- [x] **Step 3: Implement deterministic allocation and validation**

Without a focus, allocate round-robin in category priority order. With a valid focus, allocate two questions to it when possible, then cycle through other categories and the focus; reject unknown dimensions before generation or session creation.

- [x] **Step 4: Run the focused catalog test**

Expected: baseline and focused plans are deterministic, sum to the requested count and reject `GENERAL`, invalid counts and unknown focus dimensions.

### Task 2: Enforce Quotas During Generation And Assembly

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/bank/BankQuestionGenerator.java`
- Modify: `src/main/java/com/javainterview/rag/interview/bank/BankGenerationService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/bank/QuestionBankService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/QuestionBankController.java`
- Test: `src/test/java/com/javainterview/rag/interview/bank/BankGenerationServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/bank/QuestionBankServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/QuestionBankControllerTest.java`

**Interfaces:**
- Consumes: `InterviewSkillTrainingProfile.TrainingPlan`.
- Produces: `GET /api/interviews/question-bank/training-plan` and `focusDimension` on random session requests.

- [x] **Step 1: Add failing generation and random-assembly tests**

Assert the generator receives exact remaining category quotas after a partial task, off-plan categories are skipped, and a focused three-question session selects two questions from the focused category plus one from the next category.

- [x] **Step 2: Upgrade the generation contract to v2**

The Prompt receives an exact `category=count` list. Resume subtracts only questions saved by the same generation task; draft validation consumes quota per successful save. An incomplete category plan ends as the existing `INCOMPLETE_GENERATION` failure and can be explicitly retried under the same contract.

- [x] **Step 3: Add training-plan query and focused session request**

```java
TrainingPlan trainingPlan(InterviewSkill skill, int questionCount, String focusDimension)
```

Random assembly groups current ACTIVE questions by exact category and selects the requested quota. It reports the first category shortage explicitly. Manual selection rejects `focusDimension`; explicit `category` and `focusDimension` cannot be combined.

- [x] **Step 4: Run focused backend tests**

Expected: no model/network calls; generation, recovery, allocation, idempotent session creation and controller JSON contracts pass.

### Task 3: Connect Review Weakness To The Question Bank

**Files:**
- Modify: `frontend/src/api/questionBank.ts`
- Modify: `frontend/src/composables/useQuestionBank.ts`
- Create: `frontend/src/components/interview/QuestionBankTrainingPlan.vue`
- Modify: `frontend/src/views/QuestionBankView.vue`
- Modify: `frontend/src/components/interview/InterviewReviewTaskPanel.vue`
- Modify: `frontend/src/views/InterviewView.vue`

**Interfaces:**
- Consumes: training-plan API plus `session.configuration.skill`, task `weakDimensions` and first knowledge source.
- Produces: `/question-bank?source=...&skill=...&focusDimension=...` and random-session payload `focusDimension`.

- [x] **Step 1: Extend typed API contracts**

Define `SkillTrainingPlan`, `SkillTrainingCategoryQuota`, `getSkillTrainingPlan()` and optional `focusDimension` on `BankSessionPayload`.

- [x] **Step 2: Keep training-plan state in the composable**

`useQuestionBank(initialSource, initialSkill, initialFocusDimension)` owns the selected focus, loads the server plan when Skill/count/focus changes, resets an invalid cross-Skill focus, and exposes a quota-aware random-start capability.

- [x] **Step 3: Render the compact plan component**

The component receives plan, focus and busy props and emits only `update:focus`. It displays three category swatches with exact counts and keeps the existing view as the composition surface.

- [x] **Step 4: Add the review-task training link**

For grounded tasks with a weak dimension, render a `RouterLink` using the first knowledge source, current session Skill and the first weak dimension. Legacy tasks without dimensions or sources keep the existing UI unchanged.

- [x] **Step 5: Run the frontend production build**

Expected: `vue-tsc --noEmit` and Vite build pass without adding local duplicated quota rules.

### Task 4: Record And Commit The Closure

**Files:**
- Modify: `docs/04-当前进度.md`

- [x] **Step 1: Run the smallest combined backend suite**

Run the training catalog, question generation, bank service and bank controller tests under JDK 21 with repo-local Maven/temp directories.

- [x] **Step 2: Run `git diff --check` and review direct files**

Expected: no unrelated changes and no whitespace errors.

- [x] **Step 3: Update current progress with facts**

Record the new generation/assembly behavior, compatibility boundary, verification, lack of paid-model validation and the next independent 8.3 gap.

- [x] **Step 4: Commit**

```text
feat: 接入 Skill 分类配额与弱项训练
```
