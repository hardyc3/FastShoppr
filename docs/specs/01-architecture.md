# Architecture Specification

## Goals

- Keep the MVP understandable and testable for a single developer/agent workflow.
- Isolate fragile integrations (WebView, HTML parsing, ML Kit, Gemini Nano) behind interfaces.
- Make background work explicit and observable.
- Allow a future sharing backend without rewriting the domain layer.

## Technology stack

| Area | Choice |
|---|---|
| Language | Java 21 |
| UI | XML layouts, ViewBinding, Material Components, Material 3 theme |
| Build | Gradle Kotlin DSL + version catalog |
| Min / target / compile SDK | 30 / 36 / 36 |
| JDK | 21 |
| Architecture | Simple MVVM + repository |
| DI | Hilt (Java annotation processing) |
| Local DB | Room |
| Preferences | SharedPreferences |
| Async | LiveData, ExecutorService, Handler, CompletableFuture |
| Navigation | Navigation Component with Fragments |
| Background | WorkManager |
| WebView lookup | Foreground service with notification |
| Camera | CameraX |
| Handwriting | ML Kit Digital Ink Recognition (Java API) |
| Image OCR | ML Kit Text Recognition v2 (Java API) |
| On-device AI | ML Kit GenAI Prompt API Java package |
| HTTP | OkHttp |
| HTML | Jsoup |
| JSON | Gson |
| Location | Play Services Location |
| Testing | JUnit 4, Mockito, AndroidX Test, Espresso |

Pin all versions in `gradle/libs.versions.toml`. Do not use dynamic versions.

## Module strategy

Use a single `app` module for the MVP. Split into modules only when a concrete build-time or ownership problem appears. Package boundaries enforce the layers.

```text
com.fastshoppr.app
  FastShopprApplication.java
  MainActivity.java
  di/
  data/
    local/
    remote/
    repository/
    preferences/
  domain/
    model/
    repository/
    usecase/
  ui/
    theme/
    navigation/
    lists/
    listdetail/
    capture/
    lookup/
    calendar/
    settings/
    components/
  work/
```

## Layer rules

### UI

- XML layouts with ViewBinding. No Compose.
- Activities host Fragments; the main navigation graph lives in `ui/navigation/`.
- Fragments observe `LiveData<UiState>` from their ViewModel and render state.
- Fragments forward user actions to the ViewModel; no business logic in Fragments or adapters.
- Adapters use `ListAdapter`/`DiffUtil` where lists change.
- One-off effects (navigation, snackbar, permission request) use a sealed `UiEvent`/`UiEffect` type or a single-event wrapper.
- Use `viewLifecycleOwner` when observing from Fragments.
- Do not hold View or Activity references in ViewModels.

### Domain

- Java and Android imports.
- Contains models, enums, repository interfaces, and small use cases.
- Use cases are named after user actions, e.g. `AddItemUseCase`, `DeferItemUseCase`, `OrganizeListUseCase`, `CreateScheduledListUseCase`.
- A use case may combine repositories but should not know about XML, Room, or HTTP.

### Data

- Room entities, DAOs, database, migrations.
- Remote clients and parsers for stores.
- Repository implementations.
- SharedPreferences-backed settings.
- Mapping between entities and domain models.

### DI

- Hilt modules live in `di/`.
- Bind repository interfaces to implementations with `@Binds`.
- Provide clients, clocks, executors, and database with `@Provides`.
- Inject a `Clock` abstraction instead of calling `System.currentTimeMillis()` in domain code.
- Hilt supports Java classes; use `@HiltAndroidApp`, `@AndroidEntryPoint`, and `@HiltViewModel`.

## State model

Each screen defines an immutable state class and a sealed event type.

```java
public final class ExampleUiState {
    private final boolean loading;
    private final List<ItemUiModel> items;
    private final UiError error;

    public ExampleUiState(boolean loading, List<ItemUiModel> items, UiError error) {
        this.loading = loading;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.error = error;
    }

    public boolean isLoading() { return loading; }
    public List<ItemUiModel> getItems() { return items; }
    public UiError getError() { return error; }
}

public sealed interface ExampleEvent permits Navigate, ShowMessage {
    record Navigate(String route) implements ExampleEvent {}
    record ShowMessage(String message) implements ExampleEvent {}
}
```

Rules:

- State is immutable and exposes no mutable collections.
- No Android `Context` in UI state.
- Errors are represented as typed UI errors, not raw exceptions.
- Loading, empty, content, and error states are explicit.
- Do not expose database entities to the UI.
- ViewModels expose `LiveData<UiState>` and `LiveData<UiEvent>` (or a single-event wrapper).

## Navigation

- Single activity, Navigation Component with XML nav graph and Fragments.
- Destinations:
  - `lists` — list of shopping dates and upcoming trips
  - `list/{listId}` — list detail with store sections
  - `calendar` — month view of shopping dates
  - `capture/typed/{listId}` — typed item entry
  - `capture/ink/{listId}` — Digital Ink
  - `capture/camera/{listId}` — camera/gallery OCR
  - `review/{captureId}` — bulk review
  - `lookup/{listId}` — organizing progress
  - `settings` — schedule, notifications, privacy
- Pass IDs in `Bundle`s, not objects.
- Deep links are out of scope for the MVP.

## Concurrency and threading

- Use `ExecutorService` for disk and network work.
- Use `LiveData` to deliver results to the UI on the main thread.
- Room DAOs return `LiveData` or use `ExecutorService` for one-shot queries.
- Network and parsing run on a background executor.
- ML Kit `Task` callbacks may arrive on the main thread; move heavy post-processing to an executor.
- Gemini Nano calls run only while the app is in the foreground.
- WebView work runs on the main thread inside the foreground service.
- Never block the main thread.
- Inject executors for testability.

## Error model

Define a small hierarchy:

```java
public sealed interface AppError permits NetworkUnavailable, RateLimited, AccessBlocked,
        ParserFailed, LookupNotFound, AiUnavailable, UnknownError {

    record NetworkUnavailable() implements AppError {}
    record RateLimited() implements AppError {}
    record AccessBlocked() implements AppError {}
    record ParserFailed() implements AppError {}
    record LookupNotFound() implements AppError {}
    record AiUnavailable() implements AppError {}
    record UnknownError(String message) implements AppError {}
}
```

Mapping rules:

- HTTP 403, 429, or challenge -> `AccessBlocked` or `RateLimited`; stop the run and surface guidance.
- Timeout/offline -> `NetworkUnavailable`; retry later.
- DOM/JSON shape changed -> `ParserFailed`; keep item unresolved.
- No result -> `LookupNotFound`; item goes to review/manual entry.
- Gemini Nano unavailable -> `AiUnavailable`; use rules and hide AI affordances.

Never show raw stack traces to the user.

## Data access seams

Define repository interfaces in `domain/repository/`:

- `ShoppingListRepository`
- `ListItemRepository`
- `StoreRepository`
- `ScheduleRepository`
- `AisleLookupRepository`
- `AisleCacheRepository`
- `SettingsRepository`
- `CaptureRepository`
- `PriceRepository` (post-MVP)

Define integration interfaces:

- `StoreLookupService`
- `HandwritingRecognizer`
- `ImageTextRecognizer`
- `ItemNameCleaner`
- `DatePhraseParser`

Fakes live in test source sets. Production implementations are bound with Hilt.

## Background architecture

- WorkManager owns scheduling, retries, and constraints.
- Home Depot HTTP lookups run in a `Worker` or `ListenableWorker`.
- Walmart WebView lookups run through a foreground service with a persistent notification.
- A coordinator worker resolves pending items, writes results, and posts a completion notification.
- Notification permission is requested only when background organizing is enabled.
- If permission is denied, Walmart items remain queued for foreground processing.

## AI architecture

- `ItemNameCleaner` interface with `GeminiNanoItemNameCleaner`, `RuleBasedItemNameCleaner`, and `NoOpItemNameCleaner`.
- The AI implementation checks availability before each session using the Java GenAI API.
- The app never silently applies an AI suggestion.
- AI output is schema-constrained and validated before display.
- If the model is unavailable, over quota, or slow, the rules implementation is used.
- No AI call from a Worker.

## Build and quality

- `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `spotlessCheck`, and `connectedDebugAndroidTest` are the standard commands.
- Use Checkstyle/Spotless rules for Java formatting.
- Release build enables R8 and resource shrinking.
- Signing config is supplied by the environment, never committed.
- Do not add Kotlin app source, Kotlin Android plugins, or Compose dependencies. Gradle Kotlin DSL is allowed for build scripts.

## Future compatibility

- Keep `rawText`, `displayName`, and `normalizedText` separate so future sync does not corrupt user input.
- Give every list and item a stable UUID string ID so future remote IDs are possible.
- Include `updatedAt` on sync-relevant entities.
- Do not add sync fields until the sharing phase, but do not design IDs that cannot be synced.
