# Phase 9 — MVP Release

- **Status:** `in-progress`
- **Depends on:** Phase 8
- **Owner:** unassigned
- **Started:** 2026-11-17
- **Completed:** —
- **Last updated:** 2026-11-22

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
- Generic WebView branch picker in Settings, with the saved branch shown on the Organize screen.
- Final bug fixes.

## Out of scope

- New features.
- Sharing, sync, Google Sign-In.
- Price comparison.
- Automated lookup for Lowe's, Target, or Smiths.
- Automatic first-item store prompting without branch context; picking a branch
  on first use of a store is explicit.
- ZIP storage, location permission, HTTP locator parsing/cache, or manual branch
  entry (D-043).
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

- No item text, images, or saved branch addresses in logs. No ZIP or coordinates are collected.
- No secrets or keystores committed.
- Backup rules reviewed.
- Permissions audit: only camera, notifications, foreground service.
- WebView hardened per `10-security-privacy.md`.
- "Delete all data" action present.

### Branch picker (Phase 9)

- Settings has a brand selector and a **Pick branch** button for Walmart, Home
  Depot, Lowe's, Target, and Smiths. Custom stores have no automated locator.
- Tapping **Pick branch** opens that brand's public store-locator page in a
  WebView. The user navigates, selects, and confirms a branch.
- First use of a store requires picking its branch. Settings can change it.
- Only the selected branch name and street/city/state are saved, one per brand.
  Postal codes parsed from retailer pages are discarded before saving or
  display. No ZIP, coordinates, location permission/dependency, HTTP locator
  parsing, or locator cache.
- The Organize screen shows the saved branch for brands present on the list;
  it does not change `rawText` or aisles.
- "Delete all data" clears all saved branches.

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

- Use Android Studio lint and TalkBack checks.
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
- Room schema v4→v5 retires the removed sixth built-in brand. `MIGRATION_4_5`
  converts any legacy seeded store row to an archived custom store named
  "Removed store", clears its aisle cache, and exported schema `5.json` plus a
  migration test are included. No item rows are deleted.
- Branch picker uses SharedPreferences only: one `selected_store_<BRAND>` JSON
  value per brand holding the branch name and street/city/state. No ZIP,
  locator cache, location permission, dependency, or Room schema change.

## UI changes

- Accessibility fixes.
- Empty/error state polish.
- Large-font layout fixes.
- Dark theme fixes.
- Privacy/settings text.
- Settings "Pick branch" section: brand selector, **Pick branch** button,
  WebView locator screen, and saved-branch summary.
- Writing screen **Save** button and review **Add line**/**Discard**/**Save**
  navigation.
- Organize screen store-context lines for saved branches.

## Acceptance criteria

- [ ] Accessibility audit passes with no critical issues.
- [ ] TalkBack can complete the core journey.
- [ ] 100-item list scrolls smoothly and sorts under one second.
- [ ] Offline mode works for lists and cached aisles.
- [ ] No personal data appears in logs.
- [x] Permissions audit is clean.
- [ ] A branch can be picked for each of the five supported brands from the
  brand's public locator WebView; the summary persists across restarts.
- [ ] First use of a store opens the branch picker; Settings can change the
  saved branch.
- [ ] Saved branches appear on the Organize screen only for brands present on
  the list, and never replace `rawText` or aisles.
- [ ] Writing **Save** opens review; review **Add line** preserves pending
  changes, **Discard** saves none, and **Save** saves all.
- [ ] "Delete all data" clears all saved branches; no ZIP, coordinates, or
  locator cache exist.
- [x] Backup rules exclude sensitive data as documented.
- [x] "Delete all data" works and is tested.
- [x] Release build succeeds with R8 and resource shrinking.
- [ ] Release build installs and launches on a clean device/emulator.
- [x] No secrets, keystores, or debug shortcuts are committed.
- [x] README and privacy text are complete.
- [ ] Full manual demo succeeds.
- [x] All required commands pass.
- [ ] Every test in the test plan has run and passed, including `connectedDebugAndroidTest` on the simulated device (emulator); no test has failed, errored, or been skipped.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Accessibility check | Manual | TalkBack, labels, contrast, touch targets |
| 100-item performance | Instrumentation/manual | Scroll/sort |
| Offline journey | Manual | Airplane mode |
| Release install | Manual | Clean device |
| Log audit | Manual | No PII |
| Delete all data | Espresso | Clears DB/cache/prefs and saved branches |
| Branch picker | Unit + Espresso | Brand selector, **Pick branch** WebView delegate, per-brand save, first-use prompt, Settings change, delete-all |
| Handwriting **Save**/review | Espresso + manual | **Add line** preserves pending changes, **Discard** saves none, **Save** saves all |
| Organize store context | Unit | Saved branches appended for brands on the list |
| R8 keep rules | Release build | Launch and core flow |

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored, been skipped, or failed to run. That includes every test in the test plan and `./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator) or a physical device. An unavailable device is not a pass: leave the phase `in-progress` or `blocked` and record the unrun test in the completion log.

## Manual demo

1. Install the release build on a clean device.
2. Create a list, add typed and handwritten items; tap **Save** on the writing
   screen, then on review try **Add line**, **Discard**, and **Save** and confirm
   the expected list result.
3. In Settings, select Walmart and tap **Pick branch**; navigate the WebView
   locator page and confirm a branch. Repeat with one other brand, verify both
   appear in the saved-branch summary, then change one branch from Settings.
4. Organize with Walmart and Home Depot and confirm the saved branches appear on
   the Organize screen without changing item text. On first use of a new store,
   confirm the branch picker opens before the lookup.
5. Confirm aisle grouping and sort toggle.
6. Defer an item to next week and check the calendar.
7. Enable TalkBack, check off an item, and confirm the state persists after reopening the list.
8. Turn on airplane mode and confirm cached data works.
9. Delete all data and confirm the app returns to empty lists with built-in stores still available, saved branches cleared, and no onboarding screen is shown.

## Risks

- R8 stripping required classes. Mitigation: keep rules and release smoke test.
- Accessibility regressions. Mitigation: checklist and TalkBack pass.
- Backup leaking sensitive data. Mitigation: explicit backup rules.
- Performance regressions from large lists. Mitigation: profiler and indexed queries.

## Open questions

- Resolved: the release includes in-app privacy text in Settings plus a repo-root `PRIVACY.md`;
  no hosted privacy policy URL is used for this MVP (D-038).
- What is the target Play Store listing date?

## Resolution notes (2026-11-22)

- D-044: the tool-based accessibility gate is dropped. TalkBack and
  accessibility checks remain.
- ZIP/location HTTP locator replaced by generic WebView branch picker (D-043).
- Handwriting: writing **Save** opens review; review **Add line** returns to
  writing with pending changes, **Discard** saves none, **Save** saves all.

## Completion log

Rows before the final 2026-11-22 row record pre-rework code and are not evidence
for the current branch-picker or handwriting design. Automated verification for
the rework is recorded in the final row; live manual gates remain pending.

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-17 | `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck`, `assembleRelease` | PASS | Full command set BUILD SUCCESSFUL; 198 unit tests, 0 failures/errors/skipped; lint 0 errors (20 pre-existing warnings). Both unsigned and env-signed release builds succeed with R8 + resource shrinking. |
| 2026-11-17 | `connectedDebugAndroidTest` on emulator-5554 (API 36) | PASS | 35 tests, 0 failures/errors/skipped with animation scales disabled. Includes delete-all, 120-item large-list, and 3→4 migration tests. |
| 2026-11-17 | Signed release install/launch + core UI journey on clean emulator | PASS | Signed APK installed and launched with no R8 crashes; create list, add store/item, checkoff, sort, persistence across navigation, calendar, delete-all (empty lists, no onboarding), airplane-mode relaunch all exercised. Logcat showed no entered item/store text. |
| 2026-11-17 | `connectedDebugAndroidTest` on physical Samsung SM-A536U1 (`R5CT7058L2A`, Android 16/API 36) | PASS | 35 tests, 0 failures/errors/skipped. Device animation scales already 0.0. |
| 2026-11-17 | Signed release install/launch + core UI journey on physical device | PASS | Release APK launched cleanly (0 fatal logcat lines, no R8 linkage errors). Create list, add store, add typed items, checkoff, sort toggle, persistence across navigation, Settings privacy text, delete-all (empty lists, built-in stores, no onboarding), calendar localized day description, airplane-mode relaunch all exercised. No entered item/store text in logcat. |
| 2026-11-17 | Accessibility tooling probe on physical device | PARTIAL | Device accessibility tooling temporarily enabled; app launched with 0 fatal lines and first-run tutorial dismissed. Device accessibility settings restored to original values and the probe process force-stopped. Historical pre-rework result. |
| 2026-11-17 | Full handwriting/photo/organize/AI demo | NOT RUN | Manual-only gates still require a device operator. Step-by-step instructions are in `docs/release/phase-09-manual-tests.md`. See `docs/release/phase-09-verification.md`. Phase remains `in-progress`. |
| 2026-11-18 | `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck` after add-store dialog fix | PASS | 198 unit tests, 0 failures/errors/skipped; lint 0 errors (18 pre-existing warnings only); Spotless now preserves mixed CRLF/LF line endings. Dialog shows active stores plus custom-name entry. |
| 2026-11-18 | `connectedDebugAndroidTest` on emulator-5554 (API 36) after add-store dialog fix | PASS | 36 tests, 0 failures/errors/skipped. Regression test asserts the dialog lists built-in stores, selecting one adds no new store, and custom-name entry adds exactly one. Large-list check now measures sort-toggle work without Espresso root/idle overhead; `< 1_000L` threshold unchanged. |
| 2026-11-18 | `spotlessApply`, `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck` after branch-selection work (superseded by D-043) | PASS | Full command set BUILD SUCCESSFUL; 220 unit tests across 49 reports, 0 failures/errors/skipped; lint 0 errors (20 pre-existing warnings); AndroidTest APK compiles. Added branch-selection tests, Settings flow Espresso test, and Organize-context unit test. Instrumentation execution and live manual gates remain pending. Historical pre-rework result. |
| 2026-11-18 | `spotlessApply`, `testDebugUnitTest` after review fixes | PASS | 229 unit tests across 50 reports, 0 failures/errors/skipped. Added typed blocker/no-cache assertions, a no-duplicate Organize context assertion, and corrected the selected-store preference-key fake. |
| 2026-11-18 | `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `spotlessCheck` after review fixes | PASS | BUILD SUCCESSFUL; lint 0 errors (20 pre-existing warnings); AndroidTest APK compiles. |
| 2026-11-18 | `connectedDebugAndroidTest` on emulator-5554 (API 36) after review fixes | PASS | 37 tests, 0 failures/errors/skipped. Includes the Settings branch-selection flow and delete-all assertion for saved branches and settings. Historical pre-rework result. |
| 2026-11-18 | Live manual gates | NOT RUN | TalkBack/spoken output, full demo steps (including live branch-picker selection), and cached-aisle airplane-mode behavior still require a device operator. Phase remains `in-progress`. |
| 2026-11-19 | `spotlessApply`, `testDebugUnitTest`, `assembleDebug`, `lintDebug`, `spotlessCheck` after branch-selection failure fixes (superseded by D-043) | PASS | 238 unit tests across 51 reports, 0 failures/errors/skipped; lint 0 errors (19 pre-existing warnings from ML Kit metadata); build and Spotless pass. Fixed false-positive CAPTCHA block detection, blank-address place rejection, cached ordering, caller-supplied User-Agent preservation, Gson keep rules, and stale/invalid search handling. Historical pre-rework result. |
| 2026-11-19 | `connectedDebugAndroidTest` after branch-selection failure fixes | NOT RUN | No emulator or device is attached in this environment. Phase remains `in-progress`. |
| 2026-11-18 | Live manual gates | NOT RUN | Full demo steps (including live branch-picker selection) and cached-aisle airplane-mode behavior still require a device operator. Phase remains `in-progress`. |
| 2026-11-20 | `spotlessApply`, `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck`, `assembleDebugAndroidTest` after Macey's removal | PASS | 229 unit tests across 50 reports, 0 failures/errors/skipped; lint 0 errors (19 pre-existing warnings); AndroidTest APK compiles. Removed brand from seeds, UI, locator, docs, and tests; added Room v4→v5 migration plus exported `5.json`. |
| 2026-11-20 | `connectedDebugAndroidTest` on physical Samsung SM-A536U1 (Android 16/API 36) after Macey's removal | PASS | 38 tests, 0 failures/errors/skipped. Includes new v4→v5 migration test and five-store-count assertions. First run hit a latent Espresso visibility failure in `settingsTogglePersistsAcrossScreens`; fixed with `scrollTo()` before clicking the Settings AI switch. |
| 2026-11-20 | `assembleRelease` + signed release install/launch after Macey's removal | PASS | Unsigned and throwaway-key-signed release builds succeed with R8 + resource shrinking. Signed APK installed on SM-A536U1 (API 36), launched to `com.fastshoppr.app/.MainActivity`, 0 fatal logcat lines / linkage errors. Signing material lives outside the repo. |
| 2026-11-22 | Accessibility requirements restored after review | INFO | TalkBack traversal/spoken output is a manual gate again; labels and content descriptions are covered by instrumentation. See `docs/release/phase-09-manual-tests.md` Gate 3. |
| 2026-11-22 | `spotlessApply`, `compileDebugAndroidTestJava`, `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `spotlessCheck` after two-axis review fixes (superseded by D-043/D-044) | PASS | All 14 review findings fixed at that date. BUILD SUCCESSFUL; 241 unit tests across 51 reports, 0 failures/errors/skipped; lint 0 errors, 23 pre-existing warnings in untouched code/dependency metadata, none in changed files; both APKs assemble and Spotless passes. `connectedDebugAndroidTest` NOT RUN — no emulator/device attached. TalkBack, full demo, and live branch-picker gates remain manual. Historical pre-rework result; required verification pending. Phase stays `in-progress`. |
| 2026-11-22 | D-043/D-044 rework final: `spotlessApply`, `compileDebugAndroidTestJava`, `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `spotlessCheck`; `connectedDebugAndroidTest` on physical Samsung SM-A536U1 (`R5CT7058L2A`, Android 16/API 36) | PASS | Generic WebView branch picker, handwriting **Save**/review flow, and D-044 gate removal verified. 255 unit tests across 51 reports, 0 failures/errors/skipped; lint 0 errors, 21 warnings; both APKs assemble; Spotless passes. Full instrumentation suite passed twice after final hardening: 42 tests, 0 failures/errors/skipped; animation scales `0.0`. Review reload is generation-guarded and main-thread serialized; stale-load regression test added. Intermediate duplicate-row ambiguity addressed; DeskClock focus interference cleared by returning device to Home. Live manual gates remain NOT RUN. Phase stays `in-progress`. |
