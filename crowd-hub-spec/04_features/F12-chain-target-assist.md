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
