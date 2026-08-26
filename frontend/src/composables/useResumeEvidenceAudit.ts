import { computed, readonly, shallowRef } from "vue";

import {
  auditResumeEvidence,
  type ResumeEvidenceAuditResult,
} from "@/api/interview";
import { resolveApiError } from "@/api/request";

export function useResumeEvidenceAudit(resumeText: () => string) {
  const result = shallowRef<ResumeEvidenceAuditResult | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const auditedResumeText = shallowRef("");

  const normalizedResumeText = computed(() => resumeText().trim());
  const canAudit = computed(() => normalizedResumeText.value.length > 0 && !loading.value);
  const isStale = computed(() => Boolean(
    result.value && auditedResumeText.value !== normalizedResumeText.value,
  ));

  async function audit(): Promise<void> {
    const requestedResumeText = normalizedResumeText.value;
    if (!requestedResumeText || loading.value) {
      return;
    }

    loading.value = true;
    error.value = "";
    try {
      result.value = await auditResumeEvidence(requestedResumeText);
      auditedResumeText.value = requestedResumeText;
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
