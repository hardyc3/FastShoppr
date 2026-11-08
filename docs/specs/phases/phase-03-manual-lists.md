# Phase 3 — Manual Lists

- **Status:** `done`
- **Depends on:** Phase 2
- **Owner:** agent session
- **Started:** 2026-11-08
- **Completed:** 2026-11-08
- **Last updated:** 2026-11-08

## Goal

Deliver a fully usable typed-list experience with dated lists, store sections, manual aisles, checkoff, and aisle sorting.

## Context

This is the first user-facing feature phase. It proves the core interaction before adding OCR, AI, or store lookups.

## In scope

- Home screen listing shopping dates and upcoming trips.
- List detail with store sections.
- Inline typed item entry under each store section.
- Item edit screen with raw text, display name, store, date, quantity, and manual aisle/bay.
- Manual aisle entry.
- Checkoff and undo.
- Aisle ascending/descending toggle.
- Store picker with default store and manual override.
- Calendar entry point (placeholder navigation; full calendar in Phase 8).
- Empty, loading, and error states.
- Dark theme support.
- Process-death state restoration.

## Out of scope

- OCR, handwriting, AI, store lookup, background organizing, scheduling, full calendar, pricing, sharing.

## Requirements

### Home

- Show upcoming lists ordered by shopping date.
- Show past lists in a separate section or toggle.
- Each row shows date, list name, item count, and store count.
- Actions: create list for a date, open list, archive list.
- Empty state with a "Create your first list" action.

### List detail

- Header: list name, shopping date, store selector, sort direction toggle.
- Store sections for each store represented in the list.
- Each section header shows store name, address (if known), and item count.
- Inline add field under each section.
- Item row shows:
  - Written name (`rawText`) prominently.
  - Cleaned display name only if it differs, as secondary text.
  - Matched product name when present, as secondary text.
  - Aisle/bay badge or "No aisle".
  - Checkbox.
  - Overflow menu: edit, defer, set aisle, delete.
- Items without a store are not allowed; the add field assigns the section's store.
- Unresolved items appear in a distinct "Needs attention" area at the end of the section.
- Sorting applies within each store section.
- Descending mode keeps unresolved items last.

### Item edit

- Fields: raw text, display name, quantity, store, target date, aisle, bay.
- Raw text edits are explicit and save only on confirm.
- Date picker moves the item to the target list on save.
- Manual aisle/bay saves as a manual lookup result.
- Validation: raw text required, max 200 characters.
- Cancel discards changes.

### Store picker

- Shows the five chain brands and any custom stores (custom CRUD is post-MVP).
- Default selection: last used store, then nearest store if available, then Walmart.
- Manual selection persists as last used.
- Show store address so the user can verify the location.

### Sort toggle

- Toggles `ASC`/`DESC`.
- Persists in `SettingsPreferences`.
- Re-sorts immediately.
- Unresolved items remain last.

## Technical approach

- Fragments: `ListsFragment`, `ListDetailFragment`, `ItemEditFragment`.
- ViewModels: `ListsViewModel`, `ListDetailViewModel`, `ItemEditViewModel`.
- Layouts: `fragment_lists.xml`, `fragment_list_detail.xml`, `fragment_item_edit.xml`, `item_list_row.xml`, `store_section_header.xml`.
- Adapters: `ShoppingListAdapter`, `ListItemAdapter` with `DiffUtil`.
- Use `RecyclerView` with a sectioned adapter or a `ConcatAdapter`.
- Use `LiveData` and `viewLifecycleOwner`.
- Use `Bundle` arguments for IDs.
- Use a date picker dialog (`MaterialDatePicker`).
- Use a store picker dialog or bottom sheet.
- Save UI state that must survive process death in `SavedStateHandle` where practical.

## Data changes

- Add queries for upcoming/past lists and grouped items.
- Add `sortPosition` updates for manual ordering if needed.
- No schema change unless a missing field is discovered; if so, add a migration.

## UI changes

- New layouts, strings, dimens, colors, and styles.
- Material 3 components: cards, chips, text fields, checkboxes, bottom sheets.
- Content descriptions for checkbox, overflow menu, and sort toggle.
- Empty/error states use a shared component.

## Acceptance criteria

- [x] User can create a list for a date.
- [x] User can open a list and see store sections.
- [x] User can add a typed item under a specific store section.
- [x] User can edit raw text, display name, quantity, store, date, aisle, and bay.
- [x] User can check and uncheck items.
- [x] User can toggle aisle ascending/descending and the order updates.
- [x] Unresolved aisles remain last in both directions.
- [x] User can defer an item to another date and it moves lists.
- [x] Written name is always visible and never replaced by the display name or match.
- [x] Store selection persists as the last used store.
- [x] Empty, loading, and error states render correctly.
- [x] State survives rotation and process death for the list detail screen.
- [x] Dark theme renders correctly.
- [x] Espresso tests cover the core manual flow.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Create list | Espresso | Date picker + list detail |
| Add typed item | Espresso | Under a store section |
| Edit item | Espresso | Raw text and aisle |
| Checkoff | Espresso | Persists |
| Sort toggle | Unit + Espresso | Unknown last |
| Defer item | Espresso | Moves lists |
| Process death | Instrumentation | SavedStateHandle |
| Empty state | Espresso | No lists |
| Store picker | Espresso | Last used persists |

## Manual demo

1. Create a list for next Saturday.
2. Add "milk" under Walmart, "light bulbs" under Home Depot, and "paper towels" under Target.
3. Enter manual aisles for each.
4. Toggle ascending/descending and confirm the order.
5. Check off milk and confirm it stays checked after restart.
6. Defer paper towels to the following week and confirm it moves.
7. Open the item edit screen and confirm the written name remains visible.

## Risks

- Sectioned RecyclerView complexity. Mitigation: use `ConcatAdapter` or a single adapter with view types.
- Process-death restoration is easy to miss. Mitigation: explicit SavedStateHandle tests.
- Date picker time-zone bugs. Mitigation: use `LocalDate` and fixed-clock tests.

## Open questions

- Resolved: list detail shows every active store section so any store can accept an inline item.
- Resolved: checked items stay in place with strikethrough and an unchecked toggle.

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-08 | `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck`, `assembleDebugAndroidTest` | Pass | Zero lint warnings. |
| 2026-11-08 | All instrumentation tests via `am instrument -w` on the API 36 emulator | Pass (17 tests) | Includes `ManualListsFlowTest` and `ListDetailStateRestorationTest`. Gradle `connectedDebugAndroidTest` was not used because the WSL Gradle toolchain could not see the Windows-hosted emulator. |
| 2026-11-08 | Manual demo equivalent | Pass | Espresso covers create list, add under Walmart, manual aisle, checkoff, sort toggle, raw-text edit, store picker, empty state, and rotation/recreate draft restoration. Deferral is covered by `RoomDataLayerTest.moveItemMovesToListTransactionallyAndCreatesMissingList`. |
