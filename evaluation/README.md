# 固定 RAG 评估

PDF 正文提取的独立质量基线见 `evaluation/pdf/README.md`。该基线不依赖 RAG 语料、
数据库或模型调用。

本目录保存阶段 4 的可复现评估语料。固定题集共 20 题，覆盖精确术语、相似章节、标题路径、歧义问题、无答案拒答和跨章节检索。物理路径位于 `evaluation/corpus/`，导入时使用去掉 `evaluation/corpus/` 前缀后的路径作为逻辑来源：

| 物理文件 | 逻辑来源 | 覆盖题目 |
| --- | --- | --- |
| `java/collections.md` | `java/collections.md` | HashMap 树化与查询复杂度 |
| `java/concurrent-collections.md` | `java/concurrent-collections.md` | ConcurrentHashMap 并发控制与复合操作 |
| `java/concurrency.md` | `java/concurrency.md` | 线程池任务接收、`volatile` |
| `java/jvm.md` | `java/jvm.md` | GC Roots 与可达性分析 |
| `mysql/indexes.md` | `mysql/indexes.md` | 联合索引、最左前缀与覆盖索引 |
| `mysql/transactions.md` | `mysql/transactions.md` | MVCC 一致性快照 |
| `rag/agent-basics.md` | `rag/agent-basics.md` | RAG 与 Agent 职责、工具治理边界 |
| `rag/rag-basics.md` | `rag/rag-basics.md` | 来源、`topK` 与相似度阈值 |

先以 `storage` profile 启动后端，再执行：

```shell
python evaluation/run_retrieval_baseline.py --base-url http://127.0.0.1:18080
```

需要保留完整结果时显式指定输出文件：

```shell
python evaluation/run_retrieval_baseline.py --base-url http://127.0.0.1:18080 --output evaluation/results/YYYY-MM-DD-rag-retrieval-baseline.json
```

运行器会调用现有导入接口，再调用固定批量检索评估接口。重复执行不会重复向量化相同“来源 + 版本”；脚本不调用 Chat 模型，也不会自动删除已导入的基线资料。导入新版本会调用 Embedding 并写入 Qdrant，因此只能在明确授权模型费用后执行。

检索报告保留原有期望来源命中数，并增加以下指标：

- `Hit@1`：17 道可回答题中，第一条结果命中期望逻辑来源的比例。
- `Mean Recall@5`：每题 Top5 命中的不同期望逻辑来源比例，再对可回答题求平均。
- `MRR`：首个期望来源排名倒数的平均值。
- 标题路径召回率：实际结果完整标题路径精确命中的比例。
- 无答案拒答准确率：3 道无答案题在生产回答阈值 `0.60` 下没有可靠候选的比例。

脚本校验题量和指标结构，不预设未经真实运行验证的质量门槛。旧的 6 题基线结果保留为历史证据，不能当作当前 20 题结果。

2026-08-13 的首轮真实困难基线使用 `text-embedding-v3`、8 份语料和 18 个 chunk，结果见 `evaluation/results/2026-08-13-rag-hard-retrieval-baseline.json`：Hit@1 为 `1.0000`、Mean Recall@5 为 `0.9706`、MRR 为 `1.0000`、标题路径召回率为 `0.9130`、无答案拒答准确率为 `1.0000`。主要缺口集中在跨主题 Map 问题漏掉普通 HashMap 来源，以及一个歧义 Map 问题漏掉“扩容与负载因子”标题；当前没有证据支持直接引入 rerank。

## 查询拆分对照

可以在同一语料和指标口径下显式比较原查询与 `deterministic-clause-rrf-v1`：

```shell
python evaluation/run_retrieval_baseline.py --base-url http://127.0.0.1:18080 --compare-query-decomposition
```

候选策略不调用 Chat：它保留原问题，再从多句、多子句和带“分别/各自/两者”的协调结构中确定性生成最多 3 个额外查询，分别取 Top5 后按 chunk 身份去重并使用 Reciprocal Rank Fusion 合并为最终 Top5。当前固定题集会拆分 13 题并增加 29 次查询；对照响应同时返回原查询报告、候选报告、逐题查询计划、拆分题数和额外查询数，每题原查询只执行一次并被两份报告复用。

该策略当前只存在于评估接口 `/api/knowledge/evaluations/retrieval/query-decomposition`，没有接入资料检索、RAG 回答或面试链路。运行对照会为查询产生真实 Embedding 调用，必须显式授权费用；在真实结果同时改善多证据召回、保持 Hit@1/MRR 且不降低无答案拒答准确率前，不得把它写成生产能力。

## 回答要点覆盖

基线资料导入后，可以显式执行回答评估：

```shell
curl.exe -X POST http://127.0.0.1:18080/api/knowledge/evaluations/answers
```

接口会依次调用 20 道固定题目的现有 RAG 回答链路，每题写入正常的召回日志；无答案题在没有可靠来源时不调用 Chat，若错误命中可靠候选仍可能调用，因此整批最多产生 20 次 Chat 调用。报告使用 `normalized-literal-containment-v1`：回答和预期要点统一转为小写并移除非字母数字字符，再判断回答是否包含完整要点。该结果只表示可重复的字面覆盖率，不能替代语义正确性或人工评审。该接口会产生真实模型费用，必须显式授权后再执行。

## 面试回答评分

固定面试回答评分不依赖本目录的 RAG 语料，可在 `storage` profile 下显式执行：

```shell
curl.exe -X POST http://127.0.0.1:18080/api/interviews/evaluations/answers
```

接口依次执行五个脱敏样本。空回答在服务端拒绝，其余四项各产生一次 Chat 调用；响应只返回分数、知识依据状态、回答证据条数和知识证据条数，不返回回答、知识正文或引文。每次调用生成独立 `baselineRunId`，可以通过 `GET /api/interviews/evaluations/logs?mode=BASELINE&baselineRunId=...` 恢复分数、耗时和失败类型。评分链路当前不记录 token，不能从 HTTP 请求数推算 provider 计费回合。
