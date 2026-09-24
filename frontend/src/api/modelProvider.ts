import request from "@/api/request";

export type ModelCapability = "CHAT" | "EMBEDDING";

export interface ModelProviderCapability {
  capability: ModelCapability;
  providerId: string;
  providerName: string;
  modelName: string;
  endpointOrigin: string;
  enabled: boolean;
  apiKeyConfigured: boolean;
  defaultForCapability: boolean;
  expectedDimensions: number | null;
  usages: string[];
}

export interface ModelProviderOverview {
  generatedAt: string;
  capabilities: ModelProviderCapability[];
}

export interface ModelProviderTestResult {
  capability: ModelCapability;
  success: boolean;
  code: string;
  message: string;
  modelName: string;
  actualDimensions: number | null;
  durationMillis: number;
  testedAt: string;
}

export async function getModelProviderOverview(): Promise<ModelProviderOverview> {
  const response = await request.get<ModelProviderOverview>("/model-providers");
  return response.data;
}

export async function testModelProviderCapability(
  capability: ModelCapability,
): Promise<ModelProviderTestResult> {
  const response = await request.post<ModelProviderTestResult>(
    `/model-providers/${capability}/test`,
    undefined,
    { timeout: 75_000 },
  );
  return response.data;
}
