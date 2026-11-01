# Phase 7 — Background Routing

- **Status:** `not-started`
- **Depends on:** Phase 5, Phase 6
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-10-31

## Goal

Organize saved lists in the background: resolve pending aisles, group items by store, sort by aisle, and notify the user when the route is ready.

## Context

Phase 6 provides lookups. This phase makes organizing automatic and durable, with the foreground-service design required for Walmart WebView work.

## In scope

- WorkManager coordinator.
- Home Depot HTTP worker.
- Walmart foreground service with persistent notification.
- Notification channels and permission handling.
- Item state transitions.
- Grouping by store and aisle sorting.
- Progress and completion notifications.
- Retry, cancellation, and offline behavior.
- Manual refresh and retry actions.
- Tests for workers, coordinator, and notifications.

## Out of scope

- Schedule auto-creation and calendar (Phase 8).
- Price comparison.
- Sharing or authentication.
- AI in the background.

## Requirements

### Coordinator

- One organizing run per list.
- Marks eligible items `QUEUED`.
- Dispatches Home Depot and Walmart work.
- Writes results in small transactions.
- Re-sorts and groups after each result batch.
- Posts a completion notification with counts.
- New items added during a run are queued for the next run.
- Deleted items/lists cancel or discard work.
- Manual aisle edits are never overwritten.

### WorkManager

- Unique work name per list.
- `ExistingWorkPolicy.KEEP` for automatic runs.
- `REPLACE` for explicit refresh.
- Network constraint for lookup work.
- Exponential backoff, max three attempts.
- No retry on 403/429/challenge/parser failure.
- Persist status so process death does not lose state.

### Walmart foreground service

- Start with `startForeground()` immediately.
- Persistent notification with progress.
- WebView on the main thread.
- Per-item timeout.
- Stop and remove notification when done.
- Handle start failure by leaving items queued and showing an in-app message.
- Declare the correct foreground service type and permissions.

### Notifications

- Channel `organizing`.
- Progress notification is non-sensitive.
- Completion notification shows found/review/not found/error counts.
- Tapping opens the list.
- Stable notification ID per list.
- Cancel when the list is opened or work is cancelled.
- If `POST_NOTIFICATIONS` is denied, do not start the Walmart background service; queue for foreground.

### Offline and errors

- Offline: keep queued; retry when network returns.
- Blocked: stop the run and show guidance.
- Parser failure: mark item `ERROR`, continue others.
- Service killed: WorkManager retries or marks interrupted.
- User cancels: keep completed results; mark remaining pending.

## Technical approach

- `work/OrganizeListWorker.java`
- `work/HomeDepotLookupWorker.java`
- `work/WalmartLookupService.java`
- `work/OrganizingCoordinator.java`
- `work/NotificationHelper.java`
- `work/OrganizingScheduler.java`
- Use `Worker`/`ListenableWorker` and `ForegroundInfo`.
- Use `NotificationCompat` and notification channels.
- Use `ExecutorService` for DB writes.
- Use `LiveData`/`SharedFlow` alternatives? Use `LiveData` for in-app progress.
- No coroutines or Flow.

## Data changes

- Update `ListItem.lookupStatus` and match fields.
- Add any missing DAO queries for pending/queued items.
- No schema change expected; add a migration if needed.

## UI changes

- Lookup progress screen with per-store progress and cancel.
- Completion snackbar/dialog with counts.
- "Retry failed" action.
- "Organize now" action.
- Notification permission rationale.
- Unlocated section remains visible.

## Acceptance criteria

- [ ] Saving items starts an organizing run without keeping the app open.
- [ ] Home Depot work runs in WorkManager.
- [ ] Walmart work runs in a foreground service with a persistent notification.
- [ ] Notification permission denial queues Walmart items and does not crash.
- [ ] Items transition through queued/in-progress/found/not found/review/error states.
- [ ] Items are grouped by store and sorted by aisle.
- [ ] Ascending/descending toggle still works.
- [ ] Unresolved items remain last.
- [ ] Completion notification counts are accurate.
- [ ] Cancellation keeps completed results.
- [ ] Manual aisle values are not overwritten.
- [ ] Offline behavior is correct.
- [ ] 100 items organize without blocking the UI.
- [ ] Worker and notification tests pass.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Coordinator | Unit | Fake lookup services |
| Worker retry | WorkManager test | Transient failure |
| Blocked response | Unit | No retry |
| Foreground service | Instrumentation/manual | Notification |
| Permission denied | Instrumentation | Queue behavior |
| Cancellation | Unit | Completed results kept |
| Sorting/grouping | Unit | Multi-store |
| 100 items | Instrumentation | Performance |

## Manual demo

1. Add items for Walmart and Home Depot.
2. Tap "Organize now" or save.
3. Observe the foreground notification for Walmart.
4. Wait for completion and open the list.
5. Confirm grouping and aisle order.
6. Deny notification permission and repeat; confirm Walmart items queue.
7. Toggle descending and confirm unresolved items stay last.

## Risks

- Foreground service restrictions on newer Android versions. Mitigation: correct service type, start from user action, test on API 30–36.
- WebView lifecycle leaks. Mitigation: destroy and clear.
- Notification permission friction. Mitigation: explain, degrade gracefully.
- Race conditions with user edits. Mitigation: transaction checks and manual precedence.

## Open questions

- Should organizing start automatically on save or require a tap?
- Should the foreground service handle Home Depot too for a single progress notification?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
