# Interview Assistant Request Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为每次已通过参数校验的复习助手请求生成可查询 UUID，关联该请求触发的工具执行，并记录最终响应状态、累计 token 用量和总耗时。

**Architecture:** `InterviewReviewAssistantService` 创建请求上下文并通过 Spring AI `toolContext` 传入工具方法；`InterviewReviewTools` 从 `ToolContext` 读取请求 UUID，写入现有工具执行日志。独立的助手请求日志只保存元数据，不保存用户消息、最终回答、工具参数或结果正文；Spring AI 1.0.0 `OpenAiChatModel` 已确认会把工具调用前后的 usage 累加到最终 `ChatResponse`，但框架未公开 provider 调用回合数，因此本轮不记录或推算该字段。

**Tech Stack:** Java 17、Spring Boot 3.5.16、Spring AI 1.0.0、MyBatis-Plus、MySQL 5.7、JUnit 5、Mockito。

## Global Constraints

- 所有写入只允许发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅可只读参考，本轮不需要访问。
- 保护用户对 `src/main/resources/application.yml` 的端口修改，不修改、不暂存、不提交。
- 新增或修改的代码注释、日志、异常和用户可见文字默认使用简体中文。
- 不新增重试、对话历史、更多工具、通用 Agent Runtime、MCP 或前端仪表盘。
- 只运行直接相关测试；真实付费模型验证最多执行一个能触发工具调用的请求。

---

### Task 1: Define Assistant Request Log Boundary

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/tool/InterviewAssistantRequestLog.java`
- Create: `src/main/java/com/javainterview/rag/interview/tool/InterviewAssistantRequestLogStore.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/InterviewAssistantRequestLogEntity.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/InterviewAssistantRequestLogMapper.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/MySqlInterviewAssistantRequestLogStore.java`
- Test: `src/test/java/com/javainterview/rag/interview/persistence/MySqlInterviewAssistantRequestLogStoreTest.java`

**Interfaces:**
- Consumes: MyBatis-Plus `BaseMapper.insert` and the repository's existing immutable log-record pattern.
- Produces: `InterviewAssistantRequestLogStore.record(InterviewAssistantRequestLog)` and `findRecent(int)`.

- [x] **Step 1: Write the failing persistence mapping test**

```java
store.record(new InterviewAssistantRequestLog(
        null, "request-1", "qwen3.7-plus", Status.SUCCEEDED,
        1, 120, 30, 150, 800, true, null, createdAt));
assertEquals("request-1", captured.getRequestId());
assertEquals(150, captured.getTotalTokens());
```

- [x] **Step 2: Run the focused test and verify RED**

Run: `mvnw.cmd -Dtest=MySqlInterviewAssistantRequestLogStoreTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

Expected: compilation fails because the request-log types do not exist.

- [x] **Step 3: Implement the minimal model and persistence mapping**

```java
public record InterviewAssistantRequestLog(
        Long id, String requestId, String modelName, Status status,
        int toolExecutionCount, Integer promptTokens, Integer completionTokens,
        Integer totalTokens, long elapsedMillis, boolean finalResponsePresent,
        String errorType, LocalDateTime createdAt) {
}
```

The model validates nonnegative counts and usage, success/failure state consistency, and all-or-none token fields. The store maps only metadata and exposes recent logs ordered by creation time.

- [x] **Step 4: Run the focused test and verify GREEN**

Run the same command and expect all tests in `MySqlInterviewAssistantRequestLogStoreTest` to pass.

### Task 2: Correlate Assistant Requests and Tool Executions

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/tool/InterviewAssistantRequestContext.java`
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewAssistantService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewTools.java`
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewToolExecutionLog.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewToolExecutionLogEntity.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewToolExecutionLogMapper.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/MySqlInterviewToolExecutionLogStore.java`
- Test: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewAssistantServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewToolsTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/persistence/MySqlInterviewToolExecutionLogStoreTest.java`

**Interfaces:**
- Consumes: `ChatClient.ChatClientRequestSpec.toolContext(Map<String, Object>)`, `CallResponseSpec.chatResponse()`, and `ChatResponseMetadata.getUsage()`.
- Produces: `InterviewReviewAssistantResult(requestId, answer)` and nullable `assistantRequestId` on each tool execution log.

- [x] **Step 1: Write failing service and tool-correlation tests**

```java
verify(requestSpec).toolContext(argThat(context ->
        context.containsKey(InterviewAssistantRequestContext.KEY)));
verify(requestLogStore).record(argThat(log ->
        log.status() == Status.SUCCEEDED && log.totalTokens() == 150));
assertEquals(requestId, capturedToolLog.assistantRequestId());
```

Also assert that the Spring AI generated tool schema does not expose `ToolContext` and that a failed model call records only exception type, not exception message.

- [x] **Step 2: Run focused tests and verify RED**

Run: `mvnw.cmd -Dtest=InterviewReviewAssistantServiceTest,InterviewReviewToolsTest,MySqlInterviewToolExecutionLogStoreTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

Expected: compilation or assertion failure for the missing request context, request result, and correlation field.

- [x] **Step 3: Implement request context, cumulative usage capture, and correlation**

```java
InterviewAssistantRequestContext requestContext = InterviewAssistantRequestContext.create();
ChatResponse response = chatClient.prompt()
        .system(SYSTEM_PROMPT)
        .user(normalizedMessage)
        .toolContext(requestContext.asMap())
        .toolCallbacks(toolCallbackProvider)
        .call()
        .chatResponse();
```

Each `@Tool` method accepts a final `ToolContext` argument. The tool context increments an in-memory invocation counter and returns the request UUID before business execution; the assistant request logger records cumulative usage from the final response and never stores message or response text.

- [x] **Step 4: Run focused tests and verify GREEN**

Run the same command and expect all selected tests to pass.

### Task 3: Expose Logs, Migrate Schema, Verify One Real Request, and Document

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/tool/InterviewAssistantRequestLogQueryService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewAssistantResponse.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewAssistantController.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionConfiguration.java`
- Create: `deploy/mysql/init/013_interview_assistant_request_observability.sql`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewReviewAssistantControllerTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/tool/InterviewAssistantRequestLogQueryServiceTest.java`
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/03-阶段路线.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: `InterviewAssistantRequestLogStore.findRecent(int)` and the existing storage-profile controller/configuration pattern.
- Produces: `POST /api/interviews/review-assistant` response `{requestId, answer}` and `GET /api/interviews/review-assistant/requests?limit=20`.

- [x] **Step 1: Write failing controller and query validation tests**

```java
mockMvc.perform(get("/api/interviews/review-assistant/requests"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].requestId").value("request-1"));
mockMvc.perform(post("/api/interviews/review-assistant") /* request body */)
        .andExpect(jsonPath("$.requestId").value("request-1"));
```

- [x] **Step 2: Implement query API, bean wiring, and migration**

Migration `013` creates `t_interview_assistant_request_log`, then adds nullable `assistant_request_id` and an index to `t_interview_tool_execution_log`. No foreign key is added so a tool execution log can survive request-log persistence failure.

- [x] **Step 3: Run the complete focused test set**

Run: `mvnw.cmd -Dtest=InterviewReviewAssistantServiceTest,InterviewReviewToolsTest,InterviewAssistantRequestLogQueryServiceTest,MySqlInterviewAssistantRequestLogStoreTest,MySqlInterviewToolExecutionLogStoreTest,InterviewReviewAssistantControllerTest,InterviewToolExecutionLogControllerTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

Expected: all directly related tests pass with zero failures and errors.

- [x] **Step 4: Apply migration to isolated MySQL and run one paid request**

Start only a temporary backend port if needed. Create one synthetic completed interview session with one review task, call the assistant once with an explicit session ID so `list_review_tasks` executes, then verify:

```text
POST response requestId
  = assistant request log requestId
  = tool execution log assistantRequestId
toolExecutionCount = 1
promptTokens > 0, completionTokens > 0, totalTokens > 0
finalResponsePresent = true
```

Precisely delete the synthetic session, task, request log, and tool log after capturing a body-free evaluation result. Do not stop the user's service on port `8080`.

- [x] **Step 5: Update factual documentation and close the plan**

Document the correlation key, cumulative-usage semantics, privacy boundary, migration status, focused test result, real paid call count, and the remaining limitation that provider/model round count is not exposed or inferred.

- [x] **Step 6: Review diff and commit only related files**

```text
feat: 记录复习助手请求用量
```

Do not stage `src/main/resources/application.yml`.
