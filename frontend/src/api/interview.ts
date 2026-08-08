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

export interface InterviewSession {
  id: string;
  targetRole: string;
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

export interface InterviewDocumentExtraction {
  content: string;
  format: "PDF";
  pageCount: number;
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

export async function startInterviewSession(
  payload: InterviewSessionStartPayload,
): Promise<InterviewSession> {
  const response = await request.post<InterviewSession>("/interviews/sessions/start", payload, {
    timeout: 120_000,
  });
  return response.data;
}

export async function getInterviewSession(sessionId: string): Promise<InterviewSession> {
  const response = await request.get<InterviewSession>(`/interviews/sessions/${sessionId}`);
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
