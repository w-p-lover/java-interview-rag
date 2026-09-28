# Stage 10.1 Browser Voice Interview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Execute natively in the current session; the user has already requested continued development. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an immediately usable browser voice-input and question-playback path to the existing text interview without creating a second session, scoring, report, or review model.

**Architecture:** A Vue composable owns the browser `SpeechRecognition` and `speechSynthesis` lifecycles. A focused control component renders microphone/playback controls and emits only final transcript segments. `InterviewQuestionPanel` remains the single source of truth for the editable answer and submits through the existing SSE answer path.

**Tech Stack:** Vue 3.5, TypeScript, Composition API, browser Web Speech API, Lucide Vue, existing Vite build.

**Spec:** `docs/09-后续开发路线.md` section 10.1 and `Snailclimb/interview-guide@969e2af8550f680688f5b50f0d895186a661a319` voice architecture/handler tests.

## Global Constraints

- Keep Java 17, Spring Boot 3, Spring AI, Vue, MySQL, Qdrant and Lucene as the main stack.
- Reuse the current interview session, answer evaluation, report and review path.
- Do not persist raw audio or transcript before the user submits the editable answer.
- Do not add a paid ASR/TTS call, new database table, WebSocket backend or second interview domain in this slice.
- Text input must remain available at all times and be the fallback for every voice failure.
- Only write inside `D:\IdeaProjects\java-interview-rag`.

## File Map

- Create `frontend/src/composables/useInterviewVoice.ts`: browser recognition/synthesis feature detection, lifecycle, errors and mutual exclusion.
- Create `frontend/src/components/interview/InterviewVoiceControls.vue`: accessible controls and live status; emits final transcript segments.
- Modify `frontend/src/components/interview/InterviewQuestionPanel.vue`: append final transcript to the editable answer and preserve the existing submit contract.
- Modify `docs/04-当前进度.md`: record the exact first-slice capability and remaining server-streaming boundary.
- Modify `docs/06-参考项目调研.md`: record upstream commit, adopted semantics and deliberate differences.
- Modify `docs/09-后续开发路线.md`: mark 10.1 first slice rather than the entire phase complete.

## Review Focus

- Browser has no SpeechRecognition constructor: voice controls show unavailable state and text submission remains enabled.
- Microphone permission is denied: recognition stops, a low-sensitivity error is shown, and typed text is preserved.
- Recognition returns interim and final segments: interim text is preview-only; each final segment is appended exactly once.
- Question playback and microphone overlap: starting either operation stops the other to avoid capturing synthesized speech.
- Question changes or component unmounts: recognition and synthesis are aborted so late results cannot enter the next answer.

---

### Task 1: Browser Voice Lifecycle

**Files:**
- Create: `frontend/src/composables/useInterviewVoice.ts`

**Interfaces:**
- Consumes: `questionText: () => string`, `onFinalTranscript: (text: string) => void`.
- Produces: readonly `recognitionSupported`, `synthesisSupported`, `listening`, `speaking`, `interimTranscript`, `error`; actions `startListening()`, `stopListening()`, `speakQuestion()`, `stopSpeaking()`, `reset()`.

- [x] **Step 1: Define minimal browser API types**

Declare only the recognition constructor, event, result and error members used by this feature so strict TypeScript does not depend on non-standard global DOM declarations.

- [x] **Step 2: Implement recognition lifecycle**

Configure `lang = "zh-CN"`, `continuous = true`, `interimResults = true`; emit final segments once and expose interim text without adding it to the answer.

- [x] **Step 3: Implement synthesis and mutual exclusion**

Use `SpeechSynthesisUtterance` for the current question, cancel playback before recognition, abort recognition before playback, and clear both on reset/unmount.

- [x] **Step 4: Verify strict typing**

Run: `npm run typecheck`

Expected: no TypeScript errors in the new composable.

### Task 2: Voice Controls and Existing Answer Integration

**Files:**
- Create: `frontend/src/components/interview/InterviewVoiceControls.vue`
- Modify: `frontend/src/components/interview/InterviewQuestionPanel.vue`

**Interfaces:**
- `InterviewVoiceControls` props: `{ question: string; disabled: boolean }`.
- `InterviewVoiceControls` emit: `transcript: [text: string]`.
- `InterviewQuestionPanel` keeps `submit: [answer: string]` unchanged.

- [x] **Step 1: Build focused controls**

Render icon buttons for playing/stopping the question and starting/stopping microphone capture. Use `aria-pressed`, tooltips and an `aria-live` status area; do not hide the textarea.

- [x] **Step 2: Append final transcript safely**

Append a normalized final segment to the existing answer with a newline when typed content already exists; cap the combined answer at the existing 10,000-character limit.

- [x] **Step 3: Reset at question boundaries**

Key the control component by `questionIndex` so unmount cleanup prevents late recognition events from entering a new question.

- [x] **Step 4: Verify build**

Run: `npm run build`

Expected: Vue typecheck and Vite production build succeed.

### Task 3: Browser Acceptance and Documentation

**Files:**
- Modify: `docs/04-当前进度.md`
- Modify: `docs/06-参考项目调研.md`
- Modify: `docs/09-后续开发路线.md`

**Interfaces:**
- Consumes the built interview page and existing storage backend.
- Produces factual phase status and a clear remaining boundary for WebSocket/cloud ASR/TTS.

- [x] **Step 1: Verify visible fallback states**

Open `/interview` at desktop and mobile widths. Confirm text input remains usable, controls fit without horizontal overflow, unsupported recognition is explicit, and no console error appears.

- [x] **Step 2: Verify supported-browser behavior where available**

Start and stop question playback; start microphone capture only by explicit click. If automated Chromium cannot provide recognition, record that limitation rather than simulating a successful transcript.

- [x] **Step 3: Sync factual documentation**

Record the browser-native first slice, the unchanged backend answer path, zero paid model calls, and remaining WebSocket/cloud ASR/TTS work. Do not mark all of stage 10.1 complete.

- [x] **Step 4: Check and commit**

Run: `git diff --check`

Commit: `feat: 接入浏览器语音面试入口`
