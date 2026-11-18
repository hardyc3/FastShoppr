# FastShoppr

FastShoppr is a local-first Android shopping-list app written in Java. Capture a
list by typing, handwriting, or photographing a note; FastShoppr resolves items
to store aisles, groups them by store, and sorts each store section so one trip
can be completed in a single pass.

The MVP includes:

- Multiple shopping lists with one shopping date per list.
- Typed item entry, handwriting capture, and photo OCR.
- Optional on-device AI name cleanup with explicit user confirmation.
- Walmart WebView lookup and Home Depot HTTP lookup, with manual aisle entry
  always available.
- Aisle grouping, deterministic aisle sorting, and a sort-direction toggle.
- Recurring schedules, auto-created lists, deferral, overdue prompts, calendar
  view, and store-hours display.
- Offline access to lists and cached aisle results.
- Dark theme, large-font-friendly layouts, and accessibility labels.
- Settings privacy notice and a confirmed "Delete all data" action.

Sharing, sync, accounts, and price comparison are intentionally out of scope
for the MVP.

## Privacy

FastShoppr has no backend, analytics, advertising SDK, or cloud sync. Lists,
captures, aisle caches, and settings stay on the device; Android backup and
device-to-device transfer are disabled. See [PRIVACY.md](PRIVACY.md) for the
plain-language notice and the in-app text in Settings.

## Requirements

- JDK 21 (`JAVA_HOME` must point at it)
- Android SDK Platform 36 and Build-Tools 36.1.0
- Android SDK Platform-Tools and an API 30+ emulator or device for instrumentation
  tests
- Gradle is provided by the committed wrapper; do not install a separate Gradle
  version

See [docs/setup.md](docs/setup.md) for workstation setup and toolchain
verification.

## Build

Create `local.properties` with the SDK location. Do not commit it.

```properties
# Windows
sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk

# macOS / Linux
sdk.dir=/path/to/Android/Sdk
```

Then run:

```bash
./gradlew assembleDebug
```

On Windows use `gradlew.bat`:

```powershell
.\gradlew.bat assembleDebug
```

## Release build

Release builds enable R8 code shrinking and resource shrinking. Signing is
optional and supplied only through environment variables:

| Variable | Purpose |
|---|---|
| `FASTSHOPPR_KEYSTORE_PATH` | Path to the release keystore |
| `FASTSHOPPR_STORE_PASSWORD` | Keystore password |
| `FASTSHOPPR_KEY_ALIAS` | Key alias |
| `FASTSHOPPR_KEY_PASSWORD` | Key password |

When all four are present, `assembleRelease` signs with that key. When any are
absent, the build still succeeds and produces an unsigned APK for local R8
verification. No keystore, password, or `keystore.properties` file belongs in
source control.

```bash
./gradlew assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/`.

## Test and quality

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew spotlessCheck
./gradlew connectedDebugAndroidTest   # requires a running emulator/device
```

Apply Java formatting with:

```bash
./gradlew spotlessApply
```

Phase 9 verification evidence, permission/log/backup audits, and the manual
demo script are in
[docs/release/phase-09-verification.md](docs/release/phase-09-verification.md).

## Project structure

- `app/src/main/java/com/fastshoppr/app/` — application code
  - `data/` — Room, SharedPreferences, capture storage, and store lookup clients
  - `di/` — Hilt modules
  - `domain/` — pure Java models, repository interfaces, use cases, and `Clock`
  - `ui/` — Fragments, ViewModels, adapters, navigation, and ViewBinding layouts
  - `work/` — WorkManager workers, scheduler, and foreground service
- `app/src/main/res/` — layouts, navigation graph, themes, strings, and backup rules
- `app/schemas/` — exported Room schemas
- `docs/specs/` — product, architecture, and phase specifications
- `docs/release/` — release and verification records

Use `./gradlew spotlessApply` before committing Java changes. The project is
Java-only; do not add Kotlin app source, the Kotlin Android plugin, Compose,
coroutines, Flow, or DataStore.
