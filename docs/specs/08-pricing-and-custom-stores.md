# Custom Stores Specification

## Status

Custom stores are supported with manual aisle entry. User-facing custom-store
creation exists in list detail; no further custom-store phase is scheduled.
This file was formerly `08-pricing-and-custom-stores.md`; all pricing scope was
removed 2026-11-26 by D-057.

## Scope

In scope:

- User-defined local stores with no automated lookup.
- Manual aisle entry for custom stores.
- Store picker and list sections for custom stores.

Out of scope:

- Automated aisle or store-page lookup for custom stores.
- Coordinates, location permission, ZIP, or geofencing.
- Store hours or visit suggestions (removed by D-045).
- Price entry, extraction, comparison, cache, or refresh (removed by D-057).

## Data

- A custom store has `isCustom = true`.
- User fields: name, address, optional coordinates.
- Custom store data stays on the device.
- Store deletion is blocked while items reference it; archive instead.
- Soft delete keeps a 30-day restore window before purge.

## Behavior

- Custom stores appear in the store picker and as list sections.
- No automated lookup is attempted; aisle/bay entry is manual.
- Archived custom stores are excluded from active store queries.
- Restoring a soft-deleted store restores its state within 30 days.

## Tests

- Store mapper/DAO tests cover `isCustom`, archive, soft delete, and purge.
- UI tests cover custom-store creation and manual aisle entry.
