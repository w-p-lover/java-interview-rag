# 专注面试台界面 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `/interview` 从宣传式工作台改成任务优先、文案克制、可在桌面与移动端长期使用的专注面试训练界面。

**Architecture:** 保留现有 Vue 3 Composition API、路由和 `useInterviewSession` 状态边界，只调整视觉层、组件布局和文案。新增一个 `InterviewSessionBar.vue` 组件承载会话状态，其余改动在现有面试组件内完成，不改后端接口。

**Tech Stack:** Vue 3.5、Vite 8、TypeScript、Sass、OKLCH CSS tokens、`@lucide/vue`。

## Global Constraints

- 所有写入只允许发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅可只读参考。
- 不修改后端、数据库、接口字段、业务状态和 `src/main/resources/application.yml`。
- 保留 `useInterviewSession` 的 API 状态管理，不引入 Pinia 或新路由。
- 主路径不得出现 `RAG grounded`、`chunk`、`Prompt/rubric/model`、英文 eyebrow 或装饰性编号。
- 移动端 320、375、414、768 px 不允许横向滚动，按钮文字不换行。

---

### Task 1: 全局视觉令牌与依赖

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Modify: `frontend/tokens.css`
- Modify: `frontend/src/styles/index.scss`

**Interfaces:**
- Produces: 新的 Cobalt 风格 token；所有组件继续使用 `var(--color-*)`、`var(--space-*)`、`var(--radius-*)`。

- [x] 安装维护中的 `@lucide/vue`，不增加其他 UI 框架。
- [x] 将 `tokens.css` stamp 更新为 `Split Studio · Cobalt · N9 · footer none`，保留 OKLCH 和 4pt scale。
- [x] 调整背景为中性浅灰白，控件圆角收敛到 6–8 px，阴影降低到 0–8 px。
- [x] 全局按钮/表单 focus-visible 保持可见，禁用态和选中态不靠颜色单独表达。
- [x] 运行 `npm run build` 验证依赖和类型。

### Task 2: 导航与会话状态条

**Files:**
- Modify: `frontend/src/components/WorkspaceHeader.vue`
- Create: `frontend/src/components/interview/InterviewSessionBar.vue`
- Modify: `frontend/src/views/InterviewView.vue`

**Interfaces:**
- `InterviewSessionBar` props: `targetRole: string`、`status: "IN_PROGRESS" | "COMPLETED"`、`currentQuestionIndex: number`、`questionCount: number`、`progress: number`、`loading: boolean`。
- `InterviewSessionBar` emits: `reset: []`。

- [x] 将导航文案改成 `资料库`、`面试训练`，状态文本改成更短的 `就绪`。
- [x] 移除页面底部技术口号，避免重复品牌解释。
- [x] 在 `InterviewView` 中删除大 Hero，直接进入未开始/进行中/完成工作区。
- [x] 新增 `InterviewSessionBar`，用于进行中和完成状态的顶部窄条。
- [x] 运行 `npm run build` 验证组件接口。

### Task 3: 未开始准备表单

**Files:**
- Modify: `frontend/src/components/interview/InterviewSetupForm.vue`
- Modify: `frontend/src/components/interview/InterviewTextFileField.vue`

**Interfaces:**
- `InterviewSetupForm` props/emits 不变。
- `InterviewTextFileField` `v-model`、props 不变；内部使用 `details` 收起备用文本。

- [x] 将表单标题改为 `开始一场模拟面试`，说明压缩为一句用户任务文案。
- [x] 布局改为主栏材料 + 侧栏题目设置和开始按钮。
- [x] 文件控件显示当前文件、支持格式和提取状态；备用文本默认折叠。
- [x] 删除 `01 / 面试配置`、`不是固定三题` 等解释式文案。
- [x] 运行 `npm run build` 验证类型。

### Task 4: 作答、反馈、报告与复习

**Files:**
- Modify: `frontend/src/components/interview/InterviewQuestionPanel.vue`
- Modify: `frontend/src/components/interview/InterviewFeedbackPanel.vue`
- Modify: `frontend/src/components/interview/InterviewReportPanel.vue`
- Modify: `frontend/src/components/interview/InterviewReviewTaskPanel.vue`
- Modify: `frontend/src/components/interview/InterviewReviewAttemptPanel.vue`

**Interfaces:**
- 现有 props/emits 全部保持。

- [x] 当前题卡片改成主作答面板，按钮文案统一为 `提交回答` / `正在提交…`。
- [x] 资料状态改成 `有资料参考` 或 `仅根据回答评价`。
- [x] 反馈面板改为紧凑侧栏，首题空状态只提示提交后可见。
- [x] 报告面板先展示总分和三维度，再展示优势/补强；删除编号和技术标签。
- [x] 复习任务文案改为 `复习题目`、`标记完成`、`再次作答`；评价版本收进 `details`。
- [x] 运行 `npm run build`。

### Task 5: 验证、文档与提交

**Files:**
- Modify: `README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `.hallmark/log.json`

**Interfaces:**
- Produces: committed redesign and factual memory update。

- [x] 运行 `npm run build`。
- [x] 通过本地页面检查 320、375、414、768、1440 px 的无横向滚动和可读性。
- [x] 检查控制台 error/warn、键盘焦点、文件输入、details 展开和按钮忙碌态。
- [x] 更新 README、当前进度、开发记录和 ADR，说明只改前端界面。
- [x] 确认 `application.yml` 不纳入暂存，并提交中文 commit。
- [x] 遵守“所有写入只在目标仓库”约束，不更新仓库外 automation memory。
