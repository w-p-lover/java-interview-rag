# Resume Evidence Auditor Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 Spring AI 面试链路上增加简历证据审计官后端基线，确保确认事实具有原简历逐字证据，并且请求日志不保存简历或模型正文。

**Architecture:** 独立 `resume` 领域服务使用 Spring AI Structured Output 生成事实、缺失、风险和建议。Java 在返回前隔离强提示注入行、逐条校验事实引文，并通过独立 MySQL 日志记录角色、版本、token、耗时和状态；Web 层只暴露审计和最近脱敏日志两个接口。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI 1.0.0、MyBatis-Plus、MySQL 5.7、JUnit 5、Mockito。

## Global Constraints

- 所有项目写入只允许发生在 `D:\IdeaProjects\java-interview-rag`；`D:\IdeaProjects\blog\blog-springboot` 仅可只读参考。
- 新增或修改的代码注释、日志、异常和用户可见提示默认使用简体中文。
- 第一版使用普通 Spring AI 编排，不引入图框架、Tool Calling、MCP、多 Agent 或新中间件。
- 简历和模型输出不持久化；日志不包含简历、完整 Prompt、事实、风险或建议正文。
- 不修改用户当前未提交的 `README.md`、`docs/09-后续开发路线.md`、`docs/10-面试准备与项目掌握要求.md`、前端和本机配置。
- 不执行真实 Chat、Embedding、Qdrant、MySQL 迁移或生产操作。

---

### Task 1: 审计结构与证据边界

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditDraft.java`
- Create: `src/main/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditResult.java`
- Create: `src/main/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditService.java`
- Test: `src/test/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditServiceTest.java`

**Interfaces:**
- Consumes: `ChatClient` and raw `resumeText` up to 50,000 characters.
- Produces: `ResumeEvidenceAuditResult audit(String resumeText)` with `requestId`, role, Prompt version, confirmed facts, missing information, risks, and suggestions.
- Produces: each confirmed fact has `FactType` in `PROJECT/TECHNOLOGY/RESPONSIBILITY/TIME/RESULT`, statement, and exact evidence.

- [ ] **Step 1: Write failing structure and evidence tests**

Use `BeanOutputConverter<ResumeEvidenceAuditDraft>` to prove Spring AI can deserialize the intended JSON. Mock `ChatClient.responseEntity(ResumeEvidenceAuditDraft.class)` and assert that valid evidence is returned unchanged while an invented quote throws `IllegalStateException("模型返回的简历事实证据不在原始简历中")`.

- [ ] **Step 2: Run the service test and verify failure**

Run: `mvnw.cmd -Dmaven.repo.local=.m2 -Dtest=ResumeEvidenceAuditServiceTest test`

Expected: FAIL because the audit types and service do not exist.

- [ ] **Step 3: Implement the minimal structured model**

`ResumeEvidenceAuditDraft` owns model-only fact drafts and list-size/text validation. `ResumeEvidenceAuditResult` owns verified facts and copies all output lists. Do not add persistence or frontend types to these records.

- [ ] **Step 4: Implement prompt-injection isolation and exact evidence verification**

Use separate system/user ChatClient messages. Remove lines matching strong instruction patterns such as `忽略之前规则` or forged `resume_data` delimiters before model input. Validate every returned quote against the remaining original lines; append only a count-based Chinese risk when lines were isolated.

- [ ] **Step 5: Run the focused service tests**

Run: `mvnw.cmd -Dmaven.repo.local=.m2 -Dtest=ResumeEvidenceAuditServiceTest test`

Expected: PASS for valid facts, invalid evidence, blank input, empty model response, injection isolation, and log-store failure behavior.

### Task 2: Privacy-safe request observability

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditRequestLog.java`
- Create: `src/main/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditRequestLogStore.java`
- Create: `src/main/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditRequestLogQueryService.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/ResumeEvidenceAuditRequestLogEntity.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/ResumeEvidenceAuditRequestLogMapper.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/MySqlResumeEvidenceAuditRequestLogStore.java`
- Create: `deploy/mysql/init/015_resume_evidence_audit_request_log.sql`
- Test: `src/test/java/com/javainterview/rag/interview/persistence/MySqlResumeEvidenceAuditRequestLogStoreTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditRequestLogQueryServiceTest.java`

**Interfaces:**
- Produces: log fields `requestId`, `roleName`, `promptVersion`, `modelName`, status, optional token triplet, elapsed milliseconds, optional error type, and creation time.
- Produces: `List<ResumeEvidenceAuditRequestLog> findRecent(int limit)` with `limit` from 1 to 100.

- [ ] **Step 1: Write failing log-domain and mapper tests**

Assert all token fields are simultaneously present or absent, successful logs have no error type, failed logs require an error type, mapper entities expose no resume/Prompt/result-body fields, and recent-query order is delegated to the mapper.

- [ ] **Step 2: Add migration and persistence mapping**

Create `t_resume_evidence_audit_request_log` with a unique UUID, role/Prompt/model versions, status, nullable token fields, elapsed time, error type, and timestamp. Add created/status indexes only; do not store request or response JSON.

- [ ] **Step 3: Record model usage without changing audit outcome on log failure**

Use Spring AI `ResponseEntity<ChatResponse, ResumeEvidenceAuditDraft>` so one model response supplies both the structured entity and token metadata. Record success after Java evidence validation; record the exception class on failure; warn and preserve the original audit outcome if the log store itself fails.

- [ ] **Step 4: Run log and service tests**

Run: `mvnw.cmd -Dmaven.repo.local=.m2 -Dtest=ResumeEvidenceAuditServiceTest,MySqlResumeEvidenceAuditRequestLogStoreTest,ResumeEvidenceAuditRequestLogQueryServiceTest test`

Expected: PASS without database or model access.

### Task 3: HTTP contract and six fixed samples

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/web/ResumeEvidenceAuditRequest.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/ResumeEvidenceAuditController.java`
- Create: `src/main/java/com/javainterview/rag/interview/web/ResumeEvidenceAuditConfiguration.java`
- Create: `src/test/java/com/javainterview/rag/interview/web/ResumeEvidenceAuditControllerTest.java`
- Modify: `src/test/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditServiceTest.java`

**Interfaces:**
- Produces: `POST /api/interviews/resume-audits` with `{ "resumeText": "..." }`.
- Produces: `GET /api/interviews/resume-audits/requests?limit=20` returning metadata-only logs.

- [ ] **Step 1: Add six synthetic fixed samples**

Cover complete resume, missing quantified result, conflicting dates, skill stacking, vague project, and embedded prompt injection. Use only synthetic text in tests; do not save a real resume.

- [ ] **Step 2: Add HTTP contract tests**

Assert the POST response keeps facts/missing/risks/suggestions separate, invalid input returns HTTP 400, invalid model evidence returns HTTP 502, and the request-log endpoint returns no resume or model-body property.

- [ ] **Step 3: Wire storage-profile beans**

Inject the existing `ragAnswerChatClient`, the configured Chat model name, the new mapper-backed store, service, and query service. Do not change current interview preparation or session beans.

- [ ] **Step 4: Run all direct tests**

Run: `mvnw.cmd -Dmaven.repo.local=.m2 -Dtest=ResumeEvidenceAuditServiceTest,ResumeEvidenceAuditRequestLogQueryServiceTest,MySqlResumeEvidenceAuditRequestLogStoreTest,ResumeEvidenceAuditControllerTest test`

Expected: PASS without external services.

### Task 4: Facts, decisions, and commit

**Files:**
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/08-项目技术栈与功能总结.md`

**Interfaces:**
- Documents: implemented API, exact evidence boundary, injection isolation limit, privacy fields, migration state, test result, and lack of a real-model baseline.

- [ ] **Step 1: Update only clean authoritative docs**

Record the baseline as implemented and deterministically tested, not as a real-model quality result. Keep `docs/09` and `docs/10` untouched because they are user-owned changes.

- [ ] **Step 2: Run final targeted verification**

Run the four direct test classes with Microsoft JDK 21 and repository-local `.m2`/`.tmp`, then run `git diff --check` on task files.

- [ ] **Step 3: Inspect the staged snapshot**

Stage only files named in this plan. Verify `git diff --cached --name-status`, `git diff --cached --check`, and that user-owned frontend/docs/config changes remain unstaged.

- [ ] **Step 4: Commit the closure**

Run: `git commit -m "feat: 增加简历证据审计基线"`

Expected: one commit on `codex/bootstrap-governance`; no push, migration execution, or paid model call.
