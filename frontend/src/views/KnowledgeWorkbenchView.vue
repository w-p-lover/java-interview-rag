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
    error.value = resolveApiError(requestError);
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
        <h1 id="page-title">把面试资料变成可检索上下文</h1>
        <p>
          上传简历、岗位描述或 Java 面试资料。工作台会先确认内容版本、标题路径和切片结果，再为后续 RAG 问答与模拟面试提供资料基础。
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
          <dt>下一阶段</dt>
          <dd>最小 RAG 检索</dd>
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

    <section
      id="capability-boundary"
      class="capability-boundary"
      tabindex="-1"
      aria-labelledby="boundary-title"
    >
      <div class="boundary-heading">
        <h2 id="boundary-title">当前能力边界</h2>
        <p>只展示已实现链路；尚未接入的模块不会生成假数据。</p>
      </div>

      <dl class="boundary-list">
        <div>
          <dt>前端可操作</dt>
          <dd>Markdown 上传、来源校验、重复提示、版本与 Chunk 检查</dd>
        </div>
        <div>
          <dt>后端已具备</dt>
          <dd>存储模式下的向量写入、更新、删除与手动重建</dd>
        </div>
        <div>
          <dt>下一步开发</dt>
          <dd>问题向量化、相似度召回、真实来源返回与知识不足兜底</dd>
        </div>
      </dl>
    </section>

    <footer class="workspace-footer">
      <strong>Java Interview RAG</strong>
      <span aria-hidden="true">·</span>
      <span>资料准备工作台</span>
      <span aria-hidden="true">·</span>
      <span>当前阶段：RAG 检索闭环</span>
    </footer>
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

.workbench-intro h2,
.capability-boundary h2 {
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
  padding-block: var(--space-lg) var(--space-3xl);
}

.workbench-pane,
.pipeline-strip {
  min-width: 0;
  scroll-margin-top: calc(var(--space-3xl) + var(--space-md));
}

.capability-boundary {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
  gap: var(--space-2xl);
  padding: var(--space-2xl);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: calc(var(--radius-surface) * 1.35);
  scroll-margin-top: calc(var(--space-3xl) + var(--space-md));
  color: var(--color-ink);
  background: var(--color-paper-2);
  box-shadow: var(--shadow-whisper);
}

.boundary-heading {
  display: grid;
  align-content: start;
  gap: var(--space-md);
}

.boundary-heading p {
  max-width: 45ch;
  margin: 0;
  color: var(--color-muted);
  line-height: 1.6;
}

.boundary-list {
  display: grid;
  margin: 0;
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.boundary-list div {
  display: grid;
  grid-template-columns: 8rem minmax(0, 1fr);
  gap: var(--space-lg);
  padding-block: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

.boundary-list dt,
.boundary-list dd {
  margin: 0;
}

.boundary-list dt {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.boundary-list dd {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.6;
}

.workspace-footer {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding-top: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule);
  color: var(--color-muted);
  font-size: var(--text-xs);
  white-space: nowrap;
}

.workspace-footer strong {
  color: var(--color-ink);
  font-family: var(--font-display);
  font-weight: 600;
}

@media (max-width: 52rem) {
  .readiness-brief,
  .capability-boundary {
    grid-template-columns: minmax(0, 1fr);
  }

  .readiness-brief {
    gap: var(--space-xl);
  }

  .workbench-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .capability-boundary {
    gap: var(--space-xl);
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

  .capability-boundary {
    padding: var(--space-xl) var(--space-md);
    border-radius: var(--radius-surface);
  }

  .boundary-list div {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-xs);
  }

  .workspace-footer {
    display: grid;
    justify-items: start;
    gap: var(--space-xs);
    white-space: normal;
  }

  .workspace-footer > span[aria-hidden="true"] {
    display: none;
  }
}
</style>
