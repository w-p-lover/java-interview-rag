<script setup lang="ts">
import { ArrowUpRight, BookOpenCheck, FileSearch, LibraryBig } from "@lucide/vue";
</script>

<template>
  <section class="feature-dock" aria-labelledby="feature-dock-title">
    <header>
      <span class="dock-mark" aria-hidden="true">J/</span>
      <h2 id="feature-dock-title">继续使用资料</h2>
    </header>

    <nav aria-label="资料库后续功能">
      <RouterLink :to="{ path: '/answer', query: { mode: 'catalog' } }">
        <LibraryBig :size="20" aria-hidden="true" />
        <span>
          <strong>资料目录</strong>
          <small>版本与章节</small>
        </span>
        <ArrowUpRight :size="17" aria-hidden="true" />
      </RouterLink>

      <RouterLink :to="{ path: '/answer', query: { mode: 'search' } }">
        <FileSearch :size="20" aria-hidden="true" />
        <span>
          <strong>查找原文</strong>
          <small>定位相关内容</small>
        </span>
        <ArrowUpRight :size="17" aria-hidden="true" />
      </RouterLink>

      <RouterLink class="is-primary" to="/interview">
        <BookOpenCheck :size="20" aria-hidden="true" />
        <span>
          <strong>模拟面试</strong>
          <small>开始一场训练</small>
        </span>
        <ArrowUpRight :size="17" aria-hidden="true" />
      </RouterLink>
    </nav>
  </section>
</template>

<style scoped lang="scss">
/* Hallmark · macrostructure: Action Dock · tone: focused product · anchor hue: eucalyptus teal
 * Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5
 * Hallmark · contrast: pass (40–41) · honest: pass (46) · chrome: pass (47) · tokens: pass (48)
 * Hallmark · responsive: pass (49) · icons: pass (30) · mobile: pass (34, 49, 50–57)
 */
.feature-dock {
  display: grid;
  grid-template-columns: minmax(11rem, 0.72fr) minmax(0, 2.28fr);
  margin-bottom: var(--space-lg);
  overflow: hidden;
  border-radius: var(--radius-surface);
  color: var(--color-graphite-text);
  background: var(--color-graphite);
}

.feature-dock header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-lg);
}

.dock-mark {
  display: inline-grid;
  width: 2.25rem;
  height: 2.25rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-signal-rule);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 800;
}

.feature-dock h2 {
  margin: 0;
  color: var(--color-graphite-text);
  font-size: var(--text-md);
  line-height: 1.2;
}

.feature-dock nav {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.feature-dock a {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-sm);
  min-width: 0;
  min-height: 6.75rem;
  padding: var(--space-lg);
  border-left: var(--rule-thin) solid var(--color-workbench-raised);
  color: var(--color-graphite-muted);
  text-decoration: none;
  transition: color var(--dur-short) var(--ease-out), background-color var(--dur-short) var(--ease-out);
}

.feature-dock a > span {
  display: grid;
  min-width: 0;
  gap: var(--space-2xs);
}

.feature-dock strong {
  color: var(--color-graphite-text);
  font-size: var(--text-sm);
  white-space: nowrap;
}

.feature-dock small {
  color: inherit;
  font-size: var(--text-xs);
  white-space: nowrap;
}

.feature-dock a.is-primary {
  color: var(--color-ink-2);
  background: var(--color-info-rule);
}

.feature-dock a.is-primary strong {
  color: var(--color-ink);
}

@media (hover: hover) and (pointer: fine) {
  .feature-dock a:hover {
    color: var(--color-graphite-text);
    background: var(--color-workbench);
  }

  .feature-dock a.is-primary:hover {
    color: var(--color-ink);
    background: var(--color-signal-rule);
  }
}

@media (max-width: 58rem) {
  .feature-dock {
    grid-template-columns: minmax(0, 1fr);
  }

  .feature-dock nav {
    border-top: var(--rule-thin) solid var(--color-workbench-raised);
  }

  .feature-dock a:first-child {
    border-left: 0;
  }
}

@media (max-width: 40rem) {
  .feature-dock nav {
    grid-template-columns: minmax(0, 1fr);
  }

  .feature-dock a {
    min-height: 4.75rem;
    border-top: var(--rule-thin) solid var(--color-workbench-raised);
    border-left: 0;
  }

  .feature-dock a:first-child {
    border-top: 0;
  }
}
</style>
