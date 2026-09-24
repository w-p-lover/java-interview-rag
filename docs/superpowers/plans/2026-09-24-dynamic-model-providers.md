# 动态模型 Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让用户保存、验证并切换 Chat/Embedding Provider，且真实业务调用、索引契约和历史模型快照使用同一份版本化配置事实。

**Architecture:** MySQL 保存 Provider 与单例默认值，API Key 使用 AES-256-GCM 加密；运行时 Registry 解密并按 `providerId + version` 缓存 OpenAI-compatible Chat/Embedding 客户端。路由模型在每次调用时读取当前默认值并持有读锁，默认切换持写锁且在存在排队/运行长任务时拒绝，避免进行中调用或任务半途换模型。Embedding 默认值只允许选择与当前索引模型名和维度完全一致的配置。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI 1.1.8、MyBatis-Plus、MySQL 5.7、AES/GCM、Vue 3、TypeScript

**Spec:** `docs/09-后续开发路线.md` 阶段 9.3；`AGENTS.md`；`.codex/autonomous-development-prompt.md`

## Global Constraints

- 所有项目写入只发生在 `D:\IdeaProjects\java-interview-rag`；`blog-springboot` 只读。
- 不返回、记录或提交 API Key、明文探测响应、私有配置和原始异常体。
- 默认切换必须改变真实业务使用的 Chat/Embedding 客户端，不建立只影响页面的第二事实源。
- 排队或运行中的资料任务、题库任务和付费评估存在时拒绝切换默认 Provider。
- Embedding 默认值必须与当前 `KnowledgeIndexVersionMetadata` 的模型名和维度完全一致。
- 不实现 ASR、TTS、对象存储、路由策略或通用 AI 网关。
- 真实验收最多 3 次 Chat、1 次 Embedding；失败不重复调用寻找更好结果。

## Review Focus

- 更新时 API Key 留空必须保留旧密文，不能保存空 Key。
- 禁用或删除当前默认 Provider 必须拒绝，不能让运行时失去默认客户端。
- 默认版本冲突必须返回 409，不能静默覆盖另一窗口的选择。
- 有 `QUEUED/PROCESSING/RUNNING` 长任务时默认切换必须返回 409。
- Embedding 模型名或维度不匹配当前索引契约时必须拒绝设为默认。

---

### Task 1: 加密 Provider 存储与动态路由

**Files:**
- Create: `deploy/mysql/init/038_model_provider.sql`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProvider.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderDefaults.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderStore.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ProviderApiKeyCipher.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/MySqlModelProviderStore.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderMapper.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderRegistry.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/RoutingChatModel.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/RoutingEmbeddingModel.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderConfiguration.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/web/DashScopeCompatibleChatConfiguration.java`
- Modify: `src/main/resources/application-storage.yml`
- Modify: `.env.example`
- Test: `src/test/java/com/javainterview/rag/modelprovider/ProviderApiKeyCipherTest.java`
- Test: `src/test/java/com/javainterview/rag/modelprovider/ModelProviderRegistryTest.java`
- Test: `src/test/java/com/javainterview/rag/modelprovider/MySqlModelProviderStoreTest.java`

**Interfaces:**
- Produces: `ModelProviderRegistry.currentChatContract()` and routing `ChatModel`/`EmbeddingModel` beans.
- Produces: optimistic `save/update/delete/updateDefaults` store operations.

- [x] Write tests for AES-GCM nonce uniqueness/round trip/wrong-key failure, default routing, read-lock switch serialization, optimistic conflicts, blank-key retention, default deletion protection and Embedding contract mismatch.
- [x] Run the focused tests and confirm missing types fail compilation.
- [x] Implement migration, domain/store/cipher/registry/routing beans and legacy config bootstrap.
- [x] Run the focused tests until all pass.

### Task 2: Provider management API and model history contracts

**Files:**
- Modify: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderService.java`
- Modify: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderController.java`
- Modify: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderOverview.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderRequest.java`
- Create: `src/main/java/com/javainterview/rag/modelprovider/ModelProviderDefaultsRequest.java`
- Modify: eight interview service/configuration classes that currently inject a fixed model name.
- Test: `src/test/java/com/javainterview/rag/modelprovider/ModelProviderServiceTest.java`
- Test: `src/test/java/com/javainterview/rag/modelprovider/ModelProviderControllerTest.java`
- Test: direct model snapshot tests for question-bank tasks, interview configuration and request logs.

**Interfaces:**
- Produces: CRUD and provider-specific test endpoints under `/api/model-providers/providers`.
- Produces: `PUT /api/model-providers/defaults` with optimistic `version`.
- Consumes: `ModelProviderRegistry::currentChatContract`, formatted as `provider/model#version`.

- [x] Add failing tests for create/update/list/delete/default conflicts, active-task blocking, safe masking, provider-specific tests and dynamic model contract snapshots.
- [x] Implement API/service operations and preserve existing current-default test endpoints.
- [x] Replace configuration-time fixed model strings with a supplier while keeping string constructors for existing tests.
- [x] Run the focused management and affected interview tests.

### Task 3: Vue Provider management workbench

**Files:**
- Modify: `frontend/src/api/modelProvider.ts`
- Modify: `frontend/src/composables/useModelProviders.ts`
- Modify: `frontend/src/components/model-provider/ModelProviderPanel.vue`
- Create: `frontend/src/components/model-provider/ModelProviderForm.vue`
- Create: `frontend/src/components/model-provider/ModelProviderList.vue`
- Create: `frontend/src/components/model-provider/ModelProviderDefaults.vue`

**Interfaces:**
- Consumes: Provider CRUD, provider-specific tests and optimistic default update APIs.
- Produces: explicit add/edit/test/enable/default/delete commands; form never receives stored API Key.

- [x] Extend typed API and composable with immutable fetched state and explicit actions.
- [x] Build focused form, list and default selectors with props-down/events-up contracts.
- [x] Preserve last successful data on action errors and refresh after successful mutations.
- [x] Run `npm run build` and correct only directly related type/layout issues.

### Task 4: SQL and bounded runtime acceptance

**Files:**
- Modify: `docs/04-当前进度.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/08-项目技术栈与功能总结.md`
- Modify: `docs/09-后续开发路线.md`

**Interfaces:**
- Consumes: migration `038`, runtime APIs and fixed acceptance samples.
- Produces: verified development schema, two isolated Chat Provider results, compatible Embedding result and authoritative phase boundary.

- [x] Generate a local ignored encryption key without printing it, execute `038` against the development MySQL 5.7 database and verify tables/indexes.
- [x] Bootstrap the current Provider, create an alternate Chat Provider using the existing authorized credential, and run each fixed Chat test once.
- [ ] Switch Chat default, execute one bounded business/model contract check, restore the original default, and run one Embedding test; stopped after the alternate Chat probe returned `AUTHENTICATION`, as required by the fixed failure policy.
- [x] Verify desktop/mobile UI, secret absence, optimistic conflict and incompatible Embedding rejection without extra paid calls.
- [x] Update docs, run focused tests/build/`git diff --check`, commit directly related files, and leave the worktree clean.
