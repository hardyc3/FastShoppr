# Phase 0 — Manual Verification Guide

**Who this is for:** you, on a device or emulator, verifying the Phase 0 items that cannot be settled headlessly. **Time:** ~30–45 minutes.

The automated probes run on any machine. Three things need *your* hands:

1. The Walmart "Robot or human?" page (you must press and hold the button).
2. Pickup-store selection (aisle/location only shows after choosing a store).
3. Gemini Nano (requires a supported physical phone; the emulator has no AICore).

Everything is recorded by the scratch app itself (on-screen log + files you can pull). You do not need to copy anything by hand — but do jot down what you see.

**Status after emulator run (2026-11-03):** Walmart extraction succeeded on the emulator once pages loaded (milk → aisle A29, eggs → AC4, paper towels → J29; prices visible; default geo store, not chosen). Home Depot blocked everywhere including interactive WebView (Akamai error page). Target prices visible. Nano unavailable on emulator. Your run confirms this on a real phone/network and your store.

## 0. What you need

| Item | Notes |
|---|---|
| Windows 10/11 or macOS/Linux | Steps below use Windows paths; adjust for your OS |
| JDK 21 | `C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot` (already installed on this workstation) |
| Gradle 8.10.2 | `C:\workspaces\software\gradle-8.10.2` (downloaded for this spike; AGP 8.8.2 requires Gradle ≥ 8.10.2) |
| Android SDK | `C:\Users\workylappy\AppData\Local\Android\Sdk` (Platform 36, Build-Tools 36.1.0) |
| Scratch project | `C:\workspaces\phase0-spike` (NOT in git; throwaway) |
| A phone with USB debugging, or the `FastShoppr_API_36` emulator | Gemini Nano **requires a supported physical phone** (see §7). Retailer checks work on either. |

## 1. Build and install the verification app

From PowerShell or `cmd.exe`:

```bat
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
cd /d C:\workspaces\phase0-spike
C:\workspaces\software\gradle-8.10.2\bin\gradle.bat :app:installDebug
```

(If you prefer the emulator: boot `FastShoppr_API_36` first — `emulator -avd FastShoppr_API_36` — then run the same command; it installs to the emulator.)

The app installs as **Phase0Spike**. Open it; you'll see grouped buttons: Walmart / Home Depot / Target / ML Kit / Gemini Nano, and a log pane at the bottom.

## 2. Walmart — aisle/location extraction (3 items)

For each of **milk**, **eggs**, **paper towels**:

1. Tap **Walmart: milk** (etc.). A WebView opens the search page.
2. If you see **"Robot or human?"** — press and hold the button until it passes. (This is why a human is required.) Record whether it passed or stayed blocked. On the emulator the page loaded 3 of 6 sessions; plain HTTP is always blocked.
3. Walmart assigns a **default store from your IP**. To verify *your* store: tap the store/pickup selector (top of page or on a product), search your ZIP or store, confirm — aisle info then reflects that store.
4. Tap a product, confirm the page shows **Pickup at …** with an aisle/location line.
5. Go back to the search page, tap **Extract** (top-right of the probe screen).
6. Read the result pane. Look for:
   - `isRobotWall` — must be `false` now
   - `hasNextData` — expect `true`
   - `aisleMentions` / `locationSamples` — aisle strings like `"Aisle A12"`
   - `nextDataHits` — JSON windows around `aisle`/`bay`/`storeId`
7. Record per item: item, whether the wall passed, store context set (yes/no), aisle seen (verbatim), and paste the `RAW:` line into `docs/spikes/phase-00-report.md` §1 (or attach it; it is saved to the app's external files dir as `walmart-milk.txt` etc.).

If the wall does not pass on your network/device either, record that — it confirms the Phase 0 finding and keeps the manual-entry fallback as the plan of record.

## 3. Home Depot — aisle & bay (3 items)

Two probes; run both:

**A. OkHttp (automated, expected to fail):** tap **Home Depot: OkHttp probe (expect 403)**. Record the HTTP code (403 on this network) and body length (2259 bytes). Do not attempt to bypass.

**B. WebView (user-assisted):**

1. Tap **Home Depot: milk** (and **paper towels**).
2. If you see **"Something went wrong"** (Akamai error page) — record it; that is a hard block on this network (confirmed on the emulator). Try again on your phone's **mobile data** instead of Wi-Fi; Akamai blocks often differ by network.
3. If the page loads: choose a **pickup store**, open a product, and confirm **Aisle X, Bay Y** is shown.
4. Tap **Extract**. Record: aisle, bay (verbatim), whether store selection persisted across pages.
5. Paste results into the report §2. Sanitize any captures before committing (see §9).

## 4. Target — price visibility (one other store)

1. Tap **Target: paper towels (WebView)**.
2. Select a store / pickup option. Target pages are JS-rendered; prices usually appear only after store selection.
3. Tap **Extract**. Record: `priceCount`, `priceSamples`, `sizeSamples`, `hasStoreContext`. Target does not necessarily expose aisle info — the goal is **price visibility only**.
4. Paste results into the report §7.

## 5. Digital Ink — draw your handwritten list

1. Tap **Digital Ink: draw + recognize**.
2. Handwrite a short list — one item per line — e.g. `milk`, `eggs`, `paper towels`, `batteries`. Use your normal handwriting, not print-perfect letters.
3. Tap **Recognize**. (First run downloads the `en-US` model, ~67 s on Wi‑Fi.)
4. Record: what it returned vs what you wrote; which lines were right/wrong; latency shown.
5. Clear and try again with your *worst* handwriting. Note failure modes (wrong words vs garbage).

## 6. Camera OCR — photo of a handwritten list

1. Photograph a handwritten shopping list (good light, page flat).
2. Tap **OCR v2: pick photo of handwritten list**, choose the photo.
3. Record: recognized text vs actual; latency; whether lines were split correctly.
4. (Reference data point: on an artificial cursive bitmap the model produced `nillk | eggo | paper touvels | batteries` — 1/4 exact — so temper expectations.)

## 7. Gemini Nano — supported phone required

**Requirements (all must hold):** a device on Google's supported list (e.g. Pixel 9/10/11, Galaxy S25/S26 — check the current list at <https://developers.google.com/ml-kit/genai/prompt/android>), Android 15/16, up-to-date Google Play services and **AICore**, **locked bootloader**, a Google account with model availability in your region.

On the phone:

1. Install the app (§1), open it.
2. Tap **Nano: check status**. Record the raw result:
   - `AVAILABLE (3)` — good; continue to step 4.
   - `DOWNLOADABLE (1)` — model not downloaded; continue to step 3.
   - `UNAVAILABLE (0)` — this device/account/region cannot use Nano. Record it; that's the answer for Phase 5 (rules fallback stays default).
3. Tap **Nano: download model**. Watch the log pane for progress; record time and any `GenAiException` codes (e.g. quota / over-limit / AICore too old).
4. Tap **Nano: item-cleanup prompt (timed)**. It runs: *"Return only the cleaned product name… 'milk 2% gallon organic valley'"* and shows latency + response. Record both.
   - Expected good output: something like `organic valley 2% milk` or `2% milk`.
   - Record any error code verbatim (`errorCode=` is printed on failure).
5. **Quota / foreground-only checks:** run the prompt repeatedly (10×). Note if you hit quota errors. Then press Home mid-run or switch apps and retry — record what happens (inference is designed to be foreground-only; background calls should fail, not queue).
6. Paste everything into the report §5.

If you don't have a supported phone: write "not testable — no supported device" in the report and move on. That is an accepted Phase 0 outcome; AI stays optional with the rules fallback.

## 8. What "done" looks like for Phase 0

Phase 0 is reviewable when `docs/spikes/phase-00-report.md` reflects *your* runs:

- §1 Walmart: per-item wall pass/fail, store context yes/no, aisle strings (or "blocked" documented).
- §2 Home Depot: 403 code, and WebView result (or error page on your network).
- §5 Gemini Nano: status code, download result, prompt latency/response or "not testable".
- §3/§4 ML Kit: your handwriting/photo results.
- §7 Target: price visibility answer.

Then the phase file (`docs/specs/phases/phase-00-feasibility.md`) can be updated and reviewed; unchecked acceptance criteria must remain unchecked if they fail.

## 9. Sanitize before committing anything

Never commit raw captures. Before adding any HTML/JSON/text from the app to git:

- Remove UUIDs (`xxxxxxxx-xxxx-…`), cookies (`set-cookie`), tokens, IPs, account names.
- Replace real store names/numbers with `SYNTHETIC …` if the page identifies a real store you used.
- Keep fixtures minimal (the smallest HTML/JSON that shows the structure).
- `grep -riE '[0-9a-f]{8}-[0-9a-f]{4}' docs/spikes/` must return nothing.

## 10. Results locations

The app writes to its external files dir. On emulator/device with USB debugging:

```bat
adb shell ls /storage/emulated/0/Android/data/com.fastshoppr.spike/files/phase0/
adb shell cat /storage/emulated/0/Android/data/com.fastshoppr.spike/files/phase0/walmart-milk.txt
```

(From WSL use the Windows adb: `/mnt/c/Users/workylappy/AppData/Local/Android/Sdk/platform-tools/adb.exe`, and `adb shell cat … > file` — WSL cannot write Windows paths directly.)

| File | Contents |
|---|---|
| `walmart-milk.txt`, `walmart-eggs.txt`, `walmart-paper-towels.txt`, `homedepot-milk.txt`, `homedepot-paper-towels.txt`, `target-paper-towels.txt` | Extraction JSON from the WebView probes (emulator results: Walmart 3/3 extracted; HD blocked; Target prices visible) |
| `homedepot-probe.txt` | OkHttp 403 probe output |
| `nano-status.txt`, `nano-prompt.txt` | Gemini Nano checks |
| `ocr-photo-probe.txt`, `ocr-probe.txt` | OCR outputs |
| `digital-ink-probe.txt` (from older build) | Synthetic Digital Ink run |
