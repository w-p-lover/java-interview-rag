<script setup lang="ts">
type PipelineState = "idle" | "loading" | "success" | "duplicate" | "error";

const props = defineProps<{
  state: PipelineState;
}>();

const steps = [
  { key: "source", label: "SOURCE", note: "逻辑来源" },
  { key: "hash", label: "HASH", note: "内容版本" },
  { key: "section", label: "SECTION", note: "标题路径" },
  { key: "chunk", label: "CHUNK", note: "有序切片" },
] as const;

function stepClass(index: number): string[] {
  if (props.state === "success" || props.state === "duplicate") {
    return ["is-complete"];
  }
  if (props.state === "loading") {
    return index === 0 ? ["is-complete"] : index === 1 ? ["is-active"] : [];
  }
  if (props.state === "error") {
    return index === 0 ? ["is-error"] : [];
  }
  return index === 0 ? ["is-active"] : [];
}
</script>

<template>
  <ol class="pipeline" aria-label="文档处理链">
    <li
      v-for="(step, index) in steps"
      :key="step.key"
      :class="stepClass(index)"
    >
      <span class="node">{{ String(index + 1).padStart(2, "0") }}</span>
      <span class="label">
        <strong>{{ step.label }}</strong>
        <small>{{ step.note }}</small>
      </span>
    </li>
  </ol>
</template>

<style scoped lang="scss">
.pipeline {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  width: min(100%, 42rem);
  margin: 0;
  padding: 0;
  list-style: none;
}

.pipeline li {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 0.6rem;
  min-width: 0;
}

.pipeline li:not(:last-child)::after {
  content: "";
  position: absolute;
  top: 50%;
  left: 2rem;
  width: calc(100% - 2.3rem);
  height: 1px;
  background: var(--lead);
  transform: translateY(-50%);
}

.node {
  position: relative;
  z-index: 1;
  display: grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  border: 1px solid var(--lead-dark);
  border-radius: 50%;
  color: var(--ink-soft);
  background: var(--paper);
  font-family: var(--font-mono);
  font-size: 0.62rem;
  transition: all 180ms ease;
}

.label {
  position: relative;
  z-index: 1;
  display: grid;
  min-width: 0;
  padding-right: 0.5rem;
  background: var(--paper);
}

.label strong {
  overflow: hidden;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-overflow: ellipsis;
}

.label small {
  color: var(--ink-soft);
  font-size: 0.62rem;
}

.is-active .node {
  border-color: var(--cinnabar);
  color: var(--cinnabar);
  box-shadow: 0 0 0 0.3rem var(--cinnabar-soft);
}

.is-complete .node {
  border-color: var(--jade);
  color: var(--surface);
  background: var(--jade);
}

.is-complete:not(:last-child)::after {
  background: var(--jade);
}

.is-error .node {
  border-color: var(--cinnabar);
  color: var(--surface);
  background: var(--cinnabar);
}

@media (max-width: 720px) {
  .pipeline {
    grid-template-columns: 1fr;
    gap: 0.55rem;
  }

  .pipeline li:not(:last-child)::after {
    top: 1.8rem;
    left: 0.98rem;
    width: 1px;
    height: 1.1rem;
  }

  .label {
    background: transparent;
  }
}
</style>
