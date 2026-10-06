---
id: interview-guide-java-interview-rag-project-defense-v1
title: Java Interview RAG 项目深挖与面试答辩
source: interview-guide/project/java-interview-rag-project-defense.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references:
  - app/src/main/resources/skills/java/SKILL.md
  - app/src/main/resources/skills/system-design/SKILL.md
  - app/src/main/resources/skills/spring/SKILL.md
license: AGPL-3.0
transformation: 参考上游分层追问与技能训练思路，围绕本项目真实代码、接口、数据表、故障记录和评估结果独立编写项目答辩课程；未复制上游原文、Prompt、题库或源码
skill: SYSTEM_DESIGN
usage: 知识库阅读、项目检索、后端项目深挖、专项面试与复习
---

# Java Interview RAG 项目深挖与面试答辩

## 学习目标

学完后，不只要能说“我做了一个 RAG 面试系统”，还要能完成下面四件事：

1. 用 30 秒说明项目解决什么问题、核心链路和个人工作。
2. 用 90 秒讲清知识导入、混合检索、证据回答、模拟面试和复习闭环。
3. 面对一致性、失败恢复、性能、选型和线上故障追问时，能沿真实代码继续回答。
4. 清楚区分实际做过、理解但未落地和尚未掌握，不把设计设想包装成生产经验。

这不是逐字背诵稿。先记住每一节的结论，再用“问题、方案、证据、边界”四步展开。

## 一、先守住真实性边界

面试前给每个能力打上状态：

| 状态 | 回答方式 | 本项目示例 |
| --- | --- | --- |
| 实际做过 | 可以讲实现、失败、修复、指标和取舍 | Markdown/PDF 导入、Dense + BM25 + RRF、证据校验、题库与复习、attempt fencing、索引 generation、动态模型 Provider、local/S3 文件端口 |
| 理解但未在当前项目落地 | 说明为什么暂时没做，再讲适用条件和设计思路 | Redis Stream/MQ 跨进程任务、服务端实时语音、GraphRAG、Kubernetes、多实例 Worker |
| 尚未掌握 | 直接承认边界，给出排查或学习路径 | 没有真实证据的超大规模容量、生产事故数据、未做过的云上压测和多机容灾 |

推荐句式：

> 这个项目当前没有落地 X。我当时面对的约束是 Y，所以先使用 Z；如果出现 A、B 两个条件，我会再引入 X，并重点验证 C。

这种回答比“什么都做过”更可信，也更容易把追问拉回自己掌握的实现。

## 二、项目总述的三档表达

### 30 秒结论

> Java Interview RAG 是一个基于 Java 17、Spring Boot 3、Spring AI、MySQL、Qdrant、Lucene 和 Redis 的面试知识库与训练平台。它把 Markdown/PDF 资料做版本化切分和 Dense + BM25 混合检索，再用证据约束回答、模拟面试评分和复习任务形成学习闭环。我主要处理了知识索引一致性、检索与回答质量拆分、可恢复异步任务、模型路由和文件持久化这些工程问题，而不是只接一个大模型接口。

### 90 秒标准回答

> 项目的入口是知识资料。上传后先保存文件事实，再解析标题和结构化块，生成不可变文档版本和 Chunk；Dense 写入 Qdrant，Sparse 写入 Lucene。检索时两路各取候选，用 RRF 按排名融合，因此不需要直接比较余弦分数和 BM25 分数。如果一路失败，系统会显式降级为另一路；两路都失败才返回检索不可用。
>
> 回答层不会把“召回到了内容”等同于“回答正确”。它先检索，再让模型基于编号资料回答，并做 claim 级证据校验；不可靠的声明会被删除或退回资料不足。面试层可以从简历、Skill 和知识题库生成问题，逐题保存评分、追问、报告和复习任务，再通过 attempt 对比复习前后的分数。
>
> 工程上最重要的是失败后仍可继续：资料任务和题库任务保存 task/attempt，旧 Worker 的迟到写会被条件更新拒绝；Dense 和 Sparse 先构建同一 generation，核对后再切换发布指针。模型 Provider 和文件存储也做了运行时路由、版本契约和一致性检查。当前没有为了展示技术数量引入 GraphRAG、微服务或 MQ，因为单实例和现有数据规模下收益不足。

### 3 分钟完整回答

> 这个项目要解决的不是“让模型自由回答面试题”，而是把资料学习、提问、作答、评价和再练串成可验证流程。知识侧接收 Markdown 和 PDF，文件先进入对象存储并记录 SHA-256、状态和业务引用，随后解析为带标题路径、页码和块类型的 Chunk。文档内容计算不可变版本，MySQL 保存来源、版本、目录和索引状态，Qdrant 保存 Dense 向量，Lucene 保存 BM25 倒排索引。
>
> 检索侧保留 Dense、Sparse 和 Hybrid 三种可比较入口。Hybrid 为两路各取最多 `topK * 4` 个候选，以 `source + documentVersion + chunkOrder` 去重，用 `1 / (60 + rank)` 累加 RRF 分数。这样融合的是排名而不是不同量纲的原始分数。真实固定 DEV 集上，Sparse 和 Hybrid 的来源命中、Hit@1、MRR 都达到 1.0，但 Hybrid 的拒答准确率和延迟更差，所以当时没有把 Hybrid 的单次结果宣传成全面更优，而是保存了 REJECTED 决策。
>
> 回答侧把检索质量、回答质量和引用质量分开。`RagAnswerService` 先取得资料，再调用 Chat；`RagAnswerEvidenceValidator` 检查回答中的声明是否有来源支持。流式回答也按句子边界检查，不支持的内容不会继续作为可靠答案输出。只有 grounded 回答才写缓存；Redis 故障会旁路，不影响主回答。日志记录检索策略、是否降级、来源元数据、耗时和失败类型，但不保存 Chunk 正文和模型回答。
>
> 面试侧支持简历证据审计、多来源出题、逐题评分、动态追问、最终报告和复习 attempt。会话更新带版本号，过期提交会冲突；题量达到上限时在调用评价器前关闭追问，避免已经完成评分后又因为无用检索失败。复习不是覆盖原回答，而是保存新的 attempt、评价快照、来源和分数差。
>
> 最难的工程边界是 MySQL、Qdrant、Lucene 和外部模型不能放进一个本地事务。项目使用 taskId 保持业务任务身份，用 attemptId 表示当前执行所有权，再用 generation 隔离本次索引构建。新 generation 只有在两路索引写入并核对成功后才能发布；失败时旧 generation 继续可读。这个方案不是分布式事务，也不是 exactly once，而是至少一次执行配合幂等、fencing 和原子发布，让业务结果只由当前执行生效。
>
> 当前架构仍是模块化单体。没有落地 GraphRAG、通用 Agent、MCP、微服务或 Kubernetes，因为当前首要瓶颈是内容质量、检索拒答和用户真实作答能力。等到出现多实例 Worker、跨服务独立扩缩容、图关系查询或外部工具生态后，再按真实约束引入对应能力。

## 三、从业务流程追到真实入口

### 1. 知识导入与不可变版本

业务流程：

```text
上传文件
-> FileAssetService 保存 Hash、对象和状态
-> KnowledgeStoredFileParser 解析 Markdown/PDF
-> KnowledgeDocumentTaskService 异步执行
-> MarkdownContentImporter 解析标题、切 Chunk、构建目录
-> MySQL 登记来源与不可变版本
-> Dense/Sparse 构建并发布
-> 资料目录、检索、问答和题库共同消费当前版本
```

核心入口：

- `MarkdownDocumentController`：资料上传、异步导入、任务查询、详情、下载、重建、回退和淘汰。
- `FileAssetService`：10 MiB 限制、SHA-256 去重、`UPLOADING -> READY/FAILED`、下载 Hash 校验和有引用删除保护。
- `KnowledgeDocumentTaskService`：有界执行、超时、排队取消、重启接管和 attempt 条件终态。
- `MarkdownContentImporter`：版本登记、标题解析、Chunk、目录、索引失败补偿和旧版本清理。
- 表：`t_file_asset`、`t_knowledge_document_version`、`t_knowledge_document_task`、`t_knowledge_indexing_state`。

必须讲清的失败边界：

- 数据库登记成功但索引失败时，导入器删除本次版本索引并撤销登记；不能把半成品当成当前版本。
- 同来源同内容再次导入时，以不可变版本短路，不重复向量化。
- 文件元数据存在但对象丢失、长度变化或 Hash 不一致时，下载明确失败，不返回未经核对的内容。
- 数据库事务只能保护 MySQL，不能自动回滚 Qdrant、Lucene 和对象存储，因此要依靠补偿、状态和巡检。

### 2. Dense、BM25 与 RRF

#### 30 秒结论

> Dense 擅长语义相近，BM25 擅长类名、配置项和错误码等精确词。项目让两路独立召回，再用 RRF 融合排名，避免直接混合不同量纲的分数；单路故障可降级，两路都失败才拒绝服务。

#### 90 秒标准回答

> `MarkdownHybridSearchService` 为每路最多取 `min(20, topK * 4)` 个候选，用来源、文档版本和 Chunk 顺序确定同一结果。一个候选在 Dense 排第 r1、Sparse 排第 r2 时，分数是 `1/(60+r1) + 1/(60+r2)`。排序优先 RRF，平分时按首次出现顺序稳定处理。Dense 失败时返回 `SPARSE_ONLY`，Sparse 失败时返回 `DENSE_ONLY`，响应同时带 `degraded` 和原因，调用方能区分正常 Hybrid 与降级结果。

#### 3 分钟追问展开

> 直接对余弦相似度和 BM25 分数加权不稳，因为两者量纲、分布和查询变化都不同，需要额外标定。RRF 只依赖名次，解释简单，适合第一版融合。代价是它忽略了原始分数间距，所以项目保留三种检索的固定评估，而不是默认认为混合一定更好。
>
> 评估层把可回答题和无答案题分开，至少观察来源命中、Hit@1、MRR、Precision@5、噪声、拒答准确率和 P95。一次真实 DEV 对照中，Sparse 的拒答准确率为 1.0，Hybrid 为 0.3333，Hybrid P95 也明显更高，因此候选被拒绝。这说明“召回更多”可能带来更多噪声，生产决策必须同时看拒答和延迟。
>
> 当前回答链路能够消费 Hybrid 并感知降级，但是否把某个策略作为默认值仍由固定数据集和人工采用结论决定。没有做的是在线学习排序、Cross-Encoder 重排和大规模 A/B；如果面试官追问，应说明这些是后续候选，不是现有经验。

代码入口：`MarkdownSimilaritySearchService`、`LuceneMarkdownChunkIndex`、`MarkdownHybridSearchService`、`RagRetrievalEvaluationService`、`RagRetrievalExperimentService`。

### 3. 检索正确不等于回答正确

#### 30 秒结论

> 项目把检索、回答和引用拆开评估。召回命中只能证明资料找到了，模型仍可能遗漏、误读或无依据扩写，所以回答还要做 claim 级证据校验，并把不可靠答案降为资料不足。

#### 90 秒标准回答

> `RagAnswerService` 使用固定 topK 和阈值检索，给来源编号后调用 Chat。模型返回后，`RagAnswerEvidenceValidator` 检查每个可验证声明的引用和支持关系。全部可靠才返回 `grounded=true`；部分声明可靠时只保留支持部分；没有可靠声明时返回统一资料不足。流式路径按标点切出声明后再发送，避免先把无依据内容展示给用户。只有可靠回答才进入 Redis，缓存读写异常都旁路。

#### 3 分钟追问展开

> 这里至少有三种失败：没有召回到证据、召回到了但答案没使用、答案使用了却引用错误。日志和评估不能只用一个“成功率”覆盖它们。项目记录检索策略、降级状态、实际来源、grounded、耗时和失败类型；回答实验再看知识依据判断和引用完整率。
>
> 证据校验不是事实真理机。它只判断回答与当前资料的支持关系，资料本身可能过期，语义判断也可能误判。因此文档版本、来源可见性、固定样本和人工复核仍然必要。真实回答基线曾在固定 5 题上全部完成，但这个结果只适用于当时语料、Prompt、模型和数据集，不能外推为所有问题 100% 正确。
>
> 如果检索两路都不可用，服务返回“检索不可用”而不是让模型凭参数知识继续答；如果只有一路失败，回答会带降级策略。这个选择牺牲部分可用性，换取答案来源边界清晰。

代码入口：`RagAnswerService`、`RagAnswerEvidenceValidator`、`RagRetrievalLogStore`、`RedisRagAnswerCache`、`InterviewAnswerExperimentService`。

### 4. 面试、评分与复习闭环

#### 30 秒结论

> 面试不是一次生成整份报告，而是会话状态机：多来源出题、逐题回答、评分、有限追问、报告、复习任务和新的 attempt。每一步保存快照，失败后可以从服务端事实继续。

#### 90 秒标准回答

> 组卷可以来自简历证据、目标 Skill 和知识题库。正式会话保存题目、画像、配置和简历版本；回答时同时校验当前题号和会话版本，防止重复提交或并发覆盖。评价器返回技术、完整性、表达、遗漏和可选追问。达到整场题量上限后，`InterviewSessionService` 会调用无追问评价入口，不再产生无用的追问检索。会话完成后生成复习任务，新的回答保存为独立 attempt，并计算相对正式回答和上一次练习的分差。

#### 3 分钟追问展开

> 多来源出题的难点是重复和证据一致性。简历题先生成，再把题面和考察目标作为排除上下文交给方向题分支；服务端还会做规范化去重。知识题库绑定精确资料版本，题目只有审核为 ACTIVE 才能进入专项会话；资料版本变化后旧题会变成 STALE，避免新资料配旧答案。
>
> 评分本身是概率输出，所以项目保存 Prompt、Rubric、模型契约和知识来源。评分稳定性实验对同一固定题重复评分，比较通过率、知识依据判断、平均分、波动和 P95，再人工决定采用或回退。复习分数提高只证明这道题在当前 Rubric 下改善，不代表整体能力已经掌握。
>
> 当前正式后端简历会话应保留为真实基线，不由代理代答。训练阶段可以使用独立题库会话和明确标注的示范回答，等用户准备后再回到正式会话，才能得到真实能力报告。

代码入口：`InterviewQuestionGenerationService`、`QuestionBankService`、`InterviewSessionService`、`InterviewAnswerEvaluationService`、`InterviewReviewTaskService`、`InterviewReviewAttemptService`。

### 5. 可恢复任务与索引原子发布

#### 30 秒结论

> taskId 表示稳定任务，attemptId 表示当前执行权，generation 表示本次索引快照。重试会换 attempt，旧 Worker 的状态写被拒绝；Dense 和 Sparse 写入同一临时代并核对后才发布，失败时旧代继续提供检索。

#### 90 秒标准回答

> `KnowledgeDocumentTaskService` 把 `fileId` 作为可恢复输入，重启发现 RUNNING 任务时用旧 attempt 条件换成新 attempt，并限制恢复次数。题库生成还有周期心跳和失联扫描。所有成功、失败、心跳和进度更新都校验当前 attempt。索引层再创建 generation，两路索引先写临时代，验证 Chunk 数和版本后，通过 `buildingGeneration + attemptId` 条件切换 `publishedGeneration`。

#### 3 分钟追问展开

> 仅有重试会带来旧 Worker 迟到覆盖，只有 fencing 又会让任务卡死，所以恢复协议同时需要活性和安全性。进程内 completion guard 只解决同一实例中完成、异常和超时的竞态；跨重启安全仍依赖数据库 attempt 条件更新。
>
> attempt 和 generation 不能合并。attempt 管谁有权推进任务，generation 管哪套多存储数据能被查询。即使任务状态更新正确，如果 Dense 新版和 Sparse 旧版同时可见，检索仍然不一致。发布失败时不切指针，保留旧 generation；临时代由删除、巡检或重建处理。
>
> 这不是全局事务，也不保证外部模型“只调用一次”。系统提供的是至少一次执行，配合业务唯一键、条件更新、可重放输入和发布指针，使最终业务结果只由当前 attempt 生效。若未来改用 Redis Stream 或 MQ，投递通道会变化，但 task/attempt/fencing 仍然需要保留。

代码入口：`KnowledgeDocumentTaskService`、`BankGenerationService`、`KnowledgeDocumentTaskStore`、`QuestionBankStore`、`TrackingMarkdownChunkIndexer`、`CompositeMarkdownChunkIndexer`、`KnowledgeIndexingStateStore`。

### 6. 模型 Provider 与文件存储为什么要独立治理

#### 30 秒结论

> 模型和文件都不是简单配置。模型调用要固化 Provider 版本，避免任务恢复时换了模型契约；文件要同时维护数据库元数据和对象内容，并在读取时校验大小和 Hash。

#### 90 秒标准回答

> `ModelProviderRegistry` 从数据库读取启用的 Provider 和默认 Chat/Embedding，API Key 使用 AES-256-GCM 保存，客户端缓存键是 `providerId#version`。运行调用持读锁，配置刷新持写锁，避免更新过程中继续取得旧快照。异步任务保存模型标识，恢复时若 Provider 或 Prompt 契约变化就拒绝混跑。
>
> 文件侧通过 `FileObjectStorage` 端口支持 local 和 S3。`FileAssetService` 按用途和 SHA-256 去重，元数据先进入 UPLOADING，对象成功后才 READY；读取时再次核对对象存在、长度和 Hash；仍有业务引用时禁止删除。

#### 3 分钟追问展开

> 模型 Provider 的主要风险不是“能不能请求成功”，而是密钥泄露、配置热更新竞态、Embedding 维度变化和任务恢复契约漂移。项目不会在 API、列表和日志返回密钥明文、nonce 或密文；当前默认 Provider 不能直接停用或删除。Embedding 模型变化可能要求重建索引，不能只换默认配置。
>
> 对象存储也没有跨 MySQL 的原子事务。上传失败会把文件标记 FAILED；解析失败会尝试删除无引用对象；删除先检查业务引用并进入待删除状态。共享 Hash 文件如果仍被其他版本引用就保留。S3 适配只是端口实现，不代表已经在生产云环境完成大规模验证。

代码入口：`ModelProviderRegistry`、`RoutingChatModel`、`RoutingEmbeddingModel`、`ProviderApiKeyCipher`、`FileAssetService`、`LocalFileObjectStorage`、`S3FileObjectStorage`。

## 四、关键工程取舍

| 选择 | 为什么这样做 | 放弃了什么 | 何时重新评估 |
| --- | --- | --- | --- |
| 模块化单体 | 当前团队和部署规模下，事务、调试和交付成本更低 | 服务独立扩缩容与故障隔离 | 多实例 Worker、团队边界和负载差异真实出现 |
| Qdrant + Lucene | Dense 与精确词检索各自使用成熟引擎 | 单一存储的运维简单性 | 运维成本超过质量收益或统一引擎能力足够 |
| RRF 融合 | 不需要标定不同量纲分数，容易解释 | 忽略原始分数间距 | 有稳定标注集后尝试加权、重排或学习排序 |
| MySQL 任务事实源 + 本地执行器 | 当前单 storage 实例足够，状态可查询 | 跨进程投递和独立消费扩缩容 | 多实例竞争、长 Pending、独立 Worker 成立 |
| generation 原子发布 | 失败时旧索引继续可读，隔离半成品 | 临时代清理和状态机复杂度 | 改用原生支持集合别名/快照事务的统一方案 |
| claim 级证据校验 | 降低“有来源但内容无依据”的回答 | 额外延迟和误判可能 | 有更可靠引用模型或离线标注集后替换 |
| 不做 GraphRAG | 当前资料以教程和代码说明为主，向量与字段检索更直接 | 关系型多跳推理 | 出现稳定实体关系、多跳问题和图评估集 |
| 不做通用 Agent/MCP | 主链路是受控学习和面试，不需要开放执行权 | 外部工具生态和自主编排 | 出现明确工具白名单、审计和用户确认需求 |

## 五、量化结果怎么讲才不夸大

可以使用下面的证据，但必须同时说清样本边界：

- 检索固定 DEV 20 题：Sparse 的来源命中、Hit@1、MRR 和拒答准确率均为 1.0；Hybrid 前三项相同，但拒答准确率降为 0.3333，P95 从约 36 ms 升到约 374 ms，因此候选被拒绝。结论只适用于当时固定语料和 DEV 切分，HOLDOUT 未运行。
- 回答固定 5 题基线：5/5 完成，知识依据判断和引用完整率均为 100%。这不代表所有回答正确，也不代表生产 SLA。
- 评分稳定性固定 5 题：候选温度 0.0 的平均重复波动从 5.0 降到 2.0，P95 也改善，因此人工采用；平均分下降 2.25，仍需如实报告。
- 一次跨重启索引验收：任务恢复次数为 1，attempt 更换，旧 attempt 条件写影响 0 行，Dense/Sparse 与 MySQL 当前版本计数一致。一次成功演练不等于已经完成多机容灾。

不要使用没有来源的“准确率 99%”“支持百万 QPS”“线上零故障”。如果面试官追问性能，先给当前观测口径，再说明缺少压测或生产数据。

## 六、个人职责怎么讲

按“动作 + 难点 + 证据 + 结果”组织，不要只列技术名词：

> 我负责把资料导入到面试复习的主链路做成可验证闭环。一个具体难点是 MySQL、Qdrant 和 Lucene 无法使用同一事务，我把任务执行权和索引发布权拆成 attempt 与 generation 两层，通过条件更新拒绝旧 Worker，通过临时代核对后切换发布指针，失败时保留旧索引。代码可以从 `KnowledgeDocumentTaskService`、`CompositeMarkdownChunkIndexer` 和 `KnowledgeIndexingStateStore` 追踪，开发库也做过一次跨重启验收。

如果你的参与范围没有覆盖某块，就把“我负责”改为“项目中实现了，我重点理解和维护的是……”。面试表达必须与自己的真实参与一致。

## 七、常见追问与回答骨架

### 为什么不用 Elasticsearch 一个组件解决全部检索？

先承认可行，再讲当前约束：项目已有 Qdrant 向量链路，引入 Lucene 能以较小改动补足本地 BM25；RRF 融合保留两路独立评估。代价是多一套索引一致性，所以增加 generation 和巡检。如果团队已有成熟 Elasticsearch/OpenSearch 运维，统一引擎可能更合理。

### Redis 挂了会怎样？

Redis 只承担可靠回答缓存，不是知识事实源。读写异常会计为 BYPASS 并继续检索和生成，所以可用性优先；代价是下游调用和延迟上升。当前没有 single-flight，热点 Key 在缓存失效时仍可能放大并发，这是已知边界。

### Qdrant 挂了为什么还能回答？

只有 Sparse 正常时，Hybrid 服务会显式降级为 `SPARSE_ONLY`；响应和日志都记录降级。两路都失败时不让模型脱离资料自由回答，而是返回检索不可用。是否允许降级是可靠性与完整性的业务取舍。

### 如何处理模型超时和重复调用？

同步回答有整体截止时间和有界执行器；题库等异步任务持久化任务契约、attempt 和已成功结果，恢复时只补缺口。超时不代表底层调用一定停止，所以旧 attempt 的迟到结果必须被条件写拒绝。模型调用本身可能重复，不能宣称 exactly once。

### 为什么题目达到上限后还需要服务端判断？

客户端只负责交互，真正的一致性边界在服务端。`InterviewSessionService` 在评价前计算是否允许追问；达到上限后调用无追问评价方法，避免模型仍返回追问并触发额外检索或费用。

### 文档更新后旧题为什么不能继续直接使用？

题目、参考答案和证据绑定精确文档版本。当前版本变化后旧题标记为 STALE，防止新资料与旧证据混用。历史会话保留题目快照，因此复盘不受资料更新影响。

## 八、代码导航清单

| 面试概念 | 本地入口 | 继续追踪 |
| --- | --- | --- |
| 文件与不可变版本 | `FileAssetService`、`MarkdownContentImporter` | `t_file_asset`、`t_knowledge_document_version` |
| Chunk 与结构元数据 | `MarkdownHeadingParser`、`MarkdownSectionChunker` | `KnowledgeDocumentOutlineBuilder`、PDF parser |
| Dense 检索 | `MarkdownSimilaritySearchService` | Qdrant indexer/inspector |
| Sparse 检索 | `MarkdownSparseSearchService` | `LuceneMarkdownChunkIndex` |
| RRF 与降级 | `MarkdownHybridSearchService` | `/api/knowledge/search/hybrid` |
| 回答与证据 | `RagAnswerService` | `RagAnswerEvidenceValidator`、召回日志 |
| 题库与版本证据 | `QuestionBankService` | `BankSources`、`t_knowledge_bank_question` |
| 会话并发控制 | `InterviewSessionService` | `t_interview_session.version` |
| 评分与追问 | `InterviewAnswerEvaluationService` | Rubric、Prompt、模型契约 |
| 复习 attempt | `InterviewReviewAttemptService` | `t_interview_review_attempt` |
| 任务恢复 | `KnowledgeDocumentTaskService`、`BankGenerationService` | attempt、心跳、恢复上限 |
| 索引发布 | `CompositeMarkdownChunkIndexer` | `t_knowledge_indexing_state` |
| 模型动态路由 | `ModelProviderRegistry` | `RoutingChatModel`、`RoutingEmbeddingModel` |

## 九、三道项目深挖练习

### 主问题 1：架构与证据

请从一次资料上传开始，完整说明它如何成为可检索、可回答、可出题的当前版本；至少指出两个不能只靠数据库事务解决的一致性边界。

参考回答要点：文件 Hash 与对象状态、解析与 Chunk、不可变版本、MySQL 登记、Dense/Sparse generation、发布指针、资料目录、回答和题库的当前版本消费；对象存储与 MySQL、Qdrant/Lucene 与 MySQL 都不是本地事务，需要状态、补偿、巡检和原子发布。

固定追问：MySQL 已登记版本但 Sparse 写入失败时，为什么不能直接让 Dense 新版先对外提供服务？

### 主问题 2：失败恢复与一致性

资料索引任务在模型或向量写入期间发生应用重启，旧 Worker 恢复后又迟到返回。请说明 taskId、attemptId、generationId 各自的职责，以及系统如何保证旧结果不能覆盖新结果。

参考回答要点：稳定任务身份、执行所有权、索引快照身份；持久 fileId、条件接管、恢复上限、所有状态写校验 attempt、两路写临时代、核对后条件发布、失败保留旧代；至少一次执行不等于 exactly once。

固定追问：如果未来把执行器替换成 Redis Stream，哪些可靠性协议仍然不能删除？

### 主问题 3：技术取舍与未采用方案

为什么项目选择 Dense + BM25 + RRF，却没有直接加入 Cross-Encoder、GraphRAG、通用 Agent、微服务和 MQ？请给出当前证据、放弃的收益和未来触发条件。

参考回答要点：语义与精确词互补、不同分数量纲、RRF 解释性、固定评估发现 Hybrid 可能降低拒答并增加延迟；当前瓶颈和规模不支持堆技术；分别说明重排标注集、关系型多跳问题、受控工具需求、服务独立扩缩容和跨进程 Worker 等触发条件。

固定追问：如果 Hybrid 的 Hit@1 不变但拒答准确率下降、P95 上升，你会采用它吗？为什么？

## 十、训练方法

每道题按下面顺序练习：

1. 不看答案，用 30 秒只说结论。
2. 用 90 秒补上调用链和两个关键类。
3. 用 3 分钟加入一个真实失败、一个指标边界和一个未采用方案。
4. 对照检查点，只补漏项，不把整段答案逐字背诵。
5. 提交到专项面试，查看技术、完整性和表达评分。
6. 在复习 attempt 中只针对遗漏重答，再比较分数和仍未解决的问题。

判断真正掌握的标准不是“看懂参考答案”，而是关闭材料后仍能画出流程、指出故障落点，并在追问改变条件时调整方案。
