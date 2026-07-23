<script setup lang="ts">
import { computed, nextTick, shallowRef, useTemplateRef, watch } from "vue";

export interface WorkspaceCommand {
  id: string;
  label: string;
  description: string;
}

const props = defineProps<{
  open: boolean;
  commands: WorkspaceCommand[];
}>();

const emit = defineEmits<{
  close: [];
  select: [command: WorkspaceCommand];
}>();

const dialog = useTemplateRef<HTMLDialogElement>("dialog");
const searchInput = useTemplateRef<HTMLInputElement>("searchInput");
const query = shallowRef("");
const activeIndex = shallowRef(0);

const filteredCommands = computed(() => {
  const keyword = query.value.trim().toLocaleLowerCase();
  if (!keyword) {
    return props.commands;
  }
  return props.commands.filter((command) =>
    `${command.label} ${command.description}`.toLocaleLowerCase().includes(keyword),
  );
});

watch(
  () => props.open,
  async (open) => {
    if (open) {
      query.value = "";
      activeIndex.value = 0;
      if (!dialog.value?.open) {
        dialog.value?.showModal();
      }
      await nextTick();
      searchInput.value?.focus();
      return;
    }
    if (dialog.value?.open) {
      dialog.value.close();
    }
  },
);

watch(query, () => {
  activeIndex.value = 0;
});

function moveSelection(offset: number): void {
  const commandCount = filteredCommands.value.length;
  if (!commandCount) {
    return;
  }
  activeIndex.value = (activeIndex.value + offset + commandCount) % commandCount;
}

function selectCommand(command: WorkspaceCommand | undefined): void {
  if (!command) {
    return;
  }
  emit("select", command);
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === "ArrowDown") {
    event.preventDefault();
    moveSelection(1);
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    moveSelection(-1);
  } else if (event.key === "Enter") {
    event.preventDefault();
    selectCommand(filteredCommands.value[activeIndex.value]);
  }
}
</script>

<template>
  <dialog
    ref="dialog"
    class="command-dialog"
    aria-labelledby="command-title"
    @cancel.prevent="emit('close')"
    @click.self="emit('close')"
  >
    <section class="command-panel" @keydown="handleKeydown">
      <h2 id="command-title" class="sr-only">工作台命令</h2>

      <div class="command-field">
        <svg class="search-icon" viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="11" cy="11" r="6"></circle>
          <path d="m16 16 4 4"></path>
        </svg>
        <input
          ref="searchInput"
          v-model="query"
          type="search"
          autocomplete="off"
          placeholder="查找工作台区段"
          aria-label="查找工作台区段"
        />
        <kbd>Esc</kbd>
      </div>

      <div class="command-results" aria-live="polite">
        <p class="command-group">可用命令</p>
        <button
          v-for="(command, index) in filteredCommands"
          :key="command.id"
          class="command-item"
          :class="{ 'is-active': index === activeIndex }"
          type="button"
          @mouseenter="activeIndex = index"
          @click="selectCommand(command)"
        >
          <span>
            <strong>{{ command.label }}</strong>
            <small>{{ command.description }}</small>
          </span>
          <span class="command-arrow" aria-hidden="true">↵</span>
        </button>

        <p v-if="filteredCommands.length === 0" class="command-empty">
          没有匹配区段。试试“导入”“结果”或“边界”。
        </p>
      </div>

      <footer class="command-footer">
        <span><kbd>↑</kbd><kbd>↓</kbd> 选择</span>
        <span><kbd>Enter</kbd> 打开</span>
        <span><kbd>Esc</kbd> 关闭</span>
      </footer>
    </section>
  </dialog>
</template>

<style scoped lang="scss">
.command-dialog {
  position: fixed;
  inset: 0;
  width: min(36rem, calc(100% - (var(--space-md) * 2)));
  max-width: none;
  height: fit-content;
  max-height: min(80dvh, 40rem);
  margin: auto;
  padding: 0;
  overflow: visible;
  border: 0;
  color: var(--color-ink);
  background: transparent;
}

.command-dialog::backdrop {
  background: var(--color-overlay);
}

.command-dialog[open] {
  animation: palette-enter var(--dur-short) var(--ease-out);
}

.command-panel {
  overflow: hidden;
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-surface);
  background: var(--color-paper);
  box-shadow: var(--shadow-whisper);
}

.command-field {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-sm);
  min-height: 3.5rem;
  padding-inline: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

.search-icon {
  width: var(--space-lg);
  height: var(--space-lg);
  fill: none;
  stroke: var(--color-muted);
  stroke-linecap: round;
  stroke-width: var(--rule-focus);
}

.command-field input {
  min-width: 0;
  border: 0;
  outline: 0;
  color: var(--color-ink);
  background: transparent;
  font-size: var(--text-base);
}

.command-field input::placeholder {
  color: var(--color-muted);
}

kbd {
  padding: var(--space-3xs) var(--space-2xs);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: var(--color-paper-2);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  line-height: 1.4;
}

.command-results {
  display: grid;
  gap: var(--space-2xs);
  max-height: min(26rem, 56dvh);
  padding: var(--space-sm);
  overflow-y: auto;
}

.command-group {
  margin: 0;
  padding: var(--space-xs) var(--space-sm) var(--space-2xs);
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.command-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  min-height: 3.75rem;
  padding: var(--space-sm);
  border: var(--rule-thin) solid transparent;
  border-radius: var(--radius-control);
  text-align: start;
  background: transparent;
}

.command-item.is-active {
  border-color: var(--color-rule);
  background: var(--color-paper-2);
}

.command-item:active {
  transform: translateY(var(--rule-thin));
}

.command-item > span:first-child {
  display: grid;
  min-width: 0;
  gap: var(--space-3xs);
}

.command-item strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
  font-weight: 600;
}

.command-item small {
  overflow: hidden;
  color: var(--color-muted);
  font-size: var(--text-xs);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.command-arrow {
  color: var(--color-accent);
  font-family: var(--font-mono);
}

.command-empty {
  margin: 0;
  padding: var(--space-xl) var(--space-md);
  color: var(--color-muted);
  font-size: var(--text-sm);
  text-align: center;
}

.command-footer {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  border-top: var(--rule-thin) solid var(--color-rule);
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.command-footer span {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2xs);
  white-space: nowrap;
}

.sr-only {
  position: absolute;
  width: var(--rule-thin);
  height: var(--rule-thin);
  margin: calc(var(--rule-thin) * -1);
  padding: 0;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}

@keyframes palette-enter {
  from {
    opacity: 0;
    transform: translateY(calc(var(--space-xs) * -1));
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (hover: hover) and (pointer: fine) {
  .command-item:hover {
    background: var(--color-paper-2);
  }
}

@media (max-width: 39.99rem) {
  .command-dialog {
    width: calc(100% - (var(--space-sm) * 2));
    max-height: calc(100dvh - (var(--space-sm) * 2));
  }

  .command-panel {
    max-height: calc(100dvh - (var(--space-sm) * 2));
  }

  .command-footer {
    display: none;
  }

  .command-item {
    min-height: 4rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .command-dialog[open] {
    animation: palette-enter-reduced var(--dur-micro) var(--ease-out);
  }

  @keyframes palette-enter-reduced {
    from {
      opacity: 0;
    }

    to {
      opacity: 1;
    }
  }
}
</style>
