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
  <main class="workbench-page">
    <header class="masthead">
      <div class="brand">
        <span class="brand-seal">J</span>
        <span>
          <strong>Java Interview RAG</strong>
          <small>knowledge workshop</small>
        </span>
      </div>
      <div class="runtime-badge">
        <span aria-hidden="true"></span>
        session memory
      </div>
    </header>

    <section class="intro">
      <div class="intro-copy">
        <p class="kicker">Document intake / phase 01</p>
        <h1>把 Markdown 变成<br /><em>可追踪</em>的知识切片</h1>
        <p class="intro-note">
          先看清来源、版本、标题路径和切片顺序，再进入向量化。当前页面只展示已经真实实现的文档处理链。
        </p>
      </div>
      <PipelineRail :state="state" />
    </section>

    <section class="workbench-grid">
      <ImportPanel :busy="state === 'loading'" :error="error" @submit="handleImport" />
      <ChunkLedger :result="result" />
    </section>

    <footer class="page-footer">
      <span>JAVA 17 / SPRING BOOT 3 / VUE 3</span>
      <span>尚未接入持久化、Embedding 与向量库</span>
    </footer>
  </main>
</template>

<style scoped lang="scss">
.workbench-page {
  width: min(100% - 2rem, 90rem);
  margin: 0 auto;
  padding: 1.25rem 0 2rem;
}

.masthead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0 1.1rem;
  border-bottom: 1px solid var(--lead);
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.brand-seal {
  display: grid;
  width: 2.2rem;
  height: 2.2rem;
  place-items: center;
  border-radius: 0.45rem;
  color: #fff;
  background: var(--cinnabar);
  font-family: var(--font-display);
  font-size: 1.15rem;
  box-shadow: 0.18rem 0.18rem 0 var(--ink);
}

.brand > span:last-child {
  display: grid;
}

.brand strong {
  font-family: var(--font-display);
  font-size: 0.9rem;
}

.brand small {
  color: var(--ink-soft);
  font-family: var(--font-mono);
  font-size: 0.58rem;
  letter-spacing: 0.11em;
  text-transform: uppercase;
}

.runtime-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--ink-soft);
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.05em;
}

.runtime-badge span {
  width: 0.45rem;
  height: 0.45rem;
  border-radius: 50%;
  background: var(--jade);
  box-shadow: 0 0 0 0.22rem var(--jade-soft);
}

.intro {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(28rem, 1.1fr);
  align-items: end;
  gap: 3rem;
  padding: clamp(3rem, 7vw, 6rem) 0 clamp(2rem, 5vw, 4rem);
}

.kicker {
  margin: 0 0 0.9rem;
  color: var(--cinnabar);
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(2.25rem, 5vw, 4.7rem);
  font-weight: 700;
  letter-spacing: -0.05em;
  line-height: 1.12;
}

h1 em {
  position: relative;
  color: var(--jade);
  font-style: normal;
}

h1 em::after {
  content: "";
  position: absolute;
  right: -0.08em;
  bottom: 0.06em;
  left: -0.08em;
  height: 0.18em;
  background: var(--cinnabar-soft);
  transform: rotate(-1.5deg);
  z-index: -1;
}

.intro-note {
  max-width: 34rem;
  margin: 1.2rem 0 0;
  color: var(--ink-soft);
  font-size: 0.86rem;
}

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(19rem, 0.78fr) minmax(0, 1.22fr);
  align-items: start;
  gap: 1.2rem;
}

.page-footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.4rem 0 0;
  color: var(--ink-soft);
  font-family: var(--font-mono);
  font-size: 0.62rem;
  letter-spacing: 0.04em;
}

@media (max-width: 980px) {
  .intro {
    grid-template-columns: 1fr;
    align-items: start;
    gap: 2rem;
  }

  .workbench-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .workbench-page {
    width: min(100% - 1rem, 90rem);
    padding-top: 0.5rem;
  }

  .runtime-badge {
    display: none;
  }

  .intro {
    padding-top: 2.4rem;
  }

  h1 {
    font-size: clamp(2rem, 12vw, 3.1rem);
  }

  .page-footer {
    flex-direction: column;
  }
}
</style>
