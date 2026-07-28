# PDF 简历正文提取设计

## 目标

让 `/interview` 的简历和可选 JD 输入直接支持带文本层的 PDF，在浏览器本地提取正文并继续复用现有 `resumeText` 与 `jobDescription` 请求。PDF 文件不上传到知识库、不写向量、不保存到 localStorage。

## 方案选择

- 采用 `pdfjs-dist 5.4.624`。该版本使用 Apache-2.0 许可证，兼容项目允许的 Node 20.19+/22 环境，不因 PDF 功能把构建门槛抬高到 Node 22.13。
- 不采用后端 PDFBox：文本型 PDF 在浏览器即可可靠解析，新增 multipart、临时文件和清理边界没有必要。
- 不在本轮加入 OCR：扫描件无法从文本层提取正文时返回明确中文错误，避免把空文本或乱码提交给模型。

## 组件与数据流

- 新增 `frontend/src/utils/interviewDocumentText.ts`，识别 `.md`、`.markdown`、`.txt`、`.pdf` 并返回正文、格式和 PDF 页数。
- PDF.js 只在选择 PDF 时动态加载；worker 使用 Vite `?url` 资源导入，避免首页同步执行解析代码。
- PDF 按页读取文本项，尊重 `hasEOL` 换行并以两个换行连接页面；页面内只归一化多余空格和行尾空白，不猜测多栏阅读顺序。
- `InterviewTextFileField` 保留文件名、读取中、成功、错误和可编辑文本框状态。成功状态对 PDF 显示页数和字符数。

## 校验与错误

- PDF 文件最大 10 MiB，避免异常大文件占用浏览器内存；Markdown/TXT 继续沿用现有行为。
- 提取后的正文继续受简历 50000 字符、JD 30000 字符上限约束。
- 没有文本层时提示“未提取到可用文字，可能是扫描件；当前暂不支持 OCR”。
- 加密、损坏或无法解析的 PDF 返回中文错误并保留原文本。
- PDF 解析失败不回退为 `File.text()`，防止把二进制内容写入面试请求。

## 验证

- 生成包含中文和英文的两页文本 PDF，并用 Poppler 渲染检查样本可读性。
- 生成只有图片、没有文本层的扫描件样本。
- 执行 `npm run build`，确认 TypeScript、Vite worker 和生产构建通过。
- 用 Playwright 验证文本 PDF 自动填充、扫描件中文错误、原 Markdown/TXT 回归和 320px 无水平溢出；不点击开始面试。
