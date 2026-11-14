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
