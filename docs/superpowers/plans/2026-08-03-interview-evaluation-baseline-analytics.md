# Interview Evaluation Baseline Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不调用模型的前提下，为面试评价日志增加运行/样本筛选和按稳定样本跨运行的可解释统计。

**Architecture:** 继续复用 `t_interview_answer_evaluation_log` 和现有 `storage` 查询接口。MyBatis 只负责有上限的条件查询，Java 服务在返回的最近日志上按 `baselineCaseId + promptVersion + rubricVersion + modelName` 确定性聚合，HTTP 层只解析参数和返回结构化结果。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring MVC、MyBatis-Plus、MySQL 5.7/8、JUnit 5、Mockito

## Global Constraints

- 所有项目写入只允许发生在 `D:\IdeaProjects\java-interview-rag`；`blog-springboot` 仅可只读参考。
- 保留用户未提交的 `src/main/resources/application.yml` 端口 `8080` 改动，不修改、不暂存。
- 不调用 Chat、Embedding 或 Qdrant，不新增模型费用。
- 不修改 `/interview` 用户界面；评价统计属于内部质量接口。
- 新增或修改的代码注释默认使用简体中文，仅在边界不明显时添加。
- 只执行直接相关测试和一次不调用模型的真实 `storage` 查询验证。

---

### Task 1: 增加有边界的基线日志筛选

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationLogStore.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/InterviewAnswerEvaluationLogMapper.java`
- Modify: `src/main/java/com/javainterview/rag/interview/persistence/MySqlInterviewAnswerEvaluationLogStore.java`
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationLogQueryService.java`
- Test: `src/test/java/com/javainterview/rag/interview/persistence/MySqlInterviewAnswerEvaluationLogStoreTest.java`

**Interfaces:**
- Consumes: `InterviewAnswerEvaluationLog.Mode`、可空 `baselineRunId`、可空 `baselineCaseId`、1 到 100 的 `limit`。
- Produces: `findRecent(Mode mode, String baselineRunId, String baselineCaseId, int limit)`，按创建时间与主键倒序返回无正文日志。

- [x] **Step 1: 更新 Store 测试并确认旧签名不满足筛选契约**

```java
when(mapper.findRecent("BASELINE", "run-1", "complete-grounded-answer", 20))
        .thenReturn(List.of(entity));
assertEquals(List.of(log), store.findRecent(
        Mode.BASELINE, "run-1", "complete-grounded-answer", 20));
```

- [x] **Step 2: 将 Mapper 查询改为 MyBatis 动态 SQL**

```java
@Select("<script>SELECT ... FROM t_interview_answer_evaluation_log "
        + "WHERE evaluation_mode = #{evaluationMode} "
        + "<if test='baselineRunId != null'>AND baseline_run_id = #{baselineRunId} </if>"
        + "<if test='baselineCaseId != null'>AND baseline_case_id = #{baselineCaseId} </if>"
        + "ORDER BY created_at DESC, id DESC LIMIT #{limit}</script>")
```

- [x] **Step 3: 在查询服务中规范化筛选值并限制业务边界**

```java
if (mode != Mode.BASELINE && (baselineRunId != null || baselineCaseId != null)) {
    throw new IllegalArgumentException("基线运行或样本筛选只支持 BASELINE 模式");
}
```

- [x] **Step 4: 运行 Store 和查询服务测试**

Run: `mvnw.cmd -Dtest=MySqlInterviewAnswerEvaluationLogStoreTest,InterviewAnswerEvaluationLogQueryServiceTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

Expected: 查询参数原样进入 Mapper，非法非基线筛选返回 `IllegalArgumentException`。

### Task 2: 增加按稳定样本和评价版本的跨运行统计

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationLogQueryService.java`
- Test: `src/test/java/com/javainterview/rag/interview/evaluation/InterviewAnswerEvaluationLogQueryServiceTest.java`

**Interfaces:**
- Consumes: 筛选后的最近日志；迁移前基线日志可能没有运行/样本标识。
- Produces: `Report.baselineCaseSummaries`、`identifiedBaselineCount`、`unidentifiedBaselineCount`。

- [x] **Step 1: 添加两个运行、同一样本和同版本的合成日志**

```java
List<InterviewAnswerEvaluationLog> logs = List.of(
        succeeded("run-2", "case-a", 90, 120),
        succeeded("run-1", "case-a", 70, 80),
        failed("run-3", "case-a", "IllegalStateException", 20));
```

- [x] **Step 2: 断言确定性统计口径**

```java
assertEquals(3, summary.distinctRunCount());
assertEquals(2, summary.succeededCount());
assertEquals(1, summary.failedCount());
assertEquals(80.0, summary.averageOverallScore());
assertEquals(20, summary.overallScoreRange());
assertEquals(100.0, summary.overallScorePopulationVariance());
```

- [x] **Step 3: 实现样本与版本联合分组**

```java
private record BaselineCaseVersionKey(
        String baselineCaseId,
        String promptVersion,
        String rubricVersion,
        String modelName) {
}
```

- [x] **Step 4: 实现总体方差并显式处理样本不足**

```java
if (scores.size() < 2) {
    return null;
}
double mean = scores.stream().mapToInt(Integer::intValue).average().orElseThrow();
double variance = scores.stream()
        .mapToDouble(score -> Math.pow(score - mean, 2))
        .average()
        .orElseThrow();
return round(variance);
```

- [x] **Step 5: 运行查询服务测试**

Run: `mvnw.cmd -Dtest=InterviewAnswerEvaluationLogQueryServiceTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

Expected: 不同评价版本分组、历史无标识日志计数、少于两次成功时方差为 `null`。

### Task 3: 暴露 HTTP 参数并完成端到端验证与文档同步

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/web/InterviewAnswerEvaluationLogController.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewAnswerEvaluationLogControllerTest.java`
- Modify: `README.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/03-阶段路线.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: `GET /api/interviews/evaluations/logs?mode=BASELINE&baselineRunId=...&baselineCaseId=...&limit=20`。
- Produces: 原始无正文日志、版本汇总、稳定样本跨运行汇总和未识别历史记录数量。

- [x] **Step 1: 扩展控制器测试**

```java
mockMvc.perform(get("/api/interviews/evaluations/logs")
        .param("mode", "BASELINE")
        .param("baselineRunId", "run-1")
        .param("baselineCaseId", "case-a"))
        .andExpect(status().isOk());
```

- [x] **Step 2: 扩展控制器可选参数并委托查询服务**

```java
public Report findRecent(String mode, String baselineRunId,
                         String baselineCaseId, int limit) {
    return queryService.findRecent(
            parseMode(mode), baselineRunId, baselineCaseId, limit);
}
```

- [x] **Step 3: 执行直接相关测试**

Run: `mvnw.cmd -Dtest=InterviewAnswerEvaluationLogQueryServiceTest,MySqlInterviewAnswerEvaluationLogStoreTest,InterviewAnswerEvaluationLogControllerTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

Expected: 全部测试通过，主代码按 Java 17 编译成功。

- [x] **Step 4: 启动临时 storage 实例并执行零费用查询**

Run: `mvnw.cmd -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp -Dspring-boot.run.profiles=storage -Dspring-boot.run.arguments=--server.port=18084 spring-boot:run`

Expected: `GET /api/interviews/evaluations/logs?mode=BASELINE&limit=20` 返回 HTTP 200，不触发 Chat、Embedding 或 Qdrant 写入。

- [x] **Step 5: 同步项目事实文档并准备提交**

```text
feat: 完善固定评分跨运行统计
```

Expected: 文档明确总体方差口径、历史无标识日志处理、真实评分仍未执行以及用户端口改动未纳入提交。
