# File Object Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让知识资料和用户明确选择长期保存的简历原文件通过稳定 `fileId` 持久化、下载并跨重启解析，知识异步任务不再依赖请求内正文。

**Architecture:** 在现有业务领域下增加独立文件对象端口：MySQL 保存文件事实，local/S3 适配器保存字节，业务表只保存 `file_id`。知识任务提交后按 `fileId` 读取并解析，应用重启时以新 `attemptId` 有上限恢复；简历继续使用显式保存开关，不改变默认隐私边界。

**Tech Stack:** Java 17、Spring Boot 3.5、MyBatis、MySQL 5.7、AWS SDK v2 S3、Vue 3、TypeScript

**Spec:** `docs/09-后续开发路线.md` 的 9.1 与 9.4

## Global Constraints

- 所有项目写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅可读取连接配置，不得修改。
- 上游固定对照 `Snailclimb/interview-guide@969e2af8550f680688f5b50f0d895186a661a319`，只学习对象键、S3 配置、上传/下载/删除与桶检查语义，不复制 AGPL 源码。
- 知识资料原文件默认保存；简历只有用户显式选择时才保存原文件和解析正文。
- 单文件仍限制 10 MiB；不新增 OCR、DOCX、病毒扫描、用户体系或生产清理任务。
- S3 不可用时不静默回退 local；local 是显式默认实现，S3 是显式配置实现。

## Review Focus

- 同一用途同一 SHA-256 并发上传只产生一个文件事实，且对象缺失时不能把元数据误报为可下载。
- 应用在知识任务 `RUNNING` 时重启后必须更换 attempt，旧 attempt 不能写终态，恢复次数超过 1 次后明确失败。
- 不保存正文的简历版本不得产生 `fileId`；后续对同一 Hash 显式保存时只能补充存储事实，不能制造第二个内容版本。
- 删除知识来源时只能清理已无业务引用的原文件；解析中删除和仍被简历/版本引用的文件必须返回冲突。
- local 对象键必须阻止目录穿越；S3 桶不可用、对象缺失和下载失败必须保留稳定错误边界。

---

### Task 1: File Object Core

**Files:**
- Create: `src/main/java/com/javainterview/rag/file/FileAsset.java`
- Create: `src/main/java/com/javainterview/rag/file/FileAssetService.java`
- Create: `src/main/java/com/javainterview/rag/file/FileAssetStore.java`
- Create: `src/main/java/com/javainterview/rag/file/FileObjectStorage.java`
- Create: `src/main/java/com/javainterview/rag/file/LocalFileObjectStorage.java`
- Create: `src/main/java/com/javainterview/rag/file/S3FileObjectStorage.java`
- Create: `src/main/java/com/javainterview/rag/file/FileStorageConfiguration.java`
- Create: `src/main/java/com/javainterview/rag/file/FileAssetController.java`
- Create: `src/main/java/com/javainterview/rag/file/persistence/FileAssetMapper.java`
- Create: `src/main/java/com/javainterview/rag/file/persistence/MySqlFileAssetStore.java`
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-storage.yml`
- Test: `src/test/java/com/javainterview/rag/file/FileAssetServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/file/LocalFileObjectStorageTest.java`

**Interfaces:**
- Produces: `FileAssetService.store(FilePurpose, String, String, byte[]) -> FileAsset`
- Produces: `FileAssetService.read(String) -> StoredFileContent`
- Produces: `FileAssetService.deleteIfUnreferenced(String) -> FileAsset`
- Produces: `GET /api/files/{fileId}/content` and `GET /api/files/{fileId}/consistency`

- [x] **Step 1: Write focused tests for deterministic de-duplication, missing objects, reference conflicts and local path safety**

```java
assertThat(service.store(FilePurpose.KNOWLEDGE, "a.md", "text/markdown", bytes).id())
        .isEqualTo(service.store(FilePurpose.KNOWLEDGE, "copy.md", "text/markdown", bytes).id());
assertThatThrownBy(() -> service.deleteIfUnreferenced(referenced.id()))
        .isInstanceOf(FileAssetConflictException.class);
assertThatThrownBy(() -> local.read("../outside"))
        .isInstanceOf(IllegalArgumentException.class);
```

- [ ] **Step 2: Run only the two new tests and confirm they fail before implementation**（未单独执行预期失败；按项目“避免无必要测试”规则直接实现后验证）

Run: `mvnw.cmd -Dtest=FileAssetServiceTest,LocalFileObjectStorageTest test`

- [x] **Step 3: Implement the metadata state machine and local/S3 byte adapters**

```java
public enum FileAssetStatus { UPLOADING, READY, FAILED, DELETE_PENDING, DELETED }
public interface FileObjectStorage {
    void put(String objectKey, byte[] content, String contentType);
    byte[] read(String objectKey);
    boolean exists(String objectKey);
    void delete(String objectKey);
}
```

Object keys use `knowledge|resume/<first-two-hash>/<sha256>`; metadata is created before byte upload so failed or half-finished writes remain identifiable. The S3 bean is selected only by `java-interview-rag.files.backend=s3`; local remains the explicit default.

- [x] **Step 4: Add download, consistency and safe soft-delete HTTP boundaries**

Use `ContentDisposition.attachment().filename(name, UTF_8)` for downloads. Return 409 when references exist, 404 for missing/deleted metadata, and 503 when READY metadata points to a missing object.

- [x] **Step 5: Run the two focused tests**

Expected: both pass; no application-wide suite is required yet.

### Task 2: Knowledge File-ID Tasks and Restart Recovery

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/document/KnowledgeStoredFileParser.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/PdfKnowledgeDocumentExtractor.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownContentImporter.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownImportRegistry.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/document/KnowledgeDocumentSummary.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/task/KnowledgeDocumentTask.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/task/KnowledgeDocumentTaskStore.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/task/KnowledgeDocumentTaskService.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/persistence/*KnowledgeDocument*.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownDocumentController.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/MarkdownImportConfiguration.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/KnowledgeDocumentVersionItem.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/KnowledgeDocumentTaskResponse.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/task/KnowledgeDocumentTaskServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/task/InMemoryKnowledgeDocumentTaskStoreTest.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/web/KnowledgeDocumentTaskControllerTest.java`

**Interfaces:**
- Consumes: `FileAssetService.store/read/deleteIfUnreferenced`
- Produces: `KnowledgeDocumentTaskService.submit(operation, source, fileId)`
- Produces: `KnowledgeDocumentTaskService.resumePendingTasks()`
- Produces: knowledge version/task responses with nullable `fileId`

- [x] **Step 1: Extend task-store tests with file ID, recoverable query and attempt replacement**

```java
KnowledgeDocumentTask running = store.create("task", "old", "a.md", "file-1", IMPORT);
store.markRunning("task", "old");
assertThat(store.requeueAfterRestart("task", "old", "new", 1)).isTrue();
assertThat(store.markSucceeded("task", "old", "v", true, 1, "MARKDOWN", 0)).isFalse();
```

- [x] **Step 2: Parse stored bytes inside the worker and attach file IDs to imported versions**

`KnowledgeStoredFileParser.load(source, fileId)` validates extension and UTF-8/PDF content after the task is claimed. `MarkdownContentImporter` receives nullable `fileId`; successful import registers it, successful rebuild attaches it to the current version.

- [x] **Step 3: Replace request-body task input with persisted file references**

The controller validates filename and size, stores the upload, and submits `{operation, source, fileId}`. Task responses never include bytes or parsed text.

- [x] **Step 4: Recover pending tasks at startup**

`QUEUED` tasks with a file ID are resubmitted. `RUNNING` tasks atomically move to `QUEUED` with a new UUID attempt and `recovery_count + 1`; a second restart marks `RECOVERY_EXHAUSTED`. Legacy rows without `file_id` become `APPLICATION_RESTARTED_NO_FILE`.

- [x] **Step 5: Run task, controller and importer-focused tests**

Run: `mvnw.cmd -Dtest=KnowledgeDocumentTaskServiceTest,InMemoryKnowledgeDocumentTaskStoreTest,KnowledgeDocumentTaskControllerTest,MarkdownContentImporterVectorTest test`

### Task 3: Resume Explicit Original Storage and Existing Vue Entrypoints

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/resume/ResumeVersion.java`
- Modify: `src/main/java/com/javainterview/rag/interview/resume/ResumeCenterStore.java`
- Modify: `src/main/java/com/javainterview/rag/interview/resume/ResumeCenterService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/ResumeVersionEntity.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/ResumeCenterMapper.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/MySqlResumeCenterStore.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/ResumeCenterConfiguration.java`
- Modify: `frontend/src/api/resumeCenter.ts`
- Modify: `frontend/src/api/knowledge.ts`
- Modify: `frontend/src/components/resume/ResumeUploadForm.vue`
- Modify: `frontend/src/components/resume/ResumeVersionHistory.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeDocumentDetailPanel.vue`
- Test: `src/test/java/com/javainterview/rag/interview/resume/ResumeCenterServiceTest.java`

**Interfaces:**
- Consumes: file object core and nullable business `fileId`
- Produces: resume versions with nullable `fileId`; explicit saved originals download through the shared file endpoint

- [x] **Step 1: Add a service test proving the privacy default and explicit persistence path**

```java
ResumeVersion metadataOnly = service.save(file, null, "简历", "Java", false).detail().versions().get(0);
assertThat(metadataOnly.fileId()).isNull();
ResumeVersion persisted = service.save(otherFile, metadataOnly.resumeId(), "简历", "Java", true)
        .detail().versions().get(0);
assertThat(persisted.fileId()).isNotBlank();
```

- [x] **Step 2: Persist nullable `fileId` and support same-hash explicit enrichment**

Saving with `saveFullText=false` preserves existing metadata-only behavior. Saving with `true` stores the original and parsed text; if the same hash already exists without a file, update only storage fields on that immutable content version.

- [x] **Step 3: Expose download affordances in the existing components**

Component map: `ResumeCenterView` remains orchestration only; `ResumeUploadForm` owns the explicit privacy choice; `ResumeVersionHistory` renders a download icon for versions with `fileId`; `KnowledgeDocumentDetailPanel` renders a download icon for versions with `fileId`. Data continues props-down/events-up; no new store or route is introduced.

- [x] **Step 4: Run the resume service test and frontend build**

Run: `mvnw.cmd -Dtest=ResumeCenterServiceTest test`

Run: `npm run build` in `frontend`

### Task 4: Migration, Real Restart Validation and Factual Docs

**Files:**
- Create: `deploy/mysql/init/039_file_object_storage.sql`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/09-后续开发路线.md`
- Modify: `docs/08-项目技术栈与功能总结.md`
- Modify: `C:\Users\IT074\.codex\automations\java-interview-rag\memory.md` after verification only

**Interfaces:**
- Consumes: all prior tasks
- Produces: MySQL 5.7 schema, a verified local-object storage run and current project facts

- [ ] **Step 1: Apply migration 039 to the development database with Python `pymysql`**

Create `t_file_asset`; add nullable indexed `file_id` to knowledge versions, knowledge tasks and resume versions; add `recovery_count` to knowledge tasks. Query `information_schema` before each DDL and verify columns/indexes afterward.

- [ ] **Step 2: Package and start a new storage instance with an isolated port, sparse index and local object root**

Use JDK 21 only as the build/runtime environment while preserving Java 17 release. Keep object bytes under the repository `.tmp` directory and do not touch existing running instances.

- [ ] **Step 3: Run one real no-model flow through restart**

Upload a small UTF-8 knowledge file, capture `fileId`, download and compare SHA-256, create a controlled recoverable task, restart the application, verify a new attempt completes, and confirm the old attempt cannot update the terminal row. Upload one resume with explicit persistence and one metadata-only resume, verifying only the first is downloadable. Clean only validation rows and files created by this run.

- [ ] **Step 4: Update factual docs and the automation memory**

Record the upstream object-storage mapping, local differences, 8.3/9.1 comparison facts relevant to this implementation, exact SQL result, actual validation and remaining S3-live limitation. Do not claim real S3 validation if only local storage ran.

- [ ] **Step 5: Inspect diff, run `git diff --check`, commit directly related files, and confirm a clean worktree**

Commit message: `feat: 接入可恢复文件对象存储`
