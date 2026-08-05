# 面试工具执行日志 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为现有 `list_review_tasks` Spring AI 工具增加不保存回答正文的持久化执行记录，并允许按单个会话查询最近记录。

**Architecture:** 工具入口在参数归一化后记录成功或失败事实，日志存储异常只告警、不改变原工具结果或异常。`storage` profile 使用 MyBatis-Plus 写入独立 MySQL 表，查询入口固定在单会话路径下并限制返回数量。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring AI Tool Calling、MyBatis-Plus、MySQL 5.7/8、JUnit 5、Mockito、MockMvc。

## Global Constraints

- 所有仓库文件写入仅发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 只读，本轮不需要读取或修改。
- 保留用户已有的 `src/main/resources/application.yml` 端口修改，不暂存、不提交。
- 日志不保存正式回答、复习回答、知识 chunk 正文或异常消息。
- 不调用 Chat、Embedding、Qdrant，不为本轮引入新依赖。
- 只运行执行日志直接相关的 Java 测试。

---

### Task 1: 执行日志领域边界与查询

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/tool/InterviewToolExecutionLog.java`
- Create: `src/main/java/com/javainterview/rag/interview/tool/InterviewToolExecutionLogStore.java`
- Create: `src/main/java/com/javainterview/rag/interview/tool/InterviewToolExecutionLogQueryService.java`
- Test: `src/test/java/com/javainterview/rag/interview/tool/InterviewToolExecutionLogQueryServiceTest.java`

**Interfaces:**
- Produces: `InterviewToolExecutionLogStore.record(log)` 和 `findRecent(sessionId, limit)`。
- Produces: `InterviewToolExecutionLogQueryService.findRecent(sessionId, limit)`，校验非空会话 ID 和 `1..100` 条边界。

- [x] **Step 1: 写查询边界失败测试**

```java
assertThrows(IllegalArgumentException.class, () -> service.findRecent(" ", 20));
assertThrows(IllegalArgumentException.class, () -> service.findRecent("session-1", 101));
```

- [x] **Step 2: 运行测试确认因类型尚不存在而失败**

Run: `mvnw.cmd -Dtest=InterviewToolExecutionLogQueryServiceTest test`

- [x] **Step 3: 实现最小领域记录、Store 和查询服务**

```java
public interface InterviewToolExecutionLogStore {
    void record(InterviewToolExecutionLog log);
    List<InterviewToolExecutionLog> findRecent(String sessionId, int limit);
}
```

- [x] **Step 4: 运行查询服务测试并确认通过**

Run: `mvnw.cmd -Dtest=InterviewToolExecutionLogQueryServiceTest test`

### Task 2: MySQL 持久化与迁移

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/persistence/InterviewToolExecutionLogEntity.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/InterviewToolExecutionLogMapper.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/MySqlInterviewToolExecutionLogStore.java`
- Create: `src/test/java/com/javainterview/rag/interview/persistence/MySqlInterviewToolExecutionLogStoreTest.java`
- Create: `deploy/mysql/init/012_interview_tool_execution_log.sql`

**Interfaces:**
- Consumes: `InterviewToolExecutionLogStore`。
- Produces: `MySqlInterviewToolExecutionLogStore(InterviewToolExecutionLogMapper)`。

- [x] **Step 1: 写实体映射与倒序查询测试**

```java
verify(mapper).insert(captor.capture());
verify(mapper).findRecent("session-1", 20);
```

- [x] **Step 2: 运行测试确认因持久化类型尚不存在而失败**

Run: `mvnw.cmd -Dtest=MySqlInterviewToolExecutionLogStoreTest test`

- [x] **Step 3: 实现实体、Mapper、Store 和 MySQL 5.7/8 兼容建表脚本**

```sql
CREATE TABLE IF NOT EXISTS t_interview_tool_execution_log (...);
```

- [x] **Step 4: 运行 Store 测试并确认通过**

Run: `mvnw.cmd -Dtest=MySqlInterviewToolExecutionLogStoreTest test`

### Task 3: 工具执行记录与故障隔离

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewTools.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionConfiguration.java`
- Modify: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewToolsTest.java`

**Interfaces:**
- Consumes: `InterviewToolExecutionLogStore.record(log)`。
- Produces: `list_review_tasks` 每次有效会话 ID 调用对应一条 `SUCCEEDED` 或 `FAILED` 记录；日志写入故障不遮蔽工具结果。

- [x] **Step 1: 增加成功、业务失败和日志存储失败测试**

```java
verify(logStore).record(argThat(log -> log.status() == Status.SUCCEEDED));
verify(logStore).record(argThat(log -> log.status() == Status.FAILED));
assertDoesNotThrow(() -> tools.listReviewTasks("session-1", null, null, null));
```

- [x] **Step 2: 运行工具测试确认新断言失败**

Run: `mvnw.cmd -Dtest=InterviewReviewToolsTest test`

- [x] **Step 3: 在工具边界记录 UUID、工具名、会话、状态、结果数、耗时和异常类型**

```java
try {
    var result = queryTasks();
    record(SUCCEEDED, result.totalTaskCount(), null);
    return result;
} catch (RuntimeException exception) {
    record(FAILED, null, exception.getClass().getSimpleName());
    throw exception;
}
```

- [x] **Step 4: 注册 MySQL Store、查询服务并重新运行工具测试**

Run: `mvnw.cmd -Dtest=InterviewReviewToolsTest test`

### Task 4: 单会话日志查询接口与文档同步

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewToolExecutionLogController.java`
- Create: `src/test/java/com/javainterview/rag/interview/web/InterviewToolExecutionLogControllerTest.java`
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/03-阶段路线.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: `InterviewToolExecutionLogQueryService.findRecent(sessionId, limit)`。
- Produces: `GET /api/interviews/sessions/{sessionId}/tool-executions?limit=20`。

- [x] **Step 1: 写默认数量和非法数量的 MockMvc 测试**

```java
mockMvc.perform(get("/api/interviews/sessions/session-1/tool-executions"))
        .andExpect(status().isOk());
```

- [x] **Step 2: 实现只返回无正文日志的控制器并运行测试**

Run: `mvnw.cmd -Dtest=InterviewToolExecutionLogControllerTest test`

- [x] **Step 3: 同步阶段事实、执行记录与 ADR**

记录持久化字段、查询边界、故障隔离、未执行真实模型调用，以及独立 MySQL 迁移的实际验证结果。

- [x] **Step 4: 执行最终针对性验证并检查 diff**

Run: `mvnw.cmd -Dtest=InterviewReviewToolsTest,InterviewToolExecutionLogQueryServiceTest,MySqlInterviewToolExecutionLogStoreTest,InterviewToolExecutionLogControllerTest test`

Run: `git diff --check`

- [x] **Step 5: 仅暂存本轮文件并提交**

```shell
git commit -m "feat: 记录面试工具执行"
```
