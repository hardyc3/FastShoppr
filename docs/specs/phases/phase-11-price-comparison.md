# Phase 11 — Price Comparison

- **Status:** `not-started`
- **Depends on:** Phase 10
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-13

## Goal

Let users compare item prices across their local stores, with automated store-page prices where approved and manual prices everywhere else.

## Context

Phase 10 determines which stores can provide usable local prices. This phase implements the post-MVP comparison experience without weakening the routing MVP.

## In scope

- `PriceQuote` entity, DAO, and repository.
- Custom store CRUD.
- Manual price entry and edit.
- Automated price extraction for stores approved in Phase 10.
- Price cache and refresh.
- Compare-prices UI.
- Cheapest-store suggestion per item.
- Source/timestamp/staleness display.
- Tests for comparison, caching, and UI.

## Out of scope

- Coupons, loyalty pricing, or membership prices.
- Phase 0 findings (D-021/D-022): prices visible on Walmart/Target WebView sessions; Home Depot blocked; automation is never unattended; manual prices remain the guaranteed path.
- Checkout or ordering.
- Receipt scanning.
- Price history charts.
- Sharing or authentication.
- Real-time price guarantees.

## Requirements

### Data

- `PriceQuote` fields from `08-pricing-and-custom-stores.md`.
- Indexes on `storeId + normalizedQuery` and `itemId`.
- Manual quotes persist until edited/deleted.
- Automated quotes expire after 24 hours.
- Store currency as `USD` for MVP.

### Custom stores

- Add, edit, archive custom stores.
- Fields: name, address, optional coordinates, optional hours.
- Custom stores support manual aisle and manual price.
- No automated lookup for custom stores.
- Deleting a referenced store is blocked; archive instead.

### Manual prices

- Enter price, optional size, and store.
- Show source "Manual" and observation time.
- Manual price wins over automated price for that item/store.
- Validate positive price and currency.

### Automated prices

- Only for stores approved in Phase 10.
- Reuse store context, rate limits, and block handling.
- Extract price, product name, size, and URL where available.
- Do not infer prices from unrelated products.
- Cache for 24 hours.
- Stop on block responses.

### Comparison

- Compare only comparable sizes; otherwise show both with a size note.
- Ties show all tied stores.
- Stale prices are marked.
- One known price is shown without a "cheapest" claim.
- No known price shows "No price data".
- The cheapest suggestion is informational; the user can choose any store.

### UI

- Price badge on item rows.
- "Compare prices" sheet.
- Manual price entry dialog.
- Custom store settings screen.
- Stale/source/timestamp labels.
- Empty/error states.
- Accessibility labels.

## Technical approach

- `data/local/PriceQuoteEntity.java`, `PriceQuoteDao.java`.
- `data/repository/PriceRepositoryImpl.java`.
- `data/remote/PriceExtractor.java` with per-store implementations.
- `domain/usecase/ComparePricesUseCase.java`.
- `domain/usecase/SaveManualPriceUseCase.java`.
- `ui/listdetail/ComparePricesBottomSheet.java`.
- `ui/settings/CustomStoresFragment.java`.
- Use Gson for embedded JSON and Jsoup for HTML.
- Use `ExecutorService` and `LiveData`.

## Data changes

- Add `PriceQuote` table and migration.
- Add custom store fields if not already present.
- Migration test required.

## UI changes

- Price badge and compare sheet.
- Manual price dialog.
- Custom store CRUD screens.
- Settings entries.
- Strings and accessibility labels.

## Acceptance criteria

- [ ] Users can add, edit, and archive custom stores.
- [ ] Users can enter and edit manual prices.
- [ ] Automated prices work for stores approved in Phase 10.
- [ ] Prices show source, timestamp, and staleness.
- [ ] Cheapest suggestions are correct for comparable sizes.
- [ ] Size mismatches do not declare a winner.
- [ ] Ties show all tied stores.
- [ ] Manual prices win over automated prices.
- [ ] Missing prices do not block routing or checkoff.
- [ ] Price cache expires correctly.
- [ ] Blocked responses stop extraction with a clear message.
- [ ] No live price calls in CI.
- [ ] Migration and comparison tests pass.
- [ ] No regression in MVP flows.
- [ ] Every test in the test plan has run and passed, including `connectedDebugAndroidTest` on the simulated device (emulator); no test has failed, errored, or been skipped.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Comparison rules | Unit | Ties, stale, size mismatch |
| Manual price | Unit/Espresso | Save/edit |
| Custom store CRUD | Espresso | Add/archive |
| Price parser | Unit | Sanitized fixtures |
| Cache expiry | Unit | 24 hours |
| Migration | Room | Version 2 -> 3 (adds `PriceQuote`) |
| Compare sheet | Espresso | Display and selection |

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored, been skipped, or failed to run. That includes every test in the test plan and `./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator) or a physical device. An unavailable device is not a pass: leave the phase `in-progress` or `blocked` and record the unrun test in the completion log.

## Manual demo

1. Add a custom local store.
2. Add manual prices for an item at two stores.
3. Open "Compare prices" and confirm the cheapest is highlighted.
4. Add a different size and confirm no winner is declared.
5. Refresh an automated price for an approved store.
6. Confirm stale/source/timestamp labels.
7. Delete all price data and confirm no routing regression.

## Risks

- Price extraction may break. Mitigation: manual override and per-store go/no-go.
- Unit-price comparison can mislead. Mitigation: size notes and no winner on mismatch.
- Stale prices. Mitigation: timestamps, expiry, and refresh.
- Added scope destabilizing the MVP. Mitigation: keep pricing behind a separate feature path and regression tests.

## Open questions

- Should prices appear in the shopping list or only in a compare sheet?
- Should the app remember the last selected store for manual prices?
- Should price history be added later?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
