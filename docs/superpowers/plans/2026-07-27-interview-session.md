# 可持久化文本面试会话实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 保存确认画像与 3 道首轮题目，并支持恢复会话和按顺序提交回答直至完成。

**Architecture:** 使用 `InterviewSession` 单行聚合维护顺序回答状态，通过 `InterviewSessionStore` 隔离领域服务与 MySQL。MySQL 使用 JSON 文本快照保存画像、题目和回答，独立状态列与乐观锁版本保护并发提交；HTTP 层只映射创建、查询和回答接口。

**Tech Stack:** Java 17、Spring Boot 3.5.16、Spring MVC、MyBatis-Plus、MySQL 5.7/8、Jackson、JUnit 5、Mockito

## Global Constraints

- 所有项目写入仅发生在 `D:\IdeaProjects\java-interview-rag`，`blog-springboot` 保持只读。
- 新增或修改的代码注释默认使用简体中文；直观代码不增加无意义注释。
- 不新增 Maven 依赖，不调用 Chat、Embedding 或 Qdrant，不执行远程数据库迁移。
- 不实现评分、动态追问、改答案、跳题、删除会话、前端或用户身份。

---

### Task 1: 会话领域状态与服务

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/session/InterviewSessionStatus.java`
- Create: `src/main/java/com/javainterview/rag/interview/session/InterviewSessionAnswer.java`
- Create: `src/main/java/com/javainterview/rag/interview/session/InterviewSession.java`
- Create: `src/main/java/com/javainterview/rag/interview/session/InterviewSessionStore.java`
- Create: `src/main/java/com/javainterview/rag/interview/session/InterviewSessionService.java`
- Test: `src/test/java/com/javainterview/rag/interview/session/InterviewSessionServiceTest.java`

**Interfaces:**
- Consumes: `InterviewPreparationProfile` 和 `List<InterviewQuestion>`。
- Produces: `InterviewSessionService.start(String, InterviewPreparationProfile, List<InterviewQuestion>)`、`find(String)` 和 `submitAnswer(String, String)`。

- [x] **Step 1: 编写失败测试**

测试创建状态为 `IN_PROGRESS`、当前题号 0、版本 0；依次回答后题号和版本递增；第三题后状态为 `COMPLETED`；完成后拒绝回答；Store 更新返回 false 时抛出并发冲突。

- [x] **Step 2: 运行测试确认 RED**

Run: `cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=InterviewSessionServiceTest test"`

Expected: 因会话领域类型不存在而编译失败。

- [x] **Step 3: 实现最小领域模型与服务**

领域模型固定 3 道题并校验回答序号、当前题号、状态和版本一致性。服务生成 UUID 与时间戳，通过 Store 创建和读取会话；回答最多 10000 字符并使用旧版本条件更新。

- [x] **Step 4: 运行目标测试**

Run: 与 Step 2 相同。

Expected: `InterviewSessionServiceTest` 全部通过，0 失败、0 错误。

### Task 2: MySQL 会话聚合存储

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/persistence/InterviewSessionEntity.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/InterviewSessionMapper.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/MySqlInterviewSessionStore.java`
- Create: `deploy/mysql/init/004_interview_session.sql`
- Test: `src/test/java/com/javainterview/rag/interview/persistence/MySqlInterviewSessionStoreTest.java`

**Interfaces:**
- Consumes: `InterviewSessionStore`、Spring Boot `ObjectMapper` 和 MyBatis-Plus `BaseMapper`。
- Produces: JSON 快照插入、按主键读取，以及 `WHERE id = ? AND version = ? AND status = 'IN_PROGRESS'` 的乐观锁更新。

- [x] **Step 1: 编写 Store 失败测试**

测试插入实体包含画像、题目和空回答 JSON；查询实体可恢复完整领域会话；更新传递旧版本并返回实际影响行数语义。

- [x] **Step 2: 实现 Entity、Mapper、Store 与迁移 SQL**

使用 `MEDIUMTEXT` 保存三个 JSON 快照；表主键为 UUID 字符串，状态与更新时间建立联合索引。JSON 损坏时抛出中文 `IllegalStateException`，不返回部分会话。

- [x] **Step 3: 运行领域与持久化测试**

Run: `cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=InterviewSessionServiceTest,MySqlInterviewSessionStoreTest test"`

Expected: 会话服务与 MySQL Store 测试全部通过。

### Task 3: storage HTTP 接口

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionCreateRequest.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewAnswerSubmissionRequest.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionController.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionConfiguration.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewSessionControllerTest.java`

**Interfaces:**
- Consumes: `InterviewSessionService`、`InterviewSessionMapper` 和 Spring Boot `ObjectMapper`。
- Produces: `POST /api/interviews/sessions`、`GET /api/interviews/sessions/{sessionId}`、`POST /api/interviews/sessions/{sessionId}/answers`。

- [x] **Step 1: 编写控制器契约测试**

测试创建、查询和提交回答响应；测试非法输入 400、不存在 404、已完成或并发冲突 409。

- [x] **Step 2: 实现薄控制器与 storage 配置**

配置创建 MySQL Store 与领域服务；控制器只映射请求和三类异常，不调用模型或检索服务。

- [x] **Step 3: 运行直接回归**

Run: `cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=InterviewSessionServiceTest,MySqlInterviewSessionStoreTest,InterviewSessionControllerTest,InterviewQuestionGenerationServiceTest,InterviewPreparationServiceTest,JavaInterviewRagApplicationTests test"`

Expected: 所列测试全部通过，默认 `local` 上下文无数据库依赖。

### Task 4: 文档、检查与提交

**Files:**
- Modify: `README.md`
- Modify: `docs/01-需求边界.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 已通过测试的会话领域、MySQL Store、SQL 与 HTTP 契约。
- Produces: 阶段 6 最新事实、ADR-029 和单一功能提交。

- [x] **Step 1: 同步项目事实**

记录单行聚合、顺序回答、完成状态、乐观锁、迁移未执行以及尚未实现评分和追问的边界。

- [x] **Step 2: 检查差异**

Run: `git diff --check`，暂存后运行 `git diff --cached --check`。

Expected: 无空白错误，暂存区只包含本轮直接相关文件。

- [x] **Step 3: 提交闭环**

Commit: `feat: 建立可持久化面试会话`
