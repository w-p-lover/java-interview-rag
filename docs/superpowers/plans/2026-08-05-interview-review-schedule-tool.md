# 复习计划写入工具与真实调用 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 增加会话内复习任务计划日期写入工具，并通过受限 ChatClient 助手验证模型能正确调用查询和改期工具。

**Architecture:** 在现有 `InterviewReviewTools` 中新增 `schedule_review_task`，直接复用 `InterviewReviewTaskService.schedule` 的会话/任务联合定位和幂等覆盖语义，并与 `list_review_tasks` 共用无正文执行日志。新增最小助手服务和 HTTP 入口，只向模型注册这两个工具；真实验证使用隔离数据库中的合成会话与任务，完成后精确清理。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring AI 1.0.0、OpenAI 兼容 ChatClient、MyBatis-Plus、MySQL 5.7、JUnit 5、Mockito、MockMvc。

## Global Constraints

- 所有仓库文件写入仅发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅只读复用安全配置，不修改代码、配置或业务数据。
- 保留用户已有的 `src/main/resources/application.yml` 端口修改，不暂存、不提交。
- 写入工具必须同时提供明确的 `sessionId`、`taskId` 和 `yyyy-MM-dd` 日期，不允许模型猜测标识。
- 助手只注册 `list_review_tasks` 与 `schedule_review_task`，不暴露完成任务、提交回答或任意数据库能力。
- 工具执行日志不保存用户消息、回答正文、知识正文或工具返回正文。
- 真实验证最多执行两次 Chat 请求：一次查询，一次改期；完成后停止临时服务并清理合成数据。

---

### Task 1: 计划日期写入工具

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewTools.java`
- Modify: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewToolsTest.java`

**Interfaces:**
- Consumes: `InterviewReviewTaskService.schedule(String sessionId, String taskId, LocalDate plannedReviewDate)`。
- Produces: Spring AI 工具 `schedule_review_task(sessionId, taskId, plannedReviewDate)`。

- [x] **Step 1: 写成功改期、非法日期和工具元数据测试**

```java
var result = tools.scheduleReviewTask("session-1", "task-1", "2026-08-12");
assertEquals(LocalDate.of(2026, 8, 12), result.plannedReviewDate());
verify(service).schedule("session-1", "task-1", LocalDate.of(2026, 8, 12));
```

- [x] **Step 2: 运行测试确认缺少工具方法或元数据**

Run: `mvnw.cmd -Dtest=InterviewReviewToolsTest test`

- [x] **Step 3: 增加工具并抽取共用执行日志模板**

```java
@Tool(name = "schedule_review_task", description = "...")
public ReviewTaskScheduleToolResult scheduleReviewTask(
        String sessionId, String taskId, String plannedReviewDate) {
    return executeLogged("schedule_review_task", sessionId, () -> {
        LocalDate date = parseRequiredDate(plannedReviewDate);
        return ReviewTaskScheduleToolResult.from(reviewTaskService.schedule(sessionId, taskId, date));
    }, ignored -> 1);
}
```

- [x] **Step 4: 运行工具测试并确认通过**

Run: `mvnw.cmd -Dtest=InterviewReviewToolsTest test`

### Task 2: 受限复习助手入口

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewAssistantService.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewAssistantRequest.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewAssistantResponse.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewAssistantController.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionConfiguration.java`
- Test: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewAssistantServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewReviewAssistantControllerTest.java`

**Interfaces:**
- Consumes: `ChatClient` 与 `ToolCallbackProvider`。
- Produces: `InterviewReviewAssistantService.respond(String message)`。
- Produces: `POST /api/interviews/review-assistant`，请求 `{"message":"..."}`，响应 `{"answer":"..."}`。

- [x] **Step 1: 写 ChatClient 工具绑定和输入边界测试**

```java
when(responseSpec.content()).thenReturn("已查询到 1 条任务。");
assertEquals("已查询到 1 条任务。", service.respond("查询会话 session-1"));
verify(requestSpec).toolCallbacks(toolCallbackProvider);
```

- [x] **Step 2: 写 HTTP 成功、空消息和超长消息测试**

```java
mockMvc.perform(post("/api/interviews/review-assistant")
        .contentType(APPLICATION_JSON)
        .content("{\"message\":\"查询会话 session-1\"}"))
        .andExpect(status().isOk());
```

- [x] **Step 3: 实现只注册复习工具的助手和控制器**

```java
return chatClient.prompt()
        .system(SYSTEM_PROMPT)
        .user(normalizedMessage)
        .toolCallbacks(toolCallbackProvider)
        .call()
        .content();
```

- [x] **Step 4: 运行助手与控制器测试并确认通过**

Run: `mvnw.cmd -Dtest=InterviewReviewAssistantServiceTest,InterviewReviewAssistantControllerTest test`

### Task 3: 文档、真实模型验证与收尾

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/03-阶段路线.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Create: `evaluation/results/2026-08-05-interview-review-tool-calling.json`

**Interfaces:**
- Consumes: 临时 `storage` 实例、独立 MySQL、真实 `qwen3.7-plus`。
- Produces: 查询和改期两次真实 Chat 调用的工具执行事实与无正文结果报告。

- [x] **Step 1: 执行直接相关测试和 diff 检查**

Run: `mvnw.cmd -Dtest=InterviewReviewToolsTest,InterviewReviewAssistantServiceTest,InterviewReviewAssistantControllerTest test`

Run: `git diff --check`

- [x] **Step 2: 创建合成会话与一条待复习任务**

使用独立测试 UUID，保存最小合法会话/任务数据；执行前后均按精确 UUID 查询，不接触其他行。

- [x] **Step 3: 在独立端口执行两次真实助手请求**

第一次要求查询指定会话任务，第二次明确要求把指定任务安排到 `2026-08-12`。核对模型响应、数据库日期和 `list_review_tasks`/`schedule_review_task` 执行日志。

- [x] **Step 4: 停止临时服务并精确清理合成数据**

删除本轮合成任务、会话及其工具执行日志，确认 UUID 剩余 0 行；保留不含正文的结果 JSON。

- [x] **Step 5: 同步项目文档并提交**

```shell
git commit -m "feat: 增加复习计划写入工具"
```
