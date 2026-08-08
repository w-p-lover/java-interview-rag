import request from "@/api/request";

export interface MarkdownChunk {
  order: number;
  sectionOrder: number;
  headingLevel: number;
  headingPath: string[];
  content: string;
}

export interface MarkdownImportResponse {
  source: string;
  version: string;
  imported: boolean;
  chunkCount: number;
  chunks: MarkdownChunk[];
}

export interface KnowledgeAnswerSource {
  content: string;
  source: string;
  documentVersion: string;
  chunkOrder: number;
  headingPath: string[];
  similarity: number | null;
}

export interface KnowledgeAnswerResponse {
  answer: string;
  grounded: boolean;
  sources: KnowledgeAnswerSource[];
}

export async function importMarkdown(
  file: File,
  source: string,
): Promise<MarkdownImportResponse> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("source", source);

  const response = await request.post<MarkdownImportResponse>(
    "/knowledge/documents/import",
    formData,
  );
  return response.data;
}

export async function answerKnowledgeQuestion(question: string): Promise<KnowledgeAnswerResponse> {
  const response = await request.post<KnowledgeAnswerResponse>(
    "/knowledge/answer",
    { question },
    { timeout: 60_000 },
  );
  return response.data;
}
