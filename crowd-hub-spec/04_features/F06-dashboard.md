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
