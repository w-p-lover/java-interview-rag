# 面试答题工作区视觉优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变面试会话状态、接口和数据流的前提下，将进行中页面从连续白色卡片改成主次明确、可长期专注作答的双栏工作区。

**Architecture:** `InterviewView` 继续只负责编排会话状态，并把题目与反馈组合为一个有统一边界的工具面板。`InterviewQuestionPanel` 独立负责题目、知识来源和答案提交，`InterviewFeedbackPanel` 独立负责上一题评价，`InterviewSessionBar` 独立负责会话进度与重置动作；所有 props、emits 与 `useInterviewSession` 保持不变。

**Tech Stack:** Vue 3.5、Composition API、`<script setup lang="ts">`、Sass、现有 OKLCH tokens、`@lucide/vue`。

## Global Constraints

- 所有写入只允许发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅可只读参考。
- 不修改后端、API 类型、路由、`useInterviewSession` 和 `src/main/resources/application.yml`。
- 不增加依赖、渐变、玻璃效果、装饰性背景、大圆角、宽阴影或彩色侧边竖条。
- 主路径继续只展示用户任务和真实状态，不增加实现说明或模型术语。
- 桌面和移动端不得出现横向溢出，按钮文字和分数内容不得互相遮挡。

---

### Task 1: 轻量会话进度轨

**Files:**
- Modify: `frontend/src/components/interview/InterviewSessionBar.vue`

**Interfaces:**
- Consumes: 现有 `targetRole`、`status`、`currentQuestionIndex`、`questionCount`、`progress`、`loading` props。
- Produces: 现有 `reset` 事件，签名不变。

- [x] 将独立白色卡片改为无圆角的进度轨，通过文本、进度条和操作建立层级。
- [x] 保留完成状态图标、移动端图标按钮和进度条动效，并继续支持减少动效模式。

### Task 2: 统一题目与反馈工作区

**Files:**
- Modify: `frontend/src/views/InterviewView.vue`
- Modify: `frontend/src/components/interview/InterviewQuestionPanel.vue`
- Modify: `frontend/src/components/interview/InterviewFeedbackPanel.vue`

**Interfaces:**
- Consumes: `InterviewQuestion`、`InterviewSessionAnswer | null` 和现有加载状态。
- Produces: `InterviewQuestionPanel` 继续只发送 `submit(answer: string)`；反馈组件保持纯展示。

- [x] 由 `InterviewView` 提供单一工具边界，桌面为题目主区与教练侧栏，窄屏按题目、反馈顺序纵向排列。
- [x] 题目区保留回答重置、来源折叠、字符计数和禁用状态，增强题目与考察目标的可扫读性。
- [x] 反馈区使用石墨表面，通过原生 `progress` 展示技术、完整、表达三项分数。
- [x] 用 Lucide 状态图标区分优势、遗漏和建议，删除彩色左侧竖条及重复小卡片。
- [x] 空反馈状态保持简洁，但在视觉上与已评价状态一致，不制造假评分。

### Task 3: 针对性验证与文档同步

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Produces: 与真实实现一致的当前界面说明和本轮验证记录。

- [x] 运行 `npm run build`，确认 `vue-tsc --noEmit` 和 Vite 生产构建通过。
- [x] 检查桌面与移动端答题状态、文本溢出、焦点、禁用态和控制台。
- [x] 运行 `git diff --check`，确认只暂存本轮文件且保留用户端口改动。
- [x] 更新项目文档并使用中文提交信息完成独立提交。
