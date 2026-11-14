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
