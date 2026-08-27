# 投递前质量与结构化检索增强实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 收口简历审计类型准确率与 RAG 查询拆分实验，并依据真实失败分布只接入一个可量化的结构化检索增强能力。

**Architecture:** 先在当前 `storage` 链路上运行固定样本，冻结模型、语料和指标，只改变一个 Prompt 或检索候选。若查询拆分不能补齐跨主题证据，则优先利用现有 `source + documentVersion + headingPath + chunkOrder` 做确定性的同文档相邻章节扩展；只有固定集证明收益且拒答不退化时才接入生产回答链路。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring AI 1.0、Qdrant 1.17、DashScope `qwen3.7-plus` / `text-embedding-v3`、JUnit 5、Python 标准库评估脚本

**Spec:** `docs/09-后续开发路线.md`

## Global Constraints

- 所有项目写入只发生在 `D:\IdeaProjects\java-interview-rag`，`blog-springboot` 只读。
- 付费调用必须服务于本计划中的固定对照，不做无上限重复运行。
- 不覆盖工作区已有前端、配置、README、AGENTS 或路线文档改动。
- 新增或修改的代码注释默认使用简体中文。
- 不引入新中间件、图框架、OCR、GraphRAG 或通用 Agent Runtime。

---

### Task 1: 简历审计类型互斥对照

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditService.java`
- Test: `src/test/java/com/javainterview/rag/interview/resume/ResumeEvidenceAuditServiceTest.java`
- Create: `evaluation/results/2026-08-27-resume-audit-type-exclusivity.json`

**Interfaces:**
- Consumes: `FixedResumeEvidenceAuditDataset.VERSION = resume-evidence-audit-dataset-v7`
- Produces: 新 Prompt 版本及同一 11 样本的类型准确率对照

- [ ] 读取 `project-responsibility` 合成样本的完整模型响应，确认额外职责是否属于标签遗漏。
- [ ] 为系统 Prompt 增加教育专业、学历、公司和职位不得重复归为 `TECHNOLOGY` 的单变量类型互斥规则，并更新直接 Prompt 契约测试。
- [ ] 执行 11 样本真实 Chat 基线并保存脱敏报告；只有准确率 `1.0000`、召回率 `1.0000`、证据合法率 `1.0000`、虚构率 `0` 时保留生产 Prompt，否则回退。

### Task 2: 查询拆分与 RRF 真实对照

**Files:**
- Existing: `evaluation/run_retrieval_baseline.py`
- Create: `evaluation/results/2026-08-27-rag-query-decomposition-comparison.json`

**Interfaces:**
- Consumes: `GET /api/knowledge/evaluations/retrieval/query-decomposition`
- Produces: baseline/candidate 的 Hit@1、Recall@5、MRR、标题路径召回、拒答准确率和逐题差异

- [ ] 启动当前提交的独立 `storage` 实例并确认 MySQL、Qdrant 与固定语料可用。
- [ ] 使用 `--allow-embedding-calls --compare-query-decomposition` 执行一次 20 题真实对照并保存报告。
- [ ] 逐题定位来源和标题路径变化；若排序或拒答退化则拒绝生产接入。

### Task 3: 结构化检索技术闭环

**Files:**
- Modify: 由 Task 2 的失败类型确定的 `knowledge/vector` 或 `knowledge/rag` 最小生产服务
- Test: 对应的直接单元测试与评估测试
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/08-项目技术栈与功能总结.md`
- Modify: `docs/09-后续开发路线.md`
- Modify: `evaluation/README.md`

**Interfaces:**
- Consumes: Task 2 的逐题失败分布及现有 `source + documentVersion + headingPath + chunkOrder`
- Produces: 一个有固定指标收益、可回退且不破坏拒答边界的生产检索增强

- [x] 对候选生产符号执行 GitNexus upstream impact，记录直接调用方和受影响流程。
- [x] 先写失败测试，固定需要补齐的来源/标题路径以及不得退化的无答案样本。
- [x] 实现最小候选：优先同来源/同版本的相邻章节扩展；查询拆分对照未通过门槛，不接入生产。
- [x] 运行直接测试、全量编译和一次最小真实回答；固定集扩展前后质量对照留待下一轮，不把接线写成指标提升。
- [x] 更新事实文档、运行 `detect_changes`、精确暂存本轮文件并按独立闭环提交。
