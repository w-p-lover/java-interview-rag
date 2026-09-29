import request from "@/api/request";
export type AnswerExperimentStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "PARTIAL_FAILED";
export type AnswerExperimentDecision = "ADOPTED" | "REJECTED";
export interface AnswerReport { passRate:number; groundedAccuracy:number; citationCompleteness:number; averageOverallScore:number; latencyP95Ms:number; chatCallCount:number; }
export interface AnswerRun { runId:string; name:string; datasetVersion:string; promptVersion:string; rubricVersion:string; modelName:string; contractHash:string; status:AnswerExperimentStatus; totalCaseCount:number; report:AnswerReport|null; errorType:string|null; baselineRunId:string|null; decision:AnswerExperimentDecision|null; decisionNote:string|null; createdAt:string; updatedAt:string; completedAt:string|null; decidedAt:string|null; }
export interface AnswerRunView { run:AnswerRun; succeededCaseCount:number; failedCaseCount:number; pendingCaseCount:number; }
export interface AnswerMetricDelta { code:string; label:string; baseline:number; candidate:number; delta:number; higherIsBetter:boolean; }
export interface AnswerComparison { baseline:AnswerRunView; candidate:AnswerRunView; metrics:AnswerMetricDelta[]; safeguardsPassed:boolean; safeguardFailures:string[]; }
export async function getAnswerExperimentRuns(){return (await request.get<AnswerRunView[]>("/interviews/evaluation-experiments/answers")).data;}
export async function startAnswerExperiment(name:string){return (await request.post<AnswerRunView>("/interviews/evaluation-experiments/answers",{name})).data;}
export async function resumeAnswerExperiment(id:string){return (await request.post<AnswerRunView>(`/interviews/evaluation-experiments/answers/${encodeURIComponent(id)}/resume`)).data;}
export async function compareAnswerExperiments(baselineRunId:string,candidateRunId:string){return (await request.get<AnswerComparison>("/interviews/evaluation-experiments/answers/comparison",{params:{baselineRunId,candidateRunId}})).data;}
export async function decideAnswerExperiment(candidateRunId:string,baselineRunId:string,decision:AnswerExperimentDecision,note:string){return (await request.patch<AnswerRunView>(`/interviews/evaluation-experiments/answers/${encodeURIComponent(candidateRunId)}/decision`,{baselineRunId,decision,note})).data;}
