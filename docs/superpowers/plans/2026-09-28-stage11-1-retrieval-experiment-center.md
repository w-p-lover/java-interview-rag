# Stage 11.1 Retrieval Experiment Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Execute natively in the current session; the user explicitly requested direct Stage 11 development. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the existing fixed retrieval evaluations into persistent, resumable experiments that can be compared and explicitly adopted or rejected from the product UI.

**Architecture:** A retrieval experiment service executes one fixed dataset split with one existing strategy (`DENSE`, `SPARSE`, or `HYBRID`), persists each case result, and resumes only unfinished cases. A MySQL run record captures dataset, corpus, index and Git contracts. Completed runs can be compared under the same dataset split, and the candidate stores a human decision against one baseline.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis, MySQL 5.7/8, existing Qdrant/Lucene retrieval services, Vue 3 Composition API, TypeScript, Vite.

**Spec:** `docs/09-后续开发路线.md` section 11.1.

## Global Constraints

- Keep all writes inside `D:\IdeaProjects\java-interview-rag`.
- Reuse the current fixed retrieval dataset and metric calculations; do not fork retrieval logic.
- Do not call Chat in this slice. A real acceptance run may use at most 20 Hybrid query Embeddings and must stop on external failure.
- Preserve the legacy synchronous evaluation endpoints.
- Do not mark all of Stage 11.1 complete; answer, scoring and model migration experiments remain follow-up types.

## Component Map

- `RagRetrievalExperimentService`: owns run contracts, scheduling, per-case recovery, comparison and decisions.
- `RagRetrievalExperimentStore`: persists run facts without knowing retrieval implementations.
- `EvaluationExperimentView`: route composition only.
- `useRetrievalExperiments`: owns API state, polling and actions.
- `RetrievalExperimentPanel`: composes run creation, history and comparison sections.
- `RetrievalExperimentRunList`: renders run facts and emits selection/resume actions.
- `RetrievalExperimentComparison`: renders same-contract metric deltas and emits the human decision.

## Review Focus

- Duplicate start under the same run contract reuses the existing run and does not repeat query Embeddings.
- Restarted or explicitly resumed runs skip successful cases and execute only missing/failed cases.
- Runs from different dataset splits cannot be compared or used for an adoption decision.
- A partial run cannot be selected as a valid comparison result.
- Decision notes are bounded and the server, not the client, verifies the baseline/candidate relationship.

---

### Task 1: Persistent Retrieval Runs

**Files:**
- Create: `deploy/mysql/init/040_rag_retrieval_experiment.sql`
- Create: `src/main/java/com/javainterview/rag/knowledge/experiment/*`
- Create: `src/main/java/com/javainterview/rag/knowledge/persistence/*RetrievalExperiment*`
- Modify: `FixedRagEvaluationDataset`, `RagRetrievalEvaluationService`, `RagRetrievalEvaluationConfiguration`

- [x] Define deterministic `DEV/HOLDOUT` membership and stable dataset version.
- [x] Persist run contracts, case results, reports, errors and human decision fields.
- [x] Execute one case at a time through the existing metric evaluator and save progress after every case.
- [x] Recover `QUEUED/RUNNING` runs only when their current dataset/corpus/index/Git contract still matches.
- [x] Add focused tests for idempotent start, partial resume and contract drift.

### Task 2: Comparison and HTTP API

**Files:**
- Create: `RagRetrievalExperimentController.java`
- Create: request/response records under `knowledge/web`
- Test: `RagRetrievalExperimentControllerTest.java`

- [x] Add start, list, detail and resume endpoints.
- [x] Compare two successful runs with the same dataset version and split.
- [x] Return metric deltas and a conservative recommendation without auto-adopting.
- [x] Save `ADOPTED` or `REJECTED` with a bounded human note on the candidate.

### Task 3: Evaluation Workbench

**Files:**
- Create: `frontend/src/api/evaluationExperiment.ts`
- Create: `frontend/src/composables/useRetrievalExperiments.ts`
- Create: `frontend/src/components/evaluation/*.vue`
- Create: `frontend/src/views/EvaluationExperimentView.vue`
- Modify: `frontend/src/router/index.ts`, `frontend/src/components/WorkspaceHeader.vue`

- [x] Add `/evaluations` to the existing workspace navigation.
- [x] Start runs by strategy and split, poll active runs, and expose explicit resume.
- [x] Select baseline and candidate, display exact deltas, and save adoption/rejection.
- [x] Keep the route view thin and preserve mobile navigation and overflow boundaries.

### Task 4: Real Storage Acceptance and Facts

**Files:**
- Modify: `docs/04-当前进度.md`, `docs/06-参考项目调研.md`, `docs/09-后续开发路线.md`

- [x] Execute migration `040` against the development MySQL and verify columns/indexes.
- [x] Run one `SPARSE/DEV` baseline and one `HYBRID/DEV` candidate with no Chat and at most 20 query Embeddings.
- [x] Compare the two runs, save the evidence-based adopt/reject decision, and verify reload from MySQL.
- [x] Run focused backend tests, `npm run build`, browser desktop/mobile checks and `git diff --check`.
- [x] Commit only Stage 11.1 retrieval experiment files.
