# Phase 9 — Manual test instructions

These gates cannot be automated in this environment and must be run by a device
operator before Phase 9 can be marked `done`. Record the outcome for each gate
in `docs/release/phase-09-verification.md` and the phase completion log. If any
gate fails, leave Phase 9 `in-progress` and fix the issue first.

## Setup

- Use a clean device or emulator with the signed release APK installed. The
  throwaway verification key is outside the repo; for a real release, install
  the properly signed artifact.
- Turn animations off for deterministic UI behavior (Developer options →
  Window/Transition/Animator duration scale = `0x`), or use the physical device
  already configured that way.
- Keep the app's data cleared between gates unless a step says otherwise.

---

## Gate 1 — Capture, organize, and calendar demo

**Goal:** execute the parts of the phase demo that need real input and network.

1. Install the signed release APK on a device with network access.
2. Create a list. Add a **handwritten** item using **Write by hand**. On the
   writing screen tap **Save** and confirm the review/verification screen opens.
   On review tap **Add line** and confirm the app returns to writing with all
   pending changes preserved. Tap **Save** again and confirm `rawText` is the
   written text and is never replaced by a store match.
3. On review, also test **Discard**: confirm it returns to the list screen with
   none of the pending changes. Repeat **Save** and confirm it returns to the
   list screen with all changes.
4. Add an item via **Take photo or gallery** using a photographed handwritten
   note. Review and save; confirm the recognized text is editable and the image
   is not logged or uploaded.
5. Tap **Organize list**. Allow the Walmart WebView foreground service and the
   Home Depot HTTP lookup to run. Confirm the persistent notification appears
   for the Walmart lookup. On first use of a store, confirm the brand's WebView
   branch picker opens and the lookup proceeds after branch confirmation.
6. When AI cleanup suggestions appear, **Accept** one and **Reject** another.
   Confirm the displayed name changes only for the accepted, confirmed
   suggestion and that `rawText` is unchanged in both cases.
7. Confirm items are grouped by store and sorted by aisle; use the sort toggle
   and verify both directions, with unresolved aisles last.
8. Defer one item to next week using the review/date control. Open **Calendar**
   and confirm the item appears on the correct date and list.
9. Disable network, re-open the list, and confirm cached aisles are still
   visible. Attempt a **fresh** lookup for a new item while offline and confirm
   it reports a network error rather than clearing cached data or crashing.

**Pass condition:** writing **Save** opens review; review **Add line** preserves
pending changes, **Discard** saves none, and **Save** saves all; all capture paths
save recognized text correctly; the branch picker runs on first use; organize
completes with the foreground-service notification; AI suggestions require
confirmation and never overwrite `rawText`; aisle grouping/sorting and the
deferred item/calendar date are correct; cached aisles survive offline and a
fresh offline lookup fails gracefully.

## Gate 2 — Branch picker

**Goal:** pick and change one branch per brand through each brand's public
store-locator WebView and verify it appears only as store context on the Organize
screen.

1. Open **Settings → Pick branch**. Confirm the five brands are Walmart, Home
   Depot, Lowe's, Target, and Smiths, and that custom stores have no **Pick
   branch** action.
2. Select **Walmart** and tap **Pick branch**. Confirm the brand's public
   store-locator page opens in a WebView. Navigate to a branch, select it, and
   confirm it. Check the saved-branch summary.
3. Repeat with a second brand. Confirm the first use of a new store opens the
   branch picker before its organize run. Press Back from that first-use picker
   and confirm you return to Organize without the picker reopening; tapping
   Organize again reopens it.
4. Confirm the confirm dialog and saved summary show only branch name and
   street/city/state; any ZIP shown by the retailer page is not displayed.
5. In Settings, change one saved branch through **Pick branch** and confirm the
   summary updates.
6. Restart the app and confirm all saved branches persist. No ZIP, coordinate,
   location permission prompt, "Enter store manually", or "Use my location"
   control appears.
7. Create a list containing items for two of those brands, open Organize, and
   confirm both saved branches are shown as context lines. Confirm a third saved
   brand with no items on the list is not shown.
8. Confirm item text is unchanged and no aisle is invented from the saved branch.
9. Open **Settings → Privacy → Delete all data** and confirm all saved branches
   are cleared.

**Pass condition:** every supported brand can be picked through its WebView
locator page and changed in Settings; first use opens the picker; saved branches
appear only for brands on the list and never alter item text or aisles; no ZIP,
coordinates, or locator cache state exists; delete-all clears saved branches.

---

## Gate 3 — TalkBack traversal

**Goal:** TalkBack can complete the core journey and traverse lists, store
sections, the calendar, and dialogs.

1. Enable TalkBack via Settings → Accessibility → TalkBack.
2. Open FastShoppr and swipe through the lists screen. Confirm each list card is
   announced with its name and date, and open one list.
3. Swipe through the list detail screen. Confirm store section headers, item
   text, aisle values, and add-item fields are announced. Complete one checkoff.
4. Open the add-store dialog and confirm the store spinner and name field are
   announced with labels.
5. Open Settings → Pick branch. Confirm the brand selector and **Pick branch**
   button are announced with labels.
6. Open the calendar and swipe through days. Confirm date and list-state
   descriptions are announced.
7. Trigger one error message and one delete-all confirmation dialog, and confirm
   TalkBack announces the message and the dialog controls.

**Pass condition:** no unlabeled interactive control and no dead swipe stop in
the traversed screens; the core journey (open list → check off item) completes
with TalkBack on.

---

## After running

Update `docs/release/phase-09-verification.md` and the completion log in
`docs/specs/phases/phase-09-mvp-release.md` with the actual results, including
device/emulator used and any failures. Only then check the acceptance boxes and
set Phase 9 to `done`. Do not start Phase 10 until Phase 9 is reviewed.
