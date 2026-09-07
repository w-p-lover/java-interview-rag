# Phase26-索引版本漂移巡检

## 升级目标

在已有索引版本契约基础上，让只读一致性巡检识别“Chunk 数量看似正确，但索引生成契约已经缺失或不匹配”的情况。

## 完成内容

- `KnowledgeIndexCounts` 新增当前版本中元数据匹配的 Chunk 数。
- Qdrant 巡检增加基于来源、文档版本和五项版本契约的原生过滤计数。
- Lucene 巡检增加基于来源、文档版本和五项版本契约的精确计数。
- `KnowledgeIndexConsistencyReport.StoreCheck` 新增匹配数量和 `indexVersionMatched` 字段。
- 一致性服务在数量状态为正常但版本契约不匹配时，将整体报告标记为不一致；原有缺失、超额、历史版本残留和不可用状态优先保留。

## 验证结果

- Java 21 下通过 `KnowledgeIndexConsistencyServiceTest`、`QdrantKnowledgeVectorIndexInspectorTest`、`LuceneMarkdownChunkIndexTest`、`KnowledgeIndexConsistencyControllerTest`。
- 不调用 Embedding、Chat、MySQL 或真实 Qdrant，不执行自动修复。

## 边界与后续

当前只能判断“匹配/不匹配”，不能从缺失字段的旧索引推断其具体历史版本，也未修改 MySQL 登记表。下一步仅在真实旧索引样本出现时，继续区分全部缺失与部分不匹配，并评估有界重建或回退入口。
