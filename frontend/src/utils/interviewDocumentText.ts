import { extractInterviewPdfText } from "@/api/interview";
import { resolveApiError } from "@/api/request";

const TEXT_FILE_PATTERN = /\.(md|markdown|txt)$/i;
const PDF_FILE_PATTERN = /\.pdf$/i;

export interface InterviewDocumentExtraction {
  content: string;
  format: "PDF" | "TEXT";
  pageCount?: number;
}

export async function extractInterviewDocumentText(file: File): Promise<InterviewDocumentExtraction> {
  if (TEXT_FILE_PATTERN.test(file.name)) {
    return {
      content: await file.text(),
      format: "TEXT",
    };
  }
  if (PDF_FILE_PATTERN.test(file.name)) {
    try {
      return await extractInterviewPdfText(file);
    } catch (error) {
      throw new Error(resolveApiError(error, "PDF 提取失败"));
    }
  }
  throw new Error("当前仅支持 PDF、Markdown 或 TXT 文件，DOCX 暂未开放。");
}
