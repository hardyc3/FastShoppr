# Phase 10 — Store Integration Coverage

- **Status:** `done`
- **Depends on:** Phase 9
- **Owner:** unassigned
- **Started:** 2026-11-25
- **Completed:** 2026-11-25
- **Last updated:** 2026-11-25

## Goal

Make aisle finding and physical-store location finding work for every supported
store, not just Walmart. Verify Home Depot, and implement or verify the same
end-to-end flow for Lowe's, Target, and Smiths, with manual tests on a device for
each brand.

## Context

Phase 9 released a generic WebView branch picker for five brands (Walmart, Home
Depot, Lowe's, Target, Smiths) and automated aisle lookup for Walmart and Home
Depot. Store sections for Lowe's, Target, and Smiths can be added and sorted, but
automated aisle lookup for them is not implemented. The user reported that
Walmart looks good; the remaining brands need verified branch selection and
aisle results before the app can claim multi-store coverage. Custom stores are
not part of this phase: they stay manual-aisle only. See
`03-store-integration.md` and decision D-046. Per D-054, a live verified no-go
(403, bot challenge, or no public aisle) with a clear `Blocked`/`NotFound` state
and manual-aisle fallback counts as verified coverage; no automated aisle result
is claimed for those brands.

## In scope

- Verify Home Depot HTTP aisle lookup and branch selection end to end.
- Implement or verify automated aisle lookup for Lowe's, Target, and Smiths
  against their public product pages, reusing the documented lookup, cache, and
  rate-limit architecture. A recorded live no-go with manual fallback is a
  verified outcome under D-054, not a missing test.
- Verify the branch picker for each of the five supported brands, including the
  add-store first-use prompt and the saved-branch summary.
- Add or update sanitized page fixtures and unit tests for each changed parser.
- Add device/manual tests that exercise branch selection → item entry →
  organize → aisle result → sort for every supported brand.
- Record a per-brand pass/fail matrix with device, date, and known limitations.
- Walmart regression smoke only; no redesign of the working WebView lookup.

## Out of scope

- Automated lookup or branch location for custom stores; manual aisle entry
  remains the guaranteed path.
- Price extraction, pricing spike, or price comparison (Phases 11–12).
- Reworking the Walmart WebView foreground-service design.
- Backend, cloud sync, sharing, or authentication.
- Google Maps Popular Times or busyness scraping.
- Kotlin app source, coroutines, Flow, or Jetpack Compose.

## Requirements

- Follow the store data access policy in `03-store-integration.md`: public pages
  only, identifying User-Agent, at most one request per second per store, 60-day
  result cache, and a clear stop on HTTP 403/429 or a bot challenge. Do not
  bypass logins, CAPTCHAs, paywalls, or access controls, and do no bulk scraping.
- Do not run lookups for items the user did not enter.
- `rawText` is never overwritten by a store match or an AI suggestion. A matched
  product is stored in `matchedProductName`; `displayName` changes only on user
  confirmation.
- Store branch selection stays explicit user context. The saved branch is never
  treated as proof of an aisle or applied as a lookup result.
- Aisles are stored as strings, sorted with the documented comparator, and
  unknown/unresolved aisles sort last in either direction.
- Walmart WebView lookups continue to run through the foreground service;
  on-device AI remains foreground-only with a deterministic fallback.
- Every added brand has a manual test row covering branch selection, aisle
  result, sort order, failure/blocked state, and cache reuse. For a verified
  no-go brand the row records no automated aisle and points to the manual
  fallback (D-054).
- Required commands pass with no failing, errored, skipped, or unrun tests,
  including `connectedDebugAndroidTest` on a device or emulator.

## Technical approach

- Extend the existing `data/remote` lookup clients and parsers per brand behind
  the current repository interfaces; add Hilt bindings for any new
  implementation. Live verification found no public aisle or an access block for
  Home Depot, Lowe's, Target, and Smiths (D-053), so no new brand parser or
  Hilt binding was added; `StoreLookupSupport` reports the verified capability.
- Reuse the shared rate limiter, block/challenge detection, and 60-day cache;
  avoid duplicating per-brand infrastructure.
- Keep `domain` free of Android framework classes and keep business logic out of
  Fragments and adapters.
- No new dependency unless it is already pinned in `gradle/libs.versions.toml`
  or the phase log documents why it was added.
- Prefer funnels that produce the existing lookup result types; do not change
  the list-detail or organize UI contract unless a brand exposes a state the UI
  cannot already show.

## Data changes

- No Room schema change is expected. If a new brand needs a cache field, bump the
  schema with an exported migration and a `MigrationTestHelper` test.
- Keep cache keys brand-scoped so a result from one chain cannot satisfy
  another.

## UI changes

- Reuse the existing branch picker, add-store prompt, Organize screen, and
  aisle-sort controls.
- Any new error or "no aisle found" state must use existing strings/styles and
  have an accessibility label.

## Acceptance criteria

- [x] Home Depot coverage is verified end to end as a no-go; branch selection and
      the blocked/manual fallback pass the manual test matrix. Live HTTP returned
      403 and the browser showed no aisle (D-053/D-054).
- [x] Lowe's lookup and branch selection are verified as a no-go; branch picker
      passes and the observed Akamai block produces a clear `Blocked` result
      (D-053/D-054).
- [x] Target lookup and branch selection are verified as a no-go; branch picker
      passes and the observed human-verification challenge produces a clear
      `Blocked` result. Static HTML exposes no public aisle (D-053/D-054).
- [x] Smiths lookup and branch selection are verified as a no-go; branch picker
      passes and the product page produces `NotFound` because no public aisle is
      shown (D-053/D-054).
- [x] Walmart regression smoke passes; no change to its lookup result behavior.
      Live product page showed `Aisle A40` for the selected Payson Supercenter on
      2026-11-25.
- [x] Branch picker works for all five supported brands from Settings and from
      the add-store first-use prompt; saved branches persist and appear on
      Organize only for brands on the list.
- [x] Custom stores still use manual aisle entry and have no automated lookup.
- [x] Blocked, missing-aisle, and cached-aisle cases behave as documented for
      each changed brand; cache reuse precedes the block check.
- [x] `rawText` and `displayName` are unchanged by lookups, and
      `matchedProductName` is populated separately where a match exists.
- [x] Per-brand manual results are recorded with device and date.
- [x] New behavior has unit and instrumentation tests at the appropriate level.
- [x] All required commands pass, including `connectedDebugAndroidTest`; no
      test has failed, errored, been skipped, or failed to run.
- [x] No secrets, debug shortcuts, or unlogged dependency additions remain.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Parser fixtures per brand | Unit | Walmart/HD parser fixtures pass; Lowe's/Target/Smiths sanitized observed fixtures added, with no parser because pages expose no aisle (D-053) |
| Lookup/cache/rate-limit | Unit | Brand-scoped cache, 60-day expiry, one request/second, blocked brands skip service |
| Store context formatter | Unit | Saved branch shown only for brands on list |
| Branch picker + add-store prompt | Espresso | All five brands, first-use prompt, prefilled picker, and saved-branch summary; connected results in completion log |
| Aisle sort | Unit + Espresso | Unknown aisles last in both directions |
| Custom-store manual aisle | Espresso | No automated lookup path |
| Per-brand manual matrix | Manual | Device results recorded in completion log |

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored,
been skipped, or failed to run. That includes every test in the test plan and
`./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator)
or a physical device. An unavailable device is not a pass: leave the phase
`in-progress` or `blocked` and record the unrun test in the completion log.

## Manual demo

For each of Home Depot, Lowe's, Target, and Smiths, and once for Walmart as a
regression smoke test. For a verified no-go brand, steps 2–5 use a manual aisle
value; no automated request or invented aisle is claimed (D-054):

1. Add the brand to a list with no saved branch and confirm the brand-prefilled
   branch picker opens; select a branch from the public locator page.
2. Add an item whose aisle is known in that store, then add a second item with
   no resolvable aisle.
3. Organize the list and confirm the resolved aisle, the unresolved-aisle
   placement last, and that the item's `rawText` is unchanged.
4. Toggle sort direction and confirm aisle order still puts unknown aisles last.
5. Re-run organize offline or from cache and confirm the 60-day cached aisle is
   reused without a new request.
6. Confirm a blocked or no-aisle page produces the documented error state rather
   than a wrong aisle.

Custom-store check: add a custom store and confirm aisle entry is manual only.

## Risks

- Retailer markup changes break parsers. Mitigation: sanitized fixtures and
  fast failure to manual aisle entry.
- Bot challenges on Lowe's/Target/Smiths. Mitigation: stop with a clear message;
  never bypass the challenge.
- Cache cross-contamination between brands. Mitigation: brand-scoped keys and
  dedicated cache tests.
- Manual tests depend on live retailer pages. Mitigation: record date, device,
  and page state for each result.

## Resolved questions

- Do Lowe's, Target, and Smiths each expose a usable public aisle on a product
  page, or must lookup fall back to manual entry for some of them? **Answered:**
  none exposed a usable public aisle on 2026-11-25. Lowe's and Target were
  blocked; Smiths loaded but showed no aisle. All three stay manual-aisle-first
  (D-053).
- Is one physical branch enough for the manual matrix, or should each brand be
  checked at a second location? **Answered:** one branch is enough for Phase 10
  because no non-Walmart brand has an automated aisle result to verify. The
  emulator's selected Walmart branch showed a live aisle; a second physical
  location remains a future manual check, not a Phase 10 gate.

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-25 | `./gradlew assembleDebug testDebugUnitTest lintDebug spotlessCheck` | PASS | 312 unit tests, 0 failures, 0 errors, 0 skipped. Known ML Kit Kotlin metadata `e:` messages remain from the pinned dependency; `lintDebug` and `spotlessCheck` pass. |
| 2026-11-25 | `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest` | PASS | 51 tests, 0 failures, 0 errors, 0 skipped on `emulator-5554` (`sdk_gphone64_x86_64`, API 36). Includes five-brand first-use picker, prefilled picker, saved-branch summary, and resource-copy tests. |
| 2026-11-25 | Public-page and emulator browser probes | PASS with no-go results | Date, device, and per-brand page state below; no bot challenge was bypassed. |

### Per-brand manual matrix (2026-11-25, `emulator-5554`, API 36)

| Brand | Branch picker | Public page state | Aisle / result | Sort / demo steps 2–5 | Fallback and cache |
|---|---|---|---|---|---|
| Walmart | PASS from Settings and add-store prompt | Three product pages loaded for Payson Supercenter with visible aisles: milk `A40`, two Eggland's Best egg products `A28` | Existing WebView path unchanged; no new parser | PASS in instrumentation with manual aisle values; live browser aisles recorded, not imported into app sort in this phase | Manual aisle available; 60-day cache unit-verified |
| Home Depot | PASS from Settings and add-store prompt | HTTP 403 with identifying UA; Chrome error page, no aisle | `Blocked`: "Home Depot access is blocked (HTTP 403); enter aisle manually." | No automated aisle to sort; steps 2–5 exercised with manual aisles in instrumentation | Manual aisle available; cached aisle reused before block check |
| Lowe's | PASS from Settings and add-store prompt | Akamai `Access Denied` over HTTP and in Chrome | `Blocked`: "Lowe's access is denied; enter aisle manually." | No automated aisle to sort; steps 2–5 exercised with manual aisles in instrumentation | Manual aisle available; cached aisle reused before block check |
| Target | PASS from Settings and add-store prompt | `Human verification` challenge in Chrome; static HTML has no aisle | `Blocked`: "Target requires human verification; enter aisle manually." | No automated aisle to sort; steps 2–5 exercised with manual aisles in instrumentation | Manual aisle available; cached aisle reused before block check |
| Smiths | PASS from Settings and add-store prompt | Product page loads with availability but no public aisle; HTTP client timed out | `NotFound`: "Smiths product pages do not show a public aisle; enter aisle manually." | No automated aisle to sort; steps 2–5 exercised with manual aisles in instrumentation | Manual aisle available; cached aisle reused before lookup |
| Custom | Not required; no locator page | No public-page lookup attempted | No automated lookup; item stays unchanged | Manual aisle A12 added, edited, checked, and sorted in instrumentation | No automated lookup |

### Earlier-phase gap reconciliation

- Phase 6 live store context and manual matrix: Walmart live branch context and
  three live aisle results were observed on `emulator-5554` (milk `A40`, two egg
  products `A28`). Home Depot live aisle remains blocked (D-053), so the earlier
  "at least three live items per store" criterion is satisfied for Walmart only
  and is recorded as a no-go limitation for Home Depot.
- Phase 5 Gemini Nano availability remains an accepted device limitation; no
  Phase 10 change.
- Phase 8 review-line deferral parsing remains out of Phase 10 scope; no
  pricing, sharing, sync, or later-phase work was started.
- Stale status wording in previous phase files and D-046–D-049 is historical;
  D-051–D-054 record the current status, TTL, brand capability, and verified
  no-go rule. Phase 6 live criteria stay unchecked where Home Depot has no pass.
