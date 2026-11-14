# Crowd Hub (Android)

**Watch the flock.** Privacy-first companion app for Torn built natively with Kotlin, Jetpack Compose, Material 3, Room, WorkManager, and Android Keystore.

> **Disclaimer:** Unofficial app. Not affiliated with or endorsed by Torn. Complying with Torn's rules on accounts, scripting, and API usage is your responsibility.

## Architecture & Modules

Single-module Android Gradle project (`:app`) organized by strict layer boundaries:

- `core/ui` — Crow theme (`CrowBlack` `#0E0F13`, `CrowSheen` `#4E5D94`, `AmberBeak` `#D9A441`), typography, edge-to-edge components, `NavRoutes`.
- `core/vault` — `KeystoreCipher` (AES-256-GCM) and `SecretVault` backed by Room `secret_blob` (never SharedPreferences or logs).
- `core/network` — HTTPS-only `OkHttpClient`, sliding 60s `RateLimiter` (85 req/min global cap, 20 reserved for `SCRIPT`, congestion dropping for `BACKGROUND`), `ResponseCache` (30s TTL + single-flight + Room `cache_entry` stale fallback), `ServerClock` (median HTTP `Date` offset), and `CrowLog` key redaction.
- `core/alerts` — `AlertDeduper` (>10s drift threshold), `AlarmManager` exact/inexact scheduler, `BootReceiver`, `PollWorker` (15-min background cycle), and `WatchModeService` (foreground 30s ticker with auto-stop).
- `core/browser` & `core/scripts` — Hardened in-app `WebView` restricted to `torn.com` / `www.torn.com`, multi-profile cookie isolation, `UserscriptEngine` + `GmBridge` (`addWebMessageListener`), and opt-in encrypted auto re-login (never auto-solves captcha/2FA).
- `provider/torn` — `TornApi`, `TornMappers`, `TornAlertSources`, `TornUrls`, `TornSelectors`, `FlightTable`.
- `connector/ffscouter` — Opt-in FFScouter `get-stats` & `get-targets` connector with explicit consent gate, 10 req/min limiter, 50-ID batching, and 1h Room cache.
- `feature/*` — Dashboard, Stocks (Canvas charts, alerts, 30d retention/downsampling), Ranked War Stakeout, Flight Estimator, Chain Target Assist (Hospital capture, FFScouter, My List), Browser, Userscripts, Profiles (Settings -> Advanced), and Settings/Privacy/About/Reliability/Developer Probe.

## Verification Status (`VERIFY` Items)

Because the specification `_samples/*.json` and `_samples/*.html` files contained placeholders, all Torn & FFScouter DTOs use defensive lenient parsing. Before shipping to production with live accounts, verify:
- Torn v1 vs v2 field shapes & `Authorization: ApiKey` header in `TornApi` / `TornDto`
- Torn Hospital page DOM selectors in `TornSelectors.kt`
- API `plane_image_type` string mappings in Developer -> Flight Estimator Debug
- Key validation flows via Settings -> Version (x7) -> Developer -> API Probe
