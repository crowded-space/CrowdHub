# GUARDRAILS — HARD RULES (highest authority)

MUST / MUST NOT are absolute. `VERIFY` = unverified fact: never assume it; handle defensively and report it.
If a request, a phase file, or your own idea conflicts with a rule below, the rule wins. If you believe a rule blocks the spec, STOP and ask.

## A. Process
- A-01 Work ONLY on the ACTIVE PHASE. Do not implement later-phase features, not even "quickly", not even as visible stubs.
- A-02 Step 0 (plan) is mandatory. Write NO code until the user replies `APPROVED`.
- A-03 If something is ambiguous, conflicting or missing: STOP and ask. NEVER invent API fields, endpoints, URLs, CSS selectors, enum values or numbers.
- A-04 Do NOT rename/move files, packages or identifiers defined in `02_architecture/package-structure.md`.
- A-05 Do NOT refactor, restyle or "improve" code outside the active phase scope.
- A-06 Every phase ends with the Audit Report (`08_prompts/audit-report-request.md`). Never write PASS without evidence (file path + symbol).
- A-07 The project MUST compile and run after every phase. If you cannot guarantee it, say so explicitly.
- A-08 Ideas outside scope go to `BACKLOG_SUGGESTIONS.md`. Do not build them.
- A-09 Do not silently drop spec items. Anything not delivered is reported as NOT DONE with the reason.

## B. Platform
- B-01 Kotlin + Jetpack Compose (Material 3). Single Activity, single Gradle module. No NDK/native code. No Java source files unless the template forces it.
- B-02 Client-side only. FORBIDDEN: any backend; Firebase (Auth, Firestore, FCM, AI Logic, App Check, Analytics, Crashlytics, Remote Config); Gemini/any LLM API calls; Google Play Services dependencies; ads; analytics; crash-reporting SDKs.
- B-03 Only the libraries in `02_architecture/tech-constraints.md`. Adding any other dependency requires asking first.
- B-04 No Hilt/Dagger/Koin. Manual DI via `AppContainer`.
- B-05 Manifest: `allowBackup="false"`, cleartext traffic disabled, only the permissions listed in tech-constraints.
- B-06 Keep the AI Studio template's `minSdk`/`targetSdk`/Gradle/Kotlin versions. Do not bump them.

## C. Torn compliance
- C-01 Torn data comes ONLY from the official API, or from a page the user loaded manually and is currently viewing.
- C-02 MUST NOT request torn.com pages (non-API) that the user did not manually trigger. No background scraping, page polling or prefetching.
- C-03 MUST NOT automate game actions: no auto-click, auto-attack, auto-travel, auto-buy/sell, auto-refill, auto-crime; no simulated taps/keystrokes on game pages. Every state-changing action is performed by the user on torn.com.
- C-04 The attack shortcut ONLY opens the attack page. It MUST NOT press "Start fight" or any other button there.
- C-05 MUST NOT ask for, store or transmit the Torn password, except via the OPT-IN auto-login in F04, and then ONLY into the torn.com login form on the device.
- C-06 Honor API limits: every API call (including native bridges used by userscripts) MUST pass through the global `RateLimiter` (F02).
- C-07 On invalid / disabled / paused key errors STOP using that key at once. No automatic retry loops. Repeated invalid-key calls can get the IP banned.
- C-08 The Torn API key MUST NOT be sent to any host except `api.torn.com`, and to `ffscouter.com` only after the explicit consent in F09.
- C-09 Identify the app honestly in `User-Agent` (`CrowdHub/<version> (Android)`). Never spoof another app or browser for API calls.
- C-10 The About screen MUST state: unofficial, not affiliated with or endorsed by Torn.
- C-11 Multi-profile exists in the data model but is NOT exposed in the main UI (only under Settings > Advanced > Profiles).

## D. Security & privacy
- D-01 Secrets (API keys, passwords) live ONLY in `SecretVault` (Android Keystore, AES-GCM). Never in SharedPreferences/DataStore/Room plaintext, logs, exception messages, notifications, deep links, clipboard, or screenshots.
- D-02 Redact secrets in ALL logs, errors and debug UIs. HTTP header/body logging is FORBIDDEN in release builds.
- D-03 `FLAG_SECURE` on every screen that displays or edits a secret.
- D-04 WebView hardening exactly as in F04 (host allowlist, file access off, no `addJavascriptInterface` for untrusted origins).
- D-05 Userscripts are user-installed code: never auto-install, never auto-update, never download in the background.
- D-06 Data leaves the device ONLY to: Torn API (always), FFScouter (consented), pages the user opens in the browser, and hosts a user-installed script is allowed to `@connect` to.

## E. Code quality
- E-01 Parse API responses defensively: `ignoreUnknownKeys`, nullable fields, safe defaults; NO `!!` on API data; unknown enum values map to `UNKNOWN`.
- E-02 All countdowns and "time left" values use `Clock` (server-offset time from F02), never raw `System.currentTimeMillis()` in UI code.
- E-03 Every network/data screen implements Loading, Error, Empty and Stale states.
- E-04 No `GlobalScope`. Structured concurrency; cancel on lifecycle. Nothing blocking on the main thread.
- E-05 Lists use `LazyColumn` with stable keys.
- E-06 Pure logic (`RateLimiter`, `Clock`, `FlightEstimator`, `MemberDiffer`, `UserscriptMetaParser`, `AlertDeduper`, `StockAlertEvaluator`) lives in plain Kotlin classes with unit tests when the template supports tests; otherwise say so in the report.
- E-07 No hard-coded keys/IDs. No mock/fake data in production code paths (demo data only in `@Preview`/tests).
- E-08 No `TODO`/`FIXME`/placeholder logic reachable by the user. Unfinished items are reported as NOT DONE.
- E-09 No reflection tricks, no dynamic code loading, no `eval` of remote code. (Userscripts run only inside the WebView sandbox.)

## F. UX
- F-01 Dark theme with the Crow palette; plain-English labels (`01_vision/branding-crow-theme.md`).
- F-02 Touch targets >= 48dp; content descriptions on icons; readable contrast.
- F-03 Status is always text + color, never color only.
- F-04 Show "updated X ago" on polled data; dim data older than 2x its refresh interval.
- F-05 Respect the user: no dark patterns, no nag dialogs, permission prompts only in context.

## G. Output discipline
- G-01 Use the templates in `08_prompts/` for plans and reports.
- G-02 State assumptions explicitly. Mark every unverified item `VERIFY`.
- G-03 Be concise. No marketing language, no apologies, no restating the spec.

## Self-check (run before every reply that contains code)
1. Is every file/feature I touched inside the active phase?
2. Did I invent any field/URL/selector? (If yes: remove or flag VERIFY.)
3. Do all Torn API calls go through `RateLimiter`? Any secret in a log/UI? Any torn.com request the user did not trigger?
4. Does the app still compile? Are Loading/Error/Empty/Stale states present?
