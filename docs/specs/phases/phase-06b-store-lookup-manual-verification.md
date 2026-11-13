# Phase 6b — Store Lookup Manual Verification (Follow-up)

- **Status:** `not-started`
- **Depends on:** Phase 6
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-13

## Goal

Finish the Phase 6 manual verification that could not be completed in this environment: confirm Walmart assisted lookup works on a real device/network, verify the false-positive fix, and decide whether any marker/UI changes are needed before marking Phase 6 `done`.

## Context

Phase 6 is `blocked` only because of the manual live test and store-context verification. Automated checks pass, including `connectedDebugAndroidTest` (23/23 on emulator). A real Walmart run previously reported `Blocked: Walmart challenge or sign-in page detected.` even on normal pages because `isBlockedPage` matched the `perimeterx` token in Walmart's CSP allowlist. That false positive has been fixed. This follow-up records the remaining real-device checks and any follow-up code changes.

## In scope

- Manual Walmart lookup for at least three items on a real device/network.
- Assisted store selection through `WalmartStoreFragment`.
- PerimeterX/"Robot or human?" challenge handling.
- Candidate selection (`Multiple`) and manual aisle override.
- Cache hit and offline stale-result behavior.
- Home Depot manual-entry-first behavior.
- Any small marker/copy changes discovered during the run.

## Out of scope

- Phase 7 background orchestration.
- Automated price extraction.
- New store integrations.

## Requirements

- Run lookup only as a user-initiated foreground action.
- Never treat a normal Walmart page as blocked because of `perimeterx`, `*.px-cdn.net`, or `*.perimeterx.net` allowlist entries.
- Keep `rawText` unchanged; store matches separately.
- Manual aisle values must never be overwritten by lookup.
- Record whether PX passed, remained blocked, or required user interaction.

## Recommended manual tests

1. Open the Walmart store page from the lookup harness and select/confirm the user's store. Record the final URL and whether PX appeared.
2. If PX appears, complete the challenge and confirm cookies persist for later lookups.
3. Run lookup for at least three items: milk, eggs, and paper towels (or equivalent).
4. For each item, record: final URL, blocked/loaded, `__NEXT_DATA__` present, aisle/location, product name, and whether result is `FOUND`, `NEEDS_REVIEW`, `NOT_FOUND`, or `ERROR`.
5. Open "Not right?" on a `NEEDS_REVIEW` item and apply a different candidate; confirm `matchCandidatesJson` is cleared after confirmation.
6. Enter an aisle manually, rerun lookup, and confirm the manual value is preserved.
7. Clear a manual value and rerun lookup; confirm lookup can replace it.
8. Disable network and confirm cached results still show with the stale indicator.
9. Simulate/observe a blocked response and confirm the run stops with a clear message and no retry.
10. Verify Home Depot flow remains manual-entry-first and does not attempt live HTTP lookup unless explicitly enabled.
11. Check store context is visible in the lookup harness and item rows.

## Recommendations

- Capture a sanitized real Walmart search page if PX passes, and add it as a fixture to guard `isBlockedPage`.
- If PX remains blocked on the user's network, document that result and keep Walmart as assisted/manual-first.
- Consider tightening broad markers (`access denied`, `please sign in`, `sign in to continue`) only if real-page testing shows false positives.
- Keep marker detection in `WalmartAisleExtractor` pure and unit-tested; do not move it into the WebView client.
- Do not mark Phase 6 `done` until the manual live test passes or is explicitly accepted as a known limitation.

## Acceptance criteria

- [ ] Walmart assisted lookup resolves at least three items on a real device, or the limitation is documented and accepted.
- [ ] Store context is visible and verified.
- [ ] Manual override and candidate selection work on device.
- [ ] Cache and stale behavior verified.
- [ ] Blocked response behavior verified.
- [ ] Phase 6 status updated based on results.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Walmart live lookup | Manual | Real device/network |
| Store picker | Manual | Cookie persistence |
| Candidate selection | Manual | Persisted candidates |
| Manual override | Manual | No overwrite |
| Cache/offline | Manual | Stale indicator |
| Blocked response | Manual | No retry |

## Completion gate

Phase 6 remains `blocked` until this follow-up's manual tests pass or a documented decision accepts the limitation. Automated checks already pass.

## Risks

- PX may block all sessions on some networks.
- Marker changes may reintroduce false positives.
- Store context cannot be pinned programmatically.

## Open questions

- Does PX pass on the user's real device/network?
- Should low-confidence matches auto-select or require review?
- Should broad sign-in markers be removed after real-page capture?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |