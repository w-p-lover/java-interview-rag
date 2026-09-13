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

export interface KnowledgeReviewTask {
  id: string;
  question: string;
  claim: string;
  evidenceStatus: KnowledgeAnswerClaimStatus;
  sourceNumbers: number[];
  status: "PENDING" | "COMPLETED";
  plannedReviewDate: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface KnowledgeReviewEvaluation {
  technicalScore: number;
  completenessScore: number;
  communicationScore: number;
  overallScore: number;
  strengths: string[];
  gaps: string[];
  improvementSuggestion: string;
}

export interface KnowledgeReviewAttempt {
  id: string;
  taskId: string;
  answer: string;
  evaluation: KnowledgeReviewEvaluation;
  createdAt: string;
}

export interface KnowledgeReviewPathRecommendation {
  taskId: string;
  claim: string;
  question: string;
  status: "PENDING" | "COMPLETED";
  plannedReviewDate: string | null;
  attemptCount: number;
  latestOverallScore: number | null;
  bestOverallScore: number | null;
  recommendedReviewDate: string;
  intervalDays: number;
  due: boolean;
  reason: string;
}

export interface KnowledgeReviewPathResult {
  generatedFor: string;
  taskCount: number;
  recommendations: KnowledgeReviewPathRecommendation[];
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

export type KnowledgeIndexStoreStatus =
  | "CONSISTENT"
  | "MISSING_CHUNKS"
  | "EXCESS_CHUNKS"
  | "STALE_VERSIONS"
  | "UNVERIFIABLE"
  | "UNAVAILABLE";

export type KnowledgeIndexVersionStatus =
  | "MATCHED"
  | "PARTIAL_MISMATCH"
  | "ALL_MISMATCH"
  | "UNVERIFIABLE";

export type KnowledgeIndexingStatus = "INDEXING" | "READY" | "FAILED";

export interface KnowledgeIndexingStateCheck {
  tracked: boolean;
  status: KnowledgeIndexingStatus | null;
  chunkCount: number | null;
  expectedChunkCountMatched: boolean | null;
  errorType: string | null;
}

export interface KnowledgeIndexStoreCheck {
  status: KnowledgeIndexStoreStatus;
  currentVersionChunkCount: number | null;
  currentVersionMetadataMatchCount: number | null;
  staleVersionChunkCount: number | null;
  sourceChunkCount: number | null;
  expectedChunkCountMatched: boolean | null;
  indexVersionMatched: boolean | null;
  indexVersionStatus: KnowledgeIndexVersionStatus;
  errorType: string | null;
}

export interface KnowledgeIndexConsistencyResponse {
  source: string;
  documentVersion: string;
  expectedChunkCount: number | null;
  consistent: boolean;
  indexingState: KnowledgeIndexingStateCheck;
  dense: KnowledgeIndexStoreCheck;
  sparse: KnowledgeIndexStoreCheck;
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

export type KnowledgeDocumentTaskOperation = "IMPORT" | "REBUILD";
export type KnowledgeDocumentTaskStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELLED";

export interface KnowledgeDocumentTaskSubmitResponse {
  taskId: string;
  source: string;
  operation: KnowledgeDocumentTaskOperation;
  status: KnowledgeDocumentTaskStatus;
}

export interface KnowledgeDocumentTaskResponse {
  taskId: string;
  source: string;
  operation: KnowledgeDocumentTaskOperation;
  status: KnowledgeDocumentTaskStatus;
  version: string | null;
  imported: boolean;
  chunkCount: number;
  format: "MARKDOWN" | "PDF" | null;
  warningCount: number;
  errorType: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
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

export async function submitKnowledgeDocumentTask(
  file: File,
  source: string,
  operation: KnowledgeDocumentTaskOperation,
): Promise<KnowledgeDocumentTaskSubmitResponse> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("source", source);
  const path = operation === "IMPORT"
    ? "/knowledge/documents/import-tasks"
    : "/knowledge/documents/rebuild-tasks";
  const response = await request.post<KnowledgeDocumentTaskSubmitResponse>(path, formData);
  return response.data;
}

export async function getKnowledgeDocumentTask(taskId: string): Promise<KnowledgeDocumentTaskResponse> {
  const response = await request.get<KnowledgeDocumentTaskResponse>(`/knowledge/documents/tasks/${taskId}`);
  return response.data;
}

export async function cancelKnowledgeDocumentTask(taskId: string): Promise<KnowledgeDocumentTaskResponse> {
  const response = await request.patch<KnowledgeDocumentTaskResponse>(`/knowledge/documents/tasks/${taskId}/cancel`);
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
  signal?: AbortSignal,
): Promise<void> {
  const response = await fetch("/api/knowledge/answer/stream", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
    body: JSON.stringify({ question, history }),
    signal,
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

export async function inspectKnowledgeDocumentConsistency(
  source: string,
): Promise<KnowledgeIndexConsistencyResponse> {
  const response = await request.get<KnowledgeIndexConsistencyResponse>(
    "/knowledge/documents/consistency",
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

export async function createKnowledgeReviewTask(question: string, claim: KnowledgeAnswerClaimEvidence): Promise<KnowledgeReviewTask> {
  const response = await request.post<KnowledgeReviewTask>("/knowledge/review-tasks", {
    question, claim: claim.claim, evidenceStatus: claim.status, sourceNumbers: claim.sourceNumbers,
  });
  return response.data;
}

export async function getKnowledgeReviewTasks(): Promise<KnowledgeReviewTask[]> {
  const response = await request.get<KnowledgeReviewTask[]>("/knowledge/review-tasks");
  return response.data;
}

export async function completeKnowledgeReviewTask(taskId: string): Promise<KnowledgeReviewTask> {
  const response = await request.patch<KnowledgeReviewTask>(`/knowledge/review-tasks/${taskId}/complete`);
  return response.data;
}

export async function scheduleKnowledgeReviewTask(taskId: string, plannedReviewDate: string | null): Promise<KnowledgeReviewTask> {
  const response = await request.patch<KnowledgeReviewTask>(`/knowledge/review-tasks/${taskId}/schedule`, { plannedReviewDate });
  return response.data;
}

export async function submitKnowledgeReviewAttempt(taskId: string, answer: string): Promise<KnowledgeReviewAttempt> {
  const idempotencyKey = await knowledgeReviewAttemptKey(taskId, answer);
  const response = await request.post<KnowledgeReviewAttempt>(`/knowledge/review-tasks/${taskId}/attempts`, { answer }, {
    timeout: 120_000,
    headers: { "Idempotency-Key": idempotencyKey },
  });
  return response.data;
}

async function knowledgeReviewAttemptKey(taskId: string, answer: string): Promise<string> {
  const input = new TextEncoder().encode(`${taskId}\u0000${answer.trim()}`);
  if (typeof crypto !== "undefined" && crypto.subtle) {
    const digest = await crypto.subtle.digest("SHA-256", input);
    return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
  }
  let hash = 2166136261;
  for (const byte of input) hash = Math.imul(hash ^ byte, 16777619);
  return `fallback-${(hash >>> 0).toString(16)}`;
}

export async function getKnowledgeReviewAttempts(taskId: string): Promise<KnowledgeReviewAttempt[]> {
  const response = await request.get<KnowledgeReviewAttempt[]>(`/knowledge/review-tasks/${taskId}/attempts`);
  return response.data;
}

export async function getKnowledgeReviewPath(): Promise<KnowledgeReviewPathResult> {
  const response = await request.get<KnowledgeReviewPathResult>("/knowledge/review-path");
  return response.data;
}
