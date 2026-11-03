# Phase 6 — Store Lookup

- **Status:** `not-started`
- **Depends on:** Phase 0, Phase 2
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-03

## Goal

Resolve item aisles for Walmart and Home Depot, with caching, rate limiting, retries, and manual override. Phase 0 findings (D-021/D-022): Walmart lookup only via user-initiated foreground WebView (PX intermittent, assisted); Home Depot blocked (403) — manual entry first; OkHttp path stays dormant until a real-device/mobile-data pass retrieves aisle pages.

## Context

Phase 0 validated extraction. This phase turns the spikes into production services behind a stable interface. Background orchestration is Phase 7; this phase can run lookups from a foreground action or a debug harness.

## In scope

- `StoreLookupService` interface and request/result types.
- `AisleLookupRepository` with cache-first behavior.
- Walmart WebView extractor.
- Home Depot OkHttp + Jsoup extractor.
- Store context resolution (selected store, last used, GPS, manual).
- Rate limiter (one request per second per store).
- Cache TTL and refresh.
- Alternate query generation and retries.
- Manual aisle override.
- Fixture-based tests.
- Preserve `rawText`; store match separately.

## Out of scope

- WorkManager orchestration and notifications (Phase 7).
- Foreground service UI polish beyond the minimal extraction harness.
- Lowe's, Target, Smiths, or custom store lookups.
- Price extraction (Phase 11).
- AI or OCR changes.

## Requirements

### Interface

- `StoreLookupService` with `getBrand()` and `CompletableFuture<StoreLookupResult> lookup(...)`.
- Results: `Found`, `Multiple`, `NotFound`, `Blocked`, `Error`.
- The repository checks cache, then network, then manual override precedence.
- UI never calls a client directly.

### Cache

- Key: `storeId + normalizedQuery`.
- TTL: 60 days.
- Cache only successful `Found` results.
- Do not cache blocked or parser failures.
- Manual overrides take precedence.
- Expose refresh for a single item or store section.

### Rate limiting

- One request per second per store.
- Apply to HTTP and WebView navigations.
- Stop on 403/429/challenge.
- Do not retry blocked responses.

### Walmart

- Use a WebView with JavaScript enabled.
- Set store context before search.
- Extract from DOM or embedded JSON.
- Prefer structured JSON.
- Per-item timeout: 20 seconds.
- Destroy the WebView after use.
- Provide a minimal foreground harness for manual testing until Phase 7.

### Home Depot

- Use OkHttp with a descriptive user agent.
- Set store context.
- Search and fetch candidate product pages.
- Parse aisle/bay with Jsoup and/or embedded JSON.
- Max three candidate pages per item.
- Timeout: 15 seconds.
- Stop on block responses.

### Store context

- Priority: explicit item/list store, last used, nearest from location, manual ZIP/store prompt.
- Request location only on demand.
- Show selected store name/address.
- Never log coordinates.

### Retries and alternates

- Retry transient network errors twice with exponential backoff (initial attempt plus two retries).
- Do not retry blocks.
- Alternate queries only after no result.
- Multiple plausible products -> `NEEDS_REVIEW`.
- Low confidence -> keep result but mark review.

### Manual override

- User can enter aisle/bay manually at any time.
- Manual result source is `MANUAL`.
- Lookup results never overwrite a manual value.
- User can clear a manual value and re-run lookup.

## Technical approach

- `data/remote/walmart/WalmartWebViewLookupService.java`
- `data/remote/homedepot/HomeDepotHttpLookupService.java`
- `data/remote/StoreLookupCoordinator.java`
- `data/repository/AisleLookupRepositoryImpl.java`
- `data/remote/RateLimiter.java`
- `domain/usecase/LookupItemUseCase.java`
- Use Gson for embedded JSON.
- Use `ExecutorService` and `CompletableFuture`.
- Use `WebView` on the main thread.
- Use sanitized fixtures in `app/src/test/resources/fixtures/`.

## Data changes

- Populate `ListItem.aisle`, `bay`, `matchedProductName`, `matchedProductUrl`, `matchConfidence`, `lookupStatus`, `lookupSource`, `lookupError`.
- Write `AisleCache` entries on success.
- No schema change expected.

## UI changes

- Item row shows lookup status and "Not right?" action.
- Manual aisle dialog.
- Candidate selection dialog for `Multiple`.
- Refresh action.
- Blocked/error messages.

## Acceptance criteria

- [ ] Lookup interface and repository are implemented and injected.
- [ ] Home Depot fixtures parse aisle/bay correctly.
- [ ] Walmart fixtures/rendered extraction parse aisle/location correctly.
- [ ] Store context is applied and visible to the user.
- [ ] Cache hits avoid network calls.
- [ ] Expired cache is usable offline with a stale indicator.
- [ ] Rate limit is enforced.
- [ ] Blocked responses stop the run with a clear message.
- [ ] Alternate queries and retries behave as specified.
- [ ] Manual aisle values are never overwritten by lookup.
- [ ] `rawText` is unchanged; match data is stored separately.
- [ ] Unit tests use fixtures and fakes; no live calls in CI.
- [ ] A manual live test resolves at least three items per store.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Home Depot parser | Unit | Fixtures with/without aisle |
| Walmart extractor | Unit | Sanitized JSON/DOM fixtures |
| Cache hit | Unit | No network call |
| Rate limiter | Unit | 1 req/sec/store |
| Blocked response | Unit | No retry |
| Alternate query | Unit | No result -> variant |
| Manual precedence | Unit | Manual not overwritten |
| Live lookup | Manual | Real device |

## Manual demo

1. Select a Walmart store and run lookup for three items.
2. Confirm aisle data appears and the written name is unchanged.
3. Open "Not right?" and choose a different candidate or enter manually.
4. Select a Home Depot store and repeat.
5. Disable network and confirm cached results still show.
6. Simulate a blocked response and confirm the error is clear.

## Risks

- Walmart DOM/JSON changes. Mitigation: fixtures, structured extraction, manual override.
- Home Depot store-context flow changes. Mitigation: document and test context extraction.
- Rate limiting/blocking. Mitigation: cache, throttle, stop on block.
- WebView memory leaks. Mitigation: destroy after use and avoid retaining references.

## Open questions

- Which store-context method is most reliable for each retailer after Phase 0?
- Should low-confidence matches auto-select or always require review?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
