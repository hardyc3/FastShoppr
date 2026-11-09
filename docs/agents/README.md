# FastShoppr Agent Guides

Human-readable role guides for focused coding/review agents in this repository. These are **not** executable configs; use them as prompt/context when running an agent.

## Global rules (apply to every agent)

- Read `AGENTS.md`, `docs/specs/README.md`, and the current phase file before acting.
- **Strict by default.** Follow repo architecture, data invariants, security/privacy rules, and prohibited actions.
- **Java-only app code.** No Kotlin app source/plugins, no Compose, no coroutines/Flow/DataStore in app code. Gradle Kotlin DSL allowed for build scripts.
- **Local-first MVP.** No backend, analytics, sharing/sync/auth, or paid cloud LLM in Phases 0–11 unless a spec/phase explicitly allows it.
- **Never replace `rawText`** with a store match or AI suggestion. `displayName` changes require explicit user confirmation behavior in product code.
- Do not commit secrets, API keys, signing material, or debug-only shortcuts.
- Respect phase workflow: only the active phase is implemented; do not mark phases done with failing commands/unchecked criteria.
- When a change is architectural, update/add an entry in `docs/specs/12-decisions.md`.

## Agents

| File | Purpose |
|---|---|
| `refactor.md` | Restructure/improve code with **aggressive refactor allowed** (APIs/modules may change) while preserving behavior and obeying tests/architecture/invariants. |
| `pre-commit-review.md` | Review staged/changed code before commit; blocks on correctness, invariant, security, and policy violations. |
| `security-review.md` | Focused security/privacy review (permissions, secrets, logging, store access, input handling, release safety). |
| `performance-review.md` | Focused performance review (main-thread work, Room queries, background/lookup behavior, UI/rendering, memory). |
| `maintainability-review.md` | Focused readability/structure/testability review aligned with repo patterns. |
| `spec-alignment-ux-review.md` | Review against product/spec requirements and user experience (flows, states, accessibility cues, copy/feedback). |
| `phase-implementer.md` | Implement only the current phase, run required checks, update acceptance evidence/logs. |
| `spec-docs-updater.md` | Keep specs, phase files, decisions, and completion logs aligned with actual behavior/changes. |

## Suggested usage

- Before commit: `pre-commit-review` (optionally plus `security-review` or `performance-review` for risky changes).
- Large cleanup: `refactor`, then `pre-commit-review` + relevant tests.
- New phase work: `phase-implementer`, then `spec-docs-updater`.
- Feature/UX changes: `spec-alignment-ux-review` + `maintainability-review`.
