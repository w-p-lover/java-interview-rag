# 固定 RAG 评估

PDF 正文提取的独立质量基线见 `evaluation/pdf/README.md`。该基线不依赖 RAG 语料、
数据库或模型调用。

本目录保存阶段 4 的可复现评估语料。物理路径位于 `evaluation/corpus/`，导入时使用去掉 `evaluation/corpus/` 前缀后的路径作为逻辑来源：

| 物理文件 | 逻辑来源 | 覆盖题目 |
| --- | --- | --- |
| `java/collections.md` | `java/collections.md` | HashMap 树化与查询复杂度 |
| `java/concurrency.md` | `java/concurrency.md` | 线程池任务接收、`volatile` |
| `java/jvm.md` | `java/jvm.md` | GC Roots 与可达性分析 |
| `mysql/transactions.md` | `mysql/transactions.md` | MVCC 一致性快照 |
| `rag/rag-basics.md` | `rag/rag-basics.md` | 来源、`topK` 与相似度阈值 |

先以 `storage` profile 启动后端，再执行：

```shell
python evaluation/run_retrieval_baseline.py --base-url http://127.0.0.1:18080
```

运行器会调用现有导入接口，再调用固定批量检索评估接口。重复执行不会重复向量化相同“来源 + 版本”；脚本不调用 Chat 模型，也不会自动删除已导入的基线资料。

## 回答要点覆盖

基线资料导入后，可以显式执行回答评估：

```shell
curl.exe -X POST http://127.0.0.1:18080/api/knowledge/evaluations/answers
```

接口会依次调用 6 道固定题目的现有 RAG 回答链路，每题写入正常的召回日志并产生一次 Chat 调用。报告使用 `normalized-literal-containment-v1`：回答和预期要点统一转为小写并移除非字母数字字符，再判断回答是否包含完整要点。该结果只表示可重复的字面覆盖率，不能替代语义正确性或人工评审。

## 面试回答评分

固定面试回答评分不依赖本目录的 RAG 语料，可在 `storage` profile 下显式执行：

```shell
curl.exe -X POST http://127.0.0.1:18080/api/interviews/evaluations/answers
```

接口依次执行五个脱敏样本。空回答在服务端拒绝，其余四项各产生一次 Chat 调用；响应只返回分数、知识依据状态、回答证据条数和知识证据条数，不返回回答、知识正文或引文。每次调用生成独立 `baselineRunId`，可以通过 `GET /api/interviews/evaluations/logs?mode=BASELINE&baselineRunId=...` 恢复分数、耗时和失败类型。评分链路当前不记录 token，不能从 HTTP 请求数推算 provider 计费回合。
