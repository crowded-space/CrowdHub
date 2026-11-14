# Technical Constraints

## AI Studio (Android) facts the project must respect
- Kotlin + Jetpack Compose only; single-activity, single-module; no NDK/native code.
- Client-side only: no server runtime. (Docs also mention Firebase integration for Android apps; Crowd Hub deliberately does NOT use it — guardrail B-02.)
- Exported as ZIP / runs in a browser emulator / installable via ADB. The emulator may have limited WebView/WorkManager/alarm fidelity; test critical flows on a real device when possible.

## Allowed libraries (anything else requires asking)
- AndroidX: core-ktx, lifecycle (runtime/viewmodel-compose), activity-compose, navigation-compose, work-runtime-ktx, datastore-preferences, room (runtime, ktx, compiler via KSP — use whatever the template already uses for annotation processing), webkit (`androidx.webkit`, Phase 1B+).
- Compose BOM: material3, ui, ui-tooling-preview, material-icons-core/extended (icons only).
- Kotlin: coroutines (core, android), serialization-json (`kotlinx-serialization-json`).
- Networking: OkHttp (`okhttp`). No Retrofit. No logging-interceptor in release.
- Tests (if template supports): JUnit4, kotlinx-coroutines-test.
- Charts: NONE (custom Canvas). Images: NONE needed.
- Glance (home-screen widget): Phase 3 only.

## Permissions (nothing else)
- `INTERNET`
- `POST_NOTIFICATIONS` (request in context, Android 13+)
- `SCHEDULE_EXACT_ALARM` (use `AlarmManager.canScheduleExactAlarms()`; if denied, fall back to inexact `setAndAllowWhileIdle` and show a visible notice that timers may be delayed)
- `RECEIVE_BOOT_COMPLETED` (re-schedule timers)
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC` (Watch Mode)
- Do NOT request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; instead deep-link the user to the system battery settings screen from a "Reliability" info page.

## Manifest flags
`allowBackup=false`, `usesCleartextTraffic=false`, a network security config denying cleartext, `exported=false` on all components except the launcher activity (and receivers that require it, with `permission` guards if exported), a notification-tap deep-link via `PendingIntent` with `FLAG_IMMUTABLE`.

## Background execution rules
- Periodic background work: WorkManager periodic work, minimum interval 15 minutes. Constraint: network connected.
- Exact timers: AlarmManager (see above). Timers are computed from API timestamps, not from polling frequency.
- Watch Mode: user-started foreground service, type `dataSync`, ongoing notification with a Stop action, polling at most every 30 s (matches the 30 s API cache), auto-stop after a user-chosen duration (default 4 h). Handle the system's foreground-service timeout callback gracefully (stop cleanly, post a notification "Watch Mode stopped by the system"). VERIFY Android 15 `dataSync` time limits and handle `onTimeout`.
- The app MUST work correctly if the OS kills it: all timers and watches restore from Room/DataStore on next start or boot.

## Notification channels (create at app start)
`timers` (high), `war` (high), `stocks` (default), `watch_mode` (low, ongoing), `system` (low).

## Serialization & parsing
`Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }`. All DTO fields nullable unless guaranteed by a `_samples` file AND the Torn docs. Unknown enums -> `UNKNOWN`.

## Build/test hygiene
- Keep the template's Gradle/Kotlin/AGP versions. Add only the dependencies above.
- Provide unit tests for the pure classes listed in guardrail E-06 when the template supports tests.
