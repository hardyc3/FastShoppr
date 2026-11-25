# Phase 9A — Review Remediation

- **Status:** `in-progress`
- **Depends on:** Phase 9
- **Owner:** unassigned
- **Started:** 2026-11-24
- **Completed:** —
- **Last updated:** 2026-11-25

**Boundary note:** Phase 9A is the only active phase. **Phase 10 — Store Integration Coverage stays `not-started`; do not start it.** Phase 10 must remain blocked until explicitly instructed (D-020, D-046, D-047). Phase 11 — Pricing Spike and Phase 12 — Price Comparison also remain `not-started`.

## Goal

Restore the documented architectural and evidence boundaries surfaced by the Phase 9 review: a framework-free domain layer, MVVM state/adapter/Fragment boundaries, complete input and cleanup flows, honest security behavior, and test/spec evidence that matches reality. These are remediation items, not new product features.

## Context

Phase 9 closed as `done`, but review of the shipped tree found gaps between the specs (including `AGENTS.md`, `01-architecture.md`, `04-ocr-and-input.md`, and `12-decisions.md`) and the code and evidence. The domain layer imports Android/Architecture Components, adapters and Fragments hold state and logic that the architecture spec assigns to ViewModels/use cases, several spec'd input and cleanup paths are placeholders or unwired, and some completion text and unchecked acceptance criteria in earlier phase files are stale.

D-002:32 and `AGENTS.md` require the domain layer to be free of Android imports; `01-architecture.md:85` currently says "Java and Android imports", which contradicts both. D-047 inserts this phase after Phase 9. D-048 records the Android-free domain boundary and D-049 records the forget-store WebView wipe requirement; implementation of both is deferred to this phase. Nothing here authorizes Phase 10 work.

## In scope

- **Framework-free domain.** Remove `androidx.lifecycle.LiveData` from `domain/repository/ShoppingListRepository.java`, `ListItemRepository.java`, `StoreRepository.java`, `CaptureRepository.java`, and `ScheduleRepository.java`. Remove Android/Hilt/`work` dependencies from `domain/usecase/CreateScheduledListUseCase.java` (`android.content.Context`, `androidx.core.app.NotificationManagerCompat`, `dagger.hilt.android.qualifiers.ApplicationContext`, `com.fastshoppr.app.work.ScheduleNotificationHelper`). Put notification capability/permission and notification dispatch behind domain-owned ports with data-layer implementations. Update all fakes, unit tests, and instrumentation tests that implement or inject the affected interfaces. Reconcile `01-architecture.md:85` with `AGENTS.md` and D-002 so the specs state one rule.
- **MVVM boundaries.** Make `ui/calendar/CalendarUiState.java` immutable (public mutable fields at `:17-22`). Convert `CalendarDayAdapter`, `CalendarUpcomingAdapter`, and `ReviewAdapter` to `ListAdapter`/`DiffUtil`. Move Fragment-side date parsing and `LocalDate.now()` calls, permission requests/checks, and candidate decoding out of Fragments into ViewModels/use cases, leaving Fragments to render state and forward actions.
- **Spec gaps.** Implement the real typed-capture input path (currently a placeholder at `ui/capture/TypedCaptureFragment.java:15-36` that navigates with a placeholder capture ID). Make **Add line** available in review per `04-ocr-and-input.md:107`. Add ink-model deletion in Settings per `04-ocr-and-input.md:46`. Wire scheduled cache purge and the 30-day soft-delete cleanup into startup/scheduling so the documented retention rule is actually executed.
- **Security behavior.** Ensure **Forget store** wipes retailer WebView cookies and DOM storage (`CookieManager`/`WebStorage`) as required by D-032/D-043. Cancel in-flight Gemini Nano futures when the screen stops and discard late results (per D-029 foreground-only rule). An already started ML Kit call is bounded by its timeout and cannot be forcibly interrupted; add permission rationale and a settings deep link for permanent denial.
- **Quality/evidence.** Remove the skipped camera-permission test path (`CaptureFlowsTest.java:178-182` uses `Assume.assumeFalse` to skip when camera permission is already granted) and replace it with a deterministic test. Add a fake-clock rate-limiter test. Add a JVM test for `CreateScheduledListUseCase` using fakes for the new ports. Reconcile stale Phase 4/6/6b/6c/8 status text and the Phase 6 unchecked acceptance criteria at `phase-06-store-lookup.md:139`, `:148`, and `:149` **only with real evidence**; do not check boxes or rewrite status without it.

## Out of scope

- Phase 10 work: no automated lookup or branch selection for Lowe's, Target, or Smiths; no new Phase 10 brand coverage or device matrix. Phase 10 remains `not-started` and must not be started.
- Price comparison, price extraction, or any Phase 11/12 behavior.
- New dependencies (no new libraries, no Kotlin app source, no Compose, no coroutines/Flow/DataStore).
- Room schema migration unless investigation proves one is necessary; if proven, it needs its own decision entry, exported schema, and migration test before implementation.
- Changing the `rawText`, `displayName`, `normalizedText`, or soft-delete data invariants.
- Marking any earlier phase or this phase `done` without real command/test evidence.
- Starting more than one phase at a time.

## Requirements

### Architecture

- `domain` contains only pure Java: no `android.*`, `androidx.*`, Hilt, Room, or `com.fastshoppr.app.work` imports. Repository interfaces expose domain types and Java async types (`CompletableFuture`) or observation abstractions owned by the domain, not `LiveData`.
- Notification capability checks and notification dispatch are expressed as domain-owned ports (interfaces) implemented in `data`/`work` and bound through Hilt. Use cases depend on the ports, never on `Context`, `NotificationManagerCompat`, or `ScheduleNotificationHelper`.
- Presentational state classes are immutable; ViewModels expose immutable `UiState` via `LiveData` and one-off effects via a sealed `UiEvent`/single-event wrapper.
- Fragments render state and forward actions. They do not parse dates, call `LocalDate.now()`, request permissions, or decode persisted JSON.
- Adapters that display changing lists use `ListAdapter` with a `DiffUtil.ItemCallback`.
- `01-architecture.md:85` is corrected to the Android-free rule and cross-references `AGENTS.md`/D-002.

### Input and cleanup flows

- Typed capture accepts real text input, creates a capture session, and reaches review with real captured content; no placeholder capture ID reaches the review screen.
- Review **Add line** is available and preserves pending edits per `04-ocr-and-input.md:107`.
- Settings offers ink-model deletion with visible progress/result and no blocking of the rest of the app (`04-ocr-and-input.md:46`).
- Scheduled cache purge and 30-day soft-delete cleanup are invoked at startup and/or by the existing scheduler; active queries continue to exclude `deletedAt` rows.

### Security and privacy

- **Forget store** clears retailer cookies and DOM storage so a forgotten branch cannot silently persist.
- AI cleanup futures are cancelled when the review screen stops; late results are discarded and no new inference starts in the background. An already started ML Kit call is bounded by its timeout.
- Permission prompts show a rationale; permanent denial routes to the system settings screen via a deep link and does not block typed/manual entry.
- No item text, images, saved branch addresses, or location coordinates in logs.

### Quality gates

- Every test in the test plan runs and passes; no test is failed, errored, skipped, or unrun.
- `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck`, and `connectedDebugAndroidTest` pass with evidence recorded in the completion log.
- Spec status text is corrected only when backed by recorded evidence; unchecked criteria stay unchecked until that evidence exists.

## Technical approach

- Introduce domain ports such as a notification-capability/dispatcher interface and a permission-status interface (names to be confirmed during implementation); keep their implementations in `data`/`work` and bind them in Hilt modules. `CreateScheduledListUseCase` becomes a pure Java class testable on the JVM.
- Replace `LiveData` in repository interfaces with domain-owned observation or caller-supplied callbacks/`CompletableFuture`; update repository implementations, Hilt bindings, fakes, and all tests in the same change.
- Make `CalendarUiState` fields `final` and construct a new state object for every update. Add `DiffUtil.ItemCallback` implementations to `CalendarDayAdapter`, `CalendarUpcomingAdapter`, and `ReviewAdapter`; submit lists via `submitList`.
- Add use cases for date resolution (inject the domain clock rather than calling `LocalDate.now()` in Fragments), permission state/rationale decisions, and candidate decoding; ViewModels call them and expose results as state/events.
- Replace the `TypedCaptureFragment` placeholder with real input wiring through the existing capture-session and review flow, reusing `CaptureRepository` rather than `NavArguments.PLACEHOLDER_CAPTURE_ID`.
- Wire ink-model deletion through `RemoteModelManager` behind the existing settings/AI boundary; wire cleanup tasks into the current WorkManager/startup path rather than adding a new scheduler.
- Clear WebView state through `CookieManager` and `WebStorage` in the forget-store path shared by the branch-picker flows (D-043). Cancel the AI cleanup future in the screen-stop lifecycle hook.
- No new dependencies. Spotless preserves the repository's mixed CRLF/LF line endings.
- Only after code and tests are green, reconcile stale phase text: Phase 4 (`phase-04-handwriting-input.md:157`), Phase 6 (`phase-06-store-lookup.md:139`, `:148`, `:149`), Phase 6b (`phase-06b-store-lookup-manual-verification.md:139`), Phase 6c (`phase-06c-lookup-extraction-confidence-and-regex-simplification.md:120`), and Phase 8 (`phase-08-schedule-calendar.md:131`, `:225-227`) with citations to real evidence. Do not check Phase 6 criteria `:139`, `:148`, or `:149` without actual manual/live and full test-suite results.

## Data changes

- No entity or column changes are planned. Any Room migration is out of scope unless investigation proves one necessary, in which case it requires its own D-entry, exported schema, and `MigrationTest` before implementation.
- Repository interface signatures may change to remove `LiveData`. Preferences and SharedPreferences keys may gain a schedule/cache cleanup timestamp only if the existing key set cannot support the purge; document any new key here before adding it.
- Soft-delete rows continue to use `deletedAt`; the 30-day purge must not delete rows inside the restore window.

## UI changes

- Typed capture screen: real editable input, not the current placeholder body/continue button.
- Review screen: **Add line** action per `04-ocr-and-input.md:107`; adapters use `ListAdapter`/`DiffUtil` without changing visible behavior.
- Settings: ink-model deletion action; permission rationale and permanent-denial "Open settings" deep link.
- Calendar: immutable state rendering through `ListAdapter`-backed day/upcoming lists.
- Branch picker: **Forget store** clears cookies/DOM storage and updates visible store context accordingly.
- Preserve existing labels, content descriptions, TalkBack traversal, touch targets, and empty/loading/error states.

## Acceptance criteria

- [x] `domain` has no Android, AndroidX, Hilt, Room, or `work` imports; `LiveData` is removed from the five named repository interfaces and Android/`work` dependencies are removed from `CreateScheduledListUseCase`.
- [x] Notification and permission concerns are behind domain-owned ports with Hilt-bound data/work implementations; all fakes and tests are updated and compile.
- [x] `01-architecture.md:85` matches `AGENTS.md` and D-002: domain code is Java-only and Android-free.
- [x] `CalendarUiState` is immutable and `CalendarDayAdapter`, `CalendarUpcomingAdapter`, and `ReviewAdapter` use `ListAdapter`/`DiffUtil`.
- [x] Date parsing/`LocalDate.now()`, permission requests, and candidate decoding no longer occur in Fragments; they live in ViewModels/use cases.
- [ ] Typed capture takes real text input and reaches review with that text; no placeholder capture ID is used.
- [ ] Review **Add line** is available and preserves pending edits.
- [ ] Settings can delete the ink model with visible progress/result.
- [ ] Scheduled cache purge and 30-day soft-delete cleanup are wired into startup/scheduling and have JVM fake-clock coverage; instrumentation evidence is still pending.
- [ ] **Forget store** wipes `CookieManager` cookies and `WebStorage` DOM storage for retailer WebViews.
- [ ] JVM tests cover Gemini Nano future cancellation, late-result discard, and no further item started; manual confirmation on screen stop is still pending. An already started ML Kit call is bounded by its timeout.
- [ ] Permission rationale and permanent-denial settings deep link are present and do not block typed/manual entry.
- [ ] `CaptureFlowsTest.java:178-182` no longer skips via `Assume`; the replacement test runs deterministically with no skipped tests.
- [x] A fake-clock rate-limiter test and a JVM `CreateScheduledListUseCase` test with fakes exist and pass.
- [ ] Stale Phase 4/6/6b/6c/8 status text is reconciled only with cited evidence; Phase 6 criteria `:139`, `:148`, `:149` remain unchecked unless real manual/live and full-suite evidence exists.
- [x] Phase 10 remains `not-started`; no Phase 10 brand, pricing, or new-dependency work is introduced.
- [ ] `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck`, and `connectedDebugAndroidTest` pass with no new warnings or TODOs and evidence in the completion log.
- [ ] Every test in the test plan has run and passed, including `connectedDebugAndroidTest` on the simulated device (emulator); no test has failed, errored, or been skipped.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Domain import boundary scan | Static | No `android.*`/`androidx.*`/Hilt/Room/`work` imports under `domain` |
| Repository interface + fakes compile | Unit | `LiveData` removed; all implementations/fakes/tests updated |
| `CreateScheduledListUseCase` with fakes | Unit (JVM) | Fake clock, fake notification/permission ports; due/not-due, notify/no-notify, no backlog |
| Rate limiter with fake clock | Unit | Deterministic one-request-per-second-per-store assertion |
| `CalendarUiState` immutability | Unit | Fields final; state replacement on update |
| Adapter `DiffUtil` behavior | Unit | `ListAdapter` submission for calendar day, upcoming, review lists |
| Fragment logic extraction | Unit + instrumentation | Date/permission/candidate decisions in ViewModel/use case; no Fragment parsing |
| Typed capture → review | Espresso | Real text reaches review; no placeholder ID |
| Review **Add line** | Espresso | Pending edits preserved |
| Ink-model deletion | Espresso/manual | Delete action, progress, result; rest of app unblocked |
| Scheduled purge + 30-day cleanup | Unit + instrumentation | Fake clock; in-window rows retained, out-of-window rows purged |
| Forget-store WebView wipe | Espresso/manual | Cookies and DOM storage cleared |
| AI future cancellation | Unit | Future cancelled on screen stop; late result discarded; no further item started |
| Permission rationale + permanent denial | Espresso/manual | Rationale shown; settings deep link on permanent denial |
| Camera permission flows | Espresso | No `Assume`-based skip; deterministic granted/denied coverage |
| Phase status evidence reconciliation | Docs review | Citations to real command/test records only |
| Full static gates | `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck` | Evidence recorded in completion log |
| `connectedDebugAndroidTest` | Instrumentation | Simulated device (emulator) or physical device; no skips |

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored, been skipped, or failed to run. That includes every test in the test plan and `./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator) or a physical device. An unavailable device is not a pass: leave the phase `in-progress` or `blocked` and record the unrun test in the completion log.

## Manual demo

1. Confirm Phase 9A is `in-progress` with Phase 9 `done`, Phase 10 `not-started`, and no Phase 10 work started.
2. Capture a typed list, edit a line, tap **Add line** in review, and confirm pending edits are preserved and saved text equals the reviewed text.
3. In Settings, delete the ink model and confirm visible progress/result and that typed capture still works.
4. Pick a retailer branch, tap **Forget store**, and confirm the saved context and WebView cookies/DOM storage are cleared.
5. Deny a permission, confirm the rationale, then permanently deny and confirm the settings deep link; typed/manual entry remains available.
6. Enable AI cleanup, start a cleanup, leave the screen, and confirm the in-flight future is cancelled, its late result is discarded, and no further item is processed.
7. Run the scheduled purge and 30-day cleanup checks with a fake clock; confirm in-window rows are retained.
8. Reconcile stale phase text only after real evidence is recorded, then run the full static and instrumentation gates and record results.
9. Expected result: domain is Android-free, MVVM boundaries hold, spec and evidence match reality, no tests skipped, Phase 10 untouched.

## Risks

- Removing `LiveData` from repository interfaces touches many callers. Mitigation: change interfaces, implementations, fakes, and tests in one change and compile before proceeding.
- Domain-owned ports may duplicate Android capability checks. Mitigation: keep ports thin, put Android specifics in data/work implementations, and cover them with fakes.
- Larger `ListAdapter`/`DiffUtil` refactor could change review-state behavior. Mitigation: preserve stable IDs and verify with Espresso.
- Real typed capture and **Add line** may expose previously hidden capture-session assumptions. Mitigation: reuse `CaptureRepository` and the reviewed-line save path; do not change `rawText` invariants.
- Evidence reconciliation could overstate verification. Mitigation: require command/test citations and leave unchecked criteria unchecked.
- Work spilling into Phase 10 brands or pricing. Mitigation: enforce the Out of scope list and keep Phase 10 `not-started`.

## Open questions

- Which domain-owned port names/shapes best fit the existing Hilt modules, and can observation stay on `CompletableFuture`/callbacks without adding a dependency? Owner: unassigned.
- Does the scheduled cache purge require a new SharedPreferences key, or can existing timestamp keys be reused? Owner: unassigned.
- Which device/emulator will run `connectedDebugAndroidTest` and the live manual gates? Owner: unassigned.
- Can the Phase 6 live/manual criteria `:139` and `:148` be satisfied with real evidence, or must they remain unchecked and be recorded as a known limitation? Owner: unassigned.

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-24 | `./gradlew assembleDebug testDebugUnitTest lintDebug spotlessCheck assembleDebugAndroidTest --console=plain` | PASS | 298 JVM tests across 61 reports; 0 failures, 0 errors, 0 skipped. `lintDebug` passes despite dependency Kotlin-metadata `e:` lines from ML Kit GenAI artifacts. |
| 2026-11-24 | Static scans: domain imports, Fragment date logic, `Assume`, `TODO`/`FIXME`, Phase 10 status | PASS | Domain has no Android/AndroidX/Hilt/Room/`work` imports; no Fragment `LocalDate.now()` or date decoding; no `Assume`, `TODO`, or `FIXME`; Phase 10 still `not-started`. |
| 2026-11-24 | `./gradlew connectedDebugAndroidTest --console=plain` | BLOCKED | Failed with `No connected devices!`. Device `R5CT7058L2A` appeared briefly during the run, then disappeared; `adb reconnect offline` found no device. Instrumentation never executed. |
| 2026-11-24 | Manual demo steps 1–9 | NOT RUN | No live device session; device-dependent criteria stay unchecked. |
| 2026-11-24 | Stale Phase 4/6/6b/6c/8 evidence reconciliation | NOT DONE | No new manual/live evidence recorded; Phase 6 criteria `:139`, `:148`, `:149` remain unchecked. |
| 2026-11-25 | Review follow-up: forget-store ordering/failure UI, owner-scoped permission binder through a UI-owned interface, camera typed/gallery alternatives, schedule-specific rationale, persisted permanent-denial flags, ReviewAdapter text guard | PASS | JVM tests cover wipe-before-clear ordering, wipe-failure retention, restored denial state, and permission decisions. Fragments bind launchers only; ViewModels own permission checks/requests. |
| 2026-11-25 | `./gradlew spotlessApply assembleDebug testDebugUnitTest lintDebug spotlessCheck assembleDebugAndroidTest --console=plain` | PASS | 302 JVM tests across 61 reports; 0 failures, 0 errors, 0 skipped. `lintDebug` passes despite dependency Kotlin-metadata `e:` lines from ML Kit GenAI artifacts. |
| 2026-11-25 | Static scans: `git -c core.whitespace=cr-at-eol diff --check 42ea8fe`, domain Android-free boundary, `Assume`/`TODO`/`FIXME`, Phase 10 status | PASS | No whitespace errors with the repo's mixed CRLF/LF policy; domain has no Android/AndroidX/Hilt/Room/`work` imports (`javax.inject` DI annotations remain); no skipped-test markers or TODO/FIXME; Phase 10 stays `not-started`. |
| 2026-11-25 | `./gradlew connectedDebugAndroidTest --console=plain` | BLOCKED | `No connected devices!`; `adb devices -l` shows no attached device. Instrumentation never executed. |
| 2026-11-25 | Manual demo steps 1–9 | NOT RUN | No live device session; device/manual criteria stay unchecked. |
| 2026-11-25 | Stale Phase 4/6/6b/6c/8 evidence reconciliation | NOT DONE | No new manual/live evidence recorded; Phase 6 criteria `:139`, `:148`, `:149` remain unchecked. |

**Status note:** implementation and static verification are complete, but `connectedDebugAndroidTest`, the manual demo, and evidence reconciliation are not. Phase 9A remains `in-progress` until those gates pass and every acceptance criterion has evidence.
