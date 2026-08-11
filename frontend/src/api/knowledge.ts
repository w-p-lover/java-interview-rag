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

export interface KnowledgeDocumentDetailsResponse {
  source: string;
  currentVersion: string;
  versionCount: number;
  versions: KnowledgeDocumentVersionItem[];
}

export interface MarkdownDeleteResponse {
  source: string;
  deleted: boolean;
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
