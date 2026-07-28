# PDF Resume Extraction Implementation Plan

> **For agentic workers:** Implement inline in the current branch; keep the user's uncommitted `application.yml` port change untouched. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract text-layer PDF resumes and job descriptions locally in the interview page while preserving the existing string API and privacy boundary.

**Architecture:** A framework-independent TypeScript utility owns file recognition and extraction. The Vue field consumes its typed result and remains responsible only for UI state and `v-model`; PDF.js and its worker are loaded only for PDF files.

**Tech Stack:** Vue 3.5, TypeScript 5.9, Vite 8, `pdfjs-dist 5.4.624`, Playwright CLI, Poppler.

## Global Constraints

- All repository writes stay inside `D:\IdeaProjects\java-interview-rag`.
- Do not modify or stage `src/main/resources/application.yml`.
- Support text-layer PDF, Markdown, and TXT; do not implement OCR or DOCX in this closure.
- Do not upload or persist the selected interview file.
- Reject PDFs larger than 10 MiB and extracted text above the existing field character limit.

---

### Task 1: PDF.js dependency and local extractor

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Create: `frontend/src/utils/interviewDocumentText.ts`

**Interfaces:**
- Produces: `extractInterviewDocumentText(file: File): Promise<InterviewDocumentExtraction>` with `content`, `format`, and optional `pageCount`.

- [x] Install exact dependency `pdfjs-dist@5.4.624`.
- [x] Recognize `.md`, `.markdown`, `.txt`, and `.pdf`; reject all other extensions.
- [x] Read text files with `File.text()` and PDF files with lazily imported PDF.js plus the Vite worker URL.
- [x] Preserve page boundaries and meaningful line breaks; reject oversized, encrypted, damaged, and textless PDFs with Chinese errors.

### Task 2: Interview file-field integration

**Files:**
- Modify: `frontend/src/components/interview/InterviewTextFileField.vue`

**Interfaces:**
- Consumes: `extractInterviewDocumentText(File)`.
- Produces: unchanged `defineModel<string>` content for `InterviewSetupForm`.

- [x] Replace inline extension and text reading logic with the extractor utility.
- [x] Add PDF to the file input accept list and visible format label.
- [x] Show PDF page count and extracted character count on success.
- [x] Preserve the previous text on every extraction failure.

### Task 3: Verification and project facts

**Files:**
- Modify: `README.md`
- Modify: `docs/01-需求边界.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

- [x] Generate and render a two-page Chinese/English text PDF plus an image-only scanned PDF under `tmp/pdfs/`.
- [x] Run `npm run build` and verify the production bundle includes a separate PDF.js worker asset.
- [x] Use Playwright to verify text extraction, scanned-PDF rejection, Markdown/TXT regression, console health, and 320px width.
- [x] Remove temporary PDF and browser artifacts, review the diff, stage only related files, and commit with a concise Chinese message.
