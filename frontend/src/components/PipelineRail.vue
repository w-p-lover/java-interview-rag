<script setup lang="ts">
type PipelineState = "idle" | "loading" | "success" | "duplicate" | "error";

const props = defineProps<{
  state: PipelineState;
}>();

const steps = [
  { key: "select", label: "选择文件", note: "Markdown 与逻辑来源" },
  { key: "version", label: "版本判定", note: "来源 + 内容指纹" },
  { key: "parse", label: "标题解析", note: "保留标题层级与顺序" },
  { key: "chunk", label: "生成切片", note: "返回可追踪 Chunk" },
] as const;

function stepClass(index: number): string[] {
  if (props.state === "success") {
    return ["is-complete"];
  }
  if (props.state === "duplicate") {
    return index < 2 ? ["is-complete"] : ["is-skipped"];
  }
  if (props.state === "loading") {
    return index === 0 ? ["is-complete"] : ["is-processing"];
  }
  if (props.state === "error") {
    return index === 0 ? ["is-complete"] : ["is-skipped"];
  }
  return index === 0 ? ["is-current"] : [];
}

function isCurrent(index: number): boolean {
  return index === 0 && props.state === "idle";
}

function statusCopy(): string {
  if (props.state === "loading") return "后端处理中";
  if (props.state === "success") return "处理完成";
  if (props.state === "duplicate") return "重复版本已短路";
  if (props.state === "error") return "处理未完成";
  return "等待文件";
}
</script>

<template>
  <section class="pipeline" aria-labelledby="pipeline-title">
    <header>
      <h3 id="pipeline-title">本次导入链路</h3>
      <p>{{ statusCopy() }}</p>
    </header>

    <ol class="workflow-steps" aria-label="求职训练阶段">
      <li
        v-for="(step, index) in steps"
        :key="step.key"
        :class="stepClass(index)"
        :aria-current="isCurrent(index) ? 'step' : undefined"
      >
        <span class="step-node">{{ String(index + 1).padStart(2, "0") }}</span>
        <span class="step-copy">
          <strong>{{ step.label }}</strong>
          <small>{{ step.note }}</small>
        </span>
      </li>
    </ol>
  </section>
</template>

<style scoped lang="scss">
.pipeline {
  display: grid;
  grid-template-columns: 12rem minmax(0, 1fr);
  align-items: end;
  gap: var(--space-xl);
  padding: var(--space-sm) var(--space-xl) var(--space-xl);
  border-radius: 0 0 var(--radius-surface) var(--radius-surface);
  background: var(--color-workbench);
}

.pipeline > header {
  display: grid;
  gap: var(--space-xs);
}

.pipeline h3 {
  margin: 0;
  color: var(--color-graphite-text);
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-style: normal;
  font-weight: 700;
  letter-spacing: 0;
}

.pipeline header p {
  margin: 0;
  color: var(--color-graphite-muted);
  font-size: var(--text-xs);
  line-height: 1.5;
}

.workflow-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-xs);
  margin: 0;
  padding: var(--space-xs);
  border-radius: var(--radius-surface);
  background: var(--color-workbench-raised);
  list-style: none;
}

.workflow-steps li {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-xs);
  padding: var(--space-sm);
  border-radius: var(--radius-control);
}

.workflow-steps li.is-current {
  background: var(--color-surface);
}

.workflow-steps li + li {
  border-left: 0;
}

.step-node {
  color: var(--color-graphite-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-variant-numeric: tabular-nums;
}

.step-copy {
  display: grid;
  min-width: 0;
  gap: var(--space-2xs);
}

.step-copy strong {
  color: var(--color-graphite-muted);
  font-size: var(--text-sm);
  font-weight: 600;
}

.step-copy small {
  color: var(--color-graphite-muted);
  font-size: var(--text-xs);
  line-height: 1.45;
}

.workflow-steps li.is-current::before,
.workflow-steps li.is-complete::before,
.workflow-steps li.is-error::before {
  position: absolute;
  inset: auto var(--space-sm) calc(var(--rule-thin) * -1);
  height: var(--rule-focus);
  background: var(--color-accent);
  content: "";
}

.is-current .step-node,
.is-current .step-copy strong {
  color: var(--color-accent);
}

.workflow-steps li.is-complete::before {
  background: var(--color-success);
}

.is-complete .step-node,
.is-complete .step-copy strong {
  color: var(--color-success);
}

.workflow-steps li.is-error::before {
  background: var(--color-error);
}

.is-error .step-node,
.is-error .step-copy strong {
  color: var(--color-error);
}

.is-skipped,
.is-processing {
  opacity: 0.72;
}

@media (max-width: 52rem) {
  .pipeline {
    grid-template-columns: minmax(0, 1fr);
    align-items: start;
    gap: var(--space-md);
  }
}

@media (max-width: 39.99rem) {
  .workflow-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workflow-steps li + li {
    border-left: 0;
  }

  .workflow-steps li:nth-child(even) {
    border-left: 0;
  }
}
</style>
