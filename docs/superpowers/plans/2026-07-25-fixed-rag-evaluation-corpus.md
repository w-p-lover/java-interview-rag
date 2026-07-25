# 固定 RAG 评估语料 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 提供可重复导入的固定 Markdown 语料，并取得 6 道题的真实批量检索来源命中基线。

**Architecture:** 语料以仓库文件保存，逻辑来源继续由现有 HTTP 导入接口显式传入。Python 标准库运行器只负责编排导入与批量评估，不绕过应用服务或直接操作 MySQL/Qdrant。

**Tech Stack:** Markdown、Python 3 标准库、Spring MVC 现有接口、DashScope `text-embedding-v3`、Qdrant

## Global Constraints

- 所有项目文件写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅可只读参考。
- 新增或修改的代码注释使用简体中文。
- 不修改检索算法、数据库结构、前端或 Chat 回答链路。

---

### Task 1: 固化语料与运行器

**Files:**
- Create: `evaluation/README.md`
- Create: `evaluation/run_retrieval_baseline.py`
- Create: `evaluation/corpus/java/collections.md`
- Create: `evaluation/corpus/java/concurrency.md`
- Create: `evaluation/corpus/java/jvm.md`
- Create: `evaluation/corpus/mysql/transactions.md`
- Create: `evaluation/corpus/rag/rag-basics.md`

**Interfaces:**
- Consumes: `POST /api/knowledge/documents/import`、`GET /api/knowledge/evaluations/retrieval`
- Produces: 5 个精确逻辑来源和一份批量检索 JSON 报告

- [x] **Step 1: 写入覆盖 6 道固定题目的 5 份 Markdown 语料**

  每份语料必须包含对应 `expectedAnswerPoints` 的事实，并使用与 `FixedRagEvaluationDataset` 一致的逻辑来源。

- [x] **Step 2: 实现最小 Python 编排脚本**

  脚本使用 `urllib.request` 发送 multipart 请求，逐个导入语料，再请求批量评估；若不是 6/6 命中则返回非零退出码。

- [x] **Step 3: 静态校验语料映射**

  Run: `python evaluation/run_retrieval_baseline.py --help`

  Expected: 显示 `--base-url` 和 `--timeout` 参数，退出码为 0。

### Task 2: 运行真实检索基线并记录

**Files:**
- Create: `evaluation/results/2026-07-25-retrieval-baseline.json`
- Modify: `README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: Task 1 的 5 份语料与运行器
- Produces: 可复核的真实命中率、相似度和阶段进度记录

- [x] **Step 1: 执行最小相关测试**

  Run: `mvnw.cmd -Dtest=FixedRagEvaluationDatasetTest,RagRetrievalEvaluationServiceTest,RagRetrievalEvaluationControllerTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

  Expected: 评估题集、服务和控制器测试全部通过。

- [x] **Step 2: 启动当前分支的临时 storage 实例并执行基线**

  Run: `python evaluation/run_retrieval_baseline.py --base-url http://127.0.0.1:18081`

  Expected: 5 个来源导入成功或已存在，报告为 6/6 命中，退出码为 0。

- [x] **Step 3: 保存报告并同步文档**

  报告保存本轮真实响应和运行元数据；进度、开发记录和 ADR 只写已验证事实。

- [x] **Step 4: 检查并提交**

  Run: `git diff --check` 与 `git status --short`

  Expected: 没有空白错误，只包含本轮直接相关文件。
