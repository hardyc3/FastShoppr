# Product Specification

## Vision

FastShoppr turns a messy shopping list into an ordered store route. Users capture items however they prefer, and the app finds where each item lives in the store so the trip can be completed in one pass instead of wandering aisle by aisle.

## Problem

Shoppers often keep lists as notes, photos, or handwriting. A list tells you what to buy but not where to find it. Store layouts vary, item locations change, and checking each item one at a time inside a store app is slow. The result is backtracking, missed items, and extra time in the store.

## Target users

- Primary: individual shoppers in the United States who shop at large retailers.
- Secondary (post-MVP): households that share a shopping list.
- Device assumptions: Android phone, portrait orientation, API 30+, English (US) UI and handwriting.

## Core user journey

1. Create or open a shopping list for a shopping date.
2. Add items by typing, handwriting on screen, camera photo, or gallery image.
3. Review recognized text in a bulk-edit screen.
4. Optionally let on-device AI suggest cleaner item names; the user confirms.
5. Assign each item to a store section (Walmart, Home Depot, Lowe's, Smiths, or a future custom store).
6. The app looks up each item's aisle in the background and notifies the user when the route is ready.
7. Items are grouped by store and sorted by aisle; the user can reverse the aisle direction.
8. The user checks items off while shopping.
9. Items deferred to a future date move to the appropriate future list.
10. The calendar shows upcoming shopping dates and their lists.

## MVP scope (Phases 0–9)

- Dated shopping lists with a calendar view.
- Store sections within each list.
- Typed item entry.
- Handwriting via ML Kit Digital Ink.
- Camera photo and gallery import with ML Kit text recognition.
- Bulk review and edit of recognized items.
- Optional on-device AI cleanup with deterministic fallback.
- Automatic aisle lookup for Walmart and Home Depot.
- User-picked store branch from each retailer's public WebView locator, shown as verification context.
- Manual aisle entry for every store, with caching so lookups aren't required every time.
- Background organizing with notifications.
- Aisle ascending/descending sorting within each store.
- Custom-interval recurring shopping schedule with auto-created lists.
- Natural-language item deferral such as "wait a week".
- Overdue-item prompt.
- Local-only storage; no account required.

## Post-MVP scope (Phases 10–11)

- Verified search-first automated aisle lookup and branch selection for Home
  Depot, Lowe's, and Smiths, with a Walmart regression check (Phase 10). Target
  was removed because it exposes no public aisle (D-056).
- User-added local stores.
- Aisle sign lookup from user-confirmed in-store photos (Phase 11).

## Deferred scope

- Real-time shared lists and multi-user editing.
- Google Sign-In and account sync.
- Google Maps Popular Times or busyness scraping.
- Paid cloud LLMs.
- Indoor navigation, maps, or turn-by-turn store routing.
- Manual drag reordering.
- Store-specific walking routes beyond aisle order.
- Barcode scanning.
- Receipt scanning.
- Widgets, Wear OS, tablets, or multi-platform clients.

## Feature status matrix

| Feature | MVP | Post-MVP | Deferred |
|---|---|---|---|
| Typed lists | Yes | — | — |
| Digital Ink handwriting | Yes | — | — |
| Camera/gallery OCR | Yes | — | — |
| On-device AI cleanup | Yes | — | — |
| Walmart aisle lookup | Yes | — | — |
| Home Depot aisle lookup | Yes | — | — |
| Home Depot/Lowe's/Smiths automated lookup | Manual aisle | Phase 10 search-first | — |
| Multi-store list sections | Yes | — | — |
| Aisle asc/desc toggle | Yes | — | — |
| Background organizing | Yes | — | — |
| Recurring custom interval | Yes | — | — |
| Calendar view | Yes | — | — |
| Item deferral by phrase | Yes | — | — |
| Overdue prompt | Yes | — | — |
| Custom local stores | — | Yes | — |
| Aisle sign lookup | — | Phase 11 | — |
| Shared live editing | — | — | Yes |
| Google Sign-In | — | — | Yes |
| Google Maps busyness | — | — | Yes |

## Non-goals

- FastShoppr is not a delivery or e-commerce app. It does not place orders.
- It does not guarantee that an aisle is correct; store layouts change and matches can be wrong.
- It does not replace the retailer's app for inventory or checkout.
- It does not scrape private, authenticated, or bulk catalog data.
- It does not upload list contents, images, or location to a server.

## Success criteria

MVP success means a user can:

- Capture a mixed typed/handwritten list in under five minutes.
- See the written name for every item and spot a bad match.
- Get aisle results for most Walmart and Home Depot items.
- Complete a multi-store trip from a single ordered list.
- Defer an item to next week without manually moving it.
- Use the app fully offline after lists and aisle results are cached.
- Use the app without creating an account.

Quality targets:

- No crash on permission denial, missing network, or blocked retailer page.
- A 100-item list remains scrollable and sorts in under one second on a mid-range device.
- All lookup failures produce an actionable state, not a silent drop.
- No secrets, analytics, or raw personal data leave the device.

## UX principles

- **The written name is sacred.** Always show what the user wrote. Store matches are secondary.
- **Review before saving.** OCR and AI output are suggestions until the user accepts them.
- **One list, many stores.** A trip can cover multiple stores without separate lists.
- **Dates drive lists.** The calendar is the home for upcoming trips.
- **Background work is visible.** Show progress and notify when organizing completes.
- **Fail soft.** A lookup failure never blocks checking off or manually fixing an item.
- **Local-first.** Core list behavior works with no network and no account.

## Terminology

| Term | Meaning |
|---|---|
| Shopping list | A dated trip containing items grouped by store |
| Shopping date | The date the user plans to shop |
| Target date | The date an item is needed; determines which list contains it |
| Raw text | What the user wrote or OCR recognized; never replaced by a match |
| Display name | User-facing cleaned item name; AI changes require confirmation |
| Normalized text | Lowercase, punctuation-stripped text used only for lookup |
| Store section | A group of items assigned to one store |
| Match | The retailer product found for an item |
| Aisle | Retailer-provided aisle value such as `03`, `A12`, or `EC2` |
| Bay | Optional retailer shelf/bay value |
| Unlocated | An item without a resolved aisle |
| Organizing | Background lookup plus grouping and sorting |
| Deferral | Moving an item to a future shopping date |

## Constraints

- The app is Java 21 with XML Views and ViewBinding; no Kotlin app source or Jetpack Compose.
- No backend, no account, no cloud sync in the MVP.
- Retailer access is limited to public pages, rate-limited and cached.
- Walmart lookup depends on WebView behavior and can break; manual override is always available.
- On-device AI availability depends on the device; rules are the fallback.
- Store data can be wrong or stale; the UI must make correction easy.
