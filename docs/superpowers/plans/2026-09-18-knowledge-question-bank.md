# 知识库题库 Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development for the bounded frontend task; execute backend and integration inline. Steps use checkbox syntax for tracking.

**Goal:** 完成选择知识资料、异步出题、编辑审核、选题面试、报告和低分复习的用户闭环。

**Architecture:** 复用资料快照和现有面试会话；storage 使用 MySQL 保存题目与生成任务，生成调用在事务外执行，逐题幂等写入。题库状态与 InterviewGuide 对齐，现有可信证据校验作为增强层。

**Tech Stack:** Java 17 / Spring Boot 3 / Spring AI / MyBatis-Plus / MySQL 5.7 / Vue 3 / TypeScript。

**Spec:** `docs/09-后续开发路线.md` 阶段 8.2；`AGENTS.md`；上游 `969e2af8550f680688f5b50f0d895186a661a319` 的 KnowledgeBaseQuestionService、KnowledgeBaseQuestionGenerationService、QuestionGenerationStateService、KnowledgeBaseInterviewService 及直接测试。

## Global Constraints

- 所有项目写入仅在 `D:\IdeaProjects\java-interview-rag`，blog-springboot 只读。
- 只实现题库相关功能；不引入 MQ、Provider 中心或第二套评分/复习。
- 状态 `DRAFT / ACTIVE / ARCHIVED / STALE`；仅当前资料版本的 ACTIVE 题目可创建新会话。
- 生成状态 `QUEUED / PROCESSING / COMPLETED / FAILED`；失败保留成功题目，显式恢复只补缺口。
- 原始资料仅作为数据，不允许 Prompt 注入修改系统规则；所有模型生成题至少绑定一条可逐字核对的资料证据。
- 编辑会撤销审核回到 DRAFT；来源失效变 STALE，重新激活必须校验当前版本。
- 一轮真实付费验收最多 8 次 Chat：生成最多 2 次，三题评分最多 3 次，复习评分最多 1 次，故障修复后单样本最多 2 次；不重新运行旧角色迁移基线，不发起生成检索 Embedding。
- 不删除或修改已有用户资料；验证使用 evaluation/corpus 中授权项目自有资料或已有可用资料，不清理真实业务数据。

## Task 1: 后端题库与专项面试

**Files:** 新增 `interview/bank/` 领域、存储、生成服务和编排服务，新增 persistence 实体/Mapper/MySQL Store；新增 web 控制器/配置；新增 `deploy/mysql/init/030_knowledge_question_bank.sql`；修改 InterviewQuestion 以兼容构造器保存可空题库评价快照，评价 Prompt 使用该快照。

**Interfaces:** API 前缀 `/api/interviews/question-bank`。

```text
GET /?source=&status=&difficulty=&category=&keyword= -> BankQuestion[]
POST / -> {source,skill,difficulty,category,question,topicSummary,referenceAnswer,keyPoints,scoringRubric,followUps} -> BankQuestion(DRAFT)
PUT /{id} -> {expectedVersion,category,question,topicSummary,referenceAnswer,keyPoints,scoringRubric,followUps} -> BankQuestion
PATCH /{id}/status -> {expectedVersion,status: DRAFT|ACTIVE|ARCHIVED} -> BankQuestion
DELETE /{id}?expectedVersion= -> 204
GET /tasks?source= -> GenerationTask[] (recent 20)
POST /tasks -> {requestId,source,skill,difficulty,questionCount:1..8,followUpCount:0..2} -> GenerationTask
GET /tasks/{taskId} -> GenerationTask
POST /tasks/{taskId}/resume -> GenerationTask
POST /sessions -> {requestId,source,questionIds:string[],questionCount:3..8,followUpCount:0..2,skill,difficulty,category?} -> InterviewSession
```

```ts
interface BankFollowUp { question: string; referenceAnswer: string; keyPoints: string[]; scoringRubric: string }
interface BankQuestion {
 id: string; generationTaskId: string | null; source: string; documentVersion: string;
 skill: InterviewSkill; difficulty: InterviewDifficulty; category: string; question: string;
 topicSummary: string; referenceAnswer: string; keyPoints: string[]; scoringRubric: string;
 followUps: BankFollowUp[]; evidence: KnowledgeAnswerSource[];
 status: 'DRAFT'|'ACTIVE'|'ARCHIVED'|'STALE'; version: number;
 useCount: number; lastUsedAt: string | null; createdAt: string; updatedAt: string;
}
interface GenerationTask {
 id: string; requestId: string; source: string; documentVersion: string;
 skill: InterviewSkill; difficulty: InterviewDifficulty; questionCount: number; followUpCount: number;
 status: 'QUEUED'|'PROCESSING'|'COMPLETED'|'FAILED'; savedCount: number; skippedCount: number;
 attemptId: string | null; promptVersion: string; modelName: string; errorCode: string | null;
 version: number; createdAt: string; updatedAt: string;
}
```

- [ ] 实现逐题持久化、任务 requestId 唯一键、题面规范化去重、乐观锁和任务 attempt guard；重启安全失败并提供显式恢复，不自动产生费用。
- [ ] 基于当前版本快照构建有限知识片段并一次生成剩余题量，逐项检查题面/答案/Rubric/证据合法性；成功部分保存，缺口以 FAILED 返回并允许恢复。
- [ ] 组卷仅使用同来源当前版本的 ACTIVE 题目，按难度/Skill/分类筛选；人工不足或固定追问容量不足时明确 400/409；随机模式严格选满，整场含追问最多 8 题。
- [ ] 正式确认复用会话 requestId 幂等；题库答案、Rubric、原始证据进入历史题目快照。
- [ ] 运行直接相关服务/存储/控制器测试；真实模型验证只在实现完成后执行。

## Task 2: 题库工作台（可独立委派）

**Files:** 新增 `frontend/src/api/questionBank.ts`、`frontend/src/composables/useQuestionBank.ts`、`frontend/src/components/interview/QuestionBankEditor.vue`、`frontend/src/components/interview/QuestionBankList.vue`、`frontend/src/views/QuestionBankView.vue`；修改 router/index.ts、WorkspaceHeader.vue。

**Consumes:** Task 1 的上述 API；`listKnowledgeDocuments()`；现有面试 `/interview?sessionId=` 恢复入口。

**Produces:** `/question-bank` 可用工作台。

- [ ] 工作台选择当前资料、Skill、难度、题量和固定追问数，提交异步生成；稳定 requestId 在请求失败重试期间复用，显式新生成才换 id。
- [ ] 显示最近任务，PROCESSING/QUEUED 状态轮询，FAILED 可恢复，成功或部分成功后刷新题目；刷新页面恢复任务；取消旧轮询，避免切换资料后旧响应覆盖。
- [ ] 列表支持状态/难度/分类/关键词筛选、人工选题与随机组卷；显示资料版本、题目状态、使用次数。
- [ ] 编辑器支持题面、答案、要点、Rubric 和固定追问；保存、审核启用、停用、删除都携带版本；动作失败保留输入和题单可继续操作。
- [ ] 新建手工草稿可用，证据由后端关联当前资料；审核不能把来源失效题强制激活。
- [ ] 选择题目或随机组卷后导航到已有面试会话；创建请求失败保留 requestId，选择/配置变化使旧 requestId 失效。
- [ ] 保持已有系统字体、颜色 token 和工作台布局；标准输入/选择器/checkbox；图标使用 @lucide/vue；可访问标签与移动端不溢出。
- [ ] 运行 `npm run build`，只验证直接交互，不做无关视觉重构。

## Task 3: 集成与交付

- [ ] 应用 030 到独立项目数据库，核对表/索引；启动 storage 服务和前端，不改变现有服务进程。
- [ ] 在调用前记录固定样本、8 次 Chat 上限及失败停止条件；验收至少 3 题合法证据、审核/刷新恢复、正式会话重复创建同 id、完成报告和复习入口。
- [ ] 最小服务测试证明部分失败保留、恢复不重建成功题、来源失效拒绝组卷、追问容量不足、并发编辑和重复确认。
- [ ] 做 desktop/mobile 页面截图核对；逐项审查差异，修本轮明确缺陷。
- [ ] 更新 docs/04、docs/06 上游映射、docs/08 能力和 docs/09 阶段状态；直接相关文件独立提交。

## 取舍

本地单实例有界执行器代替 Redis Stream，跨 Worker 的完整心跳机制仍归阶段 9.1；重启时安全失败并显式恢复，避免未经用户操作再次付费。保留已审核题目而非每次生成删除旧题，以符合本地历史来源/失败恢复契约。生成上下文从指定版本快照切片取得，避免无来源过滤检索混入其他资料或旧版本。
