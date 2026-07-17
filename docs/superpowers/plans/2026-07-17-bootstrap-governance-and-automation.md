# Bootstrap Governance and Automation Implementation Plan

> **For agentic workers:** This plan is executed inline in the current session because it creates the initial repository governance and recurring automation; no application feature code is included.

**Goal:** Create the persistent repository instructions and project-state documents required for autonomous development, then create a recurring Codex automation scoped only to `D:\IdeaProjects\java-interview-rag`.

**Architecture:** The recurring automation prompt controls run-time behavior and autonomous task selection. `AGENTS.md` controls durable repository engineering rules. The `docs` files hold product positioning, scope, roadmap, mutable progress, research decisions, and append-only development history.

**Tech Stack:** Markdown, Git, Codex recurring automation.

## Global Constraints

- Do not modify `D:\IdeaProjects\blog\blog-springboot`.
- Keep `java-interview-rag` on the Java 17, Spring Boot 3, Spring AI, RAG, and later lightweight-Agent route.
- Allow ordinary technical decisions to be made autonomously without asking the user.
- Use focused verification; do not add or run unrelated tests.
- Do not push, create a pull request, or deploy.

---

### Task 1: Establish repository governance

**Files:**
- Create: `AGENTS.md`
- Create: `README.md`
- Create: `.gitignore`
- Create: `docs/00-项目定位.md`
- Create: `docs/01-需求边界.md`
- Create: `docs/02-技术方案.md`
- Create: `docs/03-阶段路线.md`
- Create: `docs/04-当前进度.md`
- Create: `docs/05-开发记录.md`
- Create: `docs/06-参考项目调研.md`
- Create: `docs/07-技术决策记录.md`

- [x] Write durable repository constraints in `AGENTS.md`.
- [x] Separate product scope, roadmap, progress, research, and decisions across the listed documents.
- [x] Verify all required files exist and contain no unresolved placeholders.

### Task 2: Create the recurring automation

**Files:**
- Create: `.codex/autonomous-development-prompt.md`

- [x] Write the run-time automation prompt, including autonomous decision rules, reference-project evaluation, main-repository isolation, and hard-blocker handling.
- [x] Check Codex project identifiers and document the temporary management-project limitation for the newly created directory.
- [x] Create one active daily local recurring automation whose prompt restricts all writes to this repository.
- [x] Read back the automation and verify its target, status, schedule, and prompt.

### Task 3: Final structural verification

- [x] Check `git status --short` to confirm only the new repository files are present.
- [x] Confirm `D:\IdeaProjects\blog\blog-springboot` was not modified by this work.
- [x] Validate required headings and reference URLs using a focused script.
- [x] Report the branch, created files, automation details, and anything intentionally not implemented.
