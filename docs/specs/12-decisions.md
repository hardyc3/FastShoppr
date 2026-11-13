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
- Consequences: Phases 10–11 cover pricing. The MVP data model reserves a `PriceQuote` entity but does not expose price UI.

## D-013 — Sharing, sync, and Google Sign-In are deferred

- Date: 2026-10-31
- Status: deferred
- Decision: Do not implement real-time sharing, multi-user editing, Google Sign-In, or cloud sync in Phases 0–11. Record the intended design in `11-future-sharing-sync.md`.
- Rationale: Collaboration requires a backend, auth, access rules, and conflict handling. It is a separate effort.
- Consequences: The MVP remains local-only. The data model uses stable UUIDs and `updatedAt` so future sync is possible.

## D-014 — No Google Maps Popular Times

- Date: 2026-10-31
- Status: accepted
- Decision: Do not integrate Google Maps Popular Times or scrape Maps busyness data. Use store hours and the user's schedule for visit planning.
- Rationale: There is no official Popular Times API, and scraping Maps is unreliable and against terms.
- Consequences: The app shows open/closed and hours-based suggestions only. It never claims to know current busyness.

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
- Status: accepted
- Decision: Default to the nearest store using location when the user opts in, with a manual store/ZIP override. Show the selected store in the UI.
- Rationale: Aisle data is store-specific. Location improves defaults, but the user must be able to correct it.
- Consequences: Location permission is requested only on demand. Denial falls back to last used or manual selection. Coordinates are never logged.

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
- Consequences: Phase 6 ships with manual aisle entry as the guaranteed path. Lookup is an enhancement, not a requirement. Fixtures must be re-verified on a real device before trusting the parser. Price extraction (Phase 11) is likewise blocked: automated price lookup is a no-go until a real device passes the challenges; plan manual prices.

## D-022 — Walmart WebView extraction verified on emulator; Home Depot remains blocked

- Date: 2026-11-03
- Status: accepted (amends D-021)
- Decision: Walmart lookup is **conditionally viable** via the D-004 design: on 2026-11-03 an emulator WebView session loaded walmart.com and three search pages (milk, eggs, paper towels) once past PX; aisles extracted from `__NEXT_DATA__` (`"Location":[{"displayValue":"A29","aisle":{"zone":"A","aisle":29}}]` — milk→A29, eggs→AC4, paper towels→J29), prices and default store context visible. PX pass is intermittent (≈50% of sessions; plain HTTP always blocked). Home Depot remains **blocked** (HTTP 403 Akamai from curl, OkHttp, and interactive WebView). Lookup stays: user-initiated, foreground WebView only, cached, manual override always available, never unattended. Walmart aisle parsing keys off `LocationDisplayValue`/`"Location":[...]` in `__NEXT_DATA__`; fixture tests must use sanitized, trimmed captures.
- Rationale: Phase 0 evidence (fixtures `walmart/webview-*.json`, report §1/§2). D-021's blanket "no automated path" was too strong for Walmart; HD unchanged.
- Consequences: Phase 6 may implement Walmart assisted lookup (flag-gated, user-triggered). HD ships manual-entry-first; HD HTTP stays dead until a real-device/mobile-data pass. Phase 11 pricing extraction viable for Walmart/Target where pages load; HD no-go; manual prices remain the guaranteed path everywhere. Nothing here authorizes background or headless lookup.

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
- Status: accepted (amends D-022)
- Decision: Walmart exposes no reliable programmatic way to pin a physical store from a plain WebView load (a `stores=<number>` query parameter was observed **not** to change server-side context; the store is set via Walmart's own assisted UI or defaulted by IP). The lookup harness therefore provides a user-initiated **"Verify your Walmart store"** screen that hosts the regular walmart.com site in a WebView; the user picks their store there. The internal WebView cookie jar **persists** across lookups (and app restarts) until the user taps **"Forget store"**, which wipes Walmart cookies and DOM storage in that screen. Aisles are still downgraded to `NEEDS_REVIEW` (below the 0.6 auto-accept threshold) because context cannot be proven programmatically; the user verifies on the shelf or confirms via "Not right?".
- Rationale: D-022's assisted flow requires the user to select the store once; persisting cookies makes that one-time pick actually effective. Silent guessing was the alternative (wrong-store aisles reported as FOUND), rejected per store-context rules. Cookie clearing at every run would make the store pick useless; a visible "Forget store" action keeps retention honest and local.
- Consequences: `WalmartWebViewLookupService` no longer wipes cookies/DOM storage on teardown (WebView instances are still created/destroyed per lookup). A `WalmartStoreFragment` (`walmart_store` nav destination) hosts the assisted session. Normal automated/background lookups remain out of scope (Phase 7). Store context remains unverified for acceptance purposes until checked on a physical device.

## D-033 — Multiple-match candidates persist on the item; `matchCandidatesJson` column (schema v3)

- Date: 2026-11-13
- Status: accepted
- Decision: The last `Multiple` result's candidate list is serialized (Gson) into a new nullable `ListItem.matchCandidatesJson` column (Room **2→3**, `MIGRATION_2_3` adds the column; existing rows remain `NULL`). The item-row "Not right?" action decodes and offers the persisted candidates (pick one, or enter manually) when the item is `NEEDS_REVIEW`; candidates are cleared on any `Found`/`Blocked`/`NotFound`/`Error` result, on cache hits, on manual overrides, on candidate confirmation, and on store changes. Candidates never replace `rawText` and never bypass manual precedence; `applyCandidate` remains the single write path.
- Rationale: candidates were previously discarded when the lookup harness finished, so the item row had no way to offer a re-pick (spec "Candidate selection dialog for Multiple" unreachable outside the harness run). Persisting the small payload is simpler and more honest than re-scraping (cache intentionally stores only `Found`).
- Consequences: schema version bump 2→3 requires the migration to run on upgrades; `MigrationTest` covers 2→3. Candidates are untrusted retailer text — displayed only, never executed. Item payload in memory/Room grows by a few KB for `NEEDS_REVIEW` items only.

## D-034 — Store lookup confidence must be calculated, not hardcoded

- Date: 2026-11-13
- Status: accepted
- Decision: Aisle extraction confidence must be derived from observable extraction evidence (structured JSON fields, product metadata, fallback source, ambiguity, and store-context verification). Fixed constants may be used only as base weights that are adjusted by evidence, not as the final confidence for every match. Regex simplification and confidence calculation are tracked in Phase 6c after Phase 6b.
- Rationale: Phase 6 currently assigns fixed confidence values in `WalmartAisleExtractor` and `HomeDepotAisleParser`, which hides extraction quality and makes review thresholds less meaningful. Real-page verification in Phase 6b must inform the final scoring signals.
- Consequences: Phase 6 remains done for its automated/manual scope, but confidence quality is a tracked follow-up. Future lookup code must preserve calculated confidence through repository, cache, and UI paths.

