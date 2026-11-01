# OCR and Input Specification

## Goals

- Let users add items with the least friction while keeping control over the final text.
- Preserve the written name exactly as captured unless the user edits it.
- Make recognition mistakes easy to spot and fix before items are saved.
- Work offline for Digital Ink and on-device text recognition after models are downloaded.

## Input modes

| Mode | Source | Technology | Network |
|---|---|---|---|
| Typed | Keyboard | Android `EditText` in XML layout | None |
| Digital Ink | On-screen writing | ML Kit Digital Ink Recognition (Java API) | Model download first, then offline |
| Camera | Photo of a handwritten list | CameraX + ML Kit Text Recognition v2 | On-device, no upload |
| Gallery | Existing image | Android photo picker + ML Kit Text Recognition v2 | On-device, no upload |

All four modes feed the same review pipeline.

## Typed entry

- Inline add field under each store section.
- Submit adds one item to that store section.
- Empty or whitespace-only input is rejected.
- A date/deferral control is available next to the input.
- Typed text becomes `rawText` exactly as entered after trimming outer whitespace.
- The user may later edit `rawText` explicitly.

## Digital Ink

- Use a custom `InkCanvasView extends View` that captures strokes with `Ink.Stroke.Builder`.
- Capture `x`, `y`, and timestamp for each point.
- Support finger and stylus input.
- Provide undo, clear, and finish buttons in the surrounding Fragment.
- Show the recognized text before saving.
- Download the English digital-ink model on first use.
- If the model is unavailable, show a clear error and keep typed entry available.
- Do not upload stroke data.
- Recognition runs on device.

### Model management

- Check model availability with ML Kit's `RemoteModelManager`.
- Download on first use with visible progress.
- Allow deletion in settings to reclaim storage.
- Do not block the rest of the app while a model downloads.
- Cache the `DigitalInkRecognizer` for the session; call `close()` when the screen is destroyed.

### Recognition context

- Set the writing area to the canvas size.
- Use natural stroke ordering; do not reorder strokes.
- Provide a pre-context hint of common shopping words only if it improves accuracy and is documented.
- Keep the raw recognized string before any cleanup.

## Camera capture

- Use CameraX preview and `ImageCapture`.
- Request camera permission only when the user opens the camera.
- If denied, show an explanation and keep gallery/typed options available.
- Capture a single still image.
- Store the image in app-private cache storage.
- Run ML Kit Text Recognition v2 on device using `InputImage.fromFilePath` or `InputImage.fromMediaImage`.
- Delete the image after review is saved or discarded.
- Provide a retake action.
- Do not log or upload the image.

## Gallery import

- Use the Android photo picker (`ActivityResultContracts.PickVisualMedia`).
- Do not request broad storage permission.
- Copy the selected image into app-private cache before processing.
- Run the same text-recognition pipeline as camera capture.
- Delete the cached copy after review.

## Recognition quality expectations

- ML Kit Text Recognition v2 is optimized for printed text.
- Handwritten camera photos are best-effort; accuracy may be poor.
- Digital Ink is the recommended path for handwriting.
- The UI must not imply perfect recognition.
- Show a short note in camera/gallery review: "Handwriting recognition is best-effort. Please review."
- If recognition returns nothing, allow the user to retake, pick another image, or switch to typed entry.

## Review pipeline

Every capture creates a `CaptureSession` and opens a review screen.

Review screen requirements:

- Editable list of recognized lines using a `RecyclerView` with an editable row layout.
- Each row has a checkbox for bulk actions.
- Actions: select all, clear selection, delete selected, add line, merge selected lines, split line, save selected.
- Each line can be assigned to a store section.
- Each line can receive a target date/deferral.
- Low-confidence lines are visually flagged.
- The user can edit any line before saving.
- The user can discard the entire capture.
- Saving creates `ListItem` rows with `rawText` equal to the reviewed line text.
- Do not auto-save without review.
- Preserve line order from the image/canvas as the initial `sortPosition`.

### Bulk editing rules

- Bulk delete removes only selected lines.
- Merge joins selected lines with a single space in visual order.
- Split uses the cursor position or an explicit delimiter.
- Duplicate lines are allowed; do not silently deduplicate.
- A line that becomes blank after editing is removed on save.

## Normalization

Store three values:

- `rawText` — the reviewed line exactly as the user accepted it.
- `displayName` — initially equal to `rawText`; may be changed by confirmed AI cleanup or explicit edit.
- `normalizedText` — derived for lookup.

Normalization for lookup:

1. Trim and collapse whitespace.
2. Lowercase with `Locale.US`.
3. Remove list markers and punctuation except alphanumerics and spaces.
4. Keep digits, units, and size tokens.
5. Do not remove brand names.
6. Do not apply stemming beyond the documented alternate-query rules.

## AI interaction

- AI cleanup is optional and user-triggered or offered after review.
- AI never changes `rawText`.
- AI suggestions appear as a proposed `displayName`.
- The user accepts, edits, or dismisses each suggestion.
- If AI is unavailable, the review flow continues without it.
- See `05-on-device-ai.md`.

## Permissions

| Permission | When requested | Denial behavior |
|---|---|---|
| Camera | User taps camera capture | Keep gallery and typed entry available |
| Notifications | User enables background organizing | Queue Walmart lookups until foreground |
| Location | User taps "use my location" | Ask for ZIP/store selection |

Never request permissions at app launch. Use `registerForActivityResult` in Fragments.

## Privacy

- Images and strokes stay on the device.
- Delete cached images after review.
- Do not log recognized text or image URIs.
- Do not send list contents to any server.
- Gemini Nano inference is on-device and foreground-only.

## Testing

- Unit tests for normalization, line splitting, merging, and review state.
- Fake recognizers for deterministic UI tests.
- Digital Ink tests use recorded stroke fixtures.
- Camera/gallery tests use bundled sample images and fake recognizers.
- Permission-denied paths are tested with Espresso-Intents or Robolectric.
- Review save tests verify `rawText` preservation and target-date assignment.

## Acceptance summary

- A user can add items by all four modes.
- Every mode goes through review.
- Bulk edit actions work.
- Recognition failures are recoverable.
- No image or text leaves the device.
- The written name is preserved.
