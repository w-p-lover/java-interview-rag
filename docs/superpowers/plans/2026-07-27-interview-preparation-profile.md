# 面试准备画像实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过一次结构化模型调用把目标岗位、简历和可选岗位描述转换为证据化面试准备画像。

**Architecture:** 在新的 `interview` 领域下建立画像模型与服务，复用现有 `storage` profile 的 `ChatClient`，不耦合内部固定 RAG 评估集。HTTP 层只负责请求映射和参数错误转换，业务校验与 Prompt 组装留在服务中。

**Tech Stack:** Java 17、Spring Boot 3.5.16、Spring MVC、Spring AI ChatClient、JUnit 5、Mockito

## Global Constraints

- 所有项目写入仅发生在 `D:\IdeaProjects\java-interview-rag`，`blog-springboot` 保持只读。
- 新增或修改的代码注释默认使用简体中文；直观代码不增加无意义注释。
- 不新增依赖、数据库表、前端页面、RAG 检索或真实模型调用。
- 固定 RAG 评估集不得作为用户侧面试题库或画像输入。

---

### Task 1: 面试准备画像领域服务

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/preparation/InterviewFocusSource.java`
- Create: `src/main/java/com/javainterview/rag/interview/preparation/InterviewFocusArea.java`
- Create: `src/main/java/com/javainterview/rag/interview/preparation/InterviewPreparationProfile.java`
- Create: `src/main/java/com/javainterview/rag/interview/preparation/InterviewPreparationResult.java`
- Create: `src/main/java/com/javainterview/rag/interview/preparation/InterviewPreparationService.java`
- Test: `src/test/java/com/javainterview/rag/interview/preparation/InterviewPreparationServiceTest.java`

**Interfaces:**
- Consumes: `ChatClient.prompt(String).call().entity(InterviewPreparationProfile.class)`。
- Produces: `InterviewPreparationService.prepare(String targetRole, String resumeText, String jobDescription)`。

- [x] **Step 1: 编写失败测试**

测试结构化画像返回、目标岗位和 Prompt 版本固定、Prompt 包含三类输入和资料指令边界；测试空目标岗位或空简历在调用模型前失败。

- [x] **Step 2: 运行测试确认 RED**

Run: `mvnw.cmd -Dtest=InterviewPreparationServiceTest test`

Expected: 因画像领域类型尚不存在而编译失败。

- [x] **Step 3: 实现最小模型与服务**

领域记录对文本、列表和关注领域执行不可变复制与必要校验；服务限制目标岗位 100 字符、简历 50000 字符、岗位描述 30000 字符，并以 `interview-preparation-v1` 记录 Prompt 版本。

- [x] **Step 4: 运行目标测试**

Run: `mvnw.cmd -Dtest=InterviewPreparationServiceTest test`

Expected: 目标测试通过，0 失败、0 错误。

### Task 2: storage HTTP 接口

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewPreparationRequest.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewPreparationController.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewPreparationConfiguration.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewPreparationControllerTest.java`

**Interfaces:**
- Consumes: `InterviewPreparationService.prepare(...)`。
- Produces: `POST /api/interviews/preparations`，返回 `InterviewPreparationResult`；非法参数返回 `{"message":"..."}` 和 HTTP 400。

- [x] **Step 1: 编写控制器契约测试**

测试请求字段传递、结构化 JSON 输出和空简历错误响应。

- [x] **Step 2: 实现配置与薄控制器**

配置仅在 `storage` profile 装配服务并复用现有 `ragAnswerChatClient`；控制器只映射请求与参数异常。

- [x] **Step 3: 运行直接回归**

Run: `mvnw.cmd -Dtest=InterviewPreparationServiceTest,InterviewPreparationControllerTest,RagAnswerServiceTest,JavaInterviewRagApplicationTests test`

Expected: 全部测试通过，默认 `local` 上下文不依赖 Chat Key。

### Task 3: 文档、审查与提交

**Files:**
- Modify: `README.md`
- Modify: `docs/01-需求边界.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 已通过测试的真实接口契约和 Git diff。
- Produces: 阶段 6 首个面试准备闭环事实与 ADR-027。

- [x] **Step 1: 同步项目事实**

明确固定评估集只服务系统质量，用户侧画像由简历、目标岗位和可选 JD 生成；记录阶段 5 暂不引入无数据支撑的检索增强。

- [x] **Step 2: 检查差异**

Run: `git diff --cached --check`

Expected: 无空白错误，暂存区只包含本轮直接相关文件。

- [x] **Step 3: 提交闭环**

Commit: `feat: 建立面试准备画像`
