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
