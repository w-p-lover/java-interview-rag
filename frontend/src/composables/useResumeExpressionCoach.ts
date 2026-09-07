import { computed, readonly, shallowRef } from "vue";

import {
  generateResumeExpression,
  type ResumeEvidenceAuditResult,
  type ResumeExpressionCoachResult,
} from "@/api/interview";
import { resolveApiError } from "@/api/request";

export function useResumeExpressionCoach(
  targetRole: () => string,
  auditResult: () => ResumeEvidenceAuditResult | null,
) {
  const result = shallowRef<ResumeExpressionCoachResult | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const coachedTargetRole = shallowRef("");
  const coachedAuditRequestId = shallowRef("");

  const normalizedTargetRole = computed(() => targetRole().trim());
  const currentAudit = computed(() => auditResult());
  const canCoach = computed(() => Boolean(
    currentAudit.value
      && currentAudit.value.confirmedFacts.length > 0
      && !loading.value,
  ));
  const isStale = computed(() => Boolean(
    result.value && (
      coachedTargetRole.value !== normalizedTargetRole.value
      || coachedAuditRequestId.value !== currentAudit.value?.requestId
    ),
  ));

  async function coach(): Promise<void> {
    const audit = currentAudit.value;
    const role = normalizedTargetRole.value || "Java 后端开发工程师";
    if (!audit || audit.confirmedFacts.length === 0 || loading.value) {
      return;
    }
    loading.value = true;
    error.value = "";
    try {
      result.value = await generateResumeExpression(role, audit.confirmedFacts);
      coachedTargetRole.value = role;
      coachedAuditRequestId.value = audit.requestId;
    } catch (requestError) {
      error.value = resolveApiError(requestError, "生成简历表达候选失败");
    } finally {
      loading.value = false;
    }
  }

  return {
    result: readonly(result),
    loading: readonly(loading),
    error: readonly(error),
    canCoach,
    isStale,
    coach,
  };
}
