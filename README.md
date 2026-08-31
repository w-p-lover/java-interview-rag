# Java Interview RAG

Java 求职面试知识库与智能训练平台，基于 Java 17、Spring Boot 3，围绕求职方向、简历理解、资料辅助、模拟面试、结构化反馈和复习计划逐步建设，并按需接入 Spring AI 和向量数据库。

阶段 0 的项目治理、阶段 1 的 Markdown 资料知识库和阶段 2 的向量生命周期已经完成，阶段 3 已建立最小 RAG 回答、真实来源返回和知识不足兜底，阶段 4 已形成召回日志、固定评估集和批量评估代码闭环。当前真实检索基线的 6 道题均在首位命中期望来源，没有数据支撑提前增加混合检索或 rerank；阶段 6 的文本面试训练现已具备“一键生成画像与题目 → 逐题回答 → 结构化评分 → 必要时动态追问 → 自动完成并生成报告”的完整代码闭环，正式评分和复习评分会保存经 Java 服务端逐字验真的回答引文与知识引文。阶段 7 已把完成会话中的回答遗漏和真实知识来源沉淀为复习任务，支持任务完成、可选计划日期、会话内筛选、独立 attempt 和单会话历史；证据化评分 `v3/v2` 已使用五类固定样本完成两轮真实基线，8 次成功评分均通过证据验真。Tool Calling 已具备单会话任务查询、持久化复习进展分析、计划日期写入、无正文执行日志、助手请求日志、累计 token 记录和受限 ChatClient 助手，并使用真实模型验证查询、改期和请求级可观测性；当前会话由结构化请求绑定，三个工具不能访问本次请求之外的会话。

## 当前 Web 功能

- `POST /api/knowledge/documents/import` 接收 Markdown 文件与显式逻辑来源。
- `POST /api/knowledge/documents/rebuild` 重传当前登记版本的来源与 Markdown 文件，修复缺失或残留向量；历史版本不能直接重建为检索版本。
- `DELETE /api/knowledge/documents?source=...` 清理逻辑来源下的全部向量与版本登记。
- `GET /api/knowledge/documents` 返回每个逻辑来源当前登记的版本、内容字符数和登记时间；不返回历史版本或 Markdown 正文，也不调用模型与向量库。
- `GET /api/knowledge/documents/detail?source=...` 返回指定逻辑来源的当前版本、无正文章节目录和按新到旧排列的历史登记；只读取版本登记，不返回正文或查询向量库。
- `GET /api/knowledge/search?query=...&topK=5&similarityThreshold=0.0` 在 `storage` profile 下执行相似度检索，返回真实 chunk 正文、来源、版本、顺序、标题路径和相似度。
- `POST /api/knowledge/answer` 在 `storage` profile 下根据 `{"question":"..."}` 检索并生成回答，返回回答是否由可靠资料支撑及实际使用的来源列表。
- `GET /api/knowledge/retrieval-logs?limit=20` 在 `storage` profile 下查询独立数据库中最近 RAG 召回记录，最多返回 100 条。
- `GET /api/knowledge/evaluation-cases` 返回内置的固定评估题集，包含问题、期望逻辑来源和预期回答要点，不调用模型或数据库。
- `GET /api/knowledge/evaluation-cases/structure` 返回 3 道合成 PDF 结构检索题，显式包含期望来源、完整标题路径、页码和内容块类型，不调用模型或数据库。
- `GET /api/knowledge/evaluations/retrieval` 在 `storage` profile 下逐题执行固定题集的相似度检索，返回实际来源、期望来源命中情况和整体命中率，不调用 Chat 模型。
- `GET /api/knowledge/evaluations/retrieval/structure` 在 `storage` profile 下执行结构检索题，除来源与排序外统计标题路径、页码、块类型及四项联合匹配召回率；会产生 Embedding 检索，不调用 Chat。
- `POST /api/knowledge/evaluations/answers` 在 `storage` profile 下逐题调用现有 RAG 回答服务，返回实际来源、期望来源命中情况、命中与未命中回答要点及总体覆盖率；当前固定题集会产生 6 次 Chat 调用并写入对应召回日志。
- `POST /api/interviews/preparations` 在 `storage` profile 下接收目标岗位、简历文本和可选岗位描述，通过一次结构化模型调用返回候选人摘要、简历证据、岗位要求和面试关注领域；该接口不读取内部固定 RAG 评估题集，也不直接生成面试题。
- `POST /api/interviews/resume-audits` 对简历执行一次可选的证据检查，按教育、任职、项目、技术、职责、时间和结果返回经 Java 逐字验真的确认事实，并分开返回待补信息、人工核对风险和修改建议；`/interview` 只在当前页面内存中保留结果，检查失败不阻止直接开始面试。
- `POST /api/interviews/job-matches` 只接收岗位描述和已确认简历事实，返回已有证据、部分覆盖、尚无证据和需要核对的岗位要求；岗位原文与简历证据均由 Java 服务端逐字验真，不接收或保存原始简历。
- `GET /api/interviews/job-matches/requests?limit=20` 查询岗位匹配模型请求的脱敏元数据，包括模型、Prompt、token、耗时、状态和失败类型，不返回岗位描述、简历证据或匹配正文。
- `POST /api/interviews/questions` 在 `storage` profile 下接收目标岗位和已确认的面试准备画像，通过一次结构化模型调用生成 3 到 8 道首轮面试题；每题返回考察目标、画像依据类型、知识检索查询、可靠知识命中状态和实际 Markdown chunk 来源。
- `POST /api/interviews/sessions/start` 接收目标岗位、简历、可选 JD、初始题数与整场题数上限，一次完成画像、首轮出题和持久化会话创建；默认首轮 5 道、整场最多 8 道，整场上限可配置到 12 道。
- `POST /api/interviews/sessions` 保存已有目标岗位、确认画像和题目，创建 `IN_PROGRESS` 文本面试会话。
- `GET /api/interviews/sessions/{sessionId}` 从 MySQL 恢复完整会话、回答进度和当前题号。
- `POST /api/interviews/sessions/{sessionId}/answers` 使用当前题号和会话版本提交回答，返回技术、完整性、表达、综合分、优点、遗漏、改进建议，以及 1 到 3 条回答原文证据和最多 3 条知识资料证据；模型候选引文必须经服务端逐字包含、来源序号和来源正文校验后才能持久化。模型可追加一条绑定真实知识来源的追问。当前题目队列答完后自动生成整场报告，乐观锁与题号校验会拒绝并发或延迟重复提交。
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
- `GET /api/interviews/evaluations/logs?mode=BASELINE&baselineRunId=...&baselineCaseId=...&limit=20` 在 `storage` profile 下查询最近 1 到 100 条评价日志；两个基线标识可独立或联合筛选。响应同时提供版本汇总和按稳定样本跨运行的成功/失败、总分范围、平均值、总体方差、耗时及失败类型，查询不调用模型。
- Vue `/interview` 在简历证据检查完成且填写岗位描述后提供岗位匹配顾问；岗位描述、目标岗位或审计结果变化后旧结果标记为待更新，匹配失败不阻断开始面试。
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
- PDF 确定性质量基线已扩展为 8 类合成、脱敏输入，新增嵌入中文字体的单栏简历、右侧日期列、无边框技能表和复杂侧栏；当前 `pdfbox-layout-aware-v2` 为 8/8、文本关键短语命中率 100% 且阅读顺序全部通过。该结果只证明固定样本，不代表所有真实简历。

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
docker compose up -d mysql qdrant
```

项目也会只读导入 `D:/IdeaProjects/blog/blog-springboot/.env`。因此在已有服务器环境下，不需要复制主项目密码或 API Key；新项目会复用其中的 MySQL 主机、端口、账号、密码、Qdrant 地址和 DashScope Key，但强制使用独立数据库 `java_interview_rag` 与 collection `java-interview-rag-chunks`。如需覆盖，可使用 `JAVA_INTERVIEW_RAG_` 前缀变量。

以存储 profile 启动后端：

```shell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=storage
```

`storage` 会使用服务器 `.env` 提供的连接信息，通过 Druid、MyBatis-Plus 和 MySQL 持久化“来源 + 内容版本”登记，并使用 Spring AI 的 DashScope Embedding 与 Qdrant 客户端维护 chunk 向量。向量写入按每批最多 10 个 chunk 调用 Embedding，避免大文档超过当前 DashScope 服务端批量上限。新版本成功写入后会按结构化元数据过滤删除同来源旧向量，同时保留历史登记；手动重建要求重传精确来源和当前版本 Markdown 文件，写入失败时保留登记以便重试；显式淘汰先清理 Qdrant，再删除该来源的全部 MySQL 登记。资料目录按每个大小写敏感来源的最大登记 ID 选择当前版本，详情按 ID 倒序返回全部历史登记，并从版本登记的 `outline_json` 返回当前章节目录。新导入会保存目录快照；旧登记保持 `null`，只有当前版本向量重建成功后才补齐。部署存储 profile 前需执行 `deploy/mysql/init/014_knowledge_document_outline.sql`。版本写入失败并撤销登记后会自然回退到上一成功版本。默认 `local` profile 使用对应的进程内当前版本索引。MySQL `source` 使用 `utf8mb4_bin`，查询和删除 SQL 额外使用 `BINARY` 精确比较，以保持和 Java、Qdrant 相同的大小写敏感语义。Qdrant 启动探测失败只记录警告；MySQL 是该 profile 的必要依赖。Qdrant schema 自动初始化关闭，服务器需要预先创建 collection；MySQL 中需要预先创建 `java_interview_rag` 数据库并按顺序执行仓库内初始化 SQL。
简历证据审计和岗位匹配请求日志分别需要按顺序执行 `015_resume_evidence_audit_request_log.sql` 与 `016_resume_job_match_request_log.sql`；两张日志表都不保存简历、岗位描述、证据或模型正文。

最小检索接口允许 `topK` 取 1 到 20、相似度阈值取 0 到 1；默认阈值为 0.0，用于保留阶段 3 的原始召回基线。调用方可以提高阈值，Qdrant 没有满足条件的结果时返回空列表，不生成没有来源支撑的答案。

回答接口使用阿里百炼官方 OpenAI 兼容端点手动创建 ChatModel，默认模型为 `qwen3.7-plus`、温度为 `0.2`；DashScope 原生客户端继续只承担 `text-embedding-v3`。它固定检索前 5 条且相似度不低于 `0.60` 的资料，将每条资料编号并要求模型只根据资料回答；没有可靠来源或模型返回空内容时，统一返回“知识库中没有足够可靠的资料回答这个问题。”。受控真实调用已验证 `qwen3.7-plus` 返回依据资料的回答、资料引用和 `ANSWERED` 日志；临时资料与测试日志均已清理。

阶段 4 的首个闭环记录每次回答的问题、`topK`、相似度阈值、Prompt 版本、命中来源元数据与相似度、耗时、回答状态和失败类型，不保存 chunk 正文或模型回答。迁移脚本为 `deploy/mysql/init/003_rag_retrieval_log.sql`，已执行到独立数据库 `java_interview_rag`；日志查询接口具备表结构前提，日志存储故障仍只会告警且不影响回答。

固定评估题集通过 `GET /api/knowledge/evaluation-cases` 暴露 20 个困难检索问题，每题包含稳定标识、问题、期望逻辑来源、完整标题路径和预期回答要点。`GET /api/knowledge/evaluations/retrieval` 固定以 `topK=5`、相似度阈值 `0.0` 统计 Hit@1、Recall@5、MRR、标题路径召回和拒答准确率。独立的 3 道 PDF 结构题通过 `/api/knowledge/evaluation-cases/structure` 与 `/api/knowledge/evaluations/retrieval/structure` 验证页码、代码/表格/正文块类型和完整结构匹配，不扩大回答评估的 Chat 调用范围。`POST /api/knowledge/evaluations/answers` 仍只复用原 20 题在线回答服务，以 `normalized-literal-containment-v1` 比较完整要点；该指标不等同于语义正确率，接口会产生真实模型费用并写召回日志。仓库提供相应 Markdown 与合成 PDF 语料，真实检索仍必须在费用授权后运行。

仓库在 `evaluation/corpus/` 保存与题集精确来源对应的 5 份基线 Markdown，并提供无第三方依赖的 `evaluation/run_retrieval_baseline.py` 编排导入与批量检索。2026-07-25 使用 `text-embedding-v3` 和现有 Qdrant collection 运行真实基线，6 道题均在第 1 位命中期望来源，首位相似度为 `0.6755` 至 `0.8585`；原始报告保存在 `evaluation/results/2026-07-25-retrieval-baseline.json`。语料会保留在项目独立知识库中，后续参数或检索策略变更可以直接重复执行对照。

## 面试准备画像与首轮出题

固定 RAG 评估集只用于比较检索、Prompt 和模型版本，不是用户侧面试题库。用户侧从 `POST /api/interviews/preparations` 开始：请求必须提供 `targetRole` 和 `resumeText`，`jobDescription` 可选。服务使用 `interview-preparation-v1` Prompt 要求模型只依据输入生成结构化画像，不推断不存在的经历，不生成面试题、评分或录用判断。响应中的 `resumeEvidence`、`jobRequirements` 与 `focusAreas` 将作为后续生成可追踪面试题前的可确认草稿。

调用方确认画像后，可以把 `targetRole`、`profile` 与 3 到 8 的 `questionCount` 提交到 `POST /api/interviews/questions`。服务使用 `interview-question-v1` Prompt 生成指定数量的题目，每道题必须绑定画像中已经出现的 `TARGET_ROLE`、`RESUME`、`JOB_DESCRIPTION` 依据类型，并使用独立的 `knowledgeQuery` 以 `topK=3`、相似度阈值 `0.60` 检索知识库。可靠命中时返回实际 chunk；没有命中时题目仍由画像支撑，但 `knowledgeGrounded=false` 且知识来源为空，不伪造引用。

`POST /api/interviews/sessions/start` 可以直接串联画像、指定数量的首轮出题和会话创建。会话只允许按当前题号顺序回答；每次提交先校验 `questionIndex` 和 `expectedVersion`，再调用模型生成结构化评价。评价只把题目已有的真实 chunk 视为可靠知识依据；没有来源时不会声称技术事实已经验证。模型认为存在关键遗漏时可以生成一条追问，服务再次检索真实知识来源并把追问追加到队列末尾，但不会超过 12 道整场上限，也不会追加完全重复的题目。

当当前题目队列答完后，会话自动完成，并从已经保存的逐题评价确定性计算四项平均分、最多 5 条优势和最多 5 条薄弱项。页面刷新或应用重启后可以按 UUID 恢复。建表与扩展迁移分别为 `deploy/mysql/init/004_interview_session.sql` 和 `deploy/mysql/init/005_expand_interview_session.sql`；两份脚本已按顺序执行到独立数据库 `java_interview_rag`。受控真实验证已覆盖画像、首轮出题、逐题评分、动态追问、最终报告以及进行中和已完成会话的跨重启恢复；虚构测试会话已清理。

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

开发和自动任务必须遵守 [AGENTS.md](AGENTS.md)。
