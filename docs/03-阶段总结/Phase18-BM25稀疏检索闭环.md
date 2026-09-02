# Phase18：BM25 稀疏检索闭环

## 本轮目标

在已有 Qdrant Embedding 检索之外增加真正可调用的关键词稀疏检索，优先解决 Java 标识符、方法名、异常名、版本号和数字阈值的精确召回问题，并保持现有文档版本生命周期。

## 完成内容

- 引入 Apache Lucene 9.12.1、标准分析器和 `BM25Similarity`。
- 新增 Lucene Chunk 倒排索引，保存来源、版本、顺序、标题路径、正文、页码和块类型。
- 新增组合索引器，导入、重建、历史版本删除和来源删除同时维护 Qdrant 与 Lucene。
- `local` 使用进程内 Lucene 目录，`storage` 使用 `${JAVA_INTERVIEW_RAG_SPARSE_INDEX_PATH:.tmp/sparse-index}` 文件目录。
- 新增 `GET /api/knowledge/search/sparse?query=...&topK=5`，响应明确标识 `algorithm=BM25`，不混合向量分数。
- 重复索引通过稳定 ID 更新文档，避免重复召回；重建和来源删除后不再召回旧 Chunk。

## 修改文件

- `pom.xml`：增加 Lucene 依赖和版本属性。
- `src/main/java/.../CompositeMarkdownChunkIndexer.java`：新增索引器组合委托。
- `src/main/java/.../LuceneMarkdownChunkIndex.java`：新增 BM25 倒排索引及搜索实现。
- `src/main/java/.../MarkdownSparseSearchService.java`：新增稀疏检索服务。
- `src/main/java/.../MarkdownSparseSearchConfiguration.java`：装配 local/storage 索引目录和服务。
- `src/main/java/.../MarkdownSparseSearchController.java`、`MarkdownSparseSearchResponse.java`：新增独立 HTTP 接口。
- `src/main/java/.../MarkdownImportConfiguration.java`、`src/main/resources/application-storage.yml`：接入组合索引器和持久化路径配置。
- `src/test/java/.../LuceneMarkdownChunkIndexTest.java`：覆盖关键词命中、topK、重复导入、来源删除和版本清理。

## 验证结果

- JDK 21 下 `mvnw.cmd -q -Dtest=LuceneMarkdownChunkIndexTest test` 通过，3 个测试全部通过。
- JDK 21 下 `mvnw.cmd -q -DskipTests compile` 通过。
- 未调用 Chat、Embedding、Qdrant 或其他付费/外部服务。

## 重要边界

- 本轮只完成 BM25-only 独立检索，不宣称已经完成 Hybrid Search 或 RAG 质量提升。
- `StandardAnalyzer` 的中文分词、复杂符号和查询拆分效果尚未用固定 20 题做量化对照。
- 下一轮应生成 BM25-only 与 dense-only 的固定题集对照报告，再决定是否实现受控融合。

## 后续勘误：2026-09-02 对照与融合已完成

上述“下一轮”是 BM25 独立检索完成时的历史待办。随后已完成 Hybrid-RRF v1 和一次真实 Dense-only、BM25-only、Hybrid-RRF 固定题集对照；当前结论与实现状态以 `docs/09-后续开发路线.md` 和 `evaluation/results/2026-09-02-hybrid-retrieval-baseline.json` 为准。
