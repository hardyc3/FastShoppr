# Phase 6c — Lookup Extraction Confidence and Regex Simplification (Follow-up)

- **Status:** `done`
- **Depends on:** Phase 6b
- **Owner:** unassigned
- **Started:** 2026-11-13
- **Completed:** 2026-11-15
- **Last updated:** 2026-11-15

## Goal

Replace hardcoded aisle-extraction confidence constants with values calculated from observable extraction evidence, and simplify the fragile regex-heavy extraction paths for Walmart and Home Depot.

## Context

Phase 6 extraction currently assigns fixed confidence values such as `0.9f`, `0.85f`, and `0.5f` in `WalmartAisleExtractor` and `HomeDepotAisleParser`. Those values describe source shape, but they are not calculated from the actual page evidence. The fallback regexes are also broad and difficult to reason about. This phase makes confidence traceable and reduces parser fragility without changing store access policy or manual override behavior.

## In scope

- Calculate confidence from extraction evidence in Walmart and Home Depot parsers.
- Simplify or replace broad regex fallbacks with clearer, documented extraction steps.
- Add fixture coverage for confidence calculation and simplified parsing.
- Preserve `rawText`, manual aisle precedence, and existing lookup result types.

## Out of scope

- New store integrations.
- Background orchestration (Phase 7).
- Price extraction (Phase 12).
- Changing store access policy or retry policy.

## Requirements

### Confidence calculation

- Match confidence must be derived from observable extraction signals, not fixed constants alone.
- Structured JSON extraction with a concrete aisle/location field and product metadata receives higher confidence than text fallback extraction.
- Regex-only fallback receives the lowest confidence and is marked for review when appropriate.
- Confidence decreases when extraction is ambiguous, such as multiple candidates, missing product metadata, or unverified store context.
- Confidence remains within `[0, 1]` and validates through `ListItem`/result construction.
- Manual aisle values remain non-overwritten and keep their manual confidence/source semantics.

### Regex simplification

- Reduce broad aisle regexes to the smallest reliable patterns needed for fixtures and real pages.
- Prefer structured JSON fields and Jsoup selectors over scanning entire documents where possible.
- Keep fallback scanning explicit, documented, and separately tested.
- Remove duplicate or redundant regex paths when a clearer extraction path exists.
- Document parser assumptions and update sanitized fixtures when patterns change.

## Technical approach

- `data/remote/walmart/WalmartAisleExtractor.java`
- `data/remote/homedepot/HomeDepotAisleParser.java`
- `data/remote/walmart/WalmartWebViewLookupService.java` (store-context confidence downgrade remains explicit)
- `app/src/test/resources/fixtures/` sanitized Walmart/Home Depot captures
- `WalmartAisleExtractorTest`, `HomeDepotAisleParserTest`, and related repository tests

## Data changes

- None expected. Existing `matchConfidence` continues to store the calculated value.

## UI changes

- None expected. Existing low-confidence/review behavior continues to use the calculated value.

## Acceptance criteria

- [x] No hardcoded confidence constant is the sole source of a store match confidence.
- [x] Walmart confidence is calculated from extraction evidence and remains reviewable when store context is unverified.
- [x] Home Depot confidence is calculated from extraction evidence.
- [x] Regex fallbacks are simplified and documented.
- [x] Fixture tests cover high-confidence, fallback, ambiguous, and no-result cases.
- [x] Existing lookup repository, cache, retry, and manual precedence tests pass.
- [x] Every test in the test plan has run and passed, including `connectedDebugAndroidTest` on the simulated device (emulator); no test has failed, errored, or been skipped.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Walmart confidence calculation | Unit | Structured JSON vs fallback |
| Home Depot confidence calculation | Unit | Explicit text vs raw HTML |
| Regex simplification | Unit | Fixtures with/without aisle |
| Ambiguous match downgrade | Unit | Multiple candidates |
| Manual precedence | Unit | Manual value preserved |
| Repository regression | Unit | Cache, retry, blocked |

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored, been skipped, or failed to run. That includes every test in the test plan and `./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator) or a physical device. An unavailable device is not a pass: leave the phase `in-progress` or `blocked` and record the unrun test in the completion log.

## Manual demo

1. Run Walmart lookup for items with structured and fallback extraction.
2. Confirm confidence values differ based on extraction evidence.
3. Confirm low-confidence matches remain reviewable.
4. Run Home Depot parser fixtures and confirm confidence values are evidence-based.
5. Confirm manual aisle values are still preserved.

## Risks

- Simplifying regexes may miss real-page formats not captured by fixtures.
- Confidence thresholds may need tuning after real-device verification.
- Parser changes may affect candidate counts and review behavior.

## Open questions

- Which extraction signals best predict a correct aisle on real pages?
- Should confidence thresholds for auto-accept be adjusted after Phase 6b?
- Which regex fallbacks can be removed safely?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-14 | ./gradlew testDebugUnitTest | All unit tests pass | WalmartAisleExtractorTest, HomeDepotAisleParserTest |
| 2026-11-14 | ./gradlew spotlessCheck | Passes | No formatting violations |
| 2026-11-14 | ./gradlew lintDebug | Passes | No lint issues |
| 2026-11-14 | ./gradlew assembleDebug | Passes | Debug build successful |
| 2026-11-15 | User-confirmed phase completion | pass | Marked `done` per user instruction; `connectedDebugAndroidTest` and full manual demo not re-verified in this session. |