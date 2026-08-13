# RAG Hard Evaluation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前 6 道“来源是否进入 Top5”的窄评估，扩展为 20 道覆盖精确术语、相似来源、标题路径、歧义、无答案和跨章节的困难检索评估，并输出可用于决定检索增强方向的排序与拒答指标。

**Architecture:** 保留现有 `GET /api/knowledge/evaluations/retrieval` 与 `topK=5`、阈值 `0.0` 契约，在固定题中增加题型与期望标题路径。评估服务从一次检索结果确定性计算 Hit@1、Recall@5、MRR、标题路径召回，以及按生产回答阈值 `0.60` 判断的无答案拒答；不调用 Chat、不自动执行真实 Embedding 基线。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring AI 1.0、Qdrant、JUnit 5、Mockito、Python 标准库运行器。

## Global Constraints

- 所有写入只发生在 `D:\IdeaProjects\java-interview-rag`，`D:\IdeaProjects\blog\blog-springboot` 只读。
- 不覆盖或提交当前工作区已有的前端、Docker、治理文件和本机配置改动。
- 新增或修改的代码注释、日志、异常和用户提示默认使用简体中文。
- 不调用 Chat、Embedding、Qdrant 或 MySQL；真实基线需要另行明确费用授权。
- 不实现混合检索、rerank、查询改写或图工作流，只建立选择这些方案所需的评估证据。

---

### Task 1: 困难题模型与固定数据集

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagEvaluationCaseType.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagEvaluationCase.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/evaluation/FixedRagEvaluationDataset.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/evaluation/FixedRagEvaluationDatasetTest.java`

**Interfaces:**
- Produces: `RagEvaluationCase(type, expectedSources, expectedHeadingPaths, expectedAnswerPoints)`，其中 `NO_ANSWER` 允许空期望来源，其他类型要求至少一个来源。

- [x] **Step 1: 先写失败测试**

断言固定集为 20 条、ID 唯一、六类题型均存在、无答案题来源为空，其他题均有来源和标题路径。

- [x] **Step 2: 实现最小数据模型和 20 条题**

题型固定为 `EXACT_TERM`、`SIMILAR_SECTION`、`HEADING_PATH`、`AMBIGUOUS`、`NO_ANSWER`、`CROSS_SECTION`。不增加动态题库、数据库表或配置项。

- [x] **Step 3: 运行数据集测试**

```shell
./mvnw.cmd -Dtest=FixedRagEvaluationDatasetTest test
```

Expected: 20 条题满足结构约束，六类题型完整。

### Task 2: 排序、标题与拒答指标

**Files:**
- Modify: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationCaseResult.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationReport.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationService.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/evaluation/RagAnswerEvaluationService.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/web/RagRetrievalEvaluationControllerTest.java`

**Interfaces:**
- Produces per case: first relevant rank, Hit@1, Recall@5, reciprocal rank, title path hits, whether rejection is expected, and whether the production reliability threshold would reject.
- Produces report: answerable/no-answer counts, Hit@1 rate, mean Recall@5, MRR, title path recall, and rejection accuracy while retaining existing source-hit fields.

- [x] **Step 1: 写指标失败测试**

构造相关来源位于第 2 名、两个期望来源只命中一个、两个标题路径命中一个，以及无答案最高相似度低于/高于 `0.60` 的结果。

- [x] **Step 2: 实现确定性指标计算**

来源召回按不同逻辑来源去重；标题路径使用完整列表精确匹配；MRR 使用首个期望来源排名；无答案拒答只检查是否存在相似度不低于 `0.60` 的候选。

- [x] **Step 3: 保持回答评估兼容无答案题**

`NO_ANSWER` 在回答结果没有来源时视为来源符合预期，预期要点仍通过现有字面包含评估；不改变付费回答接口的触发方式。

- [x] **Step 4: 运行服务和控制器测试**

```shell
./mvnw.cmd -Dtest=RagRetrievalEvaluationServiceTest,RagRetrievalEvaluationControllerTest,RagAnswerEvaluationServiceTest test
```

Expected: 排序、标题、拒答和兼容字段全部通过。

### Task 3: 竞争语料、运行器和事实文档

**Files:**
- Create: `evaluation/corpus/java/concurrent-collections.md`
- Create: `evaluation/corpus/mysql/indexes.md`
- Create: `evaluation/corpus/rag/agent-basics.md`
- Modify: `evaluation/run_retrieval_baseline.py`
- Modify: `evaluation/README.md`
- Modify: `docs/04-当前进度.md`
- Append: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/08-项目技术栈与功能总结.md`

**Interfaces:**
- Consumes: 8 份固定 Markdown 语料和 20 条固定题。
- Produces: 零 Chat 的真实检索报告；脚本只在用户显式运行时导入语料并调用 Embedding/Qdrant。

- [x] **Step 1: 增加三个相似竞争来源**

分别覆盖 ConcurrentHashMap、MySQL 索引和 Agent/Tool Calling，使 HashMap、MVCC、RAG 等问题不再只面对明显无关来源。

- [x] **Step 2: 更新运行器判定**

运行器打印并检查 Hit@1、Recall@5、MRR、标题路径召回和拒答准确率，不再只要求所有题期望来源进入 Top5。

- [x] **Step 3: 运行直接相关测试**

```shell
./mvnw.cmd -Dtest=FixedRagEvaluationDatasetTest,RagRetrievalEvaluationServiceTest,RagRetrievalEvaluationControllerTest,RagEvaluationDatasetControllerTest,RagAnswerEvaluationServiceTest test
```

Expected: 评估模型、HTTP JSON 和回答评估兼容性通过；不启动 storage profile。

- [x] **Step 4: 更新事实文档并提交**

明确区分“代码与确定性指标已验证”和“20 条真实 Embedding 基线尚未授权执行”。精确暂存本轮文件后提交 `feat: 扩展RAG困难评估指标`。
