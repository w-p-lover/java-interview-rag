import { onBeforeUnmount, onMounted, readonly, shallowRef } from "vue";

import {
  compareRetrievalExperiments,
  decideRetrievalExperiment,
  getRetrievalExperimentRuns,
  resumeRetrievalExperiment,
  startRetrievalExperiment,
  type ExperimentDecision,
  type RetrievalExperimentComparison,
  type RetrievalExperimentRunView,
  type RetrievalExperimentStartRequest,
} from "@/api/evaluationExperiment";
import { resolveApiError } from "@/api/request";

export function useRetrievalExperiments() {
  const runs = shallowRef<RetrievalExperimentRunView[]>([]);
  const comparison = shallowRef<RetrievalExperimentComparison | null>(null);
  const loading = shallowRef(false);
  const actionKey = shallowRef("");
  const error = shallowRef("");
  let pollTimer: number | undefined;

  async function refresh(silent = false): Promise<void> {
    if (!silent) loading.value = true;
    if (!silent) error.value = "";
    try {
      runs.value = await getRetrievalExperimentRuns();
      schedulePolling();
    } catch (requestError) {
      if (!silent) error.value = resolveApiError(requestError, "读取实验运行失败");
    } finally {
      if (!silent) loading.value = false;
    }
  }

  async function start(payload: RetrievalExperimentStartRequest): Promise<boolean> {
    return mutate("start", async () => {
      await startRetrievalExperiment(payload);
      comparison.value = null;
      await refresh(true);
    }, "启动检索实验失败");
  }

  async function resume(runId: string): Promise<boolean> {
    return mutate(`resume:${runId}`, async () => {
      await resumeRetrievalExperiment(runId);
      await refresh(true);
    }, "恢复检索实验失败");
  }

  async function compare(baselineRunId: string, candidateRunId: string): Promise<boolean> {
    return mutate("compare", async () => {
      comparison.value = await compareRetrievalExperiments(baselineRunId, candidateRunId);
    }, "比较检索实验失败");
  }

  async function decide(
    candidateRunId: string,
    baselineRunId: string,
    decision: ExperimentDecision,
    note: string,
  ): Promise<boolean> {
    return mutate("decision", async () => {
      await decideRetrievalExperiment(candidateRunId, baselineRunId, decision, note);
      await refresh(true);
      comparison.value = await compareRetrievalExperiments(baselineRunId, candidateRunId);
    }, "保存实验结论失败");
  }

  async function mutate(key: string, action: () => Promise<void>, message: string): Promise<boolean> {
    actionKey.value = key;
    error.value = "";
    try {
      await action();
      return true;
    } catch (requestError) {
      error.value = resolveApiError(requestError, message);
      return false;
    } finally {
      actionKey.value = "";
    }
  }

  function schedulePolling(): void {
    if (pollTimer) window.clearTimeout(pollTimer);
    const active = runs.value.some(({ run }) => run.status === "QUEUED" || run.status === "RUNNING");
    if (active) {
      pollTimer = window.setTimeout(() => void refresh(true), 1500);
    }
  }

  onMounted(() => void refresh());
  onBeforeUnmount(() => {
    if (pollTimer) window.clearTimeout(pollTimer);
  });

  return {
    runs: readonly(runs),
    comparison: readonly(comparison),
    loading: readonly(loading),
    actionKey: readonly(actionKey),
    error: readonly(error),
    refresh,
    start,
    resume,
    compare,
    decide,
  };
}
