import request from "@/api/request";

export interface BusinessOperationMetric {
  operation: string;
  requestCount: number;
  failureCount: number;
  failureRate: number;
  averageLatencyMillis: number;
  maxLatencyMillis: number;
}

export interface BusinessObservabilityOverview {
  generatedAt: string;
  totalRequests: number;
  totalFailures: number;
  operations: BusinessOperationMetric[];
  cache: { hitCount: number; missCount: number; bypassCount: number };
  tasks: { queued: number; running: number; succeeded: number; failed: number; cancelled: number; rejected: number; timedOut: number };
  retrieval: {
    dense: RetrievalChannelMetric;
    sparse: RetrievalChannelMetric;
  };
  feedback: { total: number; helpful: number; notHelpful: number; pending: number; evaluation: number };
}

export interface RetrievalChannelMetric {
  threadCapacity: number;
  queueCapacity: number;
  submitted: number;
  active: number;
  queued: number;
  completed: number;
  timedOut: number;
  rejected: number;
}

export async function getBusinessObservabilityOverview(): Promise<BusinessObservabilityOverview> {
  const response = await request.get<BusinessObservabilityOverview>("/observability/overview");
  return response.data;
}
