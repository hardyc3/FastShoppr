# Phase 9 Release Verification

This record accompanies the Phase 9 (MVP Release) implementation. It captures
the release configuration, audits, manual demo script, and verification
evidence required by `docs/specs/phases/phase-09-mvp-release.md`.

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

Phase 9 adds no new dependencies. Android, Hilt, Room, WorkManager, OkHttp,
Jsoup, Gson, CameraX, and ML Kit versions remain pinned in
`gradle/libs.versions.toml`. There is no analytics, advertising, crash-reporting,
cloud-database, maps, or paid-LLM SDK in the app or test configurations.

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
- No raw image, full list, address, location coordinate, or retailer page body
  is logged.
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

Manual gates in the test plan (TalkBack journey and Accessibility Scanner) are
recorded in the verification table below.

## Offline checklist

- Lists and items are read from Room and render without a network connection.
- Cached aisle results remain visible and usable after the cache is fetched.
- Lookup errors are shown inline; manual aisle entry remains available.
- A fresh retailer lookup without connectivity reports a network error rather
  than clearing cached data.

## Manual demo script

1. Install the release build on a clean device or emulator.
2. Create a list, add typed and handwritten items, review, and save.
3. Organize with Walmart and Home Depot. Accept or reject suggestions without
   changing `rawText`.
4. Confirm aisle grouping and use the sort-direction toggle.
5. Defer an item to next week and confirm it appears on the correct list/date in
   the calendar.
6. Enable TalkBack and complete a checkoff; confirm the new checked state is
   persisted after leaving and reopening the list.
7. Turn on airplane mode and confirm lists and cached aisles still work.
8. Open **Settings → Privacy → Delete all data**, confirm, and verify the app
   returns to empty lists with built-in stores still available.

## Verification results

Recorded 2026-11-17 on branch `phase9`. A command that does not run or a test
that fails, errors, or is skipped is not counted as a pass.

| Check | Command / method | Result | Evidence / notes |
|---|---|---|---|
| Debug build | `./gradlew assembleDebug` | PASS | Final full command set: BUILD SUCCESSFUL. |
| Unit tests | `./gradlew testDebugUnitTest` | PASS | 198 tests, 0 failures/errors/skipped (43 XML reports). |
| Lint | `./gradlew lintDebug` | PASS | BUILD SUCCESSFUL; 0 errors, 20 warnings (all pre-existing in untouched code; the `InkCanvasView` `performClick` warning raised earlier in this phase was resolved). ML Kit GenAI dependency metadata emits `e:` lines but does not fail the task. |
| Formatting | `./gradlew spotlessCheck` | PASS | BUILD SUCCESSFUL after `spotlessApply`. |
| Release build with R8 | `./gradlew assembleRelease` | PASS | Both unsigned fallback and env-signed builds succeed with R8 + resource shrinking. Signed APK verified with `apksigner` (throwaway key generated outside the repo, never committed). |
| Instrumentation suite | `./gradlew connectedDebugAndroidTest` | PASS | emulator-5554 (API 36), Android animation scales set to 0: 35 tests, 0 failures/errors/skipped. Also run on a connected physical Samsung SM-A536U1 (`R5CT7058L2A`, Android 16/API 36, Galaxy A53) with `ANDROID_SERIAL=R5CT7058L2A`: 35 tests, 0 failures/errors/skipped (`TEST-SM-A536U1 - 16-_app-.xml`, 54.667s). Includes `DeleteAllDataTest`, `LargeListInstrumentationTest` and the 3→4 `MigrationTest`. One earlier emulator run hit Espresso's "animations enabled" guard; disabled scales is the standard setup and the reruns passed. |
| Accessibility scan | TalkBack + Accessibility Scanner | PARTIAL | Manual-only gate. Code-level checklist complete; UI hierarchy spot-checks on both emulator and physical device confirmed content descriptions, 48dp targets, headings and localized calendar descriptions (for example `Mark milk as not checked`, `Aisle: descending`, `2026-11-17, today, no list`). On the physical device Samsung TalkBack was temporarily enabled, the first-run tutorial was dismissed, and the app launched with 0 fatal log lines; however scripted TAB/swipe traversal did not yield usable app-focused nodes, so spoken-output traversal remains unverified. Google Accessibility Scanner is not installed (only Samsung `com.samsung.android.app.vex.scanner` is present), so no Scanner report was produced. Device accessibility settings were restored to their original values. Both spoken-output and Scanner report require a device operator. |
| 100-item scroll/sort | `LargeListInstrumentationTest`, `SortItemsUseCaseTest` | PASS (automated) | 120-item list rendered and sorted in the instrumented test; scroll-to-bottom exercised. No separate Profiler timing was captured. |
| Offline journey | airplane mode demo step 7 | PARTIAL | Airplane-mode relaunch on the signed release APK passed on both the emulator and the physical device: the app relaunched with lists rendering and 0 fatal log lines. Fresh-lookup/cached-aisle behavior is covered by code and instrumented tests but was not manually re-verified on either device. |
| Release install/launch | clean emulator + physical device | PASS | Signed APK installed on a wiped API 36 emulator and on the Samsung SM-A536U1 (Android 16/API 36), then launched on both. No fatal logcat, `ClassNotFoundException` or `NoSuchMethodError` on either. Core Room/Hilt/Navigation CRUD exercised on both: create list, add store, add typed item(s), check off, toggle sort, navigate away and back (checked state and sort persisted), Settings privacy text, delete all data (empty lists, built-in stores, no onboarding), calendar with localized day descriptions. |
| Log audit | source review + logcat during demo | PASS | Source grep found no `Log.*`/`println`/`printStackTrace` in production Java. Logcat during the release demo on both emulator and physical device had 0 hits for entered item text or store names, and 0 fatal lines. |
| Full manual demo | steps 1–8 above | PARTIAL | Steps 1, 2 (typed item only), 4, 6 (checkoff without TalkBack), 7 (relaunch), 8 executed on the signed release APK on both emulator and physical device. Handwriting/photo capture, Walmart/Home Depot organize with AI suggestions, defer-to-calendar, and step 7 cached-aisle behavior were not executed in this session. |

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
- Samsung TalkBack was temporarily enabled to probe focus/labels. The first-run
  tutorial was dismissed with **Finish**. The app launched with 0 fatal lines,
  but scripted focus traversal did not expose usable app-focused nodes, so the
  spoken-output journey is still a manual gate. The device's accessibility
  settings were restored (`accessibility_enabled=0`,
  `enabled_accessibility_services` and `accessibility_script` deleted) and the
  TalkBack process was force-stopped. Airplane mode was restored off.

### Manual gates still open

These require a device operator and remain unverified; Phase 9 stays
`in-progress` until they are recorded as passes. Step-by-step instructions are
in `docs/release/phase-09-manual-tests.md`.

1. TalkBack spoken-output journey across lists → store sections → calendar →
   delete confirmation, including a checkoff and verifying it persists after
   leaving and reopening the list.
2. Accessibility Scanner report with no critical issues.
3. Full demo steps 2–5 (handwriting/photo capture, organize and AI suggestions,
   defer to next week) and step 7 cached-aisle behavior in airplane mode.
