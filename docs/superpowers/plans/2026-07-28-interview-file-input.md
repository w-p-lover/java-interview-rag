# Interview File Input Implementation Plan

> **For agentic workers:** Implement inline in the current branch; keep the user's uncommitted `application.yml` port change untouched.

**Goal:** Add local Markdown/TXT resume and JD extraction to the interview page while separating interview inputs from the long-lived knowledge base.

**Architecture:** A reusable Vue component reads and validates one local text file, then updates the existing string model. The setup form and backend API contract remain unchanged; knowledge-base edits are copy-only.

**Tech Stack:** Vue 3.5, TypeScript, Vite 8, existing SCSS tokens.

## Global Constraints

- All writes stay inside `D:\IdeaProjects\java-interview-rag`.
- Do not modify or stage the existing `src/main/resources/application.yml` change.
- Support only `.md`, `.markdown`, and `.txt` in this closure.
- Do not persist files or extracted interview text in localStorage or the knowledge base.

---

### Task 1: Reusable local text-file field

**Files:**
- Create: `frontend/src/components/interview/InterviewTextFileField.vue`

- [x] Add typed props and `defineModel<string>` for label, optional state, row count, character limit and placeholder.
- [x] Validate the filename extension, read with `File.text()`, reject blank or oversized text, and preserve the previous model on failure.
- [x] Render primary file selection, extraction status, and the editable fallback textarea using existing design tokens.

### Task 2: Wire interview inputs and correct knowledge-base copy

**Files:**
- Modify: `frontend/src/components/interview/InterviewSetupForm.vue`
- Modify: `frontend/src/views/KnowledgeWorkbenchView.vue`
- Modify: `frontend/src/components/ImportPanel.vue`

- [x] Replace the two raw textareas with file-field instances while retaining the existing payload.
- [x] Remove resume/JD wording from the long-lived knowledge import page and stable-source example.

### Task 3: Verify and record the closure

**Files:**
- Modify: `README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

- [x] Run `npm run build`.
- [x] Verify supported and unsupported file selection plus responsive layout in a browser without calling backend APIs.
- [x] Review the diff, update factual project docs, stage only directly related files, and commit with a concise Chinese message.
