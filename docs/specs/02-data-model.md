# Data Model Specification

## Conventions

- IDs are UUID strings generated on device.
- Timestamps are UTC epoch milliseconds (`long`).
- Dates that represent a shopping day are stored as `LocalDate` converted to an ISO `yyyy-MM-dd` string in the database.
- Money, if added post-MVP, is stored as integer minor units plus currency code.
- Room entities are separate from domain models. Map explicitly.
- No nullable field is used to mean "unknown" when a dedicated enum exists.
- Domain classes are immutable Java classes with constructor validation.

## Entities

### Store

```text
Store
  id: String (PK)
  brand: StoreBrand
  name: String                 // e.g. "Walmart Supercenter - Sacramento"
  addressLine: String?
  city: String?
  state: String?
  postalCode: String?
  latitude: Double?
  longitude: Double?
  isCustom: Boolean
  isDefault: Boolean
  isArchived: Boolean
  hoursJson: String?           // serialized weekly hours, nullable
  createdAt: Long
  updatedAt: Long
```

Rules:

- The five chain brands are seeded or created on demand.
- `isCustom` is true only for user-defined local stores (post-MVP UI).
- `isArchived` hides a store from active selection while keeping its referenced history.
- A store is selected per item; the default is the first active store by brand order.
- One physical branch per supported brand may be saved as verification context. It is chosen by the user in a WebView locator and is never inferred from device location.
- Device location coordinates and user-entered ZIP codes are neither collected nor stored.

### ShoppingList

```text
ShoppingList
  id: String (PK)
  name: String
  shoppingDate: String         // ISO date, unique among active lists
  scheduleId: String?          // null for one-off lists
  isAutoCreated: Boolean
  isArchived: Boolean
  deletedAt: Long?
  createdAt: Long
  updatedAt: Long
```

Rules:

- Exactly one active list per `shoppingDate`. This is enforced in a repository transaction over active, non-deleted lists.
- Auto-created lists use the schedule's date and default stores.
- Archiving a list keeps history and removes it from the active list.
- Deleting a list soft-deletes its items and capture sessions in one transaction by stamping their `deletedAt` with the list's deletion timestamp.

### ListItem

```text
ListItem
  id: String (PK)
  listId: String (FK -> ShoppingList.id, indexed)
  storeId: String (FK -> Store.id, indexed)
  rawText: String              // written/recognized name, never replaced by lookup
  displayName: String          // user-facing cleaned name
  normalizedText: String       // derived for lookup
  quantity: String?            // free-form, e.g. "2", "1 gal"
  targetDate: String           // ISO date; determines list membership
  aisle: String?               // retailer aisle value
  bay: String?                 // retailer bay value
  matchedProductName: String?  // retailer product title
  matchedProductUrl: String?
  matchConfidence: Float?      // 0.0..1.0 when known
  lookupStatus: LookupStatus
  lookupSource: LookupSource?
  lookupError: String?         // sanitized, user-safe message
  source: ItemSource
  isChecked: Boolean
  sortPosition: Int            // stable insertion order tie-breaker
  deletedAt: Long?
  createdAt: Long
  updatedAt: Long
```

Rules:

- `rawText` is immutable except by an explicit user edit.
- `displayName` defaults to `rawText`; AI cleanup requires confirmation.
- `normalizedText` is generated from `displayName` and is not user-visible.
- `targetDate` must match the `shoppingDate` of the containing list.
- Moving an item to another date moves it to that date's list in a transaction.
- `matchedProductName` is display-only; it never overwrites `rawText`.
- `lookupStatus` is the source of truth for the UI state of a lookup.

### CaptureSession

```text
CaptureSession
  id: String (PK)
  listId: String (FK, indexed)
  source: ItemSource
  imageUri: String?            // local content URI, deleted after review
  recognizedLinesJson: String  // serialized recognition output
  createdAt: Long
  completedAt: Long?
  deletedAt: Long?
```

Rules:

- Images are stored in app-private cache storage.
- Delete the image after the review is saved or discarded.
- Never log image URIs or contents.

### ShoppingSchedule

```text
ShoppingSchedule
  id: String (PK)
  intervalValue: Int           // e.g. 1, 2, 7
  intervalUnit: IntervalUnit   // DAYS or WEEKS
  nextShoppingDate: String     // ISO date
  isEnabled: Boolean
  notifyOnCreate: Boolean
  lastCreatedDate: String?
  createdAt: Long
  updatedAt: Long
```

Rules:

- MVP supports one active schedule.
- `intervalValue` must be at least 1.
- A one-off list can exist outside the schedule.
- When a list is auto-created, advance `nextShoppingDate` by the interval.
- Changing the schedule does not rewrite existing lists.

### AisleCache

```text
AisleCache
  id: String (PK)
  storeId: String (indexed)
  normalizedQuery: String      // normalized item name used for the lookup
  aisle: String?
  bay: String?
  matchedProductName: String?
  matchedProductUrl: String?
  confidence: Float?
  source: LookupSource
  fetchedAt: Long
  expiresAt: Long
```

Rules:

- Default TTL is 60 days.
- A cache hit does not require network access.
- Expired entries may be used offline with an "outdated" flag.
- Cache is keyed by store and normalized query; store context matters.
- Do not cache blocked or failed responses.

### PriceQuote (post-MVP)

```text
PriceQuote
  id: String (PK)
  storeId: String (indexed)
  itemId: String?              // null when the quote is for a search term
  normalizedQuery: String
  priceMinorUnits: Long
  currencyCode: String
  productName: String?
  productUrl: String?
  source: PriceSource
  observedAt: Long
  expiresAt: Long
```

Rules:

- Prices are location- and time-sensitive.
- Manual quotes always show their source and timestamp.
- Do not claim a price is current if it is expired.
- No price data leaves the device.

## Enums

```java
public enum StoreBrand { WALMART, HOME_DEPOT, LOWES, TARGET, SMITHS, CUSTOM }

public enum LookupStatus {
    PENDING, QUEUED, IN_PROGRESS, FOUND, NOT_FOUND, NEEDS_REVIEW, ERROR
}

public enum LookupSource { WALMART_WEBVIEW, HOME_DEPOT_HTTP, MANUAL, CACHE }

public enum ItemSource { TYPED, DIGITAL_INK, CAMERA_OCR, GALLERY_OCR }

public enum IntervalUnit { DAYS, WEEKS }

public enum PriceSource { STORE_PAGE, MANUAL, RECEIPT }

public enum SortDirection { ASC, DESC }
```

`LookupStatus` semantics:

- `PENDING` — not yet attempted.
- `QUEUED` — waiting for a worker or foreground service.
- `IN_PROGRESS` — currently being resolved.
- `FOUND` — aisle resolved with acceptable confidence.
- `NOT_FOUND` — search completed with no usable result.
- `NEEDS_REVIEW` — multiple or low-confidence results; user must choose or enter manually.
- `ERROR` — transient/blocking failure; retry is possible.

## Relationships

```text
ShoppingSchedule 1 --- * ShoppingList
ShoppingList     1 --- * ListItem
Store            1 --- * ListItem
ShoppingList     1 --- * CaptureSession
Store            1 --- * AisleCache
Store            1 --- * PriceQuote
```

Deletion rules:

- Deletion is a soft delete with a cleanup after 30 days. The user can restore deleted lists during the 30-day window.
- Deleting a list stamps the same `deletedAt` on the list, its items, and its capture sessions; restoring the list restores only records deleted with it.
- Restoring a list fails if another active list already exists for that `shoppingDate`.
- Deleting a store is blocked while items or cache entries reference it; archive instead. Archived stores are excluded from active store queries.
- Deleting an item soft-deletes the item but keeps its capture session until cleanup.

## Date semantics

- `ShoppingList.shoppingDate` is the trip date.
- `ListItem.targetDate` determines membership. It must equal the containing list's date.
- A deferral changes `targetDate` and moves the item to the target list in one transaction.
- Creating a list for a date with an existing active, non-deleted list returns the existing list.
- "Wait a week" resolves to a target date, then selects the nearest list on or after that date.
- Overdue items remain on their original list until the user chooses an action.
- The overdue prompt options are: move unchecked items to the next list, keep them on the past list, or mark them missed.
- All date comparisons use the device's local time zone; stored dates are ISO strings without time zones.
- Use `java.time.LocalDate` and `java.time.ZoneId` with core library desugaring enabled.

## Aisle sorting comparator

Aisle values are strings and may be numeric, alphanumeric, or descriptive. Implement one Java `Comparator<String>` used everywhere.

**Rules, in order (ascending default):**

1. Missing/blank/unresolved aisle (no digit run) sorts **last**.
2. A value has a **letter prefix** when non-digit characters precede the first digit run (e.g. `"A19"`, `"J15"`, `"Aisle 5"`). A value that starts with a digit is a **bare number** (e.g. `"29"`).
3. Letter-prefix aisles sort before bare numbers.
4. Among letter-prefix aisles: compare the normalized prefix before the digits (case-insensitive), then the numeric value, then the remaining suffix after the digits.
5. Among bare numbers: compare the numeric value, then the remaining suffix.
6. If still equal, compare the bay using the same letter-prefix-aware logic.
7. If still equal, compare `sortPosition` ascending for stability.
8. **In descending mode, reverse steps 3–7 but keep unresolved items last.**

**Default sort direction: ASCENDING.** User can toggle DESCENDING in settings; preference remembered across sessions via `SharedPreferences` `sort_direction`.

**Examples (ascending):**

- `A12` < `A100` < `EC2` < `2` < `03` < `10` < `unknown`
- `A19` < `A32` < `J15` < `J23` < `29`
- `Aisle 5` is treated as letter prefix `"aisle "` with numeric 5.
- `EC2` is letter prefix `"ec"` with numeric 2; it sorts after `A100` because `"ec"` > `"a"`.

## Sort direction

- **Default: ASCENDING** (ascending aisle numbers most intuitive for store navigation).
- User can toggle DESCENDING in settings; preference remembered across sessions.
- Remembered via `SharedPreferences` key `sort_direction` (`ASC` or `DESC`).
- Toggle available on list detail screen for power users.

## SharedPreferences keys

Use a single `SettingsPreferences` wrapper around `SharedPreferences`:

- `default_store_id`
- `last_used_store_id`
- `notifications_enabled`
- `background_organizing_enabled`
- `ai_cleanup_enabled`
- `sort_direction` (`ASC` or `DESC`)
- `last_calendar_month`
- `onboarding_completed`

Do not store list data in SharedPreferences.

## Migrations

- Start with schema version 1.
- Export the Room schema to `app/schemas/`.
- Every schema change requires a migration and a migration test.
- No destructive migrations in release builds.
- Keep migrations append-only; do not edit shipped migrations.

## Indexing

Add indexes for:

- `ListItem.listId`
- `ListItem.storeId`
- `ListItem.targetDate`
- `ListItem.lookupStatus`
- `ListItem.deletedAt`
- `ShoppingList.shoppingDate` (indexed; active-only uniqueness is enforced in a repository transaction)
- `ShoppingList.deletedAt`
- `CaptureSession.listId`
- `CaptureSession.deletedAt`
- `AisleCache.storeId + normalizedQuery`
- `PriceQuote.storeId + normalizedQuery`

## Validation rules

- Reject blank `rawText`.
- Trim leading/trailing whitespace from all user text.
- Collapse internal repeated whitespace in `displayName` and `normalizedText`.
- Limit item text to 200 characters.
- Limit list name to 80 characters.
- Normalize case with `Locale.US` for lookup text.
- Store all timestamps from a single injected `Clock`.
