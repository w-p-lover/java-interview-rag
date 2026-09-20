<script setup lang="ts">
import { computed, reactive, watch } from "vue";
import { Plus, Save, Trash2, X } from "@lucide/vue";

import type { BankQuestion, BankQuestionContent } from "@/api/questionBank";

const props = defineProps<{ question: BankQuestion | null; busy: boolean; error: string }>();
const emit = defineEmits<{ save: [content: BankQuestionContent]; close: [] }>();
const form = reactive({ category: "", question: "", topicSummary: "", referenceAnswer: "", keyPoints: "", scoringRubric: "", followUps: [] as Array<{ question: string; referenceAnswer: string; keyPoints: string; scoringRubric: string }> });
const valid = computed(() => Boolean(form.category.trim() && form.question.trim() && form.topicSummary.trim()
  && form.referenceAnswer.trim() && lines(form.keyPoints).length && form.scoringRubric.trim())
  && form.followUps.every((item) => item.question.trim() && item.referenceAnswer.trim() && lines(item.keyPoints).length && item.scoringRubric.trim()));

watch(() => props.question, (question) => {
  Object.assign(form, {
    category: question?.category ?? "", question: question?.question ?? "", topicSummary: question?.topicSummary ?? "",
    referenceAnswer: question?.referenceAnswer ?? "", keyPoints: question?.keyPoints.join("\n") ?? "",
    scoringRubric: question?.scoringRubric ?? "",
    followUps: question?.followUps.map((item) => ({ ...item, keyPoints: item.keyPoints.join("\n") })) ?? [],
  });
}, { immediate: true });

function lines(value: string): string[] {
  return value.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
}

function save(): void {
  if (!valid.value || props.busy) return;
  emit("save", {
    category: form.category.trim(), question: form.question.trim(), topicSummary: form.topicSummary.trim(),
    referenceAnswer: form.referenceAnswer.trim(), keyPoints: lines(form.keyPoints), scoringRubric: form.scoringRubric.trim(),
    followUps: form.followUps.map((item) => ({ question: item.question.trim(), referenceAnswer: item.referenceAnswer.trim(), keyPoints: lines(item.keyPoints), scoringRubric: item.scoringRubric.trim() })),
  });
}
</script>

<template>
  <section id="bank-editor" class="bank-editor" aria-labelledby="bank-editor-title" tabindex="-1">
    <header class="editor-heading">
      <h2 id="bank-editor-title">{{ question ? "编辑题目" : "新建手工草稿" }}</h2>
      <button type="button" class="icon-action" :disabled="busy" aria-label="关闭编辑器" title="关闭编辑器" @click="emit('close')"><X :size="18" aria-hidden="true" /></button>
    </header>
    <p v-if="question" class="editor-meta">资料版本 {{ question.documentVersion }} · 编辑版本 {{ question.version }} · 保存后待审核</p>
    <form class="editor-form" @submit.prevent="save">
      <div class="field"><label for="bank-category">分类</label><input id="bank-category" v-model="form.category" required maxlength="100" :disabled="busy" /></div>
      <div class="field"><label for="bank-topic">知识点摘要</label><input id="bank-topic" v-model="form.topicSummary" required maxlength="1000" :disabled="busy" /></div>
      <div class="field"><label for="bank-question">题面</label><textarea id="bank-question" v-model="form.question" rows="3" required maxlength="2000" :disabled="busy"></textarea></div>
      <div class="field"><label for="bank-answer">参考答案</label><textarea id="bank-answer" v-model="form.referenceAnswer" rows="6" required maxlength="8000" :disabled="busy"></textarea></div>
      <div class="field"><label for="bank-points">答案要点（每行一项）</label><textarea id="bank-points" v-model="form.keyPoints" rows="4" required maxlength="10000" :disabled="busy"></textarea></div>
      <div class="field"><label for="bank-rubric">评分 Rubric</label><textarea id="bank-rubric" v-model="form.scoringRubric" rows="4" required maxlength="4000" :disabled="busy"></textarea></div>

      <div class="follow-up-heading">
        <h3>固定追问</h3>
        <button type="button" class="secondary-action" :disabled="busy || form.followUps.length >= 2" @click="form.followUps.push({ question: '', referenceAnswer: '', keyPoints: '', scoringRubric: '' })"><Plus :size="16" aria-hidden="true" />添加追问</button>
      </div>
      <fieldset v-for="(item, index) in form.followUps" :key="index" class="follow-up" :disabled="busy">
        <legend>追问 {{ index + 1 }} · {{ form.category.trim() || "未设置分类" }}</legend>
        <button type="button" class="icon-action remove-follow-up" :aria-label="`删除追问 ${index + 1}`" title="删除追问" @click="form.followUps.splice(index, 1)"><Trash2 :size="16" aria-hidden="true" /></button>
        <div class="field"><label :for="`bank-follow-question-${index}`">追问题面</label><textarea :id="`bank-follow-question-${index}`" v-model="item.question" required rows="2" maxlength="2000"></textarea></div>
        <div class="field"><label :for="`bank-follow-answer-${index}`">追问参考答案</label><textarea :id="`bank-follow-answer-${index}`" v-model="item.referenceAnswer" required rows="3" maxlength="8000"></textarea></div>
        <div class="field"><label :for="`bank-follow-points-${index}`">追问要点（每行一项）</label><textarea :id="`bank-follow-points-${index}`" v-model="item.keyPoints" required rows="3" maxlength="10000"></textarea></div>
        <div class="field"><label :for="`bank-follow-rubric-${index}`">追问 Rubric</label><textarea :id="`bank-follow-rubric-${index}`" v-model="item.scoringRubric" required rows="3" maxlength="4000"></textarea></div>
      </fieldset>

      <details v-if="question?.evidence.length" class="evidence">
        <summary>核对资料证据 · {{ question.evidence.length }} 条</summary>
        <div v-for="(item, index) in question.evidence" :key="index" class="evidence-item">
          <strong>{{ item.headingPath.join(' / ') || item.source }}</strong>
          <span>版本 {{ item.documentVersion }} · 片段 {{ item.chunkOrder }}<template v-if="item.pageNumber != null"> · 第 {{ item.pageNumber }} 页</template></span>
          <blockquote>{{ item.content }}</blockquote>
        </div>
      </details>
      <p v-if="error" class="editor-error" role="alert">{{ error }}</p>
      <footer class="editor-actions"><button type="submit" class="primary-action" :disabled="busy || !valid"><Save :size="17" aria-hidden="true" />{{ busy ? '正在保存…' : '保存草稿' }}</button></footer>
    </form>
  </section>
</template>

<style scoped lang="scss">
.bank-editor { min-width: 0; padding-top: var(--space-lg); border-top: var(--rule-thin) solid var(--color-rule-2); }
.editor-heading, .follow-up-heading, .editor-actions { display: flex; align-items: center; justify-content: space-between; gap: var(--space-sm); }
.editor-heading h2, .follow-up-heading h3 { margin: 0; font-size: var(--text-lg); }
.editor-meta { color: var(--color-muted); font-size: var(--text-xs); overflow-wrap: anywhere; }
.editor-form, .field, .follow-up { display: grid; gap: var(--space-sm); }
.editor-form { gap: var(--space-md); }
.field label, .follow-up legend { color: var(--color-ink-2); font-size: var(--text-sm); font-weight: 650; }
.field input, .field textarea { width: 100%; min-width: 0; padding: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink); background: var(--color-surface); }
.field textarea { resize: vertical; line-height: 1.6; }
.follow-up { position: relative; min-width: 0; margin: 0; padding: var(--space-md); border: var(--rule-thin) solid var(--color-rule); }
.follow-up legend { padding-inline: var(--space-xs); }
.remove-follow-up { justify-self: end; }
.primary-action, .secondary-action, .icon-action { display: inline-flex; align-items: center; justify-content: center; gap: var(--space-xs); min-height: 2.5rem; padding: var(--space-xs) var(--space-md); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); background: var(--color-surface); font-size: var(--text-sm); }
.icon-action { width: 2.5rem; padding: 0; }
.primary-action { border-color: var(--color-accent); color: var(--color-accent-ink); background: var(--color-accent); }
.secondary-action:not(:disabled):hover, .icon-action:not(:disabled):hover { color: var(--color-accent); border-color: var(--color-accent); }
.primary-action:not(:disabled):hover { background: var(--color-accent-strong); }
.editor-actions { justify-content: end; }
.editor-error { margin: 0; color: var(--color-error); }
.evidence { font-size: var(--text-sm); }
.evidence summary { cursor: pointer; }
.evidence-item { display: grid; gap: var(--space-xs); padding-block: var(--space-md); border-bottom: var(--rule-thin) solid var(--color-rule); overflow-wrap: anywhere; }
.evidence-item span { color: var(--color-muted); font-size: var(--text-xs); }
.evidence-item blockquote { max-width: 75ch; margin: 0; white-space: pre-wrap; }
</style>
