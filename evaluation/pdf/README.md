# PDF 解析质量基线

本目录定义面试简历 PDF 提取器的仓库内确定性基线。样本由
`PdfExtractionBaselineTest` 使用 PDFBox 现场生成，不保存真实简历或个人信息，也不调用
Chat、Embedding、OCR、数据库或向量库。

当前固定覆盖三类输入：

| 样本 | 评估内容 | 当前结论 |
| --- | --- | --- |
| `single-column` | 两页单栏、页数、关键短语和顺序 | 通过 |
| `two-column` | 跨栏标题、左右两栏关键短语和阅读顺序 | 关键短语完整，但按水平行交错，阅读顺序失败 |
| `textless` | 仅位图、没有 PDF 文本操作符 | 被明确归类为 `TEXT_LAYER_MISSING` |

执行基线：

```shell
./mvnw -Dtest=PdfExtractionBaselineTest test
```

测试会把当次生成的 PDF 和报告写到 `target/pdf-extraction-baseline/`，然后与
`evaluation/results/2026-08-12-pdf-extraction-baseline.json` 比较。基线报告刻意保留当前
双栏失败，后续候选解析算法必须在同一语料和同一评估器下产生可审查的报告变化，不能只让
测试绕过已知缺口。

`keyPhraseHitRate` 是固定关键短语的字面命中率，`readingOrderPassed` 是固定标记的相对
顺序断言；二者不是 `pdf-inspector` 公布的 NID、TEDS 或 MHS 指标，也不能外推为真实简历
总体解析质量。下一步应加入脱敏真实简历、混合文本/扫描页、中文字体编码和表格样本，再决定
列检测或 OCR 路由方案。
