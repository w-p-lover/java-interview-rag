<script setup lang="ts">
import { shallowRef } from "vue";

import { importMarkdown, type MarkdownImportResponse } from "@/api/knowledge";
import { resolveApiError } from "@/api/request";
import ChunkLedger from "@/components/ChunkLedger.vue";
import ImportPanel from "@/components/ImportPanel.vue";
import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import KnowledgeFeatureDock from "@/components/knowledge/KnowledgeFeatureDock.vue";

type WorkbenchState = "idle" | "loading" | "success" | "duplicate" | "error";

interface ImportPayload {
  file: File;
  source: string;
}

const state = shallowRef<WorkbenchState>("idle");
const result = shallowRef<MarkdownImportResponse | null>(null);
const error = shallowRef("");

async function handleImport(payload: ImportPayload): Promise<void> {
  state.value = "loading";
  error.value = "";
  result.value = null;

  try {
    const response = await importMarkdown(payload.file, payload.source);
    result.value = response;
    state.value = response.imported ? "success" : "duplicate";
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
}
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
