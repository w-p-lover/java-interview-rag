# Phase16-qwen-max项目深挖评分演示

## 本轮升级

沿用上一轮 storage 演示，使用默认 `qwen3.7-max-2026-06-08` 将确定性项目深挖题接入真实流式评分，验证新模型对现有会话、事件、报告和复习任务链路的兼容性。

## 真实结果

- 临时 `storage` 实例在 `18081` 启动成功。
- 创建 `PROJECT_DEEP_DIVE` 会话，4 道题，`maxQuestionCount=4`。
- 顺序提交 4 次 `/answers/stream`，每次均收到回答接收、评价开始、评价完成阶段；最后一次收到 `SESSION_COMPLETED`。
- 4 次评价日志模型名均为 `qwen3.7-max-2026-06-08`，状态均为 `SUCCEEDED`。
- 综合分依次为 `63`、`80`、`63`、`75`，报告平均综合分 `70.3`；平均技术 `68.8`、完整 `62.5`、表达 `80.0`。
- `GET /events` 返回 9 个阶段；完成后生成 4 条 `PENDING` 复习任务，复习 attempt 数为 0。

## 失败与边界

- 第一次提交因演示脚本 Accept 头错误收到 `406 Not Acceptable`，没有进入模型评价；修正为 `text/event-stream` 后从原会话继续。
- 本轮只验证新模型的真实调用和结构化链路，不声称新模型质量优于历史 `qwen3.7-plus`，也没有运行简历审计、岗位匹配、复习 attempt 或固定评估集。
- 临时会话、复习任务、评价日志已精确删除，端口已释放。

## 验证

- 新模型 4 次评分全部通过服务端回答证据校验。
- 评价日志查询确认 `promptVersion=interview-answer-evaluation-v3`、`rubricVersion=interview-answer-rubric-v2` 和新模型名。
- 未修改模型 Prompt、评分规则、会话结构或数据库迁移。
