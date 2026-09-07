# Phase25-索引版本契约

## 升级目标

让知识索引能够记录生成它所依赖的 Embedding、切分、解析和索引 Schema 版本，避免内容 SHA-256 相同却无法区分索引生成契约的情况。

## 完成内容

- 新增 `KnowledgeIndexVersionMetadata`，统一描述 Embedding 模型、Embedding 维度、Chunk 策略版本、PDF 解析器版本和索引 Schema 版本。
- Qdrant Chunk payload 和 Lucene 文档在写入时同步保存五项版本元数据。
- 通过 `JAVA_INTERVIEW_RAG_EMBEDDING_MODEL`、`JAVA_INTERVIEW_RAG_EMBEDDING_DIMENSIONS`、`JAVA_INTERVIEW_RAG_CHUNK_POLICY_VERSION`、`JAVA_INTERVIEW_RAG_PARSER_VERSION`、`JAVA_INTERVIEW_RAG_INDEX_SCHEMA_VERSION` 支持显式覆盖。
- 保持现有构造函数兼容，默认值与当前 `text-embedding-v3`、1024 维、字段化 BM25 和 PDF 解析实现一致。

## 变更文件

- `src/main/java/com/javainterview/rag/knowledge/document/KnowledgeIndexVersionMetadata.java`
- `QdrantMarkdownChunkIndexer.java`、`LuceneMarkdownChunkIndex.java`
- `MarkdownImportConfiguration.java`、`MarkdownSparseSearchConfiguration.java`
- `src/main/resources/application.yml`
- Qdrant/Lucene 索引测试及当前进度、路线、技术总结文档。

## 验证结果

- Java 21 下 `QdrantMarkdownChunkIndexerTest`、`LuceneMarkdownChunkIndexTest` 通过。
- `git diff --check` 通过。

## 边界与后续

本阶段只保证新写入索引携带可比较的生成契约，不修改 `t_knowledge_document_version`，不自动迁移历史 Qdrant/Lucene 数据，也不提供自动重建或回退。下一步优先把元数据缺失/不匹配纳入只读巡检，再基于真实旧索引决定登记字段和有界重建入口。
