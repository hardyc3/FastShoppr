# Phase 12 — Aisle Cache & Correction

- **Status:** `not-started`
- **Depends on:** 11 (sequence only; sign results write cache in this phase)
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-26

## Goal

Reuse resolved aisle data so an item entered again is not looked up from a retailer for 90 days, and let the user mark a reported aisle as wrong without the app silently reporting that same wrong result again.

## Context

Aisle resolution currently prefers manual entry, then a 60-day `AisleCacheEntity`, then retailer websites. Successful lookups are cached by `storeId + normalizedQuery`, but manual aisle entry never writes cache, and there is no way to tell the app that a cached or freshly reported aisle is wrong. A wrong aisle can therefore be reused from cache or re-reported by the same first-match retailer result.

This phase follows Phase 11 by numbering. It has no feature dependency on Phase 10 work. It must not be started until Phase 11 is reviewed and `done` and the user explicitly instructs implementation.

Spec precedence for this phase:

- This file supersedes the 60-day TTL in `02-data-model.md`, `03-store-integration.md`, and D-051. D-058 records the change to 90 days.
- This file supersedes `docs/specs/phases/phase-11-aisle-sign-lookup.md:159`, which says sign results do not write cache. In Phase 12, sign results write cache like other automatic results and are also subject to rejected-result suppression.
- This file does not change Phase 11's lookup order. It inserts rejected-result gating after the manual short-circuit and before sign, cache, and website results.
- Existing invariants remain: `rawText` is never overwritten, manual aisle/bay always wins, unknown aisles sort last, all timestamps are UTC epoch milliseconds, active queries exclude soft deletes, and soft-deleted data remains restorable for 30 days before purge.
- No pricing work is included. Former pricing phases were removed by D-057.
- Completed phase files that still state 60 days are historical; D-058 supersedes those statements.

## In scope

- Change the single cache TTL constant from 60 days to 90 days. Keep expired entries usable offline with the existing stale indicator.
- Write `AisleCacheEntity` rows for all successful sources: Walmart WebView, shared search-first HTTP, Phase 11 aisle-sign results, and manual aisle entry from every existing manual path.
- Treat manual aisle rows as authoritative; automatic and sign lookups must not overwrite them until the user invalidates them or the TTL expires.
- New `RejectedAisleResultEntity`, DAO, repository, Hilt binding, and migration 7→8 with exported schema 8.
- Reuse the existing inline **Not right?** action. Candidate picker gains a trailing “Mark aisle incorrect” action; the existing Set aisle dialog gains a confirmation step and an optional correct-aisle field.
- Marking an aisle incorrect invalidates the shared cache row for `storeId + normalizedQuery`, stores a rejected-result fingerprint, and optionally writes a `MANUAL` cache row from the user's correction.
- Rejected fingerprints gate sign, cache, and website results on every lookup, including force refresh. Manual aisle entry is never gated.
- Rejection rows expire 90 days after rejection, soft-delete with their store, restore within the 30-day store window, and are cleared by “Delete all data”.
- Unit, Room, migration, UI, connected tests, and a manual demo.

## Out of scope

- No new retailer, parser, rate-limit, WebView, or bot-challenge behavior.
- No product-picker UI, bulk scraping, login, CAPTCHA, or access-control bypass.
- No category-to-aisle map and no free-form AI product-location inference.
- No new settings screen or user-configurable TTL; the 90-day value is a fixed constant.
- No schema change to `AisleCacheEntity` or `ListItemEntity` beyond the new rejection table and TTL constant.
- No new `LookupSource`, `LookupStatus`, Gradle dependency, Kotlin app source, Compose, build command, or signing change.
- No changes to capture, review, or lookup-progress screens.
- No pricing feature, price cache, or price comparison.

## Requirements

### Functional

- A successful lookup for `(storeId, normalizedQuery)` writes or refreshes one cache row with `fetchedAt` and `expiresAt = fetchedAt + 90 days`.
- Every successful write resets the 90-day clock: WebView, HTTP, sign, and manual.
- Cache lookup uses `storeId + normalizedQuery`. Existing brand/source scoping continues to apply.
- Later edits to `displayName`, `normalizedText`, or store do not rekey old cache rows. The next lookup uses the new key.
- Manual aisle entry from the overflow **Set aisle** action, the correction dialog, and the item editor all use one domain write path and produce a `MANUAL` cache row.
- Manual rows are never overwritten by automatic, sign, or cache results while they are valid or stale. Only invalidation, TTL cleanup, or another manual write replaces them.
- Cache rows older than 90 days stay usable offline with the existing stale indicator. They do not bypass rejected-result gating.
- The existing **Not right?** action remains the single entry point. When persisted `NEEDS_REVIEW` candidates exist it opens the candidate picker; otherwise it opens the Set aisle dialog.
- The candidate picker gains a trailing **Mark aisle incorrect** action. The existing **Set aisle** trailing action remains.
- The Set aisle dialog used for correction shows a confirmation explaining that the shared cache row for this store and item name will be invalidated. It offers an optional correct aisle and bay.
- Saving with a blank correction clears the item's `aisle`, `bay`, `matchedProductName`, `matchedProductUrl`, `matchCandidatesJson`, lookup error, confidence, and source; sets status `NOT_FOUND`; and sorts the item with unknown aisles last.
- Saving with a correction clears the stale fields, writes the user's aisle/bay as `MANUAL`, and writes a `MANUAL` cache row.
- Invalidating a cache row does not delete rejection rows for that key. Rejection rows keep their own 90-day clock.
- A rejected fingerprint is `storeId + normalizedQuery + aisle + bay + matchedProductName`. `source` is deliberately excluded because cache writes normalize source to `CACHE`.
- At most five rejection rows are kept per key, newest first. Re-adding the same fingerprint updates its `rejectedAt` and `expiresAt` instead of inserting a duplicate.
- On lookup, after the manual short-circuit and before sign/cache/website resolution, the repository computes the candidate fingerprint. If it matches any active rejection row for the key, the app does not apply the result. The item becomes `NEEDS_REVIEW` with manual entry available, or keeps the existing `NOT_FOUND` state when there are no candidates.
- Force refresh bypasses the cache row but still enforces rejection rows. A rejected aisle cannot be resurrected by refresh.
- Sign results are gated by rejection rows and write cache when accepted.
- Manual entry is never gated by rejection rows, including when the user types the previously rejected aisle deliberately.
- “Delete all data” clears cache and rejection rows, then reseeds built-in stores.

### Non-functional

- All fingerprinting, cache, and rejection work runs off the main thread; UI state posts to the main thread.
- Rejection rows store no raw images, full item text, addresses, or coordinates. Only the normalized query and match fields needed for fingerprinting are retained.
- No new network request is introduced by this phase; existing one-request-per-second and 403/429/challenge rules are unchanged.
- Accessibility: the new candidate-picker action, dialog checkbox/field, confirmation copy, source chip text, and error states need TalkBack labels and content descriptions.
- Dependency additions: none.

## Technical approach

Create or change these areas:

- `domain/model/AisleCache.java` — change the TTL constant/documentation to 90 days; keep `isExpired(now)` semantics.
- `domain/model/RejectedAisleResult.java` — pure Java model with `storeId`, `normalizedQuery`, `fingerprint`, `rejectedAt`, `expiresAt`, and `deletedAt`.
- `domain/usecase/AisleResultFingerprint.java` — deterministic fingerprint over `storeId + normalizedQuery + aisle + bay + matchedProductName`; no Android imports.
- `domain/repository/RejectedAisleResultRepository.java` — domain-owned find/add/expire/delete/delete-by-store interface.
- `domain/usecase/MarkAisleIncorrectUseCase.java` — invalidate cache, clear item fields, add/refresh rejection, optionally apply manual correction.
- `data/repository/AisleLookupRepositoryImpl.java` — 90-day constant, rejection gate after manual short-circuit, sign/cache writes, and manual cache write through the shared write path.
- `data/repository/AisleCacheRepositoryImpl.java` — expose delete-by-key for invalidation without deleting rejection rows.
- `data/repository/SetItemAisleUseCase` call sites / `UpdateItemUseCase` — route all manual aisle saves through one cache-writing domain path.
- `data/local/entity/RejectedAisleResultEntity.java`, `data/local/dao/RejectedAisleResultDao.java`, `data/repository/RejectedAisleResultRepositoryImpl.java`.
- `di/DatabaseModule.java`, `di/RepositoryModule.java` — migrate/bind the new DAO and repository.
- `data/local/Migrations.java`, `FastShopprDatabase.java` — migration 7→8 and exported schema 8.
- `domain/usecase/CleanUpExpiredDataUseCase.java` and `data/local/dao/UserDataDao.java` — purge expired rejections and clear them on delete-all.
- `ui/listdetail/ListDetailViewModel.java`, `ListDetailFragment.java`, `ListDetailAdapter.java`, `ListDetailUiEvent.java`, `ItemActionEvent`, `ReviewLookupCandidatesUseCase.java` — candidate-picker action, correction dialog event, confirmation, and chip state.
- `res/menu/menu_item_actions.xml`, `res/layout/item_list_item_row.xml`, `res/layout/dialog_set_aisle.xml`, `res/values/strings.xml` — labels, content descriptions, and confirmation copy. No new navigation destination.

Threading: database and fingerprint work on the existing executor; LiveData state and `SingleEvent` effects post to the main thread. No new foreground service and no AI inference.

## Data changes

- Keep `aisle_cache` columns unchanged. Apply the 90-day TTL when writing `expiresAt`; existing rows keep their stored expiry until refreshed, invalidated, or purged.
- Add table `rejected_lookup_results`:
  - `id TEXT PRIMARY KEY`
  - `storeId TEXT NOT NULL` with FK to `stores(id)` on `RESTRICT`
  - `normalizedQuery TEXT NOT NULL`
  - `fingerprint TEXT NOT NULL`
  - `rejectedAt INTEGER NOT NULL`, `expiresAt INTEGER NOT NULL`, `deletedAt INTEGER` nullable — all UTC epoch ms.
  - Unique index `(storeId, normalizedQuery, fingerprint)`.
  - Index `(storeId, normalizedQuery, expiresAt, deletedAt)`.
  - Keep at most five active rows per `(storeId, normalizedQuery)`, newest by `rejectedAt`.
- Room version 7 → 8 via `MIGRATION_7_8`; export schema under `app/schemas/`. Add `MigrationTest` covering 7→8 and table creation.
- Store soft-delete sets `deletedAt` on rejection rows; restore clears it; purge after 30 days removes it. Expired rejection rows (`expiresAt < now`) purge on the existing daily cleanup job regardless of `deletedAt`.
- No `ListItemEntity` schema change. Correction clears existing fields through `MarkAisleIncorrectUseCase`.
- No new SharedPreferences key.

## UI changes

- List detail item row keeps the existing aisle chip and **Not right?** action. The chip displays `Manual`, `Cached`, or `Needs review` alongside the aisle text; stale cached rows keep the existing muted styling.
- Candidate picker gains a trailing **Mark aisle incorrect** action beside **Set aisle**.
- The existing Set aisle dialog gains a confirmation step when opened through **Not right?** or the new picker action. It shows optional aisle/bay fields; saving blank is the mark-only path.
- Mark-only path clears the aisle immediately; the row moves below the “Needs attention” header with status `NOT_FOUND`.
- Refresh remains available from the item overflow and still enforces rejections.
- Add content descriptions for the new picker action, confirmation copy, source chip, and error Snackbars. Permission or lookup failure must not block manual entry.

## Acceptance criteria

- [ ] Successful WebView, HTTP, sign, and manual writes create or refresh one `aisle_cache` row keyed by `storeId + normalizedQuery` with `expiresAt = fetchedAt + 90 days`.
- [ ] Every successful write resets the 90-day clock; expired rows remain usable offline with the stale indicator.
- [ ] Manual aisle rows from overflow, correction, and item editor are authoritative and survive subsequent automatic/sign lookups until invalidation or manual replacement.
- [ ] Later `displayName`, `normalizedText`, or store edits do not rekey existing cache rows.
- [ ] Existing inline **Not right?** opens the candidate picker when candidates exist and the Set aisle dialog otherwise; no new entry point is added.
- [ ] Candidate picker exposes **Mark aisle incorrect**; confirmation explains shared-cache invalidation; optional correction saves as `MANUAL`.
- [ ] Blank correction clears item aisle/bay/match/candidate fields, sets `NOT_FOUND`, invalidates the shared cache row, and sorts the item unknown-last.
- [ ] Rejection fingerprint is `storeId + normalizedQuery + aisle + bay + matchedProductName` and excludes `source`.
- [ ] Rejection rows dedupe by unique triple, cap at five newest per key, expire after 90 days, soft-delete with store, restore with store, and clear on delete-all.
- [ ] Lookup precedence is manual → sign → cache → website; rejection rows gate sign/cache/website but never manual entry.
- [ ] Force refresh bypasses cache but still refuses to apply a rejected fingerprint; the item stays `NEEDS_REVIEW` or `NOT_FOUND` with manual entry available.
- [ ] Sign results are suppressed by rejection rows and write cache when accepted.
- [ ] Migration 7→8 and exported schema 8 pass Room migration tests.
- [ ] `rawText` is unchanged by every cache, correction, and rejection path.
- [ ] New UI states have TalkBack labels/content descriptions; manual entry remains available on every failure path.
- [ ] Every test in the test plan has run and passed, including `connectedDebugAndroidTest` on the simulated device (emulator); no test has failed, errored, or been skipped.

## Test plan

| Test | Level | Notes |
|---|---|---|
| `AisleCacheTtlTest` | Unit | 90-day expiry via injected `Clock`; write resets expiry; stale offline fallback unchanged. |
| `AisleResultFingerprintTest` | Unit | Stable fields, `source` excluded, null bay/product handling. |
| `RejectedAisleResultRepositoryTest` | Room | Unique triple, five-row cap, 90-day expiry query, soft delete, store restore, purge, delete-all. |
| `MIGRATION_7_8Test` | Room | Uses `MigrationTestHelper` and exported schema 8; verifies `rejected_lookup_results` and indexes. |
| `AisleLookupRepositoryCacheTest` | Unit | WebView/HTTP/sign/manual cache writes; manual authority; no rekey; stale cache behavior. |
| `AisleLookupRepositoryRejectionTest` | Unit | Sign/cache/website gating, force refresh gating, manual bypass, different fingerprint accepted. |
| `MarkAisleIncorrectUseCaseTest` | Unit | Cache invalidation, item-field cleanup, `NOT_FOUND`, rejection add/refresh, optional manual save. |
| `UserDataRepositoryAisleCorrectionTest` | Room | Delete-all clears cache and rejection rows; built-in stores reseeded. |
| `CleanUpExpiredDataAisleCorrectionTest` | Unit/Room | Expired rejections purged on daily job; store soft-delete purge still works. |
| `ManualListsFlowTest` | Espresso | Not right? → picker action → confirmation → blank correction → unknown-last; correction save → `Manual` chip; refresh keeps rejection. |
| `AisleCacheConnectedFlowTest` | Instrumentation | End-to-end cache hit, correction, and refresh on emulator with injected clock; no network after cache hit. |
| Manual demo | Manual | Steps below; result recorded in completion log. |

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored, been skipped, or failed to run. That includes every test in the test plan and `./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator) or a physical device. An unavailable device is not a pass: leave the phase `in-progress` or `blocked` and record the unrun test in the completion log.

## Manual demo

1. Add an item such as “eggs” to a Walmart list. Resolve it and confirm the aisle chip shows `Cached` and `rawText` is unchanged.
2. Tap **Not right?**; if candidates appear, choose **Mark aisle incorrect**. Confirm the dialog, leave correction blank, and confirm the item aisle clears, status is `NOT_FOUND`, and the row moves below “Needs attention”.
3. Tap refresh on that item. Confirm the same reported aisle is not applied; the item stays unresolved with manual entry available.
4. Tap **Not right?** again, enter a correct aisle, and save. Confirm the chip shows `Manual` and refresh does not replace it.
5. Mark the manual aisle wrong with a different correction. Confirm refresh still refuses the original rejected aisle and keeps the newer correction.
6. Soft-delete the store, restore within 30 days, and confirm rejection rows return. Run “Delete all data” and confirm cache and rejection rows are gone.
7. Run all required Gradle commands, including `connectedDebugAndroidTest`, and record results in the completion log.

## Risks

- First-match retailer results can be wrong → `matchedProductName` participates in the fingerprint and manual user entry always wins.
- Cache writes normalize `source` to `CACHE` → fingerprint excludes `source` so cache hits and fresh website results are treated as the same rejected result.
- Rejection rows live separately from cache rows → invalidating cache preserves suppression; delete-all and cleanup must clear both.
- TTL change from 60 to 90 days → D-058 supersedes D-051; tests use an injected clock and assert the new value.
- Phase 11 is still `not-started` when this phase is authored → dependency is sequence only; this file supersedes Phase 11's “no cache write for sign results” test note.
- Store deletion is restricted while cache rows reference it → rejection rows use the same FK and lifecycle; archive remains the user-facing path.
- No new network access is added → rate limits and block handling stay unchanged.

## Open questions

- None. Product decisions settled 2026-11-26; implementation follows existing Hilt, Room, LiveData, and Java patterns.

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
