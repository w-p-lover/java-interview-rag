<script setup lang="ts">
type PipelineState = "idle" | "loading" | "success" | "duplicate" | "error";

const props = defineProps<{
  state: PipelineState;
}>();

const steps = [
  { key: "goal", label: "求职目标", note: "先确定方向" },
  { key: "materials", label: "资料准备", note: "简历和岗位资料" },
  { key: "interview", label: "模拟面试", note: "提问与追问" },
  { key: "review", label: "训练复盘", note: "反馈与复习" },
] as const;

function stepClass(index: number): string[] {
  if (props.state === "success" || props.state === "duplicate") {
    return index === 1 ? ["is-complete"] : index === 2 ? ["is-current"] : [];
  }
  if (props.state === "error") {
    return index === 1 ? ["is-error"] : [];
  }
  return index === 1 ? ["is-current"] : [];
}
</script>

<template>
  <ol class="workflow-steps" aria-label="求职训练阶段">
    <li v-for="(step, index) in steps" :key="step.key" :class="stepClass(index)">
      <span class="step-node">{{ index + 1 }}</span>
      <span class="step-copy">
        <strong>{{ step.label }}</strong>
        <small>{{ step.note }}</small>
      </span>
    </li>
  </ol>
</template>

<style scoped lang="scss">
.workflow-steps {
  display: flex;
  gap: 0;
  margin: 0 0 1.5rem;
  padding: 0;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  list-style: none;
}

.workflow-steps li {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.55rem;
  flex: 1 1 0;
  min-width: 0;
  padding: 0.75rem 0.75rem 0.75rem 0;
}

.workflow-steps li + li {
  margin-left: 0.75rem;
  padding-left: 0.75rem;
  border-left: 1px solid var(--border);
}

.step-node {
  display: inline-grid;
  width: 1.5rem;
  height: 1.5rem;
  place-items: center;
  border: 1px solid var(--border-strong);
  border-radius: 50%;
  color: var(--ink-muted);
  font-family: var(--font-mono);
  font-size: 0.64rem;
}

.step-copy {
  display: grid;
  min-width: 0;
}

.step-copy strong {
  overflow: hidden;
  color: var(--ink-muted);
  font-size: 0.75rem;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-copy small {
  overflow: hidden;
  color: var(--ink-faint);
  font-size: 0.66rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workflow-steps li.is-current::after {
  position: absolute;
  right: 0.75rem;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: var(--accent);
  content: "";
}

.is-current .step-node {
  border-color: var(--accent);
  color: var(--accent);
}

.is-current .step-copy strong {
  color: var(--ink);
}

.workflow-steps li.is-complete {
  color: var(--success);
}

.is-complete .step-node {
  border-color: var(--success);
  color: var(--success);
}

.is-complete .step-copy strong {
  color: var(--success);
}

.workflow-steps li.is-error {
  color: var(--danger);
}

.is-error .step-node,
.is-error .step-copy strong {
  color: var(--danger);
}

@media (max-width: 720px) {
  .workflow-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    display: grid;
    gap: 0.5rem;
    border: 0;
  }

  .workflow-steps li,
  .workflow-steps li + li {
    margin: 0;
    padding: 0.65rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
  }

  .workflow-steps li.is-current::after {
    right: 0;
    bottom: 0;
    left: 0;
  }
}

@media (max-width: 420px) {
  .workflow-steps {
    grid-template-columns: 1fr;
  }
}
</style>
