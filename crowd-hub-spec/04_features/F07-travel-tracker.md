# F07 — Travel Tracker (own flights)
Phase: 1A | Depends on: F06, F03 | Package: `feature/dashboard` (TravelCard), `provider/torn`

## Purpose
Know exactly when YOU land. For your own account the API provides the arrival timestamp (exact), so no estimation is needed.

## Behavior
1. When `travel` data shows an active flight: Travel card shows destination, direction (outbound/return), departed time, arrival time (local), countdown, and a progress bar.
2. When status is Abroad: card shows the current country and "Not flying" (+ quick note that returning flights are tracked once departed).
3. Alerts (F03 TIMER): at arrival (default) and optionally N min before (0/1/2/5 in Settings). The notification text includes the destination and "You can be attacked 15 s after landing" is NOT shown (not relevant to own flights); keep it simple: "Landed in <place>" / "Landing in N min".
4. After landing, the alert is marked fired; a new flight creates a new alert (dedupe key includes the arrival timestamp).
5. Foreign shop/stock info is OUT OF SCOPE (Phase 3 optional connector).

## Edge cases
- Data says flying but `time_left` negative: show "Landing..." and force a refresh.
- Flight time changed by a delay: arrival timestamp changes -> timer re-scheduled (F03 rules).
- Offline: countdown continues from last known data (marked Stale).

## Acceptance criteria
- [ ] F07-AC-01 Travel card shows destination, direction, arrival time and a ticking countdown from API timestamps.
- [ ] F07-AC-02 A landing alert fires at the arrival time (and the optional early alert if configured).
- [ ] F07-AC-03 Changed arrival timestamps re-schedule the alert; completed flights are not re-alerted.
- [ ] F07-AC-04 Abroad state shows the country and no countdown; offline shows Stale data.

## Out of scope
Foreign stock, flight planner, profit calculators, other players' flights (that is F11 in Phase 2).
