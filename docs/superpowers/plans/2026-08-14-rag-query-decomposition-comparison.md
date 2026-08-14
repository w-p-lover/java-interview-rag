# RAG 查询拆分对照 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变生产检索语义的前提下，为固定 20 题评估增加原查询与确定性子句拆分策略的同口径对照。

**Architecture:** 新增无模型查询规划器，将多句、多子句和带“分别”协调结构的中文问题拆成最多四个查询；评估服务复用原查询结果，并用 Reciprocal Rank Fusion 合并额外查询结果。独立对照接口同时返回基线、候选报告和逐题查询计划，现有评估接口保持兼容。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI VectorStore、JUnit 5、Mockito、Python 3 标准库。

## Global Constraints

- 所有写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅可只读参考。
- 新增或修改的代码注释、日志、异常和用户可见提示默认使用简体中文。
- 不改变生产检索、问答、阈值和排序语义，不新增模型、中间件或付费调用。
- 只执行直接相关测试和脚本结构校验。

---

### Task 1: 确定性查询规划与结果融合

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagEvaluationQueryPlanner.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagEvaluationResultFusion.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/evaluation/RagEvaluationQueryPlannerTest.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/evaluation/RagEvaluationResultFusionTest.java`

**Interfaces:**
- Produces: `List<String> RagEvaluationQueryPlanner.plan(String question)`，首项始终为原问题，最多四项且稳定去重。
- Produces: `List<MarkdownSearchResult> RagEvaluationResultFusion.fuse(List<List<MarkdownSearchResult>> rankedResults, int topK)`，按来源、版本、chunk 顺序去重并使用 RRF 排序。

- [ ] **Step 1: 写规划器失败测试**

覆盖单句不拆分、Map 多句拆分、带“分别”的协调结构拆分、空问题拒绝和最多四个查询。

- [ ] **Step 2: 运行规划器测试确认失败**

Run: `mvn -Dmaven.repo.local=.m2 -Dtest=RagEvaluationQueryPlannerTest test`
Expected: FAIL，因为规划器尚不存在。

- [ ] **Step 3: 实现最小查询规划器**

使用中文问号、句号、分号、逗号和带“分别/各自/两者”的“与/和/以及”结构做确定性拆分；过滤过短片段并保留插入顺序，不调用 Chat。

- [ ] **Step 4: 写融合失败测试并实现 RRF**

验证原首条在同分时稳定优先、跨查询重复 chunk 只保留一次、跨章节候选可以进入 Top5、返回数量不超过 `topK`。

- [ ] **Step 5: 运行两个直接测试**

Run: `mvn -Dmaven.repo.local=.m2 -Dtest=RagEvaluationQueryPlannerTest,RagEvaluationResultFusionTest test`
Expected: PASS。

### Task 2: 评估对照接口

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationComparisonReport.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationService.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/RagRetrievalEvaluationController.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/RagRetrievalEvaluationConfiguration.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationServiceTest.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/web/RagRetrievalEvaluationControllerTest.java`

**Interfaces:**
- Produces: `RagRetrievalEvaluationComparisonReport compareQueryDecomposition()`。
- Produces: `GET /api/knowledge/evaluations/retrieval/query-decomposition`。

- [ ] **Step 1: 写服务对照失败测试**

Mock 原查询和额外子查询结果，断言基线缺失第二来源、候选覆盖第二来源、原查询不重复执行、查询计划与额外查询数准确。

- [ ] **Step 2: 实现对照报告和服务编排**

一次执行每题原查询，将结果同时用于基线和候选；候选只执行新增子查询并融合。指标继续复用现有来源、Hit@1、Recall@5、MRR、标题路径和拒答计算。

- [ ] **Step 3: 接入独立 GET 路径并补 HTTP 契约测试**

现有根路径仍返回原报告；新路径返回策略名、拆分题数、额外查询数、逐题查询和两份同结构报告。

- [ ] **Step 4: 运行服务与控制器测试**

Run: `mvn -Dmaven.repo.local=.m2 -Dtest=RagRetrievalEvaluationServiceTest,RagRetrievalEvaluationControllerTest test`
Expected: PASS。

### Task 3: 运行器与事实文档

**Files:**
- Modify: `evaluation/run_retrieval_baseline.py`
- Modify: `evaluation/README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Produces: `--compare-query-decomposition`，显式选择对照接口并分别校验基线与候选报告。

- [ ] **Step 1: 扩展 Python 运行器**

保留默认基线路径；增加显式对照参数、嵌套报告结构校验和两组指标摘要。输出文件继续只在显式传入 `--output` 时写入。

- [ ] **Step 2: 更新评估说明和项目事实文档**

记录候选策略、生产未接入、真实 Embedding 对照仍需付费授权，以及参考项目未提供可复用多证据检索实现的判断。

- [ ] **Step 3: 执行最小完整验证**

Run: `python -m py_compile evaluation/run_retrieval_baseline.py`

Run: `mvn -Dmaven.repo.local=.m2 -Dtest=RagEvaluationQueryPlannerTest,RagEvaluationResultFusionTest,RagRetrievalEvaluationServiceTest,RagRetrievalEvaluationControllerTest test`

Expected: Python 编译成功，四个直接相关测试类全部通过；不调用 Embedding、Chat、Qdrant 或 MySQL。

- [ ] **Step 4: 检查并提交直接相关文件**

Run: `git diff --check`

只暂存本计划列出的文件，检查 staged diff 后使用 `feat: 增加RAG查询拆分对照` 提交。
