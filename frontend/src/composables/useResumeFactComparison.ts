import { computed, onUnmounted, shallowRef, watch } from "vue";

import { resolveApiError } from "@/api/request";
import {
  compareResumeEvidenceAudits,
  type ResumeDetail,
  type ResumeEvidenceAuditComparison,
} from "@/api/resumeCenter";

export function useResumeFactComparison(detail: () => ResumeDetail | null) {
  const baseVersionId = shallowRef("");
  const targetVersionId = shallowRef("");
  const result = shallowRef<ResumeEvidenceAuditComparison | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  let controller: AbortController | null = null;

  const canCompare = computed(() => Boolean(
    baseVersionId.value
    && targetVersionId.value
    && baseVersionId.value !== targetVersionId.value
    && !loading.value,
  ));

  watch(
    () => {
      const value = detail();
      return value
        ? `${value.id}|${value.currentVersionId}|${value.versions.map((item) => item.id).join(",")}`
        : "";
    },
    () => {
      controller?.abort();
      const value = detail();
      const target = value?.versions.find((item) => item.id === value.currentVersionId)
        ?? value?.versions[0];
      const base = value?.versions.find((item) => item.id !== target?.id);
      baseVersionId.value = base?.id ?? "";
      targetVersionId.value = target?.id ?? "";
      result.value = null;
      error.value = "";
    },
    { immediate: true },
  );

  watch([baseVersionId, targetVersionId], () => {
    controller?.abort();
    loading.value = false;
    result.value = null;
    error.value = "";
  });

  async function compare(): Promise<void> {
    if (!canCompare.value) return;
    controller?.abort();
    const activeController = new AbortController();
    controller = activeController;
    loading.value = true;
    error.value = "";
    try {
      result.value = await compareResumeEvidenceAudits(
        baseVersionId.value,
        targetVersionId.value,
        activeController.signal,
      );
    } catch (requestError) {
      if (!activeController.signal.aborted) {
        error.value = resolveApiError(requestError, "比较简历事实失败");
      }
    } finally {
      if (!activeController.signal.aborted) loading.value = false;
    }
  }

  onUnmounted(() => controller?.abort());

  return {
    baseVersionId,
    targetVersionId,
    result,
    loading,
    error,
    canCompare,
    compare,
  };
}
