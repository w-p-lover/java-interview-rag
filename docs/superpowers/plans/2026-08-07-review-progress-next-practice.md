# 复盘后继续练习实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在单会话复盘成功后，根据已有持久化练习历史推荐一条仍有遗漏的可见任务，并让用户直接跳到该题的再次作答输入区。

**Architecture:** 不修改助手接口或模型 Prompt。`InterviewView` 只把现有 `reviewHistory` 下传给复习助手组件；组件使用纯 `computed` 从当前可见任务和历史中派生推荐项，优先未练习题，其次选择最近分数较低且仍有遗漏的题目；推荐入口使用已有作答文本框 ID 作为锚点。复盘后的再次生成仍由用户显式触发，避免自动产生模型费用。

**Tech Stack:** Vue 3.5、`<script setup lang="ts">`、TypeScript、现有 `@lucide/vue` 与 SCSS 令牌。

## Global Constraints

- 所有写入仅发生在 `D:\IdeaProjects\java-interview-rag`。
- 不覆盖或提交用户已有前端视觉改动与 `application.yml` 端口改动。
- 不新增 API、依赖、全局状态、聊天历史或自动模型调用。
- 新增用户可见文案使用简体中文，匹配现有复习助手样式和交互。
- 只执行 `npm run build`、差异检查等直接相关验证。

---

### Task 1: 复盘推荐与连续练习入口

**Files:**
- Modify: `frontend/src/views/InterviewView.vue`
- Modify: `frontend/src/components/interview/InterviewReviewAssistantPanel.vue`
- Modify: `frontend/src/components/interview/InterviewReviewAttemptPanel.vue`

**Interfaces:**
- Consumes: `InterviewReviewSessionHistory`、当前筛选后的 `InterviewReviewTask[]`、已有 `review-attempt-answer-${taskId}` 文本框 ID。
- Produces: `history` prop、复盘成功后的确定性推荐入口、作答结果区域返回复盘助手的锚点入口。

- [x] **Step 1: 扩展只读组件契约**

  在 `InterviewReviewAssistantPanel` 增加 `history: InterviewReviewSessionHistory | null` prop，由 `InterviewView` 下传现有 `reviewHistory`；不增加事件或可变共享状态。

- [x] **Step 2: 派生推荐任务**

  使用纯 `computed` 将可见任务与同 ID 历史关联，只保留 `unresolvedGaps` 非空项。排序规则固定为：未练习优先；同组按最新分数或正式回答分数升序；最后按原题顺序。推荐理由仅使用“尚未练习”或持久化的未解决遗漏数量。

- [x] **Step 3: 接通锚点操作**

  复盘请求成功后展示“继续练第 N 题”，链接到已有 `#review-attempt-answer-${taskId}`。已有 attempt 摘要提供“返回复盘助手”链接，目标为现有 `#review-assistant-title`。首次按钮显示“生成本场复盘”，成功后显示“刷新本场复盘”。

- [x] **Step 4: 保持现有视觉体系**

  复用 `--color-*`、`--space-*`、`--radius-control` 和 2.75rem 触控高度；不增加卡片、阴影、渐变或动画。锚点具备 hover、focus-visible 和移动端适配。

### Task 2: 文档、验证与提交

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/03-阶段路线.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 实际实现和验证输出。
- Produces: 与代码一致的当前能力、开发记录、参考采纳判断和长期决策。

- [x] **Step 1: 更新事实文档**

  记录确定性推荐规则、只在当前可见任务中推荐、模型回答不参与任务 ID 选择、再次复盘由用户显式触发，以及未执行真实模型或数据库验证。

- [x] **Step 2: 执行最小验证**

  运行 `npm run build`，确认 `vue-tsc --noEmit` 和 Vite 构建通过；运行 `git diff --check` 并检查目标文件差异。

- [x] **Step 3: 保护用户改动并提交**

  对脏文件只暂存本轮新增 hunk，检查缓存区不包含既有视觉改动和 `application.yml`，使用中文 `feat:` 提交信息完成本轮闭环。
