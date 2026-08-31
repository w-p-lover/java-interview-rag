# Phase11-PDF结构检索真实基线

## 升级内容

- 在 9 份竞争语料环境下，使用授权的 `text-embedding-v3` 完成一次 3 题 PDF 结构检索真实基线。
- 合成两页 PDF 生成 6 个结构 Chunk；3 道题全部首位命中目标来源和目标结构。
- 来源命中率、Hit@1、Mean Recall@5、MRR、标题路径召回、页码召回、内容块类型召回和“来源 + 标题路径 + 页码 + 块类型”联合召回均为 `1.0000`。
- 评估只调用 Embedding，不调用 Chat；基线完成后删除合成 PDF 来源，避免测试资料残留。

## 直接相关文件

- `evaluation/results/2026-08-31-pdf-structure-retrieval-baseline.json`
- `evaluation/README.md`
- `docs/04-当前进度.md`
- `docs/05-开发记录.md`
- `docs/07-技术决策记录.md`
- `docs/08-项目技术栈与功能总结.md`
- `docs/09-后续开发路线.md`

## 验证

- 以 `storage` profile 启动当前提交，运行一次：

  `python evaluation/run_retrieval_baseline.py --base-url http://127.0.0.1:18080 --allow-embedding-calls --evaluate-structure --output evaluation/results/2026-08-31-pdf-structure-retrieval-baseline.json`

- 评估报告校验通过：3/3 结构题完成，3/3 来源命中，3/3 首位命中，所有结构字段和联合结构均命中。
- 完成后删除 `java/structured-java-notes.pdf` 来源，并确认详情接口返回 404、目录中来源数恢复为 9。

## 边界与下一步

- 结果只证明当前合成 PDF、固定查询和 `text-embedding-v3` 的结构召回，不代表复杂 PDF、图片语义、合并单元格或 OCR 的通用准确率。
- 因固定样本没有结构字段缺口，本轮不增加块类型权重、章节聚合、轻量文档结构图或 GraphRAG。
- 下一步继续沿投递前收口路线推进；若无新的结构失败样本，不重复运行同一评估集。
