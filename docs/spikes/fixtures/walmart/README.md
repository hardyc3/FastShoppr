# Walmart fixtures

Capture date: 2026-11-02
URL shape:

- `https://www.walmart.com/search?q=toilet+paper` -> `307` to `/blocked?...` -> CAPTCHA
- `https://www.walmart.com/ip/Apple-AirPods-4/11381374703` -> `307` to `/blocked?...` -> CAPTCHA
- `https://www.walmart.com/` -> `200` with embedded JSON and a request-derived store context

Store context: generic sample ZIP requested for the spike. The captured home page contained a
request-derived store ID, ZIP, latitude, and longitude; these were redacted and are not part of
the fixture. No real user data is present.

Expected extraction:

- Blocked page -> `Blocked` result; stop the run.
- Home page -> online price fields are parseable; no aisle/bay field exists in static HTML.

Missing fixture: a real WebView capture with aisle/location data. It requires an Android device
with the Phase 0 harness and a selected store. Do not infer aisle values.
