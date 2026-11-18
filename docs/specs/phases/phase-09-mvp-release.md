# Phase 9 — MVP Release

- **Status:** `in-progress`
- **Depends on:** Phase 8
- **Owner:** unassigned
- **Started:** 2026-11-17
- **Completed:** —
- **Last updated:** 2026-11-17

## Goal

Harden the local routing MVP for release: accessibility, performance, privacy, offline behavior, release build, and documentation.

## Context

Phases 0–8 deliver the feature set. This phase removes rough edges, verifies quality, and produces a releasable build. Sharing, authentication, and price comparison remain out of scope.

## In scope

- Accessibility audit and fixes.
- Dark theme and large-font support.
- Performance tuning for 100+ items.
- Offline behavior verification.
- Error/empty/loading state audit.
- Privacy and backup rules.
- Release build with R8.
- Signing configuration via environment, not committed.
- Dependency and permission audit.
- README, privacy text, and demo script.
- Final bug fixes.

## Out of scope

- New features.
- Sharing, sync, Google Sign-In.
- Price comparison.
- Automated lookup for Lowe's, Target, or Smiths.
- Major refactors unless required for a release blocker.

## Requirements

### Accessibility

- All interactive controls have labels/content descriptions.
- Touch targets >= 48dp.
- Text scales without clipping.
- TalkBack can traverse lists, store sections, calendar, and dialogs.
- Status is not conveyed by color alone.
- Contrast meets WCAG AA where practical.
- Error messages are announced.

### Performance

- 100-item list scrolls smoothly.
- Sorting completes under one second.
- List detail queries are indexed.
- Images are downscaled before OCR.
- WebViews are destroyed after use.
- No main-thread disk/network work.
- No obvious memory leaks in Fragments/ViewModels.

### Privacy and security

- No item text, images, addresses, or coordinates in logs.
- No secrets or keystores committed.
- Backup rules reviewed.
- Permissions audit: only camera, location, notifications, foreground service.
- WebView hardened per `10-security-privacy.md`.
- "Delete all data" action present.

### Release build

- R8 enabled with required keep rules.
- Resource shrinking enabled.
- Version code/name set.
- Release installs and launches on a clean device/emulator.
- Signing supplied by environment.
- No debug shortcuts in release.

### Documentation

- README covers build, run, test, and known limitations.
- Privacy text explains local-only data.
- Demo script covers the full journey.
- Phase statuses updated.

## Technical approach

- Use Android Studio lint and accessibility scanner.
- Use `StrictMode` in debug to catch main-thread I/O.
- Use Layout Inspector/Profiler for list performance.
- Add R8 keep rules for Room, Hilt, Gson, and ML Kit.
- Use `android:dataExtractionRules` and backup rules.
- Use a debug-only fake data source if needed; ensure it is excluded from release.

## Data changes

- Backup rules disable cloud backup and device transfer.
- Room schema v3→v4 adds two composite indexes on `list_items` for the list-detail queries
  (`listId`, `deletedAt`, `sortPosition`) and (`listId`, `deletedAt`, `storeId`, `sortPosition`).
  This is the documented release-blocker exception for the 100-item performance criterion;
  `MIGRATION_3_4`, exported schema `4.json`, and a migration test are included.

## UI changes

- Accessibility fixes.
- Empty/error state polish.
- Large-font layout fixes.
- Dark theme fixes.
- Privacy/settings text.

## Acceptance criteria

- [ ] Accessibility audit passes with no critical issues.
- [ ] TalkBack can complete the core journey.
- [ ] 100-item list scrolls smoothly and sorts under one second.
- [ ] Offline mode works for lists and cached aisles.
- [ ] No personal data appears in logs.
- [ ] Permissions audit is clean.
- [ ] Backup rules exclude sensitive data as documented.
- [ ] "Delete all data" works and is tested.
- [ ] Release build succeeds with R8 and resource shrinking.
- [ ] Release build installs and launches on a clean device/emulator.
- [ ] No secrets, keystores, or debug shortcuts are committed.
- [ ] README and privacy text are complete.
- [ ] Full manual demo succeeds.
- [ ] All required commands pass.
- [ ] Every test in the test plan has run and passed, including `connectedDebugAndroidTest` on the simulated device (emulator); no test has failed, errored, or been skipped.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Accessibility scan | Manual/tool | TalkBack, contrast |
| 100-item performance | Instrumentation/manual | Scroll/sort |
| Offline journey | Manual | Airplane mode |
| Release install | Manual | Clean device |
| Log audit | Manual | No PII |
| Delete all data | Espresso | Clears DB/cache/prefs |
| R8 keep rules | Release build | Launch and core flow |

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored, been skipped, or failed to run. That includes every test in the test plan and `./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator) or a physical device. An unavailable device is not a pass: leave the phase `in-progress` or `blocked` and record the unrun test in the completion log.

## Manual demo

1. Install the release build on a clean device.
2. Create a list, add typed and handwritten items, review, and save.
3. Organize with Walmart and Home Depot.
4. Confirm aisle grouping and sort toggle.
5. Defer an item to next week and check the calendar.
6. Enable TalkBack and complete a checkoff.
7. Turn on airplane mode and confirm cached data works.
8. Delete all data and confirm the app returns to empty lists with built-in stores still available; no onboarding screen is shown.

## Risks

- R8 stripping required classes. Mitigation: keep rules and release smoke test.
- Accessibility regressions. Mitigation: checklist and TalkBack pass.
- Backup leaking sensitive data. Mitigation: explicit backup rules.
- Performance regressions from large lists. Mitigation: profiler and indexed queries.

## Open questions

- Resolved: the release includes in-app privacy text in Settings plus a repo-root `PRIVACY.md`;
  no hosted privacy policy URL is used for this MVP (D-038).
- What is the target Play Store listing date?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-17 | `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck`, `assembleRelease` | PASS | Full command set BUILD SUCCESSFUL; 198 unit tests, 0 failures/errors/skipped; lint 0 errors (20 pre-existing warnings). Both unsigned and env-signed release builds succeed with R8 + resource shrinking. |
| 2026-11-17 | `connectedDebugAndroidTest` on emulator-5554 (API 36) | PASS | 35 tests, 0 failures/errors/skipped with animation scales disabled. Includes delete-all, 120-item large-list, and 3→4 migration tests. |
| 2026-11-17 | Signed release install/launch + core UI journey on clean emulator | PASS | Signed APK installed and launched with no R8 crashes; create list, add store/item, checkoff, sort, persistence across navigation, calendar, delete-all (empty lists, no onboarding), airplane-mode relaunch all exercised. Logcat showed no entered item/store text. |
| 2026-11-17 | `connectedDebugAndroidTest` on physical Samsung SM-A536U1 (`R5CT7058L2A`, Android 16/API 36) | PASS | 35 tests, 0 failures/errors/skipped. Device animation scales already 0.0. |
| 2026-11-17 | Signed release install/launch + core UI journey on physical device | PASS | Release APK launched cleanly (0 fatal logcat lines, no R8 linkage errors). Create list, add store, add typed items, checkoff, sort toggle, persistence across navigation, Settings privacy text, delete-all (empty lists, built-in stores, no onboarding), calendar localized day description, airplane-mode relaunch all exercised. No entered item/store text in logcat. |
| 2026-11-17 | TalkBack probe + Accessibility Scanner on physical device | PARTIAL | Samsung TalkBack temporarily enabled; app launched with 0 fatal lines and first-run tutorial dismissed, but scripted focus traversal did not expose usable app-focused nodes, so spoken-output remains a manual gate. Google Accessibility Scanner is not installed (only Samsung's scanner). Device accessibility settings restored to original values and TalkBack force-stopped. |
| 2026-11-17 | TalkBack journey, Accessibility Scanner, full handwriting/photo/organize/AI demo | NOT RUN | Manual-only gates still require a device operator. Step-by-step instructions are in `docs/release/phase-09-manual-tests.md`. See `docs/release/phase-09-verification.md`. Phase remains `in-progress`. |
