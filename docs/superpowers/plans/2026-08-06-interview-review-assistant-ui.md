# 复习助手前端接入 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在已完成面试的复习区域提供任务查询和经过精确人工确认的助手改期入口。

**Architecture:** `InterviewView` 继续作为组合层，新建 `InterviewReviewAssistantPanel` 承载可见交互，新建 `useInterviewReviewAssistant` 管理异步请求与单次确认状态，`api/interview.ts` 只扩展后端已存在的请求契约。改期三元组来自用户选择的任务和日期，不解析模型自然语言；确认成功后由视图刷新现有任务列表。

**Tech Stack:** Vue 3.5、Composition API、TypeScript、Axios、`@lucide/vue`、Vite 8

## Global Constraints

- 所有项目写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- 当前未提交前端视觉改动是稳定基线，不回滚、不重排、不混入无关重构。
- 使用 `<script setup lang="ts">`、`shallowRef`、`computed`、typed props/emits。
- 不新增状态库、聊天历史、模型文本解析、后端接口或依赖。
- 新增用户可见文案使用简体中文，不展示 RAG、ToolContext 等实现词。

---

### Task 1: 助手 API 与请求状态

**Files:**
- Modify: `frontend/src/api/interview.ts`
- Create: `frontend/src/composables/useInterviewReviewAssistant.ts`

**Interfaces:**
- Consumes: `POST /api/interviews/review-assistant`。
- Produces: `askReviewAssistant(payload)`、`queryTasks(sessionId, message)`、`requestSchedule(task, date)`、`confirmSchedule()`。

- [x] **Step 1: 增加请求和响应类型**

```ts
export interface InterviewReviewScheduleConfirmation {
  sessionId: string;
  taskId: string;
  plannedReviewDate: string;
}

export interface InterviewReviewAssistantResponse {
  requestId: string;
  answer: string;
}
```

- [x] **Step 2: 实现请求函数**

```ts
export async function askInterviewReviewAssistant(
  payload: InterviewReviewAssistantRequest,
): Promise<InterviewReviewAssistantResponse> {
  const response = await request.post("/interviews/review-assistant", payload, {
    timeout: 120_000,
  });
  return response.data;
}
```

- [x] **Step 3: 实现单请求确认 composable**

查询消息自动带当前会话 ID；提出改期时先发送不含 `scheduleConfirmation` 的请求，成功后保存用户选择的三元组；确认时才携带三元组并在成功后清空待确认状态。

- [x] **Step 4: 运行类型检查确认 API 契约**

```shell
cd frontend
npm run typecheck
```

预期：新增 API 和 composable 无 TypeScript 错误。

### Task 2: 可见查询与改期确认面板

**Files:**
- Create: `frontend/src/components/interview/InterviewReviewAssistantPanel.vue`

**Interfaces:**
- Props: `sessionId: string`、`tasks: InterviewReviewTask[]`。
- Emits: `scheduled: []`。
- Consumes: `useInterviewReviewAssistant()`。

- [x] **Step 1: 建立查询/改期分段模式**

```vue
<button :aria-pressed="mode === 'QUERY'" @click="setMode('QUERY')">查询任务</button>
<button :aria-pressed="mode === 'SCHEDULE'" @click="setMode('SCHEDULE')">调整计划</button>
```

- [x] **Step 2: 查询模式隐藏注入会话 ID**

用户只输入自然语言问题；组件调用 `queryTasks(props.sessionId, queryMessage)`，不要求复制内部标识。

- [x] **Step 3: 改期模式使用结构化选择**

```ts
const selectedTask = computed(() =>
  props.tasks.find((task) => task.id === selectedTaskId.value) ?? null,
);
```

只允许选择待复习任务和有效日期；模型回答后显示题号、题目与日期，用户点击“确认改期”后才提交三元组。

- [x] **Step 4: 完成状态与响应式样式**

覆盖默认、hover、focus-visible、disabled、loading、error 和回答状态；桌面使用紧凑工具带，40rem 以下改为单列，不使用模态框或嵌套卡片。

### Task 3: 页面接入、验证与文档

**Files:**
- Modify: `frontend/src/views/InterviewView.vue`
- Modify: `README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: `InterviewReviewAssistantPanel` 的 `scheduled` 事件。
- Produces: 完成页中“报告 → 复习助手 → 复习题目”的任务顺序。

- [x] **Step 1: 在完成页组合助手面板**

```vue
<InterviewReviewAssistantPanel
  :session-id="session.id"
  :tasks="reviewTasks"
  @scheduled="handleAssistantScheduled"
/>
```

- [x] **Step 2: 改期成功后刷新当前筛选**

```ts
function handleAssistantScheduled(): void {
  void filterReviewTasks(reviewTaskQuery.value);
}
```

- [x] **Step 3: 执行前端构建**

```shell
cd frontend
npm run build
```

预期：`vue-tsc --noEmit` 与 Vite 构建均通过。

- [x] **Step 4: 浏览器验证**

在桌面和移动视口使用本地响应夹具验证查询、提出改期、确认改期、失败提示、按钮禁用和无横向溢出；不调用付费模型。

- [x] **Step 5: 更新事实文档并提交**

```shell
git commit -m "feat: 接入复习助手确认界面"
```

只提交本轮前端增量与对应文档；`application.yml` 和其他既有差异保持未暂存。
