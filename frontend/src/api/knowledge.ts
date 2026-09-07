import request from "@/api/request";

export interface MarkdownChunk {
  order: number;
  sectionOrder: number;
  headingLevel: number;
  headingPath: string[];
  content: string;
  pageNumber: number | null;
  blockType: KnowledgeBlockType;
}

export type KnowledgeBlockType = "TEXT" | "LIST" | "CODE" | "TABLE";

export interface MarkdownImportResponse {
  source: string;
  version: string;
  imported: boolean;
  chunkCount: number;
  chunks: MarkdownChunk[];
  format: "MARKDOWN" | "PDF";
  pageCount: number | null;
  pagesNeedingOcr: number[];
  warnings: string[];
}

export interface KnowledgeAnswerSource {
  content: string;
  source: string;
  documentVersion: string;
  chunkOrder: number;
  headingPath: string[];
  similarity: number | null;
  pageNumber: number | null;
  blockType: KnowledgeBlockType;
}

export type KnowledgeAnswerClaimStatus =
  | "SUPPORTED"
  | "EVIDENCE_INSUFFICIENT"
  | "EVIDENCE_CONFLICTING"
  | "CITATION_INVALID";

export interface KnowledgeAnswerClaimEvidence {
  claim: string;
  sourceNumbers: number[];
  status: KnowledgeAnswerClaimStatus;
  anchorCoverage: number;
}

export interface KnowledgeAnswerResponse {
  answer: string;
  grounded: boolean;
  sources: KnowledgeAnswerSource[];
  claimEvidence: KnowledgeAnswerClaimEvidence[];
  retrievalStrategy: "HYBRID" | "DENSE_ONLY" | "SPARSE_ONLY";
  degraded: boolean;
  degradationReason: string | null;
}

export interface KnowledgeConversationTurn {
  question: string;
  answer: string;
}

export interface KnowledgeAnswerStreamEvent {
  type: "delta" | "complete";
  content: string;
  grounded: boolean;
  sources: KnowledgeAnswerSource[];
  claimEvidence: KnowledgeAnswerClaimEvidence[];
  retrievalStrategy: "HYBRID" | "DENSE_ONLY" | "SPARSE_ONLY";
  degraded: boolean;
  degradationReason: string | null;
}

export interface KnowledgeSearchResponse {
  query: string;
  topK: number;
  similarityThreshold: number;
  resultCount: number;
  results: KnowledgeAnswerSource[];
}

export interface KnowledgeDocumentCatalogItem {
  source: string;
  version: string;
  contentLength: number;
  registeredAt: string;
}

export interface KnowledgeDocumentCatalogResponse {
  sourceCount: number;
  documents: KnowledgeDocumentCatalogItem[];
}

export interface KnowledgeDocumentVersionItem {
  version: string;
  contentLength: number;
  registeredAt: string;
  current: boolean;
}

export interface KnowledgeDocumentOutlineNode {
  title: string;
  level: number;
  firstChunkOrder: number | null;
  directChunkCount: number;
  children: KnowledgeDocumentOutlineNode[];
}

export interface KnowledgeDocumentOutline {
  headingCount: number;
  chunkCount: number;
  roots: KnowledgeDocumentOutlineNode[];
}

export interface KnowledgeDocumentDetailsResponse {
  source: string;
  currentVersion: string;
  versionCount: number;
  outline: KnowledgeDocumentOutline | null;
  versions: KnowledgeDocumentVersionItem[];
}

export interface MarkdownDeleteResponse {
  source: string;
  deleted: boolean;
}

export interface MarkdownRebuildResponse {
  source: string;
  version: string;
  chunkCount: number;
  format: "MARKDOWN" | "PDF";
  pageCount: number | null;
  pagesNeedingOcr: number[];
  warnings: string[];
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

export async function rebuildMarkdown(
  file: File,
  source: string,
): Promise<MarkdownRebuildResponse> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("source", source);

  const response = await request.post<MarkdownRebuildResponse>(
    "/knowledge/documents/rebuild",
    formData,
  );
  return response.data;
}

export async function answerKnowledgeQuestion(question: string, history: KnowledgeConversationTurn[] = []): Promise<KnowledgeAnswerResponse> {
  const response = await request.post<KnowledgeAnswerResponse>(
    "/knowledge/answer",
    { question, history },
    { timeout: 60_000 },
  );
  return response.data;
}

export async function streamKnowledgeQuestion(
  question: string,
  onEvent: (event: KnowledgeAnswerStreamEvent) => void,
  history: KnowledgeConversationTurn[] = [],
): Promise<void> {
  const response = await fetch("/api/knowledge/answer/stream", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
    body: JSON.stringify({ question, history }),
  });
  if (!response.ok) {
    let message = "知识问答失败";
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message || `${message}，后端返回 ${response.status}。`;
    } catch {
      message = `${message}，后端返回 ${response.status}。`;
    }
    throw new Error(message);
  }
  if (!response.body) {
    throw new Error("后端未返回流式回答");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
    const frames = buffer.split("\n\n");
    buffer = frames.pop() || "";
    for (const frame of frames) {
      const data = frame
        .split("\n")
        .filter((line) => line.startsWith("data:"))
        .map((line) => line.slice(5).trimStart())
        .join("\n");
      if (data) {
        onEvent(JSON.parse(data) as KnowledgeAnswerStreamEvent);
      }
    }
    if (done) {
      break;
    }
  }
}

export async function searchKnowledge(query: string): Promise<KnowledgeSearchResponse> {
  const response = await request.get<KnowledgeSearchResponse>(
    "/knowledge/search",
    {
      params: {
        query,
        topK: 5,
        similarityThreshold: 0,
      },
    },
  );
  return response.data;
}

export async function listKnowledgeDocuments(): Promise<KnowledgeDocumentCatalogResponse> {
  const response = await request.get<KnowledgeDocumentCatalogResponse>(
    "/knowledge/documents",
  );
  return response.data;
}

export async function getKnowledgeDocumentDetails(
  source: string,
): Promise<KnowledgeDocumentDetailsResponse> {
  const response = await request.get<KnowledgeDocumentDetailsResponse>(
    "/knowledge/documents/detail",
    { params: { source } },
  );
  return response.data;
}

export async function deleteKnowledgeDocument(source: string): Promise<MarkdownDeleteResponse> {
  const response = await request.delete<MarkdownDeleteResponse>(
    "/knowledge/documents",
    { params: { source } },
  );
  return response.data;
}
