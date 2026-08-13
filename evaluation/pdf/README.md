# PDF 解析质量基线

本目录定义面试简历 PDF 提取器的仓库内确定性基线。样本由
`PdfExtractionBaselineTest` 使用 PDFBox 现场生成，不保存真实简历或个人信息，也不调用
Chat、Embedding、OCR、数据库或向量库。

当前固定覆盖四类输入：

| 样本 | 评估内容 | 当前结论 |
| --- | --- | --- |
| `single-column` | 两页单栏、页数、关键短语和顺序 | 通过 |
| `two-column` | 跨栏标题、左右两栏关键短语和阅读顺序 | `v2` 按左栏后右栏输出；`v1` 交错失败保留在历史报告 |
| `mixed` | 一页文本和一页仅位图 | 保留文本并将第 2 页列为 OCR 候选 |
| `textless` | 仅位图、没有 PDF 文本操作符 | 被明确归类为 `TEXT_LAYER_MISSING` |

执行基线：

```shell
./mvnw -Dtest=PdfExtractionBaselineTest test
```

测试会把当次生成的 PDF 和报告写到 `target/pdf-extraction-baseline/`，然后与当前候选报告
`evaluation/results/2026-08-13-pdf-layout-aware-baseline.json` 比较。原始
`2026-08-12-pdf-extraction-baseline.json` 保留 `pdfbox-position-sort-v1` 的双栏失败，候选
`pdfbox-layout-aware-v2` 在相同评估器下记录双栏修复和混合页识别，不能通过改名覆盖旧结果。

`keyPhraseHitRate` 是固定关键短语的字面命中率，`readingOrderPassed` 是固定标记的相对
顺序断言；二者不是 `pdf-inspector` 公布的 NID、TEDS 或 MHS 指标，也不能外推为真实简历
总体解析质量。下一步应加入脱敏真实简历、中文字体编码、日期侧标和表格样本，再决定是否
继续增强本地版面算法；OCR 候选页不会自动触发外部服务。
