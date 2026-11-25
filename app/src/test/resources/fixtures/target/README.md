# Target fixtures (sanitized observation)

These are sanitized summaries of the public page state observed on 2026-11-25.
No live capture data, cookies, tokens, verification IDs, or personal information
is included.

- URL shape: `https://www.target.com/p/<product>/-/A-<id>`
- Observed states from static HTML and `emulator-5554` Chrome:
  - `human-verification.html` -> bot challenge; static product HTML contains no
    public aisle or bay.
- Phase 10 decision: no automated lookup is added; report `Blocked` and keep
  manual aisle entry. See D-053.
