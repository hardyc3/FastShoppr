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

## Gate 1 — TalkBack spoken-output journey

**Goal:** confirm a screen-reader user can complete the core journey and that a
checkoff persists.

1. Enable TalkBack (Settings → Accessibility → TalkBack, or on Samsung:
   Settings → Accessibility → Vision enhancements → TalkBack). Complete any
   first-run tutorial.
2. Launch FastShoppr. Swipe right to move focus through the launch screen and
   listen: **Create list**, **Show archived**, **Calendar**, **Settings**, and
   **Create your first list** must each be announced with a meaningful label.
3. Activate **Create list** (double-tap anywhere on the focused item). Confirm
   the date picker is announced; activate **OK**.
4. Swipe through the list detail screen: **Toggle aisle sort direction**,
   **+ Add store**, **Write by hand**, **Take photo or gallery**,
   **Organize list**, **Store hours & visit** must be announced.
5. Add a store and one typed item using TalkBack (switch to text-input mode in
   TalkBack if needed). Confirm the item text and its lookup status are spoken.
6. Focus the item checkbox and confirm it announces **Mark milk as checked**
   (or the equivalent item name). Activate it and confirm it changes to
   **Mark milk as not checked**.
7. Toggle sort and confirm the state announcement changes (for example
   **Aisle: ascending** → **Aisle: descending**).
8. Navigate back to the lists screen, reopen the same list, and confirm both
   the checked state and the sort direction are still announced correctly.
9. Open **Calendar** and confirm each day is announced with a localized
   description including the date, whether it is today, and whether it has a
   list (for example **2026-11-17, today, no list**).
10. Open **Settings → Privacy → Delete all data**, confirm the dialog text is
    read aloud, activate **Delete everything**, and confirm the empty state is
    announced after deletion.

**Pass condition:** every interactive control above is announced with a
meaningful label; the checkoff and sort persist after reopening; no focus trap
or unlabeled control is encountered. Note any spoken awkwardness or missing
label.

## Gate 2 — Accessibility Scanner report

**Goal:** no critical accessibility issues flagged by the Google Accessibility
Scanner.

1. Install **Accessibility Scanner** from the Google Play Store on the test
   device (`com.google.android.apps.accessibility.scanner`). The Samsung
   `com.samsung.android.app.vex.scanner` is a different tool and is not a
   substitute.
2. Enable the scanner via Settings → Accessibility → Accessibility Scanner.
3. Open FastShoppr on the lists screen and run a scan (scanner floating action
   button → play). Review findings.
4. Repeat on: list detail with items, the calendar screen, the Settings screen,
   and the delete-all confirmation dialog.
5. Save or export each report and attach it to the verification record.

**Pass condition:** no critical issues (missing labels, touch targets below
48dp, low-contrast text, clipped text) on any scanned screen. Non-critical
suggestions may be documented as accepted with a reason.

## Gate 3 — Capture, organize, and calendar demo

**Goal:** execute the parts of the phase demo that need real input and network.

1. Install the signed release APK on a device with network access.
2. Create a list. Add a **handwritten** item using **Write by hand** and review
   the recognized text before saving. Confirm `rawText` is the written text and
   is never replaced by a store match.
3. Add an item via **Take photo or gallery** using a photographed handwritten
   note. Review and save; confirm the recognized text is editable and the image
   is not logged or uploaded.
4. Tap **Organize list**. Allow the Walmart WebView foreground service and the
   Home Depot HTTP lookup to run. Confirm the persistent notification appears
   for the Walmart lookup.
5. When AI cleanup suggestions appear, **Accept** one and **Reject** another.
   Confirm the displayed name changes only for the accepted, confirmed
   suggestion and that `rawText` is unchanged in both cases.
6. Confirm items are grouped by store and sorted by aisle; use the sort toggle
   and verify both directions, with unresolved aisles last.
7. Defer one item to next week using the review/date control. Open **Calendar**
   and confirm the item appears on the correct date and list.
8. Disable network, re-open the list, and confirm cached aisles are still
   visible. Attempt a **fresh** lookup for a new item while offline and confirm
   it reports a network error rather than clearing cached data or crashing.

**Pass condition:** all capture paths save recognized text correctly; organize
completes with the foreground-service notification; AI suggestions require
confirmation and never overwrite `rawText`; aisle grouping/sorting and the
deferred item/calendar date are correct; cached aisles survive offline and a
fresh offline lookup fails gracefully.

---

## After running

Update `docs/release/phase-09-verification.md` and the completion log in
`docs/specs/phases/phase-09-mvp-release.md` with the actual results, including
device/emulator used and any failures. Only then check the acceptance boxes and
set Phase 9 to `done`. Do not start Phase 10 until Phase 9 is reviewed.
