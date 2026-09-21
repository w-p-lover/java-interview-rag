import { computed, readonly, shallowRef, watch } from "vue";

import {
  generateResumeExpression,
  getResumeAnalysisResult,
  type ResumeEvidenceAuditResult,
  type ResumeExpressionCoachResult,
} from "@/api/interview";
import { isApiNotFound, resolveApiError } from "@/api/request";

export function useResumeExpressionCoach(
  targetRole: () => string,
  auditResult: () => ResumeEvidenceAuditResult | null,
  resumeVersionId: () => string,
) {
  const result = shallowRef<ResumeExpressionCoachResult | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const coachedTargetRole = shallowRef("");
  const coachedAuditRequestId = shallowRef("");
  const coachedResumeVersionId = shallowRef("");
  const restored = shallowRef(false);

  const normalizedTargetRole = computed(() => targetRole().trim());
  const currentAudit = computed(() => auditResult());
  const canCoach = computed(() => Boolean(
    currentAudit.value
      && currentAudit.value.confirmedFacts.length > 0
      && !loading.value,
  ));
  const isStale = computed(() => Boolean(
    result.value && (restored.value || (
      coachedTargetRole.value !== normalizedTargetRole.value
      || coachedAuditRequestId.value !== currentAudit.value?.requestId
      || coachedResumeVersionId.value !== resumeVersionId().trim()
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
        const saved = await getResumeAnalysisResult<ResumeExpressionCoachResult>(
          versionId, "EXPRESSION_COACH", controller.signal,
        );
        result.value = saved.result;
        restored.value = true;
      } catch (requestError) {
        if (!isApiNotFound(requestError)) {
          error.value = resolveApiError(requestError, "读取已保存的表达教练结果失败");
        }
      }
    },
  );

  async function coach(): Promise<void> {
    const audit = currentAudit.value;
    const role = normalizedTargetRole.value || "Java 后端开发工程师";
    if (!audit || audit.confirmedFacts.length === 0 || loading.value) {
      return;
    }
    loading.value = true;
    error.value = "";
    try {
      const versionId = resumeVersionId().trim();
      const coached = await generateResumeExpression(
        role, audit.confirmedFacts, versionId || undefined,
      );
      if (resumeVersionId().trim() !== versionId) return;
      result.value = coached;
      coachedTargetRole.value = role;
      coachedAuditRequestId.value = audit.requestId;
      coachedResumeVersionId.value = versionId;
      restored.value = false;
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
