# 完整文本模拟面试闭环实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 一次完成可配置出题、一键开始、逐题评分、动态追问、可靠提交、最终报告和 Vue 操作页面。

**Architecture:** 扩展现有画像、出题和会话聚合，不创建平行面试体系。回答评价由独立服务调用 Chat 并绑定可选追问来源，会话领域负责题目队列、上限、状态和确定性报告；Vue 使用独立路由与 composable 串联真实 HTTP API。

**Tech Stack:** Java 17、Spring Boot 3.5.16、Spring AI、MyBatis-Plus、MySQL 5.7/8、Vue 3.5、Vue Router、TypeScript、Axios、Sass、Vite 8、JUnit 5、Mockito

## Global Constraints

- 所有项目文件写入仅发生在 `D:\IdeaProjects\java-interview-rag`，`blog-springboot` 保持只读。
- 新增或修改的代码注释默认使用简体中文，直观代码不增加无意义注释。
- 不新增 Maven/npm 依赖，不修改固定 RAG 评估集，不实现语音视频、登录、多用户或 Tool Calling。
- 前端使用 Vue Composition API、`<script setup lang="ts">`、props down/events up 和现有设计令牌。
- 自动测试不调用真实模型；真实验证必须限制在项目独立数据库和 collection。

---

### Task 1: 可配置核心题目

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/question/InterviewQuestionDraftSet.java`
- Modify: `src/main/java/com/javainterview/rag/interview/question/InterviewQuestionGenerationResult.java`
- Modify: `src/main/java/com/javainterview/rag/interview/question/InterviewQuestionGenerationService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewQuestionGenerationRequest.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewQuestionController.java`
- Modify: `src/test/java/com/javainterview/rag/interview/question/InterviewQuestionGenerationServiceTest.java`
- Modify: `src/test/java/com/javainterview/rag/interview/web/InterviewQuestionControllerTest.java`

**Interfaces:**
- Produces: `generate(String, InterviewPreparationProfile, int questionCount)`，题数 3 到 8；两参数重载保留默认 5 题。

- [ ] 编写题数 5、非法题数和模型返回数量不一致的失败测试。
- [ ] 运行目标测试确认现有固定 3 题实现失败。
- [ ] 放宽草稿/结果模型并让 Prompt 使用请求题数，更新独立出题接口。
- [ ] 运行出题服务与控制器测试至通过。

### Task 2: 逐题评价、动态追问和报告领域

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationDraft.java`
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluation.java`
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerAssessment.java`
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluator.java`
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationService.java`
- Create: `src/main/java/com/javainterview/rag/interview/session/InterviewSessionReport.java`
- Modify: `src/main/java/com/javainterview/rag/interview/session/InterviewSessionAnswer.java`
- Modify: `src/main/java/com/javainterview/rag/interview/session/InterviewSession.java`
- Modify: `src/main/java/com/javainterview/rag/interview/session/InterviewSessionService.java`
- Test: `src/test/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationServiceTest.java`
- Modify: `src/test/java/com/javainterview/rag/interview/session/InterviewSessionServiceTest.java`

**Interfaces:**
- Produces: `InterviewAnswerEvaluator.evaluate(profile, question, answer)`；`submitAnswer(sessionId, questionIndex, expectedVersion, answer)`。

- [ ] 编写结构化评分、无知识来源、追问来源绑定和 Prompt 注入边界失败测试。
- [ ] 编写题号/版本过期、追问追加、上限、完成报告失败测试。
- [ ] 实现评分记录、模型服务、题目队列状态和确定性汇总报告。
- [ ] 运行评价与会话服务测试至通过。

### Task 3: 一键开始面试和存储升级

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/session/InterviewSessionStartService.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionStartRequest.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionCreateRequest.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewAnswerSubmissionRequest.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionController.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionConfiguration.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewSessionEntity.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewSessionMapper.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/MySqlInterviewSessionStore.java`
- Create: `deploy/mysql/init/005_expand_interview_session.sql`
- Modify: `src/test/java/com/javainterview/rag/interview/persistence/MySqlInterviewSessionStoreTest.java`
- Modify: `src/test/java/com/javainterview/rag/interview/web/InterviewSessionControllerTest.java`

**Interfaces:**
- Produces: `POST /api/interviews/sessions/start`；持久化 `maxQuestionCount`、动态题目、逐题评价与 `report`。

- [ ] 编写一键编排、请求题号/版本、JSON 报告和新增列映射失败测试。
- [ ] 实现 StartService、控制器契约、配置 Bean、Entity/Mapper/Store 和 005 迁移。
- [ ] 运行全部 interview 包直接测试与默认 local 上下文。

### Task 4: Vue API、状态编排与路由

**Files:**
- Create: `frontend/src/api/interview.ts`
- Create: `frontend/src/composables/useInterviewSession.ts`
- Create: `frontend/src/views/InterviewView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/api/request.ts`

**Interfaces:**
- Produces: `startInterview`、`getInterviewSession`、`submitInterviewAnswer`；路由 `/interview`；localStorage 恢复。

- [ ] 定义与后端一致的 TypeScript 类型和 API 函数。
- [ ] 实现单一状态源 composable，派生当前题目、进度、最后评价和完成报告。
- [ ] 增加懒加载路由并保持资料工作台 `/` 不变。
- [ ] 运行 `npm run typecheck` 修正类型问题。

### Task 5: Vue 面试操作界面

**Files:**
- Create: `frontend/src/components/interview/InterviewSetupForm.vue`
- Create: `frontend/src/components/interview/InterviewQuestionPanel.vue`
- Create: `frontend/src/components/interview/InterviewFeedbackPanel.vue`
- Create: `frontend/src/components/interview/InterviewReportPanel.vue`
- Modify: `frontend/src/views/InterviewView.vue`
- Modify: `frontend/src/components/WorkspaceHeader.vue`
- Modify: `frontend/src/styles/index.scss`

**Interfaces:**
- Consumes: `useInterviewSession` 状态与动作。
- Produces: 启动、答题、查看反馈、继续、报告和重新开始的完整页面。

- [ ] 实现四个单一职责组件，显式 props/emits，不在子组件直接调用 API。
- [ ] 用现有 Workbench/Coral 令牌完成桌面双栏和移动单栏布局。
- [ ] 页头增加资料库与模拟面试路由导航，命令面板继续只服务资料页。
- [ ] 执行 `npm run build`。

### Task 6: 验证、文档和提交

**Files:**
- Modify: `README.md`
- Modify: `docs/01-需求边界.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Produces: 完整阶段 6 MVP 事实、ADR-030、验证记录和 Git 提交。

- [ ] 执行后端直接测试、前端构建和迁移结构检查。
- [ ] 在可用独立 storage 环境执行受控开始/回答/恢复/完成验证；若外部模型或迁移不可用，如实记录边界。
- [ ] 使用浏览器验证真实或拦截 API 的桌面/移动端流程、控制台和横向溢出。
- [ ] 更新项目文档，运行 `git diff --check` 与暂存区检查。
- [ ] 提交 `feat: 完成文本模拟面试闭环`。
