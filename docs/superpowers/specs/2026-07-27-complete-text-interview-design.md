# 完整文本模拟面试闭环设计

## 目标

把现有“画像、首轮题目、三题会话”升级为可直接使用的文本模拟面试：用户输入目标岗位、简历和可选 JD 后直接开始面试；系统生成可配置数量的核心题，逐题接收回答并给出结构化评价，根据回答追加必要追问，达到题目队列末尾或上限后生成最终报告。Vue 前端提供完整操作入口。

## 方案比较

1. 继续固定 3 题，答完后只生成报告：改动小，但不符合真实面试深挖过程。
2. 一开始生成全部固定题目，只做逐题评分：题数可控，但没有动态追问。
3. 生成一组核心题，逐题评分时按需追加追问，并设置总题数上限：兼顾覆盖度、动态性和成本可控；本轮采用该方案。

## 用户流程

1. 用户填写目标岗位、简历、可选 JD、初始题数和整场题数上限。
2. `POST /api/interviews/sessions/start` 串联画像生成、核心题生成、知识来源绑定和会话保存，直接返回第一题。
3. 用户提交当前题回答，请求必须包含 `questionIndex`、`expectedVersion` 和 `answer`。
4. 服务先检查题号和版本，再调用模型生成结构化评价。评价包含技术准确性、完整性、表达清晰度、优点、遗漏和改进建议。
5. 模型认为需要深挖且未达到题数上限时，生成一条追问及知识检索查询；服务绑定真实知识来源后追加到题目队列末尾。
6. 当前题目队列回答完毕后，会话自动变为 `COMPLETED` 并生成确定性汇总报告。
7. 页面刷新后使用会话 UUID 从 MySQL 恢复进度。

## 题量与完成规则

- 初始题数允许 3 到 8，默认 5。
- 整场题数上限允许初始题数到 12，默认 8。
- 初始题目应尽量覆盖不同画像关注领域。
- 每个回答最多追加 1 道追问，追问不能与已有题目文本重复。
- 题目队列耗尽即完成；题数上限是追问上限，不强制凑满。
- 已完成会话拒绝继续提交。

## 安全提交与并发

回答请求必须带客户端看到的当前题号和版本。服务在模型调用前检查它们是否等于数据库会话状态，延迟重试会直接返回 HTTP 409，不会被当成下一题回答。最终写入仍使用 `WHERE id = ? AND version = ? AND status = 'IN_PROGRESS'` 乐观锁，两个真正并发的请求最多只有一个保存成功。

## 逐题评价

`InterviewAnswerEvaluationService` 复用现有 ChatClient。Prompt 只接收确认画像、当前题目、用户回答和题目已绑定的知识来源；输入内容仅作为事实，忽略改变评分规则的指令。

结构化评价保存：

- `technicalScore`、`completenessScore`、`communicationScore`，范围 0 到 100。
- `strengths`、`gaps`、`improvementSuggestion`。
- `knowledgeGrounded`，由题目是否有真实知识来源决定，不由模型伪造。
- 可选追问题目、考察目标和知识检索查询。

没有知识来源时，Prompt 明确不得声称已验证事实正确性，只能根据回答内部逻辑、画像和考察目标评价；结果仍标记 `knowledgeGrounded=false`。

## 最终报告

报告不再增加一次模型调用，而是从已保存的逐题评价确定性汇总：回答题数、三项平均分、总体平均分、去重后的主要优点和主要薄弱项。这样报告可重复、无额外费用，也不会和逐题评价矛盾。

## 持久化升级

在现有 `t_interview_session` 上新增：

- `max_question_count`：追问题数上限。
- `report_json`：完成后的汇总报告，可为空。

题目 JSON 会随追问追加而更新，回答 JSON 保存逐题评价。新增 `005_expand_interview_session.sql` 兼容已经执行过 004 的环境；新环境仍按 001 到 005 顺序执行。

## HTTP 契约

- `POST /api/interviews/sessions/start`：从原始目标岗位、简历和 JD 一键创建完整会话。
- `POST /api/interviews/questions`：保留独立出题接口，新增可选 `questionCount`。
- `POST /api/interviews/sessions`：保留从已确认画像和题目手动创建的入口，新增 `maxQuestionCount`。
- `GET /api/interviews/sessions/{sessionId}`：恢复会话、逐题评价和最终报告。
- `POST /api/interviews/sessions/{sessionId}/answers`：提交题号、版本和回答，返回评价后的最新会话。

400 用于输入错误，404 用于不存在，409 用于题号/版本过期、会话完成或最终乐观锁冲突。

## Vue 组件边界

- `InterviewView.vue`：路由级组合层，只连接 composable 和各功能组件。
- `useInterviewSession.ts`：唯一会话状态源，负责启动、恢复、提交、继续查看下一题和重新开始。
- `InterviewSetupForm.vue`：岗位、简历、JD 和题数输入；通过 `start` 事件上送数据。
- `InterviewQuestionPanel.vue`：展示当前题目、考察目标和进度，收集回答并通过 `submit` 事件上送文本。
- `InterviewFeedbackPanel.vue`：展示刚完成题目的三项评分、优点、遗漏、建议和是否追加追问。
- `InterviewReportPanel.vue`：展示最终平均分、优点和薄弱项，提供重新开始。

组件使用 `<script setup lang="ts">`、props down/events up；不引入 Pinia，路由级 composable 足够承载单页会话状态。会话 ID 存入 `localStorage`，页面打开时尝试恢复，404 时清理过期 ID。

## 界面设计

新增 `/interview`，沿用现有暖灰纸面、珊瑚强调、集中 OKLCH 令牌和 Workbench 视觉。桌面端使用“进度/题目主栏 + 会话证据侧栏”，移动端单列。评分使用清晰数字和文本，不使用花哨仪表盘、渐变、伪实时动画或虚构数据。

资料工作台 `/` 保持原功能；页头增加“资料库 / 模拟面试”路由导航，命令面板只在资料工作台使用。

## 范围

- 不实现语音视频、计时器、防作弊、登录、多用户归属、修改历史回答、人工题库管理或 Tool Calling。
- 不引入新 Maven/npm 依赖。
- 不改变 RAG 固定评估集和已有知识库接口。
- 真实模型调用会产生费用；自动测试全部使用 Mock。迁移和真实浏览器验证需使用独立项目环境，不写 `blog` 数据空间。

## 验证

- 后端测试覆盖可配置出题、过期重试拦截、结构化评分、追问追加、题数上限、自动完成、报告汇总、JSON 持久化和 HTTP 状态。
- 前端执行 `vue-tsc` 与 Vite 生产构建。
- 浏览器至少验证开始面试、回答反馈、下一题、完成报告、刷新恢复和移动端无横向溢出。
