# Phase 5 — On-Device AI

- **Status:** `not-started`
- **Depends on:** Phase 4
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-03

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
- Hide AI actions when unavailable.
- Handle unsupported devices, unlocked bootloaders, and initialization delays gracefully.

### Cleanup

- Clean one item at a time, max five per session.
- Input max 200 characters.
- Context max five nearby items.
- Return JSON with `displayName`, `confidence`, `reason`.
- Validate JSON with Gson and reject malformed, empty, URL-containing, or overlong output.
- Reject changes to numbers/sizes unless clearly a typo.
- On validation failure, return no change and use rules.

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
- `AiCleanupViewModel` exposes state and events.
- Inject `ExecutorService` and `Clock`.
- Add a debug-only screen or log to inspect availability without exposing user data.

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

- [ ] AI cleanup is off by default and can be enabled in settings.
- [ ] Availability states are handled without crashes.
- [ ] Suggestions are shown before/after and require confirmation.
- [ ] `rawText` is never changed by AI.
- [ ] Rules fallback works when AI is unavailable.
- [ ] Malformed model output is rejected.
- [ ] Quota/busy errors fall back and do not block saving.
- [ ] AI never runs in a Worker, service, or background thread.
- [ ] No prompts or item text are logged.
- [ ] No cloud LLM or API key is used.
- [ ] Unit tests cover rules, validation, and fallback.
- [ ] Manual device test records Gemini Nano behavior.

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
5. Disable AI and repeat; confirm rules still clean obvious cases.
6. On an unsupported device/emulator, confirm the app works normally.

## Risks

- Gemini Nano unavailable on most devices. Mitigation: rules fallback and feature hidden when unavailable.
- Model output instability. Mitigation: schema validation and user confirmation.
- Quota limits. Mitigation: batch caps, cancellation, and fallback.
- Accidental raw-text changes. Mitigation: separate `displayName` and tests.

## Open questions

- Should AI cleanup run automatically after OCR review or only on demand?
- Should the app remember dismissed suggestions across sessions?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
