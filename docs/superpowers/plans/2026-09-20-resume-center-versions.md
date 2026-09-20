# Resume Center Versions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立可上传、Hash 去重、保存多版本、查询历史并切换当前版本的简历中心首版。

**Architecture:** 参考 `Snailclimb/interview-guide@969e2af8550f680688f5b50f0d895186a661a319` 的简历实体、Hash 去重、列表/详情流程，本地拆分为不可变 `ResumeVersion` 与持有当前版本指针的 `ResumeRecord`。复用现有 PDF 文本提取器，MySQL 保存版本元数据；完整正文只有在上传时显式选择后才持久化，不提前接入阶段 9 的对象存储或异步任务系统。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus、MySQL 5.7、Vue 3、TypeScript、JUnit 5、Mockito

**Spec:** `docs/09-后续开发路线.md` 阶段 8.4 与 `docs/04-当前进度.md` 当前固定队列第 1 项

## Global Constraints

- 所有项目代码和文档写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅用于只读获取已授权数据库连接信息。
- 文件上限为 10 MiB，首版支持 UTF-8 Markdown/TXT 和带文本层 PDF。
- 内容 Hash 全局去重；重复上传返回已有记录和版本，不创建第二份业务事实。
- 版本内容不可修改；切换当前版本只更新 `ResumeRecord.currentVersionId`。
- 完整正文默认不保存，必须由用户显式选择；未保存正文的版本仍保留 Hash、文件和版本元数据。
- 不新增模型调用、对象存储、Redis、用户体系或异步分析任务。

---

### Task 1: Resume Center Domain And Storage

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/resume/ResumeRecord.java`
- Create: `src/main/java/com/javainterview/rag/interview/resume/ResumeVersion.java`
- Create: `src/main/java/com/javainterview/rag/interview/resume/ResumeCenterStore.java`
- Create: `src/main/java/com/javainterview/rag/interview/resume/ResumeCenterService.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/ResumeRecordEntity.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/ResumeVersionEntity.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/ResumeCenterMapper.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/MySqlResumeCenterStore.java`
- Create: `deploy/mysql/init/032_resume_center.sql`
- Test: `src/test/java/com/javainterview/rag/interview/resume/ResumeCenterServiceTest.java`

**Interfaces:**
- Consumes: `MultipartFile` 与现有 `PdfKnowledgeDocumentExtractor`。
- Produces: `ResumeCenterService.save(file, resumeId, name, targetRole, saveFullText)`、`list()`、`detail(id)`、`switchCurrent(recordId, versionId, expectedVersion)`。

- [x] **Step 1: 写失败测试**

```java
ResumeCenterService.SaveResult saved = service.save(file, null,
        "Java 后端简历", "Java 后端工程师", true);
assertEquals(1, saved.detail().versions().size());
assertFalse(saved.duplicate());
```

同时覆盖相同文件返回原版本、追加新版本、未显式保存时正文为空、切换其他记录的版本被拒绝。

- [x] **Step 2: 运行失败测试**

Run: `.\mvnw.cmd "-Dmaven.repo.local=.m2" "-Dtest=ResumeCenterServiceTest" test`
Expected: FAIL，因为领域和服务尚不存在。

- [x] **Step 3: 实现不可变版本与事务存储**

```java
public record ResumeRecord(String id, String name, String targetRole,
                           String currentVersionId, long version,
                           LocalDateTime createdAt, LocalDateTime updatedAt) { }

public record ResumeVersion(String id, String resumeId, int versionNumber,
                            String contentHash, String originalFilename,
                            String contentType, long fileSize, String fullText,
                            LocalDateTime createdAt) { }
```

MySQL 使用 `uk_resume_version_hash` 做全局内容去重，`uk_resume_version_number` 保证同一记录版本号唯一；追加版本和更新当前版本指针在同一事务完成。

- [x] **Step 4: 运行服务测试**

Run: `.\mvnw.cmd "-Dmaven.repo.local=.m2" "-Dtest=ResumeCenterServiceTest" test`
Expected: PASS。

### Task 2: Storage API

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/web/ResumeCenterConfiguration.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/ResumeCenterController.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/ResumeCenterControllerTest.java`

**Interfaces:**
- Consumes: Task 1 的 `ResumeCenterService`。
- Produces: `GET /api/resumes`、`GET /api/resumes/{id}`、`POST /api/resumes/versions` multipart、`PATCH /api/resumes/{id}/current-version`。

- [x] **Step 1: 写控制器契约测试**

```java
mockMvc.perform(multipart("/api/resumes/versions")
        .file(file).param("name", "Java 后端简历")
        .param("targetRole", "Java 后端工程师")
        .param("saveFullText", "true"))
        .andExpect(status().isOk());
```

- [x] **Step 2: 实现 API 和错误边界**

无效文件/输入返回 400，不存在返回 404，版本冲突返回 409；返回对象包含 `duplicate`、记录元数据和按版本号倒序的版本历史。

- [x] **Step 3: 运行控制器测试**

Run: `.\mvnw.cmd "-Dmaven.repo.local=.m2" "-Dtest=ResumeCenterControllerTest,ResumeCenterServiceTest" test`
Expected: PASS。

### Task 3: Resume Center Frontend

**Files:**
- Create: `frontend/src/api/resumeCenter.ts`
- Create: `frontend/src/composables/useResumeCenter.ts`
- Create: `frontend/src/views/ResumeCenterView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/WorkspaceHeader.vue`

**Interfaces:**
- Consumes: Task 2 的四个 HTTP 接口。
- Produces: `/resumes` 工作台，支持选择记录、上传新版本、查看版本历史和切换当前版本。

- [x] **Step 1: 建立类型化 API 与单一状态源**

```ts
export interface ResumeRecordDetail {
  id: string;
  name: string;
  targetRole: string;
  currentVersionId: string | null;
  version: number;
  versions: ResumeVersion[];
}
```

- [x] **Step 2: 实现工作台交互**

上传表单支持新建记录或为当前记录追加版本，完整正文复选框默认关闭；重复上传显示已有版本，切换按钮携带记录乐观锁版本。

- [x] **Step 3: 接入路由与导航**

新增 `/resumes` 路由和“简历中心”导航，命令面板提供上传、版本历史和当前版本三个页面内定位命令。

- [x] **Step 4: 构建验证**

Run: `npm run build`（工作目录 `frontend`）
Expected: `vue-tsc --noEmit` 与 Vite 构建成功。

### Task 4: Database And Facts Closure

**Files:**
- Modify: `docs/04-当前进度.md`
- Modify: `docs/06-参考项目调研.md`

**Interfaces:**
- Consumes: 032 迁移、后端/前端验证结果与上游 commit。
- Produces: 可追溯的执行事实、上游映射和下一项代码问题。

- [x] **Step 1: 执行 032 迁移**

在目标 MySQL 5.7.36 的 `java_interview_rag` 库执行 `032_resume_center.sql`，随后核对两张表、唯一索引和空表状态。

- [x] **Step 2: 运行定向验证**

Run: `.\mvnw.cmd "-Dmaven.repo.local=.m2" "-Dtest=ResumeCenterServiceTest,ResumeCenterControllerTest" test`
Expected: PASS。

Run: `git diff --check`
Expected: 无输出。

- [x] **Step 3: 更新事实文档**

`docs/04` 记录版本中心已实现、032 已执行和下一项“把审计/岗位/深挖/面试绑定到 `resumeVersionId`”；`docs/06` 记录上游实体/Hash/历史/删除语义与本地多版本适配差异。

- [x] **Step 4: 提交闭环**

```bash
git add deploy/mysql/init/032_resume_center.sql docs/04-当前进度.md docs/06-参考项目调研.md docs/superpowers/plans/2026-09-20-resume-center-versions.md frontend/src src/main/java/com/javainterview/rag/interview src/test/java/com/javainterview/rag/interview
git commit -m "feat: 建立简历版本中心"
```
