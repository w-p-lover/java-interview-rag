<script setup lang="ts">
import { shallowRef } from "vue";
import type { KnowledgeReviewAttempt, KnowledgeReviewPathResult, KnowledgeReviewTask } from "@/api/knowledge";

const props = defineProps<{ tasks: KnowledgeReviewTask[]; attemptsByTaskId?: Record<string, KnowledgeReviewAttempt[]>; reviewPath?: KnowledgeReviewPathResult | null }>();
const emit = defineEmits<{
  complete: [task: KnowledgeReviewTask];
  schedule: [task: KnowledgeReviewTask, event: Event];
  submit: [task: KnowledgeReviewTask, answer: string];
}>();
const answers = shallowRef<Record<string, string>>({});
const expanded = shallowRef<Record<string, boolean>>({});

function toggle(task: KnowledgeReviewTask): void { expanded.value = { ...expanded.value, [task.id]: !expanded.value[task.id] }; }
function submit(task: KnowledgeReviewTask): void {
  const answer = answers.value[task.id]?.trim();
  if (answer) emit("submit", task, answer);
}
</script>

<template>
  <section v-if="props.tasks.length" class="knowledge-review-tasks" aria-labelledby="knowledge-review-title">
    <header><h2 id="knowledge-review-title">知识复习任务</h2><span>{{ props.tasks.filter((task) => task.status === 'PENDING').length }} 项待复习</span></header>
    <div v-if="props.reviewPath?.recommendations.length" class="review-path" aria-label="今日复习路径">
      <strong>今日复习路径</strong>
      <ol>
        <li v-for="recommendation in props.reviewPath.recommendations.slice(0, 5)" :key="recommendation.taskId">
          <span>{{ recommendation.claim }}</span>
          <small>{{ recommendation.due ? '今天复习' : `建议 ${recommendation.recommendedReviewDate} · ${recommendation.intervalDays} 天间隔` }} · {{ recommendation.reason }}</small>
        </li>
      </ol>
    </div>
    <ul>
      <li v-for="task in props.tasks" :key="task.id" :class="{ completed: task.status === 'COMPLETED' }">
        <div class="task-main"><strong>{{ task.claim }}</strong><small>{{ task.evidenceStatus }} · {{ task.question }}</small></div>
        <div class="task-actions">
          <label v-if="task.status === 'PENDING'">计划日期<input type="date" :value="task.plannedReviewDate ?? ''" @change="emit('schedule', task, $event)" /></label>
          <button type="button" @click="toggle(task)">{{ expanded[task.id] ? '收起训练' : '开始训练' }}</button>
          <button v-if="task.status === 'PENDING'" type="button" @click="emit('complete', task)">标记完成</button>
          <span v-else>已完成</span>
        </div>
        <div v-if="expanded[task.id]" class="attempt-box">
          <textarea v-model="answers[task.id]" rows="4" placeholder="用自己的话回答这个问题，再提交给模型评分。" />
          <button type="button" :disabled="!answers[task.id]?.trim()" @click="submit(task)">提交复习回答</button>
          <article v-for="attempt in props.attemptsByTaskId?.[task.id] ?? []" :key="attempt.id" class="attempt-result">
            <strong>得分 {{ attempt.evaluation.overallScore }}</strong>
            <p>{{ attempt.evaluation.improvementSuggestion }}</p>
          </article>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.knowledge-review-tasks { display: grid; gap: var(--space-md); margin-bottom: var(--space-lg); padding: var(--space-lg); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); background: var(--color-paper-2); }
.knowledge-review-tasks header { display: flex; align-items: baseline; justify-content: space-between; }
.knowledge-review-tasks h2 { margin: 0; font-size: var(--text-lg); }
.knowledge-review-tasks header span, .knowledge-review-tasks small { color: var(--color-muted); font-size: var(--text-xs); }
.knowledge-review-tasks ul { display: grid; gap: var(--space-sm); margin: 0; padding: 0; list-style: none; }
.review-path { display: grid; gap: var(--space-xs); padding: var(--space-sm); border-left: 3px solid var(--color-accent); background: var(--color-surface); }
.review-path ol { display: grid; gap: var(--space-xs); margin: 0; padding-left: 1.25rem; }
.review-path li { display: grid; gap: var(--space-2xs); }
.review-path small { color: var(--color-muted); font-size: var(--text-xs); }
.knowledge-review-tasks li { display: grid; gap: var(--space-sm); padding-top: var(--space-sm); border-top: var(--rule-thin) solid var(--color-rule); }
.task-main { display: grid; gap: var(--space-2xs); }
.task-actions { display: flex; align-items: center; flex-wrap: wrap; gap: var(--space-sm); }
.task-actions label { display: inline-flex; align-items: center; gap: var(--space-xs); color: var(--color-muted); font-size: var(--text-xs); }
.task-actions button, .attempt-box button { min-height: 2rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-accent); border-radius: var(--radius-control); color: var(--color-accent-strong); background: var(--color-surface); font-size: var(--text-xs); font-weight: 700; }
.attempt-box { display: grid; gap: var(--space-sm); }
.attempt-box textarea { width: 100%; resize: vertical; padding: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); }
.attempt-result { padding: var(--space-sm); border-left: 3px solid var(--color-accent); background: var(--color-surface); }
.attempt-result p { margin: var(--space-xs) 0 0; color: var(--color-muted); font-size: var(--text-sm); }
.completed { opacity: .7; }
</style>
