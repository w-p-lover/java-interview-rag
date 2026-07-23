# Markdown Vector Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成同来源新版本向量替换和显式文档删除清理，使 MySQL 版本登记与 Qdrant 向量具备可重试的最小生命周期。

**Architecture:** `MarkdownContentImporter` 继续作为同步编排入口：先登记并写入新版本，再通过 `MarkdownChunkIndexer` 删除同来源其他版本向量；显式删除时先删来源向量，再删来源全部登记。Qdrant 使用结构化元数据过滤，`local` profile 的索引删除保持空操作。

**Tech Stack:** Java 17、Spring Boot 3.5.16、Spring AI 1.0.0、Spring AI Alibaba 1.0.0.2、MyBatis-Plus、MySQL、Qdrant、JUnit 5、Mockito

## Global Constraints

- 所有仓库文件写入只发生在 `D:\IdeaProjects\java-interview-rag`；`D:\IdeaProjects\blog\blog-springboot` 仅可只读参考。
- 新增或修改的代码注释、日志、异常和用户可见提示默认使用简体中文。
- 不增加 Maven 依赖、数据库字段或新表，不实现状态机、手动重建、RAG 检索或前端页面。
- 来源按大小写敏感的精确字符串区分并拒绝首尾空白；`source` 排序规则迁移为 MySQL 5.7/8 均支持的 `utf8mb4_bin`。
- 历史“来源 + 版本”登记继续保留；Qdrant 对每个来源只保留最新成功导入版本的向量。
- Maven 使用仓库内 `.m2`、`.tmp` 和显式 `maven.repo.local`，只运行直接相关验证。

---

### Task 1: 用测试固定替换与删除契约

**Files:**
- Modify: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownContentImporterVectorTest.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownDocumentTest.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownImportRegistryTest.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownImportRegistryTest.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/vector/QdrantMarkdownChunkIndexerTest.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/web/MarkdownDocumentControllerTest.java`

**Interfaces:**
- Produces: `MarkdownChunkIndexer.deleteOtherVersions(MarkdownDocument)`
- Produces: `MarkdownChunkIndexer.deleteBySource(String)`
- Produces: `MarkdownImportRegistry.unregisterBySource(String)`
- Produces: `MarkdownContentImporter.deleteBySource(String): boolean`

- [x] **Step 1: Write the failing importer and registry tests**

增加行为断言：

```java
MarkdownImportResult changed = importer.importContent("java/basic.md", "# Java\n新内容");
verify(chunkIndexer).deleteOtherVersions(changed.document());

assertTrue(registry.unregisterBySource("java/basic.md"));
assertTrue(registry.register(MarkdownDocument.create("java/basic.md", "原内容")));
```

同时验证旧向量清理失败会撤销新版本登记并允许重试，以及 `deleteBySource` 先调用索引器再清理登记。

- [x] **Step 2: Write the failing Qdrant and Web tests**

捕获 `VectorStore.delete(Filter.Expression)`，断言替换条件由来源相等和版本不等组成；控制器测试调用：

```java
mockMvc.perform(delete("/api/knowledge/documents")
        .param("source", "java/basic.md"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source").value("java/basic.md"))
        .andExpect(jsonPath("$.deleted").value(true));
```

- [x] **Step 3: Run tests and verify RED**

Run:

```shell
cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dtest=MarkdownContentImporterVectorTest,MarkdownImportRegistryTest,MySqlMarkdownImportRegistryTest,QdrantMarkdownChunkIndexerTest,MarkdownDocumentControllerTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test"
```

Expected: compilation failure because the new lifecycle methods and delete response do not exist.

### Task 2: 实现来源级向量生命周期

**Files:**
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownChunkIndexer.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownDocument.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownImportRegistry.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/InMemoryMarkdownImportRegistry.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownContentImporter.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeDocumentVersionMapper.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownImportRegistry.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/vector/QdrantMarkdownChunkIndexer.java`
- Modify: `deploy/mysql/init/001_knowledge_document_version.sql`
- Create: `deploy/mysql/init/002_source_exact_collation.sql`

**Interfaces:**
- Consumes: metadata keys `source` and `document_version` already written by `QdrantMarkdownChunkIndexer.index(...)`.
- Produces: source-level delete operations used by the importer and HTTP layer.

- [x] **Step 1: Extend registry and indexer contracts**

```java
default void deleteOtherVersions(MarkdownDocument document) {
}

default void deleteBySource(String source) {
}
```

`InMemoryMarkdownImportRegistry.unregisterBySource` uses `removeIf`；MySQL Mapper adds `DELETE ... WHERE source = #{source}`.

- [x] **Step 2: Implement structured Qdrant filters**

```java
FilterExpressionBuilder builder = new FilterExpressionBuilder();
vectorStore.delete(builder.and(
        builder.eq("source", document.source()),
        builder.ne("document_version", document.version())
).build());
```

来源删除只使用 `builder.eq("source", source).build()`。

- [x] **Step 3: Update importer ordering and compensation**

在 `index` 成功后调用 `deleteOtherVersions`；两者共享已有 `try/catch` 和本次版本撤销逻辑。新增：

```java
public boolean deleteBySource(String source) {
    String normalizedSource = requireSource(source);
    chunkIndexer.deleteBySource(normalizedSource);
    return registry.unregisterBySource(normalizedSource);
}
```

来源校验与 `MarkdownDocument` 保持相同非空白语义，不增加额外格式限制。

- [x] **Step 4: Run focused tests and verify GREEN**

Run the Task 1 command and expect all selected tests to pass.

### Task 3: 暴露删除接口并同步项目事实

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownDeleteResponse.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownDocumentController.java`
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/superpowers/plans/2026-07-23-markdown-vector-lifecycle.md`

**Interfaces:**
- Consumes: `MarkdownContentImporter.deleteBySource(String)`.
- Produces: `DELETE /api/knowledge/documents?source=...` returning `{ "source": "...", "deleted": true }`.

- [x] **Step 1: Add the HTTP delete response and controller method**

```java
@DeleteMapping
public MarkdownDeleteResponse deleteDocument(@RequestParam String source) {
    return new MarkdownDeleteResponse(source, contentImporter.deleteBySource(source));
}
```

响应 record 在构造时保留经过业务校验的来源，不引入新的统一响应包装。

- [x] **Step 2: Update factual docs**

记录替换顺序、历史登记语义、显式删除接口、跨 MySQL/Qdrant 事务缺口和仍未实现的手动重建。

- [x] **Step 3: Run fresh backend verification**

执行聚焦测试后运行：

```shell
cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp clean verify"
```

Expected: build exit code 0, all backend tests pass, executable JAR generated.

- [x] **Step 4: Verify the real storage lifecycle**

使用唯一测试来源导入版本 A、导入版本 B、删除来源；分别确认 Qdrant 点只保留 B、DELETE 返回成功、MySQL 与 Qdrant 最终无该来源数据。再用大小写变体来源验证删除一个来源不会清理另一个来源的登记或向量。只清理本轮唯一测试来源，不操作其他数据。

- [x] **Step 5: Review and commit**

执行 `git diff --check`、审查 `git diff` 和 `git status --short`，只提交本轮直接相关文件，提交信息为 `feat: 完成 Markdown 向量替换与删除`。
