# MySQL And Qdrant Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立默认无外部依赖、启用 `storage` 后使用独立 MySQL 和 Qdrant 的存储接入基础。

**Architecture:** `MarkdownImportRegistry` 变为稳定接口，默认由内存实现提供，`mysql` profile 由 MyBatis-Plus 实现提供。`storage` profile 组启用 `mysql` 与 `vector`，后者配置 DashScope Embedding、Qdrant 和非阻塞健康检查。

**Tech Stack:** Java 17、Spring Boot 3.5.16、Druid 1.2.14、MyBatis-Plus 3.5.7、MySQL Connector/J 8.3.0、Spring AI 1.0.0、Spring AI Alibaba 1.0.0.2、Qdrant 1.10.1、JUnit 5、Mockito、Docker Compose

## Global Constraints

- 所有写入只允许发生在 `D:\IdeaProjects\java-interview-rag`；`D:\IdeaProjects\blog\blog-springboot` 仅可只读参考。
- 新项目数据库固定为 `java_interview_rag`，Qdrant collection 固定默认为 `java-interview-rag-chunks`。
- 环境变量统一使用 `JAVA_INTERVIEW_RAG_` 前缀，不提交真实密码或 API Key。
- 默认 `local` profile 不依赖 MySQL、Qdrant 或 DashScope。
- 本轮不实现 chunk 向量写入、RAG 问答或模拟面试。
- Maven 必须使用仓库内 `.m2`、`.tmp` 和显式 `maven.repo.local`。

---

### Task 1: 抽取导入登记接口

**Files:**
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownImportRegistry.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/document/InMemoryMarkdownImportRegistry.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownImportRegistryTest.java`
- Modify: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownFileImporterTest.java`

**Interfaces:**
- Produces: `MarkdownImportRegistry.register(MarkdownDocument): boolean`
- Produces: `new InMemoryMarkdownImportRegistry()`

- [ ] **Step 1: Write the failing test**

将登记器测试改为通过接口引用新内存实现：

```java
private final MarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvnw.cmd -Dtest=MarkdownImportRegistryTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

Expected: compilation failure because `InMemoryMarkdownImportRegistry` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
public interface MarkdownImportRegistry {
    boolean register(MarkdownDocument document);
}

public final class InMemoryMarkdownImportRegistry implements MarkdownImportRegistry {
    private final Set<ImportKey> registeredImports = new HashSet<>();

    @Override
    public boolean register(MarkdownDocument document) {
        return registeredImports.add(new ImportKey(document.source(), document.version()));
    }
}
```

更新测试和默认 Spring Bean 构造调用。

- [ ] **Step 4: Run test to verify it passes**

Run the same targeted command.

Expected: `MarkdownImportRegistryTest` passes.

### Task 2: 实现 MySQL 版本登记

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeDocumentVersionEntity.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/persistence/KnowledgeDocumentVersionMapper.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownImportRegistry.java`
- Create: `src/test/java/com/javainterview/rag/knowledge/persistence/MySqlMarkdownImportRegistryTest.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java`

**Interfaces:**
- Consumes: `MarkdownImportRegistry.register(MarkdownDocument): boolean`
- Produces: `MySqlMarkdownImportRegistry(KnowledgeDocumentVersionMapper)`
- Produces: `KnowledgeDocumentVersionMapper extends BaseMapper<KnowledgeDocumentVersionEntity>`

- [ ] **Step 1: Add dependencies needed to compile the persistence test**

添加 Druid、MyBatis-Plus 和 MySQL Connector/J，并保持版本与参考项目一致。

- [ ] **Step 2: Write the failing test**

```java
@Test
void returnsTrueWhenVersionIsInserted() {
    when(mapper.insert(any())).thenReturn(1);
    assertTrue(registry.register(MarkdownDocument.create("resume/backend.md", "Java")));
}

@Test
void returnsFalseOnlyForDuplicateSourceAndVersion() {
    when(mapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));
    assertFalse(registry.register(MarkdownDocument.create("resume/backend.md", "Java")));
}
```

另断言非唯一键数据库异常继续抛出。

- [ ] **Step 3: Run test to verify it fails**

Expected: compilation failure because persistence classes do not exist.

- [ ] **Step 4: Write minimal implementation**

实体映射 `t_knowledge_document_version`；登记器插入 `source`、`version`、`contentLength`、`createdAt`，只捕获 `DuplicateKeyException` 并返回 `false`。

默认 Bean 使用 `@Profile("!mysql")`，MySQL Bean 使用 `@Profile("mysql")`。

- [ ] **Step 5: Run tests to verify they pass**

Run persistence and existing importer tests.

Expected: both implementations preserve the same duplicate result contract.

### Task 3: 配置 Qdrant 与非阻塞健康检查

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/javainterview/rag/knowledge/vector/VectorStoreHealthConfiguration.java`
- Create: `src/test/java/com/javainterview/rag/knowledge/vector/VectorStoreHealthConfigurationTest.java`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/resources/application-local.yml`
- Create: `src/main/resources/application-mysql.yml`
- Create: `src/main/resources/application-vector.yml`

**Interfaces:**
- Consumes: Spring AI `VectorStore`
- Produces: `ApplicationRunner vectorStoreHealthRunner(VectorStore)` under `vector` profile

- [ ] **Step 1: Add Spring AI dependencies**

导入 Spring AI 和 Spring AI Alibaba BOM，添加 DashScope 与 Qdrant starters。

- [ ] **Step 2: Write the failing test**

```java
@Test
void doesNotBlockStartupWhenQdrantCheckFails() {
    when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenThrow(new IllegalStateException("Qdrant unavailable"));
    assertDoesNotThrow(() -> configuration.vectorStoreHealthRunner(vectorStore).run(null));
}
```

- [ ] **Step 3: Run test to verify it fails**

Expected: compilation failure because `VectorStoreHealthConfiguration` does not exist.

- [ ] **Step 4: Write minimal implementation and profile configuration**

健康检查执行 `health-check` 查询，捕获异常并记录中文警告。`application.yml` 声明默认 `local` 和 `storage: mysql,vector`；外部服务配置分别放入 `application-mysql.yml` 和 `application-vector.yml`。

- [ ] **Step 5: Run tests to verify they pass**

Run the health test and Spring Boot context test.

Expected: health test passes and default `local` context starts without external services or secrets.

### Task 4: 提供本地基础设施和使用文档

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `config/application-private.example.yml`
- Create: `deploy/mysql/init/001_knowledge_document_version.sql`
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Produces: MySQL `t_knowledge_document_version` with `UNIQUE(source, version)`
- Produces: Compose services `mysql` and `qdrant`

- [ ] **Step 1: Add Compose, SQL and examples**

Compose 仅包含 MySQL 8.0 与 Qdrant 1.10.1，使用独立 volumes；SQL 创建版本表；示例配置不含真实凭据。

- [ ] **Step 2: Document exact startup boundary**

记录 `docker compose up -d mysql qdrant`、`--spring.profiles.active=storage` 和所需环境变量，并明确当前机器未做真实连通验证。

- [ ] **Step 3: Run focused verification**

Run backend tests and package compilation with the repository-local Maven cache and temp directory.

Expected: zero test failures and successful package build under default `local` profile.

- [ ] **Step 4: Check configuration and diff**

使用结构化 YAML 解析检查配置文件；运行 `git diff --check`、`git status --short` 并审查所有变更只属于本轮任务。

- [ ] **Step 5: Commit verified closure**

```text
feat: 接入 MySQL 与 Qdrant 存储基础
```
