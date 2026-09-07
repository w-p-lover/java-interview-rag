import { computed, readonly, shallowRef } from "vue";

import {
  getInterviewReviewPath,
  type InterviewReviewPathResult,
} from "@/api/interview";
import { resolveApiError } from "@/api/request";

export function useInterviewReviewPath(sessionIds: () => readonly string[]) {
  const result = shallowRef<InterviewReviewPathResult | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const canGenerate = computed(() => sessionIds().length > 0 && !loading.value);

  async function generate(): Promise<void> {
    const ids = sessionIds().slice(0, 8);
    if (!ids.length || loading.value) {
      return;
    }
    loading.value = true;
    error.value = "";
    try {
      result.value = await getInterviewReviewPath(ids);
    } catch (requestError) {
      error.value = resolveApiError(requestError, "生成学习路径失败");
    } finally {
      loading.value = false;
    }
  }

  return {
    result: readonly(result),
    loading: readonly(loading),
    error: readonly(error),
    canGenerate,
    generate,
  };
}
