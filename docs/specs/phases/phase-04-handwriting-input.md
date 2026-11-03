# Phase 4 — Handwriting Input

- **Status:** `not-started`
- **Depends on:** Phase 3
- **Owner:** unassigned
- **Started:** —
- **Completed:** —
- **Last updated:** 2026-11-03

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

- [ ] Digital Ink captures strokes and returns recognized text.
- [ ] Camera capture runs OCR and reaches review.
- [ ] Gallery import runs OCR and reaches review.
- [ ] All captures require review before saving.
- [ ] Bulk select, delete, add, merge, and split work.
- [ ] Each line can be assigned a store and date.
- [ ] Low-confidence lines are flagged.
- [ ] Camera permission denial does not block gallery or typed entry.
- [ ] Cached images are deleted after save/discard.
- [ ] No image or recognized text is logged or uploaded.
- [ ] `rawText` equals the reviewed line.
- [ ] Unit tests cover normalization and review state.
- [ ] Espresso/Robolectric tests cover the review flow and permission denial.

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

## Completion log

| Date | Commands / checks | Result | Notes |
|---|---|---|---|
| — | — | — | — |
