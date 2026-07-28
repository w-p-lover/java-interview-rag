# Java Interview RAG

Java 求职面试知识库与智能训练平台，基于 Java 17、Spring Boot 3，围绕求职方向、简历理解、资料辅助、模拟面试、结构化反馈和复习计划逐步建设，并按需接入 Spring AI 和向量数据库。

阶段 0 的项目治理、阶段 1 的 Markdown 资料知识库和阶段 2 的向量生命周期已经完成，阶段 3 已建立最小 RAG 回答、真实来源返回和知识不足兜底，阶段 4 已形成召回日志、固定评估集和批量评估代码闭环。当前真实检索基线的 6 道题均在首位命中期望来源，没有数据支撑提前增加混合检索或 rerank；阶段 6 的文本面试训练现已具备“一键生成画像与题目 → 逐题回答 → 结构化评分 → 必要时动态追问 → 自动完成并生成报告”的完整代码闭环。回答要点真实批量基线仍待受控执行。

## 当前 Web 功能

- `POST /api/knowledge/documents/import` 接收 Markdown 文件与显式逻辑来源。
- `POST /api/knowledge/documents/rebuild` 重传已登记版本的来源与 Markdown 文件，修复缺失或残留向量。
- `DELETE /api/knowledge/documents?source=...` 清理逻辑来源下的全部向量与版本登记。
- `GET /api/knowledge/search?query=...&topK=5&similarityThreshold=0.0` 在 `storage` profile 下执行相似度检索，返回真实 chunk 正文、来源、版本、顺序、标题路径和相似度。
- `POST /api/knowledge/answer` 在 `storage` profile 下根据 `{"question":"..."}` 检索并生成回答，返回回答是否由可靠资料支撑及实际使用的来源列表。
- `GET /api/knowledge/retrieval-logs?limit=20` 在 `storage` profile 下查询独立数据库中最近 RAG 召回记录，最多返回 100 条。
- `GET /api/knowledge/evaluation-cases` 返回内置的固定评估题集，包含问题、期望逻辑来源和预期回答要点，不调用模型或数据库。
- `GET /api/knowledge/evaluations/retrieval` 在 `storage` profile 下逐题执行固定题集的相似度检索，返回实际来源、期望来源命中情况和整体命中率，不调用 Chat 模型。
- `POST /api/knowledge/evaluations/answers` 在 `storage` profile 下逐题调用现有 RAG 回答服务，返回实际来源、期望来源命中情况、命中与未命中回答要点及总体覆盖率；当前固定题集会产生 6 次 Chat 调用并写入对应召回日志。
- `POST /api/interviews/preparations` 在 `storage` profile 下接收目标岗位、简历文本和可选岗位描述，通过一次结构化模型调用返回候选人摘要、简历证据、岗位要求和面试关注领域；该接口不读取内部固定 RAG 评估题集，也不直接生成面试题。
- `POST /api/interviews/questions` 在 `storage` profile 下接收目标岗位和已确认的面试准备画像，通过一次结构化模型调用生成 3 到 8 道首轮面试题；每题返回考察目标、画像依据类型、知识检索查询、可靠知识命中状态和实际 Markdown chunk 来源。
- `POST /api/interviews/sessions/start` 接收目标岗位、简历、可选 JD、初始题数与整场题数上限，一次完成画像、首轮出题和持久化会话创建；默认首轮 5 道、整场最多 8 道，整场上限可配置到 12 道。
- `POST /api/interviews/sessions` 保存已有目标岗位、确认画像和题目，创建 `IN_PROGRESS` 文本面试会话。
- `GET /api/interviews/sessions/{sessionId}` 从 MySQL 恢复完整会话、回答进度和当前题号。
- `POST /api/interviews/sessions/{sessionId}/answers` 使用当前题号和会话版本提交回答，返回技术、完整性、表达、综合分、优点、遗漏和改进建议；模型可追加一条绑定真实知识来源的追问。当前题目队列答完后自动生成整场报告，乐观锁与题号校验会拒绝并发或延迟重复提交。
- 逻辑来源按大小写敏感的精确字符串区分，不能包含首尾空白。
- 后端返回内容版本、首次或重复导入状态，以及保留标题路径和顺序的 chunk。
- Vue 首页使用 Hallmark Workbench 结构表达求职面试产品主线，采用柔和的暖灰与珊瑚信号色，不伪造未实现模块的数据。
- Vue `/interview` 页面支持填写岗位、选择带文本层的 PDF、Markdown 或 TXT 简历与可选 JD、检查自动提取的正文和配置题数，逐题回答并查看即时反馈；PDF 由后端临时解析，文本粘贴保留为备用方式。会话 ID 保存在浏览器本地，刷新后可从后端恢复，完成后展示最终报告。
- 资料库页面支持选择或拖入 Markdown 学习资料、Java 技术资料和项目说明，并以“选择文件 → 版本判定 → 标题解析 → 生成切片”连接输入与结果，展示版本指纹、重复提示和 chunk 明细。
- 顶部命令面板支持点击或 `Ctrl/Cmd + K` 打开，使用方向键、Enter 和 Esc 完成区段跳转与关闭。
- 默认 `local` profile 的登记仅保存在 Spring Boot 进程内且不写向量；`storage` profile 使用 MySQL 持久化登记，并通过 DashScope Embedding 将首次导入的 chunk 批量写入 Qdrant。
- 相同来源和版本的重复导入会在向量化前短路；同一来源导入新内容时先写入新版本，再清理旧版本向量。已登记版本可以手动重建并收敛同来源其他版本和同版本多余 chunk；回答接口只将相似度不低于 0.60 的资料交给模型，未命中时不调用模型并返回知识不足。

## 文件入口与数据边界

- “资料库上传”和“模拟面试上传”是两个业务入口，不是同一次导入。底层可以复用文件读取组件，但不能复用数据生命周期。
- 资料库上传面向可长期复用的 Java 面试资料、项目技术说明和学习文档；成功导入后会版本登记、切分、向量化，并作为 RAG 回答、面试出题与评分的知识依据。
- 模拟面试上传面向候选人简历和可选 JD；文件只用于提取本场画像和岗位要求，默认不进入资料库、不生成知识向量，也不把原文件内容保存到浏览器本地。
- `/interview` 已以独立文件选择和自动正文提取作为主入口，文本层 PDF 在一次 multipart 请求中由后端 PDFBox 提取，Markdown/TXT 保持浏览器本地读取，随后继续使用既有字符串 API；文件与正文不会自动进入资料库或浏览器本地存储。扫描件会明确提示当前不支持 OCR，DOCX 仍需具备可靠解析能力后再开放。

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

```shell
cd frontend
npm install
npm run dev
```

浏览器访问 `http://127.0.0.1:5173`。Vite 会将 `/api` 代理到本地后端。

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

`storage` 会使用服务器 `.env` 提供的连接信息，通过 Druid、MyBatis-Plus 和 MySQL 持久化“来源 + 内容版本”登记，并使用 Spring AI 的 DashScope Embedding 与 Qdrant 客户端维护 chunk 向量。向量写入按每批最多 10 个 chunk 调用 Embedding，避免大文档超过当前 DashScope 服务端批量上限。新版本成功写入后会按结构化元数据过滤删除同来源旧向量；手动重建要求重传精确来源和 Markdown 文件，只允许已登记版本，写入失败时保留登记以便重试；显式删除先清理 Qdrant，再删除该来源的全部 MySQL 登记。MySQL `source` 使用 `utf8mb4_bin`，查询和删除 SQL 额外使用 `BINARY` 精确比较，以保持和 Java、Qdrant 相同的大小写敏感语义。Qdrant 启动探测失败只记录警告；MySQL 是该 profile 的必要依赖。Qdrant schema 自动初始化关闭，服务器需要预先创建 collection；MySQL 中需要预先创建 `java_interview_rag` 数据库并按顺序执行仓库内初始化 SQL。

最小检索接口允许 `topK` 取 1 到 20、相似度阈值取 0 到 1；默认阈值为 0.0，用于保留阶段 3 的原始召回基线。调用方可以提高阈值，Qdrant 没有满足条件的结果时返回空列表，不生成没有来源支撑的答案。

回答接口使用阿里百炼官方 OpenAI 兼容端点手动创建 ChatModel，默认模型为 `qwen3.7-plus`、温度为 `0.2`；DashScope 原生客户端继续只承担 `text-embedding-v3`。它固定检索前 5 条且相似度不低于 `0.60` 的资料，将每条资料编号并要求模型只根据资料回答；没有可靠来源或模型返回空内容时，统一返回“知识库中没有足够可靠的资料回答这个问题。”。受控真实调用已验证 `qwen3.7-plus` 返回依据资料的回答、资料引用和 `ANSWERED` 日志；临时资料与测试日志均已清理。

阶段 4 的首个闭环记录每次回答的问题、`topK`、相似度阈值、Prompt 版本、命中来源元数据与相似度、耗时、回答状态和失败类型，不保存 chunk 正文或模型回答。迁移脚本为 `deploy/mysql/init/003_rag_retrieval_log.sql`，已执行到独立数据库 `java_interview_rag`；日志查询接口具备表结构前提，日志存储故障仍只会告警且不影响回答。

固定评估题集通过 `GET /api/knowledge/evaluation-cases` 暴露 6 个基线问题，每题包含稳定标识、问题、期望逻辑来源和预期回答要点。`GET /api/knowledge/evaluations/retrieval` 固定以 `topK=5`、相似度阈值 `0.0` 逐题比较实际命中来源和期望来源，并返回整体命中率。`POST /api/knowledge/evaluations/answers` 复用在线回答服务逐题生成回答，以 `normalized-literal-containment-v1` 规则统一大小写并移除非字母数字字符后比较完整要点，返回命中和未命中要点、实际来源及覆盖率。该指标是可重复的字面覆盖基线，不等同于语义正确率；接口会产生真实模型调用费用并写召回日志。题集不自动导入资料；调用方需要使用对应的逻辑来源导入真实 Markdown，才能得到有意义的真实批量结果。评估结果持久化与语义评分仍待后续实现。

仓库在 `evaluation/corpus/` 保存与题集精确来源对应的 5 份基线 Markdown，并提供无第三方依赖的 `evaluation/run_retrieval_baseline.py` 编排导入与批量检索。2026-07-25 使用 `text-embedding-v3` 和现有 Qdrant collection 运行真实基线，6 道题均在第 1 位命中期望来源，首位相似度为 `0.6755` 至 `0.8585`；原始报告保存在 `evaluation/results/2026-07-25-retrieval-baseline.json`。语料会保留在项目独立知识库中，后续参数或检索策略变更可以直接重复执行对照。

## 面试准备画像与首轮出题

固定 RAG 评估集只用于比较检索、Prompt 和模型版本，不是用户侧面试题库。用户侧从 `POST /api/interviews/preparations` 开始：请求必须提供 `targetRole` 和 `resumeText`，`jobDescription` 可选。服务使用 `interview-preparation-v1` Prompt 要求模型只依据输入生成结构化画像，不推断不存在的经历，不生成面试题、评分或录用判断。响应中的 `resumeEvidence`、`jobRequirements` 与 `focusAreas` 将作为后续生成可追踪面试题前的可确认草稿。

调用方确认画像后，可以把 `targetRole`、`profile` 与 3 到 8 的 `questionCount` 提交到 `POST /api/interviews/questions`。服务使用 `interview-question-v1` Prompt 生成指定数量的题目，每道题必须绑定画像中已经出现的 `TARGET_ROLE`、`RESUME`、`JOB_DESCRIPTION` 依据类型，并使用独立的 `knowledgeQuery` 以 `topK=3`、相似度阈值 `0.60` 检索知识库。可靠命中时返回实际 chunk；没有命中时题目仍由画像支撑，但 `knowledgeGrounded=false` 且知识来源为空，不伪造引用。

`POST /api/interviews/sessions/start` 可以直接串联画像、指定数量的首轮出题和会话创建。会话只允许按当前题号顺序回答；每次提交先校验 `questionIndex` 和 `expectedVersion`，再调用模型生成结构化评价。评价只把题目已有的真实 chunk 视为可靠知识依据；没有来源时不会声称技术事实已经验证。模型认为存在关键遗漏时可以生成一条追问，服务再次检索真实知识来源并把追问追加到队列末尾，但不会超过 12 道整场上限，也不会追加完全重复的题目。

当当前题目队列答完后，会话自动完成，并从已经保存的逐题评价确定性计算四项平均分、最多 5 条优势和最多 5 条薄弱项。页面刷新或应用重启后可以按 UUID 恢复。建表与扩展迁移分别为 `deploy/mysql/init/004_interview_session.sql` 和 `deploy/mysql/init/005_expand_interview_session.sql`；两份脚本已按顺序执行到独立数据库 `java_interview_rag`。受控真实验证已覆盖画像、首轮出题、逐题评分、动态追问、最终报告以及进行中和已完成会话的跨重启恢复；虚构测试会话已清理。

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

开发和自动任务必须遵守 [AGENTS.md](AGENTS.md)。
