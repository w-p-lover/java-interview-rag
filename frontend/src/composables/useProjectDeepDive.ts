import { computed, readonly, shallowRef } from "vue";

import {
  generateProjectDeepDive,
  type ProjectDeepDiveResult,
  type ResumeEvidenceConfirmedFact,
} from "@/api/interview";
import { resolveApiError } from "@/api/request";

export function useProjectDeepDive(
  project: () => ResumeEvidenceConfirmedFact | null,
  includeKnowledge: () => boolean,
  auditRequestId: () => string,
) {
  const result = shallowRef<ProjectDeepDiveResult | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const generatedProjectKey = shallowRef("");
  const generatedWithKnowledge = shallowRef(false);
  const generatedAuditRequestId = shallowRef("");

  const currentProject = computed(() => project());
  const currentProjectKey = computed(() => factKey(currentProject.value));
  const canGenerate = computed(() => Boolean(currentProject.value && !loading.value));
  const isStale = computed(() => Boolean(
    result.value && (
      generatedProjectKey.value !== currentProjectKey.value
      || generatedWithKnowledge.value !== includeKnowledge()
      || generatedAuditRequestId.value !== auditRequestId()
    ),
  ));

  async function generate(): Promise<void> {
    const selectedProject = currentProject.value;
    if (!selectedProject || loading.value) {
      return;
    }
    loading.value = true;
    error.value = "";
    try {
      result.value = await generateProjectDeepDive(selectedProject, includeKnowledge());
      generatedProjectKey.value = factKey(selectedProject);
      generatedWithKnowledge.value = includeKnowledge();
      generatedAuditRequestId.value = auditRequestId();
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
