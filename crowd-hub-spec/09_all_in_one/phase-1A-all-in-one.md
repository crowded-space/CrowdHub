# CROWD HUB — ALL-IN-ONE PROMPT FOR PHASE 1A (GENERATED)
Use this when the project files are NOT visible to the agent. Everything the agent needs is below.
Paste the whole file into AI Studio (Android build). Then paste real sample JSON in the SAMPLES section at the end if you have not already.

======================================================================
## PART 0 — BOOTSTRAP PROMPT
======================================================================
# CROWD HUB — BOOTSTRAP PROMPT
(Paste into Google AI Studio → Build → Android. Replace 1A first.)

You are a senior Android engineer. You are building **Crowd Hub**: a native Android app (Kotlin + Jetpack Compose) that is a companion/QoL tool for the browser game **Torn** (torn.com), architected as a generic "Hub" so other services can be added later. Torn is the first Provider.

The full specification is in this project (repo files). READ THEM before answering. If you cannot see the files, say so immediately and STOP.

## ACTIVE PHASE: 1A
Allowed values: 1A | 1B | 2 | 3. You implement ONLY the active phase (`05_phases/phase-1A.md`) and the features it lists.

## AUTHORITY ORDER (when documents conflict, the higher one wins)
1. `06_guardrails/GUARDRAILS.md`  (hard rules — never violate)
2. `05_phases/phase-1A.md`
3. `04_features/*.md` (only those the phase lists)
4. `02_architecture/*.md`
5. `03_providers/**`
6. This prompt
If something is ambiguous, missing, or conflicting: **STOP AND ASK. NEVER GUESS.** Never invent API fields, endpoints, URLs, CSS selectors or numbers. Unknowns marked VERIFY must be handled defensively (nullable, no crash) and listed in your report.

## READ ORDER
`README_FIRST.md` (skim) → `06_guardrails/*` → `02_architecture/*` → `03_providers/torn/*` (+ `_samples/`) → the phase file → the feature files it lists → `07_checklists/phase-1A-checklist.md`.

## STEP 0 — PLAN ONLY (NO CODE)
Reply with exactly these sections, then STOP and wait for the user to reply `APPROVED`:
1. **Files** you will create/modify, in build order.
2. **Unknowns & assumptions** (every VERIFY item you will touch; which `_samples` files you found/missing).
3. **Spec conflicts or gaps** you found.
4. **Questions** (numbered; max 8).
Do not write code in Step 0.

## STEP 1 — BUILD (only after `APPROVED`)
- Follow the phase "Build order" exactly. Complete each step fully before the next.
- Do not add features, screens, libraries, animations, settings, or refactors that are not in the active phase. Put ideas into `BACKLOG_SUGGESTIONS.md` instead.
- The project must compile and run when you finish. If you cannot guarantee it, say so.

## STEP 2 — AUDIT REPORT (mandatory, same reply as the build summary)
Use the template in `08_prompts/audit-report-request.md`: every checklist ID → PASS / FAIL / NOT DONE / NA with evidence (file path + symbol). Never write PASS without evidence. List deviations and unverified items honestly.

## THE 12 RULES YOU MUST NOT BREAK (full list in GUARDRAILS.md)
1. Only the active phase. 2. Plan first, wait for APPROVED. 3. Never guess; ask.
4. Kotlin + Compose, single Activity, single module, client-side only.
5. NO Firebase, NO Gemini API, NO analytics/ads/crash SDKs, NO Hilt/Dagger/Koin, no unlisted dependencies.
6. Torn data ONLY via the official API, or from a page the user loaded manually and is viewing.
7. NO automation of game actions (no auto-click/attack/travel/buy). Attack shortcut only opens the page.
8. ALL Torn API calls go through the global RateLimiter; on invalid-key errors stop using that key immediately.
9. Secrets ONLY in SecretVault (Android Keystore). Never log, display (unmasked), back up, or transmit secrets except as the spec allows.
10. All countdowns use Clock (server-offset), not raw device time.
11. Every data screen has Loading / Error / Empty / Stale states; parse API data defensively (no `!!`).
12. Do not rename or move anything defined in `02_architecture/package-structure.md`.

## REMINDER (end of prompt — repeat of the most important rules)
Plan → wait for APPROVED → build only the active phase → audit report with evidence.
When in doubt: STOP AND ASK.


======================================================================
## PART 1 — GUARDRAILS (highest authority)  (`06_guardrails/GUARDRAILS.md`)
======================================================================
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


======================================================================
## PART 2 — ANTI SCOPE CREEP  (`06_guardrails/anti-scope-creep.md`)
======================================================================
# Anti-Scope-Creep

AI coding agents tend to "help" by adding things. Do not.

## Phase fence
| Phase | Allowed features | Forbidden until later |
|---|---|---|
| 1A | F00 F01 F02 F03 F06 F07 F08 | browser, scripts, FFScouter, war, flight, chain |
| 1B | F04 F05 | FFScouter, war, flight, chain |
| 2 | F09 F10 F11 F12 F13 | anything in F14 |
| 3 | F14 items the user names | everything else |

## Never add (any phase) unless the user explicitly asks
- Extra screens, tabs, onboarding carousels (onboarding is exactly the steps in F01).
- Animations beyond default Compose transitions; custom fonts; light theme; localization.
- New settings that are not in a feature file; "advanced" toggles; feature flags.
- Extra libraries (charts, image loaders, DI, networking, analytics...). The chart is custom Compose Canvas.
- Architecture "improvements": extra interfaces, base classes, generic repositories, use-case layers, multi-module splits.
- Behavioral "improvements" to Torn flows: macros, auto-refresh of game pages, auto-fill that clicks, any automation.
- Telemetry, crash reporting, remote config, update checkers.

## When you have an idea
Append to `BACKLOG_SUGGESTIONS.md`: `- [phase?] idea — why — risk`. Do not implement.

## When the spec is wrong or incomplete
Stop. Ask in Step 0 (or as a numbered question in the report). Do not patch the spec in code.

## Size guard
Prefer the smallest implementation that satisfies the acceptance criteria. If a file grows beyond ~400 lines, split by responsibility (inside the same package) — do not introduce new layers.


======================================================================
## PART 3 — SECURITY & PRIVACY  (`06_guardrails/security-and-privacy.md`)
======================================================================
# Security & Privacy

## Data inventory
| Data | Stored where | Encrypted | Leaves device? | Notes |
|---|---|---|---|---|
| Torn API key | `SecretVault` (`secret_blob`) | Yes (Keystore AES-GCM) | `api.torn.com`; `ffscouter.com` only if consented | One key per ProviderAccount |
| Torn web user/pass (optional) | `SecretVault` | Yes | Only typed into torn.com login form | Opt-in (F04) |
| WebView cookies/storage | Android WebView profile storage | OS-level | Only to torn.com | Per-profile isolation when supported |
| Settings/preferences | DataStore (non-secret) | No | No | No secrets here |
| Stock history, caches, flights, targets | Room | No | No | Non-secret |
| Userscripts + GM storage | Room | No | No | Scripts are user-installed code |

## SecretVault design
- Interface: `put(alias, value)`, `get(alias): String?`, `delete(alias)`, `deleteByPrefix(prefix)`, `has(alias)`.
- Master key: AES-256, `AndroidKeyStore`, alias `crowdhub_master_v1`, `KeyGenParameterSpec` with GCM, no user-auth requirement in Phase 1-2.
- Per-secret random 12-byte IV; store `(alias, iv, ciphertext)` in Room table `secret_blob`.
- Alias convention: `acct:<accountId>:api_key`, `acct:<accountId>:web_user`, `acct:<accountId>:web_pass`.
- If decrypt fails (key invalidated): treat as "secret missing", prompt re-entry. Never crash.
- Do NOT use deprecated/third-party crypto wrappers; use platform `javax.crypto` + `KeyStore`.

## Masking & display
- API key shown as `••••••••<last4>`. No "reveal" button in Phase 1-2.
- No copy-to-clipboard for secrets. (Raw JSON in API Probe is redacted and may be copied.)
- Screens with secrets set `FLAG_SECURE`.

## Backups
- `android:allowBackup="false"`; also provide `dataExtractionRules`/`fullBackupContent` excluding everything.
- Settings export (Phase 3) must exclude secrets by default.

## Logging
- Use a `Log` wrapper `CrowLog` that redacts anything matching: the active API keys, `key=` query values, `Authorization` headers, password fields.
- No logging of request/response bodies in release. Debug builds may log bodies only through API Probe (user-initiated, redacted).

## Network
- HTTPS only. `usesCleartextTraffic=false` + network security config denying cleartext.
- OkHttp default certificate validation; no custom TrustManagers, no pinning in Phase 1-2.
- Timeouts: connect 15 s, read 20 s, call 30 s.

## WebView (F04) — summary
- JavaScript on (needed by torn.com), DOM storage on, file access off, content access off, no universal access from file URLs, mixed content blocked.
- Navigation guard: only provider `allowedWebHosts`; other hosts open in the external browser after a confirmation dialog.
- Bridges: `WebViewCompat.addWebMessageListener` with strict `allowedOriginRules`; never `addJavascriptInterface` on arbitrary origins.

## Threat notes (why the rules exist)
- Lost phone: vault + FLAG_SECURE reduce exposure; consider biometric lock in Phase 3.
- Malicious userscript: sandboxed to the WebView; native bridges restricted by `@connect`, https-only, rate-limited, and blocked for non-API torn.com hosts.
- Third-party leak: FFScouter receives the Torn key only after explicit per-profile consent and can be disabled at any time (deletes cache; the key stays in the vault).


======================================================================
## PART 4 — PRODUCT BRIEF  (`01_vision/product-brief.md`)
======================================================================
# Product Brief — Crowd Hub

## One-liner
A privacy-first Android companion hub. Torn (torn.com) is the first Provider; the architecture must allow other services later without rewriting the core.

## Who it is for
A single Torn player (the author) who wants a Torn PDA-like experience with extra war/chain/stock tooling. Personal-use APK first; Play Store is out of scope.

## Principles
1. **Local-only.** No backend. API keys, cookies and credentials never leave the device (except where the spec explicitly allows: Torn API, consented FFScouter, pages the user opens).
2. **Compliance first.** Respect Torn's API limits and scripting rules (see `03_providers/torn/torn-rules-and-compliance.md`). Anything that could be seen as automation is out.
3. **Honest data.** Estimates (flight ETA, battle stats) are always labeled as estimates with a confidence indicator.
4. **Preparation, not pinpoint.** ETAs are for planning and calculation, not exact-second guarantees.
5. **Small abstractions.** Only three interfaces (`Provider`, `Connector`, `AlertSource`). No framework-building.
6. **Flavor, not friction.** Crow theme in visuals; plain English in labels.

## Scope by phase
- **1A** Profile container + vault, API manager (limiter/cache/clock), alert engine (timers + Watch Mode), dashboard, own-travel card, stock market (chart + alerts), settings/about.
- **1B** In-app browser, persistent web session, optional auto-login, userscript engine (PDA-compatible), script manager.
- **2** FFScouter connector, ranked war stakeout, flight tracker (own ETA estimator), chain target assist, attack shortcut.
- **3** Backlog (see F14).

## Non-goals (never build unless a later spec says so)
- Multi-service UIs other than Torn. Light theme (until Phase 3). Localization. Cloud sync. Accounts/login to any Crowd Hub service. Push via FCM. Auto-play/automation of any kind. In-app purchases/ads. Social features.

## Success criteria
- Phase 1A: key onboarding works; dashboard correct vs. the game; timers fire on time; stock alerts fire on threshold; app survives restart/offline.
- Phase 1B: login persists across restarts; scripts run on torn.com pages; kill switch works.
- Phase 2: enemy faction list updates at most every 30 s, statuses/timers correct; flight ETA windows labeled with confidence; hospital-based target list filters by FF and release time; one tap opens the attack page.


======================================================================
## PART 5 — BRANDING  (`01_vision/branding-crow-theme.md`)
======================================================================
# Branding — "Crow" theme

## Name
**Crowd Hub**. Wordmark: "Crow" in primary text color, "d" in the Accent color, "Hub" in muted text. The "d" is the semantic play: Crow -> Crowd (a flock). Build it with `buildAnnotatedString` (three spans) in a `CrowdWordmark()` composable. Never render the wordmark as an image.

## Palette (dark only for Phase 1-2)
| Token | Hex | Use |
|---|---|---|
| `CrowBlack` | `#0E0F13` | app background |
| `Surface` | `#171A21` | cards |
| `SurfaceHigh` | `#1F2330` | dialogs, sheets, selected rows |
| `Outline` | `#2C3242` | dividers/borders |
| `Sheen` (primary) | `#7C5CFF` | primary actions, active nav, chart line |
| `SheenSoft` | `#A58BFF` | secondary emphasis |
| `AccentD` (the "d") | `#FFB020` | wordmark "d", highlights, attention |
| `Text` | `#E6E8EE` | primary text |
| `TextMuted` | `#9AA3B5` | secondary text |
| `Ok` | `#3DDC97` | Okay / success |
| `Danger` | `#FF5C6C` | Hospital / error |
| `Info` | `#4DA3FF` | Traveling |
| `Abroad` | `#B48CFF` | Abroad |
| `Jail` | `#FF8A3D` | Jail / Federal |

Torn status chips: Okay=`Ok`, Hospital=`Danger`, Traveling=`Info`, Abroad=`Abroad`, Jail/Federal=`Jail`, Unknown=`TextMuted`. Status is ALWAYS text + color (never color only).

## Typography
System default sans for UI (no custom font files). Numbers/timers/prices: tabular figures where available, otherwise monospace. Material 3 type scale; no custom scale.

## Shape & feel
Rounded 12dp cards, 8dp chips. Subtle 1dp `Outline` borders. Dense but readable (HUD-like). No gradients except an optional faint `Sheen` glow on the splash/header.

## Icon & splash
Simple geometric crow-head vector (adaptive icon: `CrowBlack` background, crow silhouette with a thin `Sheen` highlight, small `AccentD` dot as the eye). Placeholder art is acceptable; final art comes later. Do not generate bitmap images.

## Microcopy
Plain English, short, tactical. Crow flavor allowed ONLY in: splash tagline ("Watch the flock."), empty states (e.g., "Nothing perched here yet."), and About. Never in labels, errors, or settings.

## Navigation labels (plain)
Dashboard, Stocks, War, Targets, Browser. Overflow menu: Profiles, Scripts, Settings, About. Tabs appear only when their phase ships.


======================================================================
## PART 6 — ARCHITECTURE  (`02_architecture/architecture-overview.md`)
======================================================================
# Architecture Overview

## Layers (dependency rule: arrows point downward only)
```
feature/*      Torn-specific features (dashboard, stocks, war, flight, chain, ...)
   |  uses
provider/torn  TornProvider + Torn API client + Torn models/rules
connector/*    Third-party services attached to a ProviderAccount (FFScouter)
   |  uses
core/*         Generic platform: profile, vault, network, alerts, browser, scripts, db, ui
```
- `core` MUST NOT import `provider`, `connector` or `feature`.
- `provider` may import `core`. `connector` may import `core` and its parent provider's public models.
- `feature` may import `core`, `provider`, `connector`.
- If a class knows the word "Torn", it does NOT belong in `core`.

## The only three abstractions
```kotlin
interface Provider {
    val id: String                       // "torn"
    val displayName: String
    val allowedWebHosts: Set<String>     // e.g. setOf("www.torn.com", "torn.com")
    val rateLimit: RateLimitPolicy       // Torn: 100/min budget, 30 s cache
    suspend fun validateCredential(accountId: String, secret: String): ValidationResult
    fun isLoginUrl(url: String): Boolean
    fun alertSources(): List<AlertSource>
}

interface Connector {
    val id: String                       // "ffscouter"
    val parentProviderId: String         // "torn"
    val requiresConsent: Boolean
}

interface AlertSource {
    val id: String                       // "torn.bars", "torn.stocks"
    suspend fun collect(ctx: AlertContext): List<AlertCandidate>
}
```
Do NOT add more interfaces, base classes, generic repositories or use-case layers (see anti-scope-creep).

## Profile model (multi-profile ready)
`Profile` 1—N `ProviderAccount` 1—N `ConnectorLink`. Secrets are keyed by account id in `SecretVault`. All feature data is scoped by `accountId`. The UI uses the single ACTIVE profile; switching profiles is hidden under Settings > Advanced > Profiles.

## Data flow
```
UI (Compose) <- StateFlow <- ViewModel <- Repository <- ApiClient (OkHttp) -> RateLimiter -> Cache -> network
                                              |-> Room (history, caches)
AlertEngine: AlertSource.collect() -> AlertDeduper -> Scheduler (AlarmManager / notification)
```
- ViewModels expose immutable `UiState` (sealed: Loading / Content / Error / Empty) via `StateFlow`.
- Repositories return `ApiResult<T>` (never throw to UI).
- Dependency wiring: a single `AppContainer` created in `CrowdHubApp`, passed to ViewModel factories.

## Threading
- `Dispatchers.IO` for network/DB; `Default` for parsing/estimation; main thread only for UI/WebView calls.
- One app-level `CoroutineScope(SupervisorJob() + Dispatchers.Default)` owned by `AppContainer`; services/workers use their own lifecycle scopes.

## Navigation
Single `NavHost` in `MainActivity`. Bottom bar appears with the tabs of shipped phases (see branding doc). Top app bar: wordmark + overflow menu (Profiles, Scripts, Settings, About).

## Time
`Clock` (F02) returns `nowEpochMs()` corrected by the server offset derived from HTTP `Date` headers. Every countdown and ETA uses it.

## Error model
`ApiError`: Network, Timeout, InvalidKey, KeyPaused, AccessLevel, RateLimited, IpBlocked, ProviderDown, Parse, Unknown(code, message). UI maps each to a clear message + action (retry / fix key / wait).


======================================================================
## PART 7 — PACKAGE STRUCTURE  (`02_architecture/package-structure.md`)
======================================================================
# Package Structure (DO NOT rename or move)

Base package: `app.crowdhub` (use the template's package only if AI Studio forces it; then keep the same sub-structure).

```
app.crowdhub
|- CrowdHubApp.kt                 Application + AppContainer creation
|- MainActivity.kt                single Activity, NavHost
|- core/
|  |- ui/            theme (Color.kt, Theme.kt, Type.kt), components (StatusChip, CountdownText, StaleBadge, CrowdWordmark), nav
|  |- db/            CrowdDatabase, Daos, Entities (see data-model.md), Migrations
|  |- settings/      SettingsStore (DataStore, non-secret)
|  |- vault/         SecretVault, KeystoreCipher
|  |- profile/       ProfileRepository, ActiveProfile, onboarding state
|  |- network/       HttpClientFactory, RateLimiter, ResponseCache, Clock, ApiResult, ApiError, CrowLog
|  |- alerts/        AlertEngine, AlertCandidate, AlertDeduper, Scheduler, NotificationChannels,
|  |                 AlarmReceiver, BootReceiver, PollWorker, WatchModeService
|  |- browser/       CrowWebView (Phase 1B), WebProfileManager, UrlGuard, SessionMonitor
|  |- scripts/       UserscriptEngine, UserscriptMetaParser, GmBridge, ScriptRepository (Phase 1B)
|- provider/
|  |- Provider.kt, ProviderRegistry.kt
|  |- torn/          TornProvider, TornApi (endpoints), TornDto (models), TornMappers, TornUrls, TornRules,
|                    TornSelectors (login/hospital DOM selectors, VERIFY), FlightTable (Phase 2)
|- connector/
|  |- Connector.kt
|  |- ffscouter/     FfScouterConnector, FfApi, FfDto, FfRepository (Phase 2)
|- feature/
   |- dashboard/     DashboardScreen, DashboardViewModel, cards (Bars, Cooldowns, Status, Travel, Money, Events)
   |- stocks/        StocksScreen, StockDetailScreen, StocksViewModel, PriceChart (Canvas), StockAlertEvaluator, StockRepository
   |- profiles/      ProfilesScreen (advanced), OnboardingScreens, AddAccountScreen
   |- settings/      SettingsScreen, AboutScreen, PrivacyScreen, DeveloperScreen (API Probe)
   |- browser/       BrowserScreen, QuickLinks (Phase 1B)
   |- scripts/       ScriptsScreen, ScriptEditor/Installer (Phase 1B)
   |- war/           WarScreen, WarViewModel, MemberRow, MemberDiffer, StakeoutManager (Phase 2)
   |- flight/        FlightEstimator, FlightRepository, FlightChip, FlightDebugScreen (Phase 2)
   |- chain/         TargetsScreen, TargetsViewModel, HospitalPageParser, TargetFilters (Phase 2)
```

## Naming rules
- Files named after the main class. One public class per file (small private helpers allowed).
- ViewModels: `<Feature>ViewModel`. Screens: `<Feature>Screen`. Repositories: `<Feature>Repository`. DTOs end with `Dto`. Room entities end with `Entity`.
- Strings in `strings.xml` for user-visible text. Constants in `object <Name>Defaults`.
- Tests mirror the package under `src/test` (pure Kotlin only).

## Files that MUST exist at the repo root (besides Gradle files)
`BACKLOG_SUGGESTIONS.md` (create empty on first build), the spec folders from this pack (unchanged).


======================================================================
## PART 8 — DATA MODEL  (`02_architecture/data-model.md`)
======================================================================
# Data Model (Room) — schema v1 (Phase 1A), extended per phase with migrations

Rules: `exportSchema = true`. NO `fallbackToDestructiveMigration`. Each phase that changes the schema bumps the version and ships a `Migration`. All IDs are `String` (UUID) unless stated. Timestamps are epoch milliseconds (`Long`) unless the column name ends with `Sec`.

## Phase 1A entities
```
ProfileEntity(id PK, name, createdAt, isActive)
ProviderAccountEntity(id PK, profileId FK->Profile CASCADE, providerId "torn", externalId /*player id*/ ?, displayName ?,
                      status [UNKNOWN|VALID|INVALID|PAUSED], accessLevel ?, lastValidatedAt ?, createdAt)
ConnectorLinkEntity(id PK, accountId FK->ProviderAccount CASCADE, connectorId "ffscouter", enabled, consentAt ?, UNIQUE(accountId, connectorId))
SecretBlobEntity(alias PK, iv BLOB, ciphertext BLOB, updatedAt)
ScheduledAlertEntity(dedupeKey PK, accountId, channelId, title, body, fireAt, deepLink ?, state [SCHEDULED|FIRED|CANCELLED], updatedAt)
StockSnapshotEntity(id PK auto, stockId Int, ts, price Double)           INDEX(stockId, ts)
StockWatchEntity(id PK, accountId, stockId Int, aboveTarget Double?, belowTarget Double?, pctMove Double?, pctWindowMin Int?, repeating Boolean, cooldownMin Int, armed Boolean, lastTriggeredAt ?, enabled)
CacheEntryEntity(key PK, body TEXT, fetchedAt, ttlMs)                    // optional persistent cache for stale display
```
`StockSnapshotEntity` is not account-scoped (stock prices are public data); retention: max 1 row per stock per 60 s; prune > 30 days; rows older than 48 h downsampled to 15-min buckets by a daily cleanup.

## Phase 1B additions
```
UserscriptEntity(id PK, accountId, name, namespace ?, version ?, description ?, source TEXT, matches (JSON list), excludes (JSON list),
                 runAt [DOCUMENT_START|DOCUMENT_END|DOCUMENT_IDLE], grants (JSON list), connects (JSON list),
                 allowApiKey Boolean, enabled, installedFromUrl ?, installedAt, updatedAt)
ScriptKvEntity(scriptId FK CASCADE, key, value TEXT, PRIMARY KEY(scriptId, key))
ScriptLogEntity(id PK auto, scriptId, ts, level, message)   // keep last 200 per script
```
(Credentials for auto-login go to `SecretVault`, not Room.)

## Phase 2 additions
```
FfCacheEntity(playerId Long PK, fairFight Double?, bsEstimate Long?, bsHuman String?, lastUpdatedSec Long?, fetchedAt)
MemberSnapshotEntity(factionId Long, playerId Long, ts, state, descriptionHash, untilSec ?, planeType ?, lastActionStatus ?, lastActionSec ?, level ?, PRIMARY KEY(factionId, playerId))
   // only the LATEST snapshot per member is persisted; older ones live in memory for the diff
StakeoutEntity(accountId, playerId Long, alertHospital, alertLanding, alertOnline, alertOkay, createdAt, PRIMARY KEY(accountId, playerId))
FlightObservationEntity(id PK auto, playerId Long, direction [OUT|RETURN], place, planeType ?, prevPollAt ?, detectedAt ?, landedPrevPollAt ?, landedDetectedAt ?, observedMinMs ?, observedMaxMs ?, inferredBook Boolean?)
PlayerFlightProfileEntity(playerId Long PK, hasBook Boolean?, lastObservedAt ?)
PlaneTypeMapEntity(apiValue PK, category [STANDARD|AIRSTRIP|WLT|BUSINESS|UNKNOWN], source [DEFAULT|USER])
TargetEntity(accountId, playerId Long, name ?, note ?, source [HOSPITAL_PAGE|FFSCOUTER|MANUAL|WAR], addedAt, PRIMARY KEY(accountId, playerId))
HospitalCaptureEntity(accountId, playerId Long, name, level ?, remainingSecAtCapture Long?, capturedAt, PRIMARY KEY(accountId, playerId))
```

## Deletion semantics
- Delete Profile -> cascade accounts/links; then `SecretVault.deleteByPrefix("acct:<id>:")`; cancel alarms/workers; clear web profile (1B); delete account-scoped rows in all tables above.
- Disable FFScouter -> delete `FfCacheEntity` rows; keep the secret in the vault (it's the Torn key).


======================================================================
## PART 9 — TECH CONSTRAINTS  (`02_architecture/tech-constraints.md`)
======================================================================
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


======================================================================
## PART 10 — PROVIDER DOC  (`03_providers/torn/torn-rules-and-compliance.md`)
======================================================================
# Torn — Rules & Compliance (research snapshot, Nov 2026)

> Torn's rules can change. These notes are a snapshot of what was found during research. The user MUST re-read Torn's current rules (scripting + API terms) before any release. (Checklist item in `07_checklists/definition-of-done.md`.)

## 1. Scripting rule (core of the guardrails)
A script/tool is acceptable when it works with data from the **official API**, or from a **page the player loaded manually and is currently viewing**. Requests to Torn that are neither API calls nor manually triggered by the player are NOT acceptable. Therefore:
- Allowed: API polling within limits; parsing a page the user has open (button "Capture this page" pressed by the user); user-installed userscripts that decorate the page the user is viewing; opening a link the user tapped.
- Forbidden: background fetching of torn.com pages; prefetch; automatic page refresh loops; auto-click/auto-attack/auto-travel/auto-buy; simulated input on game pages; headless browsing.

## 2. API rules (snapshot)
- Limit: **100 requests per minute per user** (all keys of the same user count together). The app budgets lower (see F02).
- Responses are cached by the service for up to ~**30 seconds**; polling faster is pointless.
- Repeated requests with **invalid keys can cause a temporary IP ban**. -> stop using a key immediately when it is rejected.
- Passwords must never be requested by API-based services.
- Services must be transparent to users about how their key is used (Torn publishes a table: data storage, data sharing, purpose, key storage/sharing, access level). Crowd Hub's declaration (use verbatim in the Privacy screen; VERIFY wording against Torn's current table):
  - Data storage: only locally on the device.
  - Data sharing: nobody (exception: FFScouter, only if the user enables it).
  - Purpose of use: personal, non-commercial companion app.
  - Key storage: locally, encrypted (Android Keystore).
  - Key sharing: not shared (exception: FFScouter, opt-in).
  - Access level required: Limited.
- API keys are read-only access to data; they cannot log in to the website. Web login requires the player's own session (F04).

## 3. Accounts
Torn is generally understood to prohibit multiple accounts per person. Crowd Hub keeps a multi-profile **data model** (for architecture only). Multi-profile UI is hidden in Settings > Advanced and the About/Privacy screen reminds the user that complying with Torn's account rules is their responsibility. (Unverified by this pack: confirm in Torn's rules.)

## 4. Feature compliance map
| Feature | Rule basis | Design constraint |
|---|---|---|
| Dashboard, stocks, war stakeout, flight tracker | API only | Through RateLimiter; no page requests |
| Hospital target list (source A) | User-loaded page | Only on user pressing "Capture"; no auto-navigation/auto-refresh |
| Attack shortcut | User-initiated link | Opens page only; no clicking inside |
| Userscripts | User-installed; run on page user views | No background download; native bridges blocked for non-API torn.com hosts |
| Auto-login (opt-in) | Own session, own credentials | Fills login form only; default no auto-submit; stops on captcha/2FA |
| FFScouter | Third-party service, consent | Explicit consent; disclose key sharing |

## 5. Login methods (PDA lesson)
Torn PDA's developer notes that "Login with Google" does not work inside the web view; players use their Torn username/password (accounts created via Google/Apple still have a password emailed on day 1 / recoverable). Crowd Hub follows the same limitation.


======================================================================
## PART 11 — PROVIDER DOC  (`03_providers/torn/torn-api-reference.md`)
======================================================================
# Torn API Reference (for implementers)

> Everything marked **VERIFY** must be confirmed against `_samples/*.json` (real responses) before use. If a sample is missing, implement defensively (nullable DTO fields) and list the item in your report. Do NOT invent fields.

## Base & auth
- v2 base: `https://api.torn.com/v2/` — v1 base: `https://api.torn.com/`.
- Auth: v2 supports `Authorization: ApiKey <key>` header (VERIFY) -> preferred, so the key never appears in URLs. v1 requires `?key=<key>`. A single `TornApi` class hides the difference; callers never see the key.
- Some selections still live only in v1 (bars, cooldowns, travel, notifications and others were noted as falling back to v1). `TornApi` must support BOTH and choose per selection (table below).
- Always send `User-Agent: CrowdHub/<version> (Android)`.

## Limits & caching (implemented in F02)
- 100 req/min per user (all keys). Service cache ~30 s. Repeated invalid keys -> temporary IP ban.
- Access levels: Public, Minimal, Limited, Full. Crowd Hub requires **Limited**.

## Error object
Responses may contain `{"error": {"code": <int>, "error": "<text>"}}` (shape VERIFY for v2). Best-known codes (VERIFY each):
| Code | Meaning | App behavior |
|---|---|---|
| 2 | Incorrect key | INVALID; stop using key; prompt |
| 5 | Too many requests | RateLimited; back off 60 s; lower priority classes first |
| 8 | IP temporarily blocked | IpBlocked; pause ALL calls 5 min; then single probe |
| 9 | API system disabled | ProviderDown; retry later (>= 2 min) |
| 10 | Key owner in federal jail | PAUSED; retry hourly |
| 13 | Key disabled (owner inactive) | INVALID |
| 16 | Access level too low | AccessLevel; show "create a Limited key" |
| 17 | Backend error | ProviderDown; retry later |
| 18 | Key paused by owner | PAUSED |
Unknown codes -> `Unknown(code, message)` shown verbatim. NEVER retry automatically on 2, 8, 10, 13, 16, 18.

## Selections by feature (paths/fields VERIFY via `_samples`)
| Need | v2 path (VERIFY) | v1 fallback (VERIFY) | Fields expected (VERIFY) | Phase |
|---|---|---|---|---|
| Validate key, player id/name/level/status | `user/basic` | `user/?selections=basic` | player_id, name, level, status{state, description, until} | 1A |
| Bars | (likely v1) | `user/?selections=bars` | energy/nerve/happy/life: current, maximum, increment, interval, tick_time, fulltime | 1A |
| Cooldowns | (likely v1) | `user/?selections=cooldowns` | drug, medical, booster (seconds) | 1A |
| Own travel | (likely v1) | `user/?selections=travel` | destination, timestamp (arrival), departed, time_left, method | 1A |
| Money | `user/money` | `user/?selections=money` | money_onhand, points, ... | 1A |
| Notifications/events | `user/notifications`, `user/events` | same names | counts, recent events | 1A |
| Stocks (public list) | `torn/stocks` | `torn/?selections=stocks` | stock_id, name, acronym, current_price, market_cap, total_shares, available_shares, forecast, demand, benefit | 1A |
| Own faction id | `user/faction` or profile | `user/?selections=profile` | faction.faction_id | 2 |
| Ranked wars of a faction | `faction/{id}/rankedwars` (confirmed in the wild) | — | war list w/ factions + timestamps | 2 |
| Faction members | `faction/{id}/members` (confirmed in the wild) | `faction/{id}?selections=basic` | per member: id, name, level, status{state, description, until, plane_image_type?}, last_action{status, relative, timestamp} | 2 |
| Other user's profile | `user/{id}/profile` | `user/{id}?selections=profile` | status, last_action, level, faction | 2 |
Notes: `status.until` may exist but whether it is populated for `Traveling` is UNKNOWN (VERIFY). `plane_image_type` is documented as populated only when state is `Traveling`; its possible values are UNKNOWN (VERIFY; mapped in F11). The legacy `travel_type` field is deprecated.

## Status model (lenient)
`state` -> enum `OKAY, HOSPITAL, JAIL, FEDERAL, TRAVELING, ABROAD, FALLEN, UNKNOWN` (case-insensitive; anything else = UNKNOWN). Keep the raw `description` string. `until` is epoch seconds when present.

## Destination parsing (VERIFY with samples)
From `description`: patterns like `Traveling to <Place>`, `Returning to Torn from <Place>`, `In <Place>`. Canonical places: `MEXICO, CAYMAN_ISLANDS, CANADA, HAWAII, UNITED_KINGDOM, ARGENTINA, SWITZERLAND, JAPAN, CHINA, UAE, SOUTH_AFRICA`. Synonyms: "Cayman Islands"/"Cayman", "United Kingdom"/"UK", "United Arab Emirates"/"UAE"/"Dubai"(city names like Zurich/Tokyo may appear). Unknown place -> `UNKNOWN` (flight tracker degrades to GRAY).

## URLs (`TornUrls.kt`)
- Home: `https://www.torn.com/index.php`
- Profile: `https://www.torn.com/profiles.php?XID={playerId}`
- Attack: `https://www.torn.com/loader.php?sid=attack&user2ID={playerId}`
- Hospital list: `https://www.torn.com/hospitalview.php`
- API docs/playground: `https://www.torn.com/api.html` (key creation page: `https://www.torn.com/preferences.php#tab=api`, VERIFY)
Only these are hard-coded; others must come from the user (quick links) or be marked VERIFY.

## Clock
Parse the HTTP `Date` header on every successful response; keep the last 5 offsets (server - device); use the median. `Clock.nowEpochMs()` = device time + offset. Unit-test with fake headers.

## Public vs. keyed data
`torn/stocks` and faction lists are readable with the user's key; no extra key is needed. Never use another person's key.


======================================================================
## PART 12 — FEATURE  (`04_features/F00-settings-privacy-about.md`)
======================================================================
# F00 — Settings, Privacy, About, Developer tools
Phase: 1A | Depends on: F01, F02 | Package: `feature/settings`

## Purpose
Central place for preferences, transparency about data, legal disclaimer, and a safe way to collect real API samples.

## Behavior
1. **Settings** (list): Notifications (per alert type toggle + lead times), Watch Mode (default duration 1/2/4/6 h), Data (clear caches, clear stock history), Reliability (info page + deep link to system battery settings), Advanced (Profiles — F01), Developer (hidden until tapping the version row 7 times).
2. **Privacy screen**: shows the data inventory summary (from `06_guardrails/security-and-privacy.md`) and the Torn API disclosure block (storage, sharing, purpose, key storage, key sharing, access level) from `torn-rules-and-compliance.md`.
3. **About**: wordmark, version, "Unofficial app. Not affiliated with or endorsed by Torn." and a reminder that the user is responsible for following Torn's rules (accounts, scripts, API).
4. **Developer > API Probe**: pick a selection (dropdown of the selections in `torn-api-reference.md`), optional path params (id), run once with USER priority via F02, show the raw JSON **with secrets redacted** and a Copy button. Shows rate-limiter state (calls in the last 60 s per priority). Also shows last-response HTTP `Date` header and the clock offset.
5. Preferences are stored in DataStore (non-secret).

## Edge cases
- Probe on an INVALID account: show the error, do not call.
- Probe output larger than ~200 KB: truncate display, still allow copy of the first 200 KB.

## Acceptance criteria
- [ ] F00-AC-01 Settings screen lists Notifications, Watch Mode, Data, Reliability, Advanced; Developer appears only after 7 taps on the version row.
- [ ] F00-AC-02 Privacy screen shows the Torn API disclosure block exactly as specified.
- [ ] F00-AC-03 About screen states the app is unofficial/not affiliated with Torn and shows the version.
- [ ] F00-AC-04 API Probe runs via the global RateLimiter (priority USER) and the output never contains the API key.
- [ ] F00-AC-05 API Probe shows clock offset and the rate-limiter usage counters.
- [ ] F00-AC-06 All preferences persist across app restarts and contain no secrets.

## Out of scope
Theme switching, language selection, export/import (Phase 3).


======================================================================
## PART 13 — FEATURE  (`04_features/F01-profile-container-and-vault.md`)
======================================================================
# F01 — Profile Container & Secret Vault
Phase: 1A | Depends on: core/db, core/vault | Package: `core/profile`, `core/vault`, `feature/profiles`

## Purpose
A generic container that groups everything about one account: provider account(s), secrets, connectors, preferences, data. Multi-profile ready, single-profile UX.

## Concepts
`Profile` (name) -> `ProviderAccount` (Torn, player id) -> `ConnectorLink` (FFScouter...). Secrets in `SecretVault` keyed `acct:<accountId>:<name>`.

## Behavior
1. **First launch -> Onboarding (exactly these steps, no carousel):**
   1. Welcome (wordmark + tagline "Watch the flock." + "Get started").
   2. Create profile: name field, default "Main".
   3. Add Torn account: explains "Use a Limited-access API key. You can revoke it anytime in Torn." + button "Open Torn API page" (opens the in-app/external browser to `https://www.torn.com/preferences.php#tab=api`, VERIFY) + key input (masked, paste-friendly, 16 chars expected but do not hard-fail on length) + "Validate".
   4. Validation: call `user/basic` (USER priority). On success store player id + name, status VALID. Then probe one Limited selection (`bars`); if error code 16 -> show "Key access level too low; create a Limited key" and do not mark the onboarding complete.
   5. Permissions in context: notifications (Android 13+), exact-alarm info (explain, deep-link to the system screen if not granted), skip allowed.
   6. Land on Dashboard.
2. **Active profile**: exactly one profile is `isActive`. All queries are scoped to the active profile's accounts. Phase 1 UI uses the first/only Torn account.
3. **Settings > Advanced > Profiles** (hidden by default; see C-11): list, create, rename, delete, switch active. Deleting asks for confirmation and performs the full wipe (see `data-model.md`).
4. **Account screen** (from Profiles or Settings): shows status rows — "Torn API: Valid (Limited)", "Web session" (Phase 1B; hidden in 1A), "FFScouter" (Phase 2; hidden in 1A). Actions: Replace key, Remove key, Delete account.
5. **Replace key**: validate new key first, then swap atomically; keep data. If the new key belongs to a different player id -> warn and require "Reset data for this account" confirmation.
6. **Vault** per `security-and-privacy.md`. API key masked everywhere (`••••••••1234`). `FLAG_SECURE` on onboarding step 3 and account screen.
7. Account status values: UNKNOWN, VALID, INVALID, PAUSED. When F02 marks a key INVALID/PAUSED the UI shows a persistent banner on Dashboard: "API key problem — tap to fix".

## Edge cases
- User kills the app mid-onboarding: resume at the last incomplete step; no half-created accounts with missing secrets (transaction: create account row only after successful validation).
- Keystore failure: show "Secure storage unavailable on this device" with a clear message; do not store the key in plaintext as fallback.
- Pasted key with whitespace/newlines: trim.

## Acceptance criteria
- [ ] F01-AC-01 First launch shows onboarding steps 1-6 in order; completing them lands on Dashboard.
- [ ] F01-AC-02 The API key is validated through `user/basic` and stored only in SecretVault (verify no plaintext copy in Room/DataStore/logs).
- [ ] F01-AC-03 Insufficient access level (code 16) blocks completion with a clear message.
- [ ] F01-AC-04 Key is masked in all UI; `FLAG_SECURE` is set on key entry/account screens.
- [ ] F01-AC-05 Profiles UI is reachable only via Settings > Advanced; create/rename/delete/switch work; deleting wipes secrets, rows, alarms.
- [ ] F01-AC-06 Replace key validates before swapping; different player id triggers a warning and a reset confirmation.
- [ ] F01-AC-07 INVALID/PAUSED account status shows a persistent banner and stops API calls for that account.
- [ ] F01-AC-08 Killing the app during onboarding resumes correctly; no account row without a secret.
- [ ] F01-AC-09 Keystore failure shows an error and never falls back to plaintext.

## Out of scope
Biometric lock, profile export/import, cloud sync, multi-provider UI.


======================================================================
## PART 14 — FEATURE  (`04_features/F02-api-manager.md`)
======================================================================
# F02 — API Manager (client, limiter, cache, clock)
Phase: 1A | Depends on: F01 | Package: `core/network`, `provider/torn`

## Purpose
One safe gateway for all Torn API calls: rate limiting, caching, error mapping, clock sync, key redaction.

## Behavior
1. **HTTP client**: OkHttp; timeouts connect 15 s / read 20 s / call 30 s; HTTPS only; `User-Agent: CrowdHub/<version> (Android)`. No body/header logging in release.
2. **ApiResult/ApiError** as in `architecture-overview.md`. Repositories never throw to UI.
3. **RateLimiter** (token bucket, per account, sliding 60 s window). Constants in `RateLimitConfig`:
   - Global internal cap: **85 req/min** (Torn's limit is 100).
   - Reserved for userscript bridges (Phase 1B): **20 req/min** (class SCRIPT).
   - App classes: USER (button/refresh/probe), WATCH (Watch Mode), BACKGROUND (WorkManager). Caps: USER up to 65, WATCH up to 45, BACKGROUND up to 15 per minute, and the sum of app classes never exceeds 65.
   - Priority when waiting: USER > WATCH > BACKGROUND > SCRIPT. BACKGROUND requests that cannot be served immediately are DROPPED (not queued).
   - Expose live counters for the API Probe screen.
4. **ResponseCache**: key = (accountId, path, sorted query params without the key). Default TTL 30 s. Optional persistent copy (`CacheEntryEntity`) used only to show STALE data when offline/errored (UI marks it Stale).
5. **Single-flight**: identical concurrent requests share one network call.
6. **Error handling**: map errors per `torn-api-reference.md`. On codes 2/10/13/18 -> set account status INVALID/PAUSED, STOP all calls for that account, emit a one-time notification on channel `system`. Code 8 -> global pause 5 min then one probe. Code 5 -> 60 s backoff. Never auto-retry 2/8/10/13/16/18. For IO errors: one retry after 2 s ONLY for USER priority.
7. **Auth**: key injected by the client from SecretVault at call time; v2 via `Authorization: ApiKey` header (VERIFY) else query param for v1. Keys never appear in logs/errors (use `CrowLog` redaction).
8. **Clock**: parse `Date` header on each successful response, keep the last 5 offsets, use the median; `Clock.nowEpochMs()`; unit tests.
9. **TornProvider**: implements `Provider` (`validateCredential`, `isLoginUrl`, `alertSources`, `allowedWebHosts`, `rateLimit`).
10. **TornApi**: typed methods for the selections needed by the active phase, hiding v1/v2 differences. DTOs derived from `_samples`; unknown fields ignored; all nullable.

## Edge cases
- Device clock wrong: offset corrects countdowns; show nothing special unless offset > 5 min (then a small info note in Developer screen).
- Airplane mode: calls fail fast with `Network`; UI shows Stale data with the age.
- Key removed while requests are in flight: cancel and ignore results.

## Acceptance criteria
- [ ] F02-AC-01 Every Torn API call goes through RateLimiter (grep shows no direct OkHttp call to `api.torn.com` outside the API layer).
- [ ] F02-AC-02 Limits implemented as specified (85 global, 20 reserved, class caps 65/45/15) and unit tested.
- [ ] F02-AC-03 Identical requests within 30 s are served from cache; concurrent identical requests are single-flighted (unit tested).
- [ ] F02-AC-04 InvalidKey/Paused errors mark the account, stop further calls, and post one system notification; no retry loop (unit tested).
- [ ] F02-AC-05 Code 8 triggers a 5-minute global pause; code 5 triggers a 60 s backoff.
- [ ] F02-AC-06 The API key never appears in logs, exception messages or UI (verify `CrowLog` redaction and the interceptor).
- [ ] F02-AC-07 `Clock` uses the median offset from `Date` headers and is unit tested.
- [ ] F02-AC-08 DTOs are nullable/lenient and compile against the `_samples` files that exist; missing samples are reported as VERIFY.
- [ ] F02-AC-09 TornProvider implements the Provider interface exactly as defined (no extra abstractions).

## Out of scope
GraphQL/other providers, WebSocket, push.


======================================================================
## PART 15 — FEATURE  (`04_features/F03-alert-engine.md`)
======================================================================
# F03 — Alert Engine (timers, polling, Watch Mode)
Phase: 1A | Depends on: F01, F02 | Package: `core/alerts`

## Purpose
Reliable notifications from two kinds of alerts: TIMER (known future time) and CONDITION (detected from polled data).

## Model
```kotlin
data class AlertCandidate(
  val dedupeKey: String, val accountId: String, val channelId: String,
  val title: String, val body: String,
  val fireAtEpochMs: Long?,         // null = fire now
  val deepLink: String?             // internal route
)
```
`AlertSource.collect(ctx)` returns candidates; `AlertDeduper` decides new/changed/cancelled; `Scheduler` schedules or posts.

## Behavior
1. **Channels** created at app start: `timers` (high), `war` (high), `stocks` (default), `watch_mode` (low, ongoing), `system` (low). Users can mute per channel via system settings; in-app toggles also exist (F00).
2. **TIMER alerts (Phase 1A sources: `torn.bars`, `torn.status`, `torn.travel`, `torn.cooldowns`)**
   - Bar full time for energy/nerve/happy/life (from `fulltime`/timestamps, VERIFY).
   - Cooldown ready: drug, medical, booster.
   - Hospital release and jail release (from `status.until`).
   - Own landing (from travel arrival timestamp) with optional "N minutes before" (default 0; options 0/1/2/5).
   - Scheduling: `AlarmManager.setExactAndAllowWhileIdle` when `canScheduleExactAlarms()`, else `setAndAllowWhileIdle` + visible notice "Timers may be delayed" in Settings > Reliability. All fire times use `Clock`.
   - `AlarmReceiver` posts the notification; `BootReceiver` re-schedules from `ScheduledAlertEntity` after reboot/app update.
3. **Refreshing timers**: whenever Dashboard data refreshes (foreground, background poll, Watch Mode) the sources re-run; the deduper re-schedules when a timestamp changed by > 10 s and cancels timers whose condition vanished (e.g., cooldown used earlier than expected).
4. **CONDITION alerts**: evaluated by `AlertSource`s during polls (Phase 1A: `torn.stocks` — see F08; Phase 2 adds war sources).
5. **Background polling**: `PollWorker` (WorkManager periodic, 15 min, network required) runs the enabled sources at BACKGROUND priority; it must be cheap (Torn: bars+cooldowns+status+travel in <= 2 calls, stocks in 1 call).
6. **Watch Mode** (foreground service, type `dataSync`): started manually (switch on Dashboard/Stocks toolbar). Polls enabled WATCH sources every 30 s (aligned with API cache). Ongoing notification on channel `watch_mode` with action "Stop". Auto-stops after the configured duration (default 4 h) and when the OS timeout callback fires (VERIFY Android 15): stop cleanly and post "Watch Mode stopped by the system". Must stop itself if no watch sources are active.
7. **Deduplication**: `fired_alert` semantics via `ScheduledAlertEntity` (`dedupeKey`, state). The same `dedupeKey` never fires twice unless its `fireAt` moved forward by > 10 s and state was reset by the source.
8. **Deep links**: tapping a notification opens the relevant screen (Dashboard, Stock detail...). `PendingIntent.FLAG_IMMUTABLE`.
9. **Permission flows** are in-context (F01 onboarding + Settings > Reliability).

## Edge cases
- App updated/killed/rebooted: timers restored.
- Two sources emit the same logical alert: dedupe by key (include accountId and logical id, e.g., `torn:energy_full:<accountId>`).
- No network at fire time: timers still fire (they are local); data refresh later.
- Alerts for an INVALID account are suppressed (except the one `system` notice).

## Acceptance criteria
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

## Out of scope
Quiet hours, snooze, custom sounds, FCM push, per-alert history screen (Phase 3).


======================================================================
## PART 16 — FEATURE  (`04_features/F06-dashboard.md`)
======================================================================
# F06 — Dashboard
Phase: 1A | Depends on: F01-F03 | Package: `feature/dashboard`

## Purpose
At-a-glance state of the player, API-only (works without web login).

## Cards (top to bottom)
1. **Status**: state chip (Okay/Hospital/Jail/Traveling/Abroad/Federal/Unknown) + description + countdown to `until` when present.
2. **Bars**: Energy, Nerve, Happy, Life — current/max, progress bar, "full in HH:MM:SS" (from API timestamps via `Clock`), tick info if available.
3. **Cooldowns**: Drug, Medical, Booster — remaining time or "Ready".
4. **Travel** (F07): shown when traveling/abroad (see F07).
5. **Money**: cash on hand + points (fields per `_samples`; show only what exists).
6. **Events/Notifications**: unread counts + latest few events (text) if available; tapping opens the browser (Phase 1B) or does nothing in 1A (no dead buttons: hide the tap affordance in 1A).

## Behavior
- Refresh: pull-to-refresh (USER) + auto-refresh every 30 s while the screen is visible and the app is in the foreground (aligned with API cache) + immediate refresh on resume if data is older than 30 s.
- Calls: combine selections into as few requests as possible (v2/v1 as needed). Respect the limiter.
- Countdowns tick every second locally (UI only; no network).
- Banner states: INVALID/PAUSED key banner (F01); offline banner; Stale badge "updated Xm ago" per card group.
- Toolbar: wordmark, Watch Mode switch (F03), overflow menu.

## UI states
Loading (skeleton cards), Content, Error (with Retry and "Fix API key" when relevant), Empty (not applicable), Stale (dim + age).

## Acceptance criteria
- [ ] F06-AC-01 Status, Bars, Cooldowns, Travel (when relevant), Money, Events cards render from real API data (fields per `_samples`).
- [ ] F06-AC-02 Countdowns use `Clock` and tick every second without network calls.
- [ ] F06-AC-03 Auto-refresh every 30 s only while visible; pull-to-refresh works; no duplicate calls (cache/single-flight).
- [ ] F06-AC-04 Loading/Error/Stale states exist; offline shows last data with age.
- [ ] F06-AC-05 INVALID key banner appears and the screen stops calling the API.
- [ ] F06-AC-06 No UI element is a dead button (events tap hidden until browser exists).
- [ ] F06-AC-07 Status chip uses text + color per branding doc.

## Out of scope
Widgets, customization of card order, charts, networth.


======================================================================
## PART 17 — FEATURE  (`04_features/F07-travel-tracker.md`)
======================================================================
# F07 — Travel Tracker (own flights)
Phase: 1A | Depends on: F06, F03 | Package: `feature/dashboard` (TravelCard), `provider/torn`

## Purpose
Know exactly when YOU land. For your own account the API provides the arrival timestamp (exact), so no estimation is needed.

## Behavior
1. When `travel` data shows an active flight: Travel card shows destination, direction (outbound/return), departed time, arrival time (local), countdown, and a progress bar.
2. When status is Abroad: card shows the current country and "Not flying" (+ quick note that returning flights are tracked once departed).
3. Alerts (F03 TIMER): at arrival (default) and optionally N min before (0/1/2/5 in Settings). The notification text includes the destination and "You can be attacked 15 s after landing" is NOT shown (not relevant to own flights); keep it simple: "Landed in <place>" / "Landing in N min".
4. After landing, the alert is marked fired; a new flight creates a new alert (dedupe key includes the arrival timestamp).
5. Foreign shop/stock info is OUT OF SCOPE (Phase 3 optional connector).

## Edge cases
- Data says flying but `time_left` negative: show "Landing..." and force a refresh.
- Flight time changed by a delay: arrival timestamp changes -> timer re-scheduled (F03 rules).
- Offline: countdown continues from last known data (marked Stale).

## Acceptance criteria
- [ ] F07-AC-01 Travel card shows destination, direction, arrival time and a ticking countdown from API timestamps.
- [ ] F07-AC-02 A landing alert fires at the arrival time (and the optional early alert if configured).
- [ ] F07-AC-03 Changed arrival timestamps re-schedule the alert; completed flights are not re-alerted.
- [ ] F07-AC-04 Abroad state shows the country and no countdown; offline shows Stale data.

## Out of scope
Foreign stock, flight planner, profit calculators, other players' flights (that is F11 in Phase 2).


======================================================================
## PART 18 — FEATURE  (`04_features/F08-stock-market.md`)
======================================================================
# F08 — Stock Market: prices, chart, alerts
Phase: 1A | Depends on: F02, F03 | Package: `feature/stocks`

## Purpose
Clean stock list with charts and price warnings.

## Data
- Source: Torn stocks list (`torn/stocks`, fields per `_samples/torn-stocks.json`): id, name, acronym, current price, market cap, shares, forecast/demand/benefit (show only those present).
- History: Torn does not give history in this endpoint (VERIFY; if `_samples` shows a history field, use it). Crowd Hub records its own snapshots: each successful fetch inserts `StockSnapshotEntity` rows (max 1 per stock per 60 s). Retention per `data-model.md`.

## Behavior
1. **List screen**: search by name/acronym; sort by price / % change (selected window) / name; each row: acronym, name, price, change % (default window 24 h; if insufficient history show "—"), sparkline (last 24 h, Canvas), bell icon if an alert exists.
2. **Detail screen**: price header, line chart (custom Compose Canvas, `Sheen` line, grid in `Outline`, touch-to-inspect crosshair showing time+price), ranges 1 h / 6 h / 24 h / 7 d / 30 d (disable ranges without data; show "Collecting data — only available since <date>"), key stats (market cap, shares, benefit text if present).
3. **Alerts** (per stock, editor bottom sheet): conditions (any combo): price **above** X, price **below** Y, **% move** of at least P% within a window W minutes (5-1440). Options: repeating (with cooldown minutes, default 60) or one-shot. Hysteresis: after firing, the rule re-arms only when the price crosses back past the threshold (or the cooldown elapsed for % moves). Alerts show on channel `stocks`; deep-link opens the detail.
4. **Evaluation**: `StockAlertEvaluator` (pure Kotlin) runs on every new snapshot batch from: foreground refresh, `PollWorker` (BACKGROUND, 15 min), Watch Mode (30 s). Source id `torn.stocks`.
5. **Refresh**: list auto-refreshes every 30 s while visible (USER-class budget is not consumed by auto refresh: use WATCH/foreground class `UI_AUTO` mapped to USER caps; keep total calls tiny — one request returns all stocks).
6. A banner on the Stocks tab explains the background limits: "Background checks run about every 15 minutes. Turn on Watch Mode for 30-second checks."
7. Cleanup job (WorkManager daily or on app start): prune/downsample per retention rules.

## Edge cases
- Price null/zero/missing stock: skip that stock for the cycle; keep last snapshot; never crash charts (guard empty/1-point series).
- Stock removed/renamed: alert remains but shows "Stock not found" and is disabled.
- Large number of snapshots: queries by index; chart downsamples to <= 300 points per render.

## Acceptance criteria
- [ ] F08-AC-01 Stock list renders from the API with search and sorting; sparkline and 24 h change use local history (shows "—" when insufficient).
- [ ] F08-AC-02 Detail chart is a custom Canvas line chart with ranges 1h/6h/24h/7d/30d, touch inspect, and graceful "collecting data" state.
- [ ] F08-AC-03 Price above/below and %-move-in-window alerts can be created, edited, enabled/disabled, deleted.
- [ ] F08-AC-04 `StockAlertEvaluator` is pure Kotlin with unit tests covering above/below, % window, hysteresis, cooldown, one-shot vs repeating.
- [ ] F08-AC-05 Alerts fire from foreground refresh, PollWorker (15 min) and Watch Mode (30 s) and are deduped.
- [ ] F08-AC-06 Snapshots are recorded at most once per minute per stock and pruned/downsampled per retention rules.
- [ ] F08-AC-07 Missing/invalid prices never crash UI or evaluator.
- [ ] F08-AC-08 The background-limits banner is shown; Watch Mode can be toggled from this tab.
- [ ] F08-AC-09 No third-party chart/network libraries added.

## Out of scope
Portfolio/holdings, buy/sell, benefit calculators, forecasting (Phase 3 for portfolio).


======================================================================
## PART 19 — PHASE WORK ORDER  (`05_phases/phase-1A.md`)
======================================================================
# PHASE 1A — Core (profile, API, alerts, dashboard, travel, stocks)

## Goal
A working, installable app that: onboards a Torn API key into a profile container, shows the dashboard, fires reliable local timers, tracks your own flights, and charts stocks with price alerts.

## Features in scope
F00 Settings/Privacy/About/Developer · F01 Profile container & vault · F02 API manager · F03 Alert engine · F06 Dashboard · F07 Own travel · F08 Stock market.

## Explicitly OUT of scope (do not build, do not stub in the UI)
Browser, WebView, userscripts, auto-login, FFScouter, war, flight tracker for others, chain/targets, attack shortcut, widgets, portfolio, light theme, export/import.
Bottom bar in 1A shows only: Dashboard, Stocks. Overflow: Settings, About (Profiles only under Settings > Advanced).

## Prerequisites (check in Step 0; report what is missing)
`_samples/`: user-basic, user-bars, user-cooldowns, user-travel, user-money, user-notifications, torn-stocks. Missing sample = implement nullable + VERIFY + tell the user.

## Build order (finish each step before the next)
1. **Skeleton**: package layout (`package-structure.md`), Crow theme (`branding-crow-theme.md`), `CrowdWordmark`, navigation shell with the two tabs, `AppContainer`, notification channels, manifest flags/permissions (`tech-constraints.md`).
2. **Database & vault**: Room (`data-model.md` Phase 1A entities, exportSchema), `KeystoreCipher`, `SecretVault`, `SettingsStore`, `CrowLog`.
3. **F01**: onboarding flow, account validation hook (stub the network call only until step 4 is done, then wire for real), Profiles (advanced), account screen, wipe logic.
4. **F02**: HttpClientFactory, RateLimiter, ResponseCache, Clock, ApiResult/ApiError, TornApi + TornProvider, DTOs from samples, redaction, unit tests (limiter, clock, cache/single-flight, error handling).
5. **F06 + F07**: Dashboard cards + Travel card + countdown component + states.
6. **F03**: AlertDeduper + Scheduler + AlarmReceiver + BootReceiver + PollWorker + WatchModeService; sources `torn.bars`, `torn.status`, `torn.travel`, `torn.cooldowns`; unit tests for the deduper.
7. **F08**: StockRepository (snapshots), list, detail chart (Canvas), alert editor, `StockAlertEvaluator` + tests, `torn.stocks` AlertSource, cleanup job, Watch Mode integration.
8. **F00**: Settings, Privacy, About, Developer (API Probe), Reliability page.
9. **Audit**: run the self-check, produce the Audit Report with evidence for every ID in `07_checklists/phase-1A-checklist.md`.

## Exit criteria
All checklist IDs PASS (or an honest NOT DONE with reason), app compiles and runs, no guardrail violations.

## Reminders (repeat of the critical rules)
Only this phase. No guessing of API fields. All API calls via RateLimiter. No secrets in logs/UI. No Firebase/Gemini/analytics. Ideas -> `BACKLOG_SUGGESTIONS.md`.


======================================================================
## PART 20 — CHECKLIST  (`07_checklists/phase-1A-checklist.md`)
======================================================================
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


======================================================================
## PART 21 — AUDIT REPORT TEMPLATE  (`08_prompts/audit-report-request.md`)
======================================================================
# Audit Report Request (send after a phase build; also the template the agent must use)

```
Produce the AUDIT REPORT for phase <<PHASE>> now. Do not change code in this reply.

Format (exactly):
1. BUILD STATUS: compiles? (yes/no/unknown) — how you know. Runs in the emulator? (yes/no/unknown).
2. CHECKLIST: for EVERY ID in 07_checklists/phase-<<PHASE>>-checklist.md output one line:
   `<ID> | PASS|FAIL|NOT DONE|NA | evidence: <file path>#<symbol or line range> | note`
   Rules: PASS requires evidence (file + symbol). If you did not verify it, write NOT DONE or FAIL, never PASS.
3. GUARDRAIL SCAN: for each of A-01..G-03 write OK or VIOLATION (+ where). Explicitly answer:
   - Any API field/URL/selector invented? (list them, each marked VERIFY)
   - Any Torn API call that bypasses RateLimiter? (search results)
   - Any secret that could reach logs/UI/backups? (search results)
   - Any request to torn.com pages not triggered by the user? (search results)
   - Any dependency outside the allowed list? (list)
4. DEVIATIONS from the spec (what/why).
5. UNVERIFIED items (VERIFY list) and which `_samples` were missing.
6. KNOWN LIMITATIONS and bugs you are aware of.
7. BACKLOG_SUGGESTIONS added (list).
Be concise and factual. No marketing language.
```


======================================================================
## SAMPLES (paste real responses; do not let the agent guess fields)
======================================================================
### user-basic.json
```
PASTE_REAL_SAMPLE_HERE
```

### user-bars.json
```
PASTE_REAL_SAMPLE_HERE
```

### user-cooldowns.json
```
PASTE_REAL_SAMPLE_HERE
```

### user-travel.json
```
PASTE_REAL_SAMPLE_HERE
```

### user-money.json
```
PASTE_REAL_SAMPLE_HERE
```

### user-notifications.json
```
PASTE_REAL_SAMPLE_HERE
```

### torn-stocks.json
```
PASTE_REAL_SAMPLE_HERE
```

======================================================================
## FINAL REMINDER
Plan only (Step 0) -> wait for APPROVED -> build ONLY phase 1A -> Audit Report with evidence. Never guess API fields. All Torn API calls via RateLimiter. No secrets in logs/UI. No automation of game actions. When in doubt: STOP AND ASK.