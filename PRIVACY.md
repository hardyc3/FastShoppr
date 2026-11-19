# FastShoppr Privacy Notice

FastShoppr keeps your shopping information on your device. It does not require
an account and does not send your lists to a FastShoppr server.

## What stays on your device

- Shopping lists, item text, check-off state, and store sections
- Handwriting strokes while you are drawing, and photos used for text recognition
- Recognized item text and any aisle results you accept or cache
- Settings, schedules, ZIP codes, and saved store branches
- Store-hours data and visit-time preferences

FastShoppr does not store your device coordinates.

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

If you use **Settings → Nearest store**, the app sends either the ZIP code you
entered or an approximate location to the public store locator of the brand you
selected, so it can suggest nearby branches. The app keeps the branch you pick
on your device and shows it on the Organize screen for verification; it does not
store your coordinates or use the saved branch to silently change an aisle. You
can always enter a store manually instead. Store-locator results are cached on
your device for 60 days.

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
- **Internet** — to look up aisles and nearby branches on public retailer pages
  and, if needed, download an on-device model.
- **Location (approximate)** — only when you tap **Use my location** in
  **Settings → Nearest store**. FastShoppr does not request it at launch.
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
list, item, capture, cached aisle, setting, saved ZIP, saved store branch,
store-locator cache, and queued task, clears WebView cookies and storage, and
restores the built-in store list. The app returns to
empty lists with built-in stores available. Uninstalling the app removes its
app-private data; backup is disabled, so nothing is restored on reinstall.
