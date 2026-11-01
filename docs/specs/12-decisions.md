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
