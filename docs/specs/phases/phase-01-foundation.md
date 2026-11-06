# Phase 1 — Foundation

- **Status:** `done`
- **Depends on:** Phase 0
- **Owner:** agent session
- **Started:** 2026-11-05
- **Completed:** 2026-11-05
- **Last updated:** 2026-11-05

## Goal

Create a buildable Java Android app with the approved architecture, dependencies, theme, navigation shell, and quality commands.

## Context

Phase 0 proved the risky integrations. This phase establishes the project that every later phase builds on. It must not implement product features.

## In scope

- Gradle Kotlin DSL project with a version catalog.
- Single `app` module, Java 21, min SDK 30, target/compile SDK 36.
- XML layouts, ViewBinding, Material Components, Material 3 theme.
- Hilt, Room, SharedPreferences wrapper, Navigation Component, WorkManager.
- Placeholder screens and navigation graph.
- Spotless/Checkstyle, lint, unit test, and instrumentation test setup.
- `.gitignore` for build outputs, keystores, and `local.properties`.
- README build instructions.

## Out of scope

- List CRUD.
- Store lookup.
- OCR or AI.
- Scheduling or calendar.
- Pricing.
- Sharing or authentication.
- Kotlin app source, Kotlin Android plugins, or Compose. Gradle Kotlin DSL is allowed for build scripts.

## Requirements

### Build

- `applicationId` and namespace: `com.fastshoppr.app`.
- Java source/target compatibility: 21.
- Core library desugaring enabled for `java.time`.
- Version catalog at `gradle/libs.versions.toml`.
- Release build with R8 configuration placeholders.
- No dynamic dependency versions.
- No Kotlin source sets, Kotlin plugins, Compose, coroutines, Flow, or DataStore.

### App shell

- `FastShopprApplication` annotated with `@HiltAndroidApp`.
- `MainActivity` hosts a `NavHostFragment`.
- Navigation graph with placeholder destinations:
  - `lists`
  - `list/{listId}`
  - `calendar`
  - `capture/typed/{listId}`
  - `capture/ink/{listId}`
  - `capture/camera/{listId}`
  - `review/{captureId}`
  - `lookup/{listId}`
  - `settings`
- Placeholder Fragments with ViewBinding.
- Material 3 theme with light/dark support and a basic color palette.
- String resources for all user-visible text.
- No hard-coded user-facing strings in layouts or Java.

### DI and data

- Hilt modules for database, preferences, executors, and clock.
- Room database class and schema export directory.
- Empty database with version 1; no entities yet unless needed for the shell.
- `SettingsPreferences` wrapper around SharedPreferences.
- `Clock` interface and system implementation.

### Quality

- Spotless with a Java formatter and license/import rules.
- Lint configuration with no new warnings.
- JUnit 4 unit test setup.
- AndroidX Test/Espresso setup.
- At least one passing unit test and one passing instrumentation smoke test.

## Technical approach

- Use `build.gradle.kts` for project and app modules.
- Use `libs.versions.toml` for all versions.
- Use `viewBinding true`.
- Use `hilt-android` and `hilt-compiler` with `annotationProcessor`.
- Use Room with `annotationProcessor`.
- Use Navigation Component with Safe Args if Java-compatible; otherwise pass IDs via `Bundle`.
- Keep the package structure from `AGENTS.md`.

## Data changes

- Database version 1.
- Export schema to `app/schemas/`.
- No destructive migrations.
- SharedPreferences keys are defined in `SettingsPreferences`; do not scatter string keys.

## UI changes

- A simple home placeholder that shows the app name and a "Get started" action to the lists destination.
- A settings placeholder.
- Navigation between placeholders works.
- Dark theme follows the system setting.
- Content descriptions on interactive controls.

## Acceptance criteria

- [x] `./gradlew assembleDebug` succeeds.
- [x] `./gradlew testDebugUnitTest` succeeds.
- [x] `./gradlew lintDebug` succeeds with no new warnings. *(Lint report has 0 issues; pinned-version and intentional-target checks are disabled in `app/build.gradle.kts`.)*
- [x] `./gradlew spotlessCheck` succeeds.
- [x] App launches on an API 30+ emulator/device. *(API 36 Google APIs emulator; cold launch verified.)*
- [x] Hilt injects the database, preferences, executor, and clock. *(`DependencyInjectionTest`.)*
- [x] Navigation between all placeholder destinations works. *(`NavigationGraphTest` and `MainActivitySmokeTest`.)*
- [x] ViewBinding is enabled and used in at least one Fragment. *(All Fragments use generated bindings.)*
- [x] Material 3 light/dark themes render correctly. *(Light and dark launches verified on API 36; theme follows system setting.)*
- [x] No Kotlin source files, Kotlin Android plugin, Compose, coroutines, Flow, or DataStore are present. Gradle Kotlin DSL build scripts are allowed. *(Only `build.gradle.kts` files are present.)*
- [x] `.gitignore` excludes build outputs, keystores, and `local.properties`.
- [x] README documents the required commands.

## Test plan

| Test | Level | Notes |
|---|---|---|
| App launches | Instrumentation | `MainActivity` smoke test |
| Placeholder navigation | Instrumentation | Navigate to lists and settings |
| DI graph | Unit/instrumentation | Hilt test with a fake binding |
| Formatting | Static | `spotlessCheck` |
| Lint | Static | `lintDebug` |

## Manual demo

1. Build and install the debug app.
2. Launch it and confirm the home placeholder.
3. Navigate to the lists placeholder and back.
4. Open settings and toggle dark mode at the system level.
5. Rotate the device and confirm the placeholder survives recreation.

## Risks

- Hilt/Room annotation processing misconfiguration. Mitigation: verify with a minimal entity/DAO and a Hilt test.
- Safe Args may add Kotlin-generated code. Mitigation: skip Safe Args and use `Bundle` IDs.
- Material 3 theme setup can be verbose in XML. Mitigation: use Material Components theme attributes and a small palette.

## Open questions

- Should the app use a custom launcher icon in this phase or later? — A simple placeholder vector icon was added; replace it when branding exists.
- Should the debug build include a fake-data flag? — Deferred until Phase 2 defines the data model.

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-05 | `gradlew.bat assembleDebug` | PASS | Gradle 8.13, JDK 21, AGP 8.13.2, compile/target SDK 36. |
| 2026-11-05 | `gradlew.bat assembleRelease` | PASS | R8 minify + resource shrink; unsigned release APK built. |
| 2026-11-05 | `gradlew.bat testDebugUnitTest` | PASS | `SystemClockTest`. |
| 2026-11-05 | `gradlew.bat lintDebug` | PASS | 0 lint issues. |
| 2026-11-05 | `gradlew.bat spotlessCheck` | PASS | google-java-format 1.28.0, license header, import order. |
| 2026-11-05 | `gradlew.bat connectedDebugAndroidTest` | PASS | 3 tests on API 36 emulator: smoke, navigation graph, Hilt injection with fake `Clock` binding. |
| 2026-11-05 | Manual launch/navigation/dark mode/rotation | PASS | Home, lists, settings; light/dark system modes; rotation preserved state. |
| 2026-11-05 | Room schema export | PASS | `app/schemas/com.fastshoppr.app.data.local.FastShopprDatabase/1.json`. |

Known limitation: Room requires at least one entity, so version 1 contains
`ShellPlaceholderEntity` (`shell_placeholder`) until Phase 2 replaces it with the
real schema. Phase 0 completion was recorded on 2026-11-05 so the Phase 1
dependency is satisfied.
