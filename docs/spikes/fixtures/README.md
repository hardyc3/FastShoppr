# Phase 0 Fixtures

Provenance for every fixture. Nothing here contains cookies, tokens, addresses, or personal data.

| File | Source | Captured | Status |
|---|---|---|---|
| `walmart/blocked.html` | Live capture via curl; Walmart returned HTTP 200 with a PX "Robot or human?" page after redirect from `/search?q=milk` | 2026-11-03 | Real capture, UUIDs redacted |
| `walmart/webview-blocked.txt` | Live WebView capture on API 36 emulator; `milk`, `eggs`, `paper towels` searches all redirected to PX "Robot or human?" page | 2026-11-03 | Real capture, UUIDs redacted |
| `walmart/product-location.json` | Reconstructed minimal `__NEXT_DATA__`-style payload showing where aisle/location would live | 2026-11-03 | SYNTHETIC — structure reference only |
| `homedepot/blocked.html` | Live capture via curl; HTTP 403 Akamai bot-challenge error page (2259 bytes) | 2026-11-03 | Real capture |
| `homedepot/blocked-headers.txt` | Response status line + server header only | 2026-11-03 | Real capture, sanitized (cookies/tokens/IPs/timing removed) |
| `homedepot/product-aisle.html` | Reconstructed product page with `__NEXT_DATA__` JSON containing `fulfillment.pickup.aisle` / `bay` | 2026-11-03 | SYNTHETIC — structure reference only |
| `homedepot/product-no-aisle.html` | Reconstructed product page, ships-to-home, no aisle fields | 2026-11-03 | SYNTHETIC — structure reference only |
| `pricing-probe.txt` | Live WebView capture, Walmart/Home Depot/Target search pages, price extraction attempt | 2026-11-03 | Real capture, UUIDs redacted |
| `walmart/webview-milk.json` | TRIMMED WebView capture; aisle A29 extracted (zone A, aisle 29); 222 price matches | 2026-11-03 | Real capture, trimmed/sanitized |
| `walmart/webview-eggs.json` | TRIMMED WebView capture; aisle AC4 extracted | 2026-11-03 | Real capture, trimmed/sanitized |
| `walmart/webview-paper-towels.json` | TRIMMED WebView capture; aisle J29 extracted | 2026-11-03 | Real capture, trimmed/sanitized |
| `homedepot/webview-error.json` | TRIMMED WebView capture; Akamai error page rendered ("Oops!! Something went wrong") | 2026-11-03 | Real capture, trimmed/sanitized |
| `target-paper-towels.json` | TRIMMED WebView capture; 67 price matches; store context visible | 2026-11-03 | Real capture, trimmed/sanitized |

Live retailer pages could not be captured as aisle-bearing fixtures:

- Home Depot returns HTTP 403 (Akamai) to plain HTTP clients from the test network, and its WebView page shows the same error page.
- Walmart redirects both plain HTTP clients and the emulator WebView to a "Robot or human?" page (see `walmart/blocked.html`, `walmart/webview-blocked.txt`).

Synthetic fixtures encode the documented/assumed page structure for parser prototyping only.
Parser output from them proves the extraction logic works, not that the live pages still
contain this structure. Re-verify against live pages on-device before Phase 6 ships.
