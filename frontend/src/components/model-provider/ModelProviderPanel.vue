<script setup lang="ts">
import { Braces, CheckCircle2, Cpu, KeyRound, LoaderCircle, PlugZap, RefreshCw, TriangleAlert } from "@lucide/vue";

import type { ModelCapability, ModelProviderTestResult } from "@/api/modelProvider";
import { useModelProviders } from "@/composables/useModelProviders";

const {
  overview,
  loading,
  loadError,
  testing,
  testResults,
  testErrors,
  refresh,
  runTest,
} = useModelProviders();

function capabilityLabel(capability: ModelCapability): string {
  return capability === "CHAT" ? "Chat" : "Embedding";
}

function capabilityDetail(capability: {
  readonly capability: ModelCapability;
  readonly expectedDimensions: number | null;
}): string {
  if (capability.capability === "EMBEDDING") {
    return `${capability.expectedDimensions ?? "-"} 维索引契约`;
  }
  return "结构化输出";
}

function resultDetail(result: ModelProviderTestResult): string {
  if (result.capability === "EMBEDDING" && result.actualDimensions !== null) {
    return `${result.actualDimensions} 维 · ${result.durationMillis} ms`;
  }
  return `${result.durationMillis} ms`;
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(new Date(value));
}
</script>

<template>
  <section id="model-provider-overview" class="provider-panel" aria-labelledby="provider-title">
    <header class="provider-heading">
      <div>
        <span>当前实例 · 启动配置</span>
        <h1 id="provider-title">模型 Provider</h1>
      </div>
      <button class="icon-button" type="button" :disabled="loading" title="刷新 Provider" @click="refresh">
        <RefreshCw :size="17" aria-hidden="true" :class="{ spinning: loading }" />
        <span class="sr-only">刷新 Provider</span>
      </button>
    </header>

    <p v-if="loadError" class="error-banner" role="alert">{{ loadError }}</p>
    <div v-else-if="loading && !overview" class="loading-state" aria-live="polite">
      <LoaderCircle :size="20" aria-hidden="true" class="spinning" />
      <span>读取当前配置</span>
    </div>

    <div v-if="overview" id="model-provider-capabilities" class="provider-grid">
      <article v-for="item in overview.capabilities" :key="item.capability" class="provider-card">
        <header class="provider-card-heading">
          <div class="capability-icon" aria-hidden="true">
            <Braces v-if="item.capability === 'CHAT'" :size="19" />
            <Cpu v-else :size="19" />
          </div>
          <div>
            <span>{{ capabilityLabel(item.capability) }}</span>
            <h2>{{ item.providerName }}</h2>
          </div>
          <span :class="['status', item.enabled ? 'healthy' : 'danger']">
            {{ item.enabled ? "已启用" : "配置不完整" }}
          </span>
        </header>

        <dl class="provider-facts">
          <div>
            <dt>模型</dt>
            <dd>{{ item.modelName }}</dd>
          </div>
          <div>
            <dt>端点</dt>
            <dd>{{ item.endpointOrigin }}</dd>
          </div>
          <div>
            <dt>能力契约</dt>
            <dd>{{ capabilityDetail(item) }}</dd>
          </div>
          <div>
            <dt>凭据</dt>
            <dd class="credential-state">
              <KeyRound :size="15" aria-hidden="true" />
              {{ item.apiKeyConfigured ? "已配置" : "未配置" }}
            </dd>
          </div>
        </dl>

        <div class="usage-list" aria-label="当前用途">
          <span v-for="usage in item.usages" :key="usage">{{ usage }}</span>
        </div>

        <footer :id="item.capability === 'CHAT' ? 'model-provider-tests' : undefined" class="provider-test">
          <div v-if="testResults[item.capability]" class="test-result" aria-live="polite">
            <CheckCircle2
              v-if="testResults[item.capability]?.success"
              :size="17"
              aria-hidden="true"
              class="healthy"
            />
            <TriangleAlert v-else :size="17" aria-hidden="true" class="danger" />
            <div>
              <strong>{{ testResults[item.capability]?.message }}</strong>
              <small>
                {{ resultDetail(testResults[item.capability]!) }} ·
                {{ formatTime(testResults[item.capability]!.testedAt) }}
              </small>
            </div>
          </div>
          <p v-else-if="testErrors[item.capability]" class="test-error" role="alert">
            {{ testErrors[item.capability] }}
          </p>
          <span v-else class="test-pending">尚未验证</span>

          <button
            class="test-button"
            type="button"
            :disabled="!item.enabled || testing[item.capability]"
            @click="runTest(item.capability)"
          >
            <LoaderCircle v-if="testing[item.capability]" :size="16" aria-hidden="true" class="spinning" />
            <PlugZap v-else :size="16" aria-hidden="true" />
            <span>{{ testing[item.capability] ? "验证中" : "验证能力" }}</span>
          </button>
        </footer>
      </article>
    </div>

    <footer v-if="overview" class="snapshot-time">
      配置快照 {{ formatTime(overview.generatedAt) }}
    </footer>
  </section>
</template>

<style scoped lang="scss">
.provider-panel { display: grid; gap: var(--space-xl); }
.provider-heading, .provider-card-heading, .provider-test { display: flex; align-items: center; }
.provider-heading { justify-content: space-between; gap: var(--space-lg); padding-bottom: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.provider-heading > div { display: grid; gap: var(--space-2xs); }
.provider-heading span, .provider-card-heading span, .provider-facts dt, .test-result small, .snapshot-time { color: var(--color-muted); font-size: var(--text-sm); }
.provider-heading h1, .provider-card-heading h2 { margin: 0; color: var(--color-ink); }
.provider-heading h1 { font-family: var(--font-display); font-size: var(--text-2xl); }
.provider-card-heading h2 { font-size: var(--text-lg); }
.icon-button { display: inline-grid; width: 2.5rem; height: 2.5rem; place-items: center; border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink-2); background: var(--color-surface); }
.icon-button:disabled { cursor: wait; opacity: 0.6; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
.provider-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-lg); }
.provider-card { display: grid; align-content: start; gap: var(--space-lg); min-width: 0; padding: var(--space-lg); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-surface); background: var(--color-paper-2); }
.provider-card-heading { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: var(--space-sm); }
.provider-card-heading > div:nth-child(2) { display: grid; gap: var(--space-3xs); min-width: 0; }
.capability-icon { display: inline-grid; width: 2.5rem; height: 2.5rem; place-items: center; border-radius: var(--radius-control); color: var(--color-accent-strong); background: var(--color-accent-soft); }
.status { align-self: start; padding-block: var(--space-2xs); font-size: var(--text-xs); font-weight: 700; }
.healthy { color: var(--color-success); }
.danger { color: var(--color-error); }
.provider-facts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-sm) var(--space-lg); margin: 0; }
.provider-facts > div { display: grid; gap: var(--space-2xs); min-width: 0; }
.provider-facts dd { min-width: 0; margin: 0; overflow-wrap: anywhere; color: var(--color-ink-2); font-family: var(--font-mono); font-size: var(--text-sm); }
.credential-state { display: flex; align-items: center; gap: var(--space-xs); }
.usage-list { display: flex; flex-wrap: wrap; gap: var(--space-xs); min-height: 2rem; }
.usage-list span { padding: var(--space-2xs) var(--space-xs); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-muted); background: var(--color-surface); font-size: var(--text-xs); }
.provider-test { justify-content: space-between; gap: var(--space-md); min-height: 3rem; padding-top: var(--space-md); border-top: var(--rule-thin) solid var(--color-rule-2); }
.test-result { display: grid; grid-template-columns: auto minmax(0, 1fr); align-items: start; gap: var(--space-xs); min-width: 0; }
.test-result > div { display: grid; gap: var(--space-3xs); min-width: 0; }
.test-result strong { color: var(--color-ink-2); font-size: var(--text-sm); overflow-wrap: anywhere; }
.test-error { margin: 0; color: var(--color-error); font-size: var(--text-sm); }
.test-pending { color: var(--color-muted); font-size: var(--text-sm); }
.test-button { display: inline-flex; flex: 0 0 auto; align-items: center; justify-content: center; gap: var(--space-xs); min-width: 7.25rem; min-height: 2.5rem; padding-inline: var(--space-md); border: var(--rule-thin) solid var(--color-ink); border-radius: var(--radius-control); color: var(--color-graphite-text); background: var(--color-graphite); font: inherit; font-size: var(--text-sm); font-weight: 700; }
.test-button:disabled { cursor: not-allowed; opacity: 0.52; }
.snapshot-time { padding-top: var(--space-md); border-top: var(--rule-thin) solid var(--color-rule-2); }
.error-banner { margin: 0; padding: var(--space-sm) var(--space-md); border: var(--rule-thin) solid var(--color-error-rule); border-radius: var(--radius-control); color: var(--color-error); background: var(--color-error-soft); font-size: var(--text-sm); }
.loading-state { display: flex; align-items: center; justify-content: center; gap: var(--space-sm); min-height: 16rem; color: var(--color-muted); }
.spinning { animation: spin 0.9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 62rem) { .provider-grid { grid-template-columns: 1fr; } }
@media (max-width: 42rem) { .provider-facts { grid-template-columns: 1fr; } .provider-test { align-items: stretch; flex-direction: column; } .test-button { width: 100%; } }
@media (prefers-reduced-motion: reduce) { .spinning { animation: none; } }
</style>
