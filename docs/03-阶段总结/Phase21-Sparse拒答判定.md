# Phase21：独立 Sparse 拒答判定

> 更新时间：2026-09-02。本阶段修复 Dense、BM25 与 Hybrid 评估中不可比较的拒答口径，未重新调用付费模型。

## 阶段目标

让每条检索通道使用与其分数体系匹配的拒答判定，并保留既有评估报告/API 的兼容形状，避免把 BM25 原始分数误读为余弦相似度。

## 完成内容

- 新增 `RagRetrievalRejectionPolicy`：`cosine_threshold_v1` 用于 Dense，`candidate_presence_v1` 用于 Sparse/BM25。
- `RagRetrievalEvaluationService` 的指标计算收敛为单一实现，默认路径显式使用余弦策略。
- `HybridRetrievalEvaluationService` 显式为 Sparse 评估选择候选存在性策略，为 Hybrid 选择余弦策略。
- 保留 `RagRetrievalEvaluationCaseResult.rejectedAtAnswerThreshold` 字段以兼容既有 JSON/测试，但文档明确它表示当前启用的策略结果。

## 修改文件

- `src/main/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalRejectionPolicy.java`：定义两种拒答策略及报告占位阈值。
- `src/main/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationService.java`：接入策略并删除重复指标计算。
- `src/main/java/com/javainterview/rag/knowledge/evaluation/HybridRetrievalEvaluationService.java`：为 Sparse/Hybrid 选择正确策略。
- `src/test/java/com/javainterview/rag/knowledge/evaluation/RagRetrievalEvaluationServiceTest.java`：增加低分 BM25 候选边界回归。
- `docs/04-当前进度.md`、`docs/05-开发记录.md`、`docs/07-技术决策记录.md`、`docs/08-项目技术栈与功能总结.md`、`docs/09-后续开发路线.md`、`evaluation/README.md`：同步当前口径和后续入口。

## 验证结果

- JDK 21 下执行 `mvnw.cmd` 定向测试：`RagRetrievalEvaluationServiceTest` 4 个、`RagRetrievalEvaluationControllerTest` 3 个，共 7 个用例通过。
- `git diff --check` 待提交前执行。

## 风险与后续

- 候选存在性只解决分数尺度错误，不等于 BM25 拒答质量已经证明；非空低相关候选仍可能进入后续回答链路。
- 下一阶段先完成字段化 BM25 和 Hard Negative/Chunk 级困难样本，再重新统计 Sparse 的拒答、Precision@K、nDCG@K、噪声和延迟；暂不引入固定 BM25 数值阈值或复杂 rerank。
