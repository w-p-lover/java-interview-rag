import request from "@/api/request";

export type RetrievalStrategy = "DENSE" | "SPARSE" | "HYBRID";
export type EvaluationSplit = "DEV" | "HOLDOUT";
export type ExperimentStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "PARTIAL_FAILED";
export type ExperimentDecision = "ADOPTED" | "REJECTED";

export interface RetrievalMetrics {
  precisionAt5: number;
  ndcgAt5: number;
  chunkRecallAt5: number;
  hardNegativeAvoidanceRate: number;
  contextNoiseRate: number;
  retrievalLatencyP95Ms: number;
}

export interface RetrievalReport {
  expectedSourceHitRate: number;
  hitAt1Rate: number;
  meanRecallAt5: number;
  meanReciprocalRank: number;
  rejectionAccuracy: number;
  advancedMetrics: RetrievalMetrics;
}

export interface RetrievalExperimentRun {
  runId: string;
  name: string;
  strategy: RetrievalStrategy;
  datasetVersion: string;
  datasetSplit: EvaluationSplit;
  corpusHash: string;
  indexContract: string;
  gitSha: string;
  contractHash: string;
  status: ExperimentStatus;
  totalCaseCount: number;
  report: RetrievalReport | null;
  errorType: string | null;
  baselineRunId: string | null;
  decision: ExperimentDecision | null;
  decisionNote: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
  decidedAt: string | null;
}

export interface RetrievalExperimentRunView {
  run: RetrievalExperimentRun;
  succeededCaseCount: number;
  failedCaseCount: number;
  pendingCaseCount: number;
}

export interface RetrievalExperimentStartRequest {
  name: string;
  strategy: RetrievalStrategy;
  split: EvaluationSplit;
}

export interface ExperimentMetricDelta {
  code: string;
  label: string;
  baseline: number;
  candidate: number;
  delta: number;
  higherIsBetter: boolean;
}

export interface RetrievalExperimentComparison {
  baseline: RetrievalExperimentRunView;
  candidate: RetrievalExperimentRunView;
  metrics: readonly ExperimentMetricDelta[];
  safeguardsPassed: boolean;
  safeguardFailures: readonly string[];
}

export async function getRetrievalExperimentRuns(): Promise<RetrievalExperimentRunView[]> {
  const response = await request.get<RetrievalExperimentRunView[]>(
    "/knowledge/evaluation-experiments/retrieval",
  );
  return response.data;
}

export async function startRetrievalExperiment(
  payload: RetrievalExperimentStartRequest,
): Promise<RetrievalExperimentRunView> {
  const response = await request.post<RetrievalExperimentRunView>(
    "/knowledge/evaluation-experiments/retrieval",
    payload,
  );
  return response.data;
}

export async function resumeRetrievalExperiment(
  runId: string,
): Promise<RetrievalExperimentRunView> {
  const response = await request.post<RetrievalExperimentRunView>(
    `/knowledge/evaluation-experiments/retrieval/${encodeURIComponent(runId)}/resume`,
  );
  return response.data;
}

export async function compareRetrievalExperiments(
  baselineRunId: string,
  candidateRunId: string,
): Promise<RetrievalExperimentComparison> {
  const response = await request.get<RetrievalExperimentComparison>(
    "/knowledge/evaluation-experiments/retrieval/comparison",
    { params: { baselineRunId, candidateRunId } },
  );
  return response.data;
}

export async function decideRetrievalExperiment(
  candidateRunId: string,
  baselineRunId: string,
  decision: ExperimentDecision,
  note: string,
): Promise<RetrievalExperimentRunView> {
  const response = await request.patch<RetrievalExperimentRunView>(
    `/knowledge/evaluation-experiments/retrieval/${encodeURIComponent(candidateRunId)}/decision`,
    { baselineRunId, decision, note },
  );
  return response.data;
}
