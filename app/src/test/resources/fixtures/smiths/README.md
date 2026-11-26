# Smiths fixtures (sanitized observation)

These are sanitized summaries of the public page state observed on 2026-11-25.
No live capture data, cookies, tokens, or personal information is included.
The saved branch summary shown by the site was not copied into the fixture.

- URL shape: `https://www.smithsfoodanddrug.com/p/<product>/<upc>`
- Store context: none embedded; public product page only, no selected branch applied
- Observed states from `emulator-5554` Chrome:
  - `product-no-public-aisle.html` -> product page loads with availability and
    price notes, but no public in-store aisle or bay.
  - The HTTP client timed out for the same page; no HTTP parser is added.
- Phase 10 decision: no automated lookup is added; report `NotFound` with a
  manual-aisle message. See D-053.
