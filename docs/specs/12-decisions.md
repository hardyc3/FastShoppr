# Decision Log

Architectural and product decisions are recorded here. Add a new entry when a decision changes. Do not rewrite history; supersede an entry with a new one.

Format:

```text
## D-XXX — Title
- Date:
- Status: accepted | superseded by D-XXX | deferred
- Decision:
- Rationale:
- Consequences:
```

---

## D-001 — Java-only Android app with XML Views

- Date: 2026-10-31
- Status: accepted
- Decision: Build the app in Java 21 using XML layouts, ViewBinding, Fragments, and Material Components. Do not add Kotlin app source, the Kotlin Android plugin, Jetpack Compose, coroutines, Flow, or DataStore. Gradle Kotlin DSL is allowed for build scripts.
- Rationale: The product owner requires Java. Compose cannot be authored in Java, so XML Views are the consistent choice.
- Consequences: Use LiveData, ExecutorService, Handler, and CompletableFuture for async work. Use SharedPreferences instead of DataStore. Hilt, Room, WorkManager, CameraX, and ML Kit all have Java APIs.

## D-002 — Single app module with MVVM + repository

- Date: 2026-10-31
- Status: accepted
- Decision: Use one `app` module with package-level layers: UI -> ViewModel -> use case -> repository -> data source. Use Hilt for DI.
- Rationale: A single module is faster to build and easier for a small team/agent workflow. Package boundaries are enough for the MVP.
- Consequences: Split modules only when a concrete build or ownership problem appears. Domain code must stay free of Android imports.

## D-003 — Local-first, no backend in the MVP

- Date: 2026-10-31
- Status: accepted
- Decision: Store lists, items, captures, cache, and prices locally. Do not add a backend, cloud database, analytics, or account in Phases 0–11.
- Rationale: The core value works offline. A backend adds cost, privacy risk, and scope before validation.
- Consequences: Sharing and sync are deferred. Store lookups call public retailer pages directly from the device.

## D-004 — Walmart lookup via WebView foreground service

- Date: 2026-10-31
- Status: accepted
- Decision: Use a WebView-based extractor for Walmart, running in a foreground service with a visible persistent notification. Do not use plain background work for WebView.
- Rationale: Walmart is JavaScript-heavy and does not reliably expose aisle data in static HTML. Android background limits make WebView unreliable outside a foreground service.
- Consequences: Requires notification permission for background runs. If denied, queue Walmart items until the app is in the foreground. Manual aisle entry remains available.

## D-005 — Home Depot lookup via OkHttp + Jsoup

- Date: 2026-10-31
- Status: accepted
- Decision: Use OkHttp to fetch public Home Depot pages and Jsoup/structured JSON to extract aisle and bay. Run in WorkManager.
- Rationale: Home Depot product pages publicly show in-store aisle/bay data, and HTTP parsing is simpler and more reliable than WebView.
- Consequences: Store context must be resolved first. Parser changes require fixture updates. Respect rate limits and stop on blocks.

## D-006 — On-device AI only, with deterministic fallback

- Date: 2026-10-31
- Status: accepted
- Decision: Use ML Kit GenAI Prompt API (Gemini Nano) for item-name cleanup and OCR post-processing where available. Fall back to deterministic rules. Never use a paid cloud LLM. Run inference only in the foreground.
- Rationale: On-device inference is free per call, private, and offline. Gemini Nano availability is limited and background inference is blocked.
- Consequences: AI is optional. The app must work fully without it. No AI in Workers. User confirmation is required before changing `displayName`.

## D-007 — Date-driven lists with item deferral

- Date: 2026-10-31
- Status: accepted
- Decision: Every list has a shopping date. Every item has a target date that determines its list. Support natural-language deferral such as "wait a week", a calendar view, and an overdue prompt.
- Rationale: Users think in terms of weekly trips and "not this week" items. Dates make deferral explicit and testable.
- Consequences: One active list per date. Deferral moves items transactionally. Overdue items prompt the user rather than moving silently.

## D-008 — Custom-interval recurring schedule

- Date: 2026-10-31
- Status: accepted
- Decision: Support one active schedule with a custom interval in days or weeks. Auto-create the next list and notify the user.
- Rationale: Shopping cadence varies. A fixed weekly schedule is too restrictive.
- Consequences: WorkManager checks daily and on app open. Only the next upcoming list is created, not a backlog.

## D-009 — Background organizing with notifications

- Date: 2026-10-31
- Status: accepted
- Decision: Run organizing through WorkManager. Use a foreground service for Walmart WebView work. Post progress and completion notifications.
- Rationale: Users should be able to add items and let the app organize without keeping it open.
- Consequences: Notification permission handling is required. Items keep explicit lookup states. Manual edits are never overwritten by in-flight results.

## D-010 — Aisle ascending/descending sorting only

- Date: 2026-10-31
- Status: accepted
- Decision: Sort by aisle ascending or descending within each store section. Unresolved items always sort last. Do not implement manual drag reorder or store-specific walking routes in this scope.
- Rationale: Aisle order solves the core backtracking problem. Extra route features add complexity without proven need.
- Consequences: A documented numeric-aware comparator is used everywhere. Ties break by bay then insertion order.

## D-011 — Preserve the written name

- Date: 2026-10-31
- Status: accepted
- Decision: `rawText` is never overwritten by a store match or AI cleanup. Store the match separately in `matchedProductName` and show it as secondary information.
- Rationale: Users must be able to spot a bad match immediately. Silent replacement destroys trust.
- Consequences: The UI always shows the written name. AI and lookup can only change display/match fields after confirmation.

## D-012 — Price comparison is post-MVP

- Date: 2026-10-31
- Status: accepted
- Decision: Implement price comparison after the routing MVP, using public store-page prices where available and manual override. Support custom local stores.
- Rationale: Routing is the core differentiator. Price data is volatile and store-specific, so it needs its own spike.
- Consequences: Phases 11–12 cover pricing after Phase 10 completes store lookup coverage. The MVP data model reserves a `PriceQuote` entity but does not expose price UI.

## D-013 — Sharing, sync, and Google Sign-In are deferred

- Date: 2026-10-31
- Status: deferred
- Decision: Do not implement real-time sharing, multi-user editing, Google Sign-In, or cloud sync in Phases 0–11. Record the intended design in `11-future-sharing-sync.md`.
- Rationale: Collaboration requires a backend, auth, access rules, and conflict handling. It is a separate effort.
- Consequences: The MVP remains local-only. The data model uses stable UUIDs and `updatedAt` so future sync is possible.

## D-014 — No Google Maps Popular Times

- Date: 2026-10-31
- Status: accepted (amended by D-045)
- Decision: Do not integrate Google Maps Popular Times or scrape Maps busyness data. Store-hours-based visit planning is removed by D-045; the app never claims to know current busyness.
- Rationale: There is no official Popular Times API, and scraping Maps is unreliable and against terms.
- Consequences: The app makes no busyness claims and shows no visit-time or open/closed estimate.

## D-015 — Public, rate-limited, cached store access

- Date: 2026-10-31
- Status: accepted
- Decision: Query only public retailer pages, identify the app in the user agent, limit to one request per second per store, cache results for 7 days, and stop on 403/429/challenges.
- Rationale: Respect retailer infrastructure and terms while keeping lookups usable.
- Consequences: Lookups may be incomplete. Manual aisle entry and cached results keep the app useful.

## D-016 — OCR with review, not auto-save

- Date: 2026-10-31
- Status: accepted
- Decision: Use ML Kit Digital Ink for on-screen handwriting and ML Kit Text Recognition v2 for camera/gallery. Every capture goes through a bulk-edit review screen before saving.
- Rationale: Camera handwriting recognition is best-effort. Users need control over what is saved.
- Consequences: `rawText` is the reviewed text. Images are deleted after review. Recognition failures are recoverable.

## D-017 — GPS with manual store override

- Date: 2026-10-31
- Status: accepted (amended by D-043)
- Decision: Default to the nearest store using location when the user opts in, with a manual store/ZIP override. Show the selected store in the UI.
- Rationale: Aisle data is store-specific. Location improves defaults, but the user must be able to correct it.
- Consequences: Location permission is requested only on demand. Denial falls back to last used or manual selection. Coordinates are never logged.
- Amendment (D-043, 2026-11-22): Location-based nearest-store selection and manual store/ZIP override are removed. The user picks a branch in the brand's public store-locator WebView; no location permission, ZIP, or coordinates are used.

## D-018 — Room + SharedPreferences

- Date: 2026-10-31
- Status: accepted
- Decision: Use Room for structured data and SharedPreferences for settings. Do not use DataStore.
- Rationale: Both are Java-friendly and stable. DataStore is Kotlin/coroutines-oriented and conflicts with the Java-only decision.
- Consequences: Settings are simple key/value. Room schemas and migrations are exported and tested.

## D-019 — Quality gates

- Date: 2026-10-31
- Status: accepted
- Decision: A phase is done only when build, unit tests, lint, Spotless, and required instrumentation/manual checks pass. No new warnings or TODOs.
- Rationale: Spec-driven development depends on verifiable completion.
- Consequences: Agents must run commands and record evidence in the phase completion log.

## D-020 — One phase at a time

- Date: 2026-10-31
- Status: accepted
- Decision: Implement exactly one phase per session, then stop for review. Do not start the next phase automatically.
- Rationale: Small reviewable chunks reduce drift and make failures easy to isolate.
- Consequences: Phase status in `README.md` and the phase file must be updated. The next phase remains `not-started`.

## D-021 — Phase 0 findings: retailer bot walls block automated lookup

- Date: 2026-11-03
- Status: accepted (amends D-004 and D-005)
- Decision: Automated aisle extraction is not viable from this environment for either MVP store. Walmart redirects plain HTTP clients and the emulator WebView to a PerimeterX "Robot or human?" page. Home Depot returns HTTP 403 (Akamai) to plain HTTP clients and shows its error page in WebView. Keep the D-004/D-005 designs as the intended architecture, but treat live lookup as **conditional go** only: it must run as a user-initiated foreground WebView session with manual override, and otherwise the app falls back to manual aisle entry and cached results. OkHttp+Jsoup for Home Depot is kept only if a future verification pass from a real device gets real aisle pages; otherwise Home Depot also ships manual-entry-first.
- Rationale: Verified 2026-11-03 from WSL (curl) and an API 36 emulator (WebView and OkHttp). Both stores present bot challenges; bypassing them is out of scope and against the store data access policy. Synthetic fixtures document the parser logic but prove nothing about live pages.
- Consequences: Phase 6 ships with manual aisle entry as the guaranteed path. Lookup is an enhancement, not a requirement. Fixtures must be re-verified on a real device before trusting the parser. Price extraction (Phase 12) is likewise blocked: automated price lookup is a no-go until a real device passes the challenges; plan manual prices.

## D-022 — Walmart WebView extraction verified on emulator; Home Depot remains blocked

- Date: 2026-11-03
- Status: accepted (amends D-021)
- Decision: Walmart lookup is **conditionally viable** via the D-004 design: on 2026-11-03 an emulator WebView session loaded walmart.com and three search pages (milk, eggs, paper towels) once past PX; aisles extracted from `__NEXT_DATA__` (`"Location":[{"displayValue":"A29","aisle":{"zone":"A","aisle":29}}]` — milk→A29, eggs→AC4, paper towels→J29), prices and default store context visible. PX pass is intermittent (≈50% of sessions; plain HTTP always blocked). Home Depot remains **blocked** (HTTP 403 Akamai from curl, OkHttp, and interactive WebView). Lookup stays: user-initiated, foreground WebView only, cached, manual override always available, never unattended. Walmart aisle parsing keys off `LocationDisplayValue`/`"Location":[...]` in `__NEXT_DATA__`; fixture tests must use sanitized, trimmed captures.
- Rationale: Phase 0 evidence (fixtures `walmart/webview-*.json`, report §1/§2). D-021's blanket "no automated path" was too strong for Walmart; HD unchanged.
- Consequences: Phase 6 may implement Walmart assisted lookup (flag-gated, user-triggered). HD ships manual-entry-first; HD HTTP stays dead until a real-device/mobile-data pass. Phase 12 pricing extraction viable for Walmart/Target where pages load; HD no-go; manual prices remain the guaranteed path everywhere. Nothing here authorizes background or headless lookup.

## D-023 — Soft delete with a 30-day restore window

- Date: 2026-11-06
- Status: accepted
- Decision: Deleting a list, item, or capture session stamps `deletedAt` instead of removing the row. Deleting a list stamps the same timestamp on its items and capture sessions. Records can be restored for 30 days and are then physically purged. Foreign-key cascades remain for the final purge.
- Rationale: Implements the deletion rule already present in `02-data-model.md` and protects users from accidental data loss.
- Consequences: Normal queries filter `deletedAt IS NULL`. Entities gained a nullable `deletedAt` column in schema version 2. Restoring a list is blocked when another active list exists for the same shopping date.

## D-024 — Active-only shopping-date uniqueness

- Date: 2026-11-06
- Status: accepted
- Decision: `ShoppingList.shoppingDate` is a non-unique index. `getOrCreate` runs in a Room transaction and returns the existing active, non-deleted list for the date; archived or deleted lists do not block a new list for that date.
- Rationale: Matches "unique among active lists" without a database partial index that Room cannot express, and allows a fresh list after an old one was archived.
- Consequences: All list creation must go through the repository transaction. Restoring a deleted list must check for a conflicting active list first.

## D-025 — Store archive flag

- Date: 2026-11-06
- Status: accepted
- Decision: Add `isArchived` to `Store`. Active store queries exclude archived stores. Deleting a store is blocked while any item or aisle-cache entry references it; archive is the supported alternative.
- Rationale: The data model requires archive-by-default for referenced stores but did not define a field for it.
- Consequences: Store deletion remains available only for unreferenced stores. Store seeding uses fixed IDs with insert-or-ignore so archived seed stores are not recreated or overwritten.

## D-026 — Schedule creation conflict resolution

- Date: 2026-11-06
- Status: accepted
- Decision: When exactly one active schedule exists and a create request targets the same next shopping date, return the existing schedule. When it targets a different date, return a decision-required result so the UI can ask whether to disable the existing schedule and create a new one or move the existing schedule to the new date and interval.
- Rationale: The user must choose between replacing the cadence and editing the current one; silently replacing or rejecting the request would lose intent.
- Consequences: `CreateScheduleUseCase` exposes `ConflictAction` values (`ASK`, `DISABLE_EXISTING`, `MOVE_EXISTING`) and a sealed `CreateScheduleResult`. Phase 8 supplies the confirmation UI.

## D-027 — Mandatory review for every capture; auto-save skip deferred

- Date: 2026-11-09
- Status: accepted (amends D-016 and `04-ocr-and-input.md`)
- Decision: Every capture (ink, camera, gallery) opens the review screen before any `ListItem` is created, regardless of recognition confidence. Confidence drives a visible flag only (ink > 0.8, OCR > 0.7 thresholds for low-confidence marking). The `04-ocr-and-input.md` high-confidence auto-save path (ink > 80%, OCR > 70% skip review) is explicitly deferred and not implemented.
- Rationale: Phase 4 implemented mandatory review with confidence badges. Camera/OCR handwriting accuracy is the weakest input path, and an unintentional skip keeps the user in control of what reaches `rawText`. One uniform flow is simpler to test and demo than a two-path flow.
- Consequences: `rawText` always equals the reviewed line. No per-capture "auto-saved" banner. Iterating to a skip path can be revisited later only via a new decision entry and spec update; `ReviewFragment`/`ReviewViewModel` remain the single save path.

## D-028 — System bar insets applied once at the activity root

- Date: 2026-11-09
- Status: accepted
- Decision: `MainActivity` applies system bar and IME insets as padding on the navigation host container. Screens and fragments do not add their own inset handling.
- Rationale: Target SDK 36 enforces edge-to-edge, so app content previously drew under the status bar and the navigation bar. On the review screen this put the save and discard buttons under the navigation bar, making them untappable.
- Consequences: One place controls insets for all screens. Layouts must not assume the full window height, and instrumentation tests must scroll to a row before interacting with it.

## D-029 — AI cleanup is opt-in and foreground-only

- Date: 2026-11-11
- Status: accepted (amends `05-on-device-ai.md`, which said AI cleanup is on by default)
- Decision: On-device AI cleanup is **off by default**. The user opts in through the settings toggle "Improve item names with on-device AI". Availability checks and cleanup run only while the review screen is started; leaving the screen cancels in-flight work and closes the model session. No model download happens without an explicit user action.
- Rationale: Phase 5 requires an optional feature with a clear opt-in, and on-device AI availability varies by device and quota. Opt-in keeps behavior predictable, avoids surprise inference, and matches the offline-first product rules.
- Consequences: The review cleanup action is hidden until the setting is enabled. `SettingsPreferences` persists `ai_cleanup_enabled` (default false). The spec's original on-by-default wording is superseded.

## D-030 — Rules fallback stays visible; validation failures fall back to rules

- Date: 2026-11-11
- Status: accepted (amends `05-on-device-ai.md` validation-failure wording)
- Decision: When AI cleanup is enabled but the model is `DOWNLOADABLE`, `DOWNLOADING`, `UNAVAILABLE`, or over quota, the run continues with deterministic rules and shows "AI cleanup unavailable; using automatic cleaning." Invalid model output is mapped to `CleanItemResult.Unavailable`, which the coordinator converts to the rules path; only a valid model result that matches the input returns `NoChange`. No automatic retries are performed for quota errors.
- Rationale: A silent no-change on malformed output would hide a broken model behind a dead action. Falling back to rules keeps the feature useful and makes the source explicit in the suggestion sheet.
- Consequences: `ItemNameCleanerCoordinator` is the single fallback point. Every suggestion carries an `AiCleanupSource` (`ON_DEVICE_AI` or `RULES`) for display. Adding retries or automatic downloads requires a new decision entry.

## D-031 — Gradle wrapper 9.5.1 and google-java-format 1.33.0

- Date: 2026-11-13
- Status: accepted
- Decision: The root Gradle wrapper is pinned to **9.5.1** (previously 8.13) and Spotless uses `googleJavaFormat` **1.33.0** (previously 1.28.0). The Android baseline remains Java 21 / SDK 36 / AGP 8.13.2.
- Rationale: Gradle 8.13/Spotless 1.28.0 do not run on the current JDK (Temurin 25.0.5); the pinned set failed with configuration-cache/serialization and JVM-version errors before any Android task could execute. Builds now complete on the current toolchain.
- Consequences: Builds from this tree require the committed wrapper (no separate Gradle install). Earlier log entries that recorded Gradle 8.13/9.9.1 describe the state at that date. Any future wrapper or `googleJavaFormat` bump requires a decision entry.

## D-032 — Walmart store context is user-assisted; cookie persistence with explicit forget

- Date: 2026-11-13
- Status: accepted (amends D-022; amended by D-043)
- Decision: Walmart exposes no reliable programmatic way to pin a physical store from a plain WebView load (a `stores=<number>` query parameter was observed **not** to change server-side context; the store is set via Walmart's own assisted UI or defaulted by IP). The lookup harness therefore provides a user-initiated **"Verify your Walmart store"** screen that hosts the regular walmart.com site in a WebView; the user picks their store there. The internal WebView cookie jar **persists** across lookups (and app restarts) until the user taps **"Forget store"**, which wipes Walmart cookies and DOM storage in that screen. Aisles are still downgraded to `NEEDS_REVIEW` (below the 0.6 auto-accept threshold) because context cannot be proven programmatically; the user verifies on the shelf or confirms via "Not right?".
- Rationale: D-022's assisted flow requires the user to select the store once; persisting cookies makes that one-time pick actually effective. Silent guessing was the alternative (wrong-store aisles reported as FOUND), rejected per store-context rules. Cookie clearing at every run would make the store pick useless; a visible "Forget store" action keeps retention honest and local.
- Consequences: `WalmartWebViewLookupService` no longer wipes cookies/DOM storage on teardown (WebView instances are still created/destroyed per lookup). A `WalmartStoreFragment` (`walmart_store` nav destination) hosts the assisted session. Normal automated/background lookups remain out of scope (Phase 7). Store context remains unverified for acceptance purposes until checked on a physical device.
- Amendment (D-043, 2026-11-22): The Walmart-only assisted store screen is replaced by the generic WebView branch picker used for all supported locator brands. Saved branch name/address is the store context; the brand-specific screen and its separate verification flag are removed.

## D-033 — Multiple-match candidates persist on the item; `matchCandidatesJson` column (schema v3)

- Date: 2026-11-13
- Status: accepted
- Decision: The last `Multiple` result's candidate list is serialized (Gson) into a new nullable `ListItem.matchCandidatesJson` column (Room **2→3**, `MIGRATION_2_3` adds the column; existing rows remain `NULL`). The item-row "Not right?" action decodes and offers the persisted candidates (pick one, or enter manually) when the item is `NEEDS_REVIEW`; candidates are cleared on any `Found`/`Blocked`/`NotFound`/`Error` result, on cache hits, on manual overrides, on candidate confirmation, and on store changes. Candidates never replace `rawText` and never bypass manual precedence; `applyCandidate` remains the single write path.
- Rationale: candidates were previously discarded when the lookup harness finished, so the item row had no way to offer a re-pick (spec "Candidate selection dialog for Multiple" unreachable outside the harness run). Persisting the small payload is simpler and more honest than re-scraping (cache intentionally stores only `Found`).
- Consequences: schema version bump 2→3 requires the migration to run on upgrades; `MigrationTest` covers 2→3. Candidates are untrusted retailer text — displayed only, never executed. Item payload in memory/Room grows by a few KB for `NEEDS_REVIEW` items only.

## D-035 — Aisle sorting groups letter-prefix aisles before bare numbers

- Date: 2026-11-14
- Status: accepted (amends D-010)
- Decision: Aisle comparator sorts letter-prefix aisles (non-digit chars before first digit run, e.g. `A19`, `J15`) before bare numbers (starts with digit, e.g. `29`). Within letter-prefix: prefix (case-insensitive), then numeric value, then suffix. Within bare numbers: numeric value, then suffix. Descending reverses: bare numbers first, then letter-prefix reversed. Unresolved aisles always sort last.
- Rationale: Real Walmart aisle values like `A19`, `J15`, `29` sort more intuitively for store navigation when letter-prefix aisles are grouped together (A-aisles, then J-aisles, then numbered aisles) rather than pure numeric order (J15 before A19 because 15 < 19). Matches user expectation for `J15, A19, J23, 29, A32` → `A19, A32, J15, J23, 29`.
- Consequences: `AisleComparator.compareResolvedValues` now classifies by `matcher.start() > 0` (has prefix) vs `== 0` (bare number). D-010's "documented numeric-aware comparator" is superseded by this letter-first rule. `02-data-model.md` updated. Existing tests updated; new tests cover the user's example and DESC direction.

## D-034 — Store lookup confidence must be calculated, not hardcoded

- Date: 2026-11-13
- Status: accepted
- Decision: Aisle extraction confidence must be derived from observable extraction evidence (structured JSON fields, product metadata, fallback source, ambiguity, and store-context verification). Fixed constants may be used only as base weights that are adjusted by evidence, not as the final confidence for every match. Regex simplification and confidence calculation are tracked in Phase 6c after Phase 6b.
- Rationale: Phase 6 currently assigns fixed confidence values in `WalmartAisleExtractor` and `HomeDepotAisleParser`, which hides extraction quality and makes review thresholds less meaningful. Real-page verification in Phase 6b must inform the final scoring signals.
- Consequences: Phase 6 remains done for its automated/manual scope, but confidence quality is a tracked follow-up. Future lookup code must preserve calculated confidence through repository, cache, and UI paths.

## D-036 — Auto-organize flow with automatic store verification

- Date: 2026-11-14
- Status: accepted (amended by D-043)
- Decision: The "Organize list" button on the list detail screen minimizes clicks by auto-starting the lookup when the Walmart store is already verified. If the store has not been verified, it automatically opens `WalmartStoreFragment` for store selection; when the user taps "Done" (fragment pops back), the lookup starts automatically. A `walmart_store_verified` preference flag tracks verification state, set when the user taps "Done" in `WalmartStoreFragment` and cleared when they tap "Forget store". The separate "Verify store" button is removed from the lookup screen; the "Start lookup" button remains as a manual re-run option.
- Rationale: Reducing button taps makes the organize flow faster and less error-prone. Store verification is a one-time step (cookies persist), so most organize attempts proceed directly to lookup.
- Consequences: `LookupFragment` no longer shows a store button; it either auto-starts or auto-navigates to verification. `WalmartStoreFragment` sets/clears the verified flag. `SettingsRepository` gains `isWalmartStoreVerified`/`setWalmartStoreVerified`. `03-store-integration.md` documents the auto-organize flow.
- Amendment (D-043, 2026-11-22): First use of a brand opens the generic WebView branch picker for that brand instead of the Walmart-only verification screen. The `walmart_store_verified` flag is replaced by one saved branch per brand; after confirmation the organize run proceeds.

## D-037 — Phase 9 list-detail indexes require Room schema v4

- Date: 2026-11-17
- Status: accepted
- Decision: Phase 9 adds two composite indexes to `list_items`: (`listId`, `deletedAt`, `sortPosition`) and (`listId`, `deletedAt`, `storeId`, `sortPosition`). Room is bumped from schema v3 to v4 with `MIGRATION_3_4`, exported schema `4.json`, and a migration test. This is the release-blocker exception allowed by the Phase 9 data-changes section.
- Rationale: the 100-item performance criterion reads and sorts items by both list and store on every render. Without these indexes the queries scan all rows for the list, which regresses large-list scroll and sort responsiveness.
- Consequences: upgrades from v3 run the index-only migration; no data is rewritten. Future list-detail query shapes should reuse these indexes or require a new migration and decision entry.

## D-038 — Privacy notice is in-app plus repo-root `PRIVACY.md`

- Date: 2026-11-17
- Status: accepted
- Decision: the MVP privacy notice is delivered as Settings text plus a standalone `PRIVACY.md` in the repository root. No hosted privacy policy URL or account-specific privacy page is introduced in Phases 0–11.
- Rationale: the app has no backend, account, analytics, or sharing, so a local, reviewable notice is sufficient for the MVP and keeps release documentation in source control.
- Consequences: `README.md` links to `PRIVACY.md`; the Settings notice must stay consistent with it. A hosted URL, if ever required for a store listing, needs a new decision entry.

## D-039 — Explicit nearest-store selection is display context in Phase 9

- Date: 2026-11-18
- Status: superseded by D-043
- Decision: Phase 9 adds a Settings flow that saves one ZIP code and one physical branch per brand (Walmart, Home Depot, Lowe's, Target, Smiths). Candidates come from each retailer's public store-locator page using the saved ZIP or an approximate location fix; the user picks and saves a branch. The saved branch is shown on the Organize screen for brands present on the list. It is verification context only: it never rewrites `rawText`, replaces an aisle, or claims provenance for an automated lookup. Manual name/address entry is always available.
- Rationale: Unexpected aisles are hard to diagnose without knowing which physical store the user means. An explicit, user-controlled branch removes ambiguity while avoiding a silent change to lookup behavior during MVP release hardening. The existing Walmart WebView verification and Home Depot lookup context remain authoritative for automated results.
- Consequences: Settings uses `ACCESS_COARSE_LOCATION` only when **Use my location** is tapped; denial, blocks, no results, and network failures fall back to ZIP or manual entry. Coordinates are never stored or logged, but the retailer receives the ZIP or coarse coordinate in the public locator URL. Locator responses are cached for 60 days in SharedPreferences, rate limited to one request per second per brand, and cleared by **Delete all data**. This decision adds no Room migration; D-040 supersedes that point for the removed brand.

## D-040 — Macey's removed from Phase 9 supported brands

- Date: 2026-11-20
- Status: accepted (amends D-039 and `03-store-integration.md`)
- Decision: Macey's is no longer a seeded brand, Settings locator option, or documented public locator target. Phase 9 supports five built-in brands: Walmart, Home Depot, Lowe's, Target, and Smiths. User-created custom stores and manual aisle entry remain available.
- Rationale: Macey's public website does not expose aisle locations, so offering branch selection would imply lookup capability the app cannot provide.
- Consequences: Room schema v4→v5 converts any legacy seeded Macey's store to an archived custom store named "Removed store" and clears its aisle cache; no item rows are deleted. Stale locator/cache keys are ignored and cleared by **Delete all data**. No automated aisle lookup is added.

## D-041 — Validated locator query and ZIP-only locator cache

- Date: 2026-11-22
- Status: superseded by D-043
- Decision: `StoreLocatorQuery` is the single validated input for the store-locator use case and repository. It carries one supported brand plus either an origin or a normalized postal code. Cache keys use only brand and ZIP; location-based results are never cached.
- Rationale: The brand/origin/ZIP triple was validated in three layers and persisted as a hash of rounded coordinates, which conflicts with the rule that coordinates are never stored. One value object removes the duplication, and a ZIP-only key preserves the 60-day cache without persisting location data.
- Consequences: A coarse-location search always hits the public locator; ZIP searches still reuse cached results for 60 days. Delete-all clears the locator cache as before.

## D-042 — Resource-backed context formatting outside ViewModels

- Date: 2026-11-22
- Status: accepted
- Decision: User-facing saved-branch context is formatted by `StoreContextFormatter`, implemented with Android resources and injected through Hilt. ViewModels consume the formatter; they do not contain hard-coded copy.
- Rationale: Hard-coded English strings in ViewModels cannot be localized and hide presentation logic in the state layer. A small injected formatter keeps strings in `strings.xml` without putting an Android `Context` in unit tests.
- Consequences: Unit tests supply a fake formatter. Adding new context copy requires a string resource and a formatter method, not ViewModel edits.

## D-043 — Generic WebView branch picker

- Date: 2026-11-22
- Status: accepted (supersedes D-039 and D-041; amends D-017, D-032, and D-036)
- Decision: Settings has a brand selector and a **Pick branch** button for Walmart, Home Depot, Lowe's, Target, and Smiths. Tapping **Pick branch** opens that brand's public store-locator page in a WebView; the user navigates the page and selects/confirms a branch. The app saves one branch per brand and shows it as store context. First use of a store requires picking its branch; Settings can change it later. Only the selected branch name and address metadata are saved. Custom stores have no automated locator. No ZIP storage, no "Enter store manually", no "Use my location", no location permission or dependency, and no HTTP locator parsing or cache.
- Rationale: User feedback (2026-11-22): the ZIP/location locator "doesn't work at all"; the user navigates retailer locator pages reliably in a WebView and should pick the branch there. A single generic flow replaces the Walmart-only verification screen and avoids collecting ZIP or coordinates.
- Consequences: Remove `ACCESS_COARSE_LOCATION`/`ACCESS_FINE_LOCATION` and the `play-services-location` dependency, all locator request/ranking/cache code, and the Walmart-only verified flag. D-039 and D-041 are superseded; D-017, D-032, and D-036 are amended as noted above. D-040 and D-042 stay accepted. Save/clear flows keep saved branches in SharedPreferences under one key per brand; **Delete all data** clears them. Postal codes parsed from a retailer page are discarded before persistence or display. No live gate is claimed for the rework until device verification is recorded.

## D-044 — Drop Accessibility Scanner gate

- Date: 2026-11-22
- Status: accepted
- Decision: Remove the Google Accessibility Scanner gate from tests, verification records, phase criteria, and specs. User feedback verbatim: "Ignored, we don't care about accessibility scanner". TalkBack, labels, touch targets, contrast, and other accessibility checks remain.
- Rationale: Scanner reports are not available in this environment and the product owner does not require them. Keeping the gate blocks release verification without adding accepted signal.
- Consequences: Manual tests start at the capture/organize demo; TalkBack traversal remains a required manual gate. No Scanner pass or partial result is recorded as a phase requirement.

## D-045 — Remove store hours and visit planning

- Date: 2026-11-22
- Status: accepted (amends D-014; supersedes the store-hours portions of Phase 8 and `06-scheduling-dates-calendar.md`)
- Decision: Remove the **Store hours & visit** action, manual hours entry, open/closed display, visit-window suggestions, `Store.hoursJson`, and the `preferred_shopping_time` setting from the MVP. The recurring schedule, calendar, deferral, and overdue features stay.
- Rationale: Product feedback (2026-11-22): "We also want to remove the store hours and visit features." The data was manually entered and unparseable, so it could not produce a trustworthy suggestion.
- Consequences: Room schema v6 drops `stores.hoursJson` with migration `MIGRATION_5_6`. `Store`/`StoreEntity`/`StoreMapper` no longer carry hours. Settings no longer stores or displays a preferred shopping time. Privacy text no longer lists store-hours or visit-time data. No Maps busyness data is used.

## D-046 — Store integration coverage inserted as Phase 10

- Date: 2026-11-22
- Status: accepted (inserts Phase 10; renumbers the pricing phases to 11 and 12; amends `AGENTS.md`, `docs/specs/README.md`, `00-product.md`, `03-store-integration.md`, and `08-pricing-and-custom-stores.md`)
- Decision: Add **Phase 10 — Store Integration Coverage** between the MVP release and pricing work. It verifies Home Depot HTTP aisle lookup and branch selection, implements or verifies automated aisle lookup and branch selection for Lowe's, Target, and Smiths, and keeps Walmart as a regression smoke only. Every supported brand gets a manual device test matrix. Custom stores stay manual-aisle only. The former Phase 10 (Pricing Spike) becomes Phase 11 and the former Phase 11 (Price Comparison) becomes Phase 12. The new Phase 10 is `not-started` and must not be started until explicitly instructed.
- Rationale: Product feedback (2026-11-22): "Walmart seems to be good but we need to do the rest and verify with some manual tests that they work." Multi-store routing is the core product promise; claiming coverage for five brands without verified lookups for four of them is misleading.
- Consequences: No price feature is pulled forward; Phase 11 still depends on a completed Phase 10 and Phase 12 on Phase 11. The existing store data access policy, explicit-branch-context rule, and `rawText` invariant are unchanged. No Room migration is expected; any new cache field requires its own migration and test. No new dependency is authorized by this decision. No implementation begins while Phase 10 is `not-started`.

