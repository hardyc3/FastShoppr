# Phase 0 — Feasibility Report

- **Date:** 2026-11-03
- **Author:** spike session (agent) + workstation verification
- **Status:** user verification partially complete (Walmart, Home Depot, and Target run); handwriting, OCR, and Gemini Nano still untested; date parsing needs refinement
- **Scratch code:** `C:\workspaces\phase0-spike` (Windows side, outside repo, throwaway; not committed)
- **Fixtures:** `docs/spikes/fixtures/` (see `fixtures/README.md` for provenance)
- **Manual verification guide:** `docs/spikes/phase-00-verification.md`

## Environment used

| Item | Value |
|---|---|
| Host | Windows 10 Pro 22H2 + WSL2 (Linux 5.15) |
| JDK (Windows build) | Temurin 21.0.13+9 |
| JDK (WSL) | OpenJDK 21 (JRE only); javac 25 used with `--release 21` for the date-parser scratch |
| Gradle | 8.10.2 (`C:\workspaces\software\gradle-8.10.2`; AGP 8.8.2 requires ≥ 8.10.2) |
| Emulator | `FastShoppr_API_36` (Pixel 9, API 36, Google APIs, x86_64), Android 16 |
| Physical device | none connected; no Gemini Nano-capable device available |
| Network | workstation network; retailer responses described below |

Nothing from the scratch project is required for Phase 1 and none of it is committed. No production project was created.

## 1. Walmart (WebView) — extraction works intermittently; 3/3 items extracted

**Headline: the PX "Robot or human?" wall blocked several sessions, then the same WebView loaded the full site with a working default store context and aisle data.** Automation is not reliable, but the data is reachable.

Timeline, all 2026-11-03:

| Time | Method | Result |
|---|---|---|
| 13:2x | `curl` + browser UA, follow redirects | HTTP 307 → `/blocked`; final HTTP 200 = "Robot or human?" page |
| 13:2x | Emulator WebView probe, `milk`/`eggs`/`paper towels` | All 3 → `/blocked`; `isRobotWall=true`; `hasNextData=false`; 0 aisle/bay matches |
| 13:2x | Emulator WebView pricing probe | Same blocked page; 0 prices |
| 14:11 | Emulator WebView (user-assisted layout, interactive), `milk` | **Loaded.** `hasNextData=true`; `isRobotWall=false`; HTML 2.6 MB; **222 price matches**; aisle JSON found: `displayValue=A29`, `zone=A, aisle=29` |
| 14:15 | Same, `eggs` | **Loaded.** `hasNextData=true`; 199 prices; aisle `AC4` (zone AC, aisle 4) |
| 14:18 | Same, `paper towels` | **Loaded.** `hasNextData=true`; 350 prices; aisle `J29` (zone J, aisle 29) |

The page passes or fails PX non-deterministically across sessions; pass rate here: 3 of 6 WebView sessions. Plain HTTP clients are always blocked. No CAPTCHA solve, login, or challenge bypass was attempted — the passing sessions loaded the normal page.

**Store context:** none was set manually. Walmart assigned a **default store from IP geolocation** (store id redacted; page showed "Pickup or delivery?"). Aisle data was served with that default context. Explicit store selection (ZIP/store pick) was **not** exercised and remains required for a user's real store.

**Extraction mechanism (verified on live pages):**

- Page is a Next.js app; product/fulfillment JSON embedded in `__NEXT_DATA__`.
- Aisle appears as `"Location":[{"displayValue":"A29","aisle":{"zone":"A","aisle":29}}]` (also `LocationDisplayValue`, `bay` absent for these items).
- In `__NEXT_DATA__`, locate `LocationDisplayValue` / `"Location":[{"displayValue"…` — zone + aisle split enables aisle sorting per `02-data-model.md`.
- Size/unit strings (`52 fl oz`, `6 Pack`, `2 LB`) and prices (`$5.32`, `4.2 ¢/fl oz`) are visible in body text; `priceCount=222–350` per search page.
- Result: **aisle/location resolves for at least three real items (milk, eggs, paper towels).** ✅

**Blocking behavior:** PX interstitial, intermittent on WebView; total on plain HTTP. Not a 403/429. Blocking is not URL-specific.

**Foreground service requirement:** yes — WebView must render and a human may need to pass the interstitial. Headless/background extraction is not viable. Even a foreground service does not defeat PX; it only keeps the WebView alive. Unattended lookup remains out of scope.

**Fixtures (trimmed, sanitized, committed):** `walmart/webview-milk.json`, `walmart/webview-eggs.json`, `walmart/webview-paper-towels.json` (UUIDs/tokens/ZIP/store id removed; product links dropped). Full raw captures stay out of git.

**Store-context mechanism:** interactive pickup-store selection is the supported path; default geolocation context may or may not match the user's store — always show the resolved store and offer override (D-017). Unverified beyond default context.

## 2. Home Depot (OkHttp + Jsoup) — blocked; parser verified only against synthetic fixtures

**Result: HTTP 403 / Akamai error page from every automated path, including the interactive WebView.**

| Method | Target | Outcome |
|---|---|---|
| `curl` + custom UA | `/s/milk`, `/s/paper%20towels` | HTTP 403, `server: AkamaiGHost`, 2259-byte error page |
| Emulator OkHttp probe | same URLs | HTTP 403, identical body, `title=Error Page` |
| Emulator WebView (user-assisted layout) | `/s/milk` | **Akamai error page** ("Oops!! Something went wrong. Please refresh page"); `hasNextData=false`; 0 prices; no store context |

Fixtures: `homedepot/blocked.html` (real, curl), `homedepot/blocked-headers.txt` (sanitized), `homedepot/webview-error.json` (real, trimmed).

**Parser prototype (synthetic fixtures only):** `product-aisle.html` → extracted `aisle=12, bay=004` from `__NEXT_DATA__` (`props.pageProps.product.fulfillment.pickup`); `product-no-aisle.html` → correctly reports ship-to-home. Extraction rule recorded: read `__NEXT_DATA__`, take `fulfillment.pickup.{aisle,bay}`; absent ⇒ ship-to-home; never invent values.

**Store context:** user run confirmed that interactive pickup-store selection is required before aisle and bay appear. Cookie vs query-parameter mechanism is still not isolated, but aisle/bay extraction worked after store selection.

**Home Depot verdict:** blocked on the original network, but a 2026-11-04 user run on a real device/network loaded store pages and showed aisle and bay for at least three items. Interactive pickup-store selection remains required; exact aisle/bay values were recorded by the user.

## 3. ML Kit — Digital Ink — works from Java

| Step | Result |
|---|---|
| Model | English (`EN_US`), `DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")` |
| Download | ≈ **67 s** first run (Wi‑Fi); cached afterwards (`model already downloaded: true`) |
| Recognize latency | **536 ms** (3 synthetic strokes) |
| Candidates | 9; top-5 garbage (`E`, `=`, `-`, `I`, `i`) — expected for synthetic scribbles |
| API notes | `com.google.mlkit:digital-ink-recognition:19.0.0`; v19 requires `RecognitionContext.setPreContext("")`; `WritingArea(w, h)` (no direction arg) |

**Limitations:** no human-drawn sample yet; accuracy on real handwriting **unmeasured**. Digital Ink remains a review-mandatory path (D-016). Emulator ≠ device perf.

## 4. ML Kit — Text Recognition v2 — works; poor on cursive

- Bundled v2 (`TextRecognizerOptions.DEFAULT_OPTIONS`) on a synthetic cursive bitmap (`milk`, `eggs`, `paper towels`, `batteries`): latency **4976 ms**; output `nillk | eggo | paper touvels | batteries` — 1/4 exact.
- Camera OCR is best-effort only; mandatory review screen regardless (D-016). Photo capture path added to the verification app (`OCR v2: pick photo of handwritten list`) — user to run on a real handwritten note.

## 5. Gemini Nano (ML Kit GenAI Prompt API) — unverifiable; emulator confirms absence

- Emulator smoke test 2026-11-03: `checkStatus()` → **UNAVAILABLE (0)**; `generateContent()` → `GenAiException [ErrorCode -101] AICore is either not installed or the installed version is too low` (2307 ms). Code path works; failure path documented.
- No supported physical device available; emulator lacks AICore by design. Latency, quota, foreground-only enforcement, and streaming behavior **cannot be measured** here.
- Package confirmed: `com.google.mlkit:genai-prompt:1.0.0-beta5` (Java `GenerativeModelFutures` API used in the scratch app).
- **Phase 5 stance:** deterministic rules fallback is the default and only guaranteed path; Nano strictly optional, never blocking. Verification remains open until a supported phone is tested (steps in `docs/spikes/phase-00-verification.md` §7). No workaround attempted (no sideloaded AICore, no modified images).

## 6. Date-phrase parsing prototype

Pure Java, fixed clock (`2026-11-03`, Tuesday). Implementation: `phase0-spike/dateparser/DatePhraseParser.java`; tests: `DatePhraseParserTest.java` — **19/19 PASS**.

| Phrase | Parsed (from 2026-11-03) | Precision | Rule |
|---|---|---|---|
| `tomorrow` | 2026-11-04 | EXACT | +1 day |
| `in 3 days` | 2026-11-06 | EXACT | today + N days |
| `in 1 day` | 2026-11-04 | EXACT | singular handled |
| `wait a week` / `in a week` | 2026-11-10 | EXACT | +7 days |
| `in 2 weeks` | 2026-11-17 | EXACT | today + N weeks |
| `next week` | 2026-11-09 (next **Monday**) | **NEEDS_CONFIRMATION** | ambiguous: next ISO Monday vs +7 days |
| `next month` | 2026-12-03 (same day) | **NEEDS_CONFIRMATION** | ambiguous: same day vs 1st; month-end clamps (Jan 31 → Feb 28) |
| `this weekend` | 2026-11-07 (Saturday) | **NEEDS_CONFIRMATION** | upcoming Saturday; Sat→today; Sun→next Sat |

**List mapping:** nearest list with `shoppingDate >= parsedDate`; none → prompt to create one (no silent creation). Unparseable input → empty, never a guess.

**Ambiguity:** `next week`, `next month`, `this weekend` parse to the common reading and are flagged `NEEDS_CONFIRMATION`; UI shows resolved date for correction. Exact phrases never prompt.

**User review (2026-11-04):** core parsing behaved as hoped; refinement still needed before production, but no blocker was identified.

## 7. Pricing availability (three stores)

| Store | Method | Local price visible? | Store context? | Size/unit? |
|---|---|---|---|---|
| Walmart | WebView (interactive) | **Yes** — 199–350 `$x.xx` matches per page (e.g. $5.32 milk 52 fl oz; $5.42 Whole Milk 128 fl oz; $23.58 Bounty 6-pack) | Yes (default geo store; "Pickup or delivery?", store redacted) | Yes (`52 fl oz`, `128 fl oz`, `6 Pack`, `2 LB`) |
| Home Depot | curl / OkHttp / WebView | **No** — HTTP 403 / Akamai error page | No | No |
| Target | WebView | **Yes** — 67 price matches (e.g. $11.79–$24.99 paper towels; $4.99); "237 results"; Pickup/2h; store banner (name/ZIP redacted) | Yes (ship-to/store banner rendered) | Yes (`100 Sheets`, `162 Sheets`, …) |

Walmart and Target both render prices **with a store-scoped context**; extraction is identical in kind to the aisle extraction above (WebView + JS). Home Depot exposes nothing without passing Akamai.

**Pricing impact (historical Phase 0 finding; no remaining phase):** automated price extraction is feasible **where pages load** (Walmart — intermittently; Target — after store selection in this test), not viable for Home Depot. A 2026-11-04 Target run also hit a bot/human verification wall, so Target extraction must be assisted/manual rather than unattended. Still: PX makes Walmart automation flaky; keep manual prices as the guaranteed path; treat extraction as optional assist with user override. Do not scrape around blocks.

## 8. Go / No-Go decisions

**Phase 5 (On-device AI): CONDITIONAL GO.**
Digital Ink + Text Recognition v2 both run from Java; models download and cache. Nano unverifiable (emulator: UNAVAILABLE 0; errorCode -101). Ship rules fallback as default; Nano optional; no acceptance criterion may depend on Nano.

**Phase 6 (Store lookup): Walmart CONDITIONAL GO (assisted); Home Depot NO-GO.**
- Walmart: 3/3 items extracted on emulator once pages loaded (milk→A29, eggs→AC4, paper towels→J29); prices and store context visible. Requires user-visible foreground WebView, pass-the-wall tolerance, manual override, cached results, `__NEXT_DATA__` parsing. Not for unattended use. D-022 records this.
- Home Depot: no automated path works (403 everywhere incl. WebView). Ships manual-entry-first; HTTP lookup stays dead until a real-device/mobile-data pass retrieves aisle pages. D-021 stands for HD.

**Pricing (historical Phase 0 finding; phase removed): CONDITIONAL GO for Walmart/Target via WebView; NO-GO for Home Depot; manual prices remain the fallback.**
Automation is flaky by design (PX); user-initiated WebView with cached results is the ceiling. Do not build headless scraping.

## Non-goals confirmed

No production app code, no Gradle project in the repo, no dependencies added anywhere, no secrets/credentials committed, no personal data in fixtures (UUIDs/tokens/ZIP/store ids redacted; store identities fictional). Scratch code lives outside the repo.

## Open questions (from phase file)

- **Which Walmart store/ZIP?** Used an IP-geolocated default store (not chosen; redacted). A user store selection flow was not exercised — on the user's device, pick the store once and verify aisle/location matches the shelf.
- **Which Home Depot store?** None reachable; page never loaded. Needs real device + mobile data.
- **Gemini Nano-capable device?** No. Emulator: UNAVAILABLE (0). On-device steps documented for user.
- **Fixtures in app test resources?** No — keep under `docs/spikes/fixtures/`; move in Phase 2.

## Known limitations carried forward

1. Walmart PX pass is intermittent (≈50% of sessions here); never unattended; user review required; fixtures must be re-verified on the user's device/store.
2. Home Depot: no live aisle data; manual entry is the only guaranteed path.
3. Gemini Nano: untested on hardware; AI path is rules-only in practice.
4. OCR accuracy on handwriting poor (cursive 1/4 exact); review mandatory.
5. Digital Ink untested on human handwriting; one-time model download ≈ 67 s.
6. Date parser handles the requested phrase set only; unlisted phrases return empty.

## Manual demo checklist (phase demo steps)

| Demo step | Status |
|---|---|
| 1. Open this report | ✅ |
| 2. Walmart/Home Depot fixtures + extracted aisle/bay | ✅ Walmart: 3 items extracted (A29 / AC4 / J29), fixtures committed. ❌ Home Depot: blocked everywhere; synthetic-fixture parse only (aisle 12 / bay 004) |
| 3. Digital Ink recognition + camera OCR | ✅ Digital Ink (download + 536 ms). ✅ OCR v2 (poor cursive; photo path added for user) |
| 4. Gemini Nano availability + rules fallback | ⚠️ Emulator: UNAVAILABLE (0), errorCode -101. Rules fallback documented; device check pending |
| 5. Date-phrase parsing table | ✅ 19/19 unit checks |
| 6. Price-availability findings | ⚠️ Walmart & Target: prices visible with store context; Target also hit a bot/human verification wall; Home Depot: none on the original network, aisle/bay visible in user run |

**Phase status:** acceptance criteria for Walmart extraction are met; Home Depot extraction and on-device Gemini Nano verification are not. Phase cannot be `done` until the user runs `docs/spikes/phase-00-verification.md` (or explicitly waives those criteria).

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-03 | `curl` Walmart search | 307 → PX blocked page | fixture saved |
| 2026-11-03 | Walmart WebView ×3 (automated) | BLOCKED | fixture `webview-blocked.txt` |
| 2026-11-03 | Walmart WebView (interactive) ×3 | **PASS** | milk→A29, eggs→AC4, paper towels→J29; prices visible; `webview-*.json` |
| 2026-11-03 | `curl` Home Depot search ×2 | HTTP 403 Akamai | blocked fixture |
| 2026-11-03 | Home Depot OkHttp (emulator) ×2 | HTTP 403 | 2259 B body |
| 2026-11-03 | Home Depot WebView (interactive) | BLOCKED | Akamai error page; `homedepot/webview-error.json` |
| 2026-11-03 | Emulator: Digital Ink EN_US download + recognize | PASS | ≈67 s; 536 ms; synthetic ink |
| 2026-11-03 | Emulator: Text Recognition v2 (cursive bitmap) | PASS w/ poor accuracy | `nillk|eggo|paper touvels|batteries` |
| 2026-11-03 | Emulator: Pricing probe ×2 | Walmart/Target: prices visible; HD: none | fixtures saved |
| 2026-11-03 | Emulator: Nano checkStatus | UNAVAILABLE (0) | AICore absent; `nano-status.txt` |
| 2026-11-03 | Emulator: Nano prompt | FAIL −101 | "AICore not installed or too low"; rules fallback required |
| 2026-11-03 | Date-parser scratch, fixed clock | 19/19 PASS | `DatePhraseParserTest` |
| 2026-11-03 | Fixture sanitization scan (uuids/cookies/tokens/ZIP/store id) | PASS | redactions applied; README lists provenance |
| 2026-11-03 | Verification app + guide for user device run | CREATED | `docs/spikes/phase-00-verification.md` |
