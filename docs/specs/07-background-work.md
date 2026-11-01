# Background Work Specification

## Goals

- Organize lists after the user saves items, without requiring the app to stay open.
- Keep long-running WebView work visible and reliable.
- Respect Android background execution limits and notification permissions.
- Never lose lookup work or silently drop items.
- Never run on-device AI in the background.

## Work types

| Work | Runner | Constraints | Visible |
|---|---|---|---|
| Home Depot HTTP lookup | WorkManager `Worker`/`ListenableWorker` | Network connected | Notification on completion |
| Walmart WebView lookup | Foreground service | Network connected | Persistent notification |
| Organizing coordinator | WorkManager | Network connected | Progress + completion notification |
| Schedule check | WorkManager periodic | None | Notification when a list is created |
| Cache cleanup | WorkManager periodic | None | None |

## WorkManager usage

- Use WorkManager for durable, deferrable work.
- Use unique work names:
  - `organize_list_<listId>`
  - `lookup_homedepot_<listId>`
  - `schedule_check`
  - `cache_cleanup`
- Use `ExistingWorkPolicy.KEEP` for organizing a list so duplicate runs do not stack.
- Use `ExistingWorkPolicy.REPLACE` when the user explicitly requests a refresh.
- Set a backoff policy of exponential with a 30-second initial delay.
- Limit retries to three attempts for transient errors.
- Do not retry HTTP 403, 429, CAPTCHA, or parser failures.
- Use `NetworkType.CONNECTED` for lookup work.
- Use `setForeground`/`ForegroundInfo` where a worker must show progress.
- Persist item status transitions (`QUEUED`, `IN_PROGRESS`, `FOUND`, etc.) so process death does not lose state.

## Organizing coordinator

1. User saves items or taps "Organize now".
2. Coordinator marks eligible items `QUEUED`.
3. Home Depot items are dispatched to an HTTP worker.
4. Walmart items are dispatched to the foreground service.
5. Results are written to the database in small transactions.
6. The list is re-sorted and grouped by store.
7. A completion notification is posted with counts: found, needs review, not found, errors.
8. Tapping the notification opens the list.

Rules:

- One organizer run per list at a time.
- New items added during a run are queued for the next run.
- Manual aisle edits are never overwritten by an in-flight lookup result.
- If the user deletes an item during a run, discard its result.
- If the user deletes the list, cancel the work.
- Do not block the list UI while organizing runs.

## Walmart foreground service

Walmart WebView work must run in a foreground service.

Requirements:

- Declare the service in the manifest.
- Declare `FOREGROUND_SERVICE` and the appropriate foreground service type (for example `dataSync`) for the target SDK.
- Start with `startForeground()` immediately.
- Show a persistent notification: "Organizing your Walmart list" with progress.
- Notification channel: `organizing`.
- Keep the WebView on the main thread.
- Use a dedicated WebView instance with JavaScript enabled.
- Apply a per-item timeout (default 20 seconds).
- Stop the service and remove the notification when done.
- Cancel cleanly if the user stops organizing.
- Do not start the service from the background without an allowed exemption; start from a user action or an expedited worker where appropriate.
- If the service cannot start, leave items `QUEUED` and show an in-app message.

### Notification permission

- Request `POST_NOTIFICATIONS` when the user enables background organizing.
- If denied, Walmart lookups remain queued until the app is in the foreground.
- Never crash or silently fail when permission is denied.
- The app must still allow manual aisle entry and foreground lookup.

## Home Depot HTTP worker

- Use OkHttp with timeouts and a descriptive user agent.
- Respect the one-request-per-second-per-store limit.
- Stop on block responses.
- Write each successful result immediately.
- Mark failures with actionable status.
- Do not hold a database transaction open during network calls.

## Progress and notifications

- Progress notification shows a short, non-sensitive summary.
- Do not include item names, store addresses, or list contents in notifications.
- Completion notification includes counts and opens the list.
- Cancel notifications when the list is opened or the work is cancelled.
- Use a stable notification ID per list.

## Failure handling

| Failure | Behavior |
|---|---|
| Offline | Keep queued; retry when network returns |
| HTTP 403/429 | Stop run; mark blocked; show guidance |
| Parser changed | Mark item `ERROR`; keep others running |
| Item not found | Mark `NOT_FOUND`; allow manual aisle |
| Service killed | WorkManager retries or marks interrupted; user can retry |
| Notification denied | Queue Walmart items for foreground |
| User cancels | Stop work; keep completed results; mark rest pending |

## AI constraint

- Do not run Gemini Nano or any on-device AI in a Worker, service, or background thread without a foreground activity.
- AI cleanup happens at input/review time in the foreground.
- Background organizing uses deterministic parsing only.

## Testing

- Unit-test the coordinator with fake lookup services.
- Use WorkManager `TestDriver`/`TestListenableWorkerBuilder` for worker tests.
- Test notification permission denied paths.
- Test cancellation and process-death recovery.
- Test that manual aisle edits are not overwritten.
- Instrumentation test the foreground service lifecycle where practical.
- Do not run live retailer calls in CI.

## Acceptance summary

- Saving items starts organizing without keeping the app open.
- Walmart WebView work shows a persistent notification.
- Home Depot HTTP work runs in WorkManager.
- Completion notifications are accurate and non-sensitive.
- Failures leave items actionable.
- No AI runs in the background.
