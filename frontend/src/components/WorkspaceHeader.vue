<script setup lang="ts">
import { onMounted, onUnmounted, shallowRef, useTemplateRef } from "vue";

import CommandPalette, { type WorkspaceCommand } from "@/components/CommandPalette.vue";

const paletteOpen = shallowRef(false);
const searchTrigger = useTemplateRef<HTMLButtonElement>("searchTrigger");

const commands: WorkspaceCommand[] = [
  { id: "import-workbench", label: "跳到资料导入", description: "选择 Markdown 文件并设置稳定来源" },
  { id: "pipeline", label: "查看处理阶段", description: "确认资料准备在求职训练流程中的位置" },
  { id: "import-result", label: "查看导入结果", description: "检查内容版本、标题路径与切片" },
  { id: "capability-boundary", label: "查看能力边界", description: "区分当前可用能力与后续模块" },
];

function handleGlobalShortcut(event: KeyboardEvent): void {
  if ((event.metaKey || event.ctrlKey) && event.key.toLocaleLowerCase() === "k") {
    event.preventDefault();
    if (paletteOpen.value) {
      handlePaletteClose();
    } else {
      paletteOpen.value = true;
    }
  }
}

function handleSelect(command: WorkspaceCommand): void {
  const destination = document.getElementById(command.id);
  paletteOpen.value = false;
  window.history.replaceState(null, "", `#${command.id}`);
  window.requestAnimationFrame(() => {
    destination?.scrollIntoView({ block: "start" });
    destination?.focus({ preventScroll: true });
  });
}

function handlePaletteClose(): void {
  paletteOpen.value = false;
  window.requestAnimationFrame(() => searchTrigger.value?.focus());
}

onMounted(() => {
  window.addEventListener("keydown", handleGlobalShortcut);
});

onUnmounted(() => {
  window.removeEventListener("keydown", handleGlobalShortcut);
});
</script>

<template>
  <header class="workspace-header">
    <div class="header-inner">
      <a class="brand" href="#main-content" aria-label="Java 面试准备台首页">
        <span class="brand-mark" aria-hidden="true">J</span>
        <span class="brand-copy">
          <strong>面试准备台</strong>
          <small>Java Interview RAG</small>
        </span>
      </a>

      <nav class="header-links" aria-label="工作台导航">
        <a href="#import-workbench">资料</a>
        <a href="#import-result">结果</a>
        <a href="#capability-boundary">边界</a>
      </nav>

      <button
        ref="searchTrigger"
        class="search-trigger"
        type="button"
        aria-haspopup="dialog"
        :aria-expanded="paletteOpen"
        @click="paletteOpen = true"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="11" cy="11" r="6"></circle>
          <path d="m16 16 4 4"></path>
        </svg>
        <span>查找区段</span>
        <kbd>Ctrl K</kbd>
      </button>

      <div class="session-state" aria-label="当前服务状态">
        <span class="session-dot" aria-hidden="true"></span>
        <span>工作台就绪</span>
      </div>
    </div>

    <CommandPalette
      :open="paletteOpen"
      :commands="commands"
      @close="handlePaletteClose"
      @select="handleSelect"
    />
  </header>
</template>

<style scoped lang="scss">
.workspace-header {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  border-bottom: var(--rule-thin) solid var(--color-rule);
  background: var(--color-surface);
}

.header-inner {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: var(--space-lg);
  width: min(calc(100% - (var(--space-lg) * 2)), 84rem);
  min-height: 4rem;
  margin-inline: auto;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  min-height: 2.75rem;
  color: var(--color-ink);
  text-decoration: none;
  white-space: nowrap;
}

.brand-mark {
  display: inline-grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent);
  font-family: var(--font-display);
  font-size: var(--text-sm);
  font-weight: 700;
}

.brand-copy {
  display: grid;
  gap: var(--space-3xs);
  line-height: 1.1;
}

.brand-copy strong {
  font-family: var(--font-display);
  font-size: var(--text-sm);
  font-weight: 600;
  letter-spacing: -0.015em;
}

.brand-copy small {
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.header-links {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.header-links a {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-height: 2.75rem;
  color: var(--color-muted);
  font-size: var(--text-sm);
  text-decoration: none;
  white-space: nowrap;
}

.header-links a::after {
  position: absolute;
  right: 0;
  bottom: var(--space-xs);
  left: 0;
  height: var(--rule-thin);
  background: var(--color-accent);
  content: "";
  opacity: 0;
  transform: scaleX(0);
  transform-origin: left;
  transition: opacity var(--dur-short) var(--ease-out), transform var(--dur-short) var(--ease-out);
}

.header-links a:focus-visible::after {
  opacity: 1;
  transform: scaleX(1);
}

.search-trigger {
  display: inline-grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-sm);
  min-width: 13rem;
  min-height: 2.75rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-surface);
  color: var(--color-muted);
  background: var(--color-paper);
  font-size: var(--text-sm);
  text-align: start;
  transition: border-color var(--dur-short) var(--ease-out), transform var(--dur-micro) var(--ease-out);
}

.search-trigger:active {
  transform: translateY(var(--rule-thin));
}

.search-trigger svg {
  width: var(--space-md);
  height: var(--space-md);
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-width: var(--rule-focus);
}

.search-trigger > span {
  white-space: nowrap;
}

.search-trigger kbd {
  display: inline-flex;
  gap: var(--space-3xs);
  padding: var(--space-3xs) var(--space-2xs);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: var(--color-paper);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  white-space: nowrap;
}

.session-state {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  white-space: nowrap;
}

.session-dot {
  width: var(--space-xs);
  height: var(--space-xs);
  border: var(--rule-thin) solid var(--color-success);
  border-radius: var(--radius-round);
  background: var(--color-success-soft);
}

@media (hover: hover) and (pointer: fine) {
  .header-links a:hover::after {
    opacity: 1;
    transform: scaleX(1);
  }

  .search-trigger:hover {
    border-color: var(--color-accent);
  }
}

@media (max-width: 64rem) {
  .header-inner {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .header-links {
    display: none;
  }
}

@media (max-width: 44rem) {
  .header-inner {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: var(--space-sm);
    width: calc(100% - (var(--space-md) * 2));
  }

  .brand-copy small,
  .session-state {
    display: none;
  }

  .search-trigger {
    grid-template-columns: auto;
    min-width: 2.75rem;
    width: 2.75rem;
    padding: 0;
    place-items: center;
  }

  .search-trigger > span,
  .search-trigger kbd {
    display: none;
  }
}

@media (pointer: coarse) {
  .header-links a,
  .search-trigger {
    min-height: 2.75rem;
  }
}
</style>
