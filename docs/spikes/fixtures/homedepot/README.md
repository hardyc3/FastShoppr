# Home Depot fixtures

Capture date: 2026-11-02
URL shapes:

- `https://www.homedepot.com/` -> `403 Access Denied` (Akamai)
- `https://www.homedepot.com/p/Scott-White-Toilet-Tissue-1000-Sheet-12-Rolls-Per-Pack-10060/312631958` -> `403`
- `https://www.homedepot.com/p/Charmin-Essentials-Soft-Toilet-Paper-Rolls-18-Mega-Rolls-003077204537/321303444` -> `403`

Store context: the rendered observations were for a public store. The store name and ZIP were
replaced with `[STORE]` in committed excerpts.

Expected extraction:

- Aisle-present rendered text -> aisle `53`, bay `009`, product name `Scott White Toilet Tissue ...`.
- Aisle-absent rendered text -> `NotFound` or `NEEDS_REVIEW`; never infer an aisle.
- Blocked page -> `Blocked`; stop the run.

Missing fixtures: raw aisle-present and aisle-absent HTML. Direct HTTP requests were blocked, so
the committed observations are text excerpts only and cannot exercise Jsoup. Capture real raw
HTML from a device/network where public product pages are reachable before writing parser tests.
