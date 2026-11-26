# Testing and Quality Specification

## Principles

- Test behavior at the lowest level that can catch the bug.
- Keep tests deterministic; no live retailer calls in CI.
- Use fakes for fragile integrations (WebView, HTTP, ML Kit, Gemini Nano).
- Keep sanitized fixtures for parsers.
- A phase is not done until its required tests pass.
- A phase cannot be marked complete if any required test fails, errors, is skipped, or does not run. This includes `connectedDebugAndroidTest` on the simulated device (emulator) or a physical device. An unavailable device is not a pass.

## Test levels

| Level | Tooling | What it covers |
|---|---|---|
| Unit | JUnit 4, Mockito, Truth/AssertJ | Domain rules, comparators, parsers, use cases, ViewModels |
| Room | AndroidX Room testing, in-memory DB | DAOs, migrations, transactions |
| UI | Espresso, FragmentScenario, Robolectric | Screens, adapters, permissions, navigation |
| Worker | WorkManager test helpers | Scheduling, retries, constraints |
| Instrumentation | AndroidX Test, Espresso | Critical flows on device/emulator |
| Manual | Device | ML Kit, WebView, Gemini Nano, notifications |

## Unit tests

Must cover:

- Aisle comparator, including `03`, `A12`, `EC2`, unknown, and descending order.
- Item normalization and alternate query generation.
- Date phrase parsing and nearest-list selection.
- Overdue prompt options.
- Schedule interval advancement.
- Review state: add, edit, delete, merge, split, select all.
- Error mapping from HTTP/parser/AI failures.

Rules:

- Use a fixed `Clock` for time-dependent tests.
- Do not use `Thread.sleep`.
- Use `ExecutorService` fakes or direct executors in tests.
- Keep tests independent and fast.

## ViewModel tests

- Observe `LiveData` with `InstantTaskExecutorRule`.
- Assert state transitions and emitted events.
- Use fake repositories.
- Test loading, empty, content, and error states.
- Test that `rawText` is never replaced by match or AI results.

## Room tests

- Use an in-memory database.
- Test unique shopping dates.
- Test transactional item moves between lists.
- Test cascade deletes for lists, items, and capture sessions.
- Test indexes where query behavior matters.
- Test every migration with `MigrationTestHelper`.

## UI tests

- Use Espresso for critical flows:
  - Create a list.
  - Add typed items under store sections.
  - Review OCR lines.
  - Enter a manual aisle.
  - Toggle sort direction.
  - Defer an item.
  - Open the calendar.
  - Save handwriting with the writing screen **Save** button and open review.
  - Review **Add line**, **Discard**, and **Save** navigation.
- Use `FragmentScenario` for isolated screen tests.
- Use Robolectric for permission-denied and date/time edge cases where practical.
- Do not assert on implementation details.

## Integration fakes

Provide fakes for:

- `StoreLookupService`
- `HandwritingRecognizer`
- `ImageTextRecognizer`
- `ItemNameCleaner`
- `DatePhraseParser`
- Notification publisher
- Branch picker WebView delegate

Fakes must be deterministic and configurable per test.

## Phase 9 branch picker and handwriting flows

- Branch picker tests use a fake WebView delegate. No real locator request,
  HTTP parsing, ZIP, location permission, or coordinate state is involved.
- Cover one saved branch per brand, first-use prompt, Settings change, and
  delete-all clearing.
- Handwriting tests assert writing **Save** opens review, review **Add line**
  returns to writing with pending changes preserved, **Discard** saves none,
  and **Save** saves all.

## Phase 10 store integration coverage

- Automated aisle lookup tests use sanitized fixtures for Home Depot, Lowe's,
  and Smiths: search link, aisle present, aisle absent, and blocked page.
- Search-first service tests assert the first query-matching same-retailer
  product link is followed, off-host and unrelated carousel links are ignored,
  two requests per lookup respect the rate limit, and HTTP 403/429 or challenge
  markers stop the run.
- Cache tests assert brand-scoped keys, 60-day expiry, and one request per second
  per store.
- Device manual tests cover branch selection → item entry → organize → aisle
  result → sort for Home Depot, Lowe's, and Smiths, plus a Walmart regression
  smoke. Target was removed under D-056. Results are recorded per brand with
  device and date.
- Custom-store tests assert manual-only aisle entry; no automated lookup path is
  exercised.

## Fixtures

- Store fixtures live in `app/src/test/resources/fixtures/`.
- Sanitize personal data, cookies, and tokens.
- Document capture date, URL shape, store context, and expected output.
- Update fixtures when parsers change.
- Never delete a failing fixture to make a test pass.

## Live and manual tests

- Live retailer tests are opt-in via an environment flag and never run in CI.
- Manual device tests are required for:
  - Digital Ink model download and recognition.
  - Camera/gallery OCR.
  - Gemini Nano availability and fallback.
  - Walmart WebView extraction.
  - WebView branch picker navigation and branch confirmation.
  - Handwriting **Save** → review **Add line**/**Discard**/**Save** flow.
  - Foreground service notification.
  - Background organizing completion notification.
- Record manual test results in the phase completion log.

## Static checks

Required:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew spotlessCheck
```

Required when the phase changes Room, permissions, notifications, or critical UI flows, or when the phase test plan includes instrumentation:

```bash
./gradlew connectedDebugAndroidTest
```

Optional but recommended:

```bash
./gradlew dependencyUpdates
```

Rules:

- No new lint warnings.
- No unused resources or imports.
- No TODO/FIXME comments in changed code.
- Java formatting enforced by Spotless/Checkstyle.
- No Kotlin app source, Kotlin Android plugins, or Compose dependencies. Gradle Kotlin DSL is allowed for build scripts.
- A test that fails, errors, is skipped, or cannot run is not a pass. Record the gap in the completion log and leave the phase incomplete.
- `connectedDebugAndroidTest` must actually execute on the simulated device (emulator) or a physical device; a compile-only run or a missing device does not satisfy the requirement.
- Do not delete, ignore, or weaken a test to make a phase pass.

## Performance

- A 100-item list scrolls smoothly on a mid-range device.
- Sorting a 100-item list completes in under one second.
- Database queries used by list detail are indexed.
- Images are downscaled before OCR.
- WebView instances are destroyed after use.
- Background work does not wake the device unnecessarily.
- Avoid holding large bitmaps in memory.

## Accessibility

- Every interactive control has a content description or label.
- Touch targets are at least 48dp.
- Text scales with system font size.
- TalkBack can traverse lists, store sections, and the calendar.
- Color contrast meets WCAG AA where practical.
- The app supports dark theme.
- Error messages are announced and actionable.
- Do not rely on color alone for status.

## Release checks

- R8 enabled with keep rules for Room, Hilt, Gson models, and ML Kit.
- No debug logging of item text or images.
- No secrets or signing files committed.
- Version code and name set.
- Privacy policy link or in-app text present.
- Backup rules reviewed; sensitive local data excluded if appropriate.
- Install and launch a release build on a clean device/emulator.

## Definition of Done for tests

A phase is complete only when:

- New behavior has unit tests.
- DAO changes have Room tests.
- Critical UI changes have Espresso or Robolectric coverage, including the
  branch picker and handwriting **Save**/review flows.
- Permission, notification, or background changes have instrumentation or manual verification.
- All required commands and tests pass, including `connectedDebugAndroidTest` on the simulated device (emulator) where required.
- No test in the phase test plan is failing, errored, skipped, or unrun.
- Manual demo steps succeed.
- Results are recorded in the phase completion log.

## CI expectations

- Build, unit tests, lint, and Spotless run on every change.
- Instrumentation tests run on a scheduled job with a simulated device (emulator) or locally on a physical device.
- Live retailer and live AI tests are excluded from CI.
- CI must fail on lint errors and on any failing, errored, skipped, or unrun required test.
