# 复习助手改期人工确认 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让复习助手只有在本次 HTTP 请求携带与工具参数完全一致的结构化人工确认时，才能修改复习计划日期。

**Architecture:** `POST /api/interviews/review-assistant` 增加可选 `scheduleConfirmation`，服务把确认对象放入模型不可见的 `ToolContext`。`schedule_review_task` 在调用现有 `InterviewReviewTaskService.schedule` 前精确比较会话 ID、任务 ID 和日期；系统提示负责引导确认，工具端负责不可绕过的写入校验。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI 1.0.0、JUnit 5、Mockito、MockMvc

## Global Constraints

- 所有写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- 不修改或提交用户已有的 `src/main/resources/application.yml` 和前端工作区改动。
- 新增或修改的代码注释、日志、异常和用户提示默认使用简体中文。
- 只执行助手、工具和控制器的针对性测试及最小编译。

---

### Task 1: 锁定结构化确认契约

**Files:**
- Modify: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewToolsTest.java`
- Modify: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewAssistantServiceTest.java`
- Modify: `src/test/java/com/javainterview/rag/interview/web/InterviewReviewAssistantControllerTest.java`

**Interfaces:**
- Consumes: 现有 `schedule_review_task`、`InterviewAssistantRequestContext` 和助手 HTTP 接口。
- Produces: 未确认拒绝、确认不匹配拒绝、确认匹配放行的测试契约。

- [x] **Step 1: 写失败测试**

```java
assertThrows(IllegalStateException.class,
        () -> tools.scheduleReviewTask("session-1", "task-1", "2026-08-12", null));

InterviewReviewScheduleConfirmation confirmation =
        new InterviewReviewScheduleConfirmation("session-1", "task-1", LocalDate.of(2026, 8, 12));
```

- [x] **Step 2: 验证测试先失败**

```shell
.\mvnw.cmd -Dmaven.repo.local=.m2 -Dtest=InterviewReviewToolsTest,InterviewReviewAssistantServiceTest,InterviewReviewAssistantControllerTest test
```

预期：确认类型或新服务签名不存在，测试编译失败。

### Task 2: 实现不可绕过的精确确认

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewScheduleConfirmation.java`
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewAssistantRequestContext.java`
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewTools.java`
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewAssistantService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewAssistantRequest.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewAssistantController.java`

**Interfaces:**
- Consumes: `InterviewReviewScheduleConfirmation(sessionId, taskId, plannedReviewDate)`。
- Produces: `respond(String message, InterviewReviewScheduleConfirmation confirmation)`；工具只在确认三元组完全匹配时调用 `schedule`。

- [x] **Step 1: 增加确认值对象和请求字段**

```java
public record InterviewReviewScheduleConfirmation(
        String sessionId, String taskId, LocalDate plannedReviewDate) {
}

public record InterviewReviewAssistantRequest(
        String message,
        InterviewReviewScheduleConfirmation scheduleConfirmation) {
}
```

- [x] **Step 2: 经 ToolContext 传递确认并强制匹配**

```java
requestContext.requireScheduleConfirmation(
        normalizedSessionId, normalizedTaskId, normalizedDate);
```

- [x] **Step 3: 更新系统提示的确认状态说明**

未携带确认时要求助手只复述目标操作并请求确认；携带确认时仍以工具端精确参数校验为最终边界。

- [x] **Step 4: 运行针对性测试**

```shell
.\mvnw.cmd -Dmaven.repo.local=.m2 -Dtest=InterviewReviewToolsTest,InterviewReviewAssistantServiceTest,InterviewReviewAssistantControllerTest test
```

预期：全部通过。

### Task 3: 文档、验证与提交

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/03-阶段路线.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 已通过测试的 HTTP 契约和工具边界。
- Produces: 与真实实现一致的阶段状态、调研结论和 ADR。

- [x] **Step 1: 更新事实文档**

记录请求示例、确认精确匹配语义、无新表/无对话历史、两份 MIT 参考项目的边界结论。

- [x] **Step 2: 最小编译并检查差异**

```shell
.\mvnw.cmd -Dmaven.repo.local=.m2 -DskipTests compile
git diff --check
git status --short
```

- [x] **Step 3: 只暂存本轮文件并提交**

```shell
git commit -m "feat: 增加复习改期人工确认"
```

不得暂存前端文件和 `src/main/resources/application.yml`。
