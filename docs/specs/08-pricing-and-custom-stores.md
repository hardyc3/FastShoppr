# Pricing and Custom Stores Specification

## Status

Post-MVP. Implemented in Phases 11–12 after the local routing MVP is released and Phase 10 completes store lookup coverage.

## Goals

- Let users compare the price of an item across a few local stores.
- Support user-defined local stores that have no automated lookup.
- Prefer public store-page prices where available, with manual override.
- Keep price data local and clearly timestamped.
- Do not block list creation or routing when price data is missing.

## Scope

In scope:

- Custom store CRUD (name, address, optional coordinates).
- Manual price entry per item/store.
- Optional store-page price extraction for supported chains.
- Cheapest-store suggestion per item.
- Price cache with expiry.
- Price source and observation time shown in the UI.
- Store sections continue to work without prices.

Out of scope:

- Real-time price guarantees.
- Coupons, loyalty pricing, or membership prices.
- Checkout or order placement.
- Receipt scanning (deferred unless added later).
- Price history charts.
- Cross-store unit-price normalization beyond a simple documented rule.

## Custom stores

- A custom store has `isCustom = true`.
- The user can add a name, address, optional coordinates, and optional hours.
- Custom stores support manual aisle and manual price entry.
- No automated lookup is attempted for custom stores.
- Custom stores appear in the store picker and as list sections.
- Deleting a custom store is blocked while items reference it; archive instead.
- Custom store data stays on the device.

## Price sources

Priority:

1. Manual price entered by the user.
2. Fresh store-page price.
3. Cached store-page price, clearly marked stale.
4. No price; show "Price unknown".

Rules:

- Manual prices always win over automated prices for that item/store.
- Automated prices are location-specific; use the selected store context.
- Do not compare prices from different package sizes without showing the size.
- Store the currency code; MVP assumes `USD`.
- Show the observation timestamp and source.
- Do not claim a price is current after its expiry.

## Price extraction

- Only public store pages.
- Reuse the store-context and rate-limit rules from `03-store-integration.md`.
- Extract price, product name, size, and URL where available.
- If the page does not show a local price, return unknown.
- Do not infer a price from search snippets or unrelated products.
- Cache successful prices for a short TTL (default 24 hours).
- Stop on block responses and surface a clear message.
- Do not scrape prices in bulk; only for items the user entered.

## Comparison logic

For each item with two or more known prices:

- Compare the total price for the same size/quantity when known.
- If sizes differ, show both prices and a size note; do not declare a winner.
- If a price is stale, mark it and allow comparison with a warning.
- If only one price is known, show it without a "cheapest" claim.
- If no prices are known, show "No price data".
- Ties show all tied stores.
- The cheapest suggestion is informational; the user can still choose any store.

## PriceQuote model

```text
PriceQuote
  id: String (PK)
  storeId: String (indexed)
  itemId: String?
  normalizedQuery: String
  priceMinorUnits: Long
  currencyCode: String
  productName: String?
  productSize: String?
  productUrl: String?
  source: PriceSource
  observedAt: Long
  expiresAt: Long
```

Indexes:

- `storeId + normalizedQuery`
- `itemId`

## UI

- List detail shows a price badge per item when known.
- A "Compare prices" action opens a sheet with stores, prices, sizes, source, and age.
- Manual price entry is available from the item menu.
- Custom store management lives in settings.
- The cheapest store is highlighted, not auto-selected.
- The user can filter the list by store or price availability.
- Price failures never block checking off items.

## Caching and refresh

- Cache successful price quotes for 24 hours.
- Allow manual refresh per item or per store.
- Respect store rate limits.
- Do not refresh all items at once.
- Delete expired quotes during periodic cleanup.
- Keep manual quotes until the user edits or deletes them.

## Risks

- Retailer pages may not expose local prices without a selected store.
- Prices may be membership-only, online-only, or location-specific.
- DOM changes can break extraction.
- Unit-price comparison can be misleading.
- Price data can become stale quickly.

Mitigations:

- Phase 11 spike validates each store before implementation.
- Manual override is always available.
- Show timestamps and sources.
- Do not promise savings or real-time accuracy.

## Phase 11 spike criteria

Before Phase 12, document for each target store:

- Whether a local price is visible on a public page.
- Whether store context is required and how it is set.
- Whether size/unit is visible.
- Sample sanitized fixtures.
- Failure modes and rate-limit behavior.
- Go/no-go for automated extraction.

If a store fails the spike, support manual prices only for that store.

## Testing

- Unit tests for comparison rules, tie handling, stale prices, and size mismatch.
- DAO tests for quote caching and expiry.
- Parser tests with sanitized fixtures.
- UI tests for manual price entry, compare sheet, and custom store CRUD.
- No live price tests in CI.

## Acceptance summary

- Users can add custom local stores.
- Users can enter and compare prices.
- Cheapest suggestions are accurate for comparable sizes.
- Stale and manual prices are clearly labeled.
- Missing prices do not block routing or shopping.
