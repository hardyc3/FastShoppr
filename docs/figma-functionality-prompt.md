# FastShoppr Figma prompt — functionality only

Copy everything below into Figma.

---

Design a phone app called **FastShoppr**. Describe only user-visible functionality. Do not propose colors, visual style, typography, spacing, alignment, icons, illustrations, branding, or layout. Use plain functional copy. Use the exact strings in quotes when given.

FastShoppr turns a messy shopping list — typed, handwritten, or photographed — into one ordered trip across multiple stores. User captures items, reviews them, assigns each item to a store, finds or enters aisles, then shops from one list grouped by store and sorted by aisle.

## Global function rules

- App is local-first. No account, sign-in, backend, cloud sync, sharing, analytics, or price comparison.
- Nothing is auto-saved from capture. Every digital-ink, camera, and gallery capture must pass through review before items appear on the list.
- The written item text is sacred. Always show it prominently. AI cleanup, store matches, and aisle lookup never replace it. A user may explicitly edit it.
- A cleaned display name is secondary and starts equal to written text. AI suggestions are suggestions until the user accepts or edits them.
- A matched product name is secondary display-only information. It never replaces written text.
- One shopping list exists per shopping date. Creating a list for a date that already has one opens that existing list.
- Every item belongs to one list and has one target date. Changing the date moves the item to the list for that date.
- Every item belongs to one store. A list can contain multiple stores.
- Items group by store. Inside each store, items sort by aisle in ascending or descending order. Missing, blank, unresolved, or non-aisle values always sort last in both directions. Sort direction is remembered.
- No manual drag reordering.
- Manual aisle entry is always available for every store. Lookup never overwrites a manual aisle. User can clear a manual aisle to allow lookup again.
- Automated lookup exists for Walmart and Home Depot. Lowe's, Target, Smiths, custom-name stores, and any store without support use manual aisles only.
- One saved branch per supported brand may be chosen from that brand's public store-locator page. Saved branch name and street/city/state are shown as verification context. No ZIP, coordinates, location permission, or manual branch entry.
- Lookup results can be stale when used offline. Show a "cached" indicator for stale-but-usable results.
- No lookup failure is silent. Every blocked, failed, not-found, offline, or needs-review result must give the user an actionable next step.
- Never claim store busyness, Popular Times, real-time occupancy, or walking-route guidance.
- Notifications never show item names, list names, store addresses, or branch details.
- Every screen and state must be reachable. Where voiceover/accessibility labels are implied, label by function, not by color.

Exact status labels to use where status appears: "pending", "queued", "in progress", "found", "needs review", "not found", "error".

---

## Screen 1 — Home / Lists

Purpose: show upcoming shopping dates and lists; start a new list; open an existing list.

User sees:
- Upcoming lists with shopping date, list name, item count, and store count.
- Past lists in a separate past area or view.
- An empty state with the action "Create your first list".
- Loading and error states for list data.

Actions:
- Create a list for a chosen date.
- Open a list.
- Archive a list.
- View past lists.
- Open Calendar.
- Open Settings.

Rules:
- One active list per date.
- Creating a list for a date that already has one opens that list.
- Opening a notification for a list goes directly to that list.
- On app open, if a past list has unchecked items, show the overdue prompt described later.
- Deleting all data returns here with empty lists and built-in stores still available. No onboarding screen appears.

---

## Screen 2 — Calendar

Purpose: see shopping dates and navigate to lists.

User sees:
- A month view of shopping dates.
- Indicators on dates that have lists.
- Current date marked.
- Past dates visible and selectable.
- Upcoming lists in date order with list name, item count, and store count.
- The last viewed month remembered.

Actions:
- Move to previous or next month.
- Tap a date with a list → open that list.
- Tap a date without a list → offer "Create list for this date".
- Open an upcoming list from the secondary upcoming-list view.

Rules:
- Calendar is navigation only. It does not edit item data directly.

---

## Screen 3 — Create list / Date picker

Purpose: choose the shopping date for a new list.

User sees:
- A date chooser.
- Existing list date shown when relevant.

Actions:
- Choose a date.
- Confirm creation.
- Cancel.

Rules:
- If a list already exists for the chosen date, open that list instead of creating a duplicate.
- New auto-created lists may start with up to three suggested empty store sections based on last-used or default stores.
- List name is user-editable where naming is supported, max 80 characters.

---

## Screen 4 — List detail

Purpose: shop the list in one pass.

User sees:
- List name and shopping date.
- Store groups for stores represented on the list.
- For each store: store name, saved branch name and address when available, item count, and items sorted by aisle.
- Each item shows written text first. If different, display name is secondary. If present, matched product name is secondary.
- Each item shows aisle/bay, or "No aisle" when unresolved.
- Each item shows lookup status as text: "pending", "queued", "in progress", "found", "needs review", "not found", "error".
- Low-confidence, multiple-match, unverified-branch, and cached items are visually marked as needing review or stale.
- Unresolved and needs-review items remain visible and sort last in both asc and desc.
- Checked items stay in place and are marked checked.
- Empty state: no items yet; user can type, write, photograph, or add a store.
- Loading and error states.

Actions:
- Add a typed item under a store with a square-plus icon button.
- The sort, add-store, write-by-hand, and photo actions are icon buttons on one row with content descriptions.
- Write by hand.
- Take photo or gallery photo.
- Organize list.
- Refresh a single item.
- Refresh a whole store section.
- Toggle sort direction. Sort direction is remembered.
- Add store.
- Check/uncheck items.
- Open item editor.
- Open "Not right?" for candidates or manual aisle.
- Defer item.
- Delete item.
- Retry failed items.
- View candidate options for items marked "needs review".
- Enter or edit aisle manually at any time.

Rules:
- Item text max 200 characters.
- Blank raw text is rejected.
- Written text is never replaced by lookup, AI, or branch data.
- Manual aisle values are never replaced by in-flight lookup results.
- "Organize list" starts lookup when a branch is already saved for brands present. If no branch is saved for a brand on the list, open that brand's branch picker first, then start automatically after confirmation.
- No separate "Verify store" or "Start lookup" action on the normal path.
- During organizing, the list remains usable. New items added during a run are queued for the next run.
- Cancel keeps completed results and leaves unfinished items pending or queued.

---

## Screen 5 — Item editor

Purpose: edit one item.

User sees:
- Raw/written text.
- Display name.
- Quantity as free text, e.g. "2", "1 gal".
- Store.
- Target date.
- Aisle.
- Bay.
- Source of aisle when known, e.g. manual or cached.
- Current lookup status and sanitized error text when present.

Actions:
- Edit raw text with explicit confirmation.
- Edit display name.
- Edit quantity.
- Change store.
- Change target date.
- Enter or clear aisle/bay.
- Save.
- Cancel.

Rules:
- Raw text is required, max 200 characters.
- Changing target date moves the item to the list for that date.
- Manual aisle saves take precedence over lookup results.
- Clearing a manual aisle allows lookup to run again.
- Cancel discards all changes.

---

## Screen 6 — Store selection

Purpose: choose which store an item belongs to.

User sees:
- Five built-in brands: Walmart, Home Depot, Lowe's, Target, Smiths.
- Saved branch name and address for a brand when available.
- Last-used store preselected where applicable.

Actions:
- Choose a built-in brand.
- Confirm selection.
- Cancel.

Rules:
- Last used store is remembered.
- Default store is first active store by brand order when no last-used store exists.
- Custom-name stores can be added elsewhere for manual-aisle use but have no locator.

---

## Screen 7 — Add store

Purpose: add a store group to a list.

User sees:
- Active built-in stores.
- A custom-name entry.

Actions:
- Select a built-in store.
- Enter a custom store name.
- Add.
- Cancel.

Rules:
- Selecting a built-in store with no items may add an empty store group for quick add.
- Custom stores have no branch picker, no automated lookup, and no custom store profile.
- Only stores represented by items or intentionally added appear on the list.

---

## Screen 8 — Typed capture

Purpose: add an item by typing.

User sees:
- Text field for item text.
- Store context for the item.
- Date/deferral control.

Actions:
- Type item text.
- Add item to current list/store.
- Enter a deferral phrase.
- Submit.

Supported deferral phrases include:
- "tomorrow"
- "in 3 days"
- "next week"
- "wait a week"
- "in 2 weeks"
- "next month"
- "next shopping day"
- "this weekend"

Rules:
- Blank or whitespace-only input is rejected.
- Text becomes written text exactly as entered after trimming outer whitespace.
- Deterministic phrases resolve automatically unless the resolved date is in the past, ambiguous, or phrase removal needs approval. Then show the deferral confirmation described later.
- Relative phrases resolve against the current list date unless the phrase explicitly says "today" or "tomorrow". Show the resolved date when confirming.
- If no target list exists for the resolved date, create one.
- If the phrase is "next shopping day" and no schedule exists, ask user to choose a date or configure a schedule.
- Never silently drop the item.

---

## Screen 9 — Writing capture

Purpose: write a shopping list on screen.

User sees:
- A writing surface.
- Recognized text before saving.
- Undo.
- Clear.
- Save.
- Model status: checking, downloading, ready, or failed.
- Visible progress when the English handwriting model is downloaded on first use.

Actions:
- Write with finger or stylus.
- Undo.
- Clear.
- Save → open review.
- Switch to typed entry.

Rules:
- Writing is on-device after model preparation.
- If the model is unavailable or preparation fails, show a clear error and keep typed entry available.
- Save does not add items to the list. It opens review.
- Writing Save → review. Review "Add line" returns here with pending changes preserved.

---

## Screen 10 — Camera capture

Purpose: photograph a handwritten or printed list and recognize text.

User sees:
- Camera preview.
- Capture action.
- Retake action.
- Recognition progress.
- Permission explanation and an explicit "Allow camera access" action when needed.

Actions:
- Allow camera access.
- Capture one still image.
- Retake.
- Continue to review.
- Choose gallery instead.
- Switch to typed entry.

Rules:
- Camera permission is requested only when camera capture is opened, never at launch.
- If permission is denied, show explanation; gallery and typed entry remain available.
- Recognition is on-device. No upload.
- Recognition result always opens review.
- Image is no longer needed after review save or discard.

---

## Screen 11 — Gallery import

Purpose: import a photo from the device photo picker and recognize text.

User sees:
- System photo picker.
- Recognition progress.
- Empty recognition state.

Actions:
- Pick a photo.
- Continue to review.
- Retake/choose another image.
- Switch to typed entry.

Rules:
- Uses the system photo picker. No broad storage permission.
- Recognition is on-device.
- Same review flow as camera capture.
- If no text is recognized, offer retake, pick another image, or typed entry.

---

## Screen 12 — Bulk review

Purpose: verify and edit all recognized lines before anything is saved.

User sees:
- Editable recognized lines.
- Initial order preserved from image or canvas.
- Checkbox per line.
- Low-confidence lines flagged.
- Per-line store selector.
- Per-line date control.
- Exact note for camera/gallery: "Handwriting recognition is best-effort. Review anytime from list detail."
- Optional "Clean up names" action with suggestion count when AI cleanup is enabled.
- Loading, empty, save-failure, and discard-failure states.

Actions:
- Edit a line.
- Select lines.
- "Select all".
- "Clear selection".
- "Delete selected".
- "Add line" as a square-plus icon button with an "Add line" content description.
- "Merge selected lines".
- "Split line".
- "Save selected".
- Assign store per line.
- Assign date per line.
- Discard entire capture.
- Save all reviewed lines.
- Clean up names.

Rules:
- Written text equals the reviewed line.
- Display name starts equal to written text.
- Nothing is added to the list until Save.
- Every line requires review, including high-confidence lines. There is no auto-save or skip-review path.
- "Add line" returns to writing with all pending changes preserved.
- "Discard" saves none and returns to the list.
- "Save" saves all changes and returns to the list.
- Blank lines are removed on save.
- Duplicate lines are allowed. No silent deduplication.
- Delete removes only selected lines.
- Merge joins selected lines with a single space in visual order.
- Split uses cursor position or explicit delimiter.
- Low confidence only flags a line. It never changes whether review is required.
- Save failure keeps capture available. Discard failure stays on review.

---

## Screen 13 — AI cleanup suggestions

Purpose: suggest cleaner display names without changing written text.

User sees:
- Before/after name pairs.
- Source of each suggestion: on-device AI or automatic rules.
- Accept, edit, and dismiss actions per suggestion.
- Exact unavailable note when rules are used: "AI cleanup unavailable; using automatic cleaning."
- Loading/progress state while cleaning runs.
- Empty suggestion state.

Actions:
- Accept one suggestion.
- Edit one suggestion.
- Dismiss one suggestion.
- "Accept all" with confirmation.
- Cancel cleanup and keep original names.
- Close and save original names.

Rules:
- AI cleanup is off by default. Settings toggle is "Improve item names with on-device AI".
- "Clean up names" appears only when enabled.
- On-device AI runs only while app is in foreground.
- If the app goes to background, cleanup is cancelled and late results ignored.
- A run covers up to 5 items at a time and is cancellable.
- Unsupported, unavailable, busy, over-quota, or malformed output falls back to automatic rules.
- Automatic rules clean whitespace, casing, list markers, and known abbreviations such as "tp" → "toilet paper", "pb" → "peanut butter", "gal" → "gallon".
- Rules never change numbers or units.
- AI never invents aisles, stores, prices, or product locations.
- AI never uses a cloud model.
- AI never blocks saving.
- Dismissed suggestions are not shown again for that item in that review session.

---

## Screen 14 — Branch picker

Purpose: choose the physical branch used as verification context for a brand.

User sees:
- Brand selector.
- That brand's public store-locator page.
- Loading state.
- Blocked or unavailable state.
- Selected branch name and street/city/state before confirmation.
- Saved branch summary when one exists.

Actions:
- Choose a brand.
- Open "Pick branch".
- Navigate the store-locator page.
- Select a branch.
- Confirm branch.
- Close without saving.
- Retry from Settings when blocked.

Rules:
- One saved branch per brand.
- First use of a store opens this screen before organizing, then returns to Organize and starts automatically after confirmation.
- Settings can change a saved branch later.
- Saved branch is display/verification context only. It never rewrites written text, replaces an aisle, or claims an automated result was proven at that branch.
- Only branch name and street/city/state are saved. No ZIP, coordinates, or location permission.
- If the locator page is blocked or unavailable, show a clear message; manual aisle remains available.
- "Delete all data" clears saved branches. Next organize prompts again.
- Custom stores have no branch picker.

---

## Screen 15 — Organize progress

Purpose: show lookup progress and results.

User sees:
- List being organized.
- Saved branch names and addresses for brands on the list.
- Per-store progress, e.g. "Checking Walmart: 3 of 12 items".
- Item status text: "pending", "queued", "in progress", "found", "needs review", "not found", "error".
- Found results with aisle/bay and matched product name shown below written text.
- Needs-review items with candidate or manual-aisle next step.
- Not-found reason, e.g. "No in-store aisle shown".
- Error state with retry.
- Blocked state for CAPTCHA, sign-in wall, bot challenge, HTTP 403, or HTTP 429.
- Offline state: work stays queued; cached results may show "cached".
- Completion counts: found, needs review, not found, errors.
- Cancel action.

Actions:
- Organize list.
- Cancel.
- Retry failed.
- Open item details.
- Enter manual aisle.
- Choose "Not right?".
- Continue shopping.

Rules:
- One organizer run per list at a time.
- Walmart work shows a persistent non-sensitive progress notification: "Organizing your Walmart list".
- Completion notification shows only counts and opens the list when tapped.
- Notifications are cancelled when list is opened or work is cancelled.
- If notification permission is denied, Walmart lookups stay queued until app is foreground. Manual aisle and foreground lookup still work.
- If background service cannot start, keep items queued and show an in-app message.
- If offline, keep work queued and retry when network returns.
- If blocked or challenged, stop that run and show guidance. Do not keep retrying or bypass access controls.
- If page/app shape changed, mark affected item "error"; keep other items running.
- Cancel keeps completed results.
- Manual aisle values are never overwritten.
- Deleting an item during a run discards its result.
- Deleting the list cancels work.

---

## Screen 16 — Manual aisle dialog

Purpose: enter an aisle without lookup.

User sees:
- Item written text.
- Store.
- Aisle field.
- Bay field.
- Save and Cancel.

Actions:
- Enter aisle.
- Enter bay.
- Save manual aisle.
- Clear existing manual aisle.
- Cancel.

Rules:
- Available for every item and every store, always.
- Manual aisle never changes written text.
- Manual aisle takes precedence over cache and lookup.
- Clearing manual aisle allows lookup to replace it on a later run.

---

## Screen 17 — Candidate selection dialog

Purpose: choose among multiple possible matches.

User sees:
- Item written text.
- Multiple candidate products with any available aisle/bay.
- "Not right?" entry point.
- Manual aisle option.

Actions:
- Choose a candidate.
- Enter manual aisle.
- Confirm.
- Cancel.

Rules:
- Used for low-confidence or multiple-match results.
- Candidates are persisted so reopening does not require a new lookup.
- Choosing a candidate updates only aisle/bay/match fields, never written text.
- Candidates clear after confirmation, found/not-found/blocked/error resolution, cache hit, manual entry, or store change.

---

## Screen 18 — Deferral confirmation

Purpose: confirm date interpretation before moving an item.

User sees:
- Item text.
- Parsed date phrase.
- Resolved date.
- Target list date when different.
- Current list date when relevant.
- Ambiguous-date warning when needed.
- Past-date warning when needed.
- Phrase-removal choice when the phrase would be removed from written text.

Actions:
- Confirm target date.
- Edit/correct date.
- Confirm phrase removal.
- Keep phrase.
- Choose date manually.
- Configure schedule when "next shopping day" has no schedule.
- Cancel.

Rules:
- Explicit user-selected date wins over parsed phrase.
- Deterministic phrase wins over AI suggestion when AI is enabled.
- If AI is enabled but no better date is suggested, ask user to pick a date.
- Move item to nearest active list on or after resolved date; create a list if none exists.
- Never silently drop item.
- Removing a date phrase from written text requires confirmation.

---

## Screen 19 — Overdue prompt

Purpose: decide what happens to unchecked items on a past list.

User sees:
- Past list date.
- Count of unchecked items.
- Target next-list date.
- Exact choices:
  - "Move unchecked items to the next list"
  - "Keep on the past list"
  - "Mark as missed"
- Dismiss action.

Actions:
- Move unchecked items.
- Keep items on past list.
- Mark as missed.
- Create next list if none exists.
- Dismiss.

Rules:
- Shows once per past list per app session.
- Never auto-moves items.
- Checked items never move.
- "Mark as missed" archives the past list; unchecked items remain archived and are not carried forward.
- If no future list exists, offer to create one, preferring next scheduled date.

---

## Screen 20 — Store hours and visit window (removed)

This screen is removed by D-045. Do not design or implement store hours,
open/closed state, manual hours entry, visit windows, or preferred shopping time.
Never claim busyness or crowd level, and do not use Google Maps Popular Times or
scraped busyness data.

---

## Screen 21 — Schedule settings

Purpose: create or manage one recurring shopping schedule.

User sees:
- Enable/disable schedule.
- Interval value and unit: days or weeks, minimum 1.
- Examples: "Every 7 days", "Every 2 weeks", "Every 14 days".
- Next shopping date.
- Notify on create toggle.
- Current schedule summary.
- Conflict state when a different schedule already exists.
- Permission explanation when notifications are enabled.

Actions:
- Create schedule.
- Edit interval.
- Edit next date.
- Enable/disable.
- Turn notifications on/off.
- Disable existing schedule or move it when conflict prompt appears.
- Cancel.

Rules:
- Only one active schedule.
- Auto-create creates only the next upcoming list, never a backlog of empty lists.
- Auto-create never duplicates an existing date.
- Changing schedule does not rewrite existing lists.
- Disabling stops future auto-creation but keeps existing lists.
- Notification permission is requested when enabling schedule with notify, not at launch.
- If permission is denied, list is still created silently; no notification.

---

## Screen 22 — Settings

Purpose: manage preferences, branch context, privacy, and data.

User sees:
- Toggle: "Improve item names with on-device AI", off by default.
- Availability status text for on-device AI.
- Schedule entry.
- Sort direction preference.
- Notification preference.
- Background organizing preference.
- Brand selector and "Pick branch" action.
- Saved branch summary for each brand.
- In-app privacy notice: local-only data; no ZIP, coordinates, or location tracking.
- "Delete all data".

Actions:
- Enable/disable AI cleanup.
- Open schedule settings.
- Change sort direction.
- Change notification preference.
- Change background organizing preference.
- Pick or change branch per brand.
- Read privacy notice.
- Delete all data.

Rules:
- AI model is never downloaded without explicit user action.
- Saved branches persist until changed or deleted.
- Delete all data clears lists, items, captures, cache, preferences, and saved branches; built-in stores remain available. No onboarding appears afterward.

---

## Screen 23 — Delete all data confirmation

Purpose: confirm destructive local-data deletion.

User sees:
- Clear statement of what will be removed: lists, items, captures, cache, preferences, saved branches.
- Confirm and Cancel.

Actions:
- Confirm deletion.
- Cancel.

Rules:
- Confirmation required.
- After confirm, app returns to empty Home/Lists.
- No onboarding screen.
- Built-in stores still available.

---

## Screen 24 — Notifications and in-app status messages

Purpose: surface background work and failures without exposing list contents.

User sees:
- Persistent Walmart progress notification: "Organizing your Walmart list".
- Completion notification with counts: found, needs review, not found, errors.
- Scheduled-list created notification when enabled.
- In-app message when notification permission or foreground service start prevents Walmart lookup; items remain queued.
- Blocked/challenge message with manual-aisle guidance.
- Offline message: work queued; cached results may show "cached".
- Empty-recognition options: retake, pick another image, or type.
- Camera-denied explanation with gallery and typed fallback.
- AI fallback note: "AI cleanup unavailable; using automatic cleaning."
- Store locator blocked message with retry from Settings.

Actions:
- Tap completion notification → open list.
- Tap scheduled-list notification → open new list.
- Retry where available.
- Go to Settings.
- Use manual aisle.
- Use typed entry.

Rules:
- Notifications contain no item names, list names, store addresses, or branch details.
- Permission denial never blocks core list editing.

---

## Explicit exclusions — do not design these

- Login, account, onboarding, profile, sharing, invitations, sync, cloud storage, conflict notices, roles.
- Price, pricing, compare prices, cheapest store, coupons, loyalty prices, price history.
- Custom store profile CRUD with address/coordinates/hours. Custom-name stores for manual aisles only.
- Location permission, "use my location", ZIP entry, nearest-store ranking, manual branch entry.
- Google Maps, busyness, Popular Times, crowd level, indoor maps, turn-by-turn route.
- Barcode scanning, receipt scanning, checkout, order placement.
- Widgets, Wear OS, tablet-specific screens.
- Manual drag reorder, store-specific walking routes.
- Auto-save capture, skip-review, or high-confidence silent save.
- AI on by default, cloud LLM, automatic AI model download.
- Any visual style direction not stated here.
