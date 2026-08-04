<script setup lang="ts">
import { shallowRef } from "vue";

import { importMarkdown, type MarkdownImportResponse } from "@/api/knowledge";
import { resolveApiError } from "@/api/request";
import ChunkLedger from "@/components/ChunkLedger.vue";
import ImportPanel from "@/components/ImportPanel.vue";
import PipelineRail from "@/components/PipelineRail.vue";
import WorkspaceHeader from "@/components/WorkspaceHeader.vue";

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
    <section class="readiness-brief" aria-labelledby="page-title">
      <div class="brief-copy">
        <h1 id="page-title">把知识资料变成可检索上下文</h1>
        <p>
          上传需要长期复用的 Java 学习资料、面试笔记或项目技术说明。工作台会确认内容版本、标题路径和切片结果，为后续 RAG 问答与模拟面试提供知识依据。
        </p>
      </div>

      <dl class="readiness-readout" aria-label="当前准备状态">
        <div>
          <dt>当前入口</dt>
          <dd>Markdown 导入</dd>
        </div>
        <div>
          <dt>检查输出</dt>
          <dd>版本与 Chunk</dd>
        </div>
        <div>
          <dt>训练入口</dt>
          <dd>文本模拟面试</dd>
        </div>
      </dl>
    </section>

    <section class="workbench-intro" aria-labelledby="workbench-title">
      <div>
        <h2 id="workbench-title">资料准备工作台</h2>
        <p>一次完成文件选择、导入处理和结果核对。</p>
      </div>
      <p class="workbench-status" aria-live="polite">
        <span aria-hidden="true"></span>
        {{ state === "loading" ? "正在处理资料" : "等待本地输入" }}
      </p>
    </section>

    <div id="pipeline" class="pipeline-strip" tabindex="-1">
      <PipelineRail :state="state" />
    </div>

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
  width: min(calc(100% - (var(--space-lg) * 2)), 84rem);
  margin-inline: auto;
  padding-block: var(--space-xl) var(--space-2xl);
}

.readiness-brief {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(18rem, 0.75fr);
  align-items: end;
  gap: var(--space-2xl);
  padding-block: var(--space-xl) var(--space-2xl);
}

.brief-copy {
  display: grid;
  gap: var(--space-lg);
}

.brief-copy h1 {
  max-width: 15ch;
  margin: 0;
  color: var(--color-ink);
  font-size: var(--text-display-s);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.08;
  text-wrap: balance;
}

.brief-copy p {
  max-width: 62ch;
  margin: 0;
  color: var(--color-muted);
  font-size: var(--text-base);
  line-height: 1.65;
  text-wrap: pretty;
}

.readiness-readout {
  display: grid;
  margin: 0;
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.readiness-readout div {
  display: grid;
  grid-template-columns: 6rem minmax(0, 1fr);
  gap: var(--space-md);
  padding-block: var(--space-sm);
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

.readiness-readout dt,
.readiness-readout dd {
  margin: 0;
}

.readiness-readout dt {
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.readiness-readout dd {
  color: var(--color-ink);
  font-size: var(--text-sm);
  font-weight: 600;
}

.workbench-intro {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

.workbench-intro > div {
  display: grid;
  gap: var(--space-xs);
}

.workbench-intro h2 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 700;
  letter-spacing: -0.025em;
  line-height: 1.2;
}

.workbench-intro p {
  margin: 0;
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.workbench-status {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  font-family: var(--font-mono);
  white-space: nowrap;
}

.workbench-status span {
  width: var(--space-xs);
  height: var(--space-xs);
  border-radius: var(--radius-round);
  background: var(--color-accent);
}

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(18rem, 0.82fr) minmax(22rem, 1.18fr);
  align-items: start;
  gap: var(--space-lg);
  padding-block: var(--space-lg) var(--space-4xl);
}

.workbench-pane,
.pipeline-strip {
  min-width: 0;
  scroll-margin-top: calc(var(--space-3xl) + var(--space-md));
}

@media (max-width: 52rem) {
  .readiness-brief {
    grid-template-columns: minmax(0, 1fr);
  }

  .readiness-brief {
    gap: var(--space-xl);
  }

  .workbench-grid {
    grid-template-columns: minmax(0, 1fr);
  }

}

@media (max-width: 39.99rem) {
  .workspace {
    width: calc(100% - (var(--space-md) * 2));
    padding-block: var(--space-lg) var(--space-xl);
  }

  .readiness-brief {
    padding-block: var(--space-lg) var(--space-xl);
  }

  .brief-copy h1 {
    max-width: 18ch;
    font-size: clamp(2rem, 11vw, 2.75rem);
  }

  .workbench-intro {
    display: grid;
    align-items: start;
  }

  .workbench-grid {
    padding-bottom: var(--space-2xl);
  }

}
</style>
