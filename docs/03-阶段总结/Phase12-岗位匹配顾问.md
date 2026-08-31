# Phase12-岗位匹配顾问

## 升级内容

- 新增岗位匹配顾问 API，使用已确认简历事实与用户提供的岗位描述进行逐条对照。
- 结构化输出 `MATCHED`、`PARTIAL`、`MISSING`、`NEEDS_VERIFICATION` 四类状态；岗位原文引文和非缺口简历证据都由 Java 服务端逐字验真。
- 新增独立请求日志和 MySQL 迁移 `016_resume_job_match_request_log.sql`，只保存模型、Prompt、token、耗时、状态和失败类型，不保存岗位描述、简历、证据或匹配正文。
- Vue `/interview` 接入岗位匹配面板，审计结果过期或 JD/岗位变化时自动标记旧结果不可参考。

## 直接相关文件

- `src/main/java/com/javainterview/rag/interview/matching/ResumeJobMatchService.java`
- `src/main/java/com/javainterview/rag/interview/matching/ResumeJobMatchRequest.java`
- `src/main/java/com/javainterview/rag/interview/matching/ResumeJobMatchResult.java`
- `src/main/java/com/javainterview/rag/interview/web/ResumeJobMatchController.java`
- `src/main/java/com/javainterview/rag/interview/web/ResumeJobMatchConfiguration.java`
- `deploy/mysql/init/016_resume_job_match_request_log.sql`
- `frontend/src/composables/useResumeJobMatch.ts`
- `frontend/src/components/interview/ResumeJobMatchPanel.vue`

## 验证

- `ResumeJobMatchServiceTest` 和 `ResumeJobMatchControllerTest` 共 5 个测试通过；应用上下文相关验证已在上一轮通过。
- 前端 `npm run build` 通过，包含 `vue-tsc --noEmit` 和 Vite 生产构建。
- 在共享 MySQL 5.7.36 的 `java_interview_rag` 中执行迁移 `016_resume_job_match_request_log.sql`：执行前表不存在，执行后包含 12 个字段、主键、请求 UUID 唯一键、状态/时间索引和创建时间索引，初始行数为 0。
- 使用脱敏岗位描述和 3 条确认事实完成 1 次 `qwen3.7-plus` 真实调用，返回 4 条要求（`MATCHED` 2、`PARTIAL` 1、`MISSING` 1）；岗位引文、非缺口简历证据逐字验真通过，缺口项未携带简历证据。
- 通过 `GET /api/interviews/job-matches/requests?limit=5` 验证日志落库，状态为 `SUCCEEDED`，包含模型、Prompt、token 和正耗时，仅返回脱敏元数据，不含岗位描述、证据或匹配正文。

## 边界与下一步

- 岗位匹配只表示当前已确认事实对 JD 的证据覆盖，不是录用评分，也不判断候选人能力真伪。
- 当前模型请求日志只记录脱敏元数据；没有用户身份和跨会话数据归属前，不建设跨岗位趋势或全量候选人画像。
- 当前岗位匹配闭环已具备部署前数据库和真实模型证据；下一步回到路线中的项目深挖面试官，先做只读知识库追问的需求边界和固定验收样本，不扩展通用招聘评分。
