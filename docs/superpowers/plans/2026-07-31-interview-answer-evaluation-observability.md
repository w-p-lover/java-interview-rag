# Interview Answer Evaluation Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立可重复执行的脱敏面试回答评分样本，并为正式面试、复习作答和基线评分记录不含正文的评价元数据。

**Architecture:** 固定样本以 Java 只读数据集保存，由批量服务调用现有 `InterviewAnswerEvaluator`，只报告评分是否落入宽松预期区间。评价日志通过装饰器统一包裹正式、复习和基线三种调用，记录版本、模型、分数、耗时、状态和失败类型；MySQL 日志不保存简历、题目或回答正文，写入失败不得影响评价主链路。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring AI、MyBatis-Plus、MySQL 5.7/8、JUnit 5、Mockito。

## Global Constraints

- 所有仓库文件写入仅发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 只读，本轮不访问其业务代码。
- 不新增依赖，不修改前端，不引入趋势、用户身份、调度器或 Prompt 管理后台。
- 新增代码注释和用户可见错误默认使用简体中文。
- 保留用户对 `src/main/resources/application.yml` 的未提交端口修改，不暂存、不覆盖。

---

### Task 1: 固定评分样本与批量报告

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/FixedInterviewAnswerEvaluationDataset.java`
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/FixedInterviewAnswerEvaluationService.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluator.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationService.java`
- Test: `src/test/java/com/javainterview/rag/interview/evaluation/FixedInterviewAnswerEvaluationServiceTest.java`

**Interfaces:**
- Produces: `FixedInterviewAnswerEvaluationDataset.findAll()` 返回稳定、唯一的脱敏样本。
- Produces: `InterviewAnswerEvaluator.evaluateBaseline(...)` 执行不生成追问的评分。
- Produces: `FixedInterviewAnswerEvaluationService.evaluate()` 返回样本通过数、版本快照和逐样本结果。

- [ ] **Step 1: 写入固定样本和批量结果测试**
- [ ] **Step 2: 运行测试并确认缺少新类型的编译失败**
- [ ] **Step 3: 实现空回答拒绝、弱回答、部分回答、完整回答和无知识来源五类样本**
- [ ] **Step 4: 实现批量评分与宽松分数区间检查，不把区间外结果伪装成接口失败**
- [ ] **Step 5: 运行定向测试确认通过**

### Task 2: 无正文评价日志

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationLog.java`
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationLogStore.java`
- Create: `src/main/java/com/javainterview/rag/interview/evaluation/ObservableInterviewAnswerEvaluator.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/InterviewAnswerEvaluationLogEntity.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/InterviewAnswerEvaluationLogMapper.java`
- Create: `src/main/java/com/javainterview/rag/interview/persistence/MySqlInterviewAnswerEvaluationLogStore.java`
- Create: `deploy/mysql/init/009_interview_answer_evaluation_log.sql`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionConfiguration.java`
- Test: `src/test/java/com/javainterview/rag/interview/evaluation/ObservableInterviewAnswerEvaluatorTest.java`

**Interfaces:**
- Produces: `InterviewAnswerEvaluationLogStore.record(log)` 记录一次评价元数据。
- Produces: `ObservableInterviewAnswerEvaluator` 在成功和失败时分别记录 `SUCCEEDED`、`FAILED`，并原样返回结果或抛出异常。

- [ ] **Step 1: 写入成功、失败和日志存储故障测试**
- [ ] **Step 2: 运行测试并确认 RED**
- [ ] **Step 3: 实现装饰器和仅含模式、版本、模型、分数、耗时、状态、失败类型的日志模型**
- [ ] **Step 4: 增加 MySQL 5.7/8 兼容迁移、Entity、Mapper 和 storage 装配**
- [ ] **Step 5: 运行定向测试确认日志不改变评价语义**

### Task 3: 批量评估入口与事实文档

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewAnswerEvaluationController.java`
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewSessionConfiguration.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewAnswerEvaluationControllerTest.java`
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Produces: `POST /api/interviews/evaluations/answers`，执行固定样本并返回评分区间结果；会产生最多四次 Chat 调用。

- [ ] **Step 1: 写入 HTTP 契约测试**
- [ ] **Step 2: 实现 storage profile 下的批量入口和 Bean 装配**
- [ ] **Step 3: 运行三组直接相关测试和评价器回归测试**
- [ ] **Step 4: 检查 `git diff`，确认用户端口改动未进入本轮差异**
- [ ] **Step 5: 同步 README、进度、开发记录、调研结论和 ADR，只记录实际验证事实**
- [ ] **Step 6: 暂存并提交本轮直接相关文件**
