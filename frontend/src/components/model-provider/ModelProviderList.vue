<script setup lang="ts">
import { Braces, Cpu, KeyRound, LoaderCircle, Pencil, PlugZap, Power, Trash2 } from "@lucide/vue";

import type { ModelCapability, ModelProvider, ModelProviderRequest, ModelProviderTestResult } from "@/api/modelProvider";

defineProps<{
  providers: readonly ModelProvider[];
  actionKey: string;
  testResults: Readonly<Record<string, ModelProviderTestResult>>;
  testErrors: Readonly<Record<string, string>>;
}>();
const emit = defineEmits<{
  edit: [provider: ModelProvider];
  test: [id: string, capability: ModelCapability];
  toggle: [payload: ModelProviderRequest];
  remove: [provider: ModelProvider];
}>();

function request(provider: ModelProvider, enabled: boolean): ModelProviderRequest {
  return {
    id: provider.id,
    name: provider.name,
    baseUrl: provider.baseUrl,
    apiKey: "",
    chatModel: provider.chatModel ?? "",
    embeddingModel: provider.embeddingModel ?? "",
    embeddingDimensions: provider.embeddingDimensions,
    temperature: provider.temperature,
    enabled,
    version: provider.version,
  };
}

function resultKey(id: string, capability: ModelCapability): string {
  return `${id}:${capability}`;
}
</script>

<template>
  <div class="provider-list">
    <article v-for="provider in providers" :key="provider.id" class="provider-row">
      <header class="row-heading">
        <div class="identity">
          <span class="provider-mark"><Braces :size="18" aria-hidden="true" /></span>
          <div><h2>{{ provider.name }}</h2><code>{{ provider.id }}#{{ provider.version }}</code></div>
        </div>
        <div class="badges">
          <span v-if="provider.defaultForChat">Chat 默认</span>
          <span v-if="provider.defaultForEmbedding">Embedding 默认</span>
          <span :class="provider.enabled ? 'enabled' : 'disabled'">{{ provider.enabled ? "已启用" : "已停用" }}</span>
        </div>
      </header>

      <div class="provider-meta">
        <span>{{ provider.endpointOrigin }}</span>
        <span><KeyRound :size="14" aria-hidden="true" />{{ provider.apiKeyConfigured ? "凭据已保存" : "凭据缺失" }}</span>
      </div>

      <div class="capabilities">
        <div v-if="provider.chatModel" class="capability">
          <div><Braces :size="16" aria-hidden="true" /><span>Chat</span><code>{{ provider.chatModel }}</code></div>
          <button type="button" :disabled="!provider.enabled || Boolean(actionKey)" title="验证 Chat" @click="emit('test', provider.id, 'CHAT')">
            <LoaderCircle v-if="actionKey === `test:${resultKey(provider.id, 'CHAT')}`" :size="15" class="spinning" aria-hidden="true" />
            <PlugZap v-else :size="15" aria-hidden="true" />
          </button>
          <small v-if="testResults[resultKey(provider.id, 'CHAT')]">{{ testResults[resultKey(provider.id, 'CHAT')]?.message }} · {{ testResults[resultKey(provider.id, 'CHAT')]?.durationMillis }} ms</small>
          <small v-else-if="testErrors[resultKey(provider.id, 'CHAT')]" class="danger">{{ testErrors[resultKey(provider.id, 'CHAT')] }}</small>
        </div>
        <div v-if="provider.embeddingModel" class="capability">
          <div><Cpu :size="16" aria-hidden="true" /><span>Embedding</span><code>{{ provider.embeddingModel }} · {{ provider.embeddingDimensions }} 维</code></div>
          <button type="button" :disabled="!provider.enabled || Boolean(actionKey)" title="验证 Embedding" @click="emit('test', provider.id, 'EMBEDDING')">
            <LoaderCircle v-if="actionKey === `test:${resultKey(provider.id, 'EMBEDDING')}`" :size="15" class="spinning" aria-hidden="true" />
            <PlugZap v-else :size="15" aria-hidden="true" />
          </button>
          <small v-if="testResults[resultKey(provider.id, 'EMBEDDING')]">{{ testResults[resultKey(provider.id, 'EMBEDDING')]?.message }} · {{ testResults[resultKey(provider.id, 'EMBEDDING')]?.durationMillis }} ms</small>
          <small v-else-if="testErrors[resultKey(provider.id, 'EMBEDDING')]" class="danger">{{ testErrors[resultKey(provider.id, 'EMBEDDING')] }}</small>
        </div>
      </div>

      <footer class="row-actions">
        <button type="button" :disabled="Boolean(actionKey)" @click="emit('edit', provider)"><Pencil :size="15" aria-hidden="true" />编辑</button>
        <button type="button" :disabled="Boolean(actionKey) || provider.defaultForChat || provider.defaultForEmbedding" @click="emit('toggle', request(provider, !provider.enabled))"><Power :size="15" aria-hidden="true" />{{ provider.enabled ? "停用" : "启用" }}</button>
        <button class="danger-button" type="button" :disabled="Boolean(actionKey) || provider.defaultForChat || provider.defaultForEmbedding" @click="emit('remove', provider)"><Trash2 :size="15" aria-hidden="true" />删除</button>
      </footer>
    </article>
  </div>
</template>

<style scoped lang="scss">
.provider-list { display: grid; gap: var(--space-md); }
.provider-row { display: grid; gap: var(--space-md); padding-block: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.row-heading, .identity, .provider-meta, .capability > div, .row-actions, .badges { display: flex; align-items: center; }
.row-heading { justify-content: space-between; gap: var(--space-md); }
.identity { gap: var(--space-sm); min-width: 0; }
.identity > div { display: grid; min-width: 0; }
.identity h2 { margin: 0; color: var(--color-ink); font-size: var(--text-lg); }
.identity code, .capability code { overflow-wrap: anywhere; color: var(--color-muted); font-family: var(--font-mono); font-size: var(--text-xs); }
.provider-mark { display: inline-grid; flex: 0 0 auto; width: 2.5rem; height: 2.5rem; place-items: center; border-radius: var(--radius-control); color: var(--color-accent-strong); background: var(--color-accent-soft); }
.badges { flex-wrap: wrap; justify-content: flex-end; gap: var(--space-xs); }
.badges span { padding: var(--space-3xs) var(--space-xs); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-muted); font-size: var(--text-xs); }
.badges .enabled { color: var(--color-success); border-color: var(--color-success-rule); }
.badges .disabled { color: var(--color-error); border-color: var(--color-error-rule); }
.provider-meta { flex-wrap: wrap; gap: var(--space-sm) var(--space-lg); color: var(--color-muted); font-size: var(--text-sm); }
.provider-meta span { display: inline-flex; align-items: center; gap: var(--space-xs); }
.capabilities { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-sm); }
.capability { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: var(--space-xs); padding: var(--space-sm); border: var(--rule-thin) solid var(--color-rule); border-radius: var(--radius-control); background: var(--color-paper-2); }
.capability > div { gap: var(--space-xs); min-width: 0; color: var(--color-ink-2); font-size: var(--text-sm); }
.capability button { display: inline-grid; width: 2rem; height: 2rem; place-items: center; border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); background: var(--color-surface); }
.capability small { grid-column: 1 / -1; color: var(--color-success); font-size: var(--text-xs); }
.capability .danger { color: var(--color-error); }
.row-actions { justify-content: flex-end; gap: var(--space-xs); }
.row-actions button { display: inline-flex; align-items: center; gap: var(--space-xs); min-height: 2.25rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); background: transparent; font-size: var(--text-sm); }
.row-actions .danger-button { color: var(--color-error); }
.spinning { animation: spin 0.9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 42rem) { .row-heading { align-items: flex-start; flex-direction: column; } .badges { justify-content: flex-start; } .capabilities { grid-template-columns: 1fr; } .row-actions { justify-content: stretch; } .row-actions button { flex: 1; justify-content: center; } }
@media (prefers-reduced-motion: reduce) { .spinning { animation: none; } }
</style>
