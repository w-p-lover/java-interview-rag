import { onMounted, readonly, shallowRef } from "vue";

import {
  getModelProviderOverview,
  testModelProviderCapability,
  type ModelCapability,
  type ModelProviderOverview,
  type ModelProviderTestResult,
} from "@/api/modelProvider";
import { resolveApiError } from "@/api/request";

export function useModelProviders() {
  const overview = shallowRef<ModelProviderOverview | null>(null);
  const loading = shallowRef(false);
  const loadError = shallowRef("");
  const testing = shallowRef<Partial<Record<ModelCapability, boolean>>>({});
  const testResults = shallowRef<Partial<Record<ModelCapability, ModelProviderTestResult>>>({});
  const testErrors = shallowRef<Partial<Record<ModelCapability, string>>>({});

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

  async function runTest(capability: ModelCapability): Promise<void> {
    testing.value = { ...testing.value, [capability]: true };
    testErrors.value = { ...testErrors.value, [capability]: "" };
    try {
      const result = await testModelProviderCapability(capability);
      testResults.value = { ...testResults.value, [capability]: result };
    } catch (error) {
      testErrors.value = {
        ...testErrors.value,
        [capability]: resolveApiError(error, "模型能力验证失败"),
      };
    } finally {
      testing.value = { ...testing.value, [capability]: false };
    }
  }

  onMounted(() => {
    void refresh();
  });

  return {
    overview: readonly(overview),
    loading: readonly(loading),
    loadError: readonly(loadError),
    testing: readonly(testing),
    testResults: readonly(testResults),
    testErrors: readonly(testErrors),
    refresh,
    runTest,
  };
}
