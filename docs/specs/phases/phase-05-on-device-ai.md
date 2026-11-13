# Phase 5 — On-Device AI

- **Status:** `done`
- **Depends on:** Phase 4
- **Owner:** unassigned
- **Started:** 2026-11-11
- **Completed:** 2026-11-11
- **Last updated:** 2026-11-11

## Goal

Add optional, user-confirmed item-name cleanup and OCR post-processing using Gemini Nano on supported devices, with a deterministic rules fallback.

## Context

Phase 0 confirmed availability and constraints. This phase integrates the Java ML Kit GenAI Prompt API without making AI a dependency for core behavior.

## In scope

- `ItemNameCleaner` interface and implementations:
  - `GeminiNanoItemNameCleaner`
  - `RuleBasedItemNameCleaner`
  - `NoOpItemNameCleaner`
- Availability detection and model download flow.
- Prompt construction and JSON validation.
- Rules fallback.
- Settings toggle (off by default).
- Review-screen suggestions with accept/edit/dismiss.
- Batch cleanup with cancellation.
- Tests with fakes and rule fixtures.

## Out of scope

- AI date parsing beyond a suggestion hook (full date parsing is Phase 8).
- AI price or route reasoning.
- Background inference.
- Cloud LLM or API keys.
- Sharing or authentication.

## Requirements

### Availability

- Use the Java GenAI API package (`com.google.mlkit.genai.prompt.java`).
- Check `AVAILABLE`, `DOWNLOADABLE`, `DOWNLOADING`, `UNAVAILABLE`, and quota/busy states.
- Never download a model without user action.
- Keep the cleanup action available when the user has enabled AI; when the model is unavailable, use rules and show the fallback note (see D-030).
- Handle unsupported devices, unlocked bootloaders, and initialization delays gracefully.

### Cleanup

- Clean one item at a time, max five per session.
- Input max 200 characters.
- Context max five nearby items.
- Return JSON with `displayName`, `confidence`, `reason`.
- Validate JSON with Gson and reject malformed, empty, URL-containing, or overlong output.
- Reject changes to numbers/sizes unless clearly a typo.
- On validation failure, return `Unavailable` so the coordinator uses rules (see D-030).

### Rules fallback

- Whitespace/casing cleanup.
- Remove list markers.
- Expand documented abbreviations.
- Never change numbers or units.
- Deterministic and unit-tested.

### UX

- Settings toggle: "Improve item names with on-device AI" (off by default).
- Review screen action: "Clean up names" with suggestion count.
- Before/after list with accept, edit, dismiss.
- "Accept all" requires confirmation.
- Dismissed suggestions are not shown again for that item.
- AI never blocks saving.
- Show a short note when AI is unavailable.

### Lifecycle

- AI runs only in the foreground.
- Close sessions when leaving the screen.
- Cancel in-flight work on screen destruction.
- Do not call AI from a Worker or foreground service.

## Technical approach

- `ItemNameCleaner` with `CompletableFuture` results.
- `GeminiNanoItemNameCleaner` wraps the Java `GenerativeModel`/Prompt API.
- `RuleBasedItemNameCleaner` is a pure Java class.
- `ItemNameCleanerCoordinator` selects the implementation and records the source.
- Cleanup state and events live in `ReviewViewModel`; the suggestion sheet observes that state.
- Inject `ExecutorService` and `Clock`.
- Settings shows availability status instead of adding a separate debug screen, so no item data is exposed.

## Data changes

- Store accepted `displayName` only.
- Do not persist prompts or model output.
- Add `ai_cleanup_enabled` to `SettingsPreferences` (already defined in Phase 2).
- No schema change expected.

## UI changes

- Settings toggle and availability status.
- Review-screen cleanup action and suggestion sheet/dialog.
- Before/after rows.
- Error/empty states.
- Accessibility labels.

## Acceptance criteria

- [x] AI cleanup is off by default and can be enabled in settings.
- [x] Availability states are handled without crashes.
- [x] Suggestions are shown before/after and require confirmation.
- [x] `rawText` is never changed by AI.
- [x] Rules fallback works when AI is unavailable.
- [x] Malformed model output is rejected.
- [x] Quota/busy errors fall back and do not block saving.
- [x] AI never runs in a Worker, service, or unattended background work.
- [x] No prompts or item text are logged.
- [x] No cloud LLM or API key is used.
- [x] Unit tests cover rules, validation, and fallback.
- [x] Manual device test records Gemini Nano behavior. On physical device `SM-A536U1` (`R5CT7058L2A`, Android 16) the live Settings screen reports: "On-device AI is not available on this device. Item names are cleaned with automatic rules." Toggle enable/persist works; fallback path is the expected on-device behavior for an unsupported/unavailable Nano device. Emulator (`emulator-5554`, API 36) also reports `UNAVAILABLE` (no AICore). No supported Gemini Nano device was available to observe an `AVAILABLE`/download/generation flow; rules fallback and faked-AI instrumentation cover cleanup behavior.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Rules cleaner | Unit | Abbreviations, markers, numbers |
| JSON validation | Unit | Malformed, empty, URL, long |
| Fallback | Unit | Unavailable/quota/timeout |
| Suggestion UI | Espresso | Accept/edit/dismiss |
| Settings toggle | Espresso | Persists |
| Live AI | Manual | Supported device |

## Manual demo

1. Enable AI cleanup in settings.
2. Capture a list with abbreviations such as "tp" and "pb".
3. Open review and tap "Clean up names".
4. Accept one suggestion, edit another, dismiss a third.
5. On an unsupported device or when the model is unavailable, enable AI and confirm rules still clean obvious cases.
6. On an unsupported device/emulator, confirm the app works normally.

## Risks

- Gemini Nano unavailable on most devices. Mitigation: deterministic rules fallback with an explicit note, and the feature stays off until the user enables it.
- Model output instability. Mitigation: schema validation and user confirmation.
- Quota limits. Mitigation: batch caps, cancellation, and fallback.
- Accidental raw-text changes. Mitigation: separate `displayName` and tests.

## Open questions

- Resolved: cleanup runs only on demand from the review screen; it is not automatic after OCR.
- Resolved: dismissed suggestions are remembered only for the current review screen session (no cross-session persistence).

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-11 | `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck` | pass | Java-only app code; GenAI dependency `com.google.mlkit:genai-prompt:1.0.0-beta5`; main-thread async updates use injected `MainThreadExecutor` (no restricted `ArchTaskExecutor`). Lint clean of errors. |
| 2026-11-11 | `connectedDebugAndroidTest` on `emulator-5554 - 16` | pass | 22/22 tests pass, including `AiCleanupFlowTest` (accept/edit/dismiss keeps `rawText`, saves accepted `displayName`, settings toggle persists). Instrumentation uses `FakeAiModule`; no live model calls. |
| 2026-11-11 | Manual live AI check on `SM-A536U1` (`R5CT7058L2A`, Android 16) | pass (unavailable on device) | Real debug build (`AiModule`/Gemini Nano cleaner) on physical device: Settings status = "On-device AI is not available on this device. Item names are cleaned with automatic rules." AI toggle enables/persists while unavailable. Matches expected fallback behavior for unsupported/unavailable Nano; emulator likewise `UNAVAILABLE`. |
| 2026-11-11 | `connectedDebugAndroidTest` on `SM-A536U1` | partial suite (Phase 5 ok) | Targeted device run: `AiCleanupFlowTest` passed; full suite 21/22 with one unrelated `ManualListsFlowTest` matcher issue fixed and reverified passing on device. |

**Known limitation:** No tested device reported Gemini Nano `AVAILABLE`/downloadable; live model generation/download could not be observed. Unavailable-device behavior was recorded on real hardware and emulator, and all AI/cleanup paths (including rules fallback) are covered by unit and faked instrumentation tests. If a Nano-capable device becomes available, record its availability/download/cleanup behavior in a follow-up note.
