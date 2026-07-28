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
