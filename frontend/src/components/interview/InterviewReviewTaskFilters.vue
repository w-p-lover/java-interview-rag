<script setup lang="ts">
import { computed, shallowRef, watch } from "vue";
import { ListFilter, RotateCcw } from "@lucide/vue";

import type { InterviewReviewTask, InterviewReviewTaskQuery } from "@/api/interview";

type StatusFilter = "" | InterviewReviewTask["status"];

const props = defineProps<{
  query: InterviewReviewTaskQuery;
  busy: boolean;
}>();

const emit = defineEmits<{
  apply: [query: InterviewReviewTaskQuery];
}>();

const status = shallowRef<StatusFilter>("");
const plannedDateFrom = shallowRef("");
const plannedDateTo = shallowRef("");

const dateRangeError = computed(() => (
  plannedDateFrom.value && plannedDateTo.value && plannedDateTo.value < plannedDateFrom.value
    ? "结束日期不能早于开始日期"
    : ""
));

watch(
  () => props.query,
  (query) => {
    status.value = query.status ?? "";
    plannedDateFrom.value = query.plannedDateFrom ?? "";
    plannedDateTo.value = query.plannedDateTo ?? "";
  },
  { immediate: true },
);

function apply(): void {
  if (dateRangeError.value) {
    return;
  }
  emit("apply", {
    ...(status.value ? { status: status.value } : {}),
    ...(plannedDateFrom.value ? { plannedDateFrom: plannedDateFrom.value } : {}),
    ...(plannedDateTo.value ? { plannedDateTo: plannedDateTo.value } : {}),
  });
}

function clear(): void {
  status.value = "";
  plannedDateFrom.value = "";
  plannedDateTo.value = "";
  emit("apply", {});
}
</script>

<template>
  <form class="review-filters" @submit.prevent="apply">
    <fieldset class="status-filter">
      <legend>任务状态</legend>
      <div class="status-options">
        <label>
          <input v-model="status" type="radio" value="" />
          <span>全部</span>
        </label>
        <label>
          <input v-model="status" type="radio" value="PENDING" />
          <span>待复习</span>
        </label>
        <label>
          <input v-model="status" type="radio" value="COMPLETED" />
          <span>已完成</span>
        </label>
      </div>
    </fieldset>

    <label class="date-filter">
      <span>计划开始</span>
      <input v-model="plannedDateFrom" type="date" />
    </label>

    <label class="date-filter">
      <span>计划结束</span>
      <input v-model="plannedDateTo" type="date" />
    </label>

    <div class="filter-actions">
      <button class="filter-submit" type="submit" :disabled="busy || Boolean(dateRangeError)">
        <ListFilter :size="16" aria-hidden="true" />
        {{ busy ? "筛选中…" : "筛选" }}
      </button>
      <button class="filter-clear" type="button" :disabled="busy" title="清除筛选" @click="clear">
        <RotateCcw :size="16" aria-hidden="true" />
        <span class="sr-only">清除筛选</span>
      </button>
    </div>

    <p v-if="dateRangeError" class="filter-error" role="alert">{{ dateRangeError }}</p>
  </form>
</template>

<style scoped lang="scss">
.review-filters {
  display: flex;
  align-items: end;
  flex-wrap: wrap;
  gap: var(--space-sm);
  padding-block: var(--space-sm) var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

.status-filter {
  display: grid;
  gap: var(--space-xs);
  min-width: 14rem;
  margin: 0;
  padding: 0;
  border: 0;
}

.status-filter legend,
.date-filter span {
  color: var(--color-muted);
  font-size: var(--text-xs);
  font-weight: 700;
}

.status-options {
  display: flex;
  min-height: 2.75rem;
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  overflow: hidden;
}

.status-options label {
  position: relative;
  display: grid;
  flex: 1;
  place-items: center;
  min-width: 4.5rem;
  cursor: pointer;
}

.status-options label + label {
  border-left: var(--rule-thin) solid var(--color-rule);
}

.status-options input {
  position: absolute;
  inline-size: 1px;
  block-size: 1px;
  opacity: 0;
}

.status-options span {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  padding-inline: var(--space-sm);
  color: var(--color-muted);
  font-size: var(--text-sm);
  font-weight: 700;
}

.status-options input:checked + span {
  color: var(--color-accent-strong);
  background: var(--color-accent-soft);
}

.status-options input:focus-visible + span {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: calc(var(--rule-thin) * -1);
}

.date-filter {
  display: grid;
  gap: var(--space-xs);
}

.date-filter input {
  min-height: 2.75rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-surface);
}

.filter-actions {
  display: flex;
  gap: var(--space-xs);
}

.filter-submit,
.filter-clear {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.75rem;
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent-strong);
  background: var(--color-surface);
  font-weight: 700;
}

.filter-submit {
  padding-inline: var(--space-md);
}

.filter-clear {
  width: 2.75rem;
  padding: 0;
  border-color: var(--color-rule-2);
  color: var(--color-muted);
}

.filter-error {
  flex-basis: 100%;
  margin: 0;
  color: var(--color-error);
  font-size: var(--text-xs);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 40rem) {
  .review-filters,
  .status-filter,
  .date-filter,
  .date-filter input {
    width: 100%;
  }

  .filter-actions {
    width: 100%;
  }

  .filter-submit {
    flex: 1;
  }
}
</style>
