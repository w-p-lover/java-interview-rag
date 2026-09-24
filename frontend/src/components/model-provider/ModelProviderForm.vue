<script setup lang="ts">
import { computed, reactive, watch } from "vue";
import { Check, X } from "@lucide/vue";

import type { ModelProvider, ModelProviderRequest } from "@/api/modelProvider";

const props = defineProps<{ provider: ModelProvider | null; busy: boolean }>();
const emit = defineEmits<{ submit: [payload: ModelProviderRequest, editing: boolean]; cancel: [] }>();

const form = reactive<ModelProviderRequest>(emptyForm());
const editing = computed(() => props.provider !== null);

watch(() => props.provider, (provider) => {
  Object.assign(form, provider ? {
    id: provider.id,
    name: provider.name,
    baseUrl: provider.baseUrl,
    apiKey: "",
    chatModel: provider.chatModel ?? "",
    embeddingModel: provider.embeddingModel ?? "",
    embeddingDimensions: provider.embeddingDimensions,
    temperature: provider.temperature,
    enabled: provider.enabled,
    version: provider.version,
  } : emptyForm());
}, { immediate: true });

function emptyForm(): ModelProviderRequest {
  return {
    id: "",
    name: "",
    baseUrl: "",
    apiKey: "",
    chatModel: "",
    embeddingModel: "",
    embeddingDimensions: null,
    temperature: 0.2,
    enabled: true,
    version: null,
  };
}

function submit(): void {
  emit("submit", {
    ...form,
    embeddingDimensions: form.embeddingModel ? form.embeddingDimensions : null,
    temperature: form.chatModel ? form.temperature : null,
  }, editing.value);
}
</script>

<template>
  <form class="provider-form" @submit.prevent="submit">
    <header class="form-heading">
      <div>
        <span>{{ editing ? "编辑配置" : "新增配置" }}</span>
        <h2>{{ editing ? provider?.name : "新 Provider" }}</h2>
      </div>
      <button class="icon-button" type="button" title="关闭编辑" @click="emit('cancel')">
        <X :size="17" aria-hidden="true" />
      </button>
    </header>

    <div class="form-grid">
      <label class="field"><span>Provider ID</span><input v-model.trim="form.id" maxlength="32" pattern="[a-z0-9][a-z0-9-]*" required :disabled="editing || busy" /></label>
      <label class="field"><span>显示名称</span><input v-model.trim="form.name" maxlength="100" required :disabled="busy" /></label>
      <label class="field field-wide"><span>OpenAI-compatible 地址</span><input v-model.trim="form.baseUrl" type="url" maxlength="500" required :disabled="busy" /></label>
      <label class="field field-wide"><span>API Key</span><input v-model.trim="form.apiKey" type="password" autocomplete="new-password" :required="!editing" :placeholder="editing ? '留空以保留现有密钥' : ''" :disabled="busy" /></label>
      <label class="field"><span>Chat 模型</span><input v-model.trim="form.chatModel" maxlength="100" :disabled="busy" /></label>
      <label class="field"><span>温度</span><input v-model.number="form.temperature" type="number" min="0" max="2" step="0.1" :disabled="busy || !form.chatModel" /></label>
      <label class="field"><span>Embedding 模型</span><input v-model.trim="form.embeddingModel" maxlength="100" :disabled="busy" /></label>
      <label class="field"><span>Embedding 维度</span><input v-model.number="form.embeddingDimensions" type="number" min="1" step="1" :required="Boolean(form.embeddingModel)" :disabled="busy || !form.embeddingModel" /></label>
    </div>

    <footer class="form-actions">
      <label class="enable-toggle"><input v-model="form.enabled" type="checkbox" :disabled="busy" /><span>启用 Provider</span></label>
      <button class="primary-button" type="submit" :disabled="busy || (!form.chatModel && !form.embeddingModel)">
        <Check :size="16" aria-hidden="true" />
        {{ busy ? "保存中" : "保存配置" }}
      </button>
    </footer>
  </form>
</template>

<style scoped lang="scss">
.provider-form { display: grid; gap: var(--space-lg); padding-block: var(--space-lg); border-block: var(--rule-thin) solid var(--color-rule-2); }
.form-heading, .form-actions { display: flex; align-items: center; justify-content: space-between; gap: var(--space-md); }
.form-heading > div { display: grid; gap: var(--space-3xs); }
.form-heading span { color: var(--color-muted); font-size: var(--text-xs); }
.form-heading h2 { margin: 0; color: var(--color-ink); font-size: var(--text-lg); }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-md); }
.field { display: grid; gap: var(--space-xs); min-width: 0; color: var(--color-ink-2); font-size: var(--text-xs); font-weight: 650; }
.field-wide { grid-column: 1 / -1; }
.field input { width: 100%; min-width: 0; height: 2.75rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink); background: var(--color-surface); font-size: var(--text-sm); font-weight: 400; }
.enable-toggle { display: inline-flex; align-items: center; gap: var(--space-xs); color: var(--color-ink-2); font-size: var(--text-sm); }
.primary-button { display: inline-flex; align-items: center; gap: var(--space-xs); min-height: 2.5rem; padding-inline: var(--space-md); border: var(--rule-thin) solid var(--color-ink); border-radius: var(--radius-control); color: var(--color-graphite-text); background: var(--color-graphite); font-weight: 700; }
.icon-button { display: inline-grid; width: 2.5rem; height: 2.5rem; place-items: center; border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); background: transparent; }
@media (max-width: 42rem) { .form-grid { grid-template-columns: 1fr; } .field-wide { grid-column: auto; } .form-actions { align-items: stretch; flex-direction: column; } .primary-button { justify-content: center; width: 100%; } }
</style>
