# Phase 11 — Pricing Spike

- **Status:** `not-started`
- **Depends on:** Phase 10
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-22

## Goal

Validate whether local prices can be extracted reliably from public store pages, and define the custom-store and manual-price requirements before building price comparison.

## Context

Price comparison is post-MVP. Phase 10 completes store lookup coverage; this spike prevents Phase 12 from depending on price data that does not exist or is too fragile.

## In scope

- Price availability research for Walmart, Home Depot, and at least one of Lowe's, Target, or Smiths (Phase 0 already probed Walmart, Home Depot, and Target).
- Store-context requirements for local prices.
- Whether size/unit is visible.
- Manual price and custom store requirements.
- Sanitized fixtures.
- `docs/spikes/phase-11-pricing-report.md`.
- Go/no-go recommendation per store.

## Out of scope

- Production price UI.
- Price comparison implementation.
- Receipt scanning.
- Coupons or loyalty pricing.
- Sharing or authentication.

## Requirements

### Price availability

For each store, document:

- Is a local price visible on a public page?
- Does it require a selected store or ZIP?
- Is the price online-only, pickup-only, or in-store?
- Is size/unit visible?
- Is the price present in HTML, embedded JSON, or only rendered JavaScript?
- What happens when blocked or out of stock?

### Custom stores

- Define the minimum fields: name, address, optional coordinates, optional hours.
- Confirm custom stores use manual aisle and manual price entry.
- Define how custom stores appear in the store picker and list sections.
- Define archive/delete behavior when items reference the store.

### Manual prices

- Define the manual price entry flow.
- Define source/timestamp display.
- Define how manual prices interact with automated prices.
- Define expiry behavior for manual prices.

### Fixtures

- Capture sanitized pages with price, without price, and blocked.
- Document expected extraction.
- Do not commit cookies, tokens, or personal data.

## Technical approach

- Use scratch Java/OkHttp/Jsoup code or a debug harness.
- Do not add production price code yet.
- Reuse the Phase 0 store-context findings.
- Record findings in the report.

## Deliverables

```text
docs/spikes/phase-11-pricing-report.md
app/src/test/resources/fixtures/pricing/
```

## Acceptance criteria

- [ ] Price availability is documented for at least three stores.
- [ ] Store-context requirements are documented.
- [ ] Size/unit visibility is documented.
- [ ] Sanitized fixtures exist for each tested store.
- [ ] Manual price and custom store requirements are documented.
- [ ] A go/no-go decision exists for automated extraction per store.
- [ ] The report recommends a Phase 12 scope.
- [ ] No production price feature is implemented.
- [ ] No personal data or secrets are committed.
- [ ] Every test in the test plan has run and passed, including `connectedDebugAndroidTest` on the simulated device (emulator) when the phase adds Android code; no test has failed, errored, or been skipped.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Price extraction | Manual | Per store |
| Store context | Manual | ZIP/store selection |
| Fixtures | Manual review | Sanitized |
| Report review | Manual | Product owner sign-off |

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored, been skipped, or failed to run. That includes every test in the test plan and, when the phase adds Android code, `./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator) or a physical device. An unavailable device is not a pass: leave the phase `in-progress` or `blocked` and record the unrun test in the completion log.

## Manual demo

1. Open the pricing report.
2. Show each store's price availability and fixture.
3. Show the manual-price and custom-store requirements.
4. Review the go/no-go recommendation and Phase 12 scope.

## Risks

- Prices may not be available on public pages. Mitigation: manual prices and a reduced Phase 12 scope.
- Prices may be location-specific and volatile. Mitigation: store context, timestamps, and expiry.
- Legal/ToS constraints. Mitigation: public pages only, rate limits, no bulk scraping.

## Open questions

- Which third store should be tested first?
- Should custom stores be available in Phase 12 or a separate phase?
- Is manual price entry enough if automated extraction fails?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
