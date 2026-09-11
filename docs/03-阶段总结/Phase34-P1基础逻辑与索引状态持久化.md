# Phase 34：P1 基础逻辑重排与索引状态持久化

## 本轮目标

围绕当前 Java 后端、RAG 工程和面试深挖方向，重新排序 P1：优先基础逻辑、失败恢复、一致性和可解释性；用户身份、GraphRAG、MCP 等方向继续保留进入条件。代码上补齐多存储索引编排状态的跨重启恢复。

## 已完成

- 新增 `t_knowledge_indexing_state`，以 `source + document_version` 为主键保存 Chunk 数量、`INDEXING/READY/FAILED` 状态、失败异常类型和更新时间。
- 新增 `MySqlMarkdownIndexingStateStore`，复用现有 `MarkdownIndexingStateStore` 契约；storage 使用 MySQL，local 继续使用内存实现。
- 使用 MySQL upsert 保证重复状态更新幂等；状态查询仍由既有一致性巡检和召回失败分层读取。
- 删除来源时同步删除对应状态，避免来源删除后残留状态误导巡检。

## 变更文件

- `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeIndexingStateEntity.java`
- `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeIndexingStateMapper.java`
- `src/main/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownIndexingStateStore.java`
- `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java`
- `deploy/mysql/init/021_knowledge_indexing_state.sql`
- `src/test/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownIndexingStateStoreTest.java`
- `docs/04-当前进度.md`、`docs/09-后续开发路线.md`、`docs/10-面试准备与项目掌握要求.md`

## 验证

- JDK 21 下 Maven 编译通过。
- `MySqlMarkdownIndexingStateStoreTest`、`TrackingMarkdownChunkIndexerTest`、`KnowledgeIndexConsistencyServiceTest` 共 9 项通过。
- `git diff --check` 通过。

## 边界与下一步

本轮只持久化编排状态，不自动修复 Qdrant/Lucene，不引入 MQ，也不宣称具备分布式事务。下一步优先选择一个真实失败样本，补充“巡检结果→用户确认→按来源重建”的可恢复闭环；没有真实运行需求时不提前做自动修复。
