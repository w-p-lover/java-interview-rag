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
  providers: ModelProvider[];
  defaultChatProviderId: string;
  defaultEmbeddingProviderId: string;
  defaultsVersion: number;
  capabilities: ModelProviderCapability[];
}

export interface ModelProvider {
  id: string;
  name: string;
  baseUrl: string;
  endpointOrigin: string;
  apiKeyConfigured: boolean;
  chatModel: string | null;
  embeddingModel: string | null;
  embeddingDimensions: number | null;
  temperature: number | null;
  enabled: boolean;
  version: number;
  defaultForChat: boolean;
  defaultForEmbedding: boolean;
}

export interface ModelProviderRequest {
  id: string;
  name: string;
  baseUrl: string;
  apiKey: string;
  chatModel: string;
  embeddingModel: string;
  embeddingDimensions: number | null;
  temperature: number | null;
  enabled: boolean;
  version: number | null;
}

export interface ModelProviderDefaultsRequest {
  chatProviderId: string;
  embeddingProviderId: string;
  version: number;
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

export async function createModelProvider(
  payload: ModelProviderRequest,
): Promise<ModelProviderOverview> {
  const response = await request.post<ModelProviderOverview>("/model-providers/providers", payload);
  return response.data;
}

export async function updateModelProvider(
  id: string,
  payload: ModelProviderRequest,
): Promise<ModelProviderOverview> {
  const response = await request.put<ModelProviderOverview>(
    `/model-providers/providers/${encodeURIComponent(id)}`,
    payload,
  );
  return response.data;
}

export async function deleteModelProvider(id: string, version: number): Promise<ModelProviderOverview> {
  const response = await request.delete<ModelProviderOverview>(
    `/model-providers/providers/${encodeURIComponent(id)}`,
    { params: { version } },
  );
  return response.data;
}

export async function testSpecificModelProviderCapability(
  id: string,
  capability: ModelCapability,
): Promise<ModelProviderTestResult> {
  const response = await request.post<ModelProviderTestResult>(
    `/model-providers/providers/${encodeURIComponent(id)}/${capability}/test`,
    undefined,
    { timeout: 75_000 },
  );
  return response.data;
}

export async function updateModelProviderDefaults(
  payload: ModelProviderDefaultsRequest,
): Promise<ModelProviderOverview> {
  const response = await request.put<ModelProviderOverview>("/model-providers/defaults", payload);
  return response.data;
}
