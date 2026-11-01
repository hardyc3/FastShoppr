# Store Integration Specification

## Scope

MVP automated lookup supports:

| Store | Method | Store context | Status |
|---|---|---|---|
| Walmart | WebView extraction | Selected store / ZIP | Phase 6 |
| Home Depot | OkHttp + Jsoup | Selected store / ZIP | Phase 6 |
| Lowe's | Manual aisle only | — | Later |
| Target | Manual aisle only | — | Later |
| Smiths | Manual aisle only | — | Later |
| Custom stores | Manual aisle only | — | Post-MVP |

Manual aisle entry is always available and must never depend on a successful lookup.

## Access policy

- Query only public retailer pages.
- Set a descriptive user agent that identifies FastShoppr.
- Limit to one request per second per store.
- Cache successful results for 60 days.
- Do not retry aggressively after a block.
- Stop on HTTP 403, 429, CAPTCHA, or bot challenge and show a clear message.
- Never bypass authentication, paywalls, or access controls.
- Never crawl entire catalogs or run lookups for items the user did not enter.
- Record assumptions in this spec and keep sanitized fixtures for tests.

## Lookup contract

```java
public interface StoreLookupService {
    StoreBrand getBrand();
    CompletableFuture<StoreLookupResult> lookup(StoreLookupRequest request);
}

public final class StoreLookupRequest {
    private final String rawText;
    private final String displayName;
    private final String normalizedText;
    private final Store store;
    // constructor and getters omitted
}

public sealed interface StoreLookupResult permits Found, Multiple, NotFound, Blocked, Error {

    record Found(
            String aisle,
            String bay,
            String productName,
            String productUrl,
            Float confidence,
            LookupSource source
    ) implements StoreLookupResult {}

    record Multiple(List<Found> candidates) implements StoreLookupResult {}

    record NotFound(String reason) implements StoreLookupResult {}

    record Blocked(String reason) implements StoreLookupResult {}

    record Error(AppError error) implements StoreLookupResult {}
}
```

The repository decides whether to use cache, network, or a manual override. The UI never calls a store client directly.

## Query normalization

- Lowercase with `Locale.US`.
- Strip punctuation except digits and letters.
- Collapse whitespace.
- Remove leading list markers such as `-`, `*`, `1.`.
- Keep size and quantity words in the query when they may affect matching.
- Generate alternate queries for retries:
  1. Normalized display name.
  2. Display name without quantity/size tokens.
  3. Singularized last word when a simple rule applies.
  4. First two meaningful words.
- Stop after at most three query variants per item per run.
- Do not invent brands or product categories.

## Walmart WebView integration

Walmart is JavaScript-heavy and does not reliably expose aisle data in static HTML. Use a WebView-based extractor.

### Requirements

- Run inside a foreground service with a visible persistent notification.
- Show progress such as "Checking Walmart: 3 of 12 items".
- Use a dedicated WebView instance; do not reuse a user-visible browser tab.
- Enable JavaScript and DOM storage.
- Set the selected store context before searching.
- Search the user's item, wait for results, and extract aisle/store availability data from the rendered DOM or page JSON.
- Prefer structured JSON embedded in the page over brittle CSS selectors.
- Inject JavaScript through `evaluateJavascript` and parse the returned JSON with Gson.
- Cap each item lookup with a timeout (default 20 seconds).
- Destroy the WebView when the run completes.
- Do not run WebView work in a plain `Worker`.

### Store context

- Determine the nearest store from the last known location or the user's selected store.
- Allow manual override in settings.
- If store context cannot be established, return `NEEDS_REVIEW` rather than guessing.
- Do not silently use a store from another city or state.

### Failure behavior

- If Walmart shows a CAPTCHA, sign-in wall, or block page, stop the run and surface `Blocked`.
- If the DOM changes, return `ParserFailed`; keep the item unresolved.
- If the item is online-only or not stocked locally, return `NotFound` with a clear reason.
- Cache successful aisle results keyed by store and query.

### Testing

- Keep sanitized HTML/JSON fixtures from Phase 0.
- Unit-test the extractor against fixtures without a WebView.
- Provide one manual instrumentation test or a debug-only harness for live extraction.
- Never run live tests in CI by default.

## Home Depot HTTP integration

Home Depot product pages publicly show in-store availability such as `Aisle 52, Bay 016`.

### Requirements

- Use OkHttp with a descriptive user agent.
- Resolve the selected store context first.
- Search the public site for the item, then fetch the best matching product page.
- Parse with Jsoup and/or structured JSON embedded in the page.
- Extract aisle, bay, product title, product URL, and availability text.
- Prefer the selected store's availability block over shipping-only content.
- Cap at three candidate pages per item.
- Timeout requests after 15 seconds.
- Rate limit to one request per second per store.

### Store context

- Determine the store from the user's selected store or nearest location.
- Persist the store identifier and any required cookies for the session.
- If the page does not show a local aisle, return `NotFound` or `NEEDS_REVIEW`.
- Do not infer an aisle from a different store.

### Failure behavior

- HTTP 403/429 -> `Blocked`; stop the run.
- Page shape changed -> `ParserFailed`.
- No aisle on an otherwise valid product -> `NotFound` with reason "No in-store aisle shown".
- Network failure -> retry with backoff, then `Error`/`NetworkUnavailable`.

### Testing

- Sanitized fixtures for product pages with aisle, without aisle, and blocked pages.
- Unit tests for parsing, store context extraction, and candidate selection.
- Optional gated live test that runs only when an environment flag is set.

## Store context

The app must know which physical store's layout to query.

Priority order:

1. Store explicitly selected by the user for the item or list.
2. Last used store.
3. Nearest store from a recent location fix.
4. Manual ZIP/store selection prompt.

Rules:

- Request location permission only when the user chooses "use my location".
- If permission is denied, use the last selected store or ask for a ZIP code.
- Show the store name and address in the list UI so the user can verify it.
- Never log coordinates.

## Caching

- Cache key: `storeId + normalizedQuery`.
- TTL: 60 days.
- Cache successful `Found` results only.
- Do not cache `Blocked`, `ParserFailed`, or transient `Error` results.
- Allow the user to refresh a single item or an entire store section.
- Show a subtle "cached" indicator when a result is older than the TTL but still usable offline.
- Manual overrides take precedence over cache and are stored as manual results.

## Retries and alternates

- Retry transient network errors twice with exponential backoff.
- Do not retry blocked responses.
- Use alternate query variants only after the primary query returns no result.
- If multiple plausible products are found, return `Multiple` and mark the item `NEEDS_REVIEW`.
- If the best match is low confidence, keep the result but mark `NEEDS_REVIEW`.
- The user can accept a candidate, edit the item, or enter an aisle manually.

## Preserving the written name

- `rawText` is never changed by a lookup.
- `matchedProductName` is stored and shown below the written name.
- The UI shows a "Not right?" action that opens candidate selection or manual aisle entry.
- Accepting a match updates only the aisle/bay/match fields, never `rawText`.

## Fixtures

Store fixtures live under:

```text
app/src/test/resources/fixtures/walmart/
app/src/test/resources/fixtures/homedepot/
```

Rules:

- Sanitize cookies, tokens, account data, addresses, and scripts that are not needed for parsing.
- Keep a small README in each fixture directory describing the capture date, URL shape, store context, and expected extraction.
- Update fixtures when a parser changes; do not delete failing fixtures to make tests pass.

## Observability

- Log only item counts, store brand, duration, and result status.
- Never log item text, product URLs with personal parameters, cookies, or coordinates.
- Surface a per-item status in the UI: pending, found, needs review, not found, error.
