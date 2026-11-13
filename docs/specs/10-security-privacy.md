# Security and Privacy Specification

## Principles

- Local-first: lists, images, aisle results, prices, and location stay on the device.
- No backend, no analytics SDK, no cloud sync, and no account in the MVP.
- Collect the minimum data needed for a feature.
- Request permissions at the moment of use, not at launch.
- Never log personal content.
- Never commit secrets.

## Data inventory

| Data | Where stored | Leaves device? |
|---|---|---|
| Shopping lists and items | Room | No |
| Capture images | App-private cache, deleted after review | No |
| Handwriting strokes | Memory/session only | No |
| Aisle cache | Room | No |
| Store selection | Room/SharedPreferences | No |
| Location coordinates | Room/SharedPreferences | No |
| Price quotes | Room | No |
| Preferences | SharedPreferences | No |

## Threat model

- A malicious or compromised retailer page could attempt to inject scripts or exfiltrate data through the WebView.
- A lost device could expose local list and location data.
- A parser or AI output could inject unexpected text into the UI.
- A future sharing feature could introduce access-control and conflict risks.

Mitigations:

- Disable unnecessary WebView features.
- Do not enable JavaScript interfaces that expose app objects.
- Validate all data returned from WebView and HTTP before storing.
- Escape or treat all retailer text as untrusted.
- Do not include secrets in WebView URLs.
- Keep local data in app-private storage.
- Use the Android backup rules to exclude sensitive data if appropriate.

## Permissions

| Permission | Purpose | Timing | Denial behavior |
|---|---|---|---|
| `CAMERA` | Photograph a handwritten list | When the user opens camera capture | Keep gallery and typed entry |
| `ACCESS_COARSE_LOCATION` | Find nearby stores | When the user taps "use my location" | Ask for ZIP/store selection |
| `ACCESS_FINE_LOCATION` | Optional precise nearby store | Only if coarse is insufficient and the user opts in | Fall back to coarse or manual |
| `POST_NOTIFICATIONS` | Background organizing and schedule alerts | When the user enables those features | Create lists silently; queue Walmart lookups |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | Walmart WebView lookup | Declared in manifest, started by user action | Queue for foreground |
| `INTERNET` | Store lookup (Walmart WebView; Home Depot HTTP when enabled) and ML Kit model download | When the user starts a lookup or the model downloads | Fall back to cached results and manual aisle entry |

Rules:

- Do not request location at app launch.
- Do not request notification permission until a notification feature is enabled.
- Explain why a permission is needed before requesting it.
- Handle "don't ask again" by directing the user to settings.
- Never block core list editing on a permission.

## Secrets

- No API keys, OAuth secrets, signing keys, or keystores in source control.
- Use environment variables or local Gradle properties for signing.
- Add `.gitignore` entries for keystores, `local.properties`, and build outputs.
- Do not embed retailer credentials.
- Do not add a cloud LLM key.

## Network security

- Use HTTPS only.
- Use OkHttp with certificate validation enabled.
- Do not add a global trust-all `TrustManager`.
- Do not log request bodies, cookies, or headers containing personal data.
- Set a descriptive user agent.
- Do not proxy traffic through a third party.
- Stop on block responses instead of trying to evade them.

## WebView security

- Enable JavaScript only because Walmart requires it.
- Disable file access, universal access from file URLs, and mixed content.
- Do not add a `JavascriptInterface` that exposes app data.
- Restrict navigation to the intended retailer domain.
- Clear WebView data after the run.
- Destroy the WebView when finished.
- Do not load arbitrary user-supplied URLs.

## Logging

Allowed:

- Item counts, store brand, duration, result status, error category.
- Non-identifying build and version information.

Not allowed:

- Item text, list names, images, image URIs, addresses, coordinates, cookies, or AI prompts.
- Full URLs containing personal parameters.
- Stack traces containing user data.

Use a debug-only logger that is disabled in release builds.

## Data retention

- Delete cached capture images after review.
- Delete expired aisle and price cache entries periodically.
- Keep lists until the user deletes or archives them.
- Keep manual aisle and price entries until the user edits or deletes them.
- Provide a "Delete all data" action in settings.

## Backups

- Review Android auto-backup rules.
- Exclude capture images and location if they are not needed for restore.
- Document the chosen behavior in the phase that adds backup rules.
- Do not back up secrets.

## Future sharing

- Sharing is deferred. See `11-future-sharing-sync.md`.
- When sharing is added, require authentication, access rules, and a privacy review.
- Do not add a backend before that phase.

## Release safety

- Remove debug-only shortcuts.
- Ensure no test credentials or fixture data ship in release.
- Verify the release manifest does not request unused permissions.
- Run a dependency vulnerability check where available.
- Confirm R8 rules do not strip required ML Kit/Room/Hilt classes.

## Incident checklist

If a privacy issue is found:

1. Stop the affected data flow.
2. Remove any logs or fixtures containing personal data.
3. Document the issue and fix in `12-decisions.md` or the relevant phase log.
4. Add a regression test where possible.
5. Re-run the release checks.
