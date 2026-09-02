# Phase19：Hybrid Retrieval v1 闭环

## 本轮目标

把前一轮独立的 Dense 和 BM25 检索器组织成可调用、可解释、可评估的 Hybrid 召回链路，并接入 RAG 主回答路径。

## 完成内容

- Dense 与 BM25 各取最多 `topK*4` 候选，按来源、版本和 Chunk 顺序稳定去重。
- 使用 RRF（`1 / (60 + rank)`）融合排名，不直接相加 Cosine 与 BM25 原始分数。
- Hybrid 查询返回两路候选数量、每条结果的 Dense/Sparse 排名、原始分数和 RRF 分数。
- `storage` profile 的同步和 SSE RAG 回答默认使用 Hybrid；相邻 Chunk 扩展仍在融合结果之后执行。
- 保留旧 Dense 检索服务给面试出题、项目深挖和历史评估，避免无关业务同时切换策略。
- 新增固定题集三策略评估入口，统一输出 Dense-only、BM25-only、Hybrid 三份报告。
- 对 Sparse/Hybrid 的拒答指标增加尺度说明，明确它表示候选是否为空，不能把 BM25 分数和余弦阈值数值等价。

## 修改文件

- `src/main/java/.../MarkdownHybridSearchService.java`：双路召回、RRF、去重和可解释命中证据。
- `src/main/java/.../MarkdownHybridSearchController.java`：Hybrid 检索 HTTP 接口。
- `src/main/java/.../RagAnswerService.java`：storage RAG 主链路优先使用 Hybrid。
- `src/main/java/.../HybridRetrievalEvaluationService.java`、`HybridRetrievalEvaluationReport.java`：三策略评估闭环。
- `src/main/java/.../RagRetrievalEvaluationService.java`：支持复用同一评估器运行不同检索器。
- `src/main/java/.../MarkdownHybridSearchConfiguration.java`、`RagRetrievalEvaluationConfiguration.java`、`RagRetrievalEvaluationController.java`：Bean 和评估接口装配。
- `README.md`、`docs/01-需求边界.md`、`docs/04-当前进度.md`、`docs/07-技术决策记录.md`、`docs/08-项目技术栈与功能总结.md`、`docs/09-后续开发路线.md`：同步实现边界和调用入口。
- `src/test/java/.../MarkdownHybridSearchServiceTest.java`、`RagAnswerServiceTest.java`：覆盖 RRF 排名、双路证据和主链路接入。

## 验证结果

- JDK 21 下 Hybrid、RAG、检索评估 Controller 和默认 local 上下文相关测试通过。
- JDK 21 下 Maven 编译通过。
- 未调用真实 Chat、Embedding、Qdrant 或 MySQL；三策略评估接口已实现但尚未在授权 storage 环境执行。

## 重要边界

- 当前 RRF 只证明可解释的多路召回编排，不代表 Hybrid 已经改善固定题集质量。
- BM25 原始分数和 Dense Cosine 分数不在同一尺度，回答可靠性不能使用 RRF 分数或 BM25 分数直接套用 `0.60` 余弦阈值。
- 下一步必须执行一次同题集 Dense-only、BM25-only、Hybrid 对照，并同时观察回答要点、拒答、延迟和上下文噪声，再决定是否保留默认 Hybrid 或进入 Rerank POC。

## 后续勘误：2026-09-02 真实三策略基线已完成

上述“下一步”是本阶段实现完成时的待办记录。2026-09-02 已在独立 `storage` 实例完成一次真实对照，结果保存于 `evaluation/results/2026-09-02-hybrid-retrieval-baseline.json`：三策略 Hit@1 均为 `1.0000`、Mean Recall@5 均为 `0.9706`、MRR 均为 `1.0000`；Hybrid 标题路径召回 `0.9565`，Dense-only `0.9130`。因此 Hybrid 保留为 storage RAG 默认主链路，但尚未证明来源排序或回答质量全面提升；BM25-only 拒答指标因复用 Dense 余弦阈值而不可直接比较。后续改为先完善 Sparse 阈值、字段化 BM25、Chunk 级困难样本、噪声与延迟。
