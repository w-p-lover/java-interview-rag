# Spring MVC 面试学习闭环 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 InterviewGuide 的 Spring MVC 高频概念独立转化为可阅读、可检索、可组卷、可作答并可按弱项再练的本地学习闭环。

**Architecture:** 复用已有 Markdown 导入、Hybrid 检索、知识题库、专项面试、评分和复习服务，不新增平行内容平台或业务接口。课程使用现有 SPRING 三维 Rubric 组织三道主问题，真实运行数据只写入独立开发库和现有 Qdrant/Lucene 开发索引。

**Tech Stack:** Markdown、Spring Boot 3 / Spring MVC、现有 REST API、MySQL 5.7、Qdrant、Lucene、DashScope Chat/Embedding

**Spec:** `.codex/autonomous-development-prompt.md`、`docs/09-后续开发路线.md`

## Global Constraints

- 所有项目文件写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- `Snailclimb/interview-guide@969e2af8550f680688f5b50f0d895186a661a319` 为 AGPL-3.0，仅学习概念和组织方式，课程必须独立重写。
- 复用现有知识库、题库、面试和复习链路，不增加 Java/Vue 功能或新表。
- Chat 最多 4 次，Embedding 仅用于一次导入和一次固定检索；任一失败不重试。
- 只运行能证明本轮学习路径的 API、数据库和 Git 差异检查。

## Review Focus

- 课程必须区分 Filter、Interceptor、ArgumentResolver 和 `@ControllerAdvice` 的执行层次与适用边界。
- 校验必须同时说明 `MethodArgumentNotValidException` 与 `HandlerMethodValidationException`，避免版本化绝对结论。
- HTTP 状态码必须由业务失败语义决定，不能把所有异常压成 200 或 500。
- 本项目案例必须来自真实类和接口，且明确当前局部异常处理的实际边界。
- 再练必须使用评分生成的真实复习任务与保存的 Rubric 版本，不手工伪造弱项结果。

---

### Task 1: 独立重写 Spring MVC 课程

**Files:**
- Create: `learning-packs/interview-guide/spring/mvc-request-validation-errors.md`

**Interfaces:**
- Consumes: InterviewGuide Spring MVC 主题、Spring Framework 官方 MVC/校验/异常文档、本项目 `BusinessRequestMetricsFilter`、`MarkdownDocumentController`、`QuestionBankController` 和 `KnowledgeRetrievalExceptionHandler`
- Produces: source `interview-guide/spring/mvc-request-validation-errors.md`，以及三个与 SPRING 训练维度一一对应的主问题

- [x] **Step 1: 写课程元数据与请求调用链**

  记录仓库、commit、AGPL-3.0、官方参考、独立重写方式和用途；说明 Servlet Filter 到 `DispatcherServlet`、`HandlerMapping`、`HandlerAdapter`、参数解析、Controller、返回值处理的调用顺序。

- [x] **Step 2: 写校验、异常与扩展点边界**

  区分反序列化、类型转换、对象校验、方法校验以及两类校验异常；按认证/跨域/观测、业务前后处理、参数构造和统一错误映射选择 Filter、Interceptor、ArgumentResolver 与 Advice。

- [x] **Step 3: 写本项目映射和三道训练题**

  用真实类说明 requestId/指标、局部异常映射、检索超时/不可用 Advice 和题库冲突状态；三题分类固定为 `框架机制与实现`、`事务与故障边界`、`调用链表达`。

### Task 2: 接入真实学习与训练链路

**Files:**
- Modify: development database and existing Qdrant/Lucene development indexes through current APIs

**Interfaces:**
- Consumes: Task 1 Markdown course
- Produces: current document version, three ACTIVE SPRING questions, one completed interview session, generated review tasks, one review attempt

- [x] **Step 1: 导入课程并核对三存储**

  调用 `/api/knowledge/documents/import`，随后调用资料详情与一致性接口，要求 MySQL `READY`、Dense/Sparse 当前版本 Chunk 数一致且没有旧版本残留。

- [x] **Step 2: 固定检索一次**

  查询“Spring MVC 中 Filter、Interceptor、参数校验和 ControllerAdvice 分别处在哪一层，异常如何变成 HTTP 状态码？”，要求目标 source 进入 Top 3、检索不降级；不为提升名次重复请求。

- [x] **Step 3: 建立并审核三道人工题**

  通过 `/api/interviews/question-bank` 创建题目并切换为 ACTIVE；每题保留参考回答、检查点、Rubric、一个固定追问和真实课程证据。

- [ ] **Step 4: 完成作答与再练**

  从三题创建 SPRING 专项会话，最多执行三次主问题评分；选择评分生成的复习任务提交一次改进回答。要求报告可读取、复习 attempt 保存 `rubricVersion` 与弱项改善评价；调用失败即停止且不重试。

  运行结果：三题会话已创建；首次评分在 `60,886 ms` 因 Chat 读超时失败，会话未写入回答。按预定失败处置停止后续评分与再练；已修复 Chat/Embedding 超时复用和自动重试，但本轮不再次付费调用。

### Task 3: 同步事实并提交

**Files:**
- Modify: `docs/04-当前进度.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `C:\Users\IT074\.codex\automations\java-interview-rag\memory.md`（完成后由自动化规则更新，不纳入 Git）

**Interfaces:**
- Consumes: Task 2 的实际版本、Chunk、检索、题目、会话、评分和复习结果
- Produces: 当前学习入口、上游到本地映射、限制与下一项的事实记录

- [x] **Step 1: 更新当前进度和上游映射**

  只记录实际完成结果、调用次数、失败边界与用户入口；不把单主题验收外推为全部 Spring MVC 质量。

- [x] **Step 2: 检查并提交**

  运行 `git diff --check`，只暂存本轮课程、计划和两份权威文档，以 `feat: 接入 Spring MVC 面试学习包` 提交。
