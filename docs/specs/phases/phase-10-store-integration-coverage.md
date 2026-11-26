# Phase 10 — Store Integration Coverage

- **Status:** `in-progress`
- **Depends on:** Phase 9
- **Owner:** unassigned
- **Started:** 2026-11-25
- **Completed:** —
- **Last updated:** 2026-11-25

## Goal

Make aisle finding work for every supported store. Walmart keeps its verified
foreground WebView path. Home Depot, Lowe's, and Smiths use a shared public-page
search-first path: search the retailer, open the first query-matching
same-retailer product result, and parse the visible aisle/bay. Target is removed because live checks
and the user's own browser verification found no public aisle.

## Context

Phase 9 released a generic WebView branch picker for five brands and automated
aisle lookup for Walmart and Home Depot. Phase 10 originally verified no-go
states for Home Depot, Lowe's, Target, and Smiths (D-053/D-054). The user then
reported that Smiths, Lowe's, and Home Depot do show aisles after a user picks a
specific item from a search; Target does not. That supersedes the earlier
"no aisle anywhere" conclusion for those three brands.

Live probes from this environment remain mixed: Walmart product pages expose
aisles; Home Depot returns an error page and Lowe's returns Akamai
`Access Denied`; Smiths product pages load but can hide availability/aisle when
the item is unavailable or the shopping modality is not in-store. Those states
must produce `Blocked` or `NotFound`, never an invented aisle. See D-055 and
D-056.

## In scope

- Shared search-first HTTP lookup for Home Depot, Lowe's, and Smiths, reusing
  the existing repository, cache, and rate-limit contracts.
- Explicit first-match trade-off: the first query-matching same-retailer product
  link is used; unrelated carousel links are skipped. Low-confidence parses
  surface as `NEEDS_REVIEW`, and manual aisle entry remains available.
- Remove Target from the enum, seeds, locator pages, UI pickers, lookup support,
  and strings, with a Room migration that converts stored Target rows to
  archived custom stores.
- Sanitized fixtures and unit tests for search links, product aisle parsing,
  blocked pages, no-aisle pages, HTTP 403/429, and two-request rate limiting.
- Updated instrumentation tests for the four-brand picker, add-store first-use
  prompt, and saved-branch summary.
- Updated manual matrix with device, date, and live page state per brand.
- Walmart regression smoke only; no redesign of the working WebView lookup.

## Out of scope

- Automated lookup or branch location for custom stores; manual aisle entry is
  guaranteed.
- Target lookup or branch picker; existing Target rows are archived as custom.
- Price extraction, pricing spike, or price comparison (Phases 11–12).
- Reworking the Walmart WebView foreground-service design.
- Backend, cloud sync, sharing, or authentication.
- Google Maps Popular Times or busyness scraping.
- Kotlin app source, coroutines, Flow, or Jetpack Compose.
- Bypassing 403s, bot challenges, logins, CAPTCHAs, paywalls, or access controls.

## Requirements

- Follow the store data access policy in `03-store-integration.md`: public pages
  only, identifying User-Agent, at most one request per second per store, 60-day
  result cache, and a clear stop on HTTP 403/429 or a bot challenge.
- Do not run lookups for items the user did not enter.
- `rawText` is never overwritten by a store match or an AI suggestion; a matched
  product is stored in `matchedProductName`.
- Store branch selection stays explicit user context. A saved branch is not
  proof of an aisle.
- Aisles are stored as strings, sorted with the documented comparator, and
  unknown/unresolved aisles sort last in either direction.
- Walmart WebView lookups continue through the foreground service.
- New Room migration is validated with `MigrationTestHelper`.
- Required commands pass with no failing, errored, skipped, or unrun tests,
  including `connectedDebugAndroidTest` on a device or emulator.

## Technical approach

- `RetailSiteConfig` holds per-retailer search URL, host, product-path token,
  and `LookupSource`.
- `RetailHttpLookupService` fetches the search page, selects the first
  query-matching same-host HTTPS product link, reserves a second rate-limit
  slot, fetches that product page, and hands the HTML to `RetailPageParser`.
- `RetailPageParser` detects access blocks/challenges before parsing, prefers a
  store-specific availability line matching the selected store name/city,
  rejects a labelled line for a different store, treats an unlabelled visible
  aisle as low confidence, and returns `Found`, `Multiple`, `NotFound`, or
  `Blocked`.
- Hilt provides the three brand-specific service instances into the existing
  `Set<StoreLookupService>`; the repository still owns cache, retries, and
  item updates.
- `StoreLookupSupport` marks Walmart/Home Depot/Lowe's/Smiths `ENABLED` and
  custom/null `MANUAL_ONLY`.
- No new dependency; Jsoup and OkHttp are already pinned.

## Data changes

- Room version `7` adds `MIGRATION_6_7`: Target rows become archived custom
  stores named `Removed store`, and the old Target cache row is deleted.
- Cache entries stay brand-scoped via `AisleCache.source` and store brand checks.
- No new entity fields.

## UI changes

- Reuse the existing branch picker, add-store prompt, Organize screen, and
  aisle-sort controls.
- The brand array now has four entries; Target no longer appears.
- Existing lookup error, `NEEDS_REVIEW`, and accessibility labels are reused.

## Acceptance criteria

- [x] Home Depot search → first product → aisle parse works from sanitized
      fixtures; live 403/error pages stop with `Blocked` and manual fallback.
- [x] Lowe's search → first product → aisle parse works from sanitized
      fixtures; live Akamai `Access Denied` stops with `Blocked`.
- [x] Smiths search → first product → aisle parse works from sanitized
      fixtures; a product without a visible aisle returns `NotFound`.
- [x] Only the first query-matching same-retailer product link is followed;
      off-host and unrelated carousel links are ignored; two requests per lookup
      are rate limited.
- [x] The selected store's availability block wins; a labelled aisle for a
      different store is not inferred.
- [x] HTTP 403/429 and challenge markers stop before any further fetch.
- [x] Low-confidence parses and their cache hits stay `NEEDS_REVIEW`.
- [x] Target is absent from picker, seeds, branch locator, and lookup support;
      stored Target rows migrate to archived custom stores.
- [x] Cache reuse stays brand-scoped and expires after 60 days.
- [x] `rawText` and `displayName` are unchanged; `matchedProductName` and
      `matchedProductUrl` are populated separately.
- [x] Walmart regression smoke passes with no change to its lookup behavior.
- [x] New behavior has unit and instrumentation tests at the appropriate level.
- [x] All required commands pass, including `connectedDebugAndroidTest`; no
      test has failed, errored, been skipped, or failed to run.
- [x] No secrets, debug shortcuts, or unlogged dependency additions remain.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Retail page parser | Unit | Query-aware link selection, store context, aisle/bay, blocked, no aisle, low confidence |
| Retail HTTP service + repository | Unit | Search-to-product flow, 403/429, no product link, search/product rate-limit slots |
| Lookup/cache/rate-limit | Unit | Brand-scoped cache, 60-day expiry, block stop, first-result behavior |
| Store capability | Unit | Four enabled brands, custom/manual-only, no Target |
| Migration 6→7 | Android | Target row archived as custom; stale cache removed |
| Branch picker + add-store prompt | Espresso | Four brands, first-use prompt, prefilled picker, saved summary |
| Aisle sort / custom manual aisle | Unit + Espresso | Unknown aisles last; custom stays manual |
| Per-brand manual matrix | Manual | Device results recorded in completion log |

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored,
been skipped, or failed to run. That includes every test in the test plan and
`./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator)
or a physical device. An unavailable device is not a pass.

## Manual demo

For Home Depot, Lowe's, Smiths, and once for Walmart as a regression smoke:

1. Add the brand to a list with no saved branch and confirm the brand-prefilled
   branch picker opens; select a branch from the public locator page.
2. Add an item whose aisle is known in that store, then add a second item with
   no resolvable aisle.
3. Organize the list. Confirm that the first query-matching product result's aisle is used or
   held as `NEEDS_REVIEW` when confidence is low, that the unresolved item sorts
   last, and that `rawText` is unchanged.
4. Toggle sort direction and confirm unknown aisles still sort last.
5. Re-run organize from cache and confirm the 60-day cached aisle is reused.
6. Confirm a blocked or no-aisle page produces the documented state rather than
   a wrong aisle.

Custom-store check: add a custom store and confirm aisle entry is manual only.

## Risks

- First query-matching result can be the wrong variant. Mitigation: store the matched
  product URL/name, downgrade low-confidence parses to `NEEDS_REVIEW`, and keep
  manual entry available (D-055).
- Retailer markup changes break parsers. Mitigation: sanitized fixtures and
  fast failure to manual aisle entry.
- Bot challenges on Home Depot/Lowe's/Smiths. Mitigation: stop with a clear
  message; never bypass the challenge.
- Cache cross-contamination between brands. Mitigation: brand-scoped keys and
  dedicated cache tests.
- Manual tests depend on live retailer pages. Mitigation: record date, device,
  and page state for each result.

## Resolved questions

- Does Target expose a usable public aisle on a product page? **Answered:** no.
  Target is removed from supported brands and stored rows are archived as
  custom (D-056).
- Do Home Depot, Lowe's, and Smiths expose aisles? **Answered:** yes, after a
  user selects a specific search result; the app uses the first query-matching
  product result as a documented trade-off (D-055). Live access can still be
  blocked or unavailable, which must surface as `Blocked`/`NotFound`.
- Is one physical branch enough for the manual matrix? **Answered:** one branch
  is enough while live non-Walmart pages can be blocked; blocked states are
  recorded with device and date.

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-25 | `./gradlew assembleDebug testDebugUnitTest lintDebug spotlessCheck` | PASS | 312 unit tests, 0 failures, 0 errors, 0 skipped. Known ML Kit Kotlin metadata `e:` messages remain from the pinned dependency; `lintDebug` and `spotlessCheck` pass. |
| 2026-11-25 | `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest` | PASS | 51 tests, 0 failures, 0 errors, 0 skipped on `emulator-5554` (`sdk_gphone64_x86_64`, API 36). Included five-brand first-use picker, prefilled picker, saved-branch summary, and resource-copy tests. |
| 2026-11-25 | Public-page and emulator browser probes | PASS with no-go results | Original matrix below; no bot challenge was bypassed. |
| 2026-11-25 | Search-first follow-up probes via Chrome DevTools Protocol | Mixed | Walmart aisles confirmed earlier; HD error page and Lowe's `Access Denied` reproduced; Smiths product HTML loaded but the sampled milk item showed `Unavailable` with no aisle. User-confirmed aisle pages for the three brands still need a shared URL/screenshot before live parser verification is claimed. |
| 2026-11-25 | `./gradlew spotlessApply assembleDebug testDebugUnitTest lintDebug spotlessCheck` | PASS | Follow-up with search-first parser, Target removal, migration 6→7, and code-review fixes: 336 unit tests, 0 failures, 0 errors, 0 skipped. Known ML Kit Kotlin metadata `e:` messages remain from the pinned dependency; lint and formatting pass. |
| 2026-11-25 | `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest` | PASS | Follow-up: 51 tests, 0 failures, 0 errors, 0 skipped on `emulator-5554` (API 36), including four-brand picker, migration 6→7, and the stabilized typed-capture wait. |
| 2026-11-25 | Two-axis code review and remediation | PASS with one open live gap | Fixed first-match link selection, selected-store availability context, page-title product name, low-confidence cache status, and stale connected assertions. Live aisle success for the three non-Walmart brands still needs user-provided pages. |

### Remaining verification before `done`

- Live first-product → aisle success for Home Depot, Lowe's, and Smiths is not
  claimed from this environment. Fixture tests cover the parse and outcome
  states; the live pages currently return an HD error page, Lowe's
  `Access Denied`, and a Smiths item with no aisle. Share exact product URLs or
  sanitized screenshots showing an aisle for each brand to complete the live
  manual demo. Phase 10 stays `in-progress` until then.

### Original per-brand manual matrix (2026-11-25, `emulator-5554`, API 36)

| Brand | Branch picker | Public page state | Aisle / result | Sort / demo steps 2–5 | Fallback and cache |
|---|---|---|---|---|---|
| Walmart | PASS from Settings and add-store prompt | Three product pages loaded for Payson Supercenter with visible aisles: milk `A40`, two Eggland's Best egg products `A28` | Existing WebView path unchanged | PASS in instrumentation with manual aisle values | Manual aisle available; 60-day cache unit-verified |
| Home Depot | PASS from Settings and add-store prompt | HTTP 403 with identifying UA; Chrome error page, no aisle | `Blocked` before the search-first follow-up | No automated aisle to sort | Manual aisle available |
| Lowe's | PASS from Settings and add-store prompt | Akamai `Access Denied` over HTTP and in Chrome | `Blocked` before the search-first follow-up | No automated aisle to sort | Manual aisle available |
| Target | PASS from Settings and add-store prompt | `Human verification` challenge in Chrome; static HTML has no aisle | Removed from supported brands in the follow-up | N/A | Archived as custom by migration 6→7 |
| Smiths | PASS from Settings and add-store prompt | Product page loads with availability but no public aisle for the sampled item | `NotFound` before the search-first follow-up | No automated aisle to sort | Manual aisle available |
| Custom | Not required; no locator page | No public-page lookup attempted | No automated lookup | Manual aisle A12 added, edited, checked, and sorted | No automated lookup |

### Earlier-phase gap reconciliation

- Phase 6 live store context and manual matrix: Walmart live branch context and
  three live aisle results were observed on `emulator-5554` (milk `A40`, two egg
  products `A28`). Non-Walmart live aisle verification remains environment-
  dependent and is recorded per brand above.
- Phase 5 Gemini Nano availability remains an accepted device limitation; no
  Phase 10 change.
- Phase 8 review-line deferral parsing remains out of Phase 10 scope.
- Stale status wording in previous phase files and D-046–D-054 is historical;
  D-055 and D-056 record the search-first follow-up and Target removal.
