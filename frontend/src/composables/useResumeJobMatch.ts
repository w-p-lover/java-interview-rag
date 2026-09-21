import { computed, readonly, shallowRef, watch } from "vue";

import {
  analyzeResumeJobMatch,
  getResumeAnalysisResult,
  type ResumeEvidenceAuditResult,
  type ResumeJobMatchResult,
} from "@/api/interview";
import { isApiNotFound, resolveApiError } from "@/api/request";

export function useResumeJobMatch(
  targetRole: () => string,
  jobDescription: () => string,
  auditResult: () => ResumeEvidenceAuditResult | null,
  resumeVersionId: () => string,
) {
  const result = shallowRef<ResumeJobMatchResult | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const matchedTargetRole = shallowRef("");
  const matchedJobDescription = shallowRef("");
  const matchedAuditRequestId = shallowRef("");
  const matchedResumeVersionId = shallowRef("");
  const restored = shallowRef(false);

  const normalizedTargetRole = computed(() => targetRole().trim());
  const normalizedJobDescription = computed(() => jobDescription().trim());
  const currentAudit = computed(() => auditResult());
  const canMatch = computed(() => Boolean(
    normalizedTargetRole.value
      && normalizedJobDescription.value
      && currentAudit.value
      && currentAudit.value.confirmedFacts.length > 0
      && !loading.value,
  ));
  const isStale = computed(() => Boolean(
    result.value && (restored.value || (
      matchedTargetRole.value !== normalizedTargetRole.value
      || matchedJobDescription.value !== normalizedJobDescription.value
      || matchedAuditRequestId.value !== currentAudit.value?.requestId
      || matchedResumeVersionId.value !== resumeVersionId().trim()
    )),
  ));

  watch(
    () => resumeVersionId().trim(),
    async (versionId, _previous, onCleanup) => {
      result.value = null;
      restored.value = false;
      error.value = "";
      if (!versionId) return;
      const controller = new AbortController();
      onCleanup(() => controller.abort());
      try {
        const saved = await getResumeAnalysisResult<ResumeJobMatchResult>(
          versionId, "JOB_MATCH", controller.signal,
        );
        result.value = saved.result;
        restored.value = true;
      } catch (requestError) {
        if (!isApiNotFound(requestError)) {
          error.value = resolveApiError(requestError, "读取已保存的岗位匹配结果失败");
        }
      }
    },
  );

  async function match(): Promise<void> {
    const audit = currentAudit.value;
    const role = normalizedTargetRole.value;
    const description = normalizedJobDescription.value;
    if (!audit || !role || !description || loading.value) {
      return;
    }
    loading.value = true;
    error.value = "";
    try {
      const versionId = resumeVersionId().trim();
      const matched = await analyzeResumeJobMatch(
        role, description, audit.confirmedFacts, versionId || undefined,
      );
      if (resumeVersionId().trim() !== versionId) return;
      result.value = matched;
      matchedTargetRole.value = role;
      matchedJobDescription.value = description;
      matchedAuditRequestId.value = audit.requestId;
      matchedResumeVersionId.value = versionId;
      restored.value = false;
    } catch (requestError) {
      error.value = resolveApiError(requestError, "岗位匹配分析失败");
    } finally {
      loading.value = false;
    }
  }

  return {
    result: readonly(result),
    loading: readonly(loading),
    error: readonly(error),
    canMatch,
    isStale,
    match,
  };
}
