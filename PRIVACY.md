# FastShoppr Privacy Notice

FastShoppr keeps your shopping information on your device. It does not require
an account and does not send your lists to a FastShoppr server.

## What stays on your device

- Shopping lists, item text, check-off state, and store sections
- Handwriting strokes while you are drawing, and photos used for text recognition
- Recognized item text and any aisle results you accept or cache
- Settings, schedules, and store selection
- Store-hours data and visit-time preferences

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

For Walmart, you can choose a physical store in a WebView hosted inside the app.
That retailer session's cookies and local storage stay on your device between
lookups so the chosen store context is retained. Use **Forget store** in the
store picker, or **Delete all data** in Settings, to clear them. This is the
documented exception to clearing WebView data after every lookup (D-032).

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

- Recognized text and item data remain until you delete the item or list.
- Deleted lists and items are kept for a 30-day restore window, then purged.
- Capture photos are stored in app-private cache and deleted after review is
  saved or discarded.
- Aisle cache entries expire after 60 days by default.

## Delete your data

Open **Settings → Privacy → Delete all data** and confirm. This removes every
list, item, capture, cached aisle, setting, and queued task, clears WebView
cookies and storage, and restores the built-in store list. The app returns to
empty lists with built-in stores available. Uninstalling the app removes its
app-private data; backup is disabled, so nothing is restored on reinstall.
