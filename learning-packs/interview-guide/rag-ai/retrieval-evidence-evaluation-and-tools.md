---
id: interview-guide-rag-retrieval-evidence-evaluation-tools-v1
title: RAG 检索、证据、评估与受控 Tool Calling
source: interview-guide/rag-ai/retrieval-evidence-evaluation-and-tools.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references: [app/src/main/resources/skills/ai-agent-dev/ai-agent-dev.md]
upstream_skill: app/src/main/resources/skills/ai-agent-dev/SKILL.md
license: AGPL-3.0
transformation: 基于上游 AI Agent 与 RAG 知识点独立扩展为端到端可信链路课程，并映射本项目 Dense/Sparse/Hybrid、claim 证据、评估和受控工具；未复制上游原文、Prompt 或源码
skill: RAG_AI
usage: 知识库阅读、RAG 问答、RAG_AI 题库、专项面试与复习
---

# RAG 检索、证据、评估与受控 Tool Calling

## 一、RAG 是多层正确性链

可靠回答依赖解析、切分、索引、召回、融合、上下文构造、生成、引用和 claim 校验。最终文本看似合理不能证明链路正确；应区分未召回、排序太低、证据不足、模型未使用证据和引用不支持 claim。

Chunk 要保存 source、documentVersion、headingPath、order、page 和 blockType，使召回能回到原文并支持结构评估。切分太大混入噪声，太小丢上下文；策略必须用固定案例和失败样本决定。

## 二、Dense、Sparse 与 Hybrid

Dense 擅长语义近似但可能忽略精确标识符，BM25 擅长术语和稀有词但对纯语义改写有限。Hybrid 先独立召回，再用 RRF 等排名融合；RRF 分数不是 cosine，不能拿同一个绝对阈值解释。单路故障可降级，但响应要暴露实际策略。

Hard Negative 用相似但错误的 Chunk 检查排序和拒答。无答案问题不能只看“是否返回空候选”，还要验证证据不足时是否可靠拒答。

## 三、生成与证据门控

Prompt 应要求仅基于编号证据回答并允许明确不足。生成后逐 claim 校验引用编号、逐字引文和来源一致性；有引用不代表引用支持结论。证据无效时可保留有依据的部分回答，其余拒绝，不能把流畅度当准确性。

缓存只保存通过证据校验的回答，Key 包含 Prompt、索引和资料版本；资料变化后旧答案不应命中。

## 四、评估要分检索和答案

检索指标包括 Hit@K、Recall@K、MRR、nDCG、Precision 和结构命中；答案指标关注 grounded、claim 支持、拒答和人工反馈。指标口径要固定数据集、语料版本、Prompt/model 版本和 Git SHA。dev 用于调整，holdout 用于最终比较，避免在同一题集反复追分。

一次整体平均分会掩盖失败层。评估结果必须能回到 query、期望 source/heading/chunk、实际排名和降级状态。

## 五、Tool Calling 的权限和状态边界

工具是模型可建议调用的受控后端函数，不等于把数据库权限交给模型。参数先做结构和业务校验，可信 sessionId 等作用域来自模型不可见的请求上下文；写工具要求用户确认与实际参数精确匹配。工具日志记录状态、耗时和错误类型，不保存不必要正文。

读工具可先落地，写工具从低风险、幂等、可回滚动作开始。多 Agent 只有在独立角色、状态协调和循环确有价值时引入；固定流程用普通 Service 或显式状态机通常更可靠。

## 六、本项目完整链路

资料导入保存快照并同时构建 Qdrant Dense 与 Lucene Sparse；Hybrid 并行召回后 RRF 融合。`RagAnswerService` 构造有界上下文、调用模型、校验 claim 引文、记录召回日志，并只缓存 grounded 回答。固定评估区分 Dense/Sparse/Hybrid、Hard Negative 和结构召回。

复习助手只注册任务列表、进展和改期工具；顶层 sessionId 进入 ToolContext，模型参数必须匹配，写入日期还需结构化确认。当前定位是受控 Tool Calling/轻量 Agent，不宣称自由多 Agent 平台。

## 七、练习题

### 主问题 1｜检索生成正确性

为什么 Hybrid RAG 不能把 BM25、cosine 和 RRF 分数当成同一尺度设置统一阈值？

参考回答：BM25 与语料词频相关且无固定上界，cosine 是向量相似度，RRF 只根据名次累计 `1/(k+rank)`。三者数值不可直接比较。应在各路内部使用适合的候选和门控，再按排名融合，并用固定数据集评估最终 Hit/Recall/拒答。

回答检查点：三种分数语义、路内门控、排名融合、固定评估和拒答。

追问：RRF 为什么能融合不同分数尺度？

追问参考：它只使用每路相对排名，不依赖原始分数可比性；代价是丢失分数间距信息。

### 主问题 2｜边界、评估与恢复

回答引用了真实 Chunk，为什么仍可能是错误回答？如何定位失败层？

参考回答：引用可能与 claim 无关、只支持部分结论或与其他证据冲突。先检查期望证据是否召回和排名，再检查上下文是否包含、模型是否使用、引文是否逐字存在、claim 是否被引文蕴含。记录 RETRIEVAL_MISSING、RANKING_LOW、EVIDENCE_INSUFFICIENT、ANSWER_UNUSED_EVIDENCE 等层次，再决定修检索、Prompt 或校验。

回答检查点：引用存在不等于支持、分层诊断、逐字引文、claim 支持和针对性修复。

追问：为什么不能把所有差评都直接加入检索评估集？

追问参考：差评可能来自生成、证据使用或用户偏好，必须人工诊断并补期望来源后才能成为检索真值。

### 主问题 3｜证据与取舍表达

复习助手的写工具为什么同时需要可信会话上下文和结构化用户确认？

参考回答：模型文本中的 sessionId 不可信，服务端顶层 sessionId 放入模型不可见上下文，工具调用必须匹配以防跨会话；写入日期还需一次请求内的确认对象与实际 task/date 精确匹配。两层分别保护作用域和用户意图，并配合业务幂等与执行日志。

回答检查点：模型参数不可信、ToolContext、作用域校验、精确确认、幂等和日志。

追问：只在系统 Prompt 中要求模型不要越权为什么不够？

追问参考：Prompt 是行为引导，不是强制访问控制；模型可能误调用或受输入影响，权限必须由确定性代码校验。
