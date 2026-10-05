# Java Interview RAG

Java 求职面试知识库与智能训练平台，基于 Java 17、Spring Boot 3，围绕求职方向、简历理解、资料辅助、模拟面试、结构化反馈和复习计划逐步建设，并按需接入 Spring AI 和向量数据库。

阶段 0 的项目治理、阶段 1 的 Markdown 资料知识库和阶段 2 的向量生命周期已经完成，阶段 3 已建立最小 RAG 回答、真实来源返回和知识不足兜底，阶段 4 已形成召回日志、固定评估集、批量评估和 Dense/BM25/Hybrid-RRF 对照闭环。2026-09-02 在 8 份固定语料、18 个 Chunk 上完成一次真实三策略基线：Dense-only 与 Hybrid-RRF 的 Hit@1、Mean Recall@5、MRR 均为 `1.0000`、`0.9706`、`1.0000`，Hybrid 将完整标题路径召回率从 `0.9130` 提升到 `0.9565`；来源排序指标没有提升，不能据此宣称整体质量全面改善。阶段 6 的文本面试训练现已具备“一键生成画像与题目 → 逐题回答 → 结构化评分 → 必要时动态追问 → 自动完成并生成报告”的完整代码闭环，正式评分和复习评分会保存经 Java 服务端逐字验真的回答引文与知识引文。阶段 7 已把完成会话中的回答遗漏和真实知识来源沉淀为复习任务，支持任务完成、可选计划日期、会话内筛选、独立 attempt 和单会话历史；证据化评分 `v3/v2` 已使用五类固定样本完成两轮真实基线，8 次成功评分均通过证据验真。Tool Calling 已具备单会话任务查询、持久化复习进展分析、计划日期写入、无正文执行日志、助手请求日志、累计 token 记录和受限 ChatClient 助手，并使用真实模型验证查询、改期和请求级可观测性；当前会话由结构化请求绑定，三个工具不能访问本次请求之外的会话。

阶段 8.1 面试配置与多来源出题已经完成，当前按 [后续开发路线](docs/09-后续开发路线.md) 进入阶段 8.2 知识库题库，随后继续异步与资源治理、实时交互与求职流程、评估和部署交付。未来能力不计入当前已实现功能。

## 当前 Web 功能

当前字段化 Sparse 评估使用 `content=1.0`、`heading=2.0`、`code=2.5`、`java_identifier=3.0`；固定题中的代表性问题还记录期望 Chunk 与 Hard Negative Chunk。检索评估报告额外返回 Precision@5、nDCG@5、Chunk Recall@5、Hard Negative 避免率、上下文噪声率和检索延迟 P95。使用 `evaluation/run_retrieval_baseline.py --evaluate-hybrid --allow-embedding-calls` 可执行 Dense-only、BM25-only、Hybrid-RRF 三策略对照。

最新字段化真实复核结果见 `evaluation/results/2026-09-02-fielded-sparse-hybrid-baseline.json`：Hybrid nDCG@5 为 `0.9772`，Sparse 检索 P95 约 `18.81ms`；来源级指标与旧基线基本持平。此后固定集已扩为 23 题，Sparse 增加 `technical_anchor_coverage_v1` 最小证据门控；零费用本地验收中 17 道可回答题全部保留候选、6 道无答案题全部拒答。该结果只证明当前固定语料边界，不宣称任意知识库都具备相同拒答质量。

- `POST /api/knowledge/documents/import` 接收 Markdown 文件与显式逻辑来源。
- `POST /api/knowledge/documents/rebuild` 重传当前登记版本的来源与 Markdown 文件，修复缺失或残留向量；历史版本不能直接重建为检索版本。
- `DELETE /api/knowledge/documents?source=...` 清理逻辑来源下的全部向量与版本登记。
- `GET /api/knowledge/documents` 返回每个逻辑来源当前登记的版本、内容字符数和登记时间；不返回历史版本或 Markdown 正文，也不调用模型与向量库。
- `GET /api/knowledge/documents/detail?source=...` 返回指定逻辑来源的当前版本、无正文章节目录和按新到旧排列的历史登记；只读取版本登记，不返回正文或查询向量库。
- `GET /api/knowledge/search?query=...&topK=5&similarityThreshold=0.0` 在 `storage` profile 下执行相似度检索，返回真实 chunk 正文、来源、版本、顺序、标题路径和相似度。
  - `GET /api/knowledge/search/sparse?query=...&topK=5` 在 `local` 与 `storage` profile 下执行 Lucene BM25 稀疏检索；问题包含英文技术锚点时，TopK 候选整体至少覆盖一半锚点才返回结果，否则返回空候选。接口返回真实 chunk、来源、版本、顺序、标题路径和 BM25 分数，不与向量分数混合。
  - `GET /api/knowledge/search/hybrid?query=...&topK=5&denseSimilarityThreshold=0.0` 在 `storage` profile 下执行 Dense + BM25 双路候选召回，使用 RRF 按排名融合，并返回每条结果的 Dense/Sparse 排名、原始分数和融合分数。
  - `POST /api/knowledge/answer` 在 `storage` profile 下根据 `{"question":"..."}` 检索并生成回答，返回回答是否由可靠资料支撑及实际使用的来源列表。
  - `POST /api/knowledge/answer/stream` 在 `storage` profile 下以 SSE 增量返回同一套 RAG 回答；最后一个 `complete` 事件携带最终回答、可靠资料状态和来源列表。
  - `GET /api/knowledge/retrieval-logs?limit=20` 在 `storage` profile 下查询独立数据库中最近 RAG 召回记录，最多返回 100 条。
  - `GET /api/observability/overview` 返回当前实例各 `/api` 业务路由的请求量、失败量、失败率、平均耗时和最大耗时，不返回业务正文。
- `GET /actuator/health`、`GET /actuator/info` 和 `GET /actuator/metrics` 提供本地运行状态、HTTP 请求及 Spring AI 调用指标；默认不暴露 Prompt、简历、回答或向量查询正文。
- `GET /api/knowledge/evaluation-cases` 返回内置的固定评估题集，包含问题、期望逻辑来源和预期回答要点，不调用模型或数据库。
- `GET /api/knowledge/evaluation-cases/structure` 返回 3 道合成 PDF 结构检索题，显式包含期望来源、完整标题路径、页码和内容块类型，不调用模型或数据库。
- `GET /api/knowledge/evaluations/retrieval` 在 `storage` profile 下逐题执行固定题集的相似度检索，返回实际来源、期望来源命中情况和整体命中率，不调用 Chat 模型。
  - `GET /api/knowledge/evaluations/retrieval/structure` 在 `storage` profile 下执行结构检索题，除来源与排序外统计标题路径、页码、块类型及四项联合匹配召回率；会产生 Embedding 检索，不调用 Chat。
  - `GET /api/knowledge/evaluations/retrieval/hybrid` 在 `storage` profile 下使用同一固定题集分别执行 Dense-only、BM25-only 和 Hybrid-RRF，返回三份可对照报告；该接口会产生 Dense Embedding 检索，不调用 Chat。
  - `GET /api/knowledge/evaluations/retrieval/adjacent-context` 在 `storage` profile 下对 Hybrid 初始候选执行相邻 Chunk 扩展开启/关闭对照，返回完整上下文的来源、Chunk、回答要点证据召回、噪声和额外向量查询数；不调用 Chat。
- `POST /api/knowledge/evaluations/answers` 在 `storage` profile 下逐题调用现有 RAG 回答服务，返回实际来源、期望来源命中情况、命中与未命中回答要点、逐题 claim 证据状态及聚合引用精确率/召回率、无依据陈述率和冲突率；当前 23 题固定集最多产生 23 次 Chat 调用并写入对应召回日志。
- `POST /api/interviews/preparations` 在 `storage` profile 下接收目标岗位、简历文本和可选岗位描述，通过一次结构化模型调用返回候选人摘要、简历证据、岗位要求和面试关注领域；该接口不读取内部固定 RAG 评估题集，也不直接生成面试题。
- `POST /api/interviews/resume-audits` 对简历执行一次可选的证据检查，按教育、任职、项目、技术、职责、时间和结果返回经 Java 逐字验真的确认事实，并分开返回待补信息、人工核对风险和修改建议；`/interview` 只在当前页面内存中保留结果，检查失败不阻止直接开始面试。简历审计默认总超时 420 秒，可由 `JAVA_INTERVIEW_RAG_RESUME_AUDIT_TIMEOUT` 覆盖；超时、鉴权失败和模型不可用返回安全 HTTP 502。
- `POST /api/interviews/evaluations/resume-audits` 在 `storage` profile 下启动或复用十一类固定付费评估运行并立即返回运行 ID；`GET /api/interviews/evaluations/resume-audits/{runId}` 查询状态、逐样本脱敏结果和终态报告，`POST /{runId}/resume` 只重试未成功样本。相同数据集、角色、Prompt 与模型契约不会重复创建运行，运行表不保存简历正文。
- `POST /api/interviews/job-matches` 只接收岗位描述和已确认简历事实，返回已有证据、部分覆盖、尚无证据和需要核对的岗位要求；岗位原文与简历证据均由 Java 服务端逐字验真，不接收或保存原始简历。
- `GET /api/interviews/job-matches/requests?limit=20` 查询岗位匹配模型请求的脱敏元数据，包括模型、Prompt、token、耗时、状态和失败类型，不返回岗位描述、简历证据或匹配正文。
- `POST /api/interviews/project-deep-dives` 只接收一条 `PROJECT` 类型的已确认事实，确定性生成基础、取舍、故障和扩展四类深挖题；默认不调用模型或向量检索，显式开启后只读检索知识库并把真实来源作为答题背景，不用资料补写项目经历。
- `POST /api/interviews/drafts` 统一生成准备画像和多来源题目草稿；有简历证据时按约 60%/40% 先生成简历题，再把本场简历题作为排除上下文生成 Skill/方向题，无简历时全部生成方向题。分支失败按可用来源降级，双失败或规范化去重产生的缺口由 Skill 固定题补足；近期同 Skill 历史知识点进入两路 Prompt，近期原题和本场已生成主题不会再次进入方向题单。相同材料、题数、Skill、难度、Prompt、模型和组卷策略在当前实例 30 分钟内复用同一结果，并发重复请求共享一次计算，`regenerate=true` 才显式重新生成；单题知识检索失败时保留题目并返回不可用来源。
- `POST /api/interviews/drafts/{draftId}/questions/{questionIndex}/replace` 只替换指定草稿题并重算来源分配；Skill 固定题从本地目录替换，不调用模型，画像题使用一次独立替换调用且排除当前题单。草稿仅在当前进程有界缓存中保存，不持久化原始简历或 JD。
- `POST /api/interviews/questions` 在 `storage` profile 下接收目标岗位和已确认的面试准备画像，通过一次结构化模型调用生成 3 到 8 道首轮面试题；每题返回考察目标、画像依据类型、知识检索查询、可靠知识命中状态和实际 Markdown chunk 来源。
- `POST /api/interviews/sessions/start` 接收目标岗位、简历、可选 JD、初始题数与整场题数上限，一次完成画像、首轮出题和持久化会话创建；默认首轮 5 道、整场最多 8 道，整场上限可配置到 12 道。
- `POST /api/interviews/sessions` 保存已有目标岗位、确认画像和题目，创建 `IN_PROGRESS` 文本面试会话。
- `GET /api/interviews/sessions/{sessionId}` 从 MySQL 恢复完整会话、回答进度和当前题号。
- `POST /api/interviews/sessions/{sessionId}/answers` 使用当前题号和会话版本提交回答，返回技术、完整性、表达、综合分、优点、遗漏、改进建议，以及 1 到 3 条回答原文证据和最多 3 条知识资料证据；模型候选引文必须经服务端逐字包含、来源序号和来源正文校验后才能持久化。模型可追加一条绑定真实知识来源的追问。当前题目队列答完后自动生成整场报告，乐观锁与题号校验会拒绝并发或延迟重复提交。
- `POST /api/interviews/sessions/{sessionId}/answers/stream` 以 SSE 推送回答接收、评价开始/完成、下一题准备或本场完成等阶段事件，最后返回完整会话；`GET /api/interviews/sessions/{sessionId}/events` 根据已保存回答重建不含回答正文的事件时间线，前端恢复会话时会展示阶段、题号和时间。
- `POST /api/interviews/sessions/{sessionId}/review-tasks` 仅为已完成会话生成待复习任务；每道存在回答遗漏的题目生成一条，重复调用不会重复插入。
- `GET /api/interviews/sessions/{sessionId}/review-tasks?status=PENDING&plannedDateFrom=2026-08-04&plannedDateTo=2026-08-10` 在单个会话内按可选状态和计划日期范围返回任务；不传筛选时按原题顺序返回全部任务。
- `PATCH /api/interviews/sessions/{sessionId}/review-tasks/{taskId}/complete` 将所属任务幂等地标记为 `COMPLETED`，重复调用保留首次完成时间。
- `PATCH /api/interviews/sessions/{sessionId}/review-tasks/{taskId}/schedule` 使用 `plannedReviewDate` 设置或清除可选计划日期，不创建提醒任务。
- `GET /api/interviews/sessions/{sessionId}/review-tasks/{taskId}/attempts` 返回该任务按序保存的复习回答和评价快照；`POST` 接收 `answer` 与 1-based `expectedAttemptNumber`，在模型评价前校验序号，返回与原回答及上次 attempt 的总分差值。
- `GET /api/interviews/sessions/{sessionId}/review-tasks/history` 从持久化任务与 attempt 聚合本场历史，返回已练题数、总作答次数、较正式回答提升的题数，以及每题正式/首次/最近/最好分数、最近未解决遗漏和完整时间线；不调用模型，也不跨会话汇总。
- `storage` profile 暴露 Spring AI 工具回调 `list_review_tasks`，允许受限复习助手在单个面试会话内按状态和计划日期查询复习任务摘要；工具只返回题目、遗漏、改进建议和知识来源元数据，不返回回答正文或 chunk 正文，真实模型查询已验证。
- `get_review_progress` 复用单会话持久化历史，返回任务总数、已练题数、练习次数、提升题数、未解决题数，以及逐题正式/最近/最好分数、分差、未解决遗漏和最近练习时间；不返回正式回答、复习回答、attempt 明细或知识正文。一次脱敏持久化会话的真实 `qwen3.7-plus` 基线已确认模型正确选择该工具，准确解释 `60 -> 75（+15）` 的提升，并将另一题表述为“尚未练习、暂无数据”而不是退步；原始计数和日志摘要保存在 `evaluation/results/2026-08-07-review-progress-assistant-baseline.json`。
- `schedule_review_task` 要求同时提供明确的会话 ID、任务 ID 和 `yyyy-MM-dd` 日期，并且本次助手请求必须携带与三项工具参数完全一致的结构化人工确认；确认缺失或不一致时在调用现有 schedule 服务前拒绝写入，不允许模型猜测或替换标识和日期。
- `POST /api/interviews/review-assistant` 接收 `{"sessionId":"...","message":"..."}`，改期确认请求额外携带 `{"scheduleConfirmation":{"sessionId":"...","taskId":"...","plannedReviewDate":"2026-08-12"}}`；消息最长 2000 字符。服务端将顶层 `sessionId` 绑定到模型不可见的单请求上下文，三个工具在调用业务服务前都必须匹配该会话；改期确认还必须与顶层会话、任务和日期完全一致。未提供确认时助手可以查询任务或分析本场进展，写入改期则必须先请求用户确认。响应返回本次 `requestId` 和最终回答。
- Vue 完成页已在本场报告与复习题目之间接入复习助手：查询任务和复盘进展均自动携带当前会话，不要求用户复制内部 ID；“生成本场复盘”会要求助手基于已保存练习次数、分数变化和未解决遗漏作答；复盘成功后，页面从当前可见任务与持久化历史中确定性选择仍有遗漏的下一题，优先未练习题，再按已有分数和原题顺序排序，用户可以直接定位到再次作答区并在作答后返回刷新复盘；调整计划模式由用户选择待复习题目和日期，先获取助手回复，再展示明确的题目/日期确认区，确认成功后刷新当前筛选下的任务。前端不解析模型回答来生成任务或写入参数，也不自动重复调用模型。
- `GET /api/interviews/review-assistant/requests?limit=20` 查询最近 1 到 100 条复习助手请求日志。日志记录请求 UUID、模型、成功/失败、工具进入次数、累计 prompt/completion/total token、总耗时、最终响应状态和异常类型；不保存用户消息、助手回答、工具参数、工具结果正文或异常消息。
- `storage` profile 的 OpenAI 兼容 ChatModel 只在模型 HTTP 层对连接失败/超时、HTTP 429 和 5xx 最多重试一次，固定退避 500ms；其他 4xx、业务异常和工具异常不重试。重试不包裹 ChatClient 助手或工具执行，因此不会重放已经执行的改期工具。
- `GET /api/interviews/sessions/{sessionId}/tool-executions?limit=20` 查询单个会话最近 1 到 100 条工具执行记录。记录包含执行 UUID、可空助手请求 UUID、工具名、成功/失败、结果数量、耗时和异常类型，不保存工具参数 JSON、回答正文、知识正文或异常消息；日志存储失败只告警，不改变工具结果。
- `POST /api/interviews/evaluations/answers` 在 `storage` profile 下依次执行空回答、无关回答、部分回答、完整回答和无知识来源回答五类脱敏样本，返回本次 `baselineRunId`、实际总分是否落入宽松预期区间，以及成功评价的回答证据/知识证据条数；不返回引文正文。五个稳定 `caseId` 共享同一运行标识，空回答在服务端拒绝，其余样本最多产生 4 次 Chat 调用。
- `POST/GET /api/interviews/evaluation-experiments/score-stability` 在 `storage` profile 下启动、查询和恢复评分稳定性实验；固定 5 题，每个可评分样本重复 2 次，持久化分数序列、知识依据判断、平均波动和 P95，支持同契约幂等、基线/候选比较及人工采用/回退。一次完整运行最多 8 次 Chat，拒答样本不调用模型。
- `GET /api/interviews/evaluations/logs?mode=BASELINE&baselineRunId=...&baselineCaseId=...&limit=20` 在 `storage` profile 下查询最近 1 到 100 条评价日志；两个基线标识可独立或联合筛选。响应同时提供版本汇总和按稳定样本跨运行的成功/失败、总分范围、平均值、总体方差、耗时及失败类型，查询不调用模型。
- Vue `/interview` 在简历证据检查完成且填写岗位描述后提供岗位匹配顾问；岗位描述、目标岗位或审计结果变化后旧结果标记为待更新，匹配失败不阻断开始面试。
- Vue `/interview` 在简历证据检查确认到项目后提供项目深挖面试官：可以切换已确认项目、显式选择是否补充资料库背景，并查看固定四维问题与可展开的真实来源；项目或审计结果变化后旧结果标记为待更新。
- 正式面试、复习作答和固定基线评分统一记录 Prompt/rubric/model、分数、知识依据状态、耗时、状态与失败类型；基线日志额外记录运行 UUID 和稳定样本 ID，正式/复习日志保持为空。评价日志不保存简历、题目、回答或知识正文，日志写入失败不影响主评价。证据化 `v3/v2` 两轮基线共执行 8 次 Chat 并写入 10 条日志，8 次成功评价均通过证据验真且分数落入预期区间；完整回答两次均为 `93`，部分回答为 `63/70`，无知识来源回答为 `63/53`，无关回答为 `3/17`。脱敏结果保存在 `evaluation/results/2026-08-08-verifiable-interview-answer-baseline.json`；两轮观测证明当前结构可用，但不足以宣称长期评分稳定或版本优于 `v2/v1`。
- 逻辑来源按大小写敏感的精确字符串区分，不能包含首尾空白。
- 后端返回内容版本、首次或重复导入状态，以及保留标题路径和顺序的 chunk。
- Vue 两个工作台共用单行 `Java 面试台` 品牌栏和现有中性灰白、石墨、珊瑚、蓝绿状态色；资料库只保留导入、处理链路和结果核对，不再展示开发进度或“能力边界”。`/interview` 采用开放式材料区、状态驱动设置台和任务优先的面试流程，进行中会话使用统一答题画布与石墨反馈侧栏，不使用营销式 Hero 或技术口号。
- Vue `/answer` 将资料目录、相似度检索和 RAG 回答接入同一用户入口：默认展示当前逻辑来源、版本、字符数与登记时间，详情可浏览由既有标题和 chunk 顺序确定性投影的当前版本章节树；每个章节可把完整标题路径预填到“查资料”，用户编辑并显式提交后才执行现有全资料库语义检索，不宣称限定当前文档或 Chunk。旧登记没有目录快照时提示重传当前原文件完成重建。用户也可以按主题或问题查找前 5 条相关原文，或发起单次可靠资料问答；有检索命中时可以把服务端归一化后的当前检索词带入问答框继续编辑，但不会自动调用 Chat。三种模式分别维护加载、错误和结果状态。检索结果与回答来源都可展开查看逻辑来源、标题路径、相似度、文档版本、chunk 顺序与原文；资料不足和接口错误使用独立状态，不把相似资料包装成可靠回答，也不在浏览器保存目录、检索、聊天历史或正文。
- Vue `/interview` 页面以面试任务为首屏，支持填写岗位、选择带文本层的 PDF、Markdown 或 TXT 简历与可选 JD、配置题数、逐题回答和查看上一题反馈；PDF 由后端临时解析，文本粘贴收进备用折叠入口。当前会话和最近 8 场入口只在浏览器保存 UUID，刷新后逐项从后端恢复岗位、状态、进度和更新时间；浏览器不保存简历、回答、评分、报告或知识正文。完成后先展示报告，再展示复习题目。待复习任务可设置或清除计划日期、按状态/日期筛选、标记完成或再次作答；页面显示本场已练题数、累计作答、提升题数和每题正式/首次/最近/最好分数，评价版本信息默认折叠。
- 资料库页面支持选择或拖入 Markdown 学习资料、Java 技术资料和项目说明，并以“选择文件 → 版本判定 → 标题解析 → 生成切片”连接输入与结果，展示版本指纹、重复提示和 chunk 明细。
- 顶部命令面板支持点击或 `Ctrl/Cmd + K` 打开，使用方向键、Enter 和 Esc 完成区段跳转与关闭。
- 默认 `local` profile 的登记仅保存在 Spring Boot 进程内且不写向量；`storage` profile 使用 MySQL 持久化登记，并通过 DashScope Embedding 将首次导入的 chunk 批量写入 Qdrant。
- 相同来源和版本的重复导入会在向量化前短路；同一来源导入新内容时先写入新版本，再清理旧版本向量。已登记版本可以手动重建并收敛同来源其他版本和同版本多余 chunk；回答接口只将相似度不低于 0.60 的资料交给模型，未命中时不调用模型并返回知识不足。

## 文件入口与数据边界

- “资料库上传”和“模拟面试上传”是两个业务入口，不是同一次导入。底层可以复用文件读取组件，但不能复用数据生命周期。
- 资料库上传面向可长期复用的 Java 面试资料、项目技术说明和学习文档；成功导入后会版本登记、切分、向量化，并作为 RAG 回答、面试出题与评分的知识依据。
- 模拟面试上传面向候选人简历和可选 JD；文件只用于提取本场画像和岗位要求，默认不进入资料库、不生成知识向量，也不把原文件内容保存到浏览器本地。
- `/interview` 已以独立文件选择和自动正文提取作为主入口，文本层 PDF 在一次 multipart 请求中由后端 PDFBox 提取，Markdown/TXT 保持浏览器本地读取，随后继续使用既有字符串 API；文件与正文不会自动进入资料库或浏览器本地存储。扫描件会明确提示当前不支持 OCR，DOCX 仍需具备可靠解析能力后再开放。
- PDF 确定性质量基线已扩展为 8 类合成、脱敏输入，新增嵌入中文字体的单栏简历、右侧日期列、无边框技能表和复杂侧栏；当前 `pdfbox-layout-aware-v3` 为 8/8、文本关键短语命中率 100% 且阅读顺序全部通过。该结果只证明固定样本，不代表所有真实简历。

## 项目主线

```text
求职目标与简历档案
→ Markdown 资料知识库
→ 文档解析与切分
→ 向量生命周期
→ RAG 问答与来源引用
→ 召回日志与 RAG 评估
→ 文本模拟面试
→ 动态追问与结构化评分
→ 错题和复习计划
→ Tool Calling
→ 必要时扩展 MCP
```

## 项目边界

- 本项目是独立的新项目，不修改 `D:\IdeaProjects\blog\blog-springboot`。
- Java 是主后端技术路线。
- 第一阶段优先完成 RAG 核心闭环和评估，不提前建设复杂 Agent 平台。
- 外部参考项目只用于设计借鉴，是否采用由真实需求和评估结果决定。

## 本地运行

准备 Java 17 或更高版本的 JDK。先在仓库根目录启动后端：

```shell
.\mvnw.cmd spring-boot:run
```

后端默认监听 `http://127.0.0.1:18080`。再启动前端：

后端启动后可访问 Knife4j 接口文档 `http://127.0.0.1:18080/doc.html`；原始 OpenAPI JSON 为 `http://127.0.0.1:18080/v3/api-docs`。

```shell
cd frontend
npm install
npm run dev
```

浏览器访问 `http://127.0.0.1:5173`。Vite 会将 `/api` 代理到本地后端。

启动前端前，可以用零依赖冒烟脚本确认默认本地链路。脚本只适用于 `local` profile，不调用 Chat、Embedding、MySQL 或 Qdrant：

```shell
python evaluation/run_local_smoke.py
```

脚本会验证固定评估集读取、Markdown 首次导入、重复导入短路、来源删除、删除后 404，以及非法来源和未知路由的失败响应；执行结束会清理临时来源。

## MySQL 与 Qdrant

项目采用独立数据库 `java_interview_rag` 和独立 Qdrant collection `java-interview-rag-chunks`，不会写入 `blog-springboot` 的业务数据。本地 Compose 使用 MySQL 8；当前共享服务器实际为 MySQL 5.7.36，仓库 SQL 同时兼容 5.7 与 8。

本机具备 Docker Compose 且需要新建本地基础设施时，先根据 [`.env.example`](.env.example) 创建本地 `.env`，再启动基础设施：

```shell
docker compose up -d --build
```

该 Compose 会按 `deploy/mysql/init/001_*.sql` 到最新迁移顺序初始化独立数据库，等待 MySQL、Qdrant、Redis 健康后创建缺失的 `java-interview-rag-chunks` collection，再启动 storage 后端和 Nginx 前端。部署完成后执行只读检查：

```shell
python evaluation/run_deploy_smoke.py
```

检查覆盖后端 Actuator 健康、资料目录、评分实验入口、前端首页和 `/api` 反向代理；不会导入资料、调用 Chat/Embedding 或写入业务数据。容器部署的备份/恢复演练仍需在具备 Docker 与数据库备份工具的环境执行。

项目也会只读导入 `D:/IdeaProjects/blog/blog-springboot/.env`。因此在已有服务器环境下，不需要复制主项目密码或 API Key；新项目会复用其中的 MySQL 主机、端口、账号、密码、Qdrant 地址和 DashScope Key，但强制使用独立数据库 `java_interview_rag` 与 collection `java-interview-rag-chunks`。如需覆盖，可使用 `JAVA_INTERVIEW_RAG_` 前缀变量。

以存储 profile 启动后端：

```shell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=storage
```

`storage` 会使用服务器 `.env` 提供的连接信息，通过 Druid、MyBatis-Plus 和 MySQL 持久化“来源 + 内容版本”登记，并使用 Spring AI 的 DashScope Embedding 与 Qdrant 客户端维护 chunk 向量。向量写入按每批最多 10 个 chunk 调用 Embedding，避免大文档超过当前 DashScope 服务端批量上限。新版本成功写入后会按结构化元数据过滤删除同来源旧向量，同时保留历史登记；手动重建要求重传精确来源和当前版本 Markdown 文件，写入失败时保留登记以便重试；显式淘汰先清理 Qdrant，再删除该来源的全部 MySQL 登记。资料目录按每个大小写敏感来源的最大登记 ID 选择当前版本，详情按 ID 倒序返回全部历史登记，并从版本登记的 `outline_json` 返回当前章节目录。新导入会保存目录快照；旧登记保持 `null`，只有当前版本向量重建成功后才补齐。部署存储 profile 前需执行 `deploy/mysql/init/014_knowledge_document_outline.sql`。版本写入失败并撤销登记后会自然回退到上一成功版本。默认 `local` profile 使用对应的进程内当前版本索引。MySQL `source` 使用 `utf8mb4_bin`，查询和删除 SQL 额外使用 `BINARY` 精确比较，以保持和 Java、Qdrant 相同的大小写敏感语义。Qdrant 启动探测失败只记录警告；MySQL 是该 profile 的必要依赖。Qdrant schema 自动初始化关闭，服务器需要预先创建 collection；MySQL 中需要预先创建 `java_interview_rag` 数据库并按顺序执行仓库内初始化 SQL。
简历证据审计和岗位匹配请求日志分别需要按顺序执行 `015_resume_evidence_audit_request_log.sql` 与 `016_resume_job_match_request_log.sql`；两张日志表都不保存简历、岗位描述、证据或模型正文。项目深挖会话迁移 `017_interview_session_mode.sql` 已在共享 MySQL 5.7.36 的独立库执行，为现有会话补充默认 `STANDARD` 模式。

最小检索接口允许 `topK` 取 1 到 20、相似度阈值取 0 到 1；默认阈值为 0.0，用于保留阶段 3 的原始召回基线。调用方可以提高阈值，Qdrant 没有满足条件的结果时返回空列表，不生成没有来源支撑的答案。

回答接口使用 OpenAI 兼容端点手动创建 ChatModel，默认模型为 `qwen3.8-max-0902`、温度为 `0.2`；DashScope 原生客户端继续只承担 `text-embedding-v3`。Chat 的端点、密钥和模型可分别通过 `JAVA_INTERVIEW_RAG_OPENAI_BASE_URL`、`JAVA_INTERVIEW_RAG_OPENAI_API_KEY` 和 `JAVA_INTERVIEW_RAG_OPENAI_CHAT_MODEL` 覆盖，未覆盖时回退到原 DashScope Chat 配置，避免 Chat 覆盖值污染 Embedding。它固定检索前 5 条且相似度不低于 `0.60` 的资料，将每条资料编号并要求模型只根据资料回答；没有可靠来源或模型返回空内容时，统一返回“知识库中没有足够可靠的资料回答这个问题。”。当前默认模型已完成失败超时与不可用安全返回验证，但尚未形成成功质量基线；历史批量质量基线来自旧模型，不能混算。

阶段 4 的首个闭环记录每次回答的问题、`topK`、相似度阈值、Prompt 版本、命中来源元数据与相似度、耗时、回答状态和失败类型，不保存 chunk 正文或模型回答。迁移脚本为 `deploy/mysql/init/003_rag_retrieval_log.sql`，已执行到独立数据库 `java_interview_rag`；日志查询接口具备表结构前提，日志存储故障仍只会告警且不影响回答。

固定评估题集通过 `GET /api/knowledge/evaluation-cases` 暴露 23 个困难检索问题，其中 17 题可回答、6 题应拒答；每题包含稳定标识、问题、期望逻辑来源、完整标题路径和预期回答要点。`GET /api/knowledge/evaluations/retrieval` 固定以 `topK=5`、相似度阈值 `0.0` 统计 Hit@1、Recall@5、MRR、标题路径召回和拒答准确率。独立的 3 道 PDF 结构题通过 `/api/knowledge/evaluation-cases/structure` 与 `/api/knowledge/evaluations/retrieval/structure` 验证页码、代码/表格/正文块类型和完整结构匹配，不扩大回答评估的 Chat 调用范围。`POST /api/knowledge/evaluations/answers` 会复用当前 23 题在线回答服务，以 `normalized-literal-containment-v1` 比较完整要点；该指标不等同于语义正确率，接口会产生真实模型费用并写召回日志。仓库提供相应 Markdown 与合成 PDF 语料，付费检索或回答评估必须在费用授权后运行。

仓库在 `evaluation/corpus/` 保存与题集精确来源对应的 8 份固定 Markdown 语料，并提供无第三方依赖的 `evaluation/run_retrieval_baseline.py` 编排导入与批量检索。2026-07-25 的 6 题/5 份语料结果保留在 `evaluation/results/2026-07-25-retrieval-baseline.json`，2026-09-02 的结果仍代表当时 20 题口径；当前固定集为 23 题，最新口径见 `evaluation/README.md`。语料会保留在项目独立知识库中，后续参数或检索策略变更可以直接执行同口径对照。

## 面试准备画像与首轮出题

固定 RAG 评估集只用于比较检索、Prompt 和模型版本，不是用户侧面试题库。用户侧从 `POST /api/interviews/preparations` 开始：请求必须提供 `targetRole` 和 `resumeText`，`jobDescription` 可选。服务使用 `interview-preparation-v1` Prompt 要求模型只依据输入生成结构化画像，不推断不存在的经历，不生成面试题、评分或录用判断。响应中的 `resumeEvidence`、`jobRequirements` 与 `focusAreas` 将作为后续生成可追踪面试题前的可确认草稿。

前端默认把目标岗位、简历/JD、3 到 8 的 `questionCount`、整场题数上限、Skill 和难度提交到 `POST /api/interviews/drafts`。服务先校验配置，再用 `interview-guide-multi-source-v3` 编排：有简历证据时按约 60% 先生成简历题，再把本场题面和考察目标加入排除上下文生成 40% Skill/方向题；无简历时全部生成方向题。简历分支失败会全量改走方向分支，方向分支失败保留简历题，两路失败或同场/近期历史规范化去重产生缺口时由 Skill 固定题补足。支持 Java 核心、Spring、数据库、系统设计和 RAG/AI 五类 Skill 及三档难度。每题保存主来源与辅助来源，生成题使用独立 `knowledgeQuery` 以 `topK=3`、相似度阈值 `0.60` 检索知识库；可靠命中时返回实际 chunk，没有命中时不伪造引用，单题检索异常也不会清空其余草稿。相同输入与运行契约在当前实例 30 分钟、最多 100 份草稿范围内复用；显式重新生成绕过复用，失败计算会移除以允许重试。前端正式确认时生成并在重试期间复用 `requestId`，MySQL 唯一键保证并发或重启后恢复同一会话；草稿缓存不作为正式幂等依据。前端显示目标/实际题数和不可用来源，少于 3 题不能创建正式会话。Skill、难度、题数、来源配额、每题来源、模型和 Prompt/策略版本会作为配置快照随正式会话持久化。原 `/preparations` 与 `/questions` 仍保留为独立能力入口。

`POST /api/interviews/sessions/start` 可以直接串联画像、指定数量的首轮出题和会话创建。会话只允许按当前题号顺序回答；每次提交先校验 `questionIndex` 和 `expectedVersion`，再调用模型生成结构化评价。评价只把题目已有的真实 chunk 视为可靠知识依据；没有来源时不会声称技术事实已经验证。模型认为存在关键遗漏时可以生成一条追问，服务再次检索真实知识来源并把追问追加到队列末尾，但不会超过 12 道整场上限，也不会追加完全重复的题目。

简历证据检查确认项目事实后，可以调用 `POST /api/interviews/sessions/start-project-deep-dive` 创建 `PROJECT_DEEP_DIVE` 会话。服务不调用 Chat 出题，而是固定生成基础、取舍、故障和扩展四题，并把整场题数上限锁定为 4；达到整场题数上限后，评价器直接关闭追问生成及追问知识检索。四题继续复用现有证据化评分、乐观锁、报告和最近会话恢复链路；知识检索默认关闭，只有请求显式开启时才只读补充真实资料来源。

当当前题目队列答完后，会话自动完成，并从已经保存的逐题评价确定性计算四项平均分、最多 5 条优势和最多 5 条薄弱项。页面刷新或应用重启后可以按 UUID 恢复。建表与扩展迁移包括 `004_interview_session.sql`、`005_expand_interview_session.sql`、`028_interview_session_configuration.sql` 和 `029_interview_session_request_id.sql`；`028`、`029` 已于 2026-09-18 在独立 MySQL 5.7.36 数据库执行并核对配置快照、幂等字段和唯一索引。受控真实验证已覆盖画像、多来源 8 题草稿、逐题评分、动态追问至 12 题上限、最终报告、复习再练以及进行中和已完成会话的跨重启恢复。

完成会话可以生成复习任务。服务逐题读取已经持久化的评价，只为 `gaps` 非空的回答生成任务，并从原题复制可追溯的知识来源元数据；任务 ID 和“会话 + 题号”唯一键共同保证重试幂等。`/interview` 在最后一题提交成功后调用 `POST` 生成，在恢复已完成会话时调用 `GET` 查询，生成失败可以在独立错误状态中重试且不影响最终报告。每条任务现在可直接提交独立 attempt：原 `InterviewSessionAnswer` 不修改，attempt 记录回答、评价、知识来源元数据、Prompt/rubric/model 版本和分数变化；复习模式不生成动态追问，序号冲突返回 409。会话历史接口基于这些持久化事实计算每题正式、首次、最近、最好分数和最近遗漏，并汇总已练题数、总次数与提升题数；前端不自行重算指标。迁移脚本 `deploy/mysql/init/006_interview_review_task.sql`、`007_complete_interview_review_task.sql`、`008_interview_review_attempt.sql` 与 `011_schedule_interview_review_task.sql` 按顺序提供持久化结构；任务可以从 `PENDING` 幂等推进到 `COMPLETED`，`completed_at` 只记录首次完成时间，`planned_review_date` 为可空日期。`011` 已在独立 MySQL 5.7.36 数据库执行并验证；重开、提醒、跨会话查询、写入型工具和模型自动工具调用仍属于后续闭环。

## 本地验证

```shell
.\mvnw.cmd test
cd frontend
npm run build
```

## 文档入口

- [项目定位](docs/00-项目定位.md)
- [需求边界](docs/01-需求边界.md)
- [技术方案](docs/02-技术方案.md)
- [阶段路线](docs/03-阶段路线.md)
- [当前进度](docs/04-当前进度.md)
- [开发记录](docs/05-开发记录.md)
- [参考项目调研](docs/06-参考项目调研.md)
- [技术决策记录](docs/07-技术决策记录.md)
- [项目技术栈与功能总结](docs/08-项目技术栈与功能总结.md)
- [后续开发路线](docs/09-后续开发路线.md)
- [面试准备与项目掌握要求](docs/10-面试准备与项目掌握要求.md)
- [问题清单](docs/11-问题清单.md)

开发和自动任务必须遵守 [AGENTS.md](AGENTS.md)。
