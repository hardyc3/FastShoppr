# Phase 4 — Handwriting Input

- **Status:** `done`
- **Depends on:** Phase 3
- **Owner:** unassigned
- **Started:** 2026-11-08
- **Completed:** 2026-11-09
- **Last updated:** 2026-11-25

## Goal

Let users add items with on-screen handwriting, camera photos, and gallery images, with a mandatory bulk-review step.

## Context

Phase 0 proved the ML Kit APIs. This phase turns them into a usable capture pipeline while preserving the written name and keeping images on the device.

## In scope

- Digital Ink canvas and recognition.
- CameraX capture and gallery import.
- ML Kit Text Recognition v2.
- Bulk review screen with editable lines.
- Select all, delete selected, add line, merge, split, save.
- Store and date assignment per line.
- Confidence flags.
- Permission handling.
- Image cleanup.
- Tests with fake recognizers and sample images.

## Out of scope

- On-device AI cleanup (Phase 5).
- Store lookup or aisle resolution.
- Background organizing.
- Sharing or authentication.

## Requirements

### Digital Ink

- `InkCanvasView extends View` captures strokes with `Ink.Stroke.Builder`.
- Undo, clear, and finish actions.
- English model download on first use with progress.
- Recognition runs on device.
- Model availability is checked before use.
- Recognizer is closed when the Fragment is destroyed.
- If the model is unavailable, typed entry remains available.

### Camera

- CameraX preview and image capture.
- Camera permission requested on demand.
- Permission denial keeps gallery and typed entry available.
- Image stored in app-private cache.
- Text Recognition v2 runs on device.
- Retake action.
- Image deleted after review save/discard.

### Gallery

- Android photo picker.
- No broad storage permission.
- Image copied to app-private cache.
- Same recognition pipeline as camera.
- Cached copy deleted after review.

### Review

- `ReviewFragment` with a `RecyclerView` of editable rows.
- Each row: checkbox, text field, confidence indicator, store selector, date control.
- Bulk actions: select all, clear, delete selected, add line, merge selected, split line.
- Save selected creates items with `rawText` equal to the reviewed line.
- Discard deletes the capture and cached image.
- Low-confidence lines are visually flagged.
- Empty recognition shows retry, gallery, and typed options.
- The written line is preserved exactly as reviewed.

## Technical approach

- Fragments: `InkCaptureFragment`, `CameraCaptureFragment`, `ReviewFragment`.
- ViewModels: `InkCaptureViewModel`, `CameraCaptureViewModel`, `ReviewViewModel`.
- Use `DigitalInkRecognition.getClient(...)`, `RemoteModelManager`, and `Ink.Builder` from Java.
- Use CameraX Java APIs and `ProcessCameraProvider`.
- Use `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`.
- Use `ActivityResultContracts` for permissions and photo picking.
- Use a fake `HandwritingRecognizer`/`ImageTextRecognizer` in tests.

## Data changes

- `CaptureSession` rows for each capture.
- `recognizedLinesJson` stores the recognition output.
- `ListItem` rows are created only after review.
- No schema change expected; if needed, add a migration.

## UI changes

- Ink canvas screen with toolbar.
- Camera preview screen with capture/retake.
- Review screen with bulk action bar.
- Strings for permission explanations and recognition limitations.
- Content descriptions for all controls.
- Dark theme support.

## Acceptance criteria

- [x] Digital Ink captures strokes and returns recognized text.
- [x] Camera capture runs OCR and reaches review.
- [x] Gallery import runs OCR and reaches review.
- [x] All captures require review before saving.
- [x] Bulk select, delete, add, merge, and split work.
- [x] Each line can be assigned a store and date.
- [x] Low-confidence lines are flagged.
- [x] Camera permission denial does not block gallery or typed entry.
- [x] Cached images are deleted after save/discard.
- [x] No image or recognized text is logged or uploaded.
- [x] `rawText` equals the reviewed line.
- [x] Unit tests cover normalization and review state.
- [x] Espresso/Robolectric tests cover the review flow and permission denial.

## Test plan

| Test | Level | Notes |
|---|---|---|
| Ink recognition | Manual + fake unit | Real device for model |
| Camera OCR | Manual + fake unit | Sample image |
| Gallery OCR | Manual + fake unit | Sample image |
| Review edit | Espresso | Add/edit/delete/merge/split |
| Permission denied | Robolectric/Espresso | Camera |
| Image cleanup | Unit | Cache file removed |
| Raw text preservation | Unit | Save mapping |

## Manual demo

1. Open a list and choose "Write by hand".
2. Write three items and finish.
3. Confirm the review screen shows editable lines.
4. Delete one line, merge two, and save.
5. Repeat with a camera photo and a gallery image.
6. Deny camera permission and confirm gallery/typed entry still work.
7. Confirm no images remain in the cache after saving.

## Risks

- Camera handwriting accuracy is poor. Mitigation: mandatory review and Digital Ink as the recommended path.
- ML Kit model download failures. Mitigation: retry, clear error, typed fallback.
- CameraX lifecycle bugs. Mitigation: bind/unbind in `onStart`/`onStop`.
- Large images causing memory pressure. Mitigation: downscale before OCR.

## Open questions

- Should the review screen support reordering lines?
- Should the app keep a thumbnail of the original image until review is saved?

## Progress notes (2026-11-08, historical verification snapshot)

At this snapshot the phase status was `in-progress` and implementation was code-complete but not yet verified. Final status is `done`; evidence is in the completion log below and in the Phase 9A emulator re-verification row.

### Done

- Gradle: pinned `buildToolsVersion "36.1.0"` (AGP picked corrupted 35.0.0 otherwise); catalog entries for `gson 2.11.0`, `camerax 1.6.2`, `exifinterface 1.4.1`, `androidx-arch-core-testing 2.2.0`; ML Kit deps switched from hardcoded strings to catalog refs. `com.google.mlkit:text-recognition:16.0.1` confirmed to BE Text Recognition v2 (Latin); there is no separate `-v2` artifact.
- Manifest: `CAMERA` permission + `uses-feature camera.any required=false`.
- Domain: `RecognizedLine`, `InkData`/`InkStroke`/`InkPoint`, `RecognitionConfidence` (ink >0.8, OCR >0.7, unknown = low), `InkRecognitionRepository`, `ImageTextRecognitionRepository`; `CaptureRepository` gained `encodeRecognizedLines`/`decodeRecognizedLines`.
- Data: `MlKitInkRecognitionRepository` (ML Kit digital-ink 19.0.0 real packages: `...digitalink.recognition.*`, `RecognitionContext` + `WritingArea(canvas)` + pre-context, `RemoteModelManager` in `com.google.mlkit.common.model`, `DownloadConditions` has NO progress listener → download progress is indeterminate only), `MlKitImageTextRecognitionRepository` (downscale to <=2048px + EXIF rotation via exifinterface), `CaptureImageStore` (app-private `cache/captures`, path-traversal-guarded delete), `CaptureRepositoryImpl` Gson codec, `di/RecognitionModule`.
- UI: `InkCanvasView` rewritten (stroke stack, timestamps, undo/clear, exports `InkData`); real `InkCaptureFragment`/`InkCaptureViewModel` (model prepare on init, states CHECKING/DOWNLOADING/READY/FAILED, finish → session → review); real `CameraCaptureFragment`/`CameraCaptureViewModel` (permission requested via explicit "Allow camera access" button — never at launch; CameraX preview bound in `onStart`, unbound in `onStop`; shutter → OCR → session; `PickVisualMedia` gallery imports copy to cache; generation counter cancels in-flight OCR on retake; pending image deleted on `onCleared` unless owned by a session); `ReviewViewModel`/`ReviewFragment` rewritten (async load on executor, parses `recognizedLinesJson`, in-place merge in visual order, split at cursor-or-first-space, per-row store selector + date control via `StorePickerDialog`/`DatePickerUtil`, defaults = first store by brand order + list date, save preserves `rawText` via `ListItem` validation, blank lines dropped on save, image deleted on save/discard); `EmptyRecognitionDialog` (retry/gallery/type); list-detail entry buttons (Write by hand / Take photo / Gallery) + `launchGallery` nav arg on `capture_camera`; `UiError`/`UiMessage`/strings/content descriptions added; bottom bars in review/ink/camera layouts changed from `Toolbar` to `LinearLayout` (Toolbar measured third child at width 0).
- Unit tests pass (`testDebugUnitTest`): `ReviewViewModelTest`, `InkCaptureViewModelTest`, `CameraCaptureViewModelTest`, `CaptureRepositoryImplTest`, `CaptureImageStoreTest`, `RecognitionConfidenceTest` (uses `InstantTaskExecutorRule`, `isReturnDefaultValues = true`).

### Environment (needed to resume)

- Build/test runs from WSL through Windows toolchain: `cmd.exe /c "gradlew.bat <task>"` (Windows JDK 21 at `D:\Software\jdk-21.0.13+9`; Linux side has only a JRE 17, no javac).
- Emulator `FastShoppr_API_36` running as `emulator-5554`; adb at `/mnt/d/Software/AndroidSDK/platform-tools/adb.exe`.
- Emulator animations are globally disabled (animator/transition/window scale = 0) — required by Espresso; note this is a device setting, not in-repo.
- If install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: `adb install -r app/build/outputs/apk/debug/app-debug.apk` manually, then rerun.
- Single instrumentation test: `cmd.exe /c "gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fastshoppr.app.CaptureFlowsTest#<method>"`.

### Resolved (2026-11-09)

- Reproduced save path with temp `TESTDBG` logs: `saveSelected` → insert → `SaveCompleted` fired and `CaptureFlowsTest` 3/3 passed on emulator. Earlier 0-row symptom was emulator-state/test interaction, not product bug; logs removed (`grep TESTDBG|android.util.Log` empty in `app/src`).
- `ManualListsFlowTest` had an IME cold-boot race (`typeText`/IME action produced partial text like `m`/`mi`/`mmilk` in one run). Test hardened to use `replaceText` + explicit add-button click (bypasses IME) and a `waitFor` after list creation. One later full-suite failure was a Material ripple animator flake on overflow click; rerun green.
- `spotlessApply` reformatted tree (CRLF from Windows toolchain); required commands rerun clean after format.
- D-027 recorded: mandatory review for every capture; `04-ocr-and-input.md` auto-save skip deferred.
- Stale Phase-3/4 placeholder strings removed/updated (`capture_ink_body`, `capture_camera_body`, `review_body` unused refs removed; `review_title` retained for nav; `capture_typed_body` rewritten to reflect current behavior).
- Manual demo (emulator `FastShoppr_API_36`, animations off): created list → ink capture → ML Kit model downloaded/ready → strokes recognized → review screen reached with low-confidence flag and bulk controls. ADB-tap IME interference prevented a full manual save tap in that run, but automated `CaptureFlowsTest` verifies save/merge/split/delete/add-line and permission-denied flows. Camera/gallery/photo-picker and image-cleanup paths covered by implementation + `CameraCaptureViewModelTest`/`CaptureImageStoreTest`/fakes.



## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| 2026-11-09 | `spotlessApply` (Windows toolchain, WSL `cmd.exe`, JDK 21) | pass | Whole-tree format normalization (CRLF). |
| 2026-11-09 | `testDebugUnitTest` | pass | 35 tasks; includes capture/review/confidence/image-store unit tests. |
| 2026-11-09 | `connectedDebugAndroidTest` (emulator `FastShoppr_API_36`, anims off) | pass | 20/20, incl. `CaptureFlowsTest` 3/3, `ManualListsFlowTest` green after IME-race hardening (`replaceText`+button). One prior suite run hit Material ripple animator flake; rerun green. |
| 2026-11-09 | `lintDebug` | pass | |
| 2026-11-09 | `assembleDebug` | pass | APK `app/build/outputs/apk/debug/app-debug.apk`. |
| 2026-11-09 | Manual demo (emulator) | partial/manual + automated | Ink model download/ready, strokes → recognition → review w/ low-confidence flag/bulk controls observed. ADB IME interference blocked manual save tap; save/merge/split/delete/add/permission/image-cleanup covered by instrumentation/unit tests. |
| 2026-11-09 | Coherence fixes + tests (review pass) | pass | `CaptureRepository.saveReviewed` makes item inserts+capture complete atomic in `runInTransaction` and computes contiguous sort positions per store; `ReviewViewModel` uses it (no partial DB state on failure), reports `DISCARD_FAILED` on discard errors, and defers discard navigation until success. Recognition repos unscoped (per-injection/VM lifetime); `ImageTextRecognitionRepository.close()` added and camera VM closes on clear. Added unit tests: atomic save transaction/order/sort positions, save failure keeps image, discard failure stays on review. Verified: `spotlessApply`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `spotlessCheck` (Windows toolchain). `connectedDebugAndroidTest` not rerun in that session; superseded by the 2026-11-25 full-suite run below. `CaptureFlowsTest` covers review save/discard flows touched by this change. |
| 2026-11-25 | Phase 9A evidence reconciliation: full instrumentation and manual capture smoke | pass | `connectedDebugAndroidTest` passed 47/47 on `emulator-5554` (0 skipped/failed), including `CaptureFlowsTest` and `TypedCaptureFlowTest`. Manual demo created two typed lines, edited one to `fresh milk`, used **Add line**, and saved both lines; Settings ink-model deletion showed progress/result. Earlier "paused mid-verification" text is historical only. |
