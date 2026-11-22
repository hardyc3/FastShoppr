# FastShoppr Privacy Notice

FastShoppr keeps your shopping information on your device. It does not require
an account and does not send your lists to a FastShoppr server.

## What stays on your device

- Shopping lists, item text, check-off state, and store sections
- Handwriting strokes while you are drawing, and photos used for text recognition
- Recognized item text, review drafts, and any aisle results you accept or cache
- Settings, schedules, and saved store branches
- Store-hours data and visit-time preferences

FastShoppr does not store your device coordinates or a ZIP code you type into a
retailer's page.

Android backup and device-to-device transfer are disabled for this app, so this
information is not copied to cloud backup or a replacement phone by the system.

## What leaves your device

FastShoppr has no backend, analytics SDK, advertising SDK, cloud sync, or paid
AI service.

When you organize a list, the app sends a search query containing item text to
the public website of the store you selected (Walmart or Home Depot) so it can
find an aisle. This is the same public page lookup a browser would perform. The
app identifies itself in the HTTP user agent, respects retailer access controls,
and stops with a clear error on a block or bot challenge.

To set a branch, open **Settings → Store branches → Pick branch**. FastShoppr
opens the selected brand's public store-locator page in an in-app WebView. You
navigate the retailer's page and choose your branch; the app saves only that
branch's name and street/city/state metadata on your device. A postal code that
appears on the retailer's page is discarded before saving or display. The app
does not fill in a ZIP, send your location, read your coordinates, or cache
locator responses.
First use of a store requires you to pick a branch. **Settings** can change or
remove it.

The branch is verification context only: it is shown on the Organize screen and
never silently changes an item's `rawText` or an aisle result. Custom stores have
no automated locator.

For Walmart lookups, retailer session cookies and local storage may stay on your
device between lookups so the chosen store context is retained. Use **Delete all
data** in Settings to clear them. This is the documented exception to clearing
WebView data after every lookup (D-032).

Optional on-device AI cleanup runs only while the app is open. It uses the
on-device model when available; if it is unavailable, FastShoppr uses local
rules. Prompts and model outputs are not stored and are never sent to a cloud
LLM.

## Permissions

FastShoppr requests only these Android permissions, at the moment they are
needed:

- **Camera** — to photograph a handwritten list.
- **Internet** — to look up aisles on public retailer pages and, if needed,
  download an on-device model.
- **Notifications** — to tell you when a scheduled list is created or an
  organized list is ready.
- **Foreground service** — to keep a user-started Walmart WebView lookup alive
  with a visible notification.

Denying a permission does not block the rest of the app; manual list entry and
manual aisle entry remain available.

## How long data is kept

- Recognized text, review drafts, and item data remain until you save or discard
  the review, or delete the item or list.
- Deleted lists, items, and capture sessions are kept for a 30-day restore
  window, then purged.
- Capture photos are stored in app-private cache and deleted after review is
  saved or discarded.
- Aisle cache entries expire after 60 days by default.

## Delete your data

Open **Settings → Privacy → Delete all data** and confirm. This removes every
list, item, capture, review draft, cached aisle, setting, saved store branch,
and queued task, clears WebView cookies and storage, and restores the built-in
store list. The app returns to empty lists with built-in stores available.
Uninstalling the app removes its app-private data; backup is disabled, so
nothing is restored on reinstall.
