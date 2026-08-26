# 简历证据审计工作流 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将简历证据审计从后端质量基线推进为可在模拟面试准备页实际使用、并由真实评估约束的完整功能。

**Architecture:** 后端继续使用单次 Spring AI Structured Output 和 Java 逐字证据验真，增加项目职责语义边界、长简历事实容量策略和按事实类型的脱敏评估指标。真实 v5 基线通过准确率、虚构率、证据合法率和职责召回门槛后，前端通过独立 composable 调用现有审计接口，由专用组件展示事实、缺失、风险和建议；审计结果只保存在页面内存中。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI、JUnit 5、Vue 3.5、TypeScript、Axios、Lucide Vue、SCSS、Playwright。

**Spec:** `docs/09-后续开发路线.md`、`docs/04-当前进度.md`、`docs/02-技术方案.md`

## Global Constraints

- 所有写入只发生在 `D:\IdeaProjects\java-interview-rag`，`blog-springboot` 只读。
- 新增或修改的代码注释、日志、异常和用户可见文字默认使用简体中文。
- 不持久化简历正文、审计事实、引文、风险或建议；现有元数据日志边界不变。
- 不增加第二次模型调用、图框架、通用 Agent Runtime、PII 抽取、OCR 或新中间件。
- 真实评估最多执行两轮，每轮 9 次 Chat；必须先通过本地测试和脱敏报告校验。
- 前端接入门槛：事实准确率 `1.0000`、证据合法率 `1.0000`、虚构率 `0`、职责事实召回率不低于 `0.9000`。
- 保留用户当前未提交的资料库前端、配置、README、AGENTS 和路线文档改动，不纳入本轮提交。

---

### Task 1: v5 固定数据集与 Prompt 边界

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/resume/FixedResumeEvidenceAuditDataset.java`
- Modify: `src/main/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditService.java`
- Modify: `src/test/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditServiceTest.java`

**Interfaces:**
- Consumes: `ResumeEvidenceAuditResult.FactType`
- Produces: 九类完整原子标签样本和 `resume-evidence-audit-v3` Prompt

- [x] **Step 1: 增加项目职责和长简历容量样本**

新增 `project-responsibility`，明确区分项目名、技术、动作职责和量化结果；新增 `multi-section-capacity`，覆盖教育、任职、三个项目及接近 30 条上限的原子事实。样本只使用合成组织和项目名称。

- [x] **Step 2: 收紧事实类型语义**

在 System Prompt 中增加以下约束：

```text
PROJECT 只表示项目或系统名称；TECHNOLOGY 只表示技术、框架、工具及其明确使用事实；
RESPONSIBILITY 表示负责、设计、实现、优化、重构等个人动作；RESULT 表示可核验结果。
同一句同时包含技术和动作时可以拆成 TECHNOLOGY 与 RESPONSIBILITY 两条事实。
facts 最多 30 条；超量时先保留每个项目的名称、至少一条职责和量化结果，再保留任职/教育、核心技术和时间。
```

- [x] **Step 3: 更新服务测试**

断言 Prompt 版本为 `resume-evidence-audit-v3`，并校验职责边界、30 条上限和保留优先级文字已进入 system prompt。

- [x] **Step 4: 运行服务测试**

Run: `mvnw.cmd -Dtest=ResumeEvidenceAuditServiceTest test`

Expected: 所有服务测试通过，不调用真实模型。

### Task 2: 类型级指标与容量诊断

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditDraft.java`
- Modify: `src/main/java/com/javainterview/rag/interview/resume/FixedResumeEvidenceAuditEvaluationService.java`
- Modify: `src/test/java/com/javainterview/rag/interview/resume/FixedResumeEvidenceAuditEvaluationServiceTest.java`
- Modify: `src/test/java/com/javainterview/rag/interview/web/ResumeEvidenceAuditEvaluationControllerTest.java`

**Interfaces:**
- Produces: `Report.factTypeMetrics()` 和容量字段
- Consumes: 九类固定样本及每例确认事实计数

- [x] **Step 1: 暴露包内事实容量常量**

将 `ResumeEvidenceAuditDraft.MAX_FACT_COUNT` 保持为 `30` 并允许同包评估服务读取，不公开为 Web 配置项。

- [x] **Step 2: 增加类型级聚合**

新增记录：

```java
public record FactTypeMetric(
        ResumeEvidenceAuditResult.FactType type,
        int expectedFactCount,
        int matchedExpectedFactCount,
        int actualConfirmedFactCount,
        int unmatchedConfirmedFactCount,
        double recallRate,
        double accuracyRate) {
}
```

`Report` 增加七种事实类型的固定顺序列表；`Metrics` 增加 `factCapacityLimit`、`maxCandidateFactCount` 和 `capacityReachedCaseCount`。

- [x] **Step 3: 更新确定性测试和 HTTP 脱敏契约**

断言九个样本、类型级计数、容量字段和报告无正文；非法证据失败仍计入聚合分母。

- [x] **Step 4: 运行评估与控制器测试**

Run: `mvnw.cmd -Dtest=FixedResumeEvidenceAuditEvaluationServiceTest,ResumeEvidenceAuditEvaluationControllerTest test`

Expected: 类型指标和容量诊断通过，报告中不存在简历或引文正文。

### Task 3: 真实基线校准与接入门槛

**Files:**
- Modify: `evaluation/run_resume_audit_baseline.py`
- Create: `evaluation/results/2026-08-26-resume-audit-baseline-v5.json`
- Create: `evaluation/results/2026-08-26-resume-audit-baseline-v6.json`
- Temporary: `src/test/java/com/javainterview/rag/knowledge/web/ResumeEvidenceAuditLiveBaselineTest.java`

**Interfaces:**
- Consumes: 正式 `DashScopeCompatibleChatConfiguration`、`ResumeEvidenceAuditService` 和固定评估服务
- Produces: 九类脱敏真实基线报告

- [x] **Step 1: 更新运行器离线校验**

要求当前正式数据集、9 个 case、7 个 `factTypeMetrics`、容量上限 30，并继续拒绝 `resumeText`、`confirmedFacts`、`evidence`、`risks`、`suggestions` 字段。

- [x] **Step 2: 运行全部直接相关后端测试**

Run: `mvnw.cmd -Dtest=ResumeEvidenceAuditServiceTest,FixedResumeEvidenceAuditEvaluationServiceTest,ResumeEvidenceAuditControllerTest,ResumeEvidenceAuditEvaluationControllerTest test`

- [x] **Step 3: 使用一次性测试入口执行授权 Chat 基线**

临时测试从环境读取 DashScope Key，使用内存日志，不加载 MySQL/Qdrant。第一轮报告用于验证类型边界；若类型级计数证明固定标签遗漏，则补齐原子标签并升级数据集，第二轮报告作为正式接入依据。完成后删除临时测试源码。

- [x] **Step 4: 检查接入门槛**

要求事实准确率 `1.0000`、证据合法率 `1.0000`、虚构率 `0`、`RESPONSIBILITY` 召回率不低于 `0.9000`。若第一轮失败，只允许根据类型计数修复可由固定原文直接证明的标签遗漏，或做一次窄 Prompt 修订后重跑；仍失败则停止前端接入。

### Task 4: 前端 API、状态和结果面板

**Files:**
- Modify: `frontend/src/api/interview.ts`
- Create: `frontend/src/composables/useResumeEvidenceAudit.ts`
- Create: `frontend/src/components/interview/ResumeEvidenceAuditPanel.vue`
- Modify: `frontend/src/components/interview/InterviewSetupForm.vue`

**Interfaces:**
- Produces: `auditResumeEvidence(resumeText)`、`useResumeEvidenceAudit()`
- Consumes: `POST /api/interviews/resume-audits`

- [x] **Step 1: 增加类型化 API**

```ts
export type ResumeEvidenceFactType =
  | "EDUCATION" | "EMPLOYMENT" | "PROJECT" | "TECHNOLOGY"
  | "RESPONSIBILITY" | "TIME" | "RESULT";

export interface ResumeEvidenceAuditResult {
  requestId: string;
  role: "RESUME_EVIDENCE_AUDITOR";
  promptVersion: string;
  confirmedFacts: ResumeEvidenceConfirmedFact[];
  missingInformation: string[];
  risks: string[];
  suggestions: string[];
}
```

- [x] **Step 2: 实现 composable**

使用最小 `shallowRef` 状态管理 `result/loading/error/auditedResumeText`；`audit()` 归一化输入并调用 API；`isStale` 根据当前简历文本派生，不保存 localStorage。

- [x] **Step 3: 实现展示组件**

组件只接收结果、加载、错误、是否过期和是否可执行，向上发出 `audit`。事实按七种类型分组；缺失、风险和建议分栏展示；加载使用固定骨架，错误支持重试；不渲染 HTML，不展示内部角色名。

- [x] **Step 4: 接入准备表单**

在简历文件字段后嵌入审计面板。审计期间禁用文件更换和开始面试；完成后可继续编辑，编辑后显示“结果已过期”并允许重新检查。审计是可选步骤，失败不阻止直接开始面试。

- [x] **Step 5: 运行 Vue 类型检查和生产构建**

Run: `npm run build`

Expected: `vue-tsc` 与 Vite 构建通过。

### Task 5: 浏览器验证、事实文档与提交

**Files:**
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Append: `docs/05-开发记录.md`
- Append: `docs/06-参考项目调研.md`
- Append: `docs/07-技术决策记录.md`
- Modify: `docs/08-项目技术栈与功能总结.md`

**Interfaces:**
- Consumes: 已通过接入门槛的 v6 正式报告和前端实现
- Produces: 可核对的项目事实、风险边界和提交

- [x] **Step 1: 启动前端并隔离 API 状态**

使用不冲突端口启动 Vite；浏览器通过路由 Mock 隔离空态、加载、成功和失败响应，不启动数据库或向量服务，也不产生额外 Chat 调用。

- [x] **Step 2: Playwright 验证**

桌面与 375px 验证空态、加载、成功、失败、结果过期、重新检查和开始面试按钮状态；检查无横向溢出、无控制台错误、键盘焦点可见。

- [x] **Step 3: 更新事实文档**

记录 v5 指标、前端工作流、参考项目采纳判断、未持久化边界和真实模型/数据库未验证范围。只有真实基线通过时才将前端能力写成已完成。

- [x] **Step 4: 精确暂存与提交**

检查 `git diff --check` 和暂存快照，只纳入本轮文件；提交信息使用：

```text
feat: 接入简历证据审计工作流
```
