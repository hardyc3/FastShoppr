# FastShoppr Agent Guide

This file is the entry point for any agent working in this repository. Read it before changing code.

## Project summary

FastShoppr is a local-first Android shopping-list app written in **Java**. A user captures a list by typing, handwriting on screen, or photographing a handwritten note. The app resolves each item to a physical store aisle, groups items by store, and sorts each store section by aisle so the user can shop in one pass.

MVP stores:

- Walmart (WebView-based lookup)
- Home Depot (HTTP + HTML parsing)

Store sections for Lowe's, Target, Smiths, and custom stores may exist in the UI, but automated lookup is added later. Manual aisle entry is always available.

## Current phase

**Phase 9 — MVP Release** is `in-progress` (see `docs/specs/phases/phase-09-mvp-release.md`). Phase 8 — Schedule & calendar is `done` and reviewed. Do not start a later phase until the current phase is marked complete and reviewed.

Do not start a later phase until the current phase is marked complete and reviewed.

## Read order

1. `AGENTS.md` (this file)
2. `docs/specs/README.md`
3. The current phase file in `docs/specs/phases/`
4. Any referenced spec documents
5. `docs/specs/12-decisions.md` before proposing an architectural change

For workstation setup and toolchain verification, see `docs/setup.md`.

## Required commands

The Gradle wrapper is committed. Run these commands from the repository root; use `gradlew.bat` on Windows.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew spotlessCheck
./gradlew connectedDebugAndroidTest   # requires a running emulator/device
```

A phase is not complete unless the relevant commands pass.

## Technology baseline

- Language: **Java 21**
- UI: XML layouts, ViewBinding, Material Components with a Material 3 theme
- Min SDK: 30
- Compile/target SDK: 36
- JDK: 21
- Build: Gradle 
- Architecture: simple MVVM + repository layer
- DI: Hilt (Java annotation processing)
- Local data: Room + SharedPreferences
- Async: LiveData, `ExecutorService`, `Handler`, `CompletableFuture`
- Background work: WorkManager + a foreground service for WebView lookups
- Navigation: Navigation Component with Fragments
- Camera: CameraX
- Handwriting: ML Kit Digital Ink Recognition (Java API)
- Image OCR: ML Kit Text Recognition v2 (Java API)
- On-device AI: ML Kit GenAI Prompt API Java package (`com.google.mlkit.genai.prompt.java`) with deterministic fallback
- HTTP: OkHttp
- HTML parsing: Jsoup
- JSON: Gson
- Location: Play Services Location
- Testing: JUnit 4, Mockito, AndroidX Test, Espresso, Room Testing (`MigrationTestHelper`)
- Package namespace: `com.fastshoppr.app`

Use the latest stable compatible versions and pin them in `gradle/libs.versions.toml`. Do not add a dependency without checking that it is already declared or documenting why it is needed.

**Do not add Kotlin app code or Jetpack Compose.** The app is Java-only. Kotlin is allowed only in Gradle build scripts (Kotlin DSL). Do not add Kotlin source files, the Kotlin Android plugin, Compose dependencies, coroutines, Flow, or DataStore.

## Repository layout

```text
AGENTS.md
docs/specs/
  README.md
  00-product.md
  01-architecture.md
  02-data-model.md
  03-store-integration.md
  04-ocr-and-input.md
  05-on-device-ai.md
  06-scheduling-dates-calendar.md
  07-background-work.md
  08-pricing-and-custom-stores.md
  09-testing-quality.md
  10-security-privacy.md
  11-future-sharing-sync.md
  12-decisions.md
  templates/phase-template.md
  phases/phase-00-feasibility.md ... phase-11-price-comparison.md
```

The Android project is created in Phase 1. Use this package layout:

```text
com.fastshoppr.app
  FastShopprApplication.java
  MainActivity.java
  di/
  data/
    local/          Room entities, DAOs, database, migrations
    remote/         Store lookup clients, parsers, WebView bridge
    repository/     Repository implementations
    preferences/    SharedPreferences-backed settings
  domain/
    model/          Pure Java models and enums
    repository/     Repository interfaces
    usecase/        Small single-purpose use cases
  ui/
    theme/          Themes, colors, dimens, styles
    navigation/     NavHostActivity, nav graph
    lists/          ListsFragment, ListsViewModel, adapters
    listdetail/     ListDetailFragment, ListDetailViewModel
    capture/        Typed, ink, camera, review screens
    lookup/         Lookup progress screen
    calendar/       Calendar screen
    settings/       Settings screen
    components/     Shared custom views and adapters
  work/
    Workers, foreground service, notifications
```

## Architecture rules

- UI -> ViewModel -> use case -> repository -> data source.
- Activities host Fragments; Fragments use ViewBinding and observe ViewModels.
- ViewModels expose immutable `UiState` through `LiveData`; one-off effects use a sealed `UiEvent` type or a single-event wrapper.
- Fragments render state and forward user actions; no business logic in Fragments or adapters.
- `domain` must not depend on Android framework classes.
- Repositories are interfaces in `domain`; implementations live in `data`.
- Hilt modules bind interfaces to implementations.
- Keep use cases small and named after the user action.
- No global mutable singletons outside Hilt.
- No blocking work on the main thread.
- Use `ExecutorService` for disk/network work and post results to the main thread.
- Do not add Kotlin app source, coroutines, Flow, Compose, or DataStore. Gradle Kotlin DSL is allowed for build scripts.

## Data invariants

These are non-negotiable:

1. `rawText` is the written or recognized item text. It is never overwritten by a store match or AI cleanup. A user may explicitly edit it.
2. `displayName` is the user-facing cleaned name. AI suggestions require user confirmation before replacing it.
3. `normalizedText` is derived for lookup only.
4. Every list has exactly one `shoppingDate`.
5. Every item belongs to exactly one list and has one `targetDate`; the item appears on the list whose date matches its target date.
6. Every item has a `storeId`. Multi-store lists group by store.
7. The matched store product is stored separately in `matchedProductName`; it never replaces `rawText`.
8. Aisle values are stored as strings and sorted with the documented comparator in `02-data-model.md`.
9. Unknown or unresolved aisles are placed last, regardless of sort direction.
10. All timestamps are stored as UTC epoch milliseconds.
11. Deletes are soft deletes: `deletedAt` marks deleted lists, items, and capture sessions; active queries exclude them; purge happens after a 30-day restore window.

## Store data access policy

- Query only public retailer pages.
- Identify the app in the HTTP user agent.
- Rate limit to at most one request per second per store.
- Cache lookup results for 60 days by default.
- Stop and surface a clear error on HTTP 403, 429, or a bot challenge.
- Do not bypass logins, CAPTCHAs, paywalls, or access controls.
- Do not perform bulk catalog scraping.
- Do not run lookups for items the user did not enter.
- Record parser assumptions and keep sanitized fixtures for tests.

## Background and AI rules

- Background organizing runs through WorkManager.
- Walmart WebView lookups run in a foreground service with a visible persistent notification.
- If notification permission is denied, queue Walmart lookups until the app is in the foreground.
- Home Depot HTTP lookups may run in normal background work.
- On-device AI inference runs only while the app is in the foreground.
- If Gemini Nano is unavailable, unsupported, or over quota, use deterministic rules and continue without blocking the user.
- Never call a paid cloud LLM.

## Security and privacy

- No backend, analytics SDK, or sharing implementation in Phases 0–11.
- No secrets, API keys, signing keys, or keystores in source control.
- Do not log raw images, full lists, addresses, or location coordinates.
- Request the minimum Android permission needed, at the moment it is needed.
- Handle permission denial without crashing or blocking core features.
- Follow `docs/specs/10-security-privacy.md`.

## Definition of Done

A phase is done only when all of the following are true:

- Every acceptance criterion in the phase file is checked.
- `assembleDebug`, unit tests, lint, and `spotlessCheck` pass.
- All required tests run and pass, including `connectedDebugAndroidTest` on the simulated device (emulator) or a physical device whenever the phase test plan requires it. A test that fails, errors, is skipped, or cannot run leaves the phase incomplete.
- New behavior has tests at the appropriate level.
- No new warnings or TODOs are left in changed code.
- No secrets or debug-only shortcuts remain.
- Relevant spec documents and the phase completion log are updated.
- A manual demo following the phase demo steps succeeds.
- The phase status is set to `done` and the next phase remains `not-started`.

## Phase workflow

1. Read `AGENTS.md`, the phase index, and the current phase file.
2. Confirm dependencies are complete.
3. Set the phase status to `in-progress`.
4. Implement only the current phase.
5. Run the required commands and fix failures.
6. Update acceptance checkboxes and the completion log.
7. Set the phase status to `done`.
8. Stop and wait for review. Do not start the next phase.

## Prohibited actions

- Starting more than one phase at a time.
- Adding Kotlin app source, Kotlin Android plugins, or Compose. Gradle Kotlin DSL is allowed for build scripts.
- Implementing deferred features such as sharing, sync, Google Sign-In, or price comparison before their phase.
- Adding a backend or cloud database.
- Adding Google Maps Popular Times or scraping Google Maps busyness data.
- Replacing `rawText` with a matched product name.
- Running AI inference in the background.
- Running WebView lookups without the foreground-service design.
- Committing secrets or generated build artifacts.
- Changing an architectural decision without adding an entry to `12-decisions.md`.

## Updating this file

If a phase establishes a new command, dependency, or convention, update this file in the same change. Keep it short and actionable.
