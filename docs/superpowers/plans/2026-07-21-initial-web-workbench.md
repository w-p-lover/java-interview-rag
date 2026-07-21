# Java 面试知识工坊首个 Web 闭环实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 Markdown multipart 导入 API，并搭建能调用该 API、展示版本与 chunk 的 Vue 3 知识工坊页面。

**Architecture:** 后端新增字符串内容导入核心和 Spring MVC 控制器，复用现有文档处理链；前端作为仓库内独立 Vite 子项目，通过开发代理访问 `/api`。功能和页面按两个可独立验证的提交顺序完成。

**Tech Stack:** Java 17、Spring Boot 3.5.16、JUnit 5、MockMvc、Vue 3、Vite、TypeScript、Vue Router、Axios、Sass、Playwright CLI。

## Global Constraints

- 所有实现写入仅发生在 `D:\IdeaProjects\java-interview-rag`；`D:\IdeaProjects\blog\shoka-blog` 只读参考。
- 后端先完成并验证，前端随后接入真实契约。
- 不增加数据库、向量库、认证、目录扫描、UI 组件库或 Pinia。
- Java 异常和界面提示使用简体中文；代码注释只解释不明显的边界。
- Node、npm 与 Playwright 缓存必须位于目标仓库内并保持 Git 忽略。

---

### Task 1: 用 MockMvc 测试固定上传 API

**Files:**
- Modify: `pom.xml`
- Create: `src/test/java/com/javainterview/rag/knowledge/web/MarkdownDocumentControllerTest.java`

- [x] 增加 `spring-boot-starter-web`，不增加数据库或其他基础设施依赖。
- [x] 编写一次测试内连续上传相同 Markdown 的 MockMvc 用例，断言首次响应包含版本与 chunk，第二次响应为重复且 chunk 为空。
- [x] 编写空逻辑来源返回 HTTP 400 和中文消息的用例。
- [x] 执行控制器测试并确认因生产类型缺失而 RED。

### Task 2: 实现内容导入核心与 HTTP 接口

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownContentImporter.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownFileImporter.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownDocumentController.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportResponse.java`

- [x] 将来源与字符串内容导入编排集中到 `MarkdownContentImporter`，文件导入器只负责 UTF-8 读取并委托。
- [x] 注册进程内登记器和固定最大 chunk 长度的内容导入器 Bean。
- [x] 实现 `POST /api/knowledge/documents/import` multipart 接口及结构化响应。
- [x] 将调用方参数错误映射为 HTTP 400 与中文 `message`。
- [x] 执行控制器测试和现有 Markdown 链路测试并确认 GREEN。
- [x] 审查后提交 `feat: 提供 Markdown 导入接口`。

### Task 3: 搭建精简 Vue 工程骨架

**Files:**
- Modify: `.gitignore`
- Create: `frontend/package.json`
- Create: `frontend/package-lock.json`
- Create: `frontend/index.html`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/src/vite-env.d.ts`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/api/request.ts`
- Create: `frontend/src/api/knowledge.ts`
- Create: `frontend/src/styles/theme.scss`
- Create: `frontend/src/styles/index.scss`

- [ ] 建立 Vue 3、Vite、TypeScript、Vue Router、Axios、Sass 依赖和脚本。
- [ ] 配置 `@` 别名及 `/api` 到 `http://localhost:8080` 的开发代理。
- [ ] 建立与 `shoka-blog` 相似但更精简的 API、路由、视图、组件和样式分层。
- [ ] 运行 npm 安装、类型检查和空壳生产构建。

### Task 4: 实现知识工坊界面并接入 API

**Files:**
- Create: `frontend/src/views/KnowledgeWorkbenchView.vue`
- Create: `frontend/src/components/ImportPanel.vue`
- Create: `frontend/src/components/PipelineRail.vue`
- Create: `frontend/src/components/ChunkLedger.vue`

- [ ] 实现文件选择、拖放、逻辑来源编辑、提交中和错误状态。
- [ ] 实现 `SOURCE → HASH → SECTION → CHUNK` 处理链脊柱。
- [ ] 实现导入结果、版本指纹、重复提示和 chunk 账页。
- [ ] 完成桌面、移动端、暗色偏好、键盘焦点和减少动效适配。
- [ ] 执行类型检查和生产构建。

### Task 5: 集成 QA 与项目文档闭环

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/superpowers/plans/2026-07-21-initial-web-workbench.md`

- [ ] 启动后端和前端，使用 Playwright CLI 完成真实上传、成功结果、重复结果和移动端布局检查。
- [ ] 保存桌面与移动端截图到已忽略的 `output/playwright/`。
- [ ] 重新执行后端相关测试、前端类型检查和生产构建。
- [ ] 同步实际能力、技术边界、Shoka 参考判断和未持久化风险。
- [ ] 审查仅包含本轮文件后提交 `feat: 搭建知识工坊前端`。
