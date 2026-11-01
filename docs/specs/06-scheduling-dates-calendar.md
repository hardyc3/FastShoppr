# Scheduling, Dates, and Calendar Specification

## Goals

- Make the shopping date the organizing unit of the app.
- Let users defer items to a future shopping date with natural language.
- Show upcoming trips in a calendar and a date list.
- Auto-create recurring lists without requiring the app to be open.
- Prompt rather than silently move overdue items.
- Use store hours and the user's schedule for visit planning; do not use Google Maps busyness data.

## Date model

- `ShoppingList.shoppingDate` is the date of the trip.
- `ListItem.targetDate` determines which list contains the item.
- A list contains only items whose `targetDate` equals its `shoppingDate`.
- There is at most one active list per date.
- One-off lists are allowed outside the recurring schedule.
- Dates are `java.time.LocalDate` values stored as ISO `yyyy-MM-dd`.
- Use the device's local time zone for "today" and date comparisons.
- Use `java.time` with core library desugaring enabled.

## Recurring schedule

MVP supports one active schedule with a custom interval.

```text
ShoppingSchedule
  intervalValue: Int
  intervalUnit: DAYS | WEEKS
  nextShoppingDate: LocalDate
  isEnabled: Boolean
  notifyOnCreate: Boolean
```

Examples:

- Every 7 days.
- Every 2 weeks.
- Every 14 days.

Rules:

- `intervalValue` must be at least 1.
- The user picks the next shopping date when creating or editing the schedule.
- When a list is auto-created, advance `nextShoppingDate` by the interval.
- If the app was closed for multiple intervals, create only the next upcoming list, not a backlog of empty lists.
- Changing the schedule does not rewrite existing lists.
- Disabling the schedule stops future auto-creation but keeps existing lists.

## Auto-create behavior

- Use WorkManager to check for a due list at least once per day.
- Also check on app open so the behavior is correct even if periodic work was delayed.
- If a list for `nextShoppingDate` does not exist, create it.
- New auto-created lists include the user's default store sections (last used stores, up to three).
- If a list already exists for that date, do not duplicate it.
- Post a notification when a list is auto-created if notifications are enabled.
- The notification opens the new list.
- Notification permission is requested when the user enables the schedule, not at app launch.
- If permission is denied, still create the list; only skip the notification.

## Calendar view

- Month view with a badge/dot on dates that have lists.
- Past dates remain visible and tappable.
- Tapping a date with a list opens that list.
- Tapping a date without a list offers "Create list for this date".
- A secondary list shows upcoming shopping dates in chronological order.
- The current date is highlighted.
- Swipe or arrow controls change months.
- The calendar is read-only for item data; it navigates to lists.
- Show a summary on each date: list name, item count, store count.

## Natural-language deferral

Users can write or say things like:

- "toilet paper wait a week"
- "milk next week"
- "paper towels in 3 days"
- "batteries tomorrow"
- "light bulbs next month"

Behavior:

1. Parse the item text and date phrase.
2. Remove the date phrase from `rawText` only if the user confirms the split in review.
3. Resolve the phrase to a target `LocalDate`.
4. Find the nearest shopping list on or after that date.
5. If no list exists, create one for the resolved date.
6. Move the item to that list.
7. The item disappears from the current list and appears on the target list.

Parsing precedence:

1. Explicit date selected by the user.
2. Deterministic phrase rules.
3. On-device AI suggestion, if enabled.
4. Ask the user to pick a date.

Deterministic phrases to support:

- `tomorrow` -> today + 1 day.
- `in N days` -> today + N days.
- `next week` / `wait a week` -> current list date + 7 days.
- `in N weeks` -> current list date + 7 * N days.
- `next month` -> current list date plus one calendar month.
- `next shopping day` -> next scheduled list after the current list.
- `this weekend` -> the next Saturday relative to the current list date.

Rules:

- Resolve relative phrases against the current list's shopping date, not the device date, unless the phrase explicitly says "today" or "tomorrow".
- If the resolved date is in the past, ask the user to confirm.
- If the phrase is ambiguous, show the parsed date and let the user correct it.
- Never silently drop the item.
- AI may suggest a date, but the deterministic validator decides whether it is acceptable.

## Manual date control

- Every item edit screen has a date picker.
- The picker defaults to the current list date.
- Selecting another date moves the item to that date's list.
- Creating a list for an arbitrary future date is allowed.
- Past dates can be selected only through the overdue flow or explicit history editing.

## Overdue items

When the app opens and a past list has unchecked items, show a prompt.

Options:

1. **Move unchecked items to the next list** — update `targetDate` and move them in one transaction.
2. **Keep on the past list** — leave the list unchanged; it remains in history.
3. **Mark as missed** — set a `missed` state or archive the list; items are not carried forward.

Rules:

- Prompt once per past list per app session.
- Do not auto-move without consent.
- Checked items are never moved.
- The prompt shows the item count and the target list date.
- If no future list exists, offer to create one.
- The user can dismiss the prompt and handle it later.

## Store hours and visit planning

- Store hours may come from the retailer page or be entered manually.
- Show "Open now", "Closes at 9 PM", or "Closed" using the device's local time.
- When a shopping date is selected, show the store's hours for that day.
- Suggest a visit window based on:
  - Store opening hours.
  - The user's selected shopping date.
  - Optional preferred time of day.
- Do not claim to know how busy a store is.
- Do not integrate Google Popular Times, Google Maps busyness, or unofficial Maps scraping.
- If hours are unknown, say so and do not guess.

## Notifications

- Channel: `shopping_schedule`.
- Notification when a scheduled list is created.
- Notification when background organizing completes (owned by `07-background-work.md`).
- Tapping the schedule notification opens the list.
- Respect the user's notification setting.
- Do not notify for past lists or overdue prompts while the app is open.

## Data changes

- Add `ShoppingSchedule` table and DAO.
- Add `targetDate` index on `ListItem`.
- Add `missed` handling via list archive or an item status; document the chosen representation in the phase.
- Store the selected shopping time preference in `SharedPreferences`.
- Store the last calendar month in `SharedPreferences`.

## Edge cases

- Device time zone changes: recompute "today" on app open; do not rewrite stored dates.
- DST transitions: use `LocalDate`; do not add 24-hour milliseconds for date math.
- Leap years and month-end: use `LocalDate.plusMonths`.
- Duplicate date lists: return the existing list.
- Schedule disabled: no new auto-created lists.
- App not opened for months: create only the next upcoming list.
- Notification permission denied: create lists silently.
- Empty auto-created list: keep it and show an empty state; do not delete automatically.

## Testing

- Unit tests for interval advancement, date phrase parsing, nearest-list selection, and overdue options.
- DAO tests for unique shopping dates and transactional moves.
- WorkManager tests with `TestDriver` or a fake scheduler.
- Calendar UI tests for badges, navigation, and month changes.
- Notification tests with a fake notifier.
- Time-zone and DST tests using fixed clocks.

## Acceptance summary

- A recurring schedule creates dated lists and notifies the user.
- The calendar shows dates with lists and opens them.
- "Wait a week" moves an item to the correct future list.
- Overdue items prompt instead of moving silently.
- Store hours are shown without any Maps busyness dependency.
