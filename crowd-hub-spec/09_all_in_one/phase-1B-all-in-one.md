# CROWD HUB — ALL-IN-ONE PROMPT FOR PHASE 1B (GENERATED)
Use this when the project files are NOT visible to the agent. Everything the agent needs is below.
Paste the whole file into AI Studio (Android build). Then paste real sample JSON in the SAMPLES section at the end if you have not already.

======================================================================
## PART 0 — BOOTSTRAP PROMPT
======================================================================
# CROWD HUB — BOOTSTRAP PROMPT
(Paste into Google AI Studio → Build → Android. Replace 1B first.)

You are a senior Android engineer. You are building **Crowd Hub**: a native Android app (Kotlin + Jetpack Compose) that is a companion/QoL tool for the browser game **Torn** (torn.com), architected as a generic "Hub" so other services can be added later. Torn is the first Provider.

The full specification is in this project (repo files). READ THEM before answering. If you cannot see the files, say so immediately and STOP.

## ACTIVE PHASE: 1B
Allowed values: 1A | 1B | 2 | 3. You implement ONLY the active phase (`05_phases/phase-1B.md`) and the features it lists.

## AUTHORITY ORDER (when documents conflict, the higher one wins)
1. `06_guardrails/GUARDRAILS.md`  (hard rules — never violate)
2. `05_phases/phase-1B.md`
3. `04_features/*.md` (only those the phase lists)
4. `02_architecture/*.md`
5. `03_providers/**`
6. This prompt
If something is ambiguous, missing, or conflicting: **STOP AND ASK. NEVER GUESS.** Never invent API fields, endpoints, URLs, CSS selectors or numbers. Unknowns marked VERIFY must be handled defensively (nullable, no crash) and listed in your report.

## READ ORDER
`README_FIRST.md` (skim) → `06_guardrails/*` → `02_architecture/*` → `03_providers/torn/*` (+ `_samples/`) → the phase file → the feature files it lists → `07_checklists/phase-1B-checklist.md`.

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
## PART 12 — FEATURE  (`04_features/F04-in-app-browser-and-session.md`)
======================================================================
# F04 — In-App Browser & Persistent Session
Phase: 1B | Depends on: F01 | Package: `core/browser`, `feature/browser`

## Purpose
Open torn.com inside the app with a login that survives restarts. The API key CANNOT log in to the website (API = data only); the web session is separate.

## Session layers (implement in this order)
1. **API-only mode (default)**: every non-browser feature works without any web login.
2. **Persistent cookies**: user logs in once manually; cookies persist in the WebView profile storage. Flush the cookie store on pause/stop. NOTE: session-only cookies (no expiry) may be dropped when the process restarts -> that is why layer 3 exists.
3. **Auto re-login (OPT-IN, off by default)**: user may store Torn username/password in `SecretVault` (`acct:<id>:web_user`, `acct:<id>:web_pass`). When `Provider.isLoginUrl(currentUrl)` is true and credentials exist, the app fills the login form using selectors from `TornSelectors.kt` (VERIFY with `_samples/login-page.html`). Default: fill only; the user taps Login. A separate toggle "Auto-submit" (default OFF) submits the form.
   - Max 2 auto-fill attempts per 10 minutes. If the page still shows the login form after submit, or a captcha/2FA/verification step is detected, STOP and show "Manual login required".
   - Credentials are inserted into the page via `evaluateJavascript` using properly escaped JSON strings; clear JS variables after use; never log them.
   - Google/Apple sign-in does not work inside the WebView (known limitation). Explain in the opt-in dialog: "Use your Torn username & password. Accounts created with Google/Apple still have a Torn password (check your first email or use Torn's password recovery)."
   - Opt-in dialog text must state: credentials are stored encrypted on this device only and are only typed into the torn.com login form.

## WebView profiles (container isolation)
- If `WebViewFeature.MULTI_PROFILE` is supported (`WebViewFeature.isFeatureSupported`), each ProviderAccount uses its own WebView profile named `acct_<accountId>` (create via `ProfileStore.getInstance().getOrCreateProfile(name)`; attach with `WebViewCompat.setProfile(webView, name)` BEFORE the first load; delete via `ProfileStore.deleteProfile` when the account is deleted).
- If not supported: use the default shared profile; show a small note on the account screen: "Session isolation not supported on this device". Deleting the account clears cookies via `CookieManager.removeAllCookies`.
- `WebProfileManager` hides this behind `fun attach(webView, accountId)` / `fun wipe(accountId)`.

## Browser behavior
1. Single WebView (no tabs). Toolbar: back, forward, reload, home, quick-links menu, scripts menu (Phase 1B F05: kill switch + per-page script list), session chip (Logged in / Logged out / Unknown).
2. Quick links: defaults Home (`https://www.torn.com/index.php`), Profile (`https://www.torn.com/profiles.php?XID={playerId}`), Hospital (`https://www.torn.com/hospitalview.php`). User can add/remove links (host must be allowed).
3. **Settings applied** (hardening, D-04): JavaScript on; DOM storage on; `allowFileAccess=false`; `allowContentAccess=false`; `allowFileAccessFromFileURLs=false`; `allowUniversalAccessFromFileURLs=false`; mixed content blocked; `setSupportMultipleWindows(false)`; no geolocation; file chooser not supported; downloads blocked.
4. **UrlGuard**: only `Provider.allowedWebHosts` load in-app. Other hosts: dialog "Open in your browser?" -> external Custom Tab/Intent. `intent:`/custom schemes are blocked.
5. **Session monitor**: on page load, `Provider.isLoginUrl(url)` -> state LOGGED_OUT, else LOGGED_IN (best effort; UNKNOWN before the first load). Emits state for the account screen and for F04 auto-login.
6. State survives rotation/process death (restore URL; session cookies as per layer 2).
7. Deep-link entry: other features can open the browser with a URL (e.g., attack page) via `BrowserNavigator.open(url)`; if LOGGED_OUT the browser shows the login page.
8. **No automation**: the browser never injects code except (a) the user's enabled userscripts (F05) and (b) the login fill above (layer 3) and (c) user-triggered capture scripts (Phase 2). No auto-refresh timers, no prefetch.

## Edge cases
- Torn shows maintenance/error pages: show the page; no special handling.
- WebView provider missing/outdated: show a clear error with a link to update "Android System WebView".
- Back button inside the browser navigates history first, then leaves the screen.

## Acceptance criteria
- [ ] F04-AC-01 Browser loads torn.com; non-allowed hosts trigger the external-open dialog; `intent:` URLs are blocked.
- [ ] F04-AC-02 WebView hardening settings (all listed) are applied; no `addJavascriptInterface` on untrusted origins.
- [ ] F04-AC-03 After a manual login and a full app restart, the session is still logged in when Torn's cookies are persistent (document the observed behavior in the report).
- [ ] F04-AC-04 Per-account WebView profile used when MULTI_PROFILE is supported; fallback path works and shows the note otherwise; deleting the account wipes the web data.
- [ ] F04-AC-05 Auto-login is opt-in, stores credentials only in SecretVault, fills the form (no submit by default), stops on captcha/2FA/failed attempts, and never logs credentials.
- [ ] F04-AC-06 Session chip reflects Logged in/out via `Provider.isLoginUrl`.
- [ ] F04-AC-07 Quick links are editable and restricted to allowed hosts.
- [ ] F04-AC-08 No code path performs automatic page loads/refreshes/clicks on torn.com (verify by search).
- [ ] F04-AC-09 `BrowserNavigator.open(url)` works from other screens and opens the login page when logged out.

## Out of scope
Tabs, downloads, password manager integration, Google sign-in, ad blocking.


======================================================================
## PART 13 — FEATURE  (`04_features/F05-userscript-engine.md`)
======================================================================
# F05 — Userscript Engine & Manager (PDA-compatible)
Phase: 1B | Depends on: F04, F02 | Package: `core/scripts`, `feature/scripts`

## Purpose
Run Tampermonkey-style userscripts on torn.com pages inside the in-app browser, in a way that existing Torn PDA-compatible scripts can work.

## Script format
Standard metadata block `// ==UserScript== ... // ==/UserScript==`. Supported keys: `@name @namespace @version @description @match @exclude @run-at @grant @connect`. Ignore unknown keys. `@require` and `@resource` are NOT supported (show a warning at install; script may fail). `@run-at`: `document-start`, `document-end` (default), `document-idle`.
Match patterns: Tampermonkey-style (`https://www.torn.com/*`). Only patterns whose host is in `Provider.allowedWebHosts` are valid; others are ignored with a warning.

## Injection
- `document-start`: `WebViewCompat.addDocumentStartJavaScript` (requires `WebViewFeature.DOCUMENT_START_SCRIPT`; allowed origin rules = provider hosts). If unsupported: inject at `onPageStarted` best-effort and warn in the manager.
- `document-end` / `document-idle`: `evaluateJavascript` from `onPageFinished` (idle = post a delayed task ~500 ms).
- Each script runs inside an IIFE with the GM shim; errors are caught and written to `ScriptLogEntity`.
- A global **Kill switch** ("Disable all scripts") is always visible in the browser menu; when on, nothing is injected.

## GM API surface (shim)
`GM_getValue, GM_setValue, GM_deleteValue, GM_listValues, GM_addStyle, GM_log, GM_info, GM_registerMenuCommand (list in menu), GM_xmlhttpRequest, unsafeWindow (= window)` plus promise versions `GM.getValue/setValue/deleteValue/listValues/xmlHttpRequest/info`. Storage = `ScriptKvEntity` (per script, per account).

## Native bridge (cross-origin requests)
- Transport: `WebViewCompat.addWebMessageListener` on a single named object (e.g., `__crowd`), `allowedOriginRules` = provider hosts ONLY. No `addJavascriptInterface`.
- `GM_xmlhttpRequest`/`GM.xmlHttpRequest` and PDA helpers `PDA_httpGet(url, headers?)` / `PDA_httpPost(url, headers?, body)` (VERIFY the exact PDA contract against Torn PDA's docs) are executed natively with OkHttp.
- Restrictions (enforced natively): https only; destination host must be in the script's `@connect` list (or equal to `api.torn.com`); max response 2 MB; timeout 20 s; **hosts `torn.com`/`www.torn.com` (non-API pages) are BLOCKED** from the bridge (C-02); requests to `api.torn.com` pass through RateLimiter class SCRIPT (F02) and return an error object `{error:"rate_limited"}` when throttled; the user's API key is never visible to the script except through the placeholder rule below.
- **API key placeholder (PDA style)**: if the script's `allowApiKey` flag is ON and the source contains the literal placeholder `###PDA-APIKEY###` (VERIFY the exact token against PDA docs), replace it with the active account's key at injection time. Default OFF per script; the install dialog shows the flag and warns that the script can read the key. The key is only substituted into scripts injected on allowed hosts.

## Manager UI (Scripts screen)
- List: name, version, matches, enabled toggle, grants badges (`API key`, `network`), last error.
- Add: paste source, or install from URL (https). Install dialog shows: metadata summary, matches, grants, connects, `allowApiKey` switch, a full-source preview, and a warning: "You are responsible for scripts you install. Torn forbids automation."
- Edit source (plain text editor), delete (also deletes KV + logs), "Clear storage", "View log" (last 200 lines).
- **No automatic updates and no background downloads** (D-05). "Check for update" is a manual button that re-fetches the install URL only when pressed.
- Scripts belong to an account (`accountId`).

## Edge cases
- Script throws: caught, logged, shown as "last error"; never crashes the page/app.
- Huge script (> 500 KB): refuse at install.
- Two scripts modify the same DOM: not our problem; keep isolation per IIFE.
- Page navigations (SPA): inject on each full page load; SPA route changes are the script's responsibility.

## Acceptance criteria
- [ ] F05-AC-01 Metadata parser handles the supported keys and ignores the rest (unit tested incl. `@match`/`@exclude` matching).
- [ ] F05-AC-02 Scripts inject at document-start / end / idle per `@run-at`; unsupported document-start falls back with a warning.
- [ ] F05-AC-03 Kill switch disables all injection immediately.
- [ ] F05-AC-04 GM_* and GM.* functions listed are implemented with persistent per-script storage.
- [ ] F05-AC-05 The bridge uses `addWebMessageListener` with strict origins; no `addJavascriptInterface`.
- [ ] F05-AC-06 `GM_xmlhttpRequest`/`PDA_http*` enforce https-only, `@connect` allowlist, size/time limits, and BLOCK non-API torn.com hosts.
- [ ] F05-AC-07 API calls from scripts to `api.torn.com` use RateLimiter class SCRIPT and never exceed 20/min.
- [ ] F05-AC-08 API-key placeholder substitution happens only when the per-script flag is ON, only on allowed hosts, and the install dialog warns about it.
- [ ] F05-AC-09 Install from URL/paste works with preview + confirmation; no automatic updates or background downloads exist (verify by search).
- [ ] F05-AC-10 Script errors are caught and logged (last 200 lines) without crashing.
- [ ] F05-AC-11 Deleting an account/script removes its scripts, KV storage and logs.

## Out of scope
Script marketplace/browser, `@require`, auto-update, Greasemonkey 3 legacy APIs beyond the list.


======================================================================
## PART 14 — PHASE WORK ORDER  (`05_phases/phase-1B.md`)
======================================================================
# PHASE 1B — Browser, Persistent Session, Userscripts

## Goal
Open torn.com in-app with a login that survives restarts, optional auto re-login, and a PDA-compatible userscript engine with a manager UI.

## Features in scope
F04 In-app browser & session · F05 Userscript engine & manager.

## Explicitly OUT of scope
FFScouter, war, flight tracker, chain/targets, attack shortcut, script marketplace, auto-update of scripts, tabs.
New tab in the bottom bar: Browser. Overflow adds: Scripts.

## Prerequisites
Phase 1A is complete and passed its checklist. `_samples/login-page.html` provided (else mark selectors VERIFY and fail safe: auto-login disabled until provided). Migration for schema v2 (Phase 1B entities) with exportSchema.

## Build order
1. **Browser shell**: `CrowWebView` composable, hardening settings, `UrlGuard`, toolbar, quick links, `BrowserNavigator`.
2. **Session**: `WebProfileManager` (MULTI_PROFILE with fallback), cookie flush, `SessionMonitor` using `Provider.isLoginUrl`, account-screen row "Web session".
3. **Auto-login (opt-in)**: credentials in SecretVault, opt-in dialog, `TornSelectors` fill logic, attempt limiter, captcha/2FA stop. Default: fill only.
4. **Script storage**: Room v2 entities + migration, `UserscriptMetaParser` (+ unit tests), `ScriptRepository`.
5. **Injection**: GM shim, document-start/end/idle, kill switch.
6. **Bridge**: `addWebMessageListener`, GM/PDA native requests with restrictions, RateLimiter class SCRIPT, API-key placeholder rule.
7. **Manager UI**: list, install (paste/URL with preview + warning), edit, delete, logs, per-script storage clear.
8. **Audit** with evidence for `07_checklists/phase-1B-checklist.md`.

## Exit criteria
All checklist IDs PASS or honestly NOT DONE; no automatic page loads/clicks; no background downloads; app compiles/runs.

## Reminders
Only this phase. WebView hardening is mandatory. Never log credentials. Never use `addJavascriptInterface`. No automation of game actions.


======================================================================
## PART 15 — CHECKLIST  (`07_checklists/phase-1B-checklist.md`)
======================================================================
# Phase 1B Checklist (GENERATED — do not edit; edit the feature files and run tools/regenerate.py)

For each line the agent reports: `ID | PASS/FAIL/NOT DONE/NA | evidence: file#symbol | note`.
The user verifies a sample of PASS items by reading the cited code and running `07_checklists/qa-manual-test-plan.md`.

## Feature acceptance criteria

### F04 — In-App Browser & Persistent Session
- [ ] F04-AC-01 Browser loads torn.com; non-allowed hosts trigger the external-open dialog; `intent:` URLs are blocked.
- [ ] F04-AC-02 WebView hardening settings (all listed) are applied; no `addJavascriptInterface` on untrusted origins.
- [ ] F04-AC-03 After a manual login and a full app restart, the session is still logged in when Torn's cookies are persistent (document the observed behavior in the report).
- [ ] F04-AC-04 Per-account WebView profile used when MULTI_PROFILE is supported; fallback path works and shows the note otherwise; deleting the account wipes the web data.
- [ ] F04-AC-05 Auto-login is opt-in, stores credentials only in SecretVault, fills the form (no submit by default), stops on captcha/2FA/failed attempts, and never logs credentials.
- [ ] F04-AC-06 Session chip reflects Logged in/out via `Provider.isLoginUrl`.
- [ ] F04-AC-07 Quick links are editable and restricted to allowed hosts.
- [ ] F04-AC-08 No code path performs automatic page loads/refreshes/clicks on torn.com (verify by search).
- [ ] F04-AC-09 `BrowserNavigator.open(url)` works from other screens and opens the login page when logged out.

### F05 — Userscript Engine & Manager (PDA-compatible)
- [ ] F05-AC-01 Metadata parser handles the supported keys and ignores the rest (unit tested incl. `@match`/`@exclude` matching).
- [ ] F05-AC-02 Scripts inject at document-start / end / idle per `@run-at`; unsupported document-start falls back with a warning.
- [ ] F05-AC-03 Kill switch disables all injection immediately.
- [ ] F05-AC-04 GM_* and GM.* functions listed are implemented with persistent per-script storage.
- [ ] F05-AC-05 The bridge uses `addWebMessageListener` with strict origins; no `addJavascriptInterface`.
- [ ] F05-AC-06 `GM_xmlhttpRequest`/`PDA_http*` enforce https-only, `@connect` allowlist, size/time limits, and BLOCK non-API torn.com hosts.
- [ ] F05-AC-07 API calls from scripts to `api.torn.com` use RateLimiter class SCRIPT and never exceed 20/min.
- [ ] F05-AC-08 API-key placeholder substitution happens only when the per-script flag is ON, only on allowed hosts, and the install dialog warns about it.
- [ ] F05-AC-09 Install from URL/paste works with preview + confirmation; no automatic updates or background downloads exist (verify by search).
- [ ] F05-AC-10 Script errors are caught and logged (last 200 lines) without crashing.
- [ ] F05-AC-11 Deleting an account/script removes its scripts, KV storage and logs.

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
## PART 16 — AUDIT REPORT TEMPLATE  (`08_prompts/audit-report-request.md`)
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
### login-page.html
```
PASTE_REAL_SAMPLE_HERE
```

======================================================================
## FINAL REMINDER
Plan only (Step 0) -> wait for APPROVED -> build ONLY phase 1B -> Audit Report with evidence. Never guess API fields. All Torn API calls via RateLimiter. No secrets in logs/UI. No automation of game actions. When in doubt: STOP AND ASK.