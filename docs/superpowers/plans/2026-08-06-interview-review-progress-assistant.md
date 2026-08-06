# 单会话复盘助手 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让用户在 `/interview` 完成页一键请求助手分析本场复习进展，并确保分析只来自持久化分数、练习次数和未解决遗漏。

**Architecture:** 复用 `InterviewReviewTaskService.findAttemptHistory(sessionId)` 作为唯一事实源，在 `InterviewReviewTools` 增加只读 `get_review_progress`，只映射汇总与逐题分数，不返回回答正文或 attempt 明细。前端继续复用现有助手 API 和 composable，在同一面板增加“复盘进展”模式，不修改 `InterviewView` 的数据所有权。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI 1.0.0 Tool Calling、JUnit 5、Mockito、Vue 3 `<script setup lang="ts">`、Vite 8、Playwright。

## Global Constraints

- 所有项目写入只允许发生在 `D:\IdeaProjects\java-interview-rag`；`D:\IdeaProjects\blog\blog-springboot` 只读。
- 新增或修改的代码注释、日志、异常和用户提示默认使用简体中文。
- 保护现有前端视觉差异、`docs/05-开发记录.md`、`docs/06-参考项目调研.md` 和 `src/main/resources/application.yml` 的用户改动。
- 本轮只完成“单会话复盘助手”一个纵向功能，不增加数据库表、跨会话查询、对话记忆、MCP 或新的写入工具。
- 验证不调用付费模型、Embedding、Qdrant 或数据库。

---

### Task 1: 固化较大闭环开发粒度

**Files:**
- Modify: `.codex/autonomous-development-prompt.md`

**Interfaces:**
- Consumes: 用户要求后续每轮进行较大幅度开发。
- Produces: 默认选择中等规模端到端闭环的长期自动化规则。

- [x] **Step 1: 增加开发粒度规则**

在“每轮闭环”之前明确：除局部缺陷、安全修复或外部阻塞外，不把可以一次集成验证的后端、接口、前端和文档拆成多个微小轮次；每轮仍只围绕一个业务目标。

- [x] **Step 2: 检查规则不扩大安全权限**

Run: `git diff -- .codex/autonomous-development-prompt.md`

Expected: 只调整开发粒度，不改变付费、生产、仓库隔离和用户改动保护规则。

### Task 2: 增加只读复习进展工具

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewTools.java`
- Modify: `src/main/java/com/javainterview/rag/interview/tool/InterviewReviewAssistantService.java`
- Test: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewToolsTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/tool/InterviewReviewAssistantServiceTest.java`

**Interfaces:**
- Consumes: `InterviewReviewTaskService.findAttemptHistory(String)` and `InterviewReviewSessionHistory`。
- Produces: `get_review_progress(sessionId, ToolContext)`、`ReviewProgressToolResult` 和 `ReviewProgressTaskToolItem`。

- [x] **Step 1: 写失败测试**

```java
InterviewReviewTools.ReviewProgressToolResult result =
        tools.getReviewProgress("session-1", new ToolContext(requestContext.asMap()));

assertEquals(2, result.totalTaskCount());
assertEquals(1, result.improvedTaskCount());
assertEquals(List.of("仍缺少监控"), result.tasks().get(0).unresolvedGaps());
```

同时断言工具 callback 数量从 2 变为 3，`get_review_progress` schema 只包含 `sessionId`，不包含 `answer` 或 `attempts`。

- [x] **Step 2: 运行测试并确认失败**

Run: `mvn -Dtest=InterviewReviewToolsTest,InterviewReviewAssistantServiceTest test`

Expected: 因 `getReviewProgress` 和第三个 callback 尚不存在而失败。

- [x] **Step 3: 实现脱敏工具结果**

```java
@Tool(name = "get_review_progress",
        description = "汇总单个面试会话的复习进展，只返回练习次数、分数变化和未解决遗漏，不返回回答正文。")
public ReviewProgressToolResult getReviewProgress(String sessionId, ToolContext toolContext) {
    String assistantRequestId = markToolExecution(toolContext);
    String normalizedSessionId = requireText(sessionId, "面试会话 ID 不能为空");
    return executeLogged(GET_REVIEW_PROGRESS, assistantRequestId, normalizedSessionId,
            () -> ReviewProgressToolResult.from(
                    reviewTaskService.findAttemptHistory(normalizedSessionId)),
            ReviewProgressToolResult::totalTaskCount);
}
```

逐题结果只包含题号、练习次数、正式/最近/最好分数、相对正式回答分差、未解决遗漏和最近练习时间。

- [x] **Step 4: 扩展助手路由提示**

系统提示明确：任务清单用 `list_review_tasks`，练习进步/分数变化/遗漏复盘必须用 `get_review_progress`，改期仍用 `schedule_review_task`；只读复盘不需要人工确认。

- [x] **Step 5: 运行后端相关测试**

Run: `mvn -Dtest=InterviewReviewToolsTest,InterviewReviewAssistantServiceTest,InterviewReviewAssistantControllerTest test`

Expected: 工具映射、日志关联、提示绑定和现有改期确认全部通过。

### Task 3: 接入前端复盘进展模式

**Files:**
- Modify: `frontend/src/composables/useInterviewReviewAssistant.ts`
- Modify: `frontend/src/components/interview/InterviewReviewAssistantPanel.vue`

**Interfaces:**
- Consumes: 现有 `askInterviewReviewAssistant` 请求函数和当前 `sessionId` prop。
- Produces: `reviewProgress(sessionId): Promise<boolean>` 和 `PROGRESS` 面板模式。

- [x] **Step 1: 增加 composable 动作**

```ts
async function reviewProgress(sessionId: string): Promise<boolean> {
  pendingConfirmation.value = null;
  return send({
    message: `请使用 get_review_progress 分析会话 ${sessionId} 的复习进展，说明练习次数、分数变化和仍未解决的遗漏。`,
  }, "生成本场复盘失败");
}
```

- [x] **Step 2: 增加第三个分段模式**

将 `AssistantMode` 扩展为 `"QUERY" | "PROGRESS" | "SCHEDULE"`，新增带 `TrendingUp` 图标的“复盘进展”按钮和“生成本场复盘”命令。模式切换继续清理过期回复和待确认状态，不新增全局状态或聊天历史。

- [x] **Step 3: 构建前端**

Run: `cd frontend && npm run build`

Expected: `vue-tsc --noEmit` 与 Vite 生产构建成功。

- [x] **Step 4: 浏览器验证**

使用本地 HTTP 夹具验证“复盘进展”请求体精确包含当前会话 ID 和 `get_review_progress` 指令；桌面 1280px、移动 390px 无横向溢出，三段模式可操作，控制台无 warning/error。

### Task 4: 更新事实文档并提交

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/03-阶段路线.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 实际实现、测试和浏览器验证结果。
- Produces: 可追溯的阶段状态、参考判断和技术决策。

- [x] **Step 1: 更新文档**

记录 `aural-oss` 的练习统计/历史与 `hiring-agent` 的结构化评价启示；明确只采纳单会话复盘，不复制跨用户看板、招聘流程或多 Agent 架构。

- [x] **Step 2: 隔离暂存**

只暂存本轮文件；`docs/05`、`docs/06` 若包含用户差异，使用索引 blob 只加入本轮段落。断言 `frontend/tokens.css`、既有视觉文件、`InterviewView.vue` 和 `application.yml` 不在暂存区。

- [x] **Step 3: 最终检查并提交**

Run: `git diff --cached --check`

Expected: 无空白错误，暂存区只含单会话复盘助手闭环。

Commit: `feat: 增加单会话复盘助手`

## Self-Review

- Spec coverage: 后端事实源、只读工具、助手路由、前端入口、开发粒度规则、测试、浏览器验证和文档均有对应任务。
- Placeholder scan: 无 `TBD`、`TODO` 或未定义接口。
- Type consistency: 后端统一使用 `ReviewProgressToolResult` / `ReviewProgressTaskToolItem`；前端统一使用 `reviewProgress` 和 `PROGRESS`。
