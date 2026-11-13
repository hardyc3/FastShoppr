# Home Depot fixtures (synthetic/sanitized)

These are synthetic, sanitized examples for unit parsing tests.
No live capture data, cookies, tokens, or personal information is included.

- Capture date: 2026-11-12 (synthetic for tests)
- URL shape: `https://www.homedepot.com/p/<product>` or search page content
- Store context: example selected store (not embedded)
- Expected extraction:
  - `product-aisle-bay.html` -> aisle `52`, bay `016`
  - `product-no-aisle.html` -> no aisle shown (NotFound)
  - `blocked.html` -> blocked (403/access denied)
