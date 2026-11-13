# Walmart fixtures (synthetic/sanitized)

These are synthetic, sanitized examples shaped like Phase 0 findings for unit parsing tests.
No live capture data, cookies, tokens, or personal information is included.

- Capture date: 2026-11-12 (synthetic for tests)
- URL shape: `https://www.walmart.com/search?q=<query>` rendered page containing `__NEXT_DATA__`
- Store context: example local store context (not embedded as personal data)
- Expected extraction:
  - `next-data-aisle-a29.json` -> aisle/location `A29`, product "Great Value Milk"
  - `next-data-multiple.json` -> multiple candidates including `AC4` and `J29`
  - `blocked.html` -> blocked/challenge page
