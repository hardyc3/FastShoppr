# Retail page fixtures

Synthetic fixtures for the search-first aisle parser. No personal data, cookies,
or tokens are included. They model the page states observed on 2026-11-25 via
public-page probes and the emulator browser.

- Capture date: 2026-11-25 (synthetic for tests)
- URL shapes: `/s/<query>` (Home Depot), `/search?searchTerm=<query>` (Lowe's and Smiths),
  then a product page under `/p/` or `/pd/`
- Store context: example selected branch in visible text only; not a real location
- Expected extraction:
  - `homedepot-search.html` → first product URL `https://www.homedepot.com/p/Milk-Crate/204793156`
  - `homedepot-search-error.html` → blocked (observed Home Depot error page shape)
  - `lowes-search.html` → first product URL on `lowes.com` under `/pd/`
  - `lowes-product-aisle.html` → aisle `14`
  - `smiths-search.html` → first product URL on `smithsfoodanddrug.com` under `/p/` after skipping the unrelated turkey carousel item
  - `smiths-product-aisle.html` → aisle `A12`
  - `search-carousel.html` → query `milk` selects the milk product, not the first carousel item
  - `../lowes/access-denied.html` → blocked
  - `../smiths/product-no-public-aisle.html` → not found
  - `search-no-product.html` → no product link (not found)
