# 可核验面试评分证据 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让正式面试和复习作答的结构化评分携带候选人回答原文引证与知识库原文引证，并由 Java 服务端在持久化前确定性验证每条引文真实存在。

**Architecture:** 模型只返回回答引文和“1-based 来源序号 + 来源引文”；`InterviewAnswerEvaluationService` 对当前回答及对应 `MarkdownSearchResult.content` 做空白归一化后的字面包含校验，再把知识证据映射成完整来源元数据。`InterviewAnswerEvaluation` 保存已验证证据并兼容没有新字段的旧会话 JSON；前端使用一个无状态证据组件同时展示正式面试反馈和复习 attempt 证据。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring AI 1.0、Jackson、JUnit 5、Mockito、Vue 3 Composition API、TypeScript、Vite 8。

## Global Constraints

- 所有项目写入只允许发生在 `D:\IdeaProjects\java-interview-rag`；`D:\IdeaProjects\blog\blog-springboot` 只读。
- 保护现有前端视觉、`docs/05-开发记录.md`、`docs/06-参考项目调研.md` 和 `src/main/resources/application.yml` 的用户改动。
- 不新增数据库表、第三方依赖、用户体系、动态岗位评分维度或付费模型调用。
- 新增或修改的注释、异常和用户可见提示使用简体中文。
- 只执行评分、JSON 兼容、前端构建和差异检查等直接相关验证。

---

### Task 1: 固定证据输出与验真契约

**Files:**
- Modify: `src/test/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationServiceTest.java`
- Create: `src/test/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationTest.java`

**Interfaces:**
- Consumes: 当前回答、题目已有 `knowledgeSources`、模型评价草稿。
- Produces: `answerEvidence: List<String>` 与 `knowledgeEvidence: List<InterviewKnowledgeEvidence>` 的成功、伪造拒绝和旧 JSON 兼容断言。

- [ ] **Step 1: 扩展结构化输出测试**

```json
{
  "answerEvidence":["使用稳定 ID"],
  "knowledgeEvidence":[{"sourceIndex":1,"quote":"新版本成功后再清理旧版本"}]
}
```

断言 Spring AI `BeanOutputConverter` 能转换 1-based 来源序号和原文引证。

- [ ] **Step 2: 增加合法证据映射断言**

```java
assertEquals(List.of("先写入新版本"), result.evaluation().answerEvidence());
assertEquals("rag/vector-lifecycle.md",
        result.evaluation().knowledgeEvidence().get(0).source());
```

同时断言 Prompt 明确要求回答证据只能引用当前回答，知识证据只能引用编号资料原文。

- [ ] **Step 3: 增加伪造证据拒绝断言**

分别让模型返回当前回答中不存在的引文、越界来源序号、来源正文中不存在的引文；断言抛出中文 `IllegalStateException`，且不会进入追问检索。

- [ ] **Step 4: 增加旧 JSON 兼容与新 JSON 往返断言**

用 Jackson 读取没有两个证据字段的旧 `InterviewAnswerEvaluation` JSON，断言恢复为空列表；对带知识来源元数据的新对象执行序列化/反序列化，断言完全一致。

- [ ] **Step 5: 运行 RED 验证**

```shell
.\mvnw.cmd -Dtest=InterviewAnswerEvaluationServiceTest,InterviewAnswerEvaluationTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test
```

预期：因草稿、领域对象和服务尚无证据字段而编译失败。

### Task 2: 实现服务端证据模型与确定性验真

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewKnowledgeEvidence.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationDraft.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluation.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationService.java`

**Interfaces:**
- Consumes: `InterviewAnswerEvaluationDraft.answerEvidence()` 与 `KnowledgeEvidenceDraft(sourceIndex, quote)`。
- Produces: 最多各 3 条、已验证且去重的回答/知识证据；Prompt 版本 `interview-answer-evaluation-v3`，rubric 版本 `interview-answer-rubric-v2`。

- [ ] **Step 1: 建立完整知识证据领域模型**

```java
public record InterviewKnowledgeEvidence(
        String source,
        String documentVersion,
        int chunkOrder,
        List<String> headingPath,
        Double similarity,
        String quote) {}
```

构造器校验来源、版本、顺序、标题路径和引文，复制不可变集合。

- [ ] **Step 2: 扩展模型草稿 Schema**

增加 `List<String> answerEvidence` 和嵌套 `KnowledgeEvidenceDraft(int sourceIndex, String quote)`；两类证据都限制最多 3 条并拒绝空字符串，旧 Java 构造器保留为委托空证据的兼容入口。

- [ ] **Step 3: 扩展持久化评价对象**

`InterviewAnswerEvaluation` 增加两个证据列表；旧 8 参数构造器继续委托空证据，Jackson 对缺失字段的 `null` 转为空列表，避免已持久化会话恢复失败。

- [ ] **Step 4: 在服务端验证模型引文**

```java
if (!normalize(answer).contains(normalize(quote))) {
    throw new IllegalStateException("模型返回的回答证据不在当前回答中");
}
```

知识证据先校验 `sourceIndex` 在 `1..knowledgeSources.size()`，再校验引文属于对应来源正文，最后映射完整来源元数据。当前回答至少需要 1 条证据；题目有可靠资料时至少需要 1 条知识证据，无可靠资料时知识证据必须为空。

- [ ] **Step 5: 升级 Prompt/rubric 版本并重跑测试**

使用 Task 1 的 Maven 命令，预期全部通过，不调用真实模型或存储。

### Task 3: 在正式反馈和复习历史展示证据

**Files:**
- Modify: `frontend/src/api/interview.ts`
- Create: `frontend/src/components/interview/InterviewEvaluationEvidence.vue`
- Modify: `frontend/src/components/interview/InterviewFeedbackPanel.vue`
- Modify: `frontend/src/components/interview/InterviewReviewAttemptPanel.vue`

**Interfaces:**
- Consumes: `InterviewAnswerEvaluation.answerEvidence` 和 `knowledgeEvidence`。
- Produces: 一个只读、无状态、纯文本插值的证据组件；正式评价和每次复习评价复用同一展示。

- [ ] **Step 1: 扩展 TypeScript 契约**

增加 `InterviewKnowledgeEvidence`，并在 `InterviewAnswerEvaluation` 中增加两个非可选数组字段，与后端 JSON 一致。

- [ ] **Step 2: 创建单一职责证据组件**

组件只接收 `evaluation` prop；有回答引文时展示“你的回答依据”，有知识引文时展示来源、标题路径和引文。所有模型文本使用 Vue 插值，不使用 `v-html`，列表 key 由来源元数据与引文组成。

- [ ] **Step 3: 组合进现有反馈与 attempt**

正式面试反馈在资料状态下方展示证据；复习历史的每条 attempt 在回答正文后展示对应证据。父组件不新增状态、watcher、请求或数据转换。

- [ ] **Step 4: 执行前端构建**

```shell
npm run build
```

预期：`vue-tsc --noEmit` 与 Vite 生产构建通过。

### Task 4: 同步事实文档并提交

**Files:**
- Modify: `README.md`
- Modify: `docs/01-需求边界.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/03-阶段路线.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/superpowers/plans/2026-08-08-verifiable-interview-evidence.md`

**Interfaces:**
- Consumes: 已通过测试和构建的证据化评分事实。
- Produces: 简历可讲的“模型结构化证据 + Java 确定性验真 + 持久化兼容 + 双场景展示”说明与长期 ADR。

- [ ] **Step 1: 更新当前能力和边界**

明确证据是模型候选输出，经服务端字面包含和来源序号验证后才持久化；它提升可解释性，但不等于自动证明评分绝对正确。

- [ ] **Step 2: 记录参考项目采纳判断**

记录 `hiring-agent` 的按维度 evidence 和 `aural-oss` 的统一 rubric 暴露出的差距；不复制 ATS、动态岗位维度、语音流程或外部实现。

- [ ] **Step 3: 记录版本和兼容决策**

新增 ADR：证据输出触发 Prompt/rubric 版本升级；现有 JSON 缺失字段恢复为空证据，新评价必须通过证据验真。

- [ ] **Step 4: 执行最终针对性验证**

```shell
.\mvnw.cmd -Dtest=InterviewAnswerEvaluationServiceTest,InterviewAnswerEvaluationTest,MySqlInterviewSessionStoreTest,InterviewReviewAttemptServiceTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test
cd frontend && npm run build
git diff --check
```

- [ ] **Step 5: 精确暂存并提交**

只暂存本轮文件；`docs/05`、`docs/06` 和 `InterviewFeedbackPanel.vue` 使用精确暂存保护用户既有差异。提交信息：

```text
feat: 增加可核验评分证据
```
