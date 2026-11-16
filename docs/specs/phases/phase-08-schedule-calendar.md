# Phase 8 — Schedule and Calendar

- **Status:** `in-progress`
- **Depends on:** Phase 3, Phase 7
- **Owner:** unassigned
- **Started:** 2026-11-16
- **Completed:** —
- **Last updated:** 2026-11-16

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

- `ui/calendar/CalendarFragment.java`, `CalendarViewModel.java`, `CalendarDayAdapter.java`, `UpcomingAdapter.java`.
- `ui/settings/ScheduleFragment.java`, `ScheduleViewModel.java`.
- `work/ScheduleCheckWorker.java`, `ScheduleWorkerScheduler.java`, `ScheduleNotificationHelper.java`.
- `domain/usecase/CreateScheduledListUseCase.java`.
- `domain/usecase/DeferItemUseCase.java`.
- `domain/usecase/ResolveDatePhraseUseCase.java`.
- `domain/usecase/HandleOverdueListUseCase.java`.
- `domain/date/DatePhraseParser.java`.
- `domain/date/NearestListResolver.java`.
- Use `java.time.LocalDate`, `Clock`, and `SharedPreferences`.
- Use WorkManager periodic work and an on-open check.
- Use `MaterialDatePicker` for manual dates.

## Data changes

- Populate `ShoppingSchedule`.
- Add `targetDate` index usage.
- **Missed representation (chosen):** archiving the past list (soft archive). Unchecked items remain in the archived list and are not carried forward; checked items are unchanged. See `docs/specs/06-scheduling-dates-calendar.md`.
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

## Implementation status (2026-11-16)

**Implemented / in place (needs verification):**

- Preferred shopping time stored in settings/preferences/repository.
- List/item data/query support for range, nearest future, past unchecked, and unchecked-item move/target-date alignment.
- Deterministic date parsing and resolution: `DatePhraseParser`, `NearestListResolver`, `ResolveDatePhraseUseCase`, `DeferItemUseCase`.
- Scheduled list creation/advance/notification logic and worker wiring: `CreateScheduledListUseCase`, `ScheduleCheckWorker`, `ScheduleWorkerScheduler`, `ScheduleNotificationHelper`; periodic and app-open checks wired.
- Schedule settings UI/navigation/layout (`ScheduleFragment`, `ScheduleViewModel`, `ScheduleUiState`): interval/next date/enable-disable/notify, preferred shopping time, conflict handling, notification permission requested when enabling schedule with notify (Android 13+ as applicable).
- Calendar UI (`CalendarFragment`, `CalendarViewModel`, day/upcoming adapters/layouts): month grid with list badges, current-day highlight, open list or offer create for empty date, upcoming list, persisted last calendar month, day accessibility descriptions.
- Deferral in list-detail typed add flow: deterministic parsing, nearest existing active list on/after resolved date (or create for resolved date), confirmation showing resolved/target dates when different, past/ambiguous confirmation, phrase-removal confirmation option, and "next shopping day" without resolvable schedule prompts user to choose date or configure schedule.
- Overdue prompt in lists: once per past list per app session, options move unchecked / keep / mark missed, checked items never moved, target date shown, create-next offered when no future list exists (preferring next scheduled date when available). Missed implemented as archive of past list.
- Store hours/visit section in list detail: manual hours entry saved to `Store.hoursJson`, hours shown for selected date when known, open/closed shown only when explicitly present and parseable (otherwise stated unavailable/unknown), visit suggestion uses preferred shopping time when available and does not guess or use busyness data.
- Auto-created empty lists surface default/last-used stores (up to 3) in list-detail UI for quick add.
- Unit tests added/updated: `domain.date.DatePhraseParserTest`, `NearestListResolverTest`, `ResolveDatePhraseUseCaseTest`, `HandleOverdueListUseCaseTest`, `AdvanceScheduleUseCaseTest` (including month-end case).

## Remaining work

- **Capture/review deferral gap:** deterministic natural-language deferral parsing and per-line confirmation are not fully wired in capture/review save flows; manual per-item date picker and list-detail typed add flow are available. Implement and verify in capture/review if required for Phase 8 acceptance, or formally document scope deferral in completion/review.
- **Test coverage to complete per plan:** schedule advancement edge cases (DST/time zone/month-end) with fixed clock/zone as needed, full supported date-phrase coverage, nearest-list/create behavior, deferral Room transaction/move, `ScheduleCheckWorker` (WorkManager test), calendar UI (Espresso), overdue dialog/options (Espresso), and notification/fake coverage as appropriate.
- **Required verification (not yet recorded as passing):** `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew spotlessCheck`, and `./gradlew connectedDebugAndroidTest` on an emulator or physical device. Fix any failures and record command/test evidence.
- **Manual demos 1–7:** execute and record results in completion log.
- **Acceptance criteria:** verify and check off all criteria only after evidence shows required tests/checks ran and passed.
- **Completion:** keep phase `in-progress` until completion gate is satisfied and review is complete; then mark `done` in this file and `docs/specs/README.md`.

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
- [ ] Every test in the test plan has run and passed, including `connectedDebugAndroidTest` on the simulated device (emulator); no test has failed, errored, or been skipped.

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

## Completion gate

This phase cannot be marked `done` while any required test has failed, errored, been skipped, or failed to run. That includes every test in the test plan and `./gradlew connectedDebugAndroidTest` on the simulated device (Android emulator) or a physical device. An unavailable device is not a pass: leave the phase `in-progress` or `blocked` and record the unrun test in the completion log.

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
- (Resolved for Phase 8) Missed representation: archive past list (soft archive), unchecked items not carried forward.

## Spec-gap resolution (2026-11-16)

- **Deferral target:** nearest existing active list on/after resolved date, else create for resolved date. Confirmation shows resolved vs target date when different; phrase removal requires user confirmation. Implemented in list-detail typed add flow.
- **Next shopping day (no schedule):** prompts user to choose date or configure schedule (no silent add).
- **Overdue:** moves unchecked items to nearest future active list; if none exists, offers create for next scheduled date when available (else next day). "Missed" archives the past list (soft archive); unchecked items remain in archived list and are not carried forward. Checked items never moved.
- **Store hours/visit:** hours shown for selected date when known; open/closed shown only if explicitly present and parseable (otherwise stated unavailable/unknown). Visit suggestion uses preferred time when available and never guesses or uses busyness data.
- **Auto-created list default store sections:** surfaced as suggested stores (default/last-used, up to 3) in list-detail UI for quick add on auto-created empty lists (consistent with item-grouped store sections).
- **Capture/review deferral parsing:** deterministic phrase parsing and per-line confirmation in capture/review save flows are a documented follow-up gap if required for acceptance; manual per-item date picker and list-detail typed add flow are available. See `docs/specs/06-scheduling-dates-calendar.md`.

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-16 | Implementation (partial) | in-progress | Implemented core Phase 8 data/domain/work/UI flows and initial unit tests (schedule settings/calendar/deferral-in-list-detail/overdue/store-hours/auto-create wiring; see "Implementation status"). Spec clarifications recorded in `docs/specs/06-scheduling-dates-calendar.md` and "Spec-gap resolution". Required build/lint/unit/instrumented verification and full plan coverage (incl. `connectedDebugAndroidTest` on emulator/device), demos 1–7, and acceptance evidence not yet completed; phase remains `in-progress`. |
| 2026-11-16 | Phase spec update | in-progress | Updated this document to reflect current implementation status, spec-gap resolutions, and remaining work/evidence needed. |
