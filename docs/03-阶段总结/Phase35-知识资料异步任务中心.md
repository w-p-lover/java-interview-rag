# Phase35：知识资料异步任务中心

## 本轮目标

把资料导入/重建从单次同步 HTTP 调用扩展为可查询、可恢复边界的异步任务闭环，降低大文件或多存储索引阶段阻塞资料库页面的风险，同时保留原有同步接口兼容性。

## 已完成

- 新增 `KnowledgeDocumentTask` 领域模型和 `QUEUED/RUNNING/SUCCEEDED/FAILED` 状态。
- 新增本地内存任务存储，状态转换单向且终态不可被重复回调覆盖。
- 新增 2 线程、8 队列的有界 `ThreadPoolTaskExecutor`，提交过载返回 HTTP 429。
- 新增异步导入、异步重建和任务状态查询接口。
- 异步任务复用既有 `MarkdownContentImporter`，成功只回传版本、切片数、格式和告警数，失败只记录异常类型，不记录上传正文。
- storage 新增 `t_knowledge_document_task` 表、MyBatis Mapper 和 MySQL Store；应用重启时遗留 `RUNNING` 任务标记为 `APPLICATION_RESTARTED`，要求用户重新上传，不自动重放。
- 资料库页面接入任务轮询、成功统计、失败原因和重新选择文件重试提示；同步接口仍保留。

## 变更文件

- `src/main/java/com/javainterview/rag/knowledge/task/*`
- `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeDocumentTask*`
- `src/main/java/com/javainterview/rag/knowledge/web/MarkdownDocumentController.java`
- `src/main/java/com/javainterview/rag/knowledge/web/KnowledgeDocumentTask*.java`
- `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java`
- `deploy/mysql/init/022_knowledge_document_task.sql`
- `frontend/src/api/knowledge.ts`
- `frontend/src/components/KnowledgeDocumentTaskPanel.vue`
- `frontend/src/views/KnowledgeWorkbenchView.vue`
- `docs/04-当前进度.md`、`docs/09-后续开发路线.md`

## 验证

- JDK 21 下 `./mvnw.cmd -q -DskipTests compile` 通过。
- `KnowledgeDocumentTaskControllerTest`、`InMemoryKnowledgeDocumentTaskStoreTest` 和既有 `MarkdownDocumentControllerTest` 通过。
- `frontend` 执行 `npm run build` 通过。
- `git diff --check` 通过。

## 边界与后续

本轮不是 MQ 或分布式任务队列：任务上传正文只在当前进程任务生命周期内存在，重启不会自动恢复正文或重放索引。异步接口当前在提交阶段完成文件解析，再将导入/索引阶段放入有界执行器；后续如出现真实大文件解析阻塞或任务取消需求，再单独补充端到端截止时间和取消语义。
