<script setup lang="ts">
import { onBeforeUnmount, onMounted, shallowRef } from "vue";

import { cancelKnowledgeDocumentTask, getKnowledgeDocumentTask, getRecentKnowledgeDocumentTasks, submitKnowledgeDocumentTask, type KnowledgeDocumentTaskResponse, type MarkdownImportResponse } from "@/api/knowledge";
import { resolveApiError } from "@/api/request";
import ChunkLedger from "@/components/ChunkLedger.vue";
import ImportPanel from "@/components/ImportPanel.vue";
import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import KnowledgeFeatureDock from "@/components/knowledge/KnowledgeFeatureDock.vue";
import KnowledgeDocumentTaskPanel from "@/components/KnowledgeDocumentTaskPanel.vue";

type WorkbenchState = "idle" | "loading" | "success" | "duplicate" | "error";

interface ImportPayload {
  file: File;
  source: string;
}

const state = shallowRef<WorkbenchState>("idle");
const result = shallowRef<MarkdownImportResponse | null>(null);
const error = shallowRef("");
const task = shallowRef<KnowledgeDocumentTaskResponse | null>(null);
const recentTasks = shallowRef<KnowledgeDocumentTaskResponse[]>([]);
let pollTimer: number | undefined;
let pollCount = 0;

function clearTaskPolling(): void {
  if (pollTimer !== undefined) {
    window.clearTimeout(pollTimer);
    pollTimer = undefined;
  }
}

function updateRecentTask(updated: KnowledgeDocumentTaskResponse): void {
  recentTasks.value = [updated, ...recentTasks.value.filter((item) => item.taskId !== updated.taskId)].slice(0, 8);
}

function applyTerminalTask(updated: KnowledgeDocumentTaskResponse): void {
  if (updated.status === "SUCCEEDED") {
    state.value = updated.imported ? "success" : "duplicate";
    result.value = {
      source: updated.source,
      version: updated.version || "",
      imported: updated.imported,
      chunkCount: updated.chunkCount,
      chunks: [],
      format: updated.format || "MARKDOWN",
      pageCount: null,
      pagesNeedingOcr: [],
      warnings: [],
    };
    return;
  }
  result.value = null;
  state.value = "error";
}

async function pollTask(taskId: string): Promise<void> {
  clearTaskPolling();
  try {
    task.value = await getKnowledgeDocumentTask(taskId);
    updateRecentTask(task.value);
    if (task.value.status === "SUCCEEDED") {
      applyTerminalTask(task.value);
      return;
    }
    if (task.value.status === "FAILED" || task.value.status === "CANCELLED") {
      applyTerminalTask(task.value);
      return;
    }
    if (++pollCount >= 30) {
      error.value = "任务状态长时间未完成，请稍后刷新资料目录确认结果。";
      return;
    }
    pollTimer = window.setTimeout(() => void pollTask(taskId), Math.min(500 + pollCount * 100, 1500));
  } catch (requestError) {
    error.value = resolveApiError(requestError, "读取资料任务状态失败");
    pollTimer = window.setTimeout(() => void pollTask(taskId), 1500);
  }
}

async function loadRecentTasks(): Promise<void> {
  try {
    recentTasks.value = await getRecentKnowledgeDocumentTasks();
    const latest = recentTasks.value[0];
    if (latest) {
      task.value = latest;
      if (latest.status === "QUEUED" || latest.status === "RUNNING") {
        state.value = "loading";
        pollCount = 0;
        await pollTask(latest.taskId);
      } else {
        applyTerminalTask(latest);
      }
    }
  } catch (requestError) {
    error.value = resolveApiError(requestError, "读取资料任务历史失败");
  }
}

async function selectRecentTask(selected: KnowledgeDocumentTaskResponse): Promise<void> {
  task.value = selected;
  error.value = "";
  pollCount = 0;
  if (selected.status === "QUEUED" || selected.status === "RUNNING") {
    state.value = "loading";
    await pollTask(selected.taskId);
  } else {
    applyTerminalTask(selected);
  }
}

async function cancelTask(): Promise<void> {
  if (!task.value || task.value.status !== "QUEUED") return;
  clearTaskPolling();
  try {
    task.value = await cancelKnowledgeDocumentTask(task.value.taskId);
    updateRecentTask(task.value);
    applyTerminalTask(task.value);
  } catch (requestError) {
    error.value = resolveApiError(requestError, "取消资料任务失败");
    pollTimer = window.setTimeout(() => void pollTask(task.value?.taskId || ""), 500);
  }
}

async function handleImport(payload: ImportPayload): Promise<void> {
  state.value = "loading";
  error.value = "";
  result.value = null;

  try {
    task.value = null;
    pollCount = 0;
    const submitted = await submitKnowledgeDocumentTask(payload.file, payload.source, "IMPORT");
    const submittedTask = await getKnowledgeDocumentTask(submitted.taskId);
    updateRecentTask(submittedTask);
    await pollTask(submitted.taskId);
  } catch (requestError) {
    error.value = resolveApiError(requestError, "导入失败");
    state.value = "error";
  }
}

function resetImportState(): void {
  if (state.value === "loading") {
    return;
  }
  state.value = "idle";
  error.value = "";
  result.value = null;
  task.value = null;
  clearTaskPolling();
}

onBeforeUnmount(clearTaskPolling);
onMounted(() => void loadRecentTasks());
</script>

<template>
  <WorkspaceHeader />

  <main id="main-content" class="workspace" tabindex="-1">
    <header class="page-heading">
      <h1>资料库</h1>
    </header>

    <KnowledgeFeatureDock />

    <section class="workbench-grid" aria-label="资料准备工作区">
      <div id="import-workbench" class="workbench-pane" tabindex="-1">
        <ImportPanel
          :busy="state === 'loading'"
          :error="error"
          :status="state"
          @reset="resetImportState"
          @submit="handleImport"
        />
      </div>

      <div id="import-result" class="workbench-pane" tabindex="-1">
        <section v-if="recentTasks.length" class="task-history" aria-labelledby="task-history-title">
          <div class="task-history-heading">
            <h2 id="task-history-title">最近资料任务</h2>
            <span>刷新页面后仍可恢复任务状态</span>
          </div>
          <button v-for="item in recentTasks" :key="item.taskId" type="button" class="task-history-item" :class="{ 'is-selected': task?.taskId === item.taskId }" @click="selectRecentTask(item)">
            <span>{{ item.operation === "IMPORT" ? "导入" : "重建" }} · {{ item.source }}</span>
            <b>{{ item.status === "QUEUED" ? "排队中" : item.status === "RUNNING" ? "处理中" : item.status === "SUCCEEDED" ? "已完成" : item.status === "CANCELLED" ? "已取消" : "失败" }}</b>
          </button>
        </section>
        <KnowledgeDocumentTaskPanel :task="task" :error="error" @cancel="cancelTask" />
        <ChunkLedger :result="result" :state="state" />
      </div>
    </section>

  </main>
</template>

<style scoped lang="scss">
.workspace {
  width: auto;
  margin-left: var(--workspace-rail);
  padding: var(--space-lg) clamp(var(--space-lg), 4vw, var(--space-3xl)) var(--space-2xl);
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

.page-heading h1 {
  margin: 0;
  color: var(--color-ink);
  font-size: var(--text-xl);
  font-family: var(--font-display);
  font-weight: 750;
  letter-spacing: 0;
  line-height: 1.2;
}

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(18rem, 0.82fr) minmax(22rem, 1.18fr);
  align-items: start;
  gap: var(--space-2xl);
  padding: var(--space-xl);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
  box-shadow: 0 1px 2px var(--color-shadow);
}

.workbench-pane {
  min-width: 0;
  scroll-margin-top: calc(var(--space-3xl) + var(--space-md));
}

.workbench-pane + .workbench-pane {
  padding-left: var(--space-2xl);
  box-shadow: inset var(--rule-thin) 0 var(--color-rule);
}

.task-history { display: grid; gap: var(--space-xs); margin-bottom: var(--space-md); padding: var(--space-md); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); background: var(--color-paper-2); }
.task-history-heading { display: flex; align-items: baseline; justify-content: space-between; gap: var(--space-sm); }
.task-history-heading h2 { margin: 0; font-size: var(--text-sm); }
.task-history-heading span { color: var(--color-muted); font-size: var(--text-xs); }
.task-history-item { display: flex; align-items: center; justify-content: space-between; gap: var(--space-sm); min-width: 0; padding: var(--space-xs) var(--space-sm); border: 0; border-top: var(--rule-thin) solid var(--color-rule); color: var(--color-ink-2); background: transparent; text-align: left; font-size: var(--text-xs); }
.task-history-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-history-item b { flex: 0 0 auto; color: var(--color-muted); font-weight: 600; }
.task-history-item.is-selected { color: var(--color-accent); background: var(--color-accent-soft); }

@media (max-width: 52rem) {
  .workbench-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .workbench-pane + .workbench-pane {
    padding-top: var(--space-2xl);
    padding-left: 0;
    box-shadow: inset 0 var(--rule-thin) var(--color-rule);
  }

}

@media (max-width: 68rem) {
  .workspace {
    margin-left: 0;
  }
}

@media (max-width: 39.99rem) {
  .workspace {
    width: calc(100% - (var(--space-md) * 2));
    margin-inline: auto;
    padding-inline: 0;
    padding-block: var(--space-lg) var(--space-xl);
  }

  .workbench-grid {
    padding: var(--space-lg);
    padding-bottom: var(--space-2xl);
  }

}
</style>
