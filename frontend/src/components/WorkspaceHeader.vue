<script setup lang="ts">
import { computed, onMounted, onUnmounted, shallowRef, useTemplateRef } from "vue";
import { Activity, BookOpen, BookOpenCheck, Command, Database, ListChecks, MessageCircleQuestion, Search } from "@lucide/vue";

import CommandPalette, { type WorkspaceCommand } from "@/components/CommandPalette.vue";

const paletteOpen = shallowRef(false);
const searchTrigger = useTemplateRef<HTMLButtonElement>("searchTrigger");

const props = withDefaults(defineProps<{
  mode?: "knowledge" | "answer" | "interview" | "question-bank" | "review" | "observability";
}>(), {
  mode: "knowledge",
});

const commands = computed<WorkspaceCommand[]>(() => {
  if (props.mode === "question-bank") {
    return [
      { id: "bank-configuration", label: "出题配置", description: "选择资料、Skill 和题量" },
      { id: "bank-tasks", label: "出题任务", description: "查看生成进度与恢复失败任务" },
      { id: "bank-questions", label: "题目列表", description: "筛选、审核并选择面试题目" },
    ];
  }
  if (props.mode === "interview") {
    return [
      { id: "interview-setup", label: "准备面试", description: "上传简历并设置题目数量" },
      { id: "interview-progress", label: "面试进度", description: "查看当前题号和整场进度" },
      { id: "current-question", label: "当前题目", description: "回到正在回答的问题" },
      { id: "latest-feedback", label: "上一题反馈", description: "查看评分和改进建议" },
      { id: "interview-report", label: "本场结果", description: "查看完成后的总分和复习题目" },
    ];
  }
  if (props.mode === "answer") {
    return [
      { id: "knowledge-question", label: "提出问题", description: "编辑当前要查询的问题" },
      { id: "knowledge-answer", label: "查看回答", description: "回到本次资料库回答" },
      { id: "knowledge-sources", label: "依据资料", description: "核对命中的原始资料" },
    ];
  }
  if (props.mode === "observability") {
    return [
      { id: "observability-overview", label: "运行概览", description: "查看业务请求和失败率" },
      { id: "observability-operations", label: "路由表现", description: "查看各业务路由耗时" },
    ];
  }
  if (props.mode === "review") {
    return [
      { id: "review-overview", label: "复习概览", description: "查看今日、逾期和已有练习数量" },
      { id: "review-filters", label: "筛选队列", description: "切换面试、知识和计划日期" },
      { id: "review-list", label: "复习任务", description: "打开具体复习训练" },
    ];
  }
  return [
    { id: "import-workbench", label: "资料导入", description: "选择文件并设置来源" },
    { id: "import-result", label: "导入结果", description: "查看版本和切片" },
  ];
});

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
        <span class="brand-mark" aria-hidden="true">J/</span>
        <span class="brand-copy">
          <strong>Java 面试台</strong>
        </span>
      </RouterLink>

      <nav class="header-links" aria-label="工作台导航">
        <RouterLink to="/" class="nav-link" aria-label="打开资料库">
          <Database :size="16" aria-hidden="true" />
          <span>资料库</span>
        </RouterLink>
        <RouterLink to="/answer" class="nav-link" aria-label="打开知识问答">
          <MessageCircleQuestion :size="16" aria-hidden="true" />
          <span>知识问答</span>
        </RouterLink>
        <RouterLink to="/interview" class="nav-link" aria-label="打开面试训练">
          <BookOpen :size="16" aria-hidden="true" />
          <span>面试训练</span>
        </RouterLink>
        <RouterLink to="/question-bank" class="nav-link" aria-label="打开知识题库" title="知识题库">
          <BookOpenCheck :size="16" aria-hidden="true" />
          <span>知识题库</span>
        </RouterLink>
        <RouterLink to="/review" class="nav-link" aria-label="打开统一复习中心">
          <ListChecks :size="16" aria-hidden="true" />
          <span>复习中心</span>
        </RouterLink>
        <RouterLink to="/observability" class="nav-link" aria-label="打开运行观测">
          <Activity :size="16" aria-hidden="true" />
          <span>运行观测</span>
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
  position: fixed;
  inset: 0 auto 0 0;
  z-index: var(--z-sticky);
  width: var(--workspace-rail);
  border-right: var(--rule-thin) solid var(--color-ink);
  background: var(--color-graphite);
}

.header-inner {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: var(--space-sm);
  width: 100%;
  height: 100%;
  padding: var(--space-lg) var(--space-md);
}

.brand,
.nav-link,
.search-trigger {
  min-height: 2.5rem;
  white-space: nowrap;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--color-graphite-text);
  text-decoration: none;
}

.brand-mark {
  display: inline-grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-signal-rule);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 800;
}

.brand-copy {
  display: grid;
  line-height: 1.05;
}

.brand strong {
  font-size: var(--text-sm);
  font-weight: 750;
  letter-spacing: 0;
}

.header-links {
  display: grid;
  gap: var(--space-xs);
  margin-top: var(--space-2xl);
}

.nav-link {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-xs);
  min-height: 3rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid transparent;
  border-radius: var(--radius-control);
  color: var(--color-graphite-muted);
  font-size: var(--text-sm);
  text-decoration: none;
  transition: background-color var(--dur-short) var(--ease-out), border-color var(--dur-short) var(--ease-out), color var(--dur-short) var(--ease-out);
}

.nav-link.router-link-active,
.nav-link:focus-visible {
  border-color: var(--color-info-rule);
  color: var(--color-graphite-text);
  background: var(--color-graphite-2);
}

.search-trigger {
  display: inline-grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-xs);
  width: 100%;
  margin-top: auto;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-graphite-muted);
  border-radius: var(--radius-control);
  color: var(--color-graphite-text);
  background: var(--color-graphite-2);
  font-size: var(--text-sm);
  text-align: start;
  transition: border-color var(--dur-short) var(--ease-out), background-color var(--dur-short) var(--ease-out);
}

.search-trigger kbd {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3xs);
  padding: var(--space-3xs) var(--space-2xs);
  border: var(--rule-thin) solid var(--color-graphite-muted);
  border-radius: var(--radius-control);
  color: var(--color-graphite-muted);
  background: var(--color-graphite);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

@media (hover: hover) and (pointer: fine) {
  .nav-link:hover,
  .search-trigger:hover {
    border-color: var(--color-info-rule);
    color: var(--color-graphite-text);
    background: var(--color-graphite-2);
  }

  .nav-link.router-link-active:hover {
    color: var(--color-graphite-text);
    background: var(--color-graphite-2);
  }
}

@media (max-width: 68rem) {
  .workspace-header {
    position: sticky;
    inset: auto;
    width: 100%;
    border-right: 0;
    border-bottom: var(--rule-thin) solid var(--color-rule);
    background: color-mix(in oklch, var(--color-surface) 94%, transparent);
    backdrop-filter: blur(14px);
  }

  .header-inner {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: var(--space-md);
    width: min(calc(100% - (var(--space-lg) * 2)), 82rem);
    height: auto;
    min-height: 3.5rem;
    margin-inline: auto;
    padding: 0;
  }

  .brand {
    color: var(--color-ink);
  }

  .brand small {
    color: var(--color-faint);
  }

  .header-links {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    margin-top: 0;
  }

  .nav-link {
    display: inline-flex;
    min-height: 2.5rem;
    color: var(--color-muted);
  }

  .nav-link::after {
    display: none;
  }

  .nav-link.router-link-active,
  .nav-link:focus-visible {
    border-color: var(--color-accent);
    color: var(--color-accent-strong);
    background: var(--color-accent-soft);
  }

  .search-trigger {
    grid-template-columns: auto;
    width: 2.5rem;
    margin-top: 0;
    padding: 0;
    place-items: center;
    border-color: var(--color-rule-2);
    color: var(--color-muted);
    background: var(--color-surface);
  }

  .search-trigger > span,
  .search-trigger kbd {
    display: none;
  }
}

@media (max-width: 42rem) {
  .header-inner {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: var(--space-xs);
    width: calc(100% - (var(--space-md) * 2));
  }

  .header-links {
    grid-column: 1 / -1;
    grid-row: 2;
    justify-content: space-between;
    gap: 0;
  }

  .nav-link {
    grid-template-columns: auto;
    width: 2.25rem;
    padding: 0;
    place-items: center;
  }

  .nav-link span {
    position: absolute;
    width: var(--rule-thin);
    height: var(--rule-thin);
    overflow: hidden;
    clip-path: inset(50%);
    white-space: nowrap;
  }

  .brand small {
    display: none;
  }

  .search-trigger {
    grid-column: 2;
    grid-row: 1;
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
