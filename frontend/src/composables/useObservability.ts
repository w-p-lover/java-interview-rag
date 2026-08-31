import { onMounted, shallowRef } from "vue";

import {
  getBusinessObservabilityOverview,
  type BusinessObservabilityOverview,
} from "@/api/observability";
import { resolveApiError } from "@/api/request";

export function useObservability() {
  const result = shallowRef<BusinessObservabilityOverview | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");

  async function refresh(): Promise<void> {
    loading.value = true;
    error.value = "";
    try {
      result.value = await getBusinessObservabilityOverview();
    } catch (requestError) {
      error.value = resolveApiError(requestError, "运行观测加载失败");
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    void refresh();
  });

  return { result, loading, error, refresh };
}
