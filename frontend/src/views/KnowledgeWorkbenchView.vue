<script setup lang="ts">
import { shallowRef } from "vue";

import { importMarkdown, type MarkdownImportResponse } from "@/api/knowledge";
import { resolveApiError } from "@/api/request";
import ChunkLedger from "@/components/ChunkLedger.vue";
import ImportPanel from "@/components/ImportPanel.vue";
import PipelineRail from "@/components/PipelineRail.vue";

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
</script>

<template>
  <main class="workspace">
    <header class="topbar">
      <a class="brand" href="#main-content" aria-label="面试准备台首页">
        <span class="brand-mark" aria-hidden="true">RAG</span>
        <span class="brand-copy">
          <strong>面试准备台</strong>
          <small>Java 求职训练</small>
        </span>
      </a>

      <nav class="primary-nav" aria-label="主导航">
        <a class="nav-item is-current" href="#main-content" aria-current="page">准备工作</a>
        <span class="nav-item is-muted">模拟面试</span>
        <span class="nav-item is-muted">训练记录</span>
      </nav>

      <div class="session-state">
        <span class="session-dot" aria-hidden="true"></span>
        <span>当前会话</span>
      </div>
    </header>

    <section id="main-content" class="workspace-heading" aria-labelledby="page-title">
      <div>
        <p class="section-label">求职准备 / 阶段 1</p>
        <h1 id="page-title">先把简历和资料准备好</h1>
        <p class="heading-note">
          导入简历、岗位描述或面试资料，先完成版本和结构检查，再进入模拟面试。
        </p>
      </div>
      <div class="phase-summary">
        <span class="phase-label">当前可用</span>
        <strong>资料导入与检查</strong>
        <small>候选人画像和面试模块将在后续接入</small>
      </div>
    </section>

    <PipelineRail :state="state" />

    <section class="content-grid" aria-label="资料准备工作区">
      <div class="main-column">
        <ImportPanel :busy="state === 'loading'" :error="error" @submit="handleImport" />

        <section class="roadmap-panel" aria-labelledby="roadmap-title">
          <header class="panel-heading compact-heading">
            <div>
              <span class="panel-kicker">求职训练主线</span>
              <h2 id="roadmap-title">从资料到面试</h2>
            </div>
            <span class="panel-note">后续模块</span>
          </header>
          <ol class="roadmap-list">
            <li class="is-available">
              <span class="roadmap-step">1</span>
              <span>导入简历和岗位资料</span>
              <small>当前可用</small>
            </li>
            <li>
              <span class="roadmap-step">2</span>
              <span>确认候选人画像</span>
              <small>待接入</small>
            </li>
            <li>
              <span class="roadmap-step">3</span>
              <span>开始模拟面试与追问</span>
              <small>待接入</small>
            </li>
            <li>
              <span class="roadmap-step">4</span>
              <span>查看反馈和复习计划</span>
              <small>待接入</small>
            </li>
          </ol>
        </section>
      </div>

      <div class="results-column">
        <ChunkLedger :result="result" />
      </div>
    </section>

    <footer class="workspace-footer">
      <span>当前版本支持 Markdown 资料导入和结构检查</span>
      <span>登记状态只保存在本次会话</span>
    </footer>
  </main>
</template>

<style scoped lang="scss">
.workspace {
  width: min(100% - 3rem, 78rem);
  margin: 0 auto;
  padding: 1.25rem 0 2.5rem;
}

.topbar {
  display: flex;
  align-items: center;
  min-height: 3.5rem;
  border-bottom: 1px solid var(--border);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  color: inherit;
  text-decoration: none;
}

.brand-mark {
  display: inline-grid;
  width: 2.5rem;
  height: 2rem;
  place-items: center;
  border: 1px solid var(--accent);
  border-radius: 0.35rem;
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.brand-copy {
  display: grid;
  gap: 0.05rem;
}

.brand-copy strong {
  font-size: 0.9rem;
  font-weight: 650;
}

.brand-copy small,
.session-state,
.nav-item,
.section-label,
.panel-kicker,
.phase-label,
.panel-note,
.workspace-footer {
  color: var(--ink-muted);
  font-size: 0.72rem;
}

.brand-copy small {
  font-size: 0.68rem;
}

.primary-nav {
  display: flex;
  gap: 1.2rem;
  margin-left: 3rem;
}

.nav-item {
  padding: 0.55rem 0;
  color: var(--ink-muted);
  text-decoration: none;
}

.nav-item.is-current {
  color: var(--ink);
  box-shadow: inset 0 -2px 0 var(--accent);
}

.nav-item.is-muted {
  color: var(--ink-faint);
}

.session-state {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 0.66rem;
}

.session-dot {
  width: 0.45rem;
  height: 0.45rem;
  border-radius: 50%;
  background: var(--success);
}

.workspace-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 2rem;
  padding: 2.75rem 0 1.75rem;
}

.section-label,
.panel-kicker,
.phase-label {
  display: block;
  margin: 0 0 0.55rem;
  color: var(--accent);
  font-weight: 650;
}

h1,
h2 {
  margin: 0;
  color: var(--ink);
  font-weight: 650;
  letter-spacing: -0.02em;
  text-wrap: balance;
}

h1 {
  max-width: 34rem;
  font-size: 2.25rem;
  line-height: 1.14;
}

.heading-note {
  max-width: 42rem;
  margin: 0.8rem 0 0;
  color: var(--ink-muted);
  font-size: 0.92rem;
  text-wrap: pretty;
}

.phase-summary {
  display: grid;
  min-width: 14rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
}

.phase-summary strong {
  font-size: 0.88rem;
  font-weight: 650;
}

.phase-summary small {
  margin-top: 0.3rem;
  color: var(--ink-muted);
  font-size: 0.72rem;
  line-height: 1.45;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(20rem, 0.85fr) minmax(0, 1.15fr);
  align-items: start;
  gap: 1rem;
}

.main-column,
.results-column {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.roadmap-panel {
  padding: 1.25rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.15rem;
}

.compact-heading {
  margin-bottom: 0.85rem;
}

.panel-kicker {
  margin-bottom: 0.25rem;
  color: var(--ink-muted);
  font-size: 0.68rem;
  font-weight: 600;
}

.panel-note {
  padding-top: 0.15rem;
  white-space: nowrap;
}

h2 {
  font-size: 1.15rem;
  line-height: 1.3;
}

.roadmap-list {
  display: grid;
  gap: 0.1rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.roadmap-list li {
  display: grid;
  grid-template-columns: 1.5rem minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.65rem;
  min-height: 2.35rem;
  color: var(--ink-muted);
  font-size: 0.82rem;
}

.roadmap-list li.is-available {
  color: var(--ink);
  font-weight: 600;
}

.roadmap-step {
  display: inline-grid;
  width: 1.35rem;
  height: 1.35rem;
  place-items: center;
  border: 1px solid var(--border-strong);
  border-radius: 50%;
  color: var(--ink-muted);
  font-family: var(--font-mono);
  font-size: 0.64rem;
}

.is-available .roadmap-step {
  border-color: var(--accent);
  color: var(--accent);
}

.roadmap-list small {
  color: var(--ink-faint);
  font-size: 0.68rem;
}

.workspace-footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding-top: 1.25rem;
  font-family: var(--font-mono);
  font-size: 0.64rem;
}

@media (max-width: 820px) {
  .workspace-heading {
    display: grid;
    gap: 1.25rem;
  }

  .phase-summary {
    min-width: 0;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .workspace {
    width: min(100% - 1.5rem, 78rem);
    padding-top: 0.75rem;
  }

  .primary-nav {
    display: none;
  }

  .workspace-heading {
    padding: 2.25rem 0 1.5rem;
  }

  h1 {
    font-size: 1.8rem;
  }

  .workspace-footer {
    flex-direction: column;
  }
}
</style>
