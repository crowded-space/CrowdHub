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
