# FastShoppr Specifications

This directory is the source of truth for what FastShoppr is, how it is built, and what "done" means for each phase. Code follows the specs; the specs are updated in the same change as the code.

## Spec-driven workflow

1. Read `AGENTS.md` and the current phase file.
2. Confirm the phase status is `in-progress` and dependencies are `done`.
3. Implement only the current phase.
4. Run the required commands and tests from `AGENTS.md`, including `connectedDebugAndroidTest` on the simulated device (emulator) when required, and confirm they pass.
5. Check off acceptance criteria only when every test has run and passed; record evidence, and update the completion log.
6. Set the phase status to `done`.
7. Stop and wait for review. Never start the next phase automatically.

A phase may not begin until the previous phase is reviewed and marked `done`. If a requirement changes, update the relevant spec and add an entry to `12-decisions.md` when the change is architectural.

## Documents

| Document | Purpose |
|---|---|
| `00-product.md` | Vision, users, journey, MVP scope, non-goals, success criteria |
| `01-architecture.md` | Stack, package layout, layers, state, DI, navigation, errors |
| `02-data-model.md` | Entities, enums, relationships, date rules, aisle sorting |
| `03-store-integration.md` | Lookup architecture, Walmart WebView, Home Depot HTTP, cache, rate limits |
| `04-ocr-and-input.md` | Typed, Digital Ink, camera, gallery, review, normalization |
| `05-on-device-ai.md` | Gemini Nano usage, fallbacks, prompts, privacy, testing |
| `06-scheduling-dates-calendar.md` | Shopping dates, custom interval, deferral parsing, calendar, overdue items, store hours |
| `07-background-work.md` | WorkManager, foreground service, notifications, retries |
| `08-pricing-and-custom-stores.md` | Post-MVP price comparison and custom local stores |
| `09-testing-quality.md` | Test strategy, fixtures, performance, accessibility, release checks |
| `10-security-privacy.md` | Local-first data, permissions, secrets, logging, release safety |
| `11-future-sharing-sync.md` | Deferred real-time sharing and account sync design |
| `12-decisions.md` | Accepted architectural and product decisions |
| `templates/phase-template.md` | Required structure for every phase file |

## Phase status

| Phase | Name | Status | Depends on |
|---|---|---|---|
| 0 | Feasibility | `done` | — |
| 1 | Foundation | `done` | 0 |
| 2 | Domain & persistence | `done` | 1 |
| 3 | Manual lists | `done` | 2 |
| 4 | Handwriting input | `done` | 3 |
| 5 | On-device AI | `done` | 4 |
| 6 | Store lookup | `blocked` | 0, 2 |
| 7 | Background routing | `not-started` | 5, 6 |
| 8 | Schedule & calendar | `not-started` | 3, 7 |
| 9 | MVP release | `not-started` | 8 |
| 10 | Pricing spike | `not-started` | 9 |
| 11 | Price comparison | `not-started` | 10 |

## Status values

- `not-started` — no implementation work has begun.
- `in-progress` — the phase is the only active implementation phase.
- `blocked` — work cannot continue; the blocker is recorded in the phase file.
- `done` — all acceptance criteria are checked and verified, every required test has run and passed, and review is completed.

## Scope guardrails

- The app is Java 21 with XML Views and ViewBinding. Do not add Kotlin app source, the Kotlin Android plugin, or Jetpack Compose. Gradle Kotlin DSL is allowed for build scripts.
- The current release is local-first. There is no backend, cloud sync, sharing, or authentication.
- Price comparison is post-MVP (Phases 10–11).
- Google Maps Popular Times is not used. Visit planning uses store hours and the user's schedule.
- On-device AI is optional and always has a deterministic fallback.
- Walmart WebView lookups require the foreground-service design.
- `rawText` is never replaced by a store match or AI suggestion.

## Evidence and completion logs

Each phase file contains a completion log. Record:

- Date, commands run, and pass/fail result.
- Manual demo result.
- Links to fixtures, screenshots, or test names where useful.
- Any known limitations carried into the next phase.

Do not mark a phase `done` with failing commands, unchecked criteria, or tests that did not run. A failed, errored, skipped, or unrun test — including `connectedDebugAndroidTest` on the simulated device (emulator) — leaves the phase incomplete.
