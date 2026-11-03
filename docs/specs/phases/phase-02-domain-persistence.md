# Phase 2 — Domain and Persistence

- **Status:** `not-started`
- **Depends on:** Phase 1
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-03

## Goal

Implement the domain models, Room schema, repositories, use cases, and settings needed for dated multi-store lists, without building the full UI.

## Context

This phase establishes the data invariants that every later feature depends on: raw-text preservation, date-driven membership, store grouping, aisle sorting, and lookup state.

## In scope

- Domain models and enums.
- Room entities, DAOs, database version 2, and migrations.
- Repository interfaces and implementations.
- Use cases for list/item/store/schedule operations.
- Aisle comparator.
- UUID generation and injected clock.
- Seed data for the five chain brands.
- `SettingsPreferences` integration for default store and sort direction.
- Unit and DAO tests.

## Out of scope

- Full list UI.
- OCR, AI, lookup, background work, scheduling UI, calendar UI, or pricing.
- Sharing or authentication.

## Requirements

### Models

- `Store`, `ShoppingList`, `ListItem`, `CaptureSession`, `ShoppingSchedule`, `AisleCache`.
- Enums: `StoreBrand`, `LookupStatus`, `LookupSource`, `ItemSource`, `IntervalUnit`, `SortDirection`.
- Immutable Java classes with constructor validation.
- Separate entity models and domain models with explicit mappers.

### Lists and items

- One active list per shopping date.
- Creating a list for an existing date returns the existing list.
- `ListItem.targetDate` must equal the containing list's `shoppingDate`.
- Moving an item to another date moves it to that date's list in one transaction.
- `rawText` is preserved; `displayName` and `normalizedText` are derived.
- Deleting a list cascades to items and capture sessions.
- Archiving a list removes it from active lists but keeps history.

### Stores

- Seed Walmart, Home Depot, Lowe's, Target, and Smiths.
- Store selection supports default and last used.
- Custom stores are represented in the schema but no UI is added yet.
- Deleting a store is blocked while referenced; use archive.

### Sorting

- Implement the numeric-aware aisle comparator from `02-data-model.md`.
- Unresolved aisles sort last in both directions.
- Tie-break by bay then `sortPosition`.
- Expose ascending and descending modes.

### Preferences

- `SettingsPreferences` supports default store, last used store, sort direction, notifications, background organizing, AI cleanup, and onboarding.
- Use SharedPreferences only; no DataStore.

### Use cases

- `CreateListUseCase`
- `AddItemUseCase`
- `UpdateItemUseCase`
- `DeleteItemUseCase`
- `MoveItemToDateUseCase`
- `ToggleItemCheckedUseCase`
- `ArchiveListUseCase`
- `CreateScheduleUseCase`
- `AdvanceScheduleUseCase`
- `SortItemsUseCase`

## Technical approach

- Use Room with `annotationProcessor`.
- Use `LiveData` for observable queries and `ExecutorService` for one-shot writes.
- Use `@Transaction` methods for moves and cascades.
- Use UUID strings for IDs.
- Inject `Clock` for timestamps and date defaults.
- Map entities to domain models in `data/repository/mappers/`.

## Data changes

- Database version 2.
- Entities: `StoreEntity`, `ShoppingListEntity`, `ListItemEntity`, `CaptureSessionEntity`, `ShoppingScheduleEntity`, `AisleCacheEntity`.
- Indices from `02-data-model.md`.
- Migration test for version 1 -> 2.

## UI changes

- None beyond placeholder wiring needed to exercise the use cases.

## Acceptance criteria

- [ ] Domain models and enums exist and are immutable.
- [ ] Room database version 2 builds and exports its schema.
- [ ] Migration 1 -> 2 passes.
- [ ] Creating a list for an existing date returns the existing list.
- [ ] Adding an item persists it with `rawText`, `displayName`, `normalizedText`, `targetDate`, and `storeId`.
- [ ] Moving an item to another date moves it to the target list transactionally.
- [ ] Deleting a list removes its items and capture sessions.
- [ ] Aisle comparator passes ascending, descending, unknown-last, and tie-break tests.
- [ ] Store seed data exists exactly once.
- [ ] `SettingsPreferences` reads and writes all defined keys.
- [ ] No Kotlin, Compose, coroutines, Flow, or DataStore is introduced.
- [ ] Unit tests and DAO tests pass.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Aisle comparator | Unit | `03`, `A12`, `EC2`, unknown, desc |
| Date membership | Unit | Target date equals list date |
| Move item transaction | Room | Move between two lists |
| Cascade delete | Room | List deletes items/captures |
| Unique shopping date | Room | Duplicate create returns existing |
| Store seed | Room | Exactly five brands |
| Preferences | Unit | Read/write each key |
| Migration 1 -> 2 | Room | `MigrationTestHelper` |

## Manual demo

1. Run the unit and DAO tests.
2. Use a debug-only screen or test harness to create a list for a date.
3. Add items with different aisles and sort them.
4. Move an item to a future date and confirm it leaves the original list.
5. Restart the app and confirm persistence.

## Risks

- Room + Java annotation processing issues. Mitigation: keep entities simple and verify early.
- Date handling around DST/time zones. Mitigation: use `LocalDate` and fixed-clock tests.
- Transactional move bugs. Mitigation: DAO transaction tests.

## Open questions

- Should archived lists be included in the calendar by default?
- Should item `quantity` be a free-form string or structured in a later phase?

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
