import { computed, readonly, shallowRef, watch } from "vue";

import {
  auditResumeEvidence,
  getResumeAnalysisResult,
  type ResumeEvidenceAuditResult,
} from "@/api/interview";
import { isApiNotFound, resolveApiError } from "@/api/request";

export function useResumeEvidenceAudit(
  resumeText: () => string,
  resumeVersionId: () => string,
) {
  const result = shallowRef<ResumeEvidenceAuditResult | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const auditedResumeText = shallowRef("");
  const auditedResumeVersionId = shallowRef("");

  const normalizedResumeText = computed(() => resumeText().trim());
  const canAudit = computed(() => normalizedResumeText.value.length > 0 && !loading.value);
  const isStale = computed(() => Boolean(
    result.value && (
      auditedResumeText.value !== normalizedResumeText.value
      || auditedResumeVersionId.value !== resumeVersionId().trim()
    ),
  ));

  watch(
    () => resumeVersionId().trim(),
    async (versionId, _previous, onCleanup) => {
      result.value = null;
      auditedResumeText.value = "";
      auditedResumeVersionId.value = "";
      error.value = "";
      if (!versionId) return;
      const controller = new AbortController();
      onCleanup(() => controller.abort());
      try {
        const saved = await getResumeAnalysisResult<ResumeEvidenceAuditResult>(
          versionId, "EVIDENCE_AUDIT", controller.signal,
        );
        result.value = saved.result;
        auditedResumeText.value = normalizedResumeText.value;
        auditedResumeVersionId.value = versionId;
      } catch (requestError) {
        if (!isApiNotFound(requestError)) {
          error.value = resolveApiError(requestError, "读取已保存的简历证据结果失败");
        }
      }
    },
  );

  async function audit(): Promise<void> {
    const requestedResumeText = normalizedResumeText.value;
    if (!requestedResumeText || loading.value) {
      return;
    }

    loading.value = true;
    error.value = "";
    try {
      const versionId = resumeVersionId().trim();
      const audited = await auditResumeEvidence(requestedResumeText, versionId || undefined);
      if (resumeVersionId().trim() !== versionId) return;
      result.value = audited;
      auditedResumeText.value = requestedResumeText;
      auditedResumeVersionId.value = versionId;
    } catch (requestError) {
      error.value = resolveApiError(requestError, "简历证据检查失败");
    } finally {
      loading.value = false;
    }
  }

  return {
    result: readonly(result),
    loading: readonly(loading),
    error: readonly(error),
    canAudit,
    isStale,
    audit,
  };
}
