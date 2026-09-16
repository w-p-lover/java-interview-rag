<script setup lang="ts">
import { Activity, AlertTriangle, RefreshCw, Timer, Wifi } from "@lucide/vue";
import { computed } from "vue";

import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import type { RetrievalChannelMetric } from "@/api/observability";
import { useObservability } from "@/composables/useObservability";

const { result, loading, error, refresh } = useObservability();

const retrievalChannels = computed(() => {
  if (!result.value) {
    return [];
  }
  return [
    { key: "dense", name: "Dense", detail: "Qdrant / Embedding", metric: result.value.retrieval.dense },
    { key: "sparse", name: "Sparse", detail: "Lucene BM25", metric: result.value.retrieval.sparse },
  ].map((channel) => ({ ...channel, state: retrievalState(channel.metric) }));
});

function retrievalState(metric: RetrievalChannelMetric): { label: string; tone: string } {
  if (metric.queueCapacity > 0 && metric.queued >= metric.queueCapacity) {
    return { label: "队列已满", tone: "danger" };
  }
  if (metric.queued > 0) {
    return { label: "正在排队", tone: "warning" };
  }
  if (metric.threadCapacity > 0 && metric.active >= metric.threadCapacity) {
    return { label: "线程满载", tone: "warning" };
  }
  if (metric.rejected > 0) {
    return { label: "存在拒绝记录", tone: "danger" };
  }
  return { label: "运行正常", tone: "healthy" };
}

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

      <section id="observability-retrieval" class="operations-section" aria-labelledby="retrieval-title">
        <header>
          <div>
            <span>RAG 检索隔离执行器</span>
            <h2 id="retrieval-title">检索容量</h2>
          </div>
          <small>Dense 与 Sparse 独立线程池 · 默认单路 5 秒截止</small>
        </header>
        <div class="operations-table-wrap">
          <table class="operations-table retrieval-table">
            <thead>
              <tr>
                <th scope="col">通道</th>
                <th scope="col">状态</th>
                <th scope="col">活动线程</th>
                <th scope="col">排队</th>
                <th scope="col">已提交</th>
                <th scope="col">执行结束</th>
                <th scope="col">超时</th>
                <th scope="col">拒绝</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="channel in retrievalChannels" :key="channel.key">
                <th scope="row">
                  <strong>{{ channel.name }}</strong>
                  <small>{{ channel.detail }}</small>
                </th>
                <td :class="channel.state.tone">
                  {{ channel.state.label }}
                </td>
                <td>{{ channel.metric.active }} / {{ channel.metric.threadCapacity }}</td>
                <td>{{ channel.metric.queued }} / {{ channel.metric.queueCapacity }}</td>
                <td>{{ channel.metric.submitted }}</td>
                <td>{{ channel.metric.completed }}</td>
                <td :class="{ danger: channel.metric.timedOut > 0 }">{{ channel.metric.timedOut }}</td>
                <td :class="{ danger: channel.metric.rejected > 0 }">{{ channel.metric.rejected }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="task-capacity-note">超时任务会从等待队列移除；活动数持续满载或排队增长时，再依据真实瓶颈调整容量。</p>
      </section>

      <section id="observability-tasks" class="operations-section" aria-labelledby="tasks-title">
        <header>
          <div>
            <span>知识资料异步执行器</span>
            <h2 id="tasks-title">任务容量</h2>
          </div>
          <small>2 个工作线程 · 8 个排队槽位 · 默认 10 分钟截止</small>
        </header>
        <div class="task-metrics">
          <article><span>排队中</span><strong>{{ result.tasks.queued }}</strong></article>
          <article><span>执行中</span><strong>{{ result.tasks.running }}</strong></article>
          <article><span>已完成</span><strong>{{ result.tasks.succeeded }}</strong></article>
          <article><span>失败</span><strong class="danger">{{ result.tasks.failed }}</strong></article>
          <article><span>已取消</span><strong>{{ result.tasks.cancelled }}</strong></article>
          <article><span>被拒绝</span><strong class="danger">{{ result.tasks.rejected }}</strong></article>
          <article><span>超时</span><strong class="danger">{{ result.tasks.timedOut }}</strong></article>
        </div>
        <p class="task-capacity-note">排队数达到 8 时，新任务会被拒绝；被拒绝次数用于识别容量边界，不代表资料正文处理失败。</p>
      </section>

      <section id="observability-feedback" class="operations-section" aria-labelledby="feedback-title">
        <header>
          <div>
            <span>RAG 真实使用反馈</span>
            <h2 id="feedback-title">回答反馈</h2>
          </div>
          <small>低敏元数据 · 人工确认后进入评估</small>
        </header>
        <div class="feedback-summary">
          <article><span>总反馈</span><strong>{{ result.feedback.total }}</strong></article>
          <article><span>有帮助</span><strong>{{ result.feedback.helpful }}</strong></article>
          <article><span>没帮助</span><strong class="danger">{{ result.feedback.notHelpful }}</strong></article>
          <article><span>待审阅</span><strong class="danger">{{ result.feedback.pending }}</strong></article>
          <article><span>已纳入评估</span><strong>{{ result.feedback.evaluation }}</strong></article>
        </div>
        <RouterLink to="/observability/feedback" class="feedback-link">打开反馈审阅中心</RouterLink>
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
.warning { color: var(--color-warning); font-weight: 700; }
.healthy { color: var(--color-success); font-weight: 700; }
.retrieval-table th:first-child { display: grid; gap: var(--space-2xs); }
.retrieval-table th:first-child strong { color: var(--color-ink); }
.retrieval-table th:first-child small { color: var(--color-muted); font-weight: 400; }
.task-metrics { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: var(--space-sm); }
.task-metrics article { display: grid; gap: var(--space-xs); padding: var(--space-md); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); background: var(--color-paper-2); }
.task-metrics span, .task-capacity-note { color: var(--color-muted); font-size: var(--text-xs); }
.task-metrics strong { color: var(--color-ink); font-family: var(--font-mono); font-size: var(--text-xl); }
.task-capacity-note { margin: 0; }
.feedback-summary { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: var(--space-sm); }
.feedback-summary article { display: grid; gap: var(--space-xs); padding: var(--space-md); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); background: var(--color-paper-2); }
.feedback-summary span { color: var(--color-muted); font-size: var(--text-xs); }
.feedback-summary strong { color: var(--color-ink); font-family: var(--font-mono); font-size: var(--text-xl); }
.feedback-link { color: var(--color-accent); font-size: var(--text-sm); font-weight: 700; }
.error-banner { padding: var(--space-sm) var(--space-md); border: var(--rule-thin) solid var(--color-error-rule); border-radius: var(--radius-control); color: var(--color-error); background: var(--color-error-soft); font-size: var(--text-sm); }
.empty-state, .loading-state { display: flex; align-items: center; justify-content: center; gap: var(--space-sm); min-height: 16rem; color: var(--color-muted); }
.empty-state svg { color: var(--color-accent); }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 68rem) { .observability-workspace { margin-left: 0; } }
@media (max-width: 68rem) { .task-metrics { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 42rem) { .observability-workspace { width: calc(100% - (var(--space-md) * 2)); margin-inline: auto; padding: var(--space-lg) 0 var(--space-xl); } .observability-heading { align-items: start; flex-direction: column; } .metric-cards, .task-metrics { grid-template-columns: 1fr; } .metric-card { min-height: auto; } }
@media (max-width: 68rem) { .feedback-summary { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 42rem) { .feedback-summary { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) { .spinning { animation: none; } }
</style>
