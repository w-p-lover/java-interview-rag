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
        <span class="brief-kicker">资料库 / 01</span>
        <h1 id="page-title">整理你的面试资料库</h1>
        <p>
          导入学习笔记、项目说明和面试资料。系统会检查版本并保留标题结构，整理完成后即可用于检索和模拟面试。
        </p>
      </div>

      <div class="brief-side">
        <div class="brief-index" aria-hidden="true">
          <strong>K</strong>
          <span>索引</span>
        </div>

        <dl class="readiness-readout" aria-label="当前准备状态">
          <div>
            <dt>当前入口</dt>
            <dd>资料导入</dd>
          </div>
          <div>
            <dt>检查内容</dt>
            <dd>版本与结构</dd>
          </div>
          <div>
            <dt>后续用途</dt>
            <dd>检索与面试</dd>
          </div>
        </dl>
      </div>
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
  width: auto;
  margin-left: var(--workspace-rail);
  padding: var(--space-xl) clamp(var(--space-lg), 4vw, var(--space-3xl)) var(--space-2xl);
}

.readiness-brief {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(20rem, 0.72fr);
  align-items: stretch;
  gap: var(--space-xl);
  padding: var(--space-md) 0 var(--space-xl);
  background: transparent;
  margin-bottom: var(--space-sm);
}

.readiness-brief::before {
  position: absolute;
  top: calc(var(--space-lg) * -1);
  right: 2%;
  z-index: 0;
  color: var(--color-paper-3);
  content: "01";
  font-family: var(--font-display);
  font-size: 8rem;
  font-weight: 800;
  line-height: 1;
}

.brief-copy,
.brief-side {
  position: relative;
  z-index: var(--z-base);
}

.brief-copy {
  display: grid;
  align-content: center;
  gap: var(--space-md);
}

.brief-copy::after {
  width: 4rem;
  height: var(--space-2xs);
  border-radius: var(--radius-round);
  background: var(--color-info-rule);
  content: "";
}

.brief-kicker {
  display: inline-flex;
  align-items: center;
  width: max-content;
  gap: var(--space-xs);
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 700;
}

.brief-kicker::before {
  width: var(--space-xs);
  height: var(--space-xs);
  border-radius: var(--radius-round);
  background: var(--color-signal-rule);
  content: "";
}

.brief-copy h1 {
  min-width: 0;
  max-width: 20ch;
  margin: 0;
  color: var(--color-ink);
  font-size: var(--text-2xl);
  font-family: var(--font-display);
  font-weight: 750;
  letter-spacing: 0;
  line-height: 1.08;
  overflow-wrap: anywhere;
  text-wrap: balance;
}

.brief-side {
  display: flex;
  align-items: center;
  min-width: 0;
  padding-left: var(--space-lg);
}

.brief-index {
  position: absolute;
  top: 50%;
  left: calc(var(--space-xl) * -1);
  z-index: var(--z-raised);
  display: grid;
  width: 4.75rem;
  height: 4.75rem;
  align-content: space-between;
  padding: var(--space-sm);
  color: var(--color-ink);
  background: var(--color-signal-soft);
  transform: translateY(-50%) rotate(2deg);
}

.brief-index strong {
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  line-height: 0.9;
}

.brief-index span {
  font-family: var(--font-body);
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}

.brief-copy p {
  max-width: 54ch;
  margin: 0;
  color: var(--color-muted);
  font-size: var(--text-base);
  line-height: 1.55;
  text-wrap: pretty;
}

.readiness-readout {
  display: grid;
  width: 100%;
  margin: 0;
  padding: var(--space-md) var(--space-md) var(--space-md) var(--space-2xl);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
  box-shadow: var(--shadow-whisper);
}

.readiness-readout div {
  display: grid;
  grid-template-columns: 6rem minmax(0, 1fr);
  gap: var(--space-md);
  padding-block: var(--space-sm);
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
  padding: var(--space-lg) var(--space-xl) var(--space-sm);
  border-radius: var(--radius-surface) var(--radius-surface) 0 0;
  color: var(--color-graphite-text);
  background: var(--color-workbench);
}

.workbench-intro > div {
  display: grid;
  gap: var(--space-xs);
}

.workbench-intro h2 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1.2;
}

.workbench-intro h2 {
  color: var(--color-graphite-text);
}

.workbench-intro p {
  margin: 0;
  color: var(--color-graphite-muted);
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
  background: var(--color-signal-rule);
}

.workbench-grid {
  position: relative;
  display: grid;
  grid-template-columns: minmax(18rem, 0.82fr) minmax(22rem, 1.18fr);
  align-items: start;
  gap: var(--space-2xl);
  margin-top: var(--space-lg);
  padding: var(--space-xl);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
  box-shadow: var(--shadow-whisper);
}

.workbench-grid::before {
  position: absolute;
  top: calc(var(--space-xs) * -1);
  left: var(--space-xl);
  width: 6.5rem;
  height: var(--space-xs);
  border-radius: var(--radius-control) var(--radius-control) 0 0;
  background: var(--color-signal-rule);
  content: "";
}

.workbench-grid::after {
  position: absolute;
  top: var(--space-md);
  right: var(--space-lg);
  width: 2.75rem;
  height: var(--space-2xs);
  background: var(--color-info-rule);
  content: "";
}

.workbench-pane,
.pipeline-strip {
  min-width: 0;
  scroll-margin-top: calc(var(--space-3xl) + var(--space-md));
}

.workbench-pane + .workbench-pane {
  padding-left: var(--space-2xl);
  box-shadow: inset var(--rule-thin) 0 var(--color-rule);
}

@media (max-width: 52rem) {
  .readiness-brief {
    grid-template-columns: minmax(0, 1fr);
  }

  .brief-side {
    padding-left: var(--space-xl);
  }

  .readiness-brief {
    gap: var(--space-xl);
  }

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

  .readiness-brief {
    grid-template-columns: minmax(0, 1fr);
    padding-block: var(--space-lg) var(--space-xl);
  }

  .brief-copy h1 {
    max-width: 18ch;
    font-size: clamp(2rem, 11vw, 2.75rem);
  }

  .brief-index {
    display: none;
  }

  .brief-side {
    padding-left: 0;
  }

  .readiness-brief::before {
    display: none;
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
