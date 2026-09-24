# 多模型 Provider 控制台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把当前 Chat 与 Embedding 的实际运行配置统一展示为可追踪能力，并允许用户用固定无敏感样本验证结构化输出和向量维度。

**Architecture:** 本轮保留现有启动时 Spring Bean，不做运行中热切换。后端从已解析的配置和 `KnowledgeIndexVersionMetadata` 生成脱敏能力快照，复用现有 `ChatClient` 与 `EmbeddingModel` 做一次性探测；Vue 新增独立控制台读取快照并按能力触发探测。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI、Vue 3、TypeScript、Vue Router、Axios

**Spec:** `docs/09-后续开发路线.md` 阶段 9.3；`AGENTS.md`；`.codex/autonomous-development-prompt.md`

## Global Constraints

- 所有写入只发生在 `D:\IdeaProjects\java-interview-rag`，`blog-springboot` 只读。
- API 不返回 API Key、探测正文、模型原始响应或原始异常体。
- Chat 与 Embedding 探测每次各最多调用一次；失败不自动重试业务动作。
- 当前业务继续使用启动时配置快照，不宣称已经支持动态 Provider 切换。
- 不提前实现 ASR、TTS、对象存储或通用 AI 网关。

---

### Task 1: Provider 能力快照与安全探测

**Files:**
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelCapability.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderOverview.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderTestResult.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderService.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderController.java`
- Test: `src/test/java/com/javainterview/rag/modelprovider/ModelProviderServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/modelprovider/ModelProviderControllerTest.java`

**Interfaces:**
- Produces: `ModelProviderService.overview(): ModelProviderOverview`
- Produces: `ModelProviderService.test(ModelCapability): ModelProviderTestResult`
- Produces: `GET /api/model-providers` and `POST /api/model-providers/{capability}/test`

- [x] **Step 1: Write failing service tests**

Cover two capability snapshots, API Key configured flags, valid structured Chat output, Embedding dimension match, dimension mismatch, and safe error classification without returning exception details.

- [x] **Step 2: Run focused tests and confirm failure**

Run: `mvn -Dmaven.repo.local=.m2 -Dtest=ModelProviderServiceTest,ModelProviderControllerTest test`

Expected: compilation fails because the new types do not exist.

- [x] **Step 3: Implement the minimum service and controller**

`CHAT` uses `chatClient.prompt(fixedPrompt).call().entity(ChatProbe.class)` and requires `status=MODEL_OK`. `EMBEDDING` uses `embeddingModel.embed(fixedText)` and compares the returned array length with `KnowledgeIndexVersionMetadata.embeddingDimensions()`. Safe failure codes are `AUTHENTICATION`、`RATE_LIMITED`、`PROVIDER_ERROR`、`TIMEOUT`、`NETWORK`、`STRUCTURED_OUTPUT_INVALID`、`DIMENSION_MISMATCH`、`UNKNOWN`.

- [x] **Step 4: Run focused tests**

Run: `mvn -Dmaven.repo.local=.m2 -Dtest=ModelProviderServiceTest,ModelProviderControllerTest test`

Expected: all focused tests pass.

### Task 2: Vue Provider 控制台

**Files:**
- Create: `frontend/src/api/modelProvider.ts`
- Create: `frontend/src/composables/useModelProviders.ts`
- Create: `frontend/src/components/model-provider/ModelProviderPanel.vue`
- Create: `frontend/src/views/ModelProviderView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/WorkspaceHeader.vue`

**Interfaces:**
- Consumes: `GET /api/model-providers`
- Consumes: `POST /api/model-providers/{capability}/test`
- Produces: lazy route `/model-providers`

- [x] **Step 1: Add typed API and composable**

Use `shallowRef` for fetched snapshots, loading, per-capability testing state, results and errors. Load once on mount; explicit actions own all mutations.

- [x] **Step 2: Add focused panel and thin route view**

Show capability、provider、model、endpoint origin、Key configured state、current default usages and last probe. One icon button per capability triggers the bounded test and disables only that capability while running.

- [x] **Step 3: Register navigation**

Add the lazy `/model-providers` route and a `Settings2` navigation item; extend `WorkspaceHeader` mode and command entries without changing unrelated routes.

- [x] **Step 4: Typecheck and build**

Run: `npm run build`

Expected: Vue typecheck and Vite build pass.

### Task 3: Runtime acceptance and authoritative docs

**Files:**
- Modify: `docs/04-当前进度.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/09-后续开发路线.md`

**Interfaces:**
- Consumes: tested API/UI behavior and InterviewGuide commit `969e2af`
- Produces: exact local/upstream mapping, current boundary, next 9.3 closure

- [x] **Step 1: Start the storage application with JDK 21**

Use existing private configuration and an available local port; this run used `18098` because other repository instances held the existing sparse index and ports. Do not print secrets.

- [x] **Step 2: Execute at most one Chat and one Embedding probe**

Acceptance: Chat returns valid `MODEL_OK` structured output; Embedding returns the configured dimension. On failure, record the classified result and stop that capability without retrying.

- [x] **Step 3: Verify the Provider page**

Open `/model-providers`, confirm both capability rows render, actions do not shift layout, and no API Key or raw response appears.

- [x] **Step 4: Update docs and inspect the diff**

Record the usable entry, upstream mapping and deliberate difference: current startup snapshot first, dynamic encrypted persistence later. Run `git diff --check`.

- [x] **Step 5: Commit the complete closure**

Run: `git add <directly-related-files>` then `git commit -m "feat: 增加模型 Provider 控制台"`.
