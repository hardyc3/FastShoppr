# Store Integration Specification

## Scope

MVP automated lookup supports:

| Store | Method | Store context | Status |
|---|---|---|---|
| Walmart | WebView extraction | Branch picked in WebView | Phase 6; Phase 10 regression passed |
| Home Depot | Search-first HTTP → first query match → aisle/bay | Branch picked in WebView | Phase 10 follow-up (D-055) |
| Lowe's | Search-first HTTP → first query match → aisle | Branch picked in WebView | Phase 10 follow-up (D-055) |
| Smiths | Search-first HTTP → first query match → aisle | Branch picked in WebView | Phase 10 follow-up (D-055) |
| Target | Removed | — | No public aisle; stored rows archived as custom (D-056) |
| Custom stores | Manual aisle only | — | No automated lookup |

Manual aisle entry is always available and must never depend on a successful lookup. Home Depot, Lowe's, and Smiths show aisles only after a specific search result is opened, so the app follows the first query-matching same-retailer product result and records that trade-off (D-055). A blocked page, missing product link, or missing aisle produces `Blocked`/`NotFound` with manual fallback. Walmart remains the foreground WebView lookup and passed the regression smoke. Target was removed because it exposes no public aisle (D-056).

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

## Branch picker (Phase 9)

Settings has a brand selector and a **Pick branch** button. Tapping **Pick
branch** opens that brand's public store-locator page in a WebView. The user
navigates the page, selects and confirms a branch, and the app saves one branch
per brand. First use of a store requires a branch pick; Settings can change it
later. The saved branch is displayed on the Organize screen for brands present
on the list. It is **display context only**: it never rewrites `rawText`, never
replaces an aisle, and does not claim that an automated result was proven
against that branch.

Supported locator brands and public pages (documented assumptions, not
guaranteed APIs):

| Brand | Public page |
|---|---|
| Walmart | `https://www.walmart.com/store/finder` |
| Home Depot | `https://www.homedepot.com/l/store-locator` |
| Lowe's | `https://www.lowes.com/store` |
| Smiths | `https://www.smithsfoodanddrug.com/stores/search` |

Custom stores have no automated locator and no **Pick branch** action.

Rules:

- Save only the selected branch name and street/city/state metadata in
  SharedPreferences, one value per brand. Do not store a ZIP, coordinates, or a
  locator cache; discard postal codes parsed from a retailer page before saving
  or displaying the branch.
- Do not request location permission and do not add a location provider
  dependency.
- Do not fetch, parse, rank, or cache locator responses over HTTP. The WebView
  is the only locator surface and the user drives selection.
- Set the WebView user agent to identify FastShoppr. Restrict navigation to the
  selected brand's public locator domain over HTTPS; expose no
  `JavascriptInterface` and no app data to page scripts.
- If the page is blocked or unavailable, show a clear message and let the user
  retry from Settings. Manual aisle entry remains available.
- **Delete all data** clears all saved branches.

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

- Run inside a foreground service with a visible persistent notification during lookup runs (Phase 7). Until then, lookups run only as explicit, user-initiated in-app actions (foreground harness).
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

- Use the branch saved for Walmart via **Pick branch**. If none is saved, open
  the branch picker before the lookup.
- The branch can be changed in Settings.
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

Home Depot product pages were expected to show in-store availability such as `Aisle 52, Bay 016`. Phase 10 re-verification on 2026-11-25 found HTTP 403 with the identifying user agent and an error page in the emulator browser, so the live HTTP path stays dormant and the app reports `Blocked` with a manual-aisle fallback (D-053).

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

- Use the branch saved for Home Depot via **Pick branch**; open the branch
  picker first if none is saved.
- Persist the saved branch metadata and any required cookies for the session.
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

The app must know which physical store's layout to query. One generic WebView
branch picker supplies that context for Walmart, Home Depot, Lowe's, and Smiths
(D-043); Phase 10 verified it for the original five brands, then removed Target
under D-056. Walmart keeps the automated WebView flow documented above. Home
Depot, Lowe's, and Smiths use the search-first public-page flow in D-055. The
saved branch is explicit user context; it is not silently applied as a lookup
result or treated as proof of an aisle.

Priority order for a user-visible store context:

1. Branch explicitly saved for the brand in **Settings → Pick branch**.
2. No branch saved: open the WebView branch picker when the brand is first added
   to a list or when Organize first uses that brand.

**Explicit selection flow:**

- Choose the brand in Settings and tap **Pick branch**.
- The app opens that brand's public store-locator page in a WebView.
- The user navigates the page, selects a branch, and confirms it.
- The app saves one branch per brand and shows it in Settings and on the
  Organize screen when that brand is present on the list.

Rules:

- First use of a store requires picking its branch; adding a supported brand to
  a list opens the brand-prefilled picker when no branch is saved. Settings can
  change the saved branch.
- Save only branch name and address metadata. No ZIP, coordinates, location
  permission, HTTP locator parsing, or locator cache.
- Show the saved store name and address so the user can verify it.
- If the locator page is blocked or unavailable, show a clear message; manual
  aisle entry remains available.
- Saved branches never change `rawText` or aisles.
- **Delete all data** clears all saved branches.

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

## Auto-organize flow

When the user taps "Organize list" from the list detail screen:

1. Navigate to the lookup progress screen.
2. If a saved branch exists for the brand, start the lookup automatically.
3. If no branch is saved for a brand on the list, open the generic WebView
   branch picker for that brand. After the user selects and confirms a branch,
   return to the lookup screen and start the lookup automatically.
4. Changing the branch in Settings takes effect on the next organize run.
5. **Delete all data** clears saved branches; the next organize run prompts for
   a branch again.

This minimizes button clicks: the user picks a branch only on first use of a
store or when changing it in Settings. No separate "Verify store" or "Start
lookup" button is needed on the normal path.
