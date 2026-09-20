import request from "@/api/request";
import type { InterviewDifficulty, InterviewSession, InterviewSkill } from "@/api/interview";
import type { KnowledgeAnswerSource } from "@/api/knowledge";

export type BankQuestionStatus = "DRAFT" | "ACTIVE" | "ARCHIVED" | "STALE";

export interface BankFollowUp {
  question: string;
  referenceAnswer: string;
  keyPoints: string[];
  scoringRubric: string;
}

export interface BankQuestionContent {
  category: string;
  question: string;
  topicSummary: string;
  referenceAnswer: string;
  keyPoints: string[];
  scoringRubric: string;
  followUps: BankFollowUp[];
}

export interface BankQuestion extends BankQuestionContent {
  id: string;
  generationTaskId: string | null;
  source: string;
  documentVersion: string;
  skill: InterviewSkill;
  difficulty: InterviewDifficulty;
  evidence: KnowledgeAnswerSource[];
  status: BankQuestionStatus;
  version: number;
  useCount: number;
  lastUsedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface GenerationTask {
  id: string;
  requestId: string;
  source: string;
  documentVersion: string;
  skill: InterviewSkill;
  difficulty: InterviewDifficulty;
  questionCount: number;
  followUpCount: number;
  status: "QUEUED" | "PROCESSING" | "COMPLETED" | "FAILED";
  savedCount: number;
  skippedCount: number;
  attemptId: string | null;
  promptVersion: string;
  modelName: string;
  errorCode: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface BankQuestionFilters {
  source: string;
  status?: BankQuestionStatus | "";
  difficulty?: InterviewDifficulty | "";
  category?: string;
  keyword?: string;
}

export interface BankGenerationPayload {
  requestId: string;
  source: string;
  skill: InterviewSkill;
  difficulty: InterviewDifficulty;
  questionCount: number;
  followUpCount: number;
}

export interface BankSessionPayload extends BankGenerationPayload {
  questionIds: string[];
  category?: string;
  focusDimension?: string;
}

export interface SkillTrainingCategoryQuota {
  category: string;
  objective: string;
  questionCount: number;
  focused: boolean;
}

export interface SkillTrainingPlan {
  version: string;
  skill: InterviewSkill;
  questionCount: number;
  focusDimension: string | null;
  categories: SkillTrainingCategoryQuota[];
}

const base = "/interviews/question-bank";

export async function listBankQuestions(filters: BankQuestionFilters, signal?: AbortSignal): Promise<BankQuestion[]> {
  const response = await request.get<BankQuestion[]>(base, { params: filters, signal });
  return response.data;
}

export async function createBankQuestion(payload: BankQuestionContent & Pick<BankQuestion, "source" | "skill" | "difficulty">): Promise<BankQuestion> {
  return (await request.post<BankQuestion>(base, payload)).data;
}

export async function updateBankQuestion(id: string, expectedVersion: number, content: BankQuestionContent): Promise<BankQuestion> {
  return (await request.put<BankQuestion>(`${base}/${encodeURIComponent(id)}`, { ...content, expectedVersion })).data;
}

export async function changeBankQuestionStatus(id: string, expectedVersion: number, status: Exclude<BankQuestionStatus, "STALE">): Promise<BankQuestion> {
  return (await request.patch<BankQuestion>(`${base}/${encodeURIComponent(id)}/status`, { expectedVersion, status })).data;
}

export async function deleteBankQuestion(id: string, expectedVersion: number): Promise<void> {
  await request.delete(`${base}/${encodeURIComponent(id)}`, { params: { expectedVersion } });
}

export async function listBankGenerationTasks(source: string, signal?: AbortSignal): Promise<GenerationTask[]> {
  return (await request.get<GenerationTask[]>(`${base}/tasks`, { params: { source }, signal })).data;
}

export async function submitBankGeneration(payload: BankGenerationPayload): Promise<GenerationTask> {
  return (await request.post<GenerationTask>(`${base}/tasks`, payload)).data;
}

export async function resumeBankGeneration(taskId: string): Promise<GenerationTask> {
  return (await request.post<GenerationTask>(`${base}/tasks/${encodeURIComponent(taskId)}/resume`)).data;
}

export async function createBankSession(payload: BankSessionPayload): Promise<InterviewSession> {
  return (await request.post<InterviewSession>(`${base}/sessions`, payload)).data;
}

export async function getSkillTrainingPlan(
  skill: InterviewSkill,
  questionCount: number,
  focusDimension?: string,
  signal?: AbortSignal,
): Promise<SkillTrainingPlan> {
  return (await request.get<SkillTrainingPlan>(`${base}/training-plan`, {
    params: { skill, questionCount, focusDimension },
    signal,
  })).data;
}
