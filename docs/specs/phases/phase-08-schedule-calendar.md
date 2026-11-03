# Phase 8 — Schedule and Calendar

- **Status:** `not-started`
- **Depends on:** Phase 3, Phase 7
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-03

## Goal

Make shopping dates first-class: recurring schedules, auto-created lists, a calendar view, natural-language deferral, overdue prompts, and store-hours-based visit planning.

## Context

Dates exist in the data model. This phase adds the user-facing scheduling and calendar experience and the deferral parser that moves items between lists.

## In scope

- Schedule settings UI with custom interval.
- WorkManager daily schedule check and on-app-open check.
- Auto-created lists with default store sections.
- Schedule notification.
- Calendar month view with list badges.
- Upcoming shopping dates list.
- Natural-language deferral parser and manual date picker.
- Overdue prompt with move/keep/missed options.
- Store hours display and visit-time suggestions.
- Tests for dates, scheduling, calendar, and deferral.

## Out of scope

- Google Maps Popular Times or busyness data.
- Price comparison.
- Sharing or authentication.
- Complex recurrence rules such as "third Saturday" or multiple schedules.

## Requirements

### Schedule

- One active schedule.
- Interval: every N days or N weeks, N >= 1.
- User selects the next shopping date.
- Enable/disable and notification toggle.
- Advancing creates only the next upcoming list, not a backlog.
- Changing the schedule does not rewrite existing lists.

### Auto-create

- WorkManager checks at least daily.
- Also check on app open.
- Create the list if missing.
- Add default store sections (last used, up to three).
- Do not duplicate an existing date list.
- Notify if enabled and permission granted.
- Create silently if notifications are denied.

### Calendar

- Month view with badges on dates with lists.
- Current date highlighted.
- Tap a date with a list -> open it.
- Tap a date without a list -> offer "Create list".
- Past dates visible and tappable.
- Upcoming dates list below or beside the calendar.
- Show list name, item count, store count.

### Deferral

- Parse phrases: `tomorrow`, `in N days`, `next week`, `wait a week`, `in N weeks`, `next month`, `this weekend`, `next shopping day`.
- Resolve relative to the current list date, except explicit "today"/"tomorrow".
- Find nearest list on or after the target date.
- Create a list if none exists.
- Move the item transactionally.
- Remove the date phrase from the item only after user confirmation.
- Ambiguous phrases show the parsed date for confirmation.
- AI may suggest a date in Phase 5's hook, but deterministic rules decide.

### Overdue

- Prompt once per past list per session.
- Options: move unchecked items to next list, keep on past list, mark missed.
- Checked items never move.
- If no future list exists, offer to create one.
- Dismissible; can be handled later.

### Store hours

- Show open/closed and hours for the selected shopping date.
- Allow manual hours entry when unknown.
- Suggest a visit window using hours and the user's preferred time.
- Never claim to know busyness.
- No Google Maps API.

## Technical approach

- `ui/calendar/CalendarFragment.java`, `CalendarViewModel.java`, `MonthAdapter.java`.
- `ui/settings/ScheduleFragment.java`, `ScheduleViewModel.java`.
- `work/ScheduleCheckWorker.java`.
- `domain/usecase/CreateScheduledListUseCase.java`.
- `domain/usecase/DeferItemUseCase.java`.
- `domain/usecase/ResolveDatePhraseUseCase.java`.
- `domain/date/DatePhraseParser.java`.
- `domain/date/NearestListResolver.java`.
- `ui/components/OverdueDialogFragment.java`.
- Use `java.time.LocalDate`, `Clock`, and `SharedPreferences`.
- Use WorkManager periodic work and an on-open check.
- Use `MaterialDatePicker` for manual dates.

## Data changes

- Populate `ShoppingSchedule`.
- Add `targetDate` index usage.
- Add `missed` representation: prefer archiving the past list or an item status field; document the choice in the completion log.
- Add preferred shopping time to `SettingsPreferences`.
- No other schema change expected.

## UI changes

- Schedule settings screen.
- Calendar screen.
- Date list on the home screen.
- Deferral dialog and date picker.
- Overdue dialog.
- Store hours section in list detail.
- Strings and accessibility labels.

## Acceptance criteria

- [ ] User can create, edit, enable, and disable a custom interval schedule.
- [ ] A scheduled list is auto-created and notified.
- [ ] Duplicate dates are not created.
- [ ] The calendar shows badges and opens lists.
- [ ] The upcoming dates list is accurate.
- [ ] "Wait a week" moves an item to the correct future list.
- [ ] Manual date selection moves an item correctly.
- [ ] Ambiguous phrases require confirmation.
- [ ] Overdue prompt offers move/keep/missed and never moves silently.
- [ ] Store hours render for the selected date.
- [ ] No Maps busyness data or API is used.
- [ ] Date parsing and schedule advancement have unit tests.
- [ ] Calendar and overdue flows have UI tests.
- [ ] DST, month-end, and time-zone edge cases are tested.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Interval advancement | Unit | Days/weeks, month-end |
| Date phrase parser | Unit | All supported phrases |
| Nearest list resolver | Unit | Create if missing |
| Defer transaction | Room | Moves item |
| Schedule worker | WorkManager test | Auto-create |
| Calendar UI | Espresso | Badges/navigation |
| Overdue dialog | Espresso | Three options |
| DST/time zone | Unit | Fixed clocks |

## Manual demo

1. Set a schedule for every 2 weeks.
2. Trigger the schedule check and confirm a list is created and notified.
3. Open the calendar and tap the date.
4. Add "toilet paper wait a week" and confirm it moves to the next list.
5. Let a list become overdue and choose "Move unchecked items".
6. Confirm store hours are shown for the selected date.
7. Toggle notifications off and confirm lists still auto-create.

## Risks

- WorkManager periodic timing is inexact. Mitigation: also check on app open.
- Date phrase ambiguity. Mitigation: confirmation UI and deterministic validation.
- Calendar performance with many lists. Mitigation: query only the visible month.
- DST/time-zone bugs. Mitigation: `LocalDate` and fixed-clock tests.

## Open questions

- Should the schedule support multiple active schedules later?
- Should missed items be archived with the list or kept with an item status?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
