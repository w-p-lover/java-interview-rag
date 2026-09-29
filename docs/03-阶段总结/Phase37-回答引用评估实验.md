# Phase37-回答引用评估实验

## 交付结果

- 在阶段 11.1 检索实验中心旁新增回答与引用实验运行框架，固定复用 5 道回答评价样本。
- 每个样本保存回答总分、知识依据判断、回答证据数量、知识证据数量、耗时和失败类型；空回答等预期拒答作为已处理样本，不把模型拒答误记为运行崩溃。
- 运行契约固定数据集版本、Prompt 版本、Rubric 版本、模型名和样本数；相同契约启动幂等复用，`QUEUED/RUNNING` 在进程恢复后继续，成功样本不会重复调用 Chat。
- `/evaluations` 新增回答实验入口，可选择基线/候选并比较评价通过率、知识依据判断准确率、引用完整率、平均总分和 P95 延迟，人工记录 `ADOPTED/REJECTED`。
- 隔离 storage 实例已真实运行一次 `answer-citation-dev-baseline`：5/5 样本成功、0 失败，4 次实际 Chat（空回答在本地校验阶段拒答）、评价通过率/知识依据判断准确率/引用完整率均为 `100%`，平均总分 `47.75`，P95 `33,596 ms`；运行 ID 为 `2f134def-7d71-4d5f-b9a1-15b064cf018d`。该数字只代表当前模型 `configured-default/qwen3.8-max-0902#0` 和固定 5 题，不代表候选模型已通过。

## 变更文件

- `src/main/java/com/javainterview/rag/interview/evaluation/AnswerExperiment*.java`：契约、运行、指标和恢复服务。
- `src/main/java/com/javainterview/rag/interview/persistence/AnswerExperiment*.java`：MySQL 实体、Mapper 和存储适配。
- `src/main/java/com/javainterview/rag/interview/web/InterviewSessionConfiguration.java`、`AnswerExperimentController.java`：Bean 与接口。
- `deploy/mysql/init/041_interview_answer_experiment.sql`：回答实验运行表。
- `frontend/src/api/answerExperiment.ts`、`frontend/src/components/evaluation/AnswerExperimentPanel.vue`、`EvaluationExperimentView.vue`：工作台入口。

## 验证与边界

- 开发库已执行并核对 `041_interview_answer_experiment.sql`：19 列、5 个索引（含唯一契约索引和基线外键）。
- `frontend` 的 `npm run build`（含 `vue-tsc --noEmit`）通过，Vite 生成 2071 个模块。
- Java 全量编译仍被本轮之前的 `file`、`bank` 源码既有编码/语法错误阻塞，未将这些无关文件纳入本轮修复；回答实验未获得后端启动级验证。
- 本轮回答基线实际执行 Chat 4 次、Embedding 0 次；后续候选模型按同一固定 5 题、最多 5 次 Chat 验收，首个非预期失败即停止，不为追求更高分重复运行。

## 后续

先在后端可编译环境执行一次固定 5 题回答基线，再进入评分稳定性/模型迁移实验；候选通过 DEV 后才运行 HOLDOUT，随后收口 Compose、备份恢复和五分钟演示。
