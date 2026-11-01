# Testing and Quality Specification

## Principles

- Test behavior at the lowest level that can catch the bug.
- Keep tests deterministic; no live retailer calls in CI.
- Use fakes for fragile integrations (WebView, HTTP, ML Kit, Gemini Nano).
- Keep sanitized fixtures for parsers.
- A phase is not done until its required tests pass.

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
- Price comparison rules (post-MVP).
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
- Location provider

Fakes must be deterministic and configurable per test.

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

Optional but recommended:

```bash
./gradlew connectedDebugAndroidTest
./gradlew dependencyUpdates
```

Rules:

- No new lint warnings.
- No unused resources or imports.
- No TODO/FIXME comments in changed code.
- Java formatting enforced by Spotless/Checkstyle.
- No Kotlin app source, Kotlin Android plugins, or Compose dependencies. Gradle Kotlin DSL is allowed for build scripts.

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
- Color contrast meets WCAG AA where practical.
- The app supports dark theme.
- TalkBack can traverse lists, store sections, and the calendar.
- Error messages are announced and actionable.
- Do not rely on color alone for status.

## Release checks

- R8 enabled with keep rules for Room, Hilt, Gson models, and ML Kit.
- No debug logging of item text, images, or location.
- No secrets or signing files committed.
- Version code and name set.
- Privacy policy link or in-app text present.
- Backup rules reviewed; sensitive local data excluded if appropriate.
- Install and launch a release build on a clean device/emulator.

## Definition of Done for tests

A phase is complete only when:

- New behavior has unit tests.
- DAO changes have Room tests.
- Critical UI changes have Espresso or Robolectric coverage.
- Permission, notification, or background changes have instrumentation or manual verification.
- All required commands pass.
- Manual demo steps succeed.
- Results are recorded in the phase completion log.

## CI expectations

- Build, unit tests, lint, and Spotless run on every change.
- Instrumentation tests run on a scheduled job or locally when a device is available.
- Live retailer and live AI tests are excluded from CI.
- CI must fail on lint errors and failing tests.
