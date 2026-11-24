# Phase 10 — Store Integration Coverage

- **Status:** `not-started`
- **Depends on:** Phase 9
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-22

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
`03-store-integration.md` and decision D-046.

## In scope

- Verify Home Depot HTTP aisle lookup and branch selection end to end.
- Implement or verify automated aisle lookup for Lowe's, Target, and Smiths
  against their public product pages, reusing the documented lookup, cache, and
  rate-limit architecture.
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
  result, sort order, failure/blocked state, and cache reuse.
- Required commands pass with no failing, errored, skipped, or unrun tests,
  including `connectedDebugAndroidTest` on a device or emulator.

## Technical approach

- Extend the existing `data/remote` lookup clients and parsers per brand behind
  the current repository interfaces; add Hilt bindings for any new
  implementation.
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

- [ ] Home Depot automated aisle lookup and branch selection pass the manual
      test matrix.
- [ ] Lowe's automated aisle lookup and branch selection are implemented or
      verified and pass the manual test matrix.
- [ ] Target automated aisle lookup and branch selection are implemented or
      verified and pass the manual test matrix.
- [ ] Smiths automated aisle lookup and branch selection are implemented or
      verified and pass the manual test matrix.
- [ ] Walmart regression smoke passes; no change to its lookup result behavior.
- [ ] Branch picker works for all five supported brands from Settings and from
      the add-store first-use prompt; saved branches persist and appear on
      Organize only for brands on the list.
- [ ] Custom stores still use manual aisle entry and have no automated lookup.
- [ ] Blocked, missing-aisle, and cached-aisle cases behave as documented for
      each changed brand.
- [ ] `rawText` and `displayName` are unchanged by lookups, and
      `matchedProductName` is populated separately where a match exists.
- [ ] Per-brand manual results are recorded with device and date.
- [ ] New behavior has unit and instrumentation tests at the appropriate level.
- [ ] All required commands pass, including `connectedDebugAndroidTest`; no
      test has failed, errored, been skipped, or failed to run.
- [ ] No secrets, debug shortcuts, or unlogged dependency additions remain.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Parser fixtures per brand | Unit | Aisle present, aisle absent, blocked page |
| Lookup/cache/rate-limit | Unit | Brand-scoped cache, 60-day expiry, one request/second |
| Store context formatter | Unit | Saved branch shown only for brands on list |
| Branch picker + add-store prompt | Espresso | All five brands, saved and unsaved branches |
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
regression smoke test:

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

## Open questions

- Do Lowe's, Target, and Smiths each expose a usable public aisle on a product
  page, or must lookup fall back to manual entry for some of them?
- Is one physical branch enough for the manual matrix, or should each brand be
  checked at a second location?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
