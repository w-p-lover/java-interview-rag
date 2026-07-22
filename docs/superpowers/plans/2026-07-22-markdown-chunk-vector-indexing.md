# Markdown Chunk Vector Indexing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成“首次导入写入 chunk 向量、重复导入不重复写入、写入失败后可重试”的阶段 2 最小闭环。

**Architecture:** `MarkdownContentImporter` 在现有版本登记、标题解析和切分之后调用 `MarkdownChunkIndexer`。`local` profile 使用无外部依赖实现，`storage` profile 使用 Spring AI `VectorStore` 批量写入 Qdrant；向量 ID 由来源、版本和 chunk 顺序稳定生成，写入失败时撤销版本登记。

**Tech Stack:** Java 17、Spring Boot 3.5.16、Spring AI 1.0.0、Spring AI Alibaba 1.0.0.2、DashScope `text-embedding-v3`、Qdrant、JUnit 5、Mockito

## Global Constraints

- 所有仓库文件写入只发生在 `D:\IdeaProjects\java-interview-rag`；`D:\IdeaProjects\blog\blog-springboot` 仅只读提供服务器连接配置。
- 使用独立数据库 `java_interview_rag` 和 collection `java-interview-rag-chunks`。
- 不提交真实密钥，不升级当前 Spring AI/Qdrant 依赖链，不提前实现更新、删除、重建或 RAG 检索 API。
- `local` profile 不连接外部服务；`storage` profile 才写入真实向量。
- Maven 使用仓库内 `.m2`、`.tmp` 和显式 `maven.repo.local`。

---

### Task 1: 用测试固定导入与向量写入语义

**Files:**
- Create: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownContentImporterVectorTest.java`
- Create: `src/test/java/com/javainterview/rag/knowledge/vector/QdrantMarkdownChunkIndexerTest.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownImportRegistryTest.java`

**Interfaces:**
- Produces: `MarkdownChunkIndexer.index(MarkdownDocument, List<MarkdownChunk>)`
- Produces: `MarkdownImportRegistry.unregister(MarkdownDocument)`

- [x] **Step 1: Write failing importer tests**

覆盖首次导入调用一次索引器、重复导入不再调用索引器，以及索引失败后撤销登记并允许同一版本重试。

- [x] **Step 2: Run tests and verify RED**

Run: `cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dtest=MarkdownContentImporterVectorTest,QdrantMarkdownChunkIndexerTest,MySqlMarkdownImportRegistryTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test"`

Expected: compilation failure because the indexer and unregister contract do not exist.

### Task 2: 实现最小向量索引和失败补偿

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownChunkIndexer.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownContentImporter.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownImportRegistry.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/InMemoryMarkdownImportRegistry.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeDocumentVersionMapper.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownImportRegistry.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/vector/QdrantMarkdownChunkIndexer.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java`

**Interfaces:**
- `MarkdownChunkIndexer.index(...)` receives one document and its ordered chunks.
- `MarkdownImportRegistry.unregister(...)` removes only the same source and version.
- Qdrant `Document` IDs are deterministic UUIDs derived from `source + version + chunk order`.

- [x] **Step 1: Implement the minimum contracts**

保留现有两参数导入器构造方式并默认使用空索引器；Spring 配置显式注入 profile 对应索引器。

- [x] **Step 2: Implement Qdrant mapping**

批量写入正文，并保存来源、文档版本、chunk/section 顺序、标题级别和标题路径元数据。

- [x] **Step 3: Implement compensation**

向量写入抛出运行时异常时撤销同一来源与版本登记，再原样抛出异常；稳定 ID 保证重试不会生成另一批点。

- [x] **Step 4: Run focused tests and verify GREEN**

Run the same targeted command and expect all selected tests to pass.

### Task 3: 真实验证并同步项目事实

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

- [x] **Step 1: Run focused backend verification**

执行直接相关测试；不运行无关前端或浏览器测试。

- [x] **Step 2: Verify real storage flow**

用唯一测试来源启动 `storage` profile，真实导入一次并确认 Qdrant 写入，再重复导入并确认向量数量不增加；验证后清理 MySQL 记录和 Qdrant 点。

- [x] **Step 3: Update factual docs**

记录已实现能力、客户端版本警告、真实验证证据以及仍未实现的更新、删除和重建。

- [x] **Step 4: Review and commit**

运行 `git diff --check`、审查 `git diff` 和 `git status --short`，只提交本轮直接相关文件，提交信息为 `feat: 实现 Markdown chunk 向量写入`。
