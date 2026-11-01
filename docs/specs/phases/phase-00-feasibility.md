# Phase 0 — Feasibility

- **Status:** `not-started`
- **Depends on:** none
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-10-31

## Goal

Prove that the risky integrations can work before committing to the app architecture: Walmart WebView aisle extraction, Home Depot HTTP parsing, ML Kit handwriting/OCR, Gemini Nano availability, date-phrase parsing, and post-MVP price availability.

## Context

This phase de-risks Phases 5–11. It produces a report, sanitized fixtures, and explicit go/no-go decisions. It is a spike, not a production feature.

## In scope

- A scratch Java/Android harness or small throwaway module for WebView and ML Kit experiments.
- Manual research and documented extraction steps for Walmart and Home Depot.
- Sanitized HTML/JSON fixtures for both stores.
- ML Kit Digital Ink and Text Recognition v2 experiments on a real device or emulator where supported.
- Gemini Nano availability/latency test on a supported device.
- Deterministic date-phrase parsing prototype.
- Lightweight price-availability observation for Walmart, Home Depot, and at least one other target store.
- `docs/spikes/phase-00-report.md` with findings and decisions.

## Out of scope

- Production app scaffolding.
- Polished UI.
- Room, Hilt, WorkManager, or navigation setup.
- Automated tests beyond throwaway scripts.
- Committing live network responses with personal data.
- Implementing the full lookup or AI features.

## Requirements

### Walmart

- Load a Walmart search/product page in a WebView with JavaScript enabled.
- Set a store context (ZIP or store selection).
- Extract aisle/location data for at least three real items.
- Document whether the data comes from DOM text, embedded JSON, or an XHR response.
- Document blocking behavior (CAPTCHA, sign-in, 403/429).
- Document whether a foreground service is required for reliable extraction.

### Home Depot

- Fetch a product page with OkHttp and parse it with Jsoup.
- Extract `Aisle` and `Bay` for at least three real items with a selected store context.
- Document how store context is set (cookie, query parameter, or page flow).
- Capture sanitized fixtures for: aisle present, aisle absent, blocked page.

### ML Kit

- Download the English Digital Ink model and recognize a sample handwritten shopping list.
- Run Text Recognition v2 on a sample handwritten photo and record accuracy limitations.
- Confirm both APIs work from Java.
- Document model download size, latency, and offline behavior.

### On-device AI

- On a supported device, check Gemini Nano availability with the ML Kit GenAI Prompt API Java package.
- Measure a simple item-cleanup prompt.
- Document unsupported-device behavior, quota errors, and the rules fallback.
- Confirm inference only works in the foreground.

### Dates

- Prototype deterministic parsing for: `tomorrow`, `in 3 days`, `next week`, `wait a week`, `in 2 weeks`, `next month`, `this weekend`.
- Define how the parsed target date maps to the nearest list on or after that date.
- Identify ambiguous phrases that need user confirmation.

### Pricing

- Observe whether local prices are visible on public pages for Walmart, Home Depot, and one other store.
- Note whether store context and size/unit are visible.
- Decide whether Phase 11 can automate extraction or must rely on manual prices for a store.

## Technical approach

- Keep the spike code separate from the eventual `app` module or delete it before Phase 1.
- Use Java only.
- Do not add production dependencies based on the spike without recording them in `12-decisions.md`.
- Sanitize fixtures before committing: remove cookies, tokens, addresses, and personal data.

## Deliverables

```text
docs/spikes/phase-00-report.md
app/src/test/resources/fixtures/walmart/
app/src/test/resources/fixtures/homedepot/
```

If the app module does not exist yet, place fixtures under `docs/spikes/fixtures/` and move them in Phase 1.

## Acceptance criteria

- [ ] Walmart extraction resolves aisle/location data for at least three real items.
- [ ] Home Depot parsing resolves aisle and bay for at least three real items.
- [ ] Store-context mechanism is documented for both stores.
- [ ] Sanitized fixtures exist for Walmart and Home Depot, including failure pages.
- [ ] Digital Ink model download and recognition work from Java.
- [ ] Text Recognition v2 runs on a handwritten sample; limitations are documented.
- [ ] Gemini Nano availability, latency, quota, and fallback behavior are documented.
- [ ] Date-phrase parsing rules and ambiguity handling are documented.
- [ ] Price availability is documented for at least three stores.
- [ ] The report contains explicit go/no-go decisions for Phases 5, 6, and 11.
- [ ] No personal data, cookies, tokens, or live credentials are committed.
- [ ] No production app code or Gradle project is required yet.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Walmart extraction | Manual | Real device, selected store |
| Home Depot parsing | Manual | OkHttp/Jsoup scratch code |
| Digital Ink | Manual | Real device, English model |
| Camera OCR | Manual | Handwritten sample photo |
| Gemini Nano | Manual | Supported device |
| Date parsing | Unit (scratch) | Fixed clock |
| Price availability | Manual | Three stores |

## Manual demo

1. Open the Phase 0 report.
2. Show the Walmart and Home Depot fixtures and the extracted aisle/bay values.
3. Show a Digital Ink recognition result and a camera OCR result.
4. Show the Gemini Nano availability result and the rules fallback.
5. Show the date-phrase parsing table.
6. Show the price-availability findings.

## Risks

- Walmart may block automated access or change its DOM. Mitigation: foreground WebView, fixtures, manual override, and a documented fallback.
- Home Depot may require cookies or a store-selection flow. Mitigation: document the flow and keep fixtures.
- Gemini Nano may be unavailable on the test device. Mitigation: rules fallback; AI remains optional.
- Camera handwriting accuracy may be poor. Mitigation: Digital Ink is the primary handwriting path; review is mandatory.
- Price data may not be available. Mitigation: manual prices and a Phase 11 go/no-go.

## Open questions

- Which Walmart store/ZIP should be used for fixtures?
- Which Home Depot store should be used for fixtures?
- Is a Gemini Nano-capable device available for testing?
- Should fixtures live in the app test resources from the start?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
