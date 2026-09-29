import request from "@/api/request";

export type ScoreStabilityStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "PARTIAL_FAILED";
export type ScoreStabilityDecision = "ADOPTED" | "REJECTED";
export interface ScoreStabilityReport { passRate:number; groundedAccuracy:number; stableCaseRate:number; averageScore:number; averageScoreRange:number; latencyP95Ms:number; chatCallCount:number; }
export interface ScoreStabilityRun { runId:string; name:string; datasetVersion:string; promptVersion:string; rubricVersion:string; modelName:string; contractHash:string; status:ScoreStabilityStatus; totalCaseCount:number; repeatCount:number; report:ScoreStabilityReport|null; errorType:string|null; baselineRunId:string|null; decision:ScoreStabilityDecision|null; decisionNote:string|null; createdAt:string; updatedAt:string; completedAt:string|null; decidedAt:string|null; }
export interface ScoreStabilityRunView { run:ScoreStabilityRun; succeededCaseCount:number; failedCaseCount:number; pendingCaseCount:number; }
export interface ScoreStabilityMetricDelta { code:string; label:string; baseline:number; candidate:number; delta:number; higherIsBetter:boolean; }
export interface ScoreStabilityComparison { baseline:ScoreStabilityRunView; candidate:ScoreStabilityRunView; metrics:ScoreStabilityMetricDelta[]; safeguardsPassed:boolean; safeguardFailures:string[]; }
export async function getScoreStabilityRuns(){return (await request.get<ScoreStabilityRunView[]>("/interviews/evaluation-experiments/score-stability")).data;}
export async function startScoreStability(name:string){return (await request.post<ScoreStabilityRunView>("/interviews/evaluation-experiments/score-stability",{name})).data;}
export async function resumeScoreStability(id:string){return (await request.post<ScoreStabilityRunView>(`/interviews/evaluation-experiments/score-stability/${encodeURIComponent(id)}/resume`)).data;}
export async function compareScoreStability(baselineRunId:string,candidateRunId:string){return (await request.get<ScoreStabilityComparison>("/interviews/evaluation-experiments/score-stability/comparison",{params:{baselineRunId,candidateRunId}})).data;}
export async function decideScoreStability(candidateRunId:string,baselineRunId:string,decision:ScoreStabilityDecision,note:string){return (await request.patch<ScoreStabilityRunView>(`/interviews/evaluation-experiments/score-stability/${encodeURIComponent(candidateRunId)}/decision`,{baselineRunId,decision,note})).data;}
