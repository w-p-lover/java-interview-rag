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

- `ResumeJobMatchServiceTest` 和 `ResumeJobMatchControllerTest` 共 5 个测试通过。
- 前端 `npm run build` 通过，包含 `vue-tsc --noEmit` 和 Vite 生产构建。
- 未执行真实 Chat 调用，未执行迁移 `016`；storage 部署前必须按顺序执行该迁移。

## 边界与下一步

- 岗位匹配只表示当前已确认事实对 JD 的证据覆盖，不是录用评分，也不判断候选人能力真伪。
- 当前模型请求日志只记录脱敏元数据；没有用户身份和跨会话数据归属前，不建设跨岗位趋势或全量候选人画像。
- 下一步优先完成一次带迁移 `016` 的 storage 启动契约检查，随后再评估项目深挖面试官是否有真实需求。
