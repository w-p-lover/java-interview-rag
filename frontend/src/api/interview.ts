import request from "@/api/request";

export type InterviewFocusSource = "TARGET_ROLE" | "RESUME" | "JOB_DESCRIPTION";

export interface InterviewFocusArea {
  topic: string;
  reason: string;
  sources: InterviewFocusSource[];
}

export interface InterviewPreparationProfile {
  candidateSummary: string;
  resumeEvidence: string[];
  jobRequirements: string[];
  focusAreas: InterviewFocusArea[];
}

export interface InterviewKnowledgeSource {
  content: string;
  source: string;
  documentVersion: string;
  chunkOrder: number;
  headingPath: string[];
  similarity: number | null;
}

export interface InterviewQuestion {
  question: string;
  objective: string;
  basisSources: InterviewFocusSource[];
  knowledgeQuery: string;
  knowledgeGrounded: boolean;
  knowledgeSources: InterviewKnowledgeSource[];
}

export interface InterviewAnswerEvaluation {
  technicalScore: number;
  completenessScore: number;
  communicationScore: number;
  overallScore: number;
  strengths: string[];
  gaps: string[];
  improvementSuggestion: string;
  knowledgeGrounded: boolean;
  answerEvidence: string[];
  knowledgeEvidence: InterviewKnowledgeEvidence[];
}

export interface InterviewKnowledgeEvidence {
  source: string;
  documentVersion: string;
  chunkOrder: number;
  headingPath: string[];
  similarity: number | null;
  quote: string;
}

export interface InterviewSessionAnswer {
  questionIndex: number;
  answer: string;
  evaluation: InterviewAnswerEvaluation;
  answeredAt: string;
}

export interface InterviewSessionReport {
  answeredQuestions: number;
  averageTechnicalScore: number;
  averageCompletenessScore: number;
  averageCommunicationScore: number;
  overallAverageScore: number;
  strengths: string[];
  gaps: string[];
  completedAt: string;
}

export type InterviewSessionMode = "STANDARD" | "PROJECT_DEEP_DIVE";

export interface InterviewSession {
  id: string;
  targetRole: string;
  mode: InterviewSessionMode;
  profile: InterviewPreparationProfile;
  questions: InterviewQuestion[];
  maxQuestionCount: number;
  answers: InterviewSessionAnswer[];
  currentQuestionIndex: number;
  status: "IN_PROGRESS" | "COMPLETED";
  version: number;
  report: InterviewSessionReport | null;
  createdAt: string;
  updatedAt: string;
}

export interface InterviewConversationEvent {
  type: "ANSWER_SUBMITTED" | "EVALUATION_STARTED" | "EVALUATION_COMPLETED" | "FOLLOW_UP_GENERATED" | "SESSION_COMPLETED";
  sequence: number;
  questionIndex: number;
  summary: string;
  occurredAt: string;
}

export interface InterviewReviewKnowledgeSource {
  source: string;
  documentVersion: string;
  chunkOrder: number;
  headingPath: string[];
  similarity: number | null;
}

export interface InterviewReviewTask {
  id: string;
  sessionId: string;
  questionIndex: number;
  question: string;
  objective: string;
  gaps: string[];
  improvementSuggestion: string;
  knowledgeGrounded: boolean;
  knowledgeSources: InterviewReviewKnowledgeSource[];
  status: "PENDING" | "COMPLETED";
  plannedReviewDate: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface InterviewReviewTaskQuery {
  status?: InterviewReviewTask["status"];
  plannedDateFrom?: string;
  plannedDateTo?: string;
}

export interface InterviewReviewAttempt {
  id: string;
  sessionId: string;
  reviewTaskId: string;
  questionIndex: number;
  attemptNumber: number;
  answer: string;
  evaluation: InterviewAnswerEvaluation;
  knowledgeGrounded: boolean;
  knowledgeSources: InterviewReviewKnowledgeSource[];
  promptVersion: string;
  rubricVersion: string;
  modelName: string;
  originalOverallScore: number;
  previousOverallScore: number | null;
  scoreDeltaFromOriginal: number;
  scoreDeltaFromPrevious: number | null;
  createdAt: string;
}

export interface InterviewReviewTaskHistory {
  reviewTaskId: string;
  questionIndex: number;
  attemptCount: number;
  originalOverallScore: number;
  firstOverallScore: number | null;
  latestOverallScore: number | null;
  bestOverallScore: number | null;
  latestScoreDeltaFromOriginal: number | null;
  unresolvedGaps: string[];
  latestAttemptAt: string | null;
  attempts: InterviewReviewAttempt[];
}

export interface InterviewReviewSessionHistory {
  sessionId: string;
  totalTaskCount: number;
  practicedTaskCount: number;
  totalAttemptCount: number;
  improvedTaskCount: number;
  tasks: InterviewReviewTaskHistory[];
}

export interface InterviewReviewScheduleConfirmation {
  sessionId: string;
  taskId: string;
  plannedReviewDate: string;
}

export interface InterviewReviewAssistantRequest {
  sessionId: string;
  message: string;
  scheduleConfirmation?: InterviewReviewScheduleConfirmation;
}

export interface InterviewReviewAssistantResponse {
  requestId: string;
  answer: string;
}

export interface InterviewSessionStartPayload {
  targetRole: string;
  resumeText: string;
  jobDescription?: string;
  initialQuestionCount: number;
  maxQuestionCount: number;
}

export interface ProjectDeepDiveSessionStartPayload {
  targetRole: string;
  project: ResumeEvidenceConfirmedFact;
  includeKnowledge: boolean;
}

export interface InterviewDocumentExtraction {
  content: string;
  format: "PDF";
  pageCount: number;
  documentType: "TEXT_BASED" | "MIXED" | "IMAGE_BASED" | "EMPTY";
  textPageCount: number;
  imagePageCount: number;
  reorderedPageCount: number;
  pagesNeedingOcr: number[];
  warnings: string[];
  pages: InterviewPdfPageExtraction[];
}

export type ResumeEvidenceFactType =
  | "EDUCATION"
  | "EMPLOYMENT"
  | "PROJECT"
  | "TECHNOLOGY"
  | "RESPONSIBILITY"
  | "TIME"
  | "RESULT";

export interface ResumeEvidenceConfirmedFact {
  type: ResumeEvidenceFactType;
  statement: string;
  evidence: string;
}

export interface ResumeEvidenceAuditResult {
  requestId: string;
  role: "RESUME_EVIDENCE_AUDITOR";
  promptVersion: string;
  confirmedFacts: readonly ResumeEvidenceConfirmedFact[];
  missingInformation: readonly string[];
  risks: readonly string[];
  suggestions: readonly string[];
}

export type ResumeJobMatchStatus = "MATCHED" | "PARTIAL" | "MISSING" | "NEEDS_VERIFICATION";

export interface ResumeJobMatchAssessment {
  requirement: string;
  requirementEvidence: string;
  status: ResumeJobMatchStatus;
  resumeEvidence: string | null;
  explanation: string;
}

export interface ResumeJobMatchResult {
  requestId: string;
  role: "JOB_MATCH_ADVISOR";
  promptVersion: string;
  summary: string;
  assessments: readonly ResumeJobMatchAssessment[];
}

export type ProjectDeepDiveDimension = "FOUNDATION" | "TRADE_OFF" | "FAILURE" | "EVOLUTION";

export interface ProjectDeepDiveKnowledgeSource {
  content: string;
  source: string;
  documentVersion: string;
  chunkOrder: number;
  headingPath: string[];
  similarity: number | null;
  pageNumber: number | null;
  blockType: string;
}

export interface ProjectDeepDiveQuestion {
  dimension: ProjectDeepDiveDimension;
  question: string;
  objective: string;
  basisEvidence: string;
  knowledgeQuery: string;
  knowledgeGrounded: boolean;
  knowledgeSources: readonly ProjectDeepDiveKnowledgeSource[];
}

export interface ProjectDeepDiveResult {
  strategyVersion: "project-deep-dive-v1";
  project: {
    statement: string;
    evidence: string;
  };
  knowledgeIncluded: boolean;
  questions: readonly ProjectDeepDiveQuestion[];
}

export interface InterviewPdfPageExtraction {
  pageNumber: number;
  contentType: "TEXT" | "TEXT_WITH_IMAGES" | "IMAGE_ONLY" | "EMPTY";
  layout: "SINGLE_COLUMN" | "TWO_COLUMN";
  textCharacterCount: number;
  imageCount: number;
  requiresOcr: boolean;
}

export async function extractInterviewPdfText(file: File): Promise<InterviewDocumentExtraction> {
  const formData = new FormData();
  formData.append("file", file);
  const response = await request.post<InterviewDocumentExtraction>(
    "/interviews/documents/extract",
    formData,
    { timeout: 30_000 },
  );
  return response.data;
}

export async function auditResumeEvidence(
  resumeText: string,
): Promise<ResumeEvidenceAuditResult> {
  const response = await request.post<ResumeEvidenceAuditResult>(
    "/interviews/resume-audits",
    { resumeText },
    { timeout: 120_000 },
  );
  return response.data;
}

export async function analyzeResumeJobMatch(
  targetRole: string,
  jobDescription: string,
  confirmedFacts: readonly ResumeEvidenceConfirmedFact[],
): Promise<ResumeJobMatchResult> {
  const response = await request.post<ResumeJobMatchResult>(
    "/interviews/job-matches",
    { targetRole, jobDescription, confirmedFacts },
    { timeout: 120_000 },
  );
  return response.data;
}

export async function generateProjectDeepDive(
  project: ResumeEvidenceConfirmedFact,
  includeKnowledge: boolean,
): Promise<ProjectDeepDiveResult> {
  const response = await request.post<ProjectDeepDiveResult>(
    "/interviews/project-deep-dives",
    { project, includeKnowledge },
    { timeout: 30_000 },
  );
  return response.data;
}

export async function startInterviewSession(
  payload: InterviewSessionStartPayload,
): Promise<InterviewSession> {
  const response = await request.post<InterviewSession>("/interviews/sessions/start", payload, {
    timeout: 120_000,
  });
  return response.data;
}

export async function startProjectDeepDiveSession(
  payload: ProjectDeepDiveSessionStartPayload,
): Promise<InterviewSession> {
  const response = await request.post<InterviewSession>(
    "/interviews/sessions/start-project-deep-dive",
    payload,
    { timeout: 120_000 },
  );
  return response.data;
}

export async function getInterviewSession(sessionId: string): Promise<InterviewSession> {
  const response = await request.get<InterviewSession>(`/interviews/sessions/${sessionId}`);
  return response.data;
}

export async function getInterviewSessionEvents(
  sessionId: string,
): Promise<InterviewConversationEvent[]> {
  const response = await request.get<InterviewConversationEvent[]>(
    `/interviews/sessions/${sessionId}/events`,
  );
  return response.data;
}

export async function submitInterviewAnswer(
  sessionId: string,
  questionIndex: number,
  expectedVersion: number,
  answer: string,
): Promise<InterviewSession> {
  const response = await request.post<InterviewSession>(
    `/interviews/sessions/${sessionId}/answers`,
    { questionIndex, expectedVersion, answer },
    { timeout: 120_000 },
  );
  return response.data;
}

export async function submitInterviewAnswerStream(
  sessionId: string,
  questionIndex: number,
  expectedVersion: number,
  answer: string,
  onStage: (event: InterviewConversationEvent) => void,
): Promise<InterviewSession> {
  const response = await fetch(`/api/interviews/sessions/${sessionId}/answers/stream`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
    body: JSON.stringify({ questionIndex, expectedVersion, answer }),
  });
  if (!response.ok || !response.body) {
    const body = await response.json().catch(() => ({})) as { message?: string };
    throw new Error(body.message || `提交回答失败，后端返回 ${response.status}。`);
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let result: InterviewSession | null = null;
  while (true) {
    const read = await reader.read();
    buffer += decoder.decode(read.value || new Uint8Array(), { stream: !read.done });
    const frames = buffer.split("\n\n");
    buffer = frames.pop() || "";
    for (const frame of frames) {
      const eventName = frame.match(/^event:\s*(.+)$/m)?.[1]?.trim();
      const data = frame.match(/^data:\s*(.+)$/m)?.[1];
      if (!data) continue;
      const payload = JSON.parse(data) as InterviewConversationEvent | InterviewSession | { message?: string };
      if (eventName === "stage") onStage(payload as InterviewConversationEvent);
      if (eventName === "session") result = payload as InterviewSession;
      if (eventName === "error") throw new Error((payload as { message?: string }).message || "提交回答失败");
    }
    if (read.done) break;
  }
  if (!result) throw new Error("面试评价未返回完整会话结果");
  return result;
}

export async function generateInterviewReviewTasks(
  sessionId: string,
): Promise<InterviewReviewTask[]> {
  const response = await request.post<InterviewReviewTask[]>(
    `/interviews/sessions/${sessionId}/review-tasks`,
  );
  return response.data;
}

export async function getInterviewReviewTasks(
  sessionId: string,
  query: InterviewReviewTaskQuery = {},
): Promise<InterviewReviewTask[]> {
  const response = await request.get<InterviewReviewTask[]>(
    `/interviews/sessions/${sessionId}/review-tasks`,
    { params: query },
  );
  return response.data;
}

export async function completeInterviewReviewTask(
  sessionId: string,
  taskId: string,
): Promise<InterviewReviewTask> {
  const response = await request.patch<InterviewReviewTask>(
    `/interviews/sessions/${sessionId}/review-tasks/${taskId}/complete`,
  );
  return response.data;
}

export async function scheduleInterviewReviewTask(
  sessionId: string,
  taskId: string,
  plannedReviewDate: string | null,
): Promise<InterviewReviewTask> {
  const response = await request.patch<InterviewReviewTask>(
    `/interviews/sessions/${sessionId}/review-tasks/${taskId}/schedule`,
    { plannedReviewDate },
  );
  return response.data;
}

export async function getInterviewReviewHistory(
  sessionId: string,
): Promise<InterviewReviewSessionHistory> {
  const response = await request.get<InterviewReviewSessionHistory>(
    `/interviews/sessions/${sessionId}/review-tasks/history`,
  );
  return response.data;
}

export async function submitInterviewReviewAttempt(
  sessionId: string,
  taskId: string,
  answer: string,
  expectedAttemptNumber: number,
): Promise<InterviewReviewAttempt> {
  const response = await request.post<InterviewReviewAttempt>(
    `/interviews/sessions/${sessionId}/review-tasks/${taskId}/attempts`,
    { answer, expectedAttemptNumber },
    { timeout: 120_000 },
  );
  return response.data;
}

export async function askInterviewReviewAssistant(
  payload: InterviewReviewAssistantRequest,
): Promise<InterviewReviewAssistantResponse> {
  const response = await request.post<InterviewReviewAssistantResponse>(
    "/interviews/review-assistant",
    payload,
    { timeout: 120_000 },
  );
  return response.data;
}
