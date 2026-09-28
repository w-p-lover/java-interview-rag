<script setup lang="ts">
import { Mic, MicOff, Volume2, VolumeX } from "@lucide/vue";
import { computed, watch } from "vue";

import { useInterviewVoice } from "@/composables/useInterviewVoice";

const props = defineProps<{
  question: string;
  disabled: boolean;
}>();

const emit = defineEmits<{
  transcript: [text: string];
}>();

const {
  recognitionSupported,
  synthesisSupported,
  listening,
  speaking,
  interimTranscript,
  error,
  startListening,
  stopListening,
  speakQuestion,
  stopSpeaking,
  reset,
} = useInterviewVoice({
  questionText: () => props.question,
  onFinalTranscript: (text) => emit("transcript", text),
});

const status = computed(() => {
  if (error.value) {
    return error.value;
  }
  if (listening.value) {
    return interimTranscript.value
      ? `正在听写：${interimTranscript.value}`
      : "正在听写…";
  }
  if (speaking.value) {
    return "正在播放题目…";
  }
  if (!recognitionSupported.value) {
    return "当前浏览器不支持语音听写，文字回答仍可使用。";
  }
  return "";
});

watch(
  () => props.disabled,
  (disabled) => {
    if (disabled) {
      reset();
    }
  },
);
</script>

<template>
  <div class="voice-controls">
    <div class="voice-actions" role="group" aria-label="语音回答控制">
      <button
        type="button"
        class="voice-button"
        :class="{ active: speaking }"
        :disabled="disabled || !synthesisSupported"
        :aria-label="speaking ? '停止播放题目' : '播放当前题目'"
        :aria-pressed="speaking"
        :title="speaking ? '停止播放题目' : '播放当前题目'"
        @click="speaking ? stopSpeaking() : speakQuestion()"
      >
        <VolumeX v-if="speaking" :size="18" aria-hidden="true" />
        <Volume2 v-else :size="18" aria-hidden="true" />
      </button>
      <button
        type="button"
        class="voice-button"
        :class="{ active: listening }"
        :disabled="disabled || !recognitionSupported"
        :aria-label="listening ? '停止语音听写' : '开始语音听写'"
        :aria-pressed="listening"
        :title="listening ? '停止语音听写' : '开始语音听写'"
        @click="listening ? stopListening() : startListening()"
      >
        <MicOff v-if="listening" :size="18" aria-hidden="true" />
        <Mic v-else :size="18" aria-hidden="true" />
      </button>
    </div>
    <p v-if="status" :class="['voice-status', { error }]" aria-live="polite">
      {{ status }}
    </p>
  </div>
</template>

<style scoped lang="scss">
.voice-controls {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: var(--space-sm);
  min-height: 2.75rem;
}

.voice-actions {
  display: flex;
  gap: var(--space-xs);
}

.voice-button {
  display: inline-grid;
  width: 2.75rem;
  height: 2.75rem;
  padding: 0;
  place-items: center;
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink-2);
  background: var(--color-paper-2);
}

.voice-button:hover:not(:disabled) {
  border-color: var(--color-accent);
  color: var(--color-accent-ink);
  background: var(--color-accent-soft);
}

.voice-button.active {
  border-color: var(--color-accent);
  color: var(--color-accent-ink);
  background: var(--color-accent-soft);
}

.voice-status {
  min-width: 0;
  margin: 0;
  color: var(--color-muted);
  font-size: var(--text-xs);
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.voice-status.error {
  color: var(--color-error);
}

@media (max-width: 32rem) {
  .voice-controls {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
