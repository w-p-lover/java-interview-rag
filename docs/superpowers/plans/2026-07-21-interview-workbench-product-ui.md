# 求职面试工作台产品界面实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 Markdown 技术验证页重构为能表达求职面试产品主线的安静型工作台，同时保留已经真实实现的资料导入和切片检查能力。

**Architecture:** 只修改 Vue 前端的视图、导入面板、结果账页、流程导航和全局主题。后端 API 契约不变；首页以产品主线组织信息，资料导入作为当前可用的准备阶段，不伪造画像、面试和评分数据。

**Tech Stack:** Vue 3 Composition API、TypeScript、Vite、Vue Router、Sass、现有 Axios API 客户端。

## Global Constraints

- 所有写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\shoka-blog` 和 `D:\IdeaProjects\blog\blog-springboot` 只读参考。
- 不新增 UI 框架、Pinia、数据库、模型调用或未实现的求职结果。
- 新增或修改的代码注释默认使用简体中文，现有代码没有必要增加注释。
- 保留真实 Markdown 导入接口、首次/重复状态和 chunk 明细。

---

### Task 1: 重构首页产品信息架构

**Files:**
- Modify: `frontend/src/views/KnowledgeWorkbenchView.vue`
- Modify: `frontend/src/components/PipelineRail.vue`

- [x] 将页面标题改为“求职面试工作台”，用简短文案表达“先准备简历和资料，再进入模拟面试”。
- [x] 将流程标签改为“求职目标、简历档案、模拟面试、训练复盘”，保留状态语义但去掉技术管线语言。
- [x] 增加轻量导航和当前阶段状态，导航仅作为产品信息架构，不创建没有路由支持的假页面。
- [x] 删除 hero 式大标题、英文 eyebrow 和装饰性品牌徽章。

### Task 2: 重构导入与结果文案

**Files:**
- Modify: `frontend/src/components/ImportPanel.vue`
- Modify: `frontend/src/components/ChunkLedger.vue`

- [x] 将导入面板命名为“准备简历和资料”，说明当前真实能力是导入与结构检查。
- [x] 将按钮、空状态、重复状态和会话提示改为面向求职资料的明确文案。
- [x] 保留文件选择、拖放、来源编辑、错误提示、真实 API 请求和 chunk 内容。
- [x] 将结果账页命名为“导入检查”，展示版本、重复判断、标题路径和切片，不声称已经生成候选人画像或面试问题。

### Task 3: 建立低装饰产品主题

**Files:**
- Modify: `frontend/src/styles/theme.scss`
- Modify: `frontend/src/styles/index.scss`
- Modify: `frontend/src/views/KnowledgeWorkbenchView.vue`
- Modify: `frontend/src/components/ImportPanel.vue`
- Modify: `frontend/src/components/ChunkLedger.vue`
- Modify: `frontend/src/components/PipelineRail.vue`

- [x] 使用中性浅色背景、单一蓝色主操作色、语义绿色/琥珀色/红色状态色和系统无衬线字体。
- [x] 删除网格背景、渐变、发光点、宽阴影、3px 彩色侧边条和过大的圆角。
- [x] 统一边框、控件、按钮、结果行和空状态的尺寸与间距。
- [x] 保留窄屏堆叠布局、可见焦点和减少动效。

### Task 4: 验证并同步文档

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`

- [x] 执行 `npm run build`。
- [x] 使用真实浏览器检查桌面和 390×844 移动端页面，确认首屏产品定位清楚且无溢出。
- [x] 更新项目文档，明确“求职面试工作台”为产品方向，Markdown 导入为资料准备基础能力。
- [x] 执行 `git diff --check` 和 `git status --short`，提交直接相关文件。
