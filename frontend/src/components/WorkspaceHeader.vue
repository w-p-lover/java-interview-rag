<script setup lang="ts">
import { computed, onMounted, onUnmounted, shallowRef, useTemplateRef } from "vue";
import { BookOpen, Command, Database, Search } from "@lucide/vue";

import CommandPalette, { type WorkspaceCommand } from "@/components/CommandPalette.vue";

const paletteOpen = shallowRef(false);
const searchTrigger = useTemplateRef<HTMLButtonElement>("searchTrigger");

const props = withDefaults(defineProps<{
  mode?: "knowledge" | "interview";
}>(), {
  mode: "knowledge",
});

const commands = computed<WorkspaceCommand[]>(() => props.mode === "interview"
  ? [
      { id: "interview-setup", label: "准备面试", description: "上传简历并设置题目数量" },
      { id: "interview-progress", label: "面试进度", description: "查看当前题号和整场进度" },
      { id: "current-question", label: "当前题目", description: "回到正在回答的问题" },
      { id: "latest-feedback", label: "上一题反馈", description: "查看评分和改进建议" },
      { id: "interview-report", label: "本场结果", description: "查看完成后的总分和复习题目" },
    ]
  : [
      { id: "import-workbench", label: "资料导入", description: "选择 Markdown 文件并设置来源" },
      { id: "pipeline", label: "处理阶段", description: "查看资料进入知识库的步骤" },
      { id: "import-result", label: "导入结果", description: "检查内容版本和切片" },
      { id: "capability-boundary", label: "能力边界", description: "查看当前已经接通的能力" },
    ]);

function handleGlobalShortcut(event: KeyboardEvent): void {
  if ((event.metaKey || event.ctrlKey) && event.key.toLocaleLowerCase() === "k") {
    event.preventDefault();
    paletteOpen.value ? handlePaletteClose() : (paletteOpen.value = true);
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
      <RouterLink class="brand" to="/" aria-label="Java 面试准备台首页">
        <span class="brand-mark" aria-hidden="true">J</span>
        <span class="brand-copy">
          <strong>面试准备台</strong>
          <small>Java 面试训练</small>
        </span>
      </RouterLink>

      <nav class="header-links" aria-label="工作台导航">
        <RouterLink to="/" class="nav-link">
          <Database :size="16" aria-hidden="true" />
          <span>资料库</span>
        </RouterLink>
        <RouterLink to="/interview" class="nav-link">
          <BookOpen :size="16" aria-hidden="true" />
          <span>面试训练</span>
        </RouterLink>
      </nav>

      <button
        ref="searchTrigger"
        class="search-trigger"
        type="button"
        aria-haspopup="dialog"
        :aria-expanded="paletteOpen"
        @click="paletteOpen = true"
      >
        <Search :size="16" aria-hidden="true" />
        <span>查找</span>
        <kbd><Command :size="12" aria-hidden="true" /> K</kbd>
      </button>

      <span class="session-state" aria-label="当前服务状态">
        <span class="session-dot" aria-hidden="true"></span>
        就绪
      </span>
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
  gap: var(--space-md);
  width: min(calc(100% - (var(--space-lg) * 2)), 82rem);
  min-height: 3.5rem;
  margin-inline: auto;
}

.brand,
.nav-link,
.search-trigger,
.session-state {
  min-height: 2.5rem;
  white-space: nowrap;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--color-ink);
  text-decoration: none;
}

.brand-mark {
  display: inline-grid;
  width: 1.875rem;
  height: 1.875rem;
  place-items: center;
  border: var(--rule-thin) solid var(--color-ink);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 700;
}

.brand-copy {
  display: grid;
  line-height: 1.08;
}

.brand-copy strong {
  font-size: var(--text-sm);
  font-weight: 700;
  letter-spacing: 0;
}

.brand-copy small {
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.header-links {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid transparent;
  border-radius: var(--radius-control);
  color: var(--color-muted);
  font-size: var(--text-sm);
  text-decoration: none;
  transition: background-color var(--dur-short) var(--ease-out), border-color var(--dur-short) var(--ease-out), color var(--dur-short) var(--ease-out);
}

.nav-link.router-link-active,
.nav-link:focus-visible {
  border-color: var(--color-rule-2);
  color: var(--color-ink);
  background: var(--color-paper-2);
}

.search-trigger {
  display: inline-grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-xs);
  width: 10rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: var(--color-surface);
  font-size: var(--text-sm);
  text-align: start;
  transition: border-color var(--dur-short) var(--ease-out), background-color var(--dur-short) var(--ease-out);
}

.search-trigger kbd {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3xs);
  padding: var(--space-3xs) var(--space-2xs);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: var(--color-paper);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.session-state {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.session-dot {
  width: var(--space-xs);
  height: var(--space-xs);
  border-radius: var(--radius-round);
  background: var(--color-success);
}

@media (hover: hover) and (pointer: fine) {
  .nav-link:hover,
  .search-trigger:hover {
    border-color: var(--color-rule-2);
    color: var(--color-ink);
    background: var(--color-paper-2);
  }
}

@media (max-width: 52rem) {
  .header-inner {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .session-state {
    display: none;
  }
}

@media (max-width: 42rem) {
  .header-inner {
    grid-template-columns: minmax(0, 1fr) auto;
    width: calc(100% - (var(--space-md) * 2));
  }

  .brand-copy small,
  .header-links {
    display: none;
  }

  .search-trigger {
    grid-template-columns: auto;
    width: 2.5rem;
    padding: 0;
    place-items: center;
  }

  .search-trigger > span,
  .search-trigger kbd {
    display: none;
  }
}

@media (pointer: coarse) {
  .brand,
  .nav-link,
  .search-trigger {
    min-height: 2.75rem;
  }
}
</style>
