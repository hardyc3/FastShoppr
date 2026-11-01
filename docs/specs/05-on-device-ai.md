# On-Device AI Specification

## Goals

- Improve item names and OCR results without a paid cloud LLM.
- Keep all inference on the device.
- Never block the user when AI is unavailable.
- Never silently change what the user wrote.

## Technology

- ML Kit GenAI Prompt API backed by Gemini Nano and Android AICore.
- Use the Java API package: `com.google.mlkit.genai.prompt.java` and related `com.google.mlkit.genai.prompt` types.
- Runs only while FastShoppr is the foreground app.
- No API key, no per-call cost, no network requirement after model availability is established.
- If the device does not support Gemini Nano, or the model is not available, use deterministic rules.

## Allowed uses

1. **Item name cleanup** — fix obvious OCR typos, expand common abbreviations, normalize casing and spacing.
2. **OCR post-processing** — suggest corrections for recognized lines before review.
3. **Date phrase interpretation** — assist parsing phrases such as "wait a week" or "next payday" into a candidate date, subject to deterministic validation.

Not allowed:

- Choosing the cheapest store or reasoning over prices.
- Generating store aisles or inventing product locations.
- Running in the background.
- Sending data to a cloud model.
- Replacing `rawText` without user confirmation.
- Generating content unrelated to shopping-list cleanup.

## Interface

```java
public interface ItemNameCleaner {
    CompletableFuture<AiAvailability> availability();
    CompletableFuture<CleanItemResult> clean(CleanItemRequest request);
    void close();
}

public sealed interface AiAvailability permits Available, Downloadable, Downloading,
        Unavailable, QuotaExceeded {

    record Available() implements AiAvailability {}
    record Downloadable() implements AiAvailability {}
    record Downloading() implements AiAvailability {}
    record Unavailable() implements AiAvailability {}
    record QuotaExceeded() implements AiAvailability {}
}

public final class CleanItemRequest {
    private final String rawText;
    private final List<String> context;
    // constructor and getters omitted
}

public sealed interface CleanItemResult permits Suggested, NoChange, Unavailable {

    record Suggested(String displayName, Float confidence, String reason)
            implements CleanItemResult {}

    record NoChange() implements CleanItemResult {}

    record Unavailable() implements CleanItemResult {}
}
```

Implementations:

- `GeminiNanoItemNameCleaner`
- `RuleBasedItemNameCleaner`
- `NoOpItemNameCleaner` for tests or when the feature is disabled

A coordinator chooses the best available implementation and records which one produced a suggestion.

## Availability and lifecycle

- **Check availability with the Java GenAI API before creating a session.**
- Handle `DOWNLOADABLE` with an explicit user action; do not download a model silently.
- Handle `DOWNLOADING` with progress.
- Handle `UNAVAILABLE` by hiding AI actions and using deterministic rules **(default: AI on, fallback to rules)**.
- Handle `BUSY` or quota errors with exponential backoff and fallback.
- Close the model/session when leaving the screen.
- **Do not call AI from a `Worker`, service, or background thread without a foreground activity.**
- **AI runs in the foreground by default**; if the app moves to background, ongoing cleanup sessions are cancelled.
- AI is disabled only when the user turns it off in settings.

## User experience

- **AI cleanup is on by default** when the device supports Gemini Nano.
- After review, offer "Clean up names" with a count of suggestions.
- Show a before/after list.
- Each suggestion can be accepted, edited, or dismissed.
- "Accept all" is allowed but requires a confirmation dialog.
- A dismissed suggestion is not shown again for that item.
- If AI is unavailable (unsupported device, over quota, downloading), deterministic rules are used automatically and a short note appears: "AI cleanup unavailable; using automatic cleaning."
- Never block saving on AI.
- AI inference runs only while the app is in the foreground.

## Prompt design

Use a short, constrained prompt. The model returns JSON only.

System intent:

```text
You clean shopping-list item names. Preserve meaning. Fix obvious spelling errors,
expand common abbreviations, and remove list markers. Do not invent brands, sizes,
or products. Return JSON with fields: displayName, confidence, reason.
If no change is needed, return displayName equal to the input.
```

Rules:

- Limit input to 200 characters.
- Limit context to the current store section and at most five nearby items.
- Request one item at a time; do not batch more than five items per session.
- Validate the returned JSON and schema with Gson.
- Reject output that is empty, longer than 200 characters, or contains URLs.
- Reject output that changes a number or size token unless the raw text clearly contains a typo.
- If validation fails, return `NoChange` and fall back to rules.

## Deterministic fallback

The rules cleaner handles:

- Whitespace and casing cleanup.
- Removal of list markers.
- Common abbreviations: `tp` -> `toilet paper`, `pb` -> `peanut butter`, `gal` -> `gallon` only when unambiguous.
- Common OCR confusions only when the correction is a known dictionary word.
- Never changes numbers or units.

If rules and AI disagree, show the AI suggestion but keep the original visible.

## User experience

- AI cleanup is off by default until the user enables it in settings.
- After review, offer "Clean up names" with a count of suggestions.
- Show a before/after list.
- Each suggestion can be accepted, edited, or dismissed.
- "Accept all" is allowed but requires a confirmation.
- A dismissed suggestion is not shown again for that item.
- If AI is unavailable, hide the action and show a short note in settings.
- Never block saving on AI.

## Privacy

- Inference is on-device.
- Do not log prompts, raw item text, or model output.
- Do not include addresses, store names with personal context, or location in prompts.
- Store only the accepted `displayName`; do not persist raw prompts.
- Respect the ML Kit GenAI terms of service.

## Performance and quota

- Target under two seconds per item on supported devices.
- Cap a cleanup run at 20 items; process sequentially with cancellation.
- Handle per-app inference quotas gracefully.
- If the model is slow, allow the user to cancel and keep the original names.
- Do not retry quota errors more than twice.
- Execute inference through an injected `ExecutorService`; post results to the main thread via `Handler` or `LiveData`.

## Testing

- Fake `ItemNameCleaner` implementations for UI and use-case tests.
- Unit tests for prompt construction, JSON/schema validation, and fallback rules.
- Tests for unavailable, downloading, quota-exceeded, malformed-output, and timeout paths.
- No live model calls in CI.
- A manual device test is required for Phase 5 acceptance.

## Acceptance summary

- AI suggestions are optional, visible, and confirmed by the user.
- `rawText` is never changed by AI.
- The app works normally when AI is unavailable.
- No AI runs in the background.
- No cloud LLM or API key is used.
