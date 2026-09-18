# Phase36：知识库题库

## 本轮目标

完成“选择资料 -> 异步生成 -> 编辑审核 -> 人工/随机组卷 -> 现有面试评分与报告”的知识库专项训练闭环，并保持与 InterviewGuide `969e2af` 的题目状态、生成状态和组卷边界可对照。

## 已完成

- 题目绑定 `source + documentVersion`，保存参考答案、要点、Rubric、固定追问和逐字证据，使用 `DRAFT/ACTIVE/ARCHIVED/STALE` 管理审核与资料失效。
- 生成任务使用 `QUEUED/PROCESSING/COMPLETED/FAILED`，支持部分成功保留、显式恢复缺口、模型超时和应用重启安全失败。
- `requestId` 保证同一生成请求幂等，`active_key` 唯一键保证同一资料当前版本最多一个活动生成任务，避免并发重复调用模型。
- `/question-bank` 工作台支持资料和具体 Skill 配置、任务轮询、手工草稿、编辑、审核启停、删除、筛选、人工选题和随机组卷。
- 正式组卷只使用当前资料版本的 ACTIVE 题目，追问容量不足时明确拒绝；会话保存答案、Rubric 和证据快照，复用现有评分、报告、复习和恢复链路。
- 题目使用次数按会话幂等记录。InterviewGuide 只作为基础语义参考，本地证据校验、恢复、幂等和复习增强继续保留；题目聚合得分、薄弱项组卷与低分维度映射统一留到阶段 8.3。

## 主要变更

- `src/main/java/com/javainterview/rag/interview/bank/*`：题库领域、生成、资料证据和组卷服务。
- `src/main/java/com/javainterview/rag/interview/persistence/*QuestionBank*`、`BankGenerationTask*`：MySQL 存储、乐观锁和活动任务唯一键。
- `src/main/java/com/javainterview/rag/interview/web/QuestionBank*`：storage 配置与题库接口。
- `deploy/mysql/init/030_knowledge_question_bank.sql`：题目、生成任务和使用记录表。
- `frontend/src/api/questionBank.ts`、`frontend/src/composables/useQuestionBank.ts`、`frontend/src/components/interview/QuestionBank*.vue`、`frontend/src/views/QuestionBankView.vue`：题库工作台和已有面试入口。
- `InterviewQuestion` 与 `InterviewAnswerEvaluationService`：题库评价快照及固定 Rubric 评价。

## 验证

- 题库生成、题目、组卷、MySQL 存储、控制器和回答评价 6 组定向测试共 18 项通过。
- 前端 `npm run build` 通过，包含 `vue-tsc --noEmit` 与 Vite 生产构建。
- 目标 MySQL 5.7.36 已存在 3 张题库表；`active_key` 与 `uk_bank_generation_active` 已执行并核对。
- 数据库核对到 1 个完成生成任务、3 道 ACTIVE 题、3 条题目使用记录和 1 场完成会话；三题评分为 98/97/100，报告已生成。

## 边界与下一步

当前异步执行仍是单实例有界线程池，跨进程 attempt fencing、心跳和自动补投属于阶段 9.1。真实样本是 3 道高分题，不能代表通用模型质量，也没有产生低分复习任务；下一阶段按 Skill/Rubric 版本和低分维度映射完成专项训练，不重复建设题库统计系统。
