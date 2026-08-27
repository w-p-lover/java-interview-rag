# 固定 RAG 评估

PDF 正文提取的独立质量基线见 `evaluation/pdf/README.md`。该基线不依赖 RAG 语料、
数据库或模型调用。

本目录保存阶段 4 的可复现评估语料。固定题集共 20 题，覆盖精确术语、相似章节、标题路径、歧义问题、无答案拒答和跨章节检索。物理路径位于 `evaluation/corpus/`，导入时使用去掉 `evaluation/corpus/` 前缀后的路径作为逻辑来源：

| 物理文件 | 逻辑来源 | 覆盖题目 |
| --- | --- | --- |
| `java/collections.md` | `java/collections.md` | HashMap 树化与查询复杂度 |
| `java/concurrent-collections.md` | `java/concurrent-collections.md` | ConcurrentHashMap 并发控制与复合操作 |
| `java/concurrency.md` | `java/concurrency.md` | 线程池任务接收、`volatile` |
| `java/jvm.md` | `java/jvm.md` | GC Roots 与可达性分析 |
| `mysql/indexes.md` | `mysql/indexes.md` | 联合索引、最左前缀与覆盖索引 |
| `mysql/transactions.md` | `mysql/transactions.md` | MVCC 一致性快照 |
| `rag/agent-basics.md` | `rag/agent-basics.md` | RAG 与 Agent 职责、工具治理边界 |
| `rag/rag-basics.md` | `rag/rag-basics.md` | 来源、`topK` 与相似度阈值 |

先以 `storage` profile 启动后端，再执行：

```shell
python evaluation/run_retrieval_baseline.py --base-url http://127.0.0.1:18080 --allow-embedding-calls
```

需要保留完整结果时显式指定输出文件：

```shell
python evaluation/run_retrieval_baseline.py --base-url http://127.0.0.1:18080 --allow-embedding-calls --output evaluation/results/YYYY-MM-DD-rag-retrieval-baseline.json
```

运行器会调用现有导入接口，再调用固定批量检索评估接口。重复执行不会重复向量化相同“来源 + 版本”；脚本不调用 Chat 模型，也不会自动删除已导入的基线资料。导入新版本和每道检索都会调用 Embedding，因此运行器默认在任何 HTTP 请求前拒绝执行，只有明确费用授权后显式提供 `--allow-embedding-calls` 才会继续。

检索报告保留原有期望来源命中数，并增加以下指标：

- `Hit@1`：17 道可回答题中，第一条结果命中期望逻辑来源的比例。
- `Mean Recall@5`：每题 Top5 命中的不同期望逻辑来源比例，再对可回答题求平均。
- `MRR`：首个期望来源排名倒数的平均值。
- 标题路径召回率：实际结果完整标题路径精确命中的比例。
- 无答案拒答准确率：3 道无答案题在生产回答阈值 `0.60` 下没有可靠候选的比例。

脚本校验题量和指标结构，不预设未经真实运行验证的质量门槛。旧的 6 题基线结果保留为历史证据，不能当作当前 20 题结果。

2026-08-13 的首轮真实困难基线使用 `text-embedding-v3`、8 份语料和 18 个 chunk，结果见 `evaluation/results/2026-08-13-rag-hard-retrieval-baseline.json`：Hit@1 为 `1.0000`、Mean Recall@5 为 `0.9706`、MRR 为 `1.0000`、标题路径召回率为 `0.9130`、无答案拒答准确率为 `1.0000`。主要缺口集中在跨主题 Map 问题漏掉普通 HashMap 来源，以及一个歧义 Map 问题漏掉“扩容与负载因子”标题；当前没有证据支持直接引入 rerank。

## 查询拆分对照

可以在同一语料和指标口径下显式比较原查询与 `deterministic-clause-rrf-v1`：

```shell
python evaluation/run_retrieval_baseline.py --base-url http://127.0.0.1:18080 --allow-embedding-calls --compare-query-decomposition
```

候选策略不调用 Chat：它保留原问题，再从多句、多子句和带“分别/各自/两者”的协调结构中确定性生成最多 3 个额外查询，分别取 Top5 后按 chunk 身份去重并使用 Reciprocal Rank Fusion 合并为最终 Top5。当前固定题集会拆分 13 题并增加 29 次查询；对照响应同时返回原查询报告、候选报告、逐题查询计划、拆分题数和额外查询数，每题原查询只执行一次并被两份报告复用。

该策略当前只存在于评估接口 `/api/knowledge/evaluations/retrieval/query-decomposition`，没有接入资料检索、RAG 回答或面试链路。运行对照会为查询产生真实 Embedding 调用，必须显式授权费用；在真实结果同时改善多证据召回、保持 Hit@1/MRR 且不降低无答案拒答准确率前，不得把它写成生产能力。

## 回答要点覆盖

基线资料导入后，可以显式执行回答评估：

```shell
curl.exe -X POST http://127.0.0.1:18080/api/knowledge/evaluations/answers
```

接口会依次调用 20 道固定题目的现有 RAG 回答链路，每题写入正常的召回日志；无答案题在没有可靠来源时不调用 Chat，若错误命中可靠候选仍可能调用，因此整批最多产生 20 次 Chat 调用。报告使用 `normalized-literal-containment-v1`：回答和预期要点统一转为小写并移除非字母数字字符，再判断回答是否包含完整要点。该结果只表示可重复的字面覆盖率，不能替代语义正确性或人工评审。该接口会产生真实模型费用，必须显式授权后再执行。

## 面试回答评分

固定面试回答评分不依赖本目录的 RAG 语料，可在 `storage` profile 下显式执行：

```shell
curl.exe -X POST http://127.0.0.1:18080/api/interviews/evaluations/answers
```

接口依次执行五个脱敏样本。空回答在服务端拒绝，其余四项各产生一次 Chat 调用；响应只返回分数、知识依据状态、回答证据条数和知识证据条数，不返回回答、知识正文或引文。每次调用生成独立 `baselineRunId`，可以通过 `GET /api/interviews/evaluations/logs?mode=BASELINE&baselineRunId=...` 恢复分数、耗时和失败类型。评分链路当前不记录 token，不能从 HTTP 请求数推算 provider 计费回合。

## 简历证据审计

十一类固定简历证据审计不依赖 RAG 语料，但会调用 Chat 模型。明确费用授权并以 `storage` profile 启动后端后执行：

```shell
python evaluation/run_resume_audit_baseline.py --base-url http://127.0.0.1:18080 --allow-chat-calls
```

需要保存脱敏报告时显式指定输出文件：

```shell
python evaluation/run_resume_audit_baseline.py --base-url http://127.0.0.1:18080 --allow-chat-calls --output evaluation/results/YYYY-MM-DD-resume-audit-baseline.json
```

接口 `POST /api/interviews/evaluations/resume-audits` 依次执行完整简历、缺少量化结果、时间冲突、技能堆砌、项目空泛、提示注入、三种教育/任职版式、项目职责边界和多段容量十一类样本，每例产生一次 Chat 调用。原教育/任职样本只保留用户简历中的最小分类字段，新增日期前置、多段教育和多段任职主体均为合成内容。运行器默认在任何 HTTP 请求前拒绝执行，只有明确费用授权后显式提供 `--allow-chat-calls` 才会继续；每轮固定产生 11 次 Chat。报告计算审计/结构成功率、事实准确率、预期事实召回率、证据合法率、信息缺失识别率、虚构率和风险信号召回率，增加七种事实类型的准确率/召回率以及 30 条事实容量诊断，并汇总 token、平均耗时与 P95。没有配置可核对的模型单价时不估算费用。

响应和可选结果文件不含简历、确认事实、证据、风险或建议正文，只保留版本、样本 ID、计数、比率、token、耗时和失败类型。

2026-08-24 首轮 `v1` 真实报告只标代表性事实且要求证据完整相等，把拆分更细的合法事实误记为虚构，因此仅作为评估器校准证据保存在 `results/2026-08-24-resume-audit-baseline-v1.json`。补全 20 条原子事实并使用同类型证据包含匹配后，`v2` 报告保存在 `results/2026-08-24-resume-audit-baseline-v2.json`：六例审计/结构成功率和证据合法率均为 `1.00`，事实准确率 `1.00`、预期事实召回率 `0.80`、信息缺失识别率 `0.75`、虚构率 `0`、风险信号召回率 `1.00`，严格通过 `2/6`。

同日的 Prompt 单变量候选报告保存在 `results/2026-08-24-resume-audit-prompt-v2.json`。候选将事实召回率与信息缺失召回率都提高到 `1.00`、严格通过提高到 `4/6`，但事实准确率降为 `0.9091`、虚构率升为 `0.0909`，且 token 增加约 `6.9%`，因此没有替换生产 `resume-evidence-audit-v1`。后续不得通过放宽 `dataset-v2` 接受该结果，应先增加脱敏的事实类型级统计，再定位标签外事实的分类来源。

同日一份用户授权的真实校招 PDF 在 `pdfbox-layout-aware-v3` 修复右侧日期顺序后，执行了一次生产 `resume-evidence-audit-v1` 审计。脱敏摘要保存在 `results/2026-08-24-real-resume-audit-summary.json`：27 条事实全部通过字面证据校验，返回 4 条缺失、4 条风险和 4 条建议，总 token `7560`、耗时 `104550ms`。人工复核至少发现 2 条事实类型错误、个人技能和项目职责分类遗漏、1 条低价值缺失诊断，且已使用 30 条事实上限的 90%；因此证据合法率不能替代语义准确率，当前角色仍不进入前端。原 PDF、提取正文和完整审计结果均未持久化。

2026-08-25 用户授权的 `resume-evidence-audit-dataset-v4` 真实基线保存在 `results/2026-08-25-resume-audit-baseline-v4.json`：7/7 审计和结构解析成功，事实准确率 `1.0000`、预期事实召回率 `0.9677`、证据合法率 `1.0000`、信息缺失识别率 `0.7500`、虚构率 `0`、风险信号召回率 `1.0000`，严格通过 `5/7`。教育/任职样本 11 条事实全部命中，未匹配事实为 0；总 token `19714`、平均 `2816.2857`、平均耗时 `35730.7143ms`、P95 `54305ms`。该结果证明新增类型没有造成证据或事实准确率退化，但缺少信息识别仍是主要缺口。

2026-08-26 在用户明确授权下执行两轮、合计 18 次 `qwen3.7-plus` Chat。第一轮 `dataset-v5` 报告保存在 `results/2026-08-26-resume-audit-baseline-v5.json`：9/9 审计与结构成功，事实召回率 `1.0000`，但事实准确率为 `0.9688`、虚构率为 `0.0313`。类型级计数把两条额外事实定位为 `vague-project` 中可逐字证明的职责和 `prompt-injection` 中可逐字证明的项目名称，因此 v5 是固定评估标签校准失败证据，不是正式质量基线。

补齐上述两条标签后的正式 `dataset-v6` 报告保存在 `results/2026-08-26-resume-audit-baseline-v6.json`：9/9 审计与结构成功，事实准确率 `1.0000`、预期事实召回率 `0.9219`、证据合法率 `1.0000`、信息缺失识别率 `1.0000`、虚构率 `0`、风险召回率 `1.0000`，严格通过 `8/9`；最大候选事实数为 `25/30`，没有触顶样本。总 token `28391`、平均 `3154.5556`，平均耗时 `39062.2222ms`、P95 `85476ms`。项目、技术、职责、时间和结果召回率均为 `1.0000`；教育/任职样本本轮只命中 `6/11`，而同一 Prompt 的 v5 第一轮命中 `11/11`，说明单样本召回存在模型波动。该结果支持把审计作为可选证据检查接入前端，但不支持“完整简历解析”或稳定全量召回的表述。

2026-08-27 将固定集升级为 `dataset-v7`：增加日期前置和多段教育/任职两种全合成版式，并将多段容量样本从 25 条扩为恰好 30 条互不重复的原子事实。离线 Mock 回归覆盖 11 个样本、90 条预期事实，确认类型聚合和 `30/30` 触顶诊断契约。

同日用户明确授权后完成 11 次 `qwen3.7-plus` Chat，正式报告保存在 `results/2026-08-27-resume-audit-baseline-v7.json`：11/11 审计与结构成功，事实准确率 `0.9677`、预期事实召回率 `1.0000`、证据合法率 `1.0000`、信息缺失识别率 `1.0000`、虚构率 `0.0323`、风险召回率 `1.0000`，严格通过 `9/11`。教育 `16/16`、任职 `12/12` 全部命中且没有额外事实，30 条容量样本完整命中并触顶；多段教育/任职样本额外返回 2 条技术事实，项目职责样本额外返回 1 条职责事实。总 token `35165`、平均 `3196.8182`，平均耗时 `39360.4545ms`、P95 `76768ms`。该结果不支持进入分节抽取或有界修正，下一步先复核职责标签，并以单变量 Prompt 候选约束教育专业和职位不得重复归为技术事实。共享数据库未执行迁移 `015`，11 条角色请求日志均未落库，但日志失败没有影响审计主结果。
