# Phase 9 Release Verification

This record accompanies the Phase 9 (MVP Release) implementation. It captures
the release configuration, audits, manual demo script, and verification
evidence required by `docs/specs/phases/phase-09-mvp-release.md`.

> **Rework status (2026-11-22):** the D-043/D-044/D-045 rework (generic WebView
> branch picker, handwriting **Save**/review flow, removal of the dropped
> accessibility-tool gate, and removal of store hours/visit features) is
> code-complete. D-043/D-044 instrumentation passed earlier. The D-045 static
> gate passes; `connectedDebugAndroidTest` is blocked because no device or
> emulator is attached. Live manual gates remain open. Phase 9 stays
> `in-progress`.

## Release configuration

- `applicationId`: `com.fastshoppr.app`
- `versionCode`: `1`, `versionName`: `1.0`
- Release build enables R8 (`isMinifyEnabled = true`) and resource shrinking
  (`isShrinkResources = true`).
- Signing material is never stored in the repository. `app/build.gradle.kts`
  wires a release `signingConfig` only when all four environment variables below
  are present:
  - `FASTSHOPPR_KEYSTORE_PATH`
  - `FASTSHOPPR_STORE_PASSWORD`
  - `FASTSHOPPR_KEY_ALIAS`
  - `FASTSHOPPR_KEY_PASSWORD`
- If any variable is absent, `assembleRelease` produces an unsigned APK. This
  keeps local R8 verification possible without secrets; a Play upload still
  requires a properly signed artifact.
- `app/proguard-rules.pro` keeps Room entities/DAOs/generated implementations,
  Hilt-generated classes, Gson payload models, and ML Kit reflection entry
  points. No wildcard keep rule covers the whole app.

## Permission audit

| Permission | Declared in manifest | Why it is needed | Requested at launch? |
|---|---|---|---|
| `CAMERA` | Yes | Photograph a handwritten list on the camera capture screen. | No; on camera use. |
| `INTERNET` | Yes | Public Walmart/Home Depot aisle lookups and on-device model download. | Not a runtime permission. |
| `POST_NOTIFICATIONS` | Yes | Scheduled list and organizing progress notifications. | No; when notification is first needed. |
| `FOREGROUND_SERVICE` | Yes | User-started Walmart WebView lookup service. | Not a runtime permission. |
| `FOREGROUND_SERVICE_DATA_SYNC` | Yes | Foreground service type for the WebView lookup task. | Not a runtime permission. |

No location, contacts, storage, microphone, SMS, or phone permission is
requested. `android.hardware.camera.any` is declared with
`android:required="false"`, so devices without a camera can still install and
use the rest of the app. Permission denial is handled without blocking manual
entry.

## Dependency audit

The location dependency is removed; no location permission or provider remains.
Android, Hilt, Room, WorkManager, OkHttp, Jsoup, Gson, CameraX, and ML Kit
versions remain pinned in `gradle/libs.versions.toml`. There is no analytics,
advertising, crash-reporting, cloud-database, maps, location, or paid-LLM SDK in
the app or test configurations.

## Backup and data-extraction audit

`AndroidManifest.xml` sets `android:allowBackup="false"`,
`android:fullBackupContent="false"`, and
`android:dataExtractionRules="@xml/data_extraction_rules"`. Both
`cloud-backup` and `device-transfer` rules exclude domain `root` path `.`,
so app-private data is not backed up or transferred. This matches
`PRIVACY.md` and the Settings privacy notice.

## Logging and privacy audit

- No `Log.*`, `Timber`, `println`, or `printStackTrace` calls exist in app
  production source.
- StrictMode is installed only when the `ApplicationInfo.FLAG_DEBUGGABLE` flag
  is set; it uses `penaltyLog()` for debug diagnostics only.
- WorkManager logging is `INFO` for debuggable builds and `WARN` otherwise.
- No raw image, full list, saved branch address, or retailer page body is
  logged. No ZIP or coordinates are collected.
- ML Kit text recognition, handwriting recognition, and Prompt API processing
  run on device; no cloud LLM is called.

## WebView hardening audit

- Walmart lookups require JavaScript for the public site, but file access is
  disabled, mixed content is never allowed, and no `JavascriptInterface` is
  exposed.
- Navigation is restricted to HTTPS URLs on `walmart.com`; off-domain or
  non-HTTPS navigation is refused and ends the lookup with an error.
- Each lookup WebView is destroyed on every terminal path, including caller
  cancellation. Cache, history, and form data are cleared, and delayed extraction
  and timeout callbacks are removed.
- Retailer cookies/DOM storage are intentionally retained between lookups so a
  user-picked Walmart store survives (D-032). This exception is disclosed in
  `PRIVACY.md`; **Forget store** and **Delete all data** both clear it.
- Branch picker WebViews load only the selected brand's public locator domain
  over HTTPS, expose no `JavascriptInterface`, and are destroyed on confirm or
  screen close. No locator response is parsed or cached.

## Branch picker audit

- Settings has a brand selector and a **Pick branch** button for Walmart, Home
  Depot, Lowe's, Target, and Smiths. Custom stores have no automated locator.
- Tapping **Pick branch** opens that brand's public store-locator page in a
  WebView; the user navigates, selects, and confirms a branch. First use of a
  store requires a branch pick; Settings can change it.
- Only the selected branch name and street/city/state are saved in
  SharedPreferences, one value per brand. Postal codes parsed from retailer
  pages are discarded before saving or display. No ZIP, coordinates, location
  permission, HTTP locator parsing, or locator cache.
- Saved branches are displayed on the Organize screen for brands on the list
  and are never used as proof of an aisle.
- **Delete all data** clears all saved branches.

## Secrets and debug shortcuts audit

- `.gitignore` excludes `local.properties`, `*.jks`, `*.keystore`, `*.p12`,
  `keystore.properties`, `secrets.properties`, and `api-keys.properties`.
- Release signing uses environment variables only; no fallback to the debug
  keystore.
- No hard-coded API key, password, signing credential, test account, or
  debug-only navigation shortcut exists in source.
- No `TODO`, `FIXME`, `XXX`, or `HACK` marker remains in production Java source.

## Accessibility checklist

Code-level fixes in this phase:

- 48dp minimum touch targets for calendar navigation, calendar days, sort
  toggle, capture shortcuts, "Not right", review actions, and settings actions.
- Content descriptions and action hints for list cards, store sections,
  attention headers, "Not right", review checkboxes/date/store buttons, and the
  handwriting canvas.
- `accessibilityLiveRegion="polite"` on empty-state/upcoming messages and
  shared state messages; state messages are also announced when shown.
- Heading semantics on list/store/attention section headers.
- Localized calendar day descriptions (date, today, has list/no list).
- Large-font-friendly text: removed single-line clipping from capture shortcuts
  and review buttons; text uses `sp` and wraps.
- Dark-theme-aware handwriting ink uses `colorOnSurface` before the highlight
  color.
- Cached lookup errors are labeled as cached so status is not conveyed by color
  alone.
- Brand selector and **Pick branch** control have visible labels and
  accessibility labels (`labelFor`/content description).

Manual gates in the test plan (TalkBack, capture/organize demo, and branch
picker) are recorded in the verification table below.

## Offline checklist

- Lists and items are read from Room and render without a network connection.
- Cached aisle results remain visible and usable after the cache is fetched.
- Lookup errors are shown inline; manual aisle entry remains available.
- A fresh retailer lookup without connectivity reports a network error rather
  than clearing cached data.

## Manual demo script

1. Install the release build on a clean device or emulator.
2. Create a list, add typed and handwritten items; tap **Save** on the writing
   screen, then exercise review **Add line**, **Discard**, and **Save**.
3. In **Settings → Pick branch**, pick a branch for Walmart and one other brand
   from each brand's public locator WebView and verify the summary. Change one
   branch from Settings.
4. Organize with Walmart and Home Depot. Accept or reject suggestions without
   changing `rawText`, and confirm the saved branches appear on the Organize
   screen for the brands on the list. On first use of a store, confirm the
   branch picker opens before the lookup.
5. Confirm aisle grouping and use the sort-direction toggle.
6. Defer an item to next week and confirm it appears on the correct list/date in
   the calendar.
7. Complete a checkoff; confirm the new checked state is persisted after
   leaving and reopening the list.
8. Turn on airplane mode and confirm lists and cached aisles still work.
9. Open **Settings → Privacy → Delete all data**, confirm, and verify the app
   returns to empty lists with built-in stores still available and all saved
   branches cleared.

## Verification results

Recorded 2026-11-17 and updated through 2026-11-22 on branch `phase9`. A command
that does not run or a test that fails, errors, or is skipped is not counted as a
pass. Rows before the final 2026-11-22 rework row are historical and do not
verify the current design.

| Check | Command / method | Result | Evidence / notes |
|---|---|---|---|
| Debug build | `./gradlew assembleDebug` | PASS | Final full command set: BUILD SUCCESSFUL. |
| Unit tests | `./gradlew testDebugUnitTest` | PASS | 198 tests, 0 failures/errors/skipped (43 XML reports). |
| Lint | `./gradlew lintDebug` | PASS | BUILD SUCCESSFUL; 0 errors, 20 warnings (all pre-existing in untouched code; the `InkCanvasView` `performClick` warning raised earlier in this phase was resolved). ML Kit GenAI dependency metadata emits `e:` lines but does not fail the task. |
| Formatting | `./gradlew spotlessCheck` | PASS | BUILD SUCCESSFUL after `spotlessApply`. |
| Branch-selection code gates (2026-11-18, superseded by D-043) | `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck` | PASS | Full command set BUILD SUCCESSFUL; 220 unit tests across 49 reports, 0 failures/errors/skipped; lint 0 errors, 20 pre-existing warnings; instrumentation APK compiles. Instrumentation execution and live manual gates are still pending. |
| Post-review code gates (2026-11-18) | `spotlessApply`, `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck` | PASS | BUILD SUCCESSFUL; 229 unit tests across 50 reports, 0 failures/errors/skipped; lint 0 errors, 20 pre-existing warnings. Added typed blocker/no-cache assertions, no-duplicate Organize context assertion, and corrected selected-store preference-key fake. |
| Two-axis review fixes (2026-11-22, superseded by D-043/D-044) | `spotlessApply`, `compileDebugAndroidTestJava`, `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `spotlessCheck` | PASS | All 14 Standards/Spec findings fixed. BUILD SUCCESSFUL; 241 unit tests across 51 reports, 0 failures/errors/skipped; lint 0 errors, 23 pre-existing warnings (none in changed files); both APKs assemble. Historical pre-rework result. Instrumentation not run: `connectedDebugAndroidTest` fails with `No connected devices!`. |
| D-043/D-044 rework final (2026-11-22) | `spotlessApply`, `compileDebugAndroidTestJava`, `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `spotlessCheck`; `connectedDebugAndroidTest` on Samsung SM-A536U1 (`R5CT7058L2A`, Android 16/API 36) | PASS | BUILD SUCCESSFUL. 255 unit tests across 51 reports, 0 failures/errors/skipped; lint 0 errors, 21 warnings (ML Kit dependency metadata / untouched code); both APKs assemble; Spotless passes. Full instrumentation suite passed twice after final hardening: 42 tests, 0 failures/errors/skipped each; animation scales `0.0`. Review reload is generation-guarded and mutates on the main thread; stale-load regression test added. Intermediate duplicate-row ambiguity was addressed by that hardening, and DeskClock focus interference was cleared by returning the device to Home. Live manual gates still open. Historical for D-045. |
| D-045 static final (2026-11-22) | `spotlessApply`, `compileDebugAndroidTestJava`, `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `spotlessCheck` | PASS | BUILD SUCCESSFUL. Store hours/visit, `Store.hoursJson`, and preferred shopping time removed; Room v6 + `MIGRATION_5_6` + v5→v6 migration test added. 255 unit tests across 51 reports, 0 failures/errors/skipped; both APKs assemble; Spotless passes. Lint 0 errors, 59 warnings = 21 prior warnings + 38 from untracked icon assets (unused/duplicate/XML-vs-PNG); no errors in changed code. Untracked icon filenames were changed from invalid `-` to valid `_` to unblock Android resource merge and kept unchanged otherwise. |
| D-045 instrumentation (2026-11-22) | `connectedDebugAndroidTest` on Samsung SM-A536U1 (`R5CT7058L2A`, Android 16/API 36) | PASS | 43 tests, 0 failures/errors/skipped; animation scales `0.0`. Includes the new v5→v6 migration test. Earlier attempts were blocked by a stale signed-release APK signature conflict and a v5 fixture lacking NOT NULL `isCustom`/`isDefault`/`isArchived`; device reboot and fixture correction resolved both. Live manual gates still open; phase stays `in-progress`. |
| App-store listing/attribution (2026-11-22) | `docs/specs/13-app-store-listing.md` | PASS | Listing copy drafted with MVP-only claims and the exact required `Uicons by <a href="https://www.flaticon.com/uicons">Flaticon</a>` HTML attribution. Linked from `docs/specs/README.md`. No hours/visit/price/sharing/sync/account claims. |
| Icon adoption final static (2026-11-22) | `compileDebugAndroidTestJava`, `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `spotlessCheck` | PASS | BUILD SUCCESSFUL. Supplied icons wired: `ic_launcher` launcher, `camera_24` photo, `journal_alt_24` write, `shopping_cart_add_24` add store, `sort_alt_24` sort, `square_plus_24` plain Add. List actions share one row as centered 48dp `ImageButton`s with content descriptions; obsolete `ic_sort.xml`, `add_item`, and `add_store` strings removed. 255 unit tests across 51 reports, 0 failures/errors/skipped; both APKs assemble; Spotless passes. Lint 0 errors, 71 warnings (pre-existing unused resources/SDK warnings plus supplied icon asset XML/PNG/duplicate/launcher-shape warnings); no warning in changed Java, layout, or string files. |
| Final rework gate (2026-11-22) | `compileDebugAndroidTestJava`, `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `spotlessCheck`, `connectedDebugAndroidTest` | PASS | BUILD SUCCESSFUL. 255 unit tests across 51 reports and 43 connected tests, 0 failures/errors/skipped in both suites. Lint 0 errors, 71 warnings; both APKs assemble; Spotless passes. Covers D-045 migration fix and icon adoption together. Live manual gates remain open; phase stays `in-progress`. |
| Mipmap launcher icon (2026-11-22) | `compileDebugAndroidTestJava`, `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `spotlessCheck` | PASS | Manifest uses `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`; `@drawable/ic_launcher` remains only the notification small icon. 255 unit tests across 51 reports, 0 failures/errors/skipped; both APKs assemble; Spotless passes. Lint 0 errors, 68 warnings. Connected rerun pending: device detached after static tasks completed. Phase stays `in-progress`. |
| Post-review instrumentation (2026-11-18) | `connectedDebugAndroidTest` on emulator-5554 (API 36) | PASS | 37 tests, 0 failures/errors/skipped. Includes Settings branch-selection flow and delete-all assertion for saved branches and settings. Historical pre-rework result. |
| Release build with R8 | `./gradlew assembleRelease` | PASS | Both unsigned fallback and env-signed builds succeed with R8 + resource shrinking. Signed APK verified with `apksigner` (throwaway key generated outside the repo, never committed). |
| Instrumentation suite (2026-11-17, historical) | `./gradlew connectedDebugAndroidTest` | PASS | emulator-5554 (API 36), Android animation scales set to 0: 35 tests, 0 failures/errors/skipped. Also run on a connected physical Samsung SM-A536U1 (`R5CT7058L2A`, Android 16/API 36, Galaxy A53) with `ANDROID_SERIAL=R5CT7058L2A`: 35 tests, 0 failures/errors/skipped (`TEST-SM-A536U1 - 16-_app-.xml`, 54.667s). Includes `DeleteAllDataTest`, `LargeListInstrumentationTest` and the 3→4 `MigrationTest`. One earlier emulator run hit Espresso's "animations enabled" guard; disabled scales is the standard setup and the reruns passed. |
| 100-item scroll/sort | `LargeListInstrumentationTest`, `SortItemsUseCaseTest` | PASS (automated) | 120-item list rendered and sorted in the instrumented test; scroll-to-bottom exercised. No separate Profiler timing was captured. |
| Offline journey | airplane mode demo step 7 | PARTIAL | Airplane-mode relaunch on the signed release APK passed on both the emulator and the physical device: the app relaunched with lists rendering and 0 fatal log lines. Fresh-lookup/cached-aisle behavior is covered by code and instrumented tests but was not manually re-verified on either device. |
| Release install/launch (2026-11-17, historical) | clean emulator + physical device | PASS | Signed APK installed on a wiped API 36 emulator and on the Samsung SM-A536U1 (Android 16/API 36), then launched on both. No fatal logcat, `ClassNotFoundException` or `NoSuchMethodError` on either. Core Room/Hilt/Navigation CRUD exercised on both: create list, add store, add typed item(s), check off, toggle sort, navigate away and back (checked state and sort persisted), Settings privacy text, delete all data (empty lists, built-in stores, no onboarding), calendar with localized day descriptions. |
| Log audit | source review + logcat during demo | PASS | Source grep found no `Log.*`/`println`/`printStackTrace` in production Java. Logcat during the release demo on both emulator and physical device had 0 hits for entered item text or store names, and 0 fatal lines. |
| Full manual demo | steps 1–9 above | PARTIAL | Steps 1, 2 (typed item only), 4, 6, 7 (relaunch), 8 executed on the signed release APK on both emulator and physical device. Handwriting/photo capture, Walmart/Home Depot organize with AI suggestions, defer-to-calendar, and step 8 cached-aisle behavior were not executed in this session. |

### Connected physical device run (2026-11-17)

A physical Samsung Galaxy A53 (`SM_A536U1`, serial `R5CT7058L2A`, Android 16 /
API 36) was connected for this verification pass. Animation scales were already
`0.0`. The following were executed on it:

- `ANDROID_SERIAL=R5CT7058L2A ./gradlew connectedDebugAndroidTest --no-daemon`
  → 35 tests, 0 failures/errors/skipped, 0 skipped (`TEST-SM-A536U1 - 16-_app-.xml`).
- Signed release APK installed and launched; `topResumedActivity` was
  `com.fastshoppr.app/.MainActivity`; 0 fatal logcat lines and no
  `ClassNotFoundException`/`NoSuchMethodError`.
- UI smoke on the release APK: create list (date picker), add store, add typed
  items, check off an item (`Mark milk as checked` → `Mark milk as not checked`),
  toggle sort (`Aisle: ascending` → `Aisle: descending`), navigate away and back
  with checkoff and sort persisted, Settings privacy text present, delete all
  data (empty lists, built-in stores, no onboarding), calendar month and
  localized day description (`2026-11-17, today, no list`), airplane-mode
  relaunch with 0 fatal lines.
- Device accessibility tooling was temporarily enabled to probe labels/focus.
  The first-run tutorial was dismissed and the app launched with 0 fatal lines.
  Device accessibility settings were restored
  (`accessibility_enabled=0`, `enabled_accessibility_services` and
  `accessibility_script` deleted), the probe process was force-stopped, and
  airplane mode was restored off.

### Manual gates still open

These require a device operator and remain unverified; Phase 9 stays
`in-progress` until they are recorded as passes. Step-by-step instructions are
in `docs/release/phase-09-manual-tests.md`.

1. Full demo in `docs/release/phase-09-manual-tests.md` Gate 1 (handwriting
   **Save**/review flow, photo capture, branch picker on first use, organize and
   AI suggestions, defer to next week, cached-aisle behavior).
2. `docs/release/phase-09-manual-tests.md` Gate 2 (WebView branch picker,
   first-use prompt, Settings change, Organize context, delete-all clearing).
3. `docs/release/phase-09-manual-tests.md` Gate 3 (TalkBack traversal).
4. Confirm no **Store hours & visit** action, no visit-time preference in
   Settings/Schedule, and no open/closed or hours text appears on a list.


## User feedback (verbatim, 2026-11-22)

Ignored, we don't care about accessibility scanner

**Resolution (D-044):** The tool-based gate is dropped from tests, verification, phase criteria, and specs. TalkBack and other accessibility checks remain.

### Handwriting feedback

The UI for hadnwritting isn't intuitive.
I want to write
replace finish with save, save takes you to the verification screen
verification screen
  add line should go back to the writing screen
  discard should go back to the list screen with none of the changes
  save should go back to the list screen with all of the changes

everything else looks good and works as expected

**Resolution:** Writing screen action is **Save** and opens review. Review **Add line** returns to writing with pending changes, **Discard** saves none, and **Save** saves all. Updated in `04-ocr-and-input.md`, `09-testing-quality.md`, Phase 9, and manual tests.

### Branch-picker feedback
I'm not sure how this code is supposed to work but it doesn't work at all. It's not finding any stores.
I think what we need to do is open the store locator page for any given store in a web view and have the user select it, this should be something they have to do when they use a particular store the first time. In settings they should be able to change it, so we don't need to store a zip code, rather have a selector for the store branch and the find nearby stores button opens that brand's store locator where they can pick the store. get rid of the enter store manually button and the use my location button. change the text on the find nearby stores button to pick branch.

**Resolution (D-043):** Settings has a brand selector and **Pick branch**. Tapping it opens that brand's public store-locator page in a WebView for selection/confirmation. One branch per brand is saved (name/address only); first use of a store requires a pick, Settings can change it. No ZIP, manual entry, location button/permission, or HTTP locator parsing/cache. Custom stores have no automated locator. Automated verification passed; live manual gates remain.

### Store-hours removal

We also want to remove the store hours and visit features,

**Resolution (D-045):** Removed the **Store hours & visit** action, manual hours entry, open/closed display, visit-window suggestions, `Store.hoursJson`, and the `preferred_shopping_time` preference. Room schema v6 drops `stores.hoursJson` via `MIGRATION_5_6`. Specs, privacy text, and the design prompt are updated.
