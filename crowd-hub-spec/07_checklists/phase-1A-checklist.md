# Phase 1A Checklist (GENERATED — do not edit; edit the feature files and run tools/regenerate.py)

For each line the agent reports: `ID | PASS/FAIL/NOT DONE/NA | evidence: file#symbol | note`.
The user verifies a sample of PASS items by reading the cited code and running `07_checklists/qa-manual-test-plan.md`.

## Feature acceptance criteria

### F00 — Settings, Privacy, About, Developer tools
- [ ] F00-AC-01 Settings screen lists Notifications, Watch Mode, Data, Reliability, Advanced; Developer appears only after 7 taps on the version row.
- [ ] F00-AC-02 Privacy screen shows the Torn API disclosure block exactly as specified.
- [ ] F00-AC-03 About screen states the app is unofficial/not affiliated with Torn and shows the version.
- [ ] F00-AC-04 API Probe runs via the global RateLimiter (priority USER) and the output never contains the API key.
- [ ] F00-AC-05 API Probe shows clock offset and the rate-limiter usage counters.
- [ ] F00-AC-06 All preferences persist across app restarts and contain no secrets.

### F01 — Profile Container & Secret Vault
- [ ] F01-AC-01 First launch shows onboarding steps 1-6 in order; completing them lands on Dashboard.
- [ ] F01-AC-02 The API key is validated through `user/basic` and stored only in SecretVault (verify no plaintext copy in Room/DataStore/logs).
- [ ] F01-AC-03 Insufficient access level (code 16) blocks completion with a clear message.
- [ ] F01-AC-04 Key is masked in all UI; `FLAG_SECURE` is set on key entry/account screens.
- [ ] F01-AC-05 Profiles UI is reachable only via Settings > Advanced; create/rename/delete/switch work; deleting wipes secrets, rows, alarms.
- [ ] F01-AC-06 Replace key validates before swapping; different player id triggers a warning and a reset confirmation.
- [ ] F01-AC-07 INVALID/PAUSED account status shows a persistent banner and stops API calls for that account.
- [ ] F01-AC-08 Killing the app during onboarding resumes correctly; no account row without a secret.
- [ ] F01-AC-09 Keystore failure shows an error and never falls back to plaintext.

### F02 — API Manager (client, limiter, cache, clock)
- [ ] F02-AC-01 Every Torn API call goes through RateLimiter (grep shows no direct OkHttp call to `api.torn.com` outside the API layer).
- [ ] F02-AC-02 Limits implemented as specified (85 global, 20 reserved, class caps 65/45/15) and unit tested.
- [ ] F02-AC-03 Identical requests within 30 s are served from cache; concurrent identical requests are single-flighted (unit tested).
- [ ] F02-AC-04 InvalidKey/Paused errors mark the account, stop further calls, and post one system notification; no retry loop (unit tested).
- [ ] F02-AC-05 Code 8 triggers a 5-minute global pause; code 5 triggers a 60 s backoff.
- [ ] F02-AC-06 The API key never appears in logs, exception messages or UI (verify `CrowLog` redaction and the interceptor).
- [ ] F02-AC-07 `Clock` uses the median offset from `Date` headers and is unit tested.
- [ ] F02-AC-08 DTOs are nullable/lenient and compile against the `_samples` files that exist; missing samples are reported as VERIFY.
- [ ] F02-AC-09 TornProvider implements the Provider interface exactly as defined (no extra abstractions).

### F03 — Alert Engine (timers, polling, Watch Mode)
- [ ] F03-AC-01 Channels `timers`, `war`, `stocks`, `watch_mode`, `system` exist with the specified importance.
- [ ] F03-AC-02 Energy/nerve/happy/life full, cooldown ready, hospital/jail release and own landing timers are scheduled from API timestamps via `Clock` and fire at the right time.
- [ ] F03-AC-03 Exact alarms are used when permitted; otherwise a visible fallback notice exists.
- [ ] F03-AC-04 Timers survive app kill and device reboot (BootReceiver re-schedules from the DB).
- [ ] F03-AC-05 Changed timestamps (> 10 s) re-schedule; vanished conditions cancel the timer (unit tested via `AlertDeduper`).
- [ ] F03-AC-06 `PollWorker` runs every 15 min at BACKGROUND priority and never exceeds its limiter class cap.
- [ ] F03-AC-07 Watch Mode runs as a `dataSync` foreground service with a Stop action, 30 s polling, auto-stop duration, and graceful OS-timeout handling.
- [ ] F03-AC-08 The same `dedupeKey` never fires twice for the same fire time (unit tested).
- [ ] F03-AC-09 Notification taps deep-link to the right screen with `FLAG_IMMUTABLE` PendingIntents.
- [ ] F03-AC-10 Alerts are suppressed for INVALID/PAUSED accounts.

### F06 — Dashboard
- [ ] F06-AC-01 Status, Bars, Cooldowns, Travel (when relevant), Money, Events cards render from real API data (fields per `_samples`).
- [ ] F06-AC-02 Countdowns use `Clock` and tick every second without network calls.
- [ ] F06-AC-03 Auto-refresh every 30 s only while visible; pull-to-refresh works; no duplicate calls (cache/single-flight).
- [ ] F06-AC-04 Loading/Error/Stale states exist; offline shows last data with age.
- [ ] F06-AC-05 INVALID key banner appears and the screen stops calling the API.
- [ ] F06-AC-06 No UI element is a dead button (events tap hidden until browser exists).
- [ ] F06-AC-07 Status chip uses text + color per branding doc.

### F07 — Travel Tracker (own flights)
- [ ] F07-AC-01 Travel card shows destination, direction, arrival time and a ticking countdown from API timestamps.
- [ ] F07-AC-02 A landing alert fires at the arrival time (and the optional early alert if configured).
- [ ] F07-AC-03 Changed arrival timestamps re-schedule the alert; completed flights are not re-alerted.
- [ ] F07-AC-04 Abroad state shows the country and no countdown; offline shows Stale data.

### F08 — Stock Market: prices, chart, alerts
- [ ] F08-AC-01 Stock list renders from the API with search and sorting; sparkline and 24 h change use local history (shows "—" when insufficient).
- [ ] F08-AC-02 Detail chart is a custom Canvas line chart with ranges 1h/6h/24h/7d/30d, touch inspect, and graceful "collecting data" state.
- [ ] F08-AC-03 Price above/below and %-move-in-window alerts can be created, edited, enabled/disabled, deleted.
- [ ] F08-AC-04 `StockAlertEvaluator` is pure Kotlin with unit tests covering above/below, % window, hysteresis, cooldown, one-shot vs repeating.
- [ ] F08-AC-05 Alerts fire from foreground refresh, PollWorker (15 min) and Watch Mode (30 s) and are deduped.
- [ ] F08-AC-06 Snapshots are recorded at most once per minute per stock and pruned/downsampled per retention rules.
- [ ] F08-AC-07 Missing/invalid prices never crash UI or evaluator.
- [ ] F08-AC-08 The background-limits banner is shown; Watch Mode can be toggled from this tab.
- [ ] F08-AC-09 No third-party chart/network libraries added.

## Definition of Done
- [ ] DOD-01 Project compiles with the template's Gradle/Kotlin versions; no new warnings-as-errors disabled.
- [ ] DOD-02 App launches, survives rotation, background/foreground, and process death without crashing.
- [ ] DOD-03 No TODO/FIXME/placeholder logic reachable by the user.
- [ ] DOD-04 Search for `firebase|FirebaseApp|generativeai|GoogleGenerativeAI|crashlytics|analytics` -> no hits in code/Gradle.
- [ ] DOD-05 Search for `api.torn.com` -> hits only inside `provider/torn` (and the script bridge in 1B) and always via RateLimiter.
- [ ] DOD-06 Search for `GlobalScope`, `!!` on DTO fields, `Log.d(` with request/response bodies -> none.
- [ ] DOD-07 Manifest has `allowBackup=false`, cleartext disabled, only permitted permissions.
- [ ] DOD-08 No secret appears in logs, UI (unmasked), notifications, or backups.
- [ ] DOD-09 About screen states "unofficial / not affiliated with Torn".
- [ ] DOD-10 Every data screen has Loading/Error/Empty/Stale states.
- [ ] DOD-11 Countdowns use `Clock`.
- [ ] DOD-12 Unit tests exist for the pure classes listed in E-06 for the features of this phase (or the report explains why the template cannot run tests).

## Guardrail spot checks
- [ ] GR-01 A-01..A-09 process rules respected (only this phase, plan approved, no invented data, no renames).
- [ ] GR-02 B-01..B-06 platform rules respected (Kotlin/Compose, single module, no Firebase/Gemini/analytics, allowed libs only).
- [ ] GR-03 C-01..C-11 Torn compliance respected (API only or user-loaded page; no automation; limiter used).
- [ ] GR-04 D-01..D-06 security/privacy respected (vault only, redaction, FLAG_SECURE, WebView hardening).
- [ ] GR-05 E-01..E-09 code-quality rules respected (defensive parsing, Clock, states, no GlobalScope, tests for pure logic).
- [ ] GR-06 F-01..F-05 UX rules respected (Crow palette, a11y, text+color status, staleness).
