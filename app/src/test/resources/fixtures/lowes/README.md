# Lowe's fixtures (sanitized observation)

These are sanitized summaries of the public page state observed on 2026-11-25.
No live capture data, cookies, tokens, reference IDs, or personal information is
included.

- URL shape: `https://www.lowes.com/pd/<product>/<id>`
- Store context: none embedded; public product page only, no selected branch applied
- Observed states from HTTP and `emulator-5554` Chrome:
  - `access-denied.html` -> Akamai `Access Denied`; no aisle shown.
- Phase 10 decision: no automated lookup is added; report `Blocked` and keep
  manual aisle entry. See D-053.
