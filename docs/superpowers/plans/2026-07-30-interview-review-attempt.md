# 面试复习二次作答实施计划

## 1. 领域与评价契约

- 新增 `InterviewAnswerPreviousAttempt`，扩展 `InterviewAnswerEvaluator` 的复习评价入口。
- 为 `InterviewAnswerEvaluationService` 增加历史回答提示、复习模式（不生成追问）和 prompt/rubric/model 版本元数据。
- 保持正式面试调用路径和已有构造器兼容。

## 2. 后端 attempt 闭环

- 新增 `InterviewReviewAttempt`、异常、store、MySQL entity/mapper/store。
- 扩展复习任务 store 暴露按 session/task 查询。
- 在 `InterviewReviewTaskService` 中实现列表和提交：校验会话/任务/序号，读取原回答与历史 attempt，调用复习评价器，计算分差后持久化。
- 新增请求 DTO、控制器路由和 400/404/409 映射。
- 添加 `008_interview_review_attempt.sql`。

## 3. 前端纵向闭环

- 在 API 类型中加入 attempt 与提交接口。
- 在 `useInterviewSession` 中维护按 task ID 的 attempts、请求状态和错误；恢复/生成任务后加载历史。
- 新增 `InterviewReviewAttemptPanel.vue`，在任务项中提供文本回答、提交、历史评价和分差。
- 保持 `InterviewView` 为组合层，补齐事件传递和清理状态。

## 4. 验证与文档

- 先运行新增及受影响的 Java 单测，再执行 Maven 编译/测试（使用仓库本地缓存和 JDK 21）。
- 执行前端 typecheck/build；不做无关大范围测试。
- 更新 README 与阶段路线文档，说明 attempt 已完成及下一闭环仍为评价质量/日志。
- 检查工作树，确保不包含用户的 `application.yml` 改动后提交。
