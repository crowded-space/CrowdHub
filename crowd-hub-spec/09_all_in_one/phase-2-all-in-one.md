# CROWD HUB — ALL-IN-ONE PROMPT FOR PHASE 2 (GENERATED)
Use this when the project files are NOT visible to the agent. Everything the agent needs is below.
Paste the whole file into AI Studio (Android build). Then paste real sample JSON in the SAMPLES section at the end if you have not already.

======================================================================
## PART 0 — BOOTSTRAP PROMPT
======================================================================
# CROWD HUB — BOOTSTRAP PROMPT
(Paste into Google AI Studio → Build → Android. Replace 2 first.)

You are a senior Android engineer. You are building **Crowd Hub**: a native Android app (Kotlin + Jetpack Compose) that is a companion/QoL tool for the browser game **Torn** (torn.com), architected as a generic "Hub" so other services can be added later. Torn is the first Provider.

The full specification is in this project (repo files). READ THEM before answering. If you cannot see the files, say so immediately and STOP.

## ACTIVE PHASE: 2
Allowed values: 1A | 1B | 2 | 3. You implement ONLY the active phase (`05_phases/phase-2.md`) and the features it lists.

## AUTHORITY ORDER (when documents conflict, the higher one wins)
1. `06_guardrails/GUARDRAILS.md`  (hard rules — never violate)
2. `05_phases/phase-2.md`
3. `04_features/*.md` (only those the phase lists)
4. `02_architecture/*.md`
5. `03_providers/**`
6. This prompt
If something is ambiguous, missing, or conflicting: **STOP AND ASK. NEVER GUESS.** Never invent API fields, endpoints, URLs, CSS selectors or numbers. Unknowns marked VERIFY must be handled defensively (nullable, no crash) and listed in your report.

## READ ORDER
`README_FIRST.md` (skim) → `06_guardrails/*` → `02_architecture/*` → `03_providers/torn/*` (+ `_samples/`) → the phase file → the feature files it lists → `07_checklists/phase-2-checklist.md`.

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
## PART 12 — PROVIDER DOC  (`03_providers/torn/flight-times.md`)
======================================================================
# Flight Times (Torn Wiki "Travel" page, retrieved Nov 2026)

## Model
`duration = standard(dest) x typeMultiplier x bookFactor x (1 +/- 3% variance)`
- Types: STANDARD x1.00, AIRSTRIP x0.70, WLT x0.50 (shown as "Private" on the Travel Agent page), BUSINESS x0.30.
- Book "Mailing Yourself Abroad": additional -25% for 31 days -> `bookFactor = 0.75` (not visible in the API).
- Flight time has a **3% variance**. A Detective Agency "Watchlist" special can **extend** a flight; WLT 9M-share holders are immune to such delays (VERIFY).
- While Traveling a player **cannot be attacked**; on arrival there is a **15 s grace period** before they can be attacked.
- Players in a 10-star Theater hide their travel status/destination from others -> untrackable.

## Table (minutes, WITHOUT book) — source of truth for `FlightTable`
| Place | Standard | Airstrip | WLT | Business |
|---|---|---|---|---|
| MEXICO | 24 | 17 | 12 | 7 |
| CAYMAN_ISLANDS | 33 | 23 | 17 | 10 |
| CANADA | 39 | 27 | 19 | 12 |
| HAWAII | 127 | 89 | 63 | 38 |
| UNITED_KINGDOM | 151 | 106 | 75 | 45 |
| ARGENTINA | 158 | 111 | 79 | 47 |
| SWITZERLAND | 166 | 116 | 83 | 50 |
| JAPAN | 213 | 149 | 107 | 64 |
| CHINA | 229 | 160 | 114 | 69 |
| UAE | 257 | 180 | 128 | 77 |
| SOUTH_AFRICA | 282 | 197 | 141 | 85 |

## Reference: wiki table WITH book (minutes) — use ONLY for unit tests
`withBook ~= round(withoutBook x 0.75)` within +/-1 minute.
| Place | Standard | Airstrip | WLT | Business |
|---|---|---|---|---|
| MEXICO | 18 | 13 | 9 | 5 |
| CAYMAN_ISLANDS | 25 | 17 | 13 | 8 |
| CANADA | 29 | 20 | 15 | 9 |
| HAWAII | 95 | 67 | 48 | 29 |
| UNITED_KINGDOM | 113 | 80 | 57 | 34 |
| ARGENTINA | 119 | 83 | 59 | 35 |
| SWITZERLAND | 125 | **97 (wiki anomaly; expected ~87)** | 62 | 38 |
| JAPAN | 160 | 112 | 80 | 48 |
| CHINA | 172 | 120 | 86 | 52 |
| UAE | 193 | 135 | 97 | 58 |
| SOUTH_AFRICA | 212 | 148 | 106 | 64 |
The SWITZERLAND/Airstrip with-book value on the wiki looks like a typo; the estimator uses the multiplicative model, so it is unaffected. Skip that cell in the +/-1 test.

## Implementation notes
- `FlightTable` is a plain Kotlin object with a `Map<Place, IntArray(4)>` of the first table, plus `fun baseMinutes(place, category)`.
- Travel times have been changed by Torn in the past; keep the table in ONE file and expose it in the Flight Debug screen so it can be edited quickly.
- Standard ticket costs exist on the wiki but are irrelevant to Crowd Hub.


======================================================================
## PART 13 — PROVIDER DOC  (`03_providers/torn/connector-ffscouter.md`)
======================================================================
# Connector — FFScouter (third-party)

> All endpoint/field/limit details below are best-known and **VERIFY**: read `https://ffscouter.com/openapi` and paste real responses into `_samples/ffscouter-*.json`. Build DTOs from the samples only.

## What it gives
Fair Fight (FF) score and estimated battle stats (BS) for Torn players, plus a "target finder" that returns candidate targets by filters. FFScouter also offers a war-room style monitor of enemy factions (not used; Crowd Hub builds its own).

## Requirements (VERIFY)
- The player must be **registered at ffscouter.com** (their Torn API key is registered there).
- Auth is the Torn API key (16 alphanumeric) sent as a query parameter or header (VERIFY which). This means **the Torn key is shared with a third party** -> explicit consent screen (F09).
- FFScouter states it stores the key and restricts access to service owners for debugging (research snapshot; show this in the consent text; VERIFY current wording).

## Endpoints (best-known, VERIFY)
- `GET https://ffscouter.com/api/v1/get-stats?key=<key>&targets=<id1,id2,...>` -> per target: `player_id`, `fair_fight`, `bs_estimate`, `bs_estimate_human`, `last_updated` (all nullable; some players may be missing).
- `GET https://ffscouter.com/api/v1/get-targets?key=<key>&<filters>` -> candidate list. Filters seen: level range, FF range, limit (1-50), inactive-only, factionless (names VERIFY).
- A premium flight-tracker endpoint exists; Crowd Hub does NOT use it.

## Client rules
- Separate limiter `FfRateLimiter`: default 10 req/min (conservative; VERIFY the documented limit and lower if needed).
- Batch size default 50 IDs per call (VERIFY the max; keep configurable in `FfDefaults`).
- Cache `FfCacheEntity` per player: fresh for 1 h; manual refresh forces; show age. Never re-request IDs fresh in cache.
- Errors: 401/403 -> "Not registered at FFScouter" + button to open ffscouter.com; 429 -> back off 60 s; 5xx/timeouts -> keep stale cache, show badge.
- If consent is off: no FFScouter call is ever made; UI hides FF/BS columns and FF filters.
- Disable: delete `FfCacheEntity` rows. Re-enable requires consent again.

## Display
- FF shown with 2 decimals; BS shown as `bs_estimate_human` if present else a compact format (`1.2b`). Always labeled "est." with a tooltip "Estimate from FFScouter; may be outdated".
- FF badge bands (configurable in settings, defaults): `< 2.0` muted, `2.0-2.99` Ok color, `>= 3.0` AccentD.


======================================================================
## PART 14 — FEATURE  (`04_features/F09-ffscouter-connector.md`)
======================================================================
# F09 — FFScouter Connector (consent, enable/disable, repository)
Phase: 2 | Depends on: F01, F02 | Package: `connector/ffscouter`
Read first: `03_providers/torn/connector-ffscouter.md`.

## Purpose
Provide Fair Fight (FF) and estimated battle stats (BS) for players, on demand, with explicit consent.

## Behavior
1. **Enable flow** (Account screen > FFScouter row > Enable): consent dialog with the exact points: (a) your Torn API key will be sent to ffscouter.com; (b) FFScouter says it stores the key and restricts access to its owners (show the current wording from the connector doc; VERIFY); (c) you must be registered at ffscouter.com; (d) you can disable anytime. Buttons: "Open ffscouter.com", "I understand, enable", "Cancel". Consent is stored per account (`ConnectorLinkEntity.consentAt`).
2. **Test**: after enabling, run a `get-stats` call for the user's own player id; map errors (401/403 -> "Not registered at FFScouter").
3. **Repository** `FfRepository`: `suspend fun stats(ids: List<Long>, forceRefresh: Boolean): Map<Long, FfStats>`; batches per `FfDefaults.batchSize` (default 50); caches 1 h in `FfCacheEntity`; uses `FfRateLimiter` (default 10/min); returns cached stale values on errors with a Stale flag.
4. **UI components** (reused by F10/F12): `FfBadge(fairFight)`, `BsText(bsEstimate/human)`, both labeled "est.".
5. **Disable**: confirm; delete `FfCacheEntity` rows; set `enabled=false`. When disabled, FF/BS UI and filters are hidden everywhere.
6. The Torn key is read from SecretVault at call time; never stored elsewhere; never logged.

## Edge cases
- Player missing from FFScouter response: show "—" (no FF) and keep them in lists.
- Network/5xx: show cached data (Stale) or "—".
- Account switched/deleted: cache rows removed.

## Acceptance criteria
- [ ] F09-AC-01 No FFScouter request is ever made before consent (verify by search for the base URL usage).
- [ ] F09-AC-02 Consent dialog contains all four points and is stored per account with a timestamp.
- [ ] F09-AC-03 `FfRepository` batches, caches (1 h), rate-limits (10/min default) and returns stale data on error.
- [ ] F09-AC-04 401/403 errors produce the "Not registered at FFScouter" message with a button to open ffscouter.com.
- [ ] F09-AC-05 Disabling deletes the cache and hides FF/BS UI everywhere.
- [ ] F09-AC-06 DTOs are built from `_samples/ffscouter-*.json`; unknown/missing fields are handled (nullable).
- [ ] F09-AC-07 The key never appears in logs/UI; requests use HTTPS only.

## Out of scope
FFScouter premium endpoints, war-room integration, auto-registration.


======================================================================
## PART 15 — FEATURE  (`04_features/F10-ranked-war-stakeout.md`)
======================================================================
# F10 — Ranked War Stakeout
Phase: 2 | Depends on: F02, F03, F09 (optional), F11, F13 | Package: `feature/war`

## Purpose
Watch the enemy faction in a ranked war: who is Okay/Hospital/Abroad/Traveling/online, timers, FF/BS estimates, flight ETAs, and a one-tap attack shortcut. API-only (no page scraping).

## Data flow
1. Own faction id from the API (profile/faction selection, VERIFY in `_samples`).
2. `faction/{ownId}/rankedwars` -> find the active (or upcoming) ranked war -> enemy faction id. (Parse leniently from `_samples/faction-rankedwars.json`.)
3. `faction/{enemyId}/members` -> list of members (status, last_action, level...). One request returns all members.
4. FF/BS via F09 for all member ids (batched, cached) when enabled.
5. **Manual fallback**: "Stake out another faction" -> enter a faction id (for testing or when detection fails).

## Refresh
- War screen visible: refresh every 30 s (aligned with API cache) using class WATCH if Watch Mode is on, else USER.
- Watch Mode on: poll every 30 s even when the screen is not visible (alerts).
- Background (PollWorker): NOT used for war (too slow to be useful) — except a one-time "war started?" check each 15 min that only calls `rankedwars` (BACKGROUND) and posts "Ranked war detected vs <faction>" once per war.

## Diff engine (`MemberDiffer`, pure Kotlin, unit tested)
Compares snapshot N-1 and N per member; emits `MemberEvent`: `StatusChanged(old,new)`, `HospitalEntered/Left`, `TravelStarted(dest, direction)`, `Landed(place)`, `WentOnline/WentOffline/IdleChanged`, `UntilChanged`. Events feed (a) stakeout alerts (b) the Flight tracker (F11). Keep only the latest snapshot per member in Room; previous in memory.

## UI
- Header: our faction vs enemy faction (names), war state (Active/Upcoming/Ended), last updated + Stale badge.
- Quick filter chips (multi-select): **Attackable now** (Okay and not Traveling/Abroad), **Leaving hospital soon** (<= N min, default 5), **Abroad/Traveling**, **Online**, **Hospital**.
- Sort: default = attackable first, then hospital by time left ascending, then traveling by ETA, then abroad, then others. Alternative sorts: level, FF desc, BS asc, name.
- Extra filters (bottom sheet): FF range, BS range, level range, hide offline > X hours.
- **Row** (LazyColumn, key=playerId): name + level; state chip + timer (hospital `until` countdown); last action (Online/Idle/Offline + relative); FF badge + BS est. (if F09 enabled); flight chip (F11) when Traveling/Abroad; star (stakeout toggle); **Attack** button (F13).
- Tapping a row opens a detail sheet: raw status text, last action, flight history for that player, "Open profile" (browser).

## Stakeout (per member, star icon)
Alert options per starred member: leaves hospital (at `until` minus lead; default 0 s), lands (at ETA window start minus lead), becomes Okay, goes online. Lead times configurable (0/10/30/60 s, 2 min). Alerts on channel `war`, deduped by `war:<playerId>:<eventType>:<timestampBucket>`.

## Edge cases
- No active war: show "No active ranked war" + manual faction input.
- Members count changes mid-war: list adapts.
- `until` missing for hospital: show "Hospital (time unknown)" and do not schedule.
- Rate limits: when throttled, keep last data (Stale) and show the next-refresh time.
- Enemy hides travel (10-star Theater): such a player simply looks Okay/hidden; no special handling.

## Acceptance criteria
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

## Out of scope
War score charts, chain/respect analytics, auto-targeting, enemy flight planning beyond F11.


======================================================================
## PART 16 — FEATURE  (`04_features/F11-flight-tracker.md`)
======================================================================
# F11 — Flight Tracker (ETA estimator for OTHER players)
Phase: 2 | Depends on: F10 (MemberDiffer), flight-times.md | Package: `feature/flight`, `provider/torn/FlightTable`

## Purpose
Estimate when an enemy lands, for **preparation and calculation** (not pinpoint accuracy). Every ETA is a **window** with a **confidence** level.

## Inputs
- `MemberEvent.TravelStarted(playerId, direction, place, planeTypeApi?, prevPollAt, detectedAt)` from F10.
- `status.until` (if the API provides it for Traveling — VERIFY via `_samples/flight-data-notes.md`). **If `until` is present and in the future, use it as the ETA (confidence HIGH) and skip estimation.**
- `FlightTable` (`flight-times.md`), `PlaneTypeMap` (Room, user-editable), `PlayerFlightProfile` (book knowledge).

## Plane type mapping
`status.plane_image_type` (API string, VERIFY values) -> `PlaneCategory` {STANDARD, AIRSTRIP, WLT, BUSINESS, UNKNOWN} via `PlaneTypeMapEntity`. Defaults: EMPTY (everything UNKNOWN) until the user fills `_samples/flight-data-notes.md` and the Flight Debug screen. Do NOT guess the mapping.

## Algorithm (`FlightEstimator`, pure Kotlin, unit tested)
```
base      = FlightTable.baseMinutes(place, category)            // category known -> one value
bases     = all 4 categories                                     // category UNKNOWN -> min..max
variance  = 3%
bookFactor: hasBook == true -> 0.75 ; false -> 1.0 ; unknown -> {0.75 , 1.0}
durMin    = min(base) * min(bookFactor) * 0.97
durMax    = max(base) * max(bookFactor) * 1.03
departWindow = [prevPollAt, detectedAt]       // when the status flipped to Traveling between two polls
ETA window = [ departWindow.start + durMin , departWindow.end + durMax ]
```
- If departure was NOT observed (the player was already Traveling when first seen at `firstSeenAt`): the flight started at some unknown time before `firstSeenAt`, so the ETA window is `[now, firstSeenAt + durMax]` (it cannot land later than a flight that left exactly at `firstSeenAt`). Confidence is GRAY.
- Direction & place from `status.description` parsing (`torn-api-reference.md`).
- Overdue: if now > window.end + 60 s and still Traveling -> state DELAYED (a Detective Agency delay is possible): show "Delayed (+mm:ss)" and widen the window end by +10% of durMax every 5 min; never claim precision.

## Confidence
- **GREEN**: category known AND book known AND departure observed.
- **YELLOW**: departure observed but book OR category unknown (window shown as range).
- **GRAY**: departure not observed, place unknown, or `Hidden`.
Display: chip "Lands ~mm:ss–mm:ss" (or `~mm:ss` when window <= 2 min) + colored dot + "est." label. Chip tap -> sheet explaining the model and the uncertainty sources.

## Learning (per player)
- On landing (state leaves Traveling): `observed = landedWindow - departWindow` (needs observed departure). Compare against `durMin..durMax` for book=false and book=true for the known category; if exactly one fits -> set `hasBook` accordingly with `lastObservedAt`.
- Validity: `hasBook=true` known for 31 days; `hasBook=false` known for 7 days (the player may buy a book). Expired -> unknown.
- Persist `FlightObservationEntity` rows (keep last 50 per player; prune 30 days).
- If category is UNKNOWN but observed ratio matches exactly one category for a known place/book, DO NOT auto-fill the map; show the suggestion in Flight Debug only.

## Alerts
- "Lands in N min" scheduled at `window.start - lead` (default lead 2 min, "earliest" mode) or at the window midpoint (user option). If the window moves, re-schedule (F03 rules).
- After landing there is a **15 s grace period** before the player can be attacked: the row shows "Attackable in 0:15" countdown after the landing event. While Traveling the player cannot be attacked.

## Flight Debug screen (Settings > Developer > Flight Debug)
- Editable `PlaneTypeMap` (API value -> category) and a list of observed `plane_image_type` values with counts.
- Table view of `FlightTable` (read-only).
- Recent observations: place, direction, plane value, observed duration vs expected for each category/book combo, inferred book.

## Edge cases
- Return flights (`Returning to Torn from X`) use the same table; direction RETURN.
- Hospitalized abroad then released: not a flight.
- Unknown place string: log once to Flight Debug; GRAY.
- Player hides travel (10-star Theater): no events -> nothing shown.

## Acceptance criteria
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

## Out of scope
Using FFScouter's premium flight data, predicting departures, flight planning for the user.


======================================================================
## PART 17 — FEATURE  (`04_features/F12-chain-target-assist.md`)
======================================================================
# F12 — Chain Target Assist
Phase: 2 | Depends on: F02, F04 (browser), F09 (optional), F13 | Package: `feature/chain`

## Purpose
Find good targets quickly: list players (especially hospitalized ones with their release time), filter by FF and level, sort by release time, and jump to the attack page.

## Target sources (the API has NO list of "all players in hospital" that we found; do not invent one)
- **A. Hospital page capture** (primary for hospital lists): the user opens the hospital list page in the in-app browser (F04) and taps **"Capture this page"**. Only then the app runs a read-only extraction script (`HospitalPageParser`, selectors from `TornSelectors.kt`, VERIFY with `_samples/hospital-page-rows.html`) on the page that is **currently displayed**, extracting per row: player id, name, level (if shown), remaining hospital time text. Convert the remaining text to seconds; store `capturedAt` + `remainingSec` -> `leavesAt = capturedAt + remainingSec` (use `Clock`). No auto-navigation, no auto-pagination, no auto-refresh; each page the user wants is captured manually (button "Capture" again).
- **B. FFScouter targets** (requires F09): `get-targets` with filters (level range, FF range, limit<=50, inactive/factionless options per connector doc) -> candidate ids with FF/BS. Live status via Torn `user/{id}/profile` (API) for at most **25 tracked targets**, each refreshed at most once per minute, class WATCH/USER, spread evenly by the limiter.
- **C. Personal list**: manual add by player id/URL; also "Add to targets" from War rows (F10).

## UI (Targets tab)
- Segmented source switch: Hospital · FFScouter · My list.
- **Filters** (sheet): FF min/max; level min/max; BS max (optional); status = All / Hospital / Okay; "Leaving hospital within N min" (default 10); hide offline > X hours (only when data exists).
- **Sort**: leaves soonest (default for Hospital), FF desc, BS asc, level, name.
- **Row**: name+level, status chip + countdown (`leavesAt - now`), FF badge, BS est., age of data ("captured 3m ago" / "updated 40s ago"), Attack button (F13), star (watch: alert when they become Okay).
- Empty state tells the user how to capture ("Open Hospital in Browser, scroll to the page you want, tap Capture.").
- **Staleness**: hospital captures older than 10 min are dimmed; "Refresh top N via API" button (N<=20) re-checks status through the API (USER priority).

## Alerts
Starred targets: notify when `leavesAt` is reached (or status becomes Okay via API) — channel `war`, dedupe `chain:<playerId>:out:<leavesAtBucket>`.

## Edge cases
- Selector drift: if parsing yields 0 rows on a hospital URL, show "Page layout not recognized — update TornSelectors" and do not guess.
- Time text formats (e.g., `HH:MM:SS`, `Xd Yh`, `Nm`): parser handles the formats seen in `_samples`; unknown -> row without timer.
- Duplicate players across captures: latest capture wins.
- FFScouter disabled: sources A and C work without FF/BS; source B hidden.

## Acceptance criteria
- [ ] F12-AC-01 "Capture this page" is a user-pressed button; no code path loads/refreshes/paginates torn.com pages automatically (verify by search).
- [ ] F12-AC-02 `HospitalPageParser` is selector-driven from `TornSelectors`, parses rows from the sample HTML, handles unknown formats gracefully, and is unit tested with the sample.
- [ ] F12-AC-03 Captured rows store `leavesAt` (using `Clock`) and countdowns tick locally without network.
- [ ] F12-AC-04 Filters (FF, level, status, leaving-within-N-min) and sorts work and combine correctly (unit tested `TargetFilters`).
- [ ] F12-AC-05 FFScouter source works only with consent; live status checks are limited to 25 targets at <= 1 request/min per target through the RateLimiter.
- [ ] F12-AC-06 My list supports manual add/remove and "Add to targets" from War rows.
- [ ] F12-AC-07 Starred targets alert when they leave hospital; deduped.
- [ ] F12-AC-08 The Attack button opens the attack page via F13 only.
- [ ] F12-AC-09 Staleness dimming and "Refresh top N via API" work; empty and error states exist.

## Out of scope
Auto-attack/auto-queue, chain timer UI (F14), bounty lists, stat estimation of our own.


======================================================================
## PART 18 — FEATURE  (`04_features/F13-attack-shortcut.md`)
======================================================================
# F13 — Attack Shortcut
Phase: 2 | Depends on: F04 | Package: `provider/torn/TornUrls`, `feature/browser`

## Behavior
`AttackNavigator.open(playerId)` builds `TornUrls.attack(playerId)` = `https://www.torn.com/loader.php?sid=attack&user2ID={playerId}` and opens it via `BrowserNavigator.open(url)` (F04). Setting "Open attacks in": In-app browser (default) / External browser.
- If the web session is LOGGED_OUT the browser shows the login page (user logs in manually; then the user taps the Attack button again).
- The app NEVER interacts with the attack page (no clicking "Start fight", no script injection by Crowd Hub; user-enabled userscripts still run as usual).
- Attack buttons appear in: War rows (F10), Targets rows (F12), and the member detail sheet.

## Acceptance criteria
- [ ] F13-AC-01 The URL is built exactly as specified from the player id; only digits are accepted as id.
- [ ] F13-AC-02 The attack page opens in the chosen browser mode; external mode uses an Intent.
- [ ] F13-AC-03 No Crowd Hub code clicks or scripts anything on the attack page (verify by search).
- [ ] F13-AC-04 Logged-out state leads to the login page without losing the intended target (the user can tap Attack again).

## Out of scope
Attack automation, fight result tracking.


======================================================================
## PART 19 — PHASE WORK ORDER  (`05_phases/phase-2.md`)
======================================================================
# PHASE 2 — FFScouter, Ranked War Stakeout, Flight Tracker, Chain Target Assist

## Goal
Enemy faction monitoring with FF/BS estimates, flight ETA windows with confidence, hospital-based chain targets with filters, and one-tap attack page opening.

## Features in scope
F09 FFScouter connector · F10 Ranked war stakeout · F11 Flight tracker · F12 Chain target assist · F13 Attack shortcut.

## Explicitly OUT of scope
Anything in F14, FFScouter premium endpoints, automation of any kind.
New bottom tabs: War, Targets (total: Dashboard, Stocks, War, Targets, Browser).

## Prerequisites (Step 0 MUST verify; if missing -> ask, do not guess)
`_samples/`: faction-members (with Traveling/Hospital/Abroad/Okay members), faction-rankedwars, user-profile-other, ffscouter-get-stats, ffscouter-get-targets, hospital-page-rows.html, flight-data-notes.md (answers on `status.until` and `plane_image_type`).
Phases 1A and 1B completed and passed. Migration for schema v3 with exportSchema.

## Build order
1. **Contract check (no code)**: in Step 0 list the DTO fields you derived from each sample and every item still VERIFY.
2. **F09**: consent flow, FfRepository, limiter, cache, UI components.
3. **F13**: TornUrls.attack + AttackNavigator.
4. **F10 core**: rankedwars detection, members repository, `MemberDiffer` (+ tests), War screen (list/filters/sort), manual faction fallback.
5. **F11**: FlightTable, PlaneTypeMap, FlightEstimator (+ tests), PlayerFlightProfile learning, chips/alerts, Flight Debug screen.
6. **F10 stakeout**: starred members, alerts, Watch Mode integration, war-start detection.
7. **F12**: HospitalPageParser (+ tests with the sample), Capture button in the browser toolbar (visible only on the hospital URL), Targets screen, sources A/B/C, filters (+ tests), alerts.
8. **Audit** with evidence for `07_checklists/phase-2-checklist.md`.

## Exit criteria
All IDs PASS or honest NOT DONE; no non-API torn.com request without a user press; API budget respected (show limiter counters in the report from a test run if possible).

## Reminders
Estimates are labeled "est." with confidence. No automation. FFScouter only after consent. Do not invent plane-type mappings.


======================================================================
## PART 20 — CHECKLIST  (`07_checklists/phase-2-checklist.md`)
======================================================================
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
### faction-members.json
```
PASTE_REAL_SAMPLE_HERE
```

### faction-rankedwars.json
```
PASTE_REAL_SAMPLE_HERE
```

### user-profile-other.json
```
PASTE_REAL_SAMPLE_HERE
```

### ffscouter-get-stats.json
```
PASTE_REAL_SAMPLE_HERE
```

### ffscouter-get-targets.json
```
PASTE_REAL_SAMPLE_HERE
```

### hospital-page-rows.html
```
PASTE_REAL_SAMPLE_HERE
```

### flight-data-notes.md
```
PASTE_REAL_SAMPLE_HERE
```

======================================================================
## FINAL REMINDER
Plan only (Step 0) -> wait for APPROVED -> build ONLY phase 2 -> Audit Report with evidence. Never guess API fields. All Torn API calls via RateLimiter. No secrets in logs/UI. No automation of game actions. When in doubt: STOP AND ASK.