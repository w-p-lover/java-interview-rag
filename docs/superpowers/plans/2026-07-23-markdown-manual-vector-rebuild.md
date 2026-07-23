# Markdown 手动向量重建实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 提供只针对已登记 Markdown 版本的同步向量重建入口，使缺失、旧版本和同版本残留向量能够通过重传文件幂等收敛。

**Architecture:** 在现有登记器增加精确查询，在导入器增加不改变登记的重建编排，并由 Qdrant 索引器覆盖默认重建方法完成“先写预期点、再删其他版本、最后删同版本多余点”。Web 层新增独立 multipart 接口和最小响应，不改变普通导入与删除契约。

**Tech Stack:** Java 17、Spring Boot 3.5.16、Spring MVC、MyBatis-Plus 3.5.7、Spring AI 1.0.0、Qdrant、JUnit 5、Mockito、Maven Wrapper。

## Global Constraints

- 所有项目写入仅发生在 `D:\IdeaProjects\java-interview-rag`；`D:\IdeaProjects\blog\blog-springboot` 只读。
- 新增或修改的代码注释和用户可见错误默认使用简体中文。
- 不增加 Maven 依赖、数据库字段、新表、异步任务、批量重建、前端入口或 RAG 检索。
- 来源保持大小写敏感、无首尾空白的精确字符串语义。
- 只允许已登记的精确“来源 + 内容版本”重建；重建不得创建或删除登记。
- 使用仓库内 `.m2\repository` 与 `.tmp`，通过 `cmd.exe /d /c` 调用 Maven Wrapper。
- 先运行聚焦测试，最终只执行一次与本轮收口相关的 `clean verify`。

---

## 文件结构

- `MarkdownImportRegistry.java`：定义已登记版本的精确查询契约。
- `InMemoryMarkdownImportRegistry.java`：从现有联合键集合查询登记。
- `KnowledgeDocumentVersionMapper.java`：使用精确来源和版本计数。
- `MySqlMarkdownImportRegistry.java`：将 Mapper 计数映射为布尔值。
- `MarkdownRebuildResult.java`：保存重建文档和不可修改 chunk 列表。
- `MarkdownContentImporter.java`：校验登记、解析、切分并调用重建索引。
- `MarkdownChunkIndexer.java`：提供保持函数式接口兼容的默认重建方法。
- `QdrantMarkdownChunkIndexer.java`：实现 Qdrant 点的精确收敛顺序。
- `MarkdownRebuildResponse.java`：将领域结果映射为最小 HTTP 响应。
- `MarkdownDocumentController.java`：暴露 multipart 重建接口。
- 现有四个测试类：只增加对应层的直接行为验证，不新建重复测试基类。

### Task 1: 已登记版本查询与重建编排

**Files:**
- Modify: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownImportRegistryTest.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownImportRegistryTest.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownContentImporterVectorTest.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownImportRegistry.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/InMemoryMarkdownImportRegistry.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeDocumentVersionMapper.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownImportRegistry.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownRebuildResult.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownContentImporter.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownChunkIndexer.java`

**Interfaces:**
- Consumes: `MarkdownDocument.create(String source, String content)`、现有标题解析和 chunk 切分。
- Produces: `boolean MarkdownImportRegistry.isRegistered(MarkdownDocument document)`、`void MarkdownChunkIndexer.rebuild(MarkdownDocument document, List<MarkdownChunk> chunks)`、`MarkdownRebuildResult MarkdownContentImporter.rebuildContent(String source, String content)`。

- [x] **Step 1: 写登记查询和重建编排的失败测试**

在内存登记测试中加入精确查询：

```java
@Test
void findsOnlyTheRegisteredSourceAndVersion() {
    MarkdownDocument registered = MarkdownDocument.create("java/basic.md", "已登记");
    registry.register(registered);

    assertTrue(registry.isRegistered(registered));
    assertFalse(registry.isRegistered(MarkdownDocument.create("JAVA/basic.md", "已登记")));
    assertFalse(registry.isRegistered(MarkdownDocument.create("java/basic.md", "新内容")));
}
```

在 MySQL 登记测试中加入计数和精确 SQL 验证：

```java
@Test
void findsRegisteredSourceAndVersion() {
    MarkdownDocument document = MarkdownDocument.create("resume/backend.md", "Java 后端");
    when(mapper.countBySourceAndVersion(document.source(), document.version())).thenReturn(1);

    assertTrue(registry.isRegistered(document));

    verify(mapper).countBySourceAndVersion(document.source(), document.version());
}
```

并在 Mapper 注解测试中断言：

```java
assertEquals("SELECT COUNT(*) FROM t_knowledge_document_version "
        + "WHERE BINARY source = BINARY #{source} AND version = #{version}",
        registeredSelect.value()[0]);
```

在导入器测试中加入三个直接行为：

```java
@Test
void rebuildsARegisteredHistoricalVersionWithoutChangingRegistration() {
    InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
    MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
    MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
    MarkdownImportResult original = sourceImporter.importContent("java/rebuild.md", "# Java\n原内容");
    sourceImporter.importContent("java/rebuild.md", "# Java\n新内容");

    MarkdownRebuildResult rebuilt = sourceImporter.rebuildContent("java/rebuild.md", "# Java\n原内容");

    assertEquals(original.document().source(), rebuilt.document().source());
    assertEquals(original.document().content(), rebuilt.document().content());
    assertEquals(original.document().version(), rebuilt.document().version());
    assertEquals(original.chunks(), rebuilt.chunks());
    assertTrue(registry.isRegistered(original.document()));
    verify(indexer).rebuild(rebuilt.document(), rebuilt.chunks());
}

@Test
void rejectsRebuildForAnUnregisteredVersionBeforeIndexing() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> importer.rebuildContent("java/missing.md", "# Java\n未导入"));

    assertEquals("文档版本尚未导入，不能重建", exception.getMessage());
    verify(chunkIndexer, never()).rebuild(any(), any());
}

@Test
void keepsRegistrationWhenRebuildFailsSoTheVersionCanRetry() {
    InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
    MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
    MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
    MarkdownImportResult imported = sourceImporter.importContent("java/retry-rebuild.md", "# Java\n重试");
    doThrow(new IllegalStateException("Qdrant unavailable"))
            .doNothing()
            .when(indexer).rebuild(any(MarkdownDocument.class), any());

    assertThrows(IllegalStateException.class,
            () -> sourceImporter.rebuildContent("java/retry-rebuild.md", "# Java\n重试"));
    MarkdownRebuildResult retried = sourceImporter.rebuildContent(
            "java/retry-rebuild.md", "# Java\n重试");

    assertEquals(imported.document().source(), retried.document().source());
    assertEquals(imported.document().version(), retried.document().version());
    assertTrue(registry.isRegistered(imported.document()));
    verify(indexer, times(2)).rebuild(any(MarkdownDocument.class), any());
}
```

- [x] **Step 2: 运行聚焦测试并确认 RED**

Run:

```text
cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=MarkdownImportRegistryTest,MySqlMarkdownImportRegistryTest,MarkdownContentImporterVectorTest test"
```

Expected: 编译失败，明确缺少 `isRegistered`、`countBySourceAndVersion`、`rebuildContent`、`rebuild` 和 `MarkdownRebuildResult`，不是测试语法或环境失败。

- [x] **Step 3: 实现最小登记查询契约**

接口与内存实现：

```java
boolean isRegistered(MarkdownDocument document);

@Override
public boolean isRegistered(MarkdownDocument document) {
    return registeredImports.contains(new ImportKey(document.source(), document.version()));
}
```

Mapper 与 MySQL 实现：

```java
@Select("SELECT COUNT(*) FROM t_knowledge_document_version "
        + "WHERE BINARY source = BINARY #{source} AND version = #{version}")
int countBySourceAndVersion(@Param("source") String source, @Param("version") String version);

@Override
public boolean isRegistered(MarkdownDocument document) {
    Objects.requireNonNull(document, "Markdown 文档不能为空");
    return mapper.countBySourceAndVersion(document.source(), document.version()) > 0;
}
```

- [x] **Step 4: 实现最小重建结果和编排**

`MarkdownRebuildResult`：

```java
public record MarkdownRebuildResult(MarkdownDocument document, List<MarkdownChunk> chunks) {

    public MarkdownRebuildResult {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        chunks = List.copyOf(chunks);
    }
}
```

索引器默认方法：

```java
default void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks) {
    index(document, chunks);
    deleteOtherVersions(document);
}
```

导入器方法：

```java
public MarkdownRebuildResult rebuildContent(String source, String content) {
    MarkdownDocument document = MarkdownDocument.create(source, content);
    if (!registry.isRegistered(document)) {
        throw new IllegalArgumentException("文档版本尚未导入，不能重建");
    }

    List<MarkdownSection> sections = headingParser.parse(document);
    List<MarkdownChunk> chunks = sectionChunker.chunk(sections, maxChunkLength);
    chunkIndexer.rebuild(document, chunks);
    return new MarkdownRebuildResult(document, chunks);
}
```

- [x] **Step 5: 运行聚焦测试并确认 GREEN**

Run: 与 Step 2 相同命令。

Expected: 三个测试类全部通过，0 failures、0 errors。

### Task 2: Qdrant 重建精确收敛

**Files:**
- Modify: `src/test/java/com/javainterview/rag/knowledge/vector/QdrantMarkdownChunkIndexerTest.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/vector/QdrantMarkdownChunkIndexer.java`

**Interfaces:**
- Consumes: Task 1 的 `MarkdownChunkIndexer.rebuild(...)`，现有稳定 ID 和元数据映射。
- Produces: 非空列表按顺序收敛版本与 chunk 范围；空列表删除整个来源。

- [x] **Step 1: 写 Qdrant 收敛顺序的失败测试**

```java
@Test
@SuppressWarnings({"rawtypes", "unchecked"})
void rebuildsExpectedChunksBeforeRemovingOtherVersionsAndStaleChunkOrders() {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantMarkdownChunkIndexer indexer = new QdrantMarkdownChunkIndexer(vectorStore);
    MarkdownDocument document = MarkdownDocument.create("java/rebuild.md", "# Java\n集合\n\n并发");
    List<MarkdownChunk> chunks = List.of(
            new MarkdownChunk(document.source(), 0, 0, 1, List.of("Java"), "集合"),
            new MarkdownChunk(document.source(), 1, 0, 1, List.of("Java"), "并发")
    );

    indexer.rebuild(document, chunks);

    var orderedCalls = inOrder(vectorStore);
    orderedCalls.verify(vectorStore).add(anyList());
    ArgumentCaptor<Filter.Expression> filters = ArgumentCaptor.forClass(Filter.Expression.class);
    orderedCalls.verify(vectorStore, times(2)).delete(filters.capture());
    FilterExpressionBuilder builder = new FilterExpressionBuilder();
    assertEquals(builder.and(
            builder.eq("source", document.source()),
            builder.ne("document_version", document.version())
    ).build(), filters.getAllValues().get(0));
    assertEquals(builder.and(
            builder.and(
                    builder.eq("source", document.source()),
                    builder.eq("document_version", document.version())
            ),
            builder.gte("chunk_order", chunks.size())
    ).build(), filters.getAllValues().get(1));
}

@Test
void rebuildsAnEmptyDocumentByDeletingTheWholeSource() {
    VectorStore vectorStore = mock(VectorStore.class);
    QdrantMarkdownChunkIndexer indexer = new QdrantMarkdownChunkIndexer(vectorStore);
    MarkdownDocument document = MarkdownDocument.create("java/empty.md", "");

    indexer.rebuild(document, List.of());

    verify(vectorStore, never()).add(anyList());
    ArgumentCaptor<Filter.Expression> filter = ArgumentCaptor.forClass(Filter.Expression.class);
    verify(vectorStore).delete(filter.capture());
    FilterExpressionBuilder builder = new FilterExpressionBuilder();
    assertEquals(builder.eq("source", document.source()).build(), filter.getValue());
}
```

- [x] **Step 2: 运行 Qdrant 测试并确认 RED**

Run:

```text
cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=QdrantMarkdownChunkIndexerTest test"
```

Expected: 默认 `rebuild` 缺少同版本残留删除，空列表也不会删除整个来源，因此断言失败。

- [x] **Step 3: 覆盖 Qdrant 重建实现**

```java
@Override
public void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks) {
    Objects.requireNonNull(document, "Markdown 文档不能为空");
    Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
    if (chunks.isEmpty()) {
        deleteBySource(document.source());
        return;
    }

    index(document, chunks);
    deleteOtherVersions(document);
    FilterExpressionBuilder builder = new FilterExpressionBuilder();
    vectorStore.delete(builder.and(
            builder.and(
                    builder.eq("source", document.source()),
                    builder.eq("document_version", document.version())
            ),
            builder.gte("chunk_order", chunks.size())
    ).build());
}
```

- [x] **Step 4: 运行 Qdrant 测试并确认 GREEN**

Run: 与 Step 2 相同命令。

Expected: `QdrantMarkdownChunkIndexerTest` 全部通过，0 failures、0 errors。

### Task 3: multipart 重建 API

**Files:**
- Modify: `src/test/java/com/javainterview/rag/knowledge/web/MarkdownDocumentControllerTest.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownRebuildResponse.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownDocumentController.java`

**Interfaces:**
- Consumes: Task 1 的 `MarkdownContentImporter.rebuildContent(...)` 与 `MarkdownRebuildResult`。
- Produces: `POST /api/knowledge/documents/rebuild`，响应 `MarkdownRebuildResponse(String source, String version, int chunkCount)`。

- [x] **Step 1: 写控制器端到端失败测试**

```java
@Test
void rebuildsARegisteredMultipartMarkdownVersion() throws Exception {
    MockMultipartFile file = markdownFile("# Java\n重建内容");
    String source = "java/web-rebuild.md";
    mockMvc.perform(multipart("/api/knowledge/documents/import")
                    .file(file)
                    .param("source", source))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(true));

    mockMvc.perform(multipart("/api/knowledge/documents/rebuild")
                    .file(file)
                    .param("source", source))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.source").value(source))
            .andExpect(jsonPath("$.version").isNotEmpty())
            .andExpect(jsonPath("$.chunkCount").value(1));
}

@Test
void rejectsRebuildForAnUnregisteredMultipartMarkdownVersion() throws Exception {
    mockMvc.perform(multipart("/api/knowledge/documents/rebuild")
                    .file(markdownFile("# Java\n未导入"))
                    .param("source", "java/web-missing.md"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("文档版本尚未导入，不能重建"));
}
```

- [x] **Step 2: 运行 Web 测试并确认 RED**

Run:

```text
cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=MarkdownDocumentControllerTest test"
```

Expected: `/rebuild` 没有映射，返回 404。

- [x] **Step 3: 实现最小响应和控制器方法**

```java
public record MarkdownRebuildResponse(String source, String version, int chunkCount) {

    public static MarkdownRebuildResponse from(MarkdownRebuildResult result) {
        return new MarkdownRebuildResponse(result.document().source(), result.document().version(),
                result.chunks().size());
    }
}
```

```java
@PostMapping(value = "/rebuild", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public MarkdownRebuildResponse rebuildDocument(@RequestParam MultipartFile file,
                                               @RequestParam String source) throws IOException {
    String content = new String(file.getBytes(), StandardCharsets.UTF_8);
    return MarkdownRebuildResponse.from(contentImporter.rebuildContent(source, content));
}
```

- [x] **Step 4: 运行直接相关测试并确认 GREEN**

Run:

```text
cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Dtest=MarkdownImportRegistryTest,MySqlMarkdownImportRegistryTest,MarkdownContentImporterVectorTest,QdrantMarkdownChunkIndexerTest,MarkdownDocumentControllerTest test"
```

Expected: 本轮五个直接相关测试类全部通过，0 failures、0 errors。

### Task 4: 真实恢复验证、文档收口与提交

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/superpowers/plans/2026-07-23-markdown-manual-vector-rebuild.md`

**Interfaces:**
- Consumes: 已运行的服务器 MySQL、DashScope Embedding、Qdrant 和本轮 `/rebuild` API。
- Produces: 可重复的真实修复证据、ADR-016、阶段 2 验收状态和本轮单一功能提交。

- [x] **Step 1: 使用唯一来源执行真实 `storage` 修复**

使用形如 `codex/rebuild-20260723-<唯一后缀>.md` 的来源：普通导入后记录版本和点数；只删 Qdrant 点而保留 MySQL 登记；确认普通导入返回 `imported=false` 且点未恢复；写入同版本 `chunk_order=99` 的残留点；调用 `/rebuild` 后确认预期点恢复、`chunk_order=99` 消失且 MySQL 登记仍为 1。

验证仅操作该唯一来源，不清空 collection 或版本表。完成后调用现有 DELETE 接口，并确认该来源的 MySQL 行数与 Qdrant 点数均为 0。

- [x] **Step 2: 同步项目事实文档**

更新内容必须明确：

- 重建要求重传精确来源与 Markdown 文件，数据库不保存正文。
- 只允许已登记版本，失败不撤销登记。
- Qdrant 通过稳定 ID、其他版本删除和同版本高 `chunk_order` 删除实现收敛。
- 阶段 2 在真实恢复验证通过后完成，下一候选是阶段 3 最小检索与真实来源返回。
- 新增 ADR-016，记录采用重传文件而不是扩表保存正文的理由和回看条件。

- [x] **Step 3: 请求只读代码审查并处理结论**

审查范围从设计提交 `26fbdc2` 的父提交到当前工作树，重点核对：未登记版本是否可能写向量、重建失败是否误删登记、空文档与历史版本语义、Qdrant 过滤条件和来源精确语义。Critical 和 Important 问题在最终验证前修复；Minor 只在直接影响本轮正确性时处理。

- [x] **Step 4: 停止本项目后端并执行最终 fresh verification**

只停止监听 `18080` 且命令行属于 `java-interview-rag` 的 Java 进程，保留 `5173` 前端和其他用户进程。

Run:

```text
cmd.exe /d /c "set JAVA_HOME=C:\Users\IT074\.jdks\ms-21.0.10&& set MAVEN_USER_HOME=D:\IdeaProjects\java-interview-rag\.m2&& set TMP=D:\IdeaProjects\java-interview-rag\.tmp&& set TEMP=D:\IdeaProjects\java-interview-rag\.tmp&& mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository clean verify"
```

Expected: BUILD SUCCESS，全部测试 0 failures、0 errors，生成 `target/java-interview-rag-0.0.1-SNAPSHOT.jar`。

- [x] **Step 5: 检查差异并提交本轮功能**

```text
git diff --check
git status --short
git diff --stat
```

确认只包含本轮计划、代码、测试和项目事实文档后提交：

```text
git add README.md docs src
git commit -m "feat: 完成 Markdown 手动向量重建"
```

- [x] **Step 6: 恢复已验证的 `storage` 后端并更新自动化记忆**

使用刚生成的 JAR、`storage` profile 和隐藏窗口重新监听 `127.0.0.1:18080`；确认端口、HTTP 状态和启动日志无错误。向 `C:\Users\IT074\.codex\automations\java-interview-rag\memory.md` 追加本轮事实、测试数量、真实验证结果、提交、运行 PID、未 push 状态与当前运行时间。

---

## 计划自审

- 规格覆盖：登记查询、未登记拒绝、历史版本恢复、失败保留登记、Qdrant 非空与空文档收敛、multipart API、真实修复、文档和阶段收口均有对应任务。
- 占位符检查：无 `TBD`、`TODO`、模糊错误处理或引用未定义类型；真实验证的唯一后缀在执行时生成，属于防止数据碰撞的运行值，不是缺失设计。
- 类型一致性：`isRegistered(MarkdownDocument)`、`rebuild(MarkdownDocument, List<MarkdownChunk>)`、`rebuildContent(String, String)`、`MarkdownRebuildResult` 和 `MarkdownRebuildResponse` 在各任务中名称与参数一致。
- 范围检查：不保存正文、不改表、不做前端、不引入状态机或检索，能够作为阶段 2 单独验收闭环。
