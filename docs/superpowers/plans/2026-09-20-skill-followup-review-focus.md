# Skill 低分追问与复习聚焦 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让题库固定追问和复习二次作答都携带并使用已保存的 Skill 低分维度，历史任务继续按创建时的 Rubric 版本解释。

**Architecture:** 题库会话继续复用 `QuestionBankContext`，新增训练维度快照并由题目分类确定，不新增数据库表。复习提交通过现有 `InterviewAnswerEvaluator` 增加聚焦评价重载，把任务持久化的 `rubricVersion` 与 `weakDimensions` 传入 Prompt；无弱项维度的旧任务继续走兼容路径。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI、Vue 3、TypeScript、JUnit 5、Mockito

**Spec:** `docs/09-后续开发路线.md` 阶段 8.3 与 `docs/04-当前进度.md` 当前固定队列第 1 项

## Global Constraints

- 所有写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- 不新增模型调用次数、数据库迁移或第二套评价结果。
- 旧题库会话允许训练维度为空；旧复习任务没有弱项维度时保持原评价路径。
- 只运行能证明本轮行为的定向测试和一次前端构建。

---

### Task 1: 题库训练维度快照

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/bank/QuestionBankContext.java`
- Modify: `src/main/java/com/javainterview/rag/interview/bank/QuestionBankService.java`
- Test: `src/test/java/com/javainterview/rag/interview/bank/QuestionBankServiceTest.java`

**Interfaces:**
- Consumes: `BankQuestion.category()` 与现有题库会话创建流程。
- Produces: `QuestionBankContext.trainingDimension()`；固定追问题目目标显示对应维度。

- [x] **Step 1: 写失败测试**

```java
assertEquals("并发与边界", fixedFollowUp.bankContext().trainingDimension());
assertTrue(fixedFollowUp.objective().contains("固定追问维度：并发与边界"));
```

- [x] **Step 2: 运行失败测试**

Run: `mvn -Dmaven.repo.local=.m2 -Dtest=QuestionBankServiceTest test`
Expected: FAIL，因为 `QuestionBankContext` 尚无 `trainingDimension`。

- [x] **Step 3: 写最小实现**

```java
public record QuestionBankContext(..., String trainingDimension) { }
```

保留六参数构造器兼容现有测试和旧调用；新题库会话以 `BankQuestion.category()` 写入训练维度，固定追问的 `objective` 明确追加该维度。

- [x] **Step 4: 运行测试验证通过**

Run: `mvn -Dmaven.repo.local=.m2 -Dtest=QuestionBankServiceTest test`
Expected: PASS。

### Task 2: 保存版本驱动的复习聚焦评分

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluator.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/ObservableInterviewAnswerEvaluator.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewSkillRubric.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewSkillRubricCatalog.java`
- Modify: `src/main/java/com/javainterview/rag/interview/review/InterviewReviewTaskService.java`
- Test: `src/test/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/review/InterviewReviewAttemptServiceTest.java`

**Interfaces:**
- Consumes: `InterviewReviewTask.rubricVersion()`、`InterviewReviewTask.weakDimensions()` 与会话 Skill。
- Produces: `evaluateForReview(..., InterviewSkill skill, String rubricVersion, List<String> focusDimensions)`。

- [x] **Step 1: 写失败测试**

```java
verify(evaluator).evaluateForReview(profile, question, answer, previous,
        InterviewSkill.RAG_AI, "interview-skill-rubric-v1", List.of("边界、评估与恢复"));
assertTrue(prompt.contains("本次复习重点：边界、评估与恢复"));
```

- [x] **Step 2: 运行失败测试**

Run: `mvn -Dmaven.repo.local=.m2 -Dtest=InterviewReviewAttemptServiceTest,InterviewAnswerEvaluationServiceTest test`
Expected: FAIL，因为评价器还未接收保存的版本和弱项维度。

- [x] **Step 3: 写最小实现**

新增默认重载并在可观测包装器中透传；`InterviewSkillRubricCatalog.forVersion` 只解析仍受支持的已保存版本，`InterviewSkillRubric.promptSection` 校验弱项属于当前 Skill 后追加聚焦规则。任务没有弱项维度时继续调用旧重载。

- [x] **Step 4: 运行测试验证通过**

Run: `mvn -Dmaven.repo.local=.m2 -Dtest=InterviewReviewAttemptServiceTest,InterviewAnswerEvaluationServiceTest,ObservableInterviewAnswerEvaluatorTest,InterviewSkillRubricCatalogTest test`
Expected: PASS。

### Task 3: 用户界面与事实文档收口

**Files:**
- Modify: `frontend/src/components/interview/InterviewReviewAttemptPanel.vue`
- Modify: `frontend/src/components/interview/InterviewReviewTaskPanel.vue`
- Modify: `docs/04-当前进度.md`

**Interfaces:**
- Consumes: 复习任务已有 `weakDimensions`。
- Produces: 二次作答区域就地展示本次训练重点，不新增前端状态源。

- [x] **Step 1: 透传弱项维度**

```vue
<InterviewReviewAttemptPanel :focus-dimensions="task.weakDimensions" />
```

- [x] **Step 2: 展示训练重点**

```vue
<p v-if="focusDimensions.length">本次重点：{{ focusDimensions.join("、") }}</p>
```

- [x] **Step 3: 更新当前进度**

记录固定追问维度快照、保存版本驱动的复习评分、兼容边界和下一项代码问题。

- [x] **Step 4: 最小验证**

Run: `npm run build`（工作目录 `frontend`）
Expected: 构建成功。

Run: `git diff --check`
Expected: 无输出。

### Task 4: 检查并提交

**Files:**
- Review: 本计划列出的所有文件

**Interfaces:**
- Consumes: 定向测试、前端构建与最终差异。
- Produces: 一个可独立审查的 Git 提交。

- [x] **Step 1: 检查工作区和差异**

Run: `git status --short` 与 `git diff --stat`
Expected: 仅包含本轮直接相关文件。

- [x] **Step 2: 提交闭环**

```bash
git add docs/04-当前进度.md docs/superpowers/plans/2026-09-20-skill-followup-review-focus.md frontend/src/components/interview/InterviewReviewAttemptPanel.vue frontend/src/components/interview/InterviewReviewTaskPanel.vue frontend/src/components/interview/QuestionBankEditor.vue src/main/java/com/javainterview/rag/interview src/test/java/com/javainterview/rag/interview
git commit -m "feat: 聚焦 Skill 低分追问与复习"
```
