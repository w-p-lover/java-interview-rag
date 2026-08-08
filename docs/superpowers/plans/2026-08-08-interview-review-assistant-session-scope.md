# 复习助手会话作用域 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将复习助手的当前会话从自然语言约定升级为结构化请求和模型不可见的服务端强制作用域，阻止工具查询或修改其他会话。

**Architecture:** `POST /api/interviews/review-assistant` 新增必填 `sessionId`，Controller 将其传给助手服务。助手服务校验会话和改期确认的一致性，把会话作用域放入既有 `InterviewAssistantRequestContext`；三个工具在调用 `InterviewReviewTaskService` 前统一执行作用域校验，并沿用现有失败执行日志。前端组合式函数继续接收页面已有会话 ID，但不再依赖自然语言消息传递该标识。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring AI 1.0、JUnit 5、Mockito、Vue 3 Composition API、TypeScript。

## Global Constraints

- 所有写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- 不修改或暂存用户已有前端视觉改动和 `src/main/resources/application.yml`。
- 新增或修改的注释、异常和用户可见提示使用简体中文。
- 不调用付费模型、MySQL、Qdrant 或 Embedding。
- 只执行直接相关后端测试、前端构建和差异检查。

---

### Task 1: 固定后端请求和隐藏上下文契约

**Files:**
- Modify: `src/test/java/com/javainterview/rag/interview/web/InterviewReviewAssistantControllerTest.java`
- Modify: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewAssistantServiceTest.java`
- Modify: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewToolsTest.java`

**Interfaces:**
- Consumes: 现有 `InterviewReviewAssistantService.respond`、`InterviewAssistantRequestContext` 和三个复习工具。
- Produces: `respond(String sessionId, String message, InterviewReviewScheduleConfirmation confirmation)`，以及上下文中的严格会话校验行为。

- [x] **Step 1: 为 Controller 增加结构化会话断言**

```java
verify(service).respond("session-1", "查询本场任务", null);
```

请求体缺少 `sessionId` 时，断言返回 HTTP 400 和“复习助手会话 ID 不能为空”。

- [x] **Step 2: 为助手服务增加前置校验断言**

```java
assertThrows(IllegalArgumentException.class,
        () -> service.respond("session-1", "确认改期", otherSessionConfirmation));
verifyNoInteractions(chatClient);
```

同时从 `toolContext` 取出请求上下文，验证当前会话为 `session-1`。

- [x] **Step 3: 为三个工具增加跨会话拒绝断言**

```java
ToolContext context = new ToolContext(
        InterviewAssistantRequestContext.create("session-1", null).asMap());
assertThrows(IllegalStateException.class,
        () -> tools.getReviewProgress("session-2", context));
verifyNoInteractions(reviewTaskService);
```

断言失败仍记录一次工具执行，状态为 `FAILED`，会话字段为模型实际传入的 `session-2`。

- [x] **Step 4: 运行测试确认旧实现不满足新契约**

```shell
.\mvnw.cmd -Dtest=InterviewReviewAssistantControllerTest,InterviewReviewAssistantServiceTest,InterviewReviewToolsTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test
```

预期：因请求记录和服务签名尚未包含 `sessionId`，或上下文没有会话校验能力而失败。

### Task 2: 实现服务端会话作用域

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewAssistantRequest.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewReviewAssistantController.java`
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewAssistantService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewAssistantRequestContext.java`
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewTools.java`

**Interfaces:**
- Consumes: 请求 `sessionId`、消息和可选人工确认。
- Produces: 模型不可见的 `sessionId` 作用域；三个工具只允许调用该会话。

- [x] **Step 1: 扩展请求记录与 Controller 转发**

```java
public record InterviewReviewAssistantRequest(
        String sessionId,
        String message,
        InterviewReviewScheduleConfirmation scheduleConfirmation) {}
```

Controller 调用 `service.respond(request.sessionId(), request.message(), request.scheduleConfirmation())`。

- [x] **Step 2: 在助手服务调用模型前校验作用域**

```java
String normalizedSessionId = requireText(sessionId, "复习助手会话 ID 不能为空");
if (confirmation != null && !confirmation.sessionId().equals(normalizedSessionId)) {
    throw new IllegalArgumentException("改期确认会话与当前会话不一致");
}
```

使用 `InterviewAssistantRequestContext.create(normalizedSessionId, confirmation)` 创建隐藏上下文。

- [x] **Step 3: 在请求上下文中保存并校验会话**

```java
void requireSessionScope(String sessionId) {
    if (!this.sessionId.equals(sessionId)) {
        throw new IllegalStateException("工具会话与当前复习助手会话不一致");
    }
}
```

- [x] **Step 4: 让三个工具统一执行作用域校验**

将 `requireSessionScope(toolContext, normalizedSessionId)` 放进 `executeLogged` 的 action，确保越权被业务调用前拒绝且失败事实仍进入既有执行日志；缺少助手请求上下文时也拒绝。

- [x] **Step 5: 运行三个相关测试类**

使用 Task 1 的 Maven 命令，预期全部通过且不访问外部服务。

### Task 3: 调整 Vue 请求契约

**Files:**
- Modify: `frontend/src/api/interview.ts`
- Modify: `frontend/src/composables/useInterviewReviewAssistant.ts`

**Interfaces:**
- Consumes: 组件已经传入的 `sessionId`。
- Produces: 每次助手请求都携带结构化 `sessionId`；用户消息只表达查询、复盘或改期意图。

- [x] **Step 1: 扩展 TypeScript 请求类型**

```ts
export interface InterviewReviewAssistantRequest {
  sessionId: string;
  message: string;
  scheduleConfirmation?: InterviewReviewScheduleConfirmation;
}
```

- [x] **Step 2: 更新组合式函数的四类请求**

查询、复盘、提出改期和确认改期均设置 `sessionId`。消息中保留任务 ID 与日期用于模型理解和人工确认语义，但不再把会话 ID 作为可信作用域来源。

- [x] **Step 3: 执行前端生产构建**

```shell
npm run build
```

预期：`vue-tsc --noEmit` 和 Vite 构建通过；不修改组件结构或样式。

### Task 4: 同步事实文档并提交

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/03-阶段路线.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 已通过测试和构建的真实实现。
- Produces: 与代码一致的会话作用域说明、调研结论、验证记录和 ADR。

- [x] **Step 1: 更新架构和接口事实**

明确助手请求必须携带结构化 `sessionId`，该字段进入模型不可见上下文；工具参数仍由模型生成，但服务端只允许匹配当前会话。

- [x] **Step 2: 追加开发记录和参考结论**

记录两个参考仓库版本未变化、均为 MIT；本轮只采纳“操作上下文由应用绑定”的质量边界，不复制外部实现。

- [x] **Step 3: 记录长期决策**

新增 ADR：会话作用域属于应用请求事实，不能只依赖 Prompt 或模型生成参数；写入仍额外保留精确人工确认。

- [x] **Step 4: 执行最小最终检查**

```shell
git diff --check
git status --short
git diff -- <本轮文件>
```

确认未修改 `blog-springboot`，未暂存用户视觉文件和 `application.yml`，文档用户改动仍保留在工作区。

- [x] **Step 5: 提交直接相关文件**

```shell
git commit -m "fix: 限制复习助手会话作用域"
```

提交后再次检查 `git status --short`，确保只剩用户原有改动。
