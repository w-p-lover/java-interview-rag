<script setup lang="ts">
import { Activity, AlertTriangle, RefreshCw, Timer, Wifi } from "@lucide/vue";

import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import { useObservability } from "@/composables/useObservability";

const { result, loading, error, refresh } = useObservability();

function formatRate(value: number): string {
  return `${(value * 100).toFixed(1)}%`;
}

function formatMillis(value: number): string {
  return value < 1 ? "< 1 ms" : `${Math.round(value)} ms`;
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(new Date(value));
}
</script>

<template>
  <WorkspaceHeader mode="observability" />
  <main id="observability-overview" class="observability-workspace" tabindex="-1">
    <header class="observability-heading">
      <div>
        <span>当前实例 · 只读</span>
        <h1>运行观测</h1>
      </div>
      <button class="refresh-button" type="button" :disabled="loading" @click="refresh">
        <RefreshCw :size="16" aria-hidden="true" :class="{ spinning: loading }" />
        <span>{{ loading ? "刷新中" : "刷新指标" }}</span>
      </button>
    </header>

    <p v-if="error" class="error-banner" role="alert">{{ error }}</p>

    <template v-if="result">
      <section class="metric-cards" aria-label="运行概览">
        <article class="metric-card">
          <Wifi :size="19" aria-hidden="true" />
          <span>业务请求</span>
          <strong>{{ result.totalRequests }}</strong>
          <small>当前进程累计</small>
        </article>
        <article class="metric-card">
          <Activity :size="19" aria-hidden="true" />
          <span>回答缓存命中</span>
          <strong>{{ result.cache.hitCount }}</strong>
          <small>Redis 命中次数</small>
        </article>
        <article class="metric-card">
          <Activity :size="19" aria-hidden="true" />
          <span>缓存旁路</span>
          <strong>{{ result.cache.bypassCount }}</strong>
          <small>缓存异常后回源</small>
        </article>
        <article class="metric-card">
          <Activity :size="19" aria-hidden="true" />
          <span>缓存未命中</span>
          <strong>{{ result.cache.missCount }}</strong>
          <small>正常回源请求</small>
        </article>
        <article class="metric-card">
          <AlertTriangle :size="19" aria-hidden="true" />
          <span>失败请求</span>
          <strong>{{ result.totalFailures }}</strong>
          <small>客户端与服务端错误</small>
        </article>
        <article class="metric-card">
          <Timer :size="19" aria-hidden="true" />
          <span>已观测路由</span>
          <strong>{{ result.operations.length }}</strong>
          <small>按稳定路由聚合</small>
        </article>
      </section>

      <section id="observability-operations" class="operations-section" aria-labelledby="operations-title">
        <header>
          <div>
            <span>HTTP 业务链路</span>
            <h2 id="operations-title">路由表现</h2>
          </div>
          <small>更新于 {{ formatTime(result.generatedAt) }}</small>
        </header>

        <div v-if="result.operations.length" class="operations-table-wrap">
          <table class="operations-table">
            <thead>
              <tr>
                <th scope="col">业务路由</th>
                <th scope="col">请求量</th>
                <th scope="col">失败率</th>
                <th scope="col">平均耗时</th>
                <th scope="col">最大耗时</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="operation in result.operations" :key="operation.operation">
                <th scope="row"><code>{{ operation.operation }}</code></th>
                <td>{{ operation.requestCount }}</td>
                <td :class="{ danger: operation.failureRate > 0 }">{{ formatRate(operation.failureRate) }}</td>
                <td>{{ formatMillis(operation.averageLatencyMillis) }}</td>
                <td>{{ formatMillis(operation.maxLatencyMillis) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-state">
          <Activity :size="24" aria-hidden="true" />
          <p>还没有可统计的业务请求，刷新页面后再试。</p>
        </div>
      </section>
    </template>
    <div v-else-if="loading" class="loading-state" aria-live="polite">正在读取运行指标</div>
  </main>
</template>

<style scoped lang="scss">
.observability-workspace { display: grid; gap: var(--space-xl); margin-left: var(--workspace-rail); padding: var(--space-xl) clamp(var(--space-lg), 4vw, var(--space-3xl)) var(--space-2xl); }
.observability-heading, .observability-heading > div, .operations-section > header, .metric-card { display: flex; }
.observability-heading, .operations-section > header { align-items: end; justify-content: space-between; gap: var(--space-lg); padding-bottom: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.observability-heading > div, .operations-section > header > div { flex-direction: column; gap: var(--space-2xs); }
h1, h2, p { margin: 0; }
h1 { color: var(--color-ink); font-family: var(--font-display); font-size: var(--text-2xl); }
h2 { color: var(--color-ink); font-size: var(--text-xl); }
.observability-heading span, .operations-section span, .operations-section small, .metric-card span, .metric-card small { color: var(--color-muted); font-size: var(--text-sm); }
.refresh-button { display: inline-flex; align-items: center; gap: var(--space-xs); min-height: 2.5rem; padding-inline: var(--space-md); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink-2); background: var(--color-surface); font: inherit; font-size: var(--text-sm); font-weight: 700; }
.refresh-button:disabled { cursor: wait; opacity: 0.6; }
.spinning { animation: spin 0.9s linear infinite; }
.metric-cards { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-md); }
.metric-card { flex-direction: column; gap: var(--space-xs); min-height: 9.5rem; padding: var(--space-lg); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-surface); background: var(--color-paper-2); }
.metric-card svg { color: var(--color-accent); }
.metric-card strong { color: var(--color-ink); font-family: var(--font-mono); font-size: var(--text-2xl); }
.operations-section { display: grid; gap: var(--space-lg); }
.operations-table-wrap { overflow-x: auto; border-block: var(--rule-thin) solid var(--color-rule-2); }
.operations-table { width: 100%; border-collapse: collapse; color: var(--color-ink-2); font-size: var(--text-sm); text-align: left; }
th, td { padding: var(--space-md); border-bottom: var(--rule-thin) solid var(--color-rule-2); white-space: nowrap; }
thead th { color: var(--color-muted); font-weight: 700; }
tbody tr:last-child th, tbody tr:last-child td { border-bottom: 0; }
code { color: var(--color-ink); font-family: var(--font-mono); font-size: var(--text-xs); }
.danger { color: var(--color-error); font-weight: 700; }
.error-banner { padding: var(--space-sm) var(--space-md); border: var(--rule-thin) solid var(--color-error-rule); border-radius: var(--radius-control); color: var(--color-error); background: var(--color-error-soft); font-size: var(--text-sm); }
.empty-state, .loading-state { display: flex; align-items: center; justify-content: center; gap: var(--space-sm); min-height: 16rem; color: var(--color-muted); }
.empty-state svg { color: var(--color-accent); }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 68rem) { .observability-workspace { margin-left: 0; } }
@media (max-width: 42rem) { .observability-workspace { width: calc(100% - (var(--space-md) * 2)); margin-inline: auto; padding: var(--space-lg) 0 var(--space-xl); } .observability-heading { align-items: start; flex-direction: column; } .metric-cards { grid-template-columns: 1fr; } .metric-card { min-height: auto; } }
@media (prefers-reduced-motion: reduce) { .spinning { animation: none; } }
</style>
