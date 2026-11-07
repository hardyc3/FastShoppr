# FastShoppr

FastShoppr is a local-first Android shopping-list app written in Java. Lists are
organized by store and sorted by aisle so a shopping trip can be completed in one
pass.

This repository currently contains Phases 0–2: the Java 21 Android foundation
plus the domain and persistence layer. The app has XML Views, ViewBinding, a
Material 3 theme, NavHost placeholder screens, Hilt dependency injection, a
Room schema with migrations and seed data, SharedPreferences settings, use
cases, and the quality commands later phases build on.

## Requirements

- JDK 21 (`JAVA_HOME` must point at it)
- Android SDK Platform 36 and Build-Tools 36.1.0
- Android SDK Platform-Tools and an API 30+ emulator or device for instrumentation
  tests
- Gradle is provided by the committed wrapper; do not install a separate Gradle
  version

See `docs/setup.md` for workstation setup and toolchain verification.

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

## Project structure

- `app/src/main/java/com/fastshoppr/app/` — application code
  - `data/` — Room, SharedPreferences, and future remote/repository code
  - `di/` — Hilt modules
  - `domain/` — pure Java models, repository interfaces, use cases, and `Clock`
  - `ui/` — Fragments, navigation, and ViewBinding layouts
- `app/src/main/res/` — layouts, navigation graph, themes, and strings
- `app/schemas/` — exported Room schemas
- `docs/specs/` — product, architecture, and phase specifications

Use `./gradlew spotlessApply` before committing Java changes. The project is
Java-only; do not add Kotlin app source, the Kotlin Android plugin, Compose,
coroutines, Flow, or DataStore.
