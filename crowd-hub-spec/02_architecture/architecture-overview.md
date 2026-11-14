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
