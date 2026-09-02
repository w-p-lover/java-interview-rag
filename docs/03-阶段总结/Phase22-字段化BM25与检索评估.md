# Phase22：字段化 BM25 与检索评估闭环

> 更新时间：2026-09-02。本阶段完成 Sparse 检索字段化和困难样本评估基础，未调用付费模型。

## 阶段目标

将 Sparse 检索从单一正文/标题查询升级为可解释字段权重，并建立能发现来源内 Chunk 错位、Hard Negative 污染、上下文噪声和延迟退化的固定评估指标。

## 完成内容

- Lucene 索引拆分正文、标题、代码和 Java 标识符字段，权重固定为 `1.0/2.0/2.5/3.0`。
- Sparse API 返回 `BM25-fielded-v1` 和字段权重策略，检索结果继续保留完整来源元数据。
- 固定评估题支持期望 Chunk 与 Hard Negative Chunk 标注，当前为三个 Java 精确题接入代表性标注。
- 报告和逐题结果增加 Precision@5、nDCG@5、Chunk Recall@5、Hard Negative 避免率、上下文噪声率和检索延迟 P95。
- 既有拒答策略继续按通道隔离：Dense/Hybrid 使用余弦阈值，Sparse 使用候选存在性。

## 修改文件

- `src/main/java/com/javainterview/rag/knowledge/vector/LuceneMarkdownChunkIndex.java`：增加四个 BM25 字段、可解释权重和 Java 标识符抽取。
- `src/main/java/com/javainterview/rag/knowledge/web/MarkdownSparseSearchResponse.java`：返回字段权重策略。
- `src/main/java/com/javainterview/rag/knowledge/evaluation/RagEvaluationCase.java`、`RagEvaluationChunkExpectation.java`：增加 Chunk/Hard Negative 评估契约。
- `src/main/java/com/javainterview/rag/knowledge/evaluation/FixedRagEvaluationDataset.java`：补充代表性 Chunk 和负例标注。
- `src/main/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationService.java`：计算高级指标和逐题检索延迟。
- `src/main/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationCaseResult.java`、`RagRetrievalEvaluationReport.java`、`RagRetrievalEvaluationMetrics.java`：暴露逐题与聚合指标。
- `src/test/java/com/javainterview/rag/knowledge/vector/LuceneMarkdownChunkIndexTest.java`、`src/test/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationServiceTest.java`：增加字段、Chunk、Hard Negative 和指标测试。
- `docs/04-当前进度.md`、`docs/05-开发记录.md`、`docs/07-技术决策记录.md`、`docs/08-项目技术栈与功能总结.md`、`docs/09-后续开发路线.md`、`evaluation/README.md`：同步实现状态和真实基线边界。

## 指标口径

- Precision@5：Top5 返回结果中命中期望 Chunk 的比例；未配置 Chunk 标注时按期望逻辑来源判定相关性。
- nDCG@5：按上述二值相关性计算折损累计增益并与理想排序归一化。
- Chunk Recall@5：期望 Chunk 在 Top5 中命中的比例，只对配置 Chunk 标注的题聚合。
- Hard Negative 避免率：标注负例未出现在 Top5 的比例，只对配置负例的题聚合。
- 上下文噪声率：`1 - Precision@5`，仅聚合可回答题。
- 检索延迟 P95：逐题 retriever 调用耗时的 95 分位数，不包含 Embedding 生成和 Chat 回答。

## 验证结果

- JDK 21 定向 Maven 测试共 12 个用例通过，`git diff --check` 通过。
- 字段化权重和高级指标目前只完成本地确定性验证；新字段真实 storage 基线待下一轮受控执行。

## 风险与后续

- 当前 Java 标识符抽取是确定性正则，尚未覆盖所有泛型、注解和中文上下文边界。
- 固定题只补充了代表性 Chunk/负例标注，不能外推为完整语料质量结论。
- 下一轮运行一次授权 storage 固定 20 题三策略对照，保存新字段权重下的来源、拒答、Precision/nDCG、噪声和 P95 结果；若无稳定收益，不增加 rerank。
