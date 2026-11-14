# Phase 2 Checklist (GENERATED — do not edit; edit the feature files and run tools/regenerate.py)

For each line the agent reports: `ID | PASS/FAIL/NOT DONE/NA | evidence: file#symbol | note`.
The user verifies a sample of PASS items by reading the cited code and running `07_checklists/qa-manual-test-plan.md`.

## Feature acceptance criteria

### F09 — FFScouter Connector (consent, enable/disable, repository)
- [ ] F09-AC-01 No FFScouter request is ever made before consent (verify by search for the base URL usage).
- [ ] F09-AC-02 Consent dialog contains all four points and is stored per account with a timestamp.
- [ ] F09-AC-03 `FfRepository` batches, caches (1 h), rate-limits (10/min default) and returns stale data on error.
- [ ] F09-AC-04 401/403 errors produce the "Not registered at FFScouter" message with a button to open ffscouter.com.
- [ ] F09-AC-05 Disabling deletes the cache and hides FF/BS UI everywhere.
- [ ] F09-AC-06 DTOs are built from `_samples/ffscouter-*.json`; unknown/missing fields are handled (nullable).
- [ ] F09-AC-07 The key never appears in logs/UI; requests use HTTPS only.

### F10 — Ranked War Stakeout
- [ ] F10-AC-01 War detection from `rankedwars` yields the enemy faction id (sample-based); manual fallback by faction id works.
- [ ] F10-AC-02 Enemy member list loads with a single members request per refresh; refresh at most every 30 s.
- [ ] F10-AC-03 Rows show state chip (text+color), timer, last action, level; hospital countdown uses `Clock`.
- [ ] F10-AC-04 FF/BS appear only when FFScouter is enabled (F09); otherwise hidden without layout glitches.
- [ ] F10-AC-05 Quick filters, sort modes and extra filters work and persist during the session.
- [ ] F10-AC-06 `MemberDiffer` emits the listed events and is unit tested (including first-seen-already-traveling and landing cases).
- [ ] F10-AC-07 Stakeout alerts (hospital exit, landing, okay, online) fire via the `war` channel, deduped, with configurable lead times.
- [ ] F10-AC-08 Watch Mode keeps war polling alive in the background at 30 s; stopping Watch Mode stops it.
- [ ] F10-AC-09 The Attack button opens the attack page through F13 and nothing else.
- [ ] F10-AC-10 No torn.com page request is made for war data (verify by search); everything is API.
- [ ] F10-AC-11 Loading/Error/Empty/Stale states exist; throttling never crashes the screen.

### F11 — Flight Tracker (ETA estimator for OTHER players)
- [ ] F11-AC-01 If `status.until` is provided and in the future it is used directly (HIGH) — gated by sample evidence, otherwise the estimator is used.
- [ ] F11-AC-02 `FlightEstimator` computes windows per the algorithm for all category/book combinations; unit tested against `flight-times.md` incl. variance and unknown book/category.
- [ ] F11-AC-03 Confidence (GREEN/YELLOW/GRAY) is computed per the rules and always displayed with "est.".
- [ ] F11-AC-04 First-seen-already-traveling yields GRAY with a bounded window; overdue flights show DELAYED and widen as specified.
- [ ] F11-AC-05 Landing observations update `PlayerFlightProfile.hasBook` with the stated validity periods (unit tested).
- [ ] F11-AC-06 `PlaneTypeMap` has no hard-coded guesses; the Flight Debug screen can edit it; unmapped values -> UNKNOWN category.
- [ ] F11-AC-07 "Lands in N min" alerts schedule from the window start (or midpoint option), re-schedule when the window changes.
- [ ] F11-AC-08 A 15 s post-landing "Attackable in" countdown is shown on the row.
- [ ] F11-AC-09 Flight Debug screen shows the plane-type map editor, observed values, table and recent observations.
- [ ] F11-AC-10 No estimation is presented as exact; the explanation sheet lists the uncertainty sources (poll interval, 3% variance, book, delays, hidden travel).

### F12 — Chain Target Assist
- [ ] F12-AC-01 "Capture this page" is a user-pressed button; no code path loads/refreshes/paginates torn.com pages automatically (verify by search).
- [ ] F12-AC-02 `HospitalPageParser` is selector-driven from `TornSelectors`, parses rows from the sample HTML, handles unknown formats gracefully, and is unit tested with the sample.
- [ ] F12-AC-03 Captured rows store `leavesAt` (using `Clock`) and countdowns tick locally without network.
- [ ] F12-AC-04 Filters (FF, level, status, leaving-within-N-min) and sorts work and combine correctly (unit tested `TargetFilters`).
- [ ] F12-AC-05 FFScouter source works only with consent; live status checks are limited to 25 targets at <= 1 request/min per target through the RateLimiter.
- [ ] F12-AC-06 My list supports manual add/remove and "Add to targets" from War rows.
- [ ] F12-AC-07 Starred targets alert when they leave hospital; deduped.
- [ ] F12-AC-08 The Attack button opens the attack page via F13 only.
- [ ] F12-AC-09 Staleness dimming and "Refresh top N via API" work; empty and error states exist.

### F13 — Attack Shortcut
- [ ] F13-AC-01 The URL is built exactly as specified from the player id; only digits are accepted as id.
- [ ] F13-AC-02 The attack page opens in the chosen browser mode; external mode uses an Intent.
- [ ] F13-AC-03 No Crowd Hub code clicks or scripts anything on the attack page (verify by search).
- [ ] F13-AC-04 Logged-out state leads to the login page without losing the intended target (the user can tap Attack again).

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
