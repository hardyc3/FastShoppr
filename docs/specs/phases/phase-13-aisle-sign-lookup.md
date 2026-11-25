# Phase 13 — Aisle Sign Lookup

- **Status:** `not-started`
- **Depends on:** 12 (sequence only; no feature dependency on Phases 10–12)
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-25

## Goal

Let the user photograph in-store aisle signs, confirm OCR results, and use those store-specific signs to resolve item aisles on-device. On-device AI may suggest an aisle only from confirmed sign text; deterministic matching is always available.

## Context

Today aisle resolution uses manual entry, a 60-day `AisleCacheEntity`, and retailer lookups (Walmart WebView, Home Depot HTTP). Many stores print an aisle number with short category lists on physical aisle signs. Capturing those signs once per store gives a local, user-verified source of aisle knowledge.

This phase follows Phase 12 by numbering but does **not** depend on Phase 10 brand coverage, Phase 11 pricing work, or Phase 12 price comparison. It must not be started until explicitly instructed.

Spec precedence for this phase:

- This file supersedes the `05-on-device-ai.md:24-31` prohibition on generating store aisles **only** for bounded inference over user-confirmed `AisleSign` rows. It does not permit free-form product-location invention or background AI. `05-on-device-ai.md` is intentionally left unchanged.
- This file amends `10-security-privacy.md` with aisle-sign text, camera purpose, and retention rules; those edits are recorded in D-050 in `12-decisions.md`.
- Existing invariants remain: `rawText` is never overwritten, manual aisle/bay always wins, unknown aisles sort last, all timestamps are UTC epoch milliseconds, active queries exclude soft deletes, and soft-deleted data remains restorable for 30 days before purge.

## In scope

- New store-management UI: Settings → Stores → "Aisle signs" list, camera capture, OCR, user review/edit, save, edit, and delete.
- One photo may yield multiple confirmed sign entries; each entry is stored separately for the selected `storeId`.
- OCR uses existing on-device ML Kit Text Recognition v2. Temporary JPEG deleted after confirm/discard; original image not retained.
- New `AisleSignEntity`, DAO, repository, Hilt bindings, Room migration 6→7, and exported schema 7.
- `StoreEntity.deletedAt` with 30-day restore window; restoring a store restores its signs. Existing `isArchived` flag stays.
- Lookup precedence: manual → sign corpus → `AisleCacheEntity` → retailer website. A store with at least one active sign skips retailer lookup for sign-resolved items.
- Deterministic token-overlap matcher over saved sign category text.
- Bounded on-device GenAI Prompt API matcher, opt-in and foreground-only, with validation and deterministic fallback.
- New settings: `sign_lookup_enabled` (default `true`) and `aisle_sign_ai_enabled` (default `false`).
- Provenance via new `LookupSource.AISLE_SIGN_AI` and `AISLE_SIGN_RULE`; sign id and confidence recorded in existing `matchCandidatesJson`. No new `ListItemEntity` column.
- "Delete all data" clears aisle signs, related temp images, and the new preference keys.
- Unit, Room, migration, UI, and connected tests plus a manual demo.

## Out of scope

- No retailer website, WebView, parser, rate-limit, or cache behavior changes beyond lookup ordering.
- No Phase 10 brand coverage, Phase 11 pricing spike, or Phase 12 price comparison work.
- No global category-to-aisle map, free-form AI product-location invention, cloud LLM, or paid API.
- No geofence, location permission, ZIP, or coordinate use.
- No image gallery, image history, thumbnail retention, or server upload for sign photos.
- No AI use in background work; no new foreground service.
- No changes to `ListItem.rawText`, `displayName` cleanup prompts, or AI item-name cleanup settings behavior.
- No new Gradle dependency, Kotlin app source, or Compose.
- No new build command or signing change.

## Requirements

### Functional

- User opens a store's "Aisle signs" screen, grants `CAMERA` at point of use, photographs one or more signs, and sees OCR results as editable rows.
- Each editable row requires an aisle string. Bay is optional. Raw OCR text is preserved and never overwritten by normalization or AI output.
- If OCR detects no aisle number, the user must type one before the row can be saved.
- User may edit, delete, or save each detected sign. Saving creates one `AisleSign` row per confirmed sign.
- Re-photographing the same `storeId` + aisle soft-deletes the prior row and creates the new row. Old version is retained 30 days and is not user-restorable.
- Deleting a sign soft-deletes it for 30 days. There is no separate sign-restore UI; store restore within 30 days restores its soft-deleted signs.
- Soft-deleting a store sets `deletedAt` on the store and all its signs. Restore within 30 days clears `deletedAt` on both. Purge after 30 days removes both.
- Item lookup order: manual location wins immediately; otherwise active signs for the item's `storeId` are consulted; if no confident sign match exists, existing cache is consulted; only then retailer lookup runs.
- A store with an active sign corpus never triggers retailer website lookup for an item resolved by that corpus, even when the sign matcher returns `unknown`.
- AI inference runs only when `aisle_sign_ai_enabled` is on, the app is in the foreground, the model reports `Available`, and the matcher has not been cancelled. Otherwise deterministic matching runs.
- Confident match threshold is `>= 0.6`, matching current repository convention. Below threshold → `unknown` aisle, sorted last; no `NEEDS_REVIEW` status from the sign path.
- Manual aisle/bay entry always wins and is never overwritten by sign lookup.
- `sign_lookup_enabled = false` disables sign-based resolution; manual, cache, and website behavior returns to current behavior.

### Non-functional

- Sign photos and OCR prompts are never logged. Only counts, brand, status, duration, and error category are logged, per `10-security-privacy.md`.
- AI prompt input capped at 20 signs and 200 characters of category text per sign.
- AI output must be JSON with `aisle`, `confidence`, and `signId`. An aisle value not present in the supplied sign list is invalid → deterministic fallback.
- AI session opens on demand, closes on completion or screen stop, and drops results after cancellation.
- All OCR, matching, and database work runs off the main thread; results post to the main thread.
- Accessibility: camera rationale, editable sign rows, save/delete actions, and error states need TalkBack labels and content descriptions. Permission denial must not block manual aisle entry.

## Technical approach

Create or change these areas:

- `domain/model/AisleSign.java` — pure Java model.
- `domain/model/AisleSignMatch.java` — `Matched(signId, aisle, bay, confidence)` / `NoMatch` / `Unavailable`.
- `domain/repository/AisleSignRepository.java` — domain-owned CRUD/restore interface.
- `domain/ai/AisleSignMatcher.java` — domain-owned AI port; no Android imports.
- `domain/usecase/` — `CaptureAisleSignsUseCase`, `SaveAisleSignsUseCase`, `ListAisleSignsUseCase`, `DeleteAisleSignUseCase`, `MatchItemToAisleSignUseCase`.
- `data/local/entity/AisleSignEntity.java`, `data/local/dao/AisleSignDao.java`, `data/repository/AisleSignRepositoryImpl.java`.
- `data/remote/sign/AisleSignTextParser.java` — converts ML Kit recognized lines into candidate aisle + category rows; no network.
- `data/remote/ai/GeminiNanoAisleSignMatcher.java`, `AisleSignPromptBuilder.java`, `AisleSignOutputValidator.java` — reuse existing GenAI availability/timeout/cancellation patterns from `GeminiNanoItemNameCleaner`.
- `domain/ai/DeterministicAisleSignMatcher.java` — token overlap over normalized sign text; tie → `NoMatch`.
- `ui/settings/AisleSignsFragment.java`, `AisleSignsViewModel.java`, `AisleSignCaptureFragment.java`, `AisleSignReviewAdapter` plus XML layouts and strings.
- `data/preferences/SettingsPreferences.java` — two new keys and defaults.
- `di/` — bind sign repository and matcher implementations.
- `data/repository/AisleLookupRepositoryImpl.java` — insert sign branch after manual short-circuit and before cache lookup; stamp new `LookupSource` values and provenance JSON.
- `data/repository/UserDataRepositoryImpl.java` and `data/local/dao/UserDataDao.java` — clear signs on "Delete all data".
- `data/local/Migrations.java` and `FastShopprDatabase.java` — migration 6→7 and schema export.

Threading: CameraX image capture → executor OCR → main-thread editable state; save on executor; matching on executor. AI inference is foreground-only and cancellable; background organization uses deterministic matching only.

No dependency additions. No Kotlin app source. No Compose.

## Data changes

- Add table `aisle_signs`:
  - `id TEXT PRIMARY KEY`
  - `storeId TEXT NOT NULL` with FK to `stores(id)` on `RESTRICT`
  - `rawOcrText TEXT NOT NULL`
  - `normalizedTokensJson TEXT NOT NULL`
  - `aisle TEXT NOT NULL`
  - `bay TEXT` nullable
  - `confidence REAL NOT NULL`
  - `createdAt INTEGER NOT NULL`, `updatedAt INTEGER NOT NULL`, `deletedAt INTEGER` nullable — all UTC epoch ms.
  - Index `(storeId, deletedAt, aisle)`.
- Add `stores.deletedAt INTEGER` nullable; keep `isArchived`. Active store queries filter both `isArchived = 0` and `deletedAt IS NULL` where applicable.
- Room version 6 → 7 via `MIGRATION_6_7`; export schema under `app/schemas/`. Add `MigrationTest` covering 6→7, null `deletedAt` backfill, and sign table creation.
- No `ListItemEntity` schema change. Sign id and confidence go into existing `matchCandidatesJson`.
- Preferences: `sign_lookup_enabled` default `true`; `aisle_sign_ai_enabled` default `false`.
- Purge job extends existing soft-delete cleanup: `aisle_signs.deletedAt < now - 30 days` and `stores.deletedAt < now - 30 days` are hard-deleted; store purge also hard-deletes its signs.
- Delete-all-data clears `aisle_signs`, temp sign images under `cache/captures`, and both new preference keys, then reseeds built-in stores.

## UI changes

- Settings → Stores gains "Aisle signs" per store. No new top-level navigation destination.
- Aisle signs screen states: empty ("No aisle signs captured"), loading, permission rationale, permission denied (deep link to settings; manual aisle still available), OCR failure, list, edit, delete confirmation.
- Capture screen uses existing CameraX back-camera/minimum-latency pattern. Gallery import is not part of this phase.
- Review screen shows one editable row per detected sign: aisle (required), bay (optional), raw OCR text (read-only), category text (editable), save/discard.
- Sign list sorted with the documented aisle comparator; unknown aisles last.
- Add content descriptions and focus order for camera button, rows, edit/delete actions, and error banners.

## Acceptance criteria

- [ ] User can capture a sign photo from Settings → Stores → "Aisle signs" and review OCR rows.
- [ ] Multiple signs in one photo become separate editable rows; no aisle detected blocks save until user types one.
- [ ] Saved sign rows are linked to the selected `storeId`; original JPEG is deleted after save/discard.
- [ ] Store with active signs resolves items via sign corpus and does not call retailer website lookup.
- [ ] Lookup precedence verified: manual → sign → cache → website.
- [ ] Deterministic matcher runs when AI unavailable/disabled/backgrounded; tie or sub-0.6 confidence yields `unknown` sorted last.
- [ ] AI matcher is opt-in, foreground-only, bounded to supplied signs, validates JSON, rejects invented aisles, and closes/cancels cleanly.
- [ ] New `LookupSource` values and provenance JSON appear on matched items; `rawText` unchanged.
- [ ] Store soft-delete hides store and signs; restore within 30 days restores both; purge after 30 days removes both.
- [ ] Sign-only soft delete has no restore UI and purges after 30 days; re-capture supersedes prior row.
- [ ] "Delete all data" clears signs and new preferences.
- [ ] Migration 6→7 and exported schema 7 pass Room migration tests.
- [ ] New UI states and permission denial handled without blocking manual aisle entry.
- [ ] Every test in the test plan has run and passed, including `connectedDebugAndroidTest` on the simulated device (emulator); no test has failed, errored, or been skipped.

## Test plan

| Test | Level | Notes |
|---|---|---|
| `AisleSignTextParserTest` | Unit | Sanitized sign fixtures: standard aisle, multiple signs, no aisle, abbreviation, noisy category list. |
| `DeterministicAisleSignMatcherTest` | Unit | Token overlap, tie → `NoMatch`, >20 signs cap, empty corpus. |
| `AisleSignPromptBuilderTest` / `AisleSignOutputValidatorTest` | Unit | 20-sign cap, 200-char cap, JSON validation, invented aisle rejected, malformed JSON → fallback. |
| `GeminiNanoAisleSignMatcherTest` | Unit | Fake availability/quota/busy/cancel; closes session; no background invocation. |
| `AisleSignRepositoryImplTest` | Room | CRUD, active query excludes `deletedAt`, store restore restores signs, 30-day purge. |
| `MIGRATION_6_7Test` | Room | Uses `MigrationTestHelper` and exported schema 7; null backfill and table creation. |
| `AisleLookupRepositorySignTest` | Unit | Precedence, no website call with signs, low confidence → `unknown`, no cache write for sign results, manual short-circuit. |
| `SettingsPreferencesAisleSignTest` | Unit | Toggle defaults `true` / `false`; persistence. |
| `UserDataRepositoryAisleSignTest` | Room | Delete-all-data clears signs and prefs; built-in stores reseeded. |
| `AisleSignsFlowTest` | Espresso/Robolectric | Capture → edit → save → list → delete; permission denied path; TalkBack labels present. |
| `AisleSignConnectedFlowTest` | Instrumentation | Camera + OCR + Room on emulator; no image left in `cache/captures`. |
| Manual demo | Manual | Steps below; result recorded in completion log. |

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored, been skipped, or failed to run. That includes every test in the test plan and `./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator) or a physical device. An unavailable device is not a pass: leave the phase `in-progress` or `blocked` and record the unrun test in the completion log.

## Manual demo

1. Open Settings → Stores → Walmart → Aisle signs. Deny camera permission; confirm manual aisle entry still works and rationale/deep-link appears.
2. Grant camera permission, photograph a sign showing an aisle number and category words, then confirm multiple editable rows appear.
3. Edit one row's aisle, leave another blank and confirm save is blocked until an aisle is typed. Save valid rows.
4. Add an item matching a saved sign with AI off; confirm aisle assigned, `LookupSource.AISLE_SIGN_RULE`, no website lookup visible, and `rawText` unchanged.
5. Enable AI aisle inference, add a different matching item; confirm `LookupSource.AISLE_SIGN_AI`, prompt bounded to saved signs, and output validated.
6. Add an item with no sign match; confirm `unknown` aisle sorted last and no website lookup for that store.
7. Soft-delete the store, confirm signs hidden, restore within 30 days, confirm signs return.
8. Run "Delete all data"; confirm signs and toggles reset.

## Risks

- In-store sign formats vary → mandatory user review/edit; sanitized fixture set expanded as formats appear.
- AI could invent aisles → prompt restricted to supplied signs; validator rejects out-of-list values; deterministic fallback.
- Store archive semantics change → migration + restore/purge tests; `isArchived` retained for existing UI.
- 30-day purge depends on existing cleanup scheduling → extend and test the same job; no separate scheduler.
- No photo retained → disputing an OCR result means re-photographing; accepted tradeoff for privacy.
- Historical note (superseded 2026-11-25): Phase 9A connected tests were once blocked by no device; they now pass 49/49 on `emulator-5554`. Phase 13 remains `not-started` until explicitly instructed.

## Open questions

- None. Product decisions settled 2026-11-25; implementation details follow existing Hilt, Room, OCR, and GenAI patterns.

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
