import { computed, readonly, shallowRef } from "vue";

import {
  analyzeResumeJobMatch,
  type ResumeEvidenceAuditResult,
  type ResumeJobMatchResult,
} from "@/api/interview";
import { resolveApiError } from "@/api/request";

export function useResumeJobMatch(
  targetRole: () => string,
  jobDescription: () => string,
  auditResult: () => ResumeEvidenceAuditResult | null,
) {
  const result = shallowRef<ResumeJobMatchResult | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const matchedTargetRole = shallowRef("");
  const matchedJobDescription = shallowRef("");
  const matchedAuditRequestId = shallowRef("");

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
    result.value && (
      matchedTargetRole.value !== normalizedTargetRole.value
      || matchedJobDescription.value !== normalizedJobDescription.value
      || matchedAuditRequestId.value !== currentAudit.value?.requestId
    ),
  ));

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
      result.value = await analyzeResumeJobMatch(role, description, audit.confirmedFacts);
      matchedTargetRole.value = role;
      matchedJobDescription.value = description;
      matchedAuditRequestId.value = audit.requestId;
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
