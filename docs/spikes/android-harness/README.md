# Phase 0 Android harness (throwaway)

A Java-only Android app that exercises the device-only Phase 0 checks:

- Walmart WebView extraction (JavaScript, DOM storage, selected store).
- ML Kit Digital Ink Recognition (English model download + recognition).
- ML Kit Text Recognition v2 (camera photo or gallery image).
- ML Kit GenAI Prompt API / Gemini Nano (availability, download, prompt, quota errors).

It is not production code, has no architecture, and should be deleted before Phase 1.

## Requirements

- JDK 21.
- Android SDK Platform 36 and build-tools.
- Gradle 8.13 (the wrapper properties are included; generate the wrapper first).
- A physical Android device/emulator running API 26 or newer.
- Gemini Nano probe only: a supported physical device with AICore. Emulators are not supported.

## Build

From this directory:

```bash
gradle wrapper --gradle-version 8.13
./gradlew assembleDebug
./gradlew installDebug
```

Or open `docs/spikes/android-harness` in Android Studio and run the `app` configuration.

If Java 21 bytecode fails in D8, change `sourceCompatibility`/`targetCompatibility` in
`app/build.gradle.kts` to `VERSION_17`. Production remains Java 21.

## Manual checks

### 1. Walmart WebView

1. Open `Walmart WebView probe`.
2. Load `https://www.walmart.com/search?q=toilet+paper` (or another item).
3. If asked, complete the human check.
4. Select a store in the page (ZIP/store picker).
5. Wait for search results, then tap `Extract`.
6. Record: final URL, title, `blocked`, `aisleMatches`, `scriptMatches`, and visible text sample.
7. Repeat for at least three real items.

Evidence to record in `../phase-00-report.md`:

- Whether aisle/location data appears in DOM text, embedded JSON, or an XHR response.
- Whether a CAPTCHA, sign-in wall, 403, or 429 appeared.
- Whether the page behaved differently while the app was in the foreground.
- Screenshot or text excerpt with personal data redacted.

### 2. Digital Ink

1. Open `Digital Ink probe`.
2. Tap `Download model` and record download time. Official size is about 20 MB.
3. Write a shopping item such as `toilet paper`.
4. Tap `Recognize` and record latency, top candidate, and score.
5. Enable airplane mode and recognize again to confirm offline behavior.
6. Tap `Clear` between samples. Record accuracy limitations.

### 3. Text Recognition v2

1. Write a shopping list on paper and photograph it, or use a sample image.
2. Open `Text Recognition probe`.
3. Tap `Take photo` or `Pick image`.
4. Record latency, full text, per-line confidence, and failure cases.
5. Confirm whether the line confidence values are useful for the 70% auto-save threshold.

### 4. Gemini Nano

1. Open `Gemini Nano probe` on a supported physical device.
2. Tap `Check availability`; record `AVAILABLE`, `DOWNLOADABLE`, `DOWNLOADING`, or `UNAVAILABLE`.
3. If `DOWNLOADABLE`, tap `Download model` and record progress and completion.
4. With `toilet papr` in the input, tap `Run prompt`; record latency and raw response.
5. Run several prompts to observe quota/busy behavior. Record `GenAiException` code and retry delay.
6. Press Home while a prompt is running to verify foreground-only enforcement
   (`BACKGROUND_USE_BLOCKED`).
7. Confirm the deterministic rules fallback is always displayed.
8. Repeat on an unsupported device/emulator to record `UNAVAILABLE` behavior.

## Evidence rules

- Do not paste cookies, tokens, session IDs, addresses, coordinates, or full personal URLs into
  the report.
- Redact store names and ZIPs if they identify a real user location.
- Do not commit captured screenshots that contain personal data.
- The harness displays results only; it does not upload or persist them.
