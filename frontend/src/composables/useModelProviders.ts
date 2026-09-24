import { onMounted, readonly, shallowRef } from "vue";

import {
  createModelProvider,
  deleteModelProvider,
  getModelProviderOverview,
  testSpecificModelProviderCapability,
  updateModelProvider,
  updateModelProviderDefaults,
  type ModelCapability,
  type ModelProviderDefaultsRequest,
  type ModelProviderOverview,
  type ModelProviderRequest,
  type ModelProviderTestResult,
} from "@/api/modelProvider";
import { resolveApiError } from "@/api/request";

export function useModelProviders() {
  const overview = shallowRef<ModelProviderOverview | null>(null);
  const loading = shallowRef(false);
  const loadError = shallowRef("");
  const actionKey = shallowRef("");
  const actionError = shallowRef("");
  const testResults = shallowRef<Record<string, ModelProviderTestResult>>({});
  const testErrors = shallowRef<Record<string, string>>({});

  async function refresh(): Promise<void> {
    loading.value = true;
    loadError.value = "";
    try {
      overview.value = await getModelProviderOverview();
    } catch (error) {
      loadError.value = resolveApiError(error, "Provider 配置加载失败");
    } finally {
      loading.value = false;
    }
  }

  async function mutate(key: string, action: () => Promise<ModelProviderOverview>): Promise<boolean> {
    actionKey.value = key;
    actionError.value = "";
    try {
      overview.value = await action();
      return true;
    } catch (error) {
      actionError.value = resolveApiError(error, "Provider 操作失败");
      return false;
    } finally {
      actionKey.value = "";
    }
  }

  function saveProvider(payload: ModelProviderRequest, editing: boolean): Promise<boolean> {
    return mutate(`save:${payload.id}`, () => editing
      ? updateModelProvider(payload.id, payload)
      : createModelProvider(payload));
  }

  function removeProvider(id: string, version: number): Promise<boolean> {
    return mutate(`delete:${id}`, () => deleteModelProvider(id, version));
  }

  function saveDefaults(payload: ModelProviderDefaultsRequest): Promise<boolean> {
    return mutate("defaults", () => updateModelProviderDefaults(payload));
  }

  async function runTest(id: string, capability: ModelCapability): Promise<void> {
    const key = `${id}:${capability}`;
    actionKey.value = `test:${key}`;
    actionError.value = "";
    testErrors.value = { ...testErrors.value, [key]: "" };
    try {
      const result = await testSpecificModelProviderCapability(id, capability);
      testResults.value = { ...testResults.value, [key]: result };
    } catch (error) {
      testErrors.value = { ...testErrors.value, [key]: resolveApiError(error, "模型能力验证失败") };
    } finally {
      actionKey.value = "";
    }
  }

  onMounted(() => {
    void refresh();
  });

  return {
    overview: readonly(overview),
    loading: readonly(loading),
    loadError: readonly(loadError),
    actionKey: readonly(actionKey),
    actionError: readonly(actionError),
    testResults: readonly(testResults),
    testErrors: readonly(testErrors),
    refresh,
    saveProvider,
    removeProvider,
    saveDefaults,
    runTest,
  };
}
