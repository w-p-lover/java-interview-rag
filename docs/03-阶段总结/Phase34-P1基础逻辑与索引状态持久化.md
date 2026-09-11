# Phase 34：P1 基础逻辑重排与索引状态持久化

## 本轮目标

围绕当前 Java 后端、RAG 工程和面试深挖方向，重新排序 P1：优先基础逻辑、失败恢复、一致性和可解释性；用户身份、GraphRAG、MCP 等方向继续保留进入条件。代码上补齐多存储索引编排状态的跨重启恢复。

## 已完成

- 新增 `t_knowledge_indexing_state`，以 `source + document_version` 为主键保存 Chunk 数量、`INDEXING/READY/FAILED` 状态、失败异常类型和更新时间。
- 新增 `MySqlMarkdownIndexingStateStore`，复用现有 `MarkdownIndexingStateStore` 契约；storage 使用 MySQL，local 继续使用内存实现。
- 使用 MySQL upsert 保证重复状态更新幂等；状态查询仍由既有一致性巡检和召回失败分层读取。
- 删除来源时同步删除对应状态，避免来源删除后残留状态误导巡检。
- 知识资料详情页接入只读一致性巡检，展示 Dense/Qdrant、Sparse/Lucene 和编排状态；异常时要求人工确认后再重建，不自动修复。
- 重建流程在索引写入成功但章节目录更新失败时回写 `FAILED`；重建成功后详情页自动再次巡检，避免只凭接口返回判断已收敛。
- 来源删除失败时不再清理索引编排状态；只有全部索引删除成功才移除状态，保证失败后仍可巡检和人工重试。
- 资料维护页将删除动作错误与详情读取错误分离；删除失败时保留详情、巡检结果和确认框，可直接重试淘汰，不需要重新选择资料。

## 变更文件

- `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeIndexingStateEntity.java`
- `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeIndexingStateMapper.java`
- `src/main/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownIndexingStateStore.java`
- `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java`
- `deploy/mysql/init/021_knowledge_indexing_state.sql`
- `src/test/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownIndexingStateStoreTest.java`
- `frontend/src/api/knowledge.ts`、`frontend/src/composables/useKnowledgeCatalog.ts`、`frontend/src/components/knowledge/KnowledgeDocumentDetailPanel.vue`、`frontend/src/views/KnowledgeAnswerView.vue`
- `docs/04-当前进度.md`、`docs/09-后续开发路线.md`、`docs/10-面试准备与项目掌握要求.md`

## 验证

- JDK 21 下 Maven 编译通过。
- `MySqlMarkdownIndexingStateStoreTest`、`TrackingMarkdownChunkIndexerTest`、`KnowledgeIndexConsistencyServiceTest` 共 9 项通过。
- `git diff --check` 通过。
- `frontend` 执行 `npm run build` 通过。

## 边界与下一步

本轮只持久化编排状态并接入“巡检结果→人工确认→已有重建入口→自动复查”的页面闭环，同时修复删除失败时状态被误清理的问题。不自动修复 Qdrant/Lucene，不引入 MQ，也不宣称具备分布式事务。下一步优先选择一个真实失败样本，验证重建后的状态和三存储计数能够重新收敛；没有真实运行需求时不提前做自动修复。
