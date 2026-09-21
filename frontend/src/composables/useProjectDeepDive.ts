import { computed, readonly, shallowRef, watch } from "vue";

import {
  generateProjectDeepDive,
  getResumeAnalysisResult,
  type ProjectDeepDiveResult,
  type ResumeEvidenceConfirmedFact,
} from "@/api/interview";
import { isApiNotFound, resolveApiError } from "@/api/request";

export function useProjectDeepDive(
  project: () => ResumeEvidenceConfirmedFact | null,
  includeKnowledge: () => boolean,
  auditRequestId: () => string,
  resumeVersionId: () => string,
) {
  const result = shallowRef<ProjectDeepDiveResult | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const generatedProjectKey = shallowRef("");
  const generatedWithKnowledge = shallowRef(false);
  const generatedAuditRequestId = shallowRef("");
  const generatedResumeVersionId = shallowRef("");
  const restored = shallowRef(false);

  const currentProject = computed(() => project());
  const currentProjectKey = computed(() => factKey(currentProject.value));
  const canGenerate = computed(() => Boolean(currentProject.value && !loading.value));
  const isStale = computed(() => Boolean(
    result.value && (restored.value || (
      generatedProjectKey.value !== currentProjectKey.value
      || generatedWithKnowledge.value !== includeKnowledge()
      || generatedAuditRequestId.value !== auditRequestId()
      || generatedResumeVersionId.value !== resumeVersionId().trim()
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
        const saved = await getResumeAnalysisResult<ProjectDeepDiveResult>(
          versionId, "PROJECT_DEEP_DIVE", controller.signal,
        );
        result.value = saved.result;
        restored.value = true;
      } catch (requestError) {
        if (!isApiNotFound(requestError)) {
          error.value = resolveApiError(requestError, "读取已保存的项目深挖结果失败");
        }
      }
    },
  );

  async function generate(): Promise<void> {
    const selectedProject = currentProject.value;
    if (!selectedProject || loading.value) {
      return;
    }
    loading.value = true;
    error.value = "";
    try {
      const versionId = resumeVersionId().trim();
      const generated = await generateProjectDeepDive(
        selectedProject, includeKnowledge(), versionId || undefined,
      );
      if (resumeVersionId().trim() !== versionId) return;
      result.value = generated;
      generatedProjectKey.value = factKey(selectedProject);
      generatedWithKnowledge.value = includeKnowledge();
      generatedAuditRequestId.value = auditRequestId();
      generatedResumeVersionId.value = versionId;
      restored.value = false;
    } catch (requestError) {
      error.value = resolveApiError(requestError, "项目深挖题生成失败");
    } finally {
      loading.value = false;
    }
  }

  return {
    result: readonly(result),
    loading: readonly(loading),
    error: readonly(error),
    canGenerate,
    isStale,
    generate,
  };
}

function factKey(fact: ResumeEvidenceConfirmedFact | null): string {
  return fact ? `${fact.type}\u0000${fact.statement}\u0000${fact.evidence}` : "";
}
