import pdfWorkerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url";

const PDF_MAX_BYTES = 10 * 1024 * 1024;
const TEXT_FILE_PATTERN = /\.(md|markdown|txt)$/i;
const PDF_FILE_PATTERN = /\.pdf$/i;

export interface InterviewDocumentExtraction {
  content: string;
  format: "PDF" | "TEXT";
  pageCount?: number;
}

interface PdfTextItem {
  str: string;
  hasEOL: boolean;
}

export async function extractInterviewDocumentText(file: File): Promise<InterviewDocumentExtraction> {
  if (TEXT_FILE_PATTERN.test(file.name)) {
    return {
      content: await file.text(),
      format: "TEXT",
    };
  }
  if (PDF_FILE_PATTERN.test(file.name)) {
    return extractPdfText(file);
  }
  throw new Error("当前仅支持 PDF、Markdown 或 TXT 文件，DOCX 暂未开放。");
}

async function extractPdfText(file: File): Promise<InterviewDocumentExtraction> {
  if (file.size > PDF_MAX_BYTES) {
    throw new Error("PDF 文件不能超过 10 MB。");
  }

  const pdfjs = await import("pdfjs-dist");
  pdfjs.GlobalWorkerOptions.workerSrc = pdfWorkerUrl;
  const loadingTask = pdfjs.getDocument({ data: new Uint8Array(await file.arrayBuffer()) });

  try {
    const document = await loadingTask.promise;
    const pages: string[] = [];
    for (let pageNumber = 1; pageNumber <= document.numPages; pageNumber += 1) {
      const page = await document.getPage(pageNumber);
      const textContent = await page.getTextContent();
      const pageText = normalizePageText(textContent.items);
      if (pageText) {
        pages.push(pageText);
      }
    }

    const content = pages.join("\n\n").trim();
    if (!content) {
      throw new Error("未提取到可用文字，可能是扫描件；当前暂不支持 OCR。");
    }
    return {
      content,
      format: "PDF",
      pageCount: document.numPages,
    };
  } catch (error) {
    if (error instanceof Error && error.message.includes("当前暂不支持 OCR")) {
      throw error;
    }
    if (error instanceof Error && error.name === "PasswordException") {
      throw new Error("PDF 已加密，当前无法提取正文，请先解除密码保护。", { cause: error });
    }
    throw new Error("PDF 解析失败，请确认文件完整，或改用 Markdown/TXT。", { cause: error });
  } finally {
    await loadingTask.destroy();
  }
}

function isPdfTextItem(item: unknown): item is PdfTextItem {
  return typeof item === "object"
    && item !== null
    && "str" in item
    && typeof item.str === "string"
    && "hasEOL" in item
    && typeof item.hasEOL === "boolean";
}

function normalizePageText(items: readonly unknown[]): string {
  let text = "";
  for (const item of items) {
    if (!isPdfTextItem(item) || !item.str) {
      continue;
    }
    if (needsWordSeparator(text, item.str)) {
      text += " ";
    }
    text += item.str;
    if (item.hasEOL) {
      text += "\n";
    }
  }
  return text
    .replace(/[ \t]+\n/g, "\n")
    .replace(/[ \t]{2,}/g, " ")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function needsWordSeparator(existingText: string, nextText: string): boolean {
  if (!existingText || /\s$/.test(existingText) || /^\s/.test(nextText)) {
    return false;
  }
  return /[A-Za-z0-9]$/.test(existingText) && /^[A-Za-z0-9]/.test(nextText);
}
