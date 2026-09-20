<script setup lang="ts">
import type { SkillTrainingPlan } from "@/api/questionBank";

defineProps<{
  plan: SkillTrainingPlan | null;
  loading: boolean;
  busy: boolean;
}>();

const focus = defineModel<string>("focus", { required: true });
</script>

<template>
  <section class="training-plan" aria-labelledby="training-plan-title">
    <div class="training-control">
      <label id="training-plan-title" for="training-focus">训练重点</label>
      <select id="training-focus" v-model="focus" :disabled="busy || loading">
        <option value="">均衡训练</option>
        <option v-for="item in plan?.categories ?? []" :key="item.category" :value="item.category">
          {{ item.category }}
        </option>
      </select>
    </div>
    <div v-if="plan" class="quota-list" aria-label="分类配额">
      <div v-for="item in plan.categories" :key="item.category" :class="{ focused: item.focused }">
        <span>{{ item.category }}</span>
        <strong>{{ item.questionCount }}</strong>
      </div>
    </div>
    <span v-else class="plan-status">{{ loading ? "正在读取配额…" : "配额不可用" }}</span>
  </section>
</template>

<style scoped lang="scss">
.training-plan {
  display: grid;
  grid-template-columns: minmax(10rem, 0.8fr) minmax(0, 2fr);
  align-items: end;
  gap: var(--space-md);
  grid-column: 1 / -1;
  padding-block: var(--space-sm);
  border-block: var(--rule-thin) solid var(--color-rule);
}

.training-control {
  display: grid;
  gap: var(--space-xs);
}

.training-control label {
  color: var(--color-ink-2);
  font-size: var(--text-xs);
  font-weight: 650;
}

.training-control select {
  width: 100%;
  min-width: 0;
  height: 2.75rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-surface);
  font-size: var(--text-sm);
}

.quota-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-sm);
}

.quota-list > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  min-height: 2.75rem;
  padding-inline: var(--space-sm);
  border-left: 3px solid var(--color-rule-2);
  color: var(--color-muted);
  background: var(--color-paper-2);
  font-size: var(--text-xs);
}

.quota-list > div.focused {
  border-color: var(--color-accent);
  color: var(--color-ink-2);
}

.quota-list strong {
  color: var(--color-accent-strong);
  font-family: var(--font-mono);
  font-size: var(--text-lg);
}

.plan-status {
  align-self: center;
  color: var(--color-muted);
  font-size: var(--text-sm);
}

@media (max-width: 48rem) {
  .training-plan,
  .quota-list {
    grid-template-columns: 1fr;
  }
}
</style>
