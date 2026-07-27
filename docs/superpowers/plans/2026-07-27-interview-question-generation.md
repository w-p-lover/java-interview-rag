# 首轮面试题生成与知识来源绑定实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于已确认的面试准备画像生成 3 道可追溯面试题，并为每题绑定可靠的真实知识库来源。

**Architecture:** 在独立 `interview.question` 包中建立模型草稿、最终题目和生成服务。服务复用 `ChatClient` 生成结构化草稿，再复用 `MarkdownSimilaritySearchService` 以固定参数逐题检索；HTTP 层只负责映射请求和参数错误。

**Tech Stack:** Java 17、Spring Boot 3.5.16、Spring MVC、Spring AI ChatClient、Spring AI VectorStore、JUnit 5、Mockito

## Global Constraints

- 所有项目写入仅发生在 `D:\IdeaProjects\java-interview-rag`，`blog-springboot` 保持只读。
- 新增或修改的代码注释默认使用简体中文；直观代码不增加无意义注释。
- 不新增依赖、数据库表、前端页面、面试会话、参考答案、评分或真实模型调用。
- 固定 RAG 评估集不得作为用户侧面试题来源。

---

### Task 1: 面试题生成与知识来源编排

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/question/InterviewQuestionDraft.java`
- Create: `src/main/java/com/javainterview/rag/interview/question/InterviewQuestionDraftSet.java`
- Create: `src/main/java/com/javainterview/rag/interview/question/InterviewQuestion.java`
- Create: `src/main/java/com/javainterview/rag/interview/question/InterviewQuestionGenerationResult.java`
- Create: `src/main/java/com/javainterview/rag/interview/question/InterviewQuestionGenerationService.java`
- Test: `src/test/java/com/javainterview/rag/interview/question/InterviewQuestionGenerationServiceTest.java`

**Interfaces:**
- Consumes: `InterviewPreparationProfile`、`ChatClient.prompt(String).call().entity(InterviewQuestionDraftSet.class)`、`MarkdownSimilaritySearchService.search(String, int, double)`。
- Produces: `InterviewQuestionGenerationService.generate(String targetRole, InterviewPreparationProfile profile)`，固定返回 Prompt 版本 `interview-question-v1` 和 3 道 `InterviewQuestion`。

- [x] **Step 1: 编写失败测试**

测试 Spring AI 结构化转换、目标岗位与画像进入 Prompt、模型必须生成 3 道题、每个查询使用 `topK=3` 和阈值 `0.60`、真实检索结果进入最终题目、无命中时返回空知识来源，以及空目标岗位在模型调用前失败。

- [x] **Step 2: 运行测试确认 RED**

Run: `cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=InterviewQuestionGenerationServiceTest test"`

Expected: 因 `interview.question` 领域类型尚不存在而编译失败。

- [x] **Step 3: 实现最小领域模型与服务**

模型记录对文本和列表执行非空校验与不可变复制；草稿集合要求恰好 3 道题。服务校验目标岗位和画像，Prompt 限制事实与输出边界，检查草稿依据类型不超出画像范围，并为每题执行一次固定参数检索。

- [x] **Step 4: 运行目标测试**

Run: 与 Step 2 相同。

Expected: `InterviewQuestionGenerationServiceTest` 全部通过，0 失败、0 错误。

### Task 2: storage HTTP 接口

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewQuestionGenerationRequest.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewQuestionController.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewQuestionConfiguration.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewQuestionControllerTest.java`

**Interfaces:**
- Consumes: `InterviewQuestionGenerationService.generate(String, InterviewPreparationProfile)`。
- Produces: `POST /api/interviews/questions`；非法参数返回 `{"message":"..."}` 和 HTTP 400。

- [x] **Step 1: 编写控制器契约测试**

测试目标岗位和画像字段传递、Prompt 版本、画像依据类型、知识命中状态、真实来源 JSON，以及空目标岗位错误响应。

- [x] **Step 2: 实现薄控制器与 storage 配置**

配置复用现有 `ragAnswerChatClient` 和 `MarkdownSimilaritySearchService`；控制器仅映射请求并处理 `IllegalArgumentException`。

- [x] **Step 3: 运行直接回归**

Run: `cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=InterviewQuestionGenerationServiceTest,InterviewQuestionControllerTest,InterviewPreparationServiceTest,InterviewPreparationControllerTest,JavaInterviewRagApplicationTests test"`

Expected: 所列测试全部通过，默认 `local` 上下文不依赖 Chat 或向量服务。

### Task 3: 文档、检查与提交

**Files:**
- Modify: `README.md`
- Modify: `docs/01-需求边界.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 已通过测试的真实题目生成与知识来源契约。
- Produces: 阶段 6 最新事实、ADR-028 和可回溯 Git 提交。

- [x] **Step 1: 同步项目事实**

记录画像驱动出题、依据类型、真实知识来源、空命中语义、固定 3 题以及未实现的会话/答案/评分边界。

- [x] **Step 2: 执行差异检查**

Run: `git diff --check`，随后检查 `git diff --stat` 和目标文件内容。

Expected: 无空白错误，只包含本轮直接相关改动。

- [x] **Step 3: 提交闭环**

Commit: `feat: 生成可追溯首轮面试题`
