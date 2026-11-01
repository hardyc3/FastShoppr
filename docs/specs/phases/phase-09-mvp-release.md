# Phase 9 — MVP Release

- **Status:** `not-started`
- **Depends on:** Phase 8
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-10-31

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

- Backup rules only.
- No schema change unless a release blocker requires one; if so, add a migration and test.

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

## Manual demo

1. Install the release build on a clean device.
2. Create a list, add typed and handwritten items, review, and save.
3. Organize with Walmart and Home Depot.
4. Confirm aisle grouping and sort toggle.
5. Defer an item to next week and check the calendar.
6. Enable TalkBack and complete a checkoff.
7. Turn on airplane mode and confirm cached data works.
8. Delete all data and confirm the app returns to onboarding/empty state.

## Risks

- R8 stripping required classes. Mitigation: keep rules and release smoke test.
- Accessibility regressions. Mitigation: checklist and TalkBack pass.
- Backup leaking sensitive data. Mitigation: explicit backup rules.
- Performance regressions from large lists. Mitigation: profiler and indexed queries.

## Open questions

- Should the release include a privacy policy URL or only in-app text?
- What is the target Play Store listing date?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
