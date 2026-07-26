# RAG 回答要点对比实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于固定题集批量生成 RAG 回答，并以可解释的标准化字面匹配输出回答要点覆盖报告。

**Architecture:** 新增独立回答评估服务，复用 `FixedRagEvaluationDataset` 和 `RagAnswerService`，不改变在线回答链路。新增 `storage` profile 下的薄配置与 `POST` 控制器，报告只返回来源元数据和要点匹配结果。

**Tech Stack:** Java 17、Spring Boot 3.5.16、Spring MVC、Spring AI、JUnit 5、Mockito

## Global Constraints

- 所有实现写入 `D:\IdeaProjects\java-interview-rag`，`blog-springboot` 保持只读。
- 新增或修改的代码注释默认使用简体中文；没有不明显边界时不添加注释。
- 不增加依赖、数据库结构、前端功能或额外模型裁判。
- 只执行直接相关测试，不主动产生真实 Chat 调用费用。

---

### Task 1: 回答评估领域服务

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagAnswerEvaluationCaseResult.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagAnswerEvaluationReport.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagAnswerEvaluationService.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/evaluation/RagAnswerEvaluationServiceTest.java`

**Interfaces:**
- Consumes: `FixedRagEvaluationDataset.findAll()`、`RagAnswerService.answer(String)`。
- Produces: `RagAnswerEvaluationService.evaluate()` 返回 `RagAnswerEvaluationReport`。

- [x] **Step 1: 编写失败测试**

测试两道题，断言标准化后完整要点匹配、未命中要点、来源命中、可靠回答数和总体覆盖率。

- [x] **Step 2: 运行测试确认 RED**

Run: `mvnw.cmd -Dtest=RagAnswerEvaluationServiceTest test`

Expected: 因回答评估类型尚不存在而编译失败。

- [x] **Step 3: 实现最小领域模型与服务**

逐题调用现有回答服务；只保留字母数字并转小写后执行完整要点包含判断；汇总命中指标并返回不可变列表。

- [x] **Step 4: 运行测试确认 GREEN**

Run: `mvnw.cmd -Dtest=RagAnswerEvaluationServiceTest test`

Expected: 目标测试通过。

### Task 2: storage HTTP 入口

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/web/RagAnswerEvaluationConfiguration.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/web/RagAnswerEvaluationController.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/web/RagAnswerEvaluationControllerTest.java`

**Interfaces:**
- Consumes: `RagAnswerEvaluationService.evaluate()`。
- Produces: `POST /api/knowledge/evaluations/answers`。

- [x] **Step 1: 编写失败的控制器测试**

断言 `POST` 返回匹配方法、汇总指标与单题命中/未命中要点。

- [x] **Step 2: 实现配置和薄控制器**

仅在 `storage` profile 装配服务与路由，不增加请求参数。

- [x] **Step 3: 运行直接相关测试**

Run: `mvnw.cmd -Dtest=RagAnswerEvaluationServiceTest,RagAnswerEvaluationControllerTest,RagRetrievalEvaluationServiceTest,RagAnswerServiceTest test`

Expected: 全部测试通过，0 失败、0 错误。

### Task 3: 文档、检查与提交

**Files:**
- Modify: `README.md`
- Modify: `evaluation/README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 已通过测试的接口与实际 diff。
- Produces: 与代码一致的阶段 4 状态和 ADR-026。

- [x] **Step 1: 同步事实文档**

记录标准化字面匹配方法、`POST` 费用边界、已验证内容和未执行的真实批量调用。

- [x] **Step 2: 检查差异**

Run: `git diff --check` 和 `git status --short`

Expected: 无空白错误，只包含本轮直接相关文件。

- [x] **Step 3: 提交闭环**

Commit: `feat: 增加 RAG 回答要点评估`
