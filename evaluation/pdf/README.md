# PDF 解析质量基线

本目录定义面试简历 PDF 提取器的仓库内确定性基线。样本由
`PdfExtractionBaselineTest` 使用 PDFBox 现场生成，不保存真实简历或个人信息，也不调用
Chat、Embedding、OCR、数据库或向量库。

当前固定覆盖八类输入：

| 样本 | 评估内容 | 当前结论 |
| --- | --- | --- |
| `single-column` | 两页单栏、页数、关键短语和顺序 | 通过 |
| `two-column` | 跨栏标题、左右两栏关键短语和阅读顺序 | `v2` 按左栏后右栏输出；`v1` 交错失败保留在历史报告 |
| `chinese-resume` | 运行时嵌入黑体的脱敏中文单栏简历 | 中文关键短语与从上到下顺序通过 |
| `date-rail` | 经历正文配轻微纵向偏移的右侧窄日期列 | 保持主体先于对应日期，不误判为双栏 |
| `skills-table` | 无边框技能键值列与中部跨行说明 | 保守保持逐行单栏，不把键值列拆成左右正文 |
| `complex-sidebar` | 左侧联系方式/技能侧栏与右侧经历主栏 | 按左侧栏后右主栏输出 |
| `mixed` | 一页文本和一页仅位图 | 保留文本并将第 2 页列为 OCR 候选 |
| `textless` | 仅位图、没有 PDF 文本操作符 | 被明确归类为 `TEXT_LAYER_MISSING` |

执行基线：

```shell
./mvnw -Dtest=PdfExtractionBaselineTest test
```

测试会把当次生成的 PDF 和报告写到 `target/pdf-extraction-baseline/`，然后与当前候选报告
`evaluation/results/2026-08-24-pdf-right-metadata-baseline.json` 比较。原始
`2026-08-12-pdf-extraction-baseline.json` 保留 `pdfbox-position-sort-v1` 的双栏失败，候选
`2026-08-13-pdf-layout-aware-baseline.json` 保留四类样本结果，
`2026-08-13-pdf-complex-resume-baseline.json` 保留 8 类 `v2` 结果；当前报告使用
`pdfbox-layout-aware-v3` 和同一 `repository-deterministic-v2` 评估器，不能通过改名覆盖旧结果。

`keyPhraseHitRate` 是固定关键短语的字面命中率，`readingOrderPassed` 是固定标记的相对
顺序断言；二者不是 `pdf-inspector` 公布的 NID、TEDS 或 MHS 指标，也不能外推为真实简历
总体解析质量。中文样本运行时使用 `C:/Windows/Fonts/simhei.ttf` 生成，字体不存在时整条
基线会明确跳过，仓库不提交系统字体。`v3` 的右侧日期形态来自授权真实简历暴露的问题，但
fixture 已完全脱敏且不包含真实原文；后续仍需用新的授权样本覆盖复杂字体编码和嵌套表格。
OCR 候选页不会自动触发外部服务。
